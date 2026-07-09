package com.bulgyeong.safetyapp.location

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

data class WitnessRecord(
    val timestamp: Long,
    val peerId: String,
    val rssi: Int,
    val distance: Double
) {
    fun toJsonLine(): String {
        return "{\"timestamp\":$timestamp,\"peerId\":\"$peerId\",\"rssi\":$rssi,\"distance\":$distance}\n"
    }
}

class LocationRepository(private val context: Context) {
    private val recordsFile = File(context.filesDir, "tracking_records.jsonl")
    private val sensorFile = File(context.filesDir, "sensor_records.jsonl")
    private val witnessFile = File(context.filesDir, "witness_records.jsonl")

    @Synchronized
    fun appendTrackingRecord(record: TrackingRecord) {
        try {
            FileWriter(recordsFile, true).use { writer ->
                writer.append(record.toJsonLine())
            }
        } catch (_: IOException) {
            // 실패하더라도 서비스는 계속 실행
        }
    }

    @Synchronized
    fun appendSensorSample(sampleJson: String) {
        try {
            FileWriter(sensorFile, true).use { writer ->
                writer.append(sampleJson).append('\n')
            }
        } catch (_: IOException) {
            // 실패하더라도 서비스는 계속 실행
        }
    }

    @Synchronized
    fun appendWitnessRecord(record: WitnessRecord) {
        try {
            FileWriter(witnessFile, true).use { writer ->
                writer.append(record.toJsonLine())
            }
        } catch (_: IOException) {
            // 실패하더라도 서비스는 계속 실행
        }
    }

    fun getTrackingFile(): File = recordsFile
    fun getSensorFile(): File = sensorFile
    fun getWitnessFile(): File = witnessFile
}
