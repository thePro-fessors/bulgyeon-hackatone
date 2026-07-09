package com.bulgyeong.safetyapp.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.math.pow

class BleUwbScanner(
    private val context: Context,
    private val callback: Callback
) {
    interface Callback {
        fun onBleReadings(readings: List<BeaconReading>)
    }

    private val bluetoothAdapter: BluetoothAdapter?
    private val scanner: BluetoothLeScanner?
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanPeriodMs = 12000L
    private val restPeriodMs = 18000L

    private val knownAnchors = listOf(
        BeaconAnchor("anchor-north-01", "AA:BB:CC:DD:EE:01", 37.5010, 127.0396),
        BeaconAnchor("anchor-east-01", "AA:BB:CC:DD:EE:02", 37.5005, 127.0401),
        BeaconAnchor("anchor-south-01", "AA:BB:CC:DD:EE:03", 37.5000, 127.0390)
    )

    private val knownAnchorMap = knownAnchors.associateBy { it.macAddress }
    private var isScanning = false

    init {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        scanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning || scanner == null || bluetoothAdapter?.isEnabled != true) return
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000180F-0000-1000-8000-00805f9b34fb")).build())
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        scanner.startScan(filters, settings, scanCallback)
        isScanning = true
        scanHandler.postDelayed(::stopScanning, scanPeriodMs)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning || scanner == null) return
        scanner.stopScan(scanCallback)
        isScanning = false
        scanHandler.postDelayed({ if (!isScanning) startScanning() }, restPeriodMs)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address == null) return
            val reading = buildReading(result)
            if (reading != null) {
                callback.onBleReadings(listOf(reading))
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            val readings = results.mapNotNull(::buildReading)
            if (readings.isNotEmpty()) {
                callback.onBleReadings(readings)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // 스캔 실패는 재시도 타이머로 회복
        }
    }

    private fun buildReading(result: ScanResult): BeaconReading? {
        val address = result.device.address ?: return null
        val rssi = result.rssi
        val knownAnchor = knownAnchorMap[address]
        val estimatedDistance = rssiToDistance(rssi)
        val anchorId = knownAnchor?.anchorId
        return BeaconReading(
            timestamp = System.currentTimeMillis(),
            anchorId = anchorId,
            macAddress = address,
            rssi = rssi,
            estimatedDistanceMeters = estimatedDistance,
            latitude = knownAnchor?.latitude,
            longitude = knownAnchor?.longitude
        )
    }

    fun estimateLocationFromReadings(readings: List<BeaconReading>): Pair<Double, Double>? {
        val anchors = readings.filter { it.latitude != null && it.longitude != null }
        if (anchors.size < 2) return null
        var weightSum = 0f
        var weightedLat = 0.0
        var weightedLon = 0.0

        anchors.forEach { anchor ->
            val weight = 1f / (anchor.estimatedDistanceMeters.coerceAtLeast(1f))
            weightSum += weight
            weightedLat += anchor.latitude!! * weight
            weightedLon += anchor.longitude!! * weight
        }

        if (weightSum <= 0f) return null
        return Pair(weightedLat / weightSum, weightedLon / weightSum)
    }

    private fun rssiToDistance(rssi: Int): Float {
        val txPower = -59
        val ratio = rssi.toDouble() / txPower
        return if (ratio < 1.0) {
            ratio.toFloat().pow(10f)
        } else {
            ((0.89976) * ratio.toFloat().pow(7.7095f) + 0.111f).toFloat()
        }
    }
}
