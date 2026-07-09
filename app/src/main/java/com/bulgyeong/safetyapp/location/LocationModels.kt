package com.bulgyeong.safetyapp.location

import android.location.Location

enum class TrackingState {
    GPS_ACTIVE,
    BLE_UWB_ACTIVE,
    PDR_ACTIVE
}

data class TrackingRecord(
    val timestamp: Long,
    val source: String,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracy: Float?,
    val relativeX: Float?,
    val relativeY: Float?,
    val extra: String?
) {
    fun toJsonLine(): String {
        val lat = latitude?.toString() ?: "null"
        val lon = longitude?.toString() ?: "null"
        val alt = altitude?.toString() ?: "null"
        val acc = accuracy?.toString() ?: "null"
        val x = relativeX?.toString() ?: "null"
        val y = relativeY?.toString() ?: "null"
        val safeExtra = extra?.replace("\n", " ")?.replace("\r", " ") ?: ""
        return "{\"timestamp\":$timestamp,\"source\":\"$source\",\"latitude\":$lat,\"longitude\":$lon,\"altitude\":$alt,\"accuracy\":$acc,\"relativeX\":$x,\"relativeY\":$y,\"extra\":\"$safeExtra\"}\n"
    }
}

data class BeaconAnchor(
    val anchorId: String,
    val macAddress: String,
    val latitude: Double,
    val longitude: Double
)

data class BeaconReading(
    val timestamp: Long,
    val anchorId: String?,
    val macAddress: String,
    val rssi: Int,
    val estimatedDistanceMeters: Float,
    val latitude: Double?,
    val longitude: Double?
)

data class PdrPosition(
    val timestamp: Long,
    val xMeters: Float,
    val yMeters: Float,
    val headingDegrees: Float,
    val stepCount: Int
)

fun Location.toTrackingRecord(source: String, relativeX: Float? = null, relativeY: Float? = null, extra: String? = null): TrackingRecord {
    return TrackingRecord(
        timestamp = System.currentTimeMillis(),
        source = source,
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        accuracy = if (hasAccuracy()) accuracy else null,
        relativeX = relativeX,
        relativeY = relativeY,
        extra = extra
    )
}
