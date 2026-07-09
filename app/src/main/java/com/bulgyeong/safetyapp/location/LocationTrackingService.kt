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
        val estimated = bleScanner.estimateLocationFromReadings(readings)
        estimated?.let { (lat, lon) ->
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
        stateMachine.onPdrMotion()
    }

    override fun onPdrSensorSample(timestamp: Long, json: String) {
        repository.appendSensorSample("{\"type\":\"pdr\",\"timestamp\":$timestamp,\"payload\":$json}")
    }

    companion object {
        private const val NOTIFICATION_ID = 9012
        private const val CHANNEL_ID = "location_tracking_channel"
    }
}
