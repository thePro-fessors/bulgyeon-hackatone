package com.bulgyeong.safetyapp.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.location.*

class LocationTrackingService : LifecycleService(), TrackingStateMachine.Listener, BleUwbScanner.Callback, PdrNavigator.PdrCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: LocationRepository
    private lateinit var bleScanner: BleUwbScanner
    private lateinit var pdrNavigator: PdrNavigator
    private lateinit var stateMachine: TrackingStateMachine

    private var lastGpsTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        repository = LocationRepository(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        bleScanner = BleUwbScanner(this, this)
        pdrNavigator = PdrNavigator(this, this)
        stateMachine = TrackingStateMachine(this)
        setupLocationCallback()
        createNotificationChannel()
        startForegroundServiceNotification()
        stateMachine.start()
        requestLocationUpdates()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        bleScanner.stopScanning()
        pdrNavigator.stop()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // 최초 기동 시 즉시 위치 정보 1회 리포트 (기본 가상 좌표 또는 Last Known Location)
        val employeeId = com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId
        if (employeeId != null) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                val lat = loc?.latitude ?: 35.1595
                val lon = loc?.longitude ?: 129.0430
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportLocation(
                            com.bulgyeong.safetyapp.data.api.LocationReportRequest(
                                employeeId,
                                lat,
                                lon
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.addOnFailureListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportLocation(
                            com.bulgyeong.safetyapp.data.api.LocationReportRequest(
                                employeeId,
                                35.1595,
                                129.0430
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastGpsTimestamp = System.currentTimeMillis()
                stateMachine.onGpsLocation(location.accuracy)
                repository.appendTrackingRecord(location.toTrackingRecord("gps"))
                updateLocationText("🌐 GPS 추적 중\n위도: ${String.format("%.5f", location.latitude)}\n경도: ${String.format("%.5f", location.longitude)}\n정확도: ${location.accuracy}m")

                // 실시간 GPS 서버 전송 연동
                val employeeId = com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId
                if (employeeId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportLocation(
                                com.bulgyeong.safetyapp.data.api.LocationReportRequest(
                                    employeeId,
                                    location.latitude,
                                    location.longitude
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                when (stateMachineState) {
                    TrackingState.PDR_ACTIVE, TrackingState.BLE_UWB_ACTIVE -> stopFallbackModes()
                    else -> { }
                }
            }
        }
    }

    private var stateMachineState: TrackingState = TrackingState.GPS_ACTIVE

    private fun stopFallbackModes() {
        bleScanner.stopScanning()
        pdrNavigator.stop()
    }

    private fun createNotificationChannel() {
        val channelId = CHANNEL_ID
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "위치 추적", NotificationManager.IMPORTANCE_LOW).apply {
                description = "필드 작업자 위치 추적을 위한 백그라운드 서비스"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("필드 위치 추적 실행 중")
            .setContentText("GPS, BLE/UWB, PDR를 자동 전환하여 경로를 기록합니다.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStateChanged(state: TrackingState) {
        stateMachineState = state
        when (state) {
            TrackingState.GPS_ACTIVE -> {
                bleScanner.stopScanning()
                pdrNavigator.stop()
            }
            TrackingState.BLE_UWB_ACTIVE -> {
                pdrNavigator.stop()
                bleScanner.startScanning()
            }
            TrackingState.PDR_ACTIVE -> {
                bleScanner.startScanning()
                pdrNavigator.start()
            }
        }
    }

    override fun onRequestBleScan() {
        bleScanner.startScanning()
    }

    override fun onRequestPdrStart() {
        pdrNavigator.start()
    }

    override fun onAnchorDetected(anchorLat: Double, anchorLon: Double) {
        pdrNavigator.applyAnchorCorrection(anchorLat, anchorLon)
        val correctedPosition = pdrNavigator.getAbsoluteGeoPosition()

        // 앵커 절대 보정 좌표 서버 보고 연동
        val employeeId = com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId
        if (employeeId != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportLocation(
                        com.bulgyeong.safetyapp.data.api.LocationReportRequest(
                            employeeId,
                            correctedPosition.first,
                            correctedPosition.second
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        repository.appendTrackingRecord(
            TrackingRecord(
                timestamp = System.currentTimeMillis(),
                source = "anchor_correction",
                latitude = correctedPosition.first,
                longitude = correctedPosition.second,
                altitude = null,
                accuracy = null,
                relativeX = pdrNavigator.getCurrentPosition().xMeters,
                relativeY = pdrNavigator.getCurrentPosition().yMeters,
                extra = "anchor correction 적용"
            )
        )
    }

    override fun onBleReadings(readings: List<BeaconReading>) {
        repository.appendSensorSample(
            "{\"type\":\"ble_scan\",\"timestamp\":${System.currentTimeMillis()},\"readings\":${readings.map { "{\\\"macAddress\\\":\\\"${it.macAddress}\\\",\\\"rssi\\\":${it.rssi},\\\"distance\\\":${it.estimatedDistanceMeters}}" }}}"
        )
        stateMachine.onBleReadings(readings)
        updateLocationText("📶 BLE 스캔 중\n감지된 비콘 수: ${readings.size}개")
        val estimated = bleScanner.estimateLocationFromReadings(readings)
        estimated?.let { (lat, lon) ->
            updateLocationText("📶 BLE 신호 기반 위치 추정 완료\n위도: ${String.format("%.5f", lat)}\n경도: ${String.format("%.5f", lon)}")

            // BLE 기반 추정 위치 서버 보고 연동
            val employeeId = com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId
            if (employeeId != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportLocation(
                            com.bulgyeong.safetyapp.data.api.LocationReportRequest(
                                employeeId,
                                lat,
                                lon
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            repository.appendTrackingRecord(
                TrackingRecord(
                    timestamp = System.currentTimeMillis(),
                    source = "ble_estimate",
                    latitude = lat,
                    longitude = lon,
                    altitude = null,
                    accuracy = null,
                    relativeX = null,
                    relativeY = null,
                    extra = "BLE/UWB fallback 위치 추정"
                )
            )
        }
    }

    override fun onPdrPositionUpdated(position: PdrPosition) {
        repository.appendTrackingRecord(
            TrackingRecord(
                timestamp = position.timestamp,
                source = "pdr",
                latitude = null,
                longitude = null,
                altitude = null,
                accuracy = null,
                relativeX = position.xMeters,
                relativeY = position.yMeters,
                extra = "PDR 상대 경로, heading=${position.headingDegrees}, steps=${position.stepCount}"
            )
        )
        updateLocationText("🚶 PDR 이동 측정 중\n상대 X: ${String.format("%.1f", position.xMeters)}m\nY: ${String.format("%.1f", position.yMeters)}m\n걸음 수: ${position.stepCount}보")
        stateMachine.onPdrMotion()
    }

    override fun onPdrSensorSample(timestamp: Long, json: String) {
        repository.appendSensorSample("{\"type\":\"pdr\",\"timestamp\":$timestamp,\"payload\":$json}")
    }

    companion object {
        private const val NOTIFICATION_ID = 9012
        private const val CHANNEL_ID = "location_tracking_channel"

        private val _lastLocationText = MutableStateFlow("📡 추적 시작 대기 중...")
        val lastLocationText = _lastLocationText.asStateFlow()

        fun updateLocationText(text: String) {
            _lastLocationText.value = text
        }
    }
}
