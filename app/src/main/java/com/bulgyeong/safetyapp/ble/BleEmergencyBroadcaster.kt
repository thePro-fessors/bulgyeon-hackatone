package com.bulgyeong.safetyapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import java.nio.ByteBuffer

class BleEmergencyBroadcaster(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private var advertiseCallback: AdvertiseCallback? = null

    @SuppressLint("MissingPermission")
    fun startAdvertising(employeeId: String, latitude: Double, longitude: Double) {
        if (advertiser == null) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val manufacturerId = 0xFFF0
        val payloadBuffer = ByteBuffer.allocate(13) // total 13 bytes payload

        // 1. Emergency Type flag (0x03 for Fall Fallback)
        payloadBuffer.put(0x03.toByte())

        // 2. Employee Number integer (4 bytes)
        val empNumber = employeeId.toIntOrNull() ?: 0
        payloadBuffer.putInt(empNumber)

        // 3. Float Latitude (4 bytes) and Longitude (4 bytes)
        payloadBuffer.putFloat(latitude.toFloat())
        payloadBuffer.putFloat(longitude.toFloat())

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(manufacturerId, payloadBuffer.array())
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
            }
        }

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (advertiser == null || advertiseCallback == null) return
        advertiser.stopAdvertising(advertiseCallback)
        advertiseCallback = null
    }
}
