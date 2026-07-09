package com.bulgyeong.safetyapp.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PdrNavigator(
    context: Context,
    private val callback: PdrCallback
) : SensorEventListener {

    interface PdrCallback {
        fun onPdrPositionUpdated(position: PdrPosition)
        fun onPdrSensorSample(timestamp: Long, json: String)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var lastStepTime = 0L
    private var stepCount = 0
    private var headingDegrees = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var offsetLat = 0.0
    private var offsetLon = 0.0
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    private var lastAccel = FloatArray(3)
    private var lastMagnet = FloatArray(3)

    fun start() {
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun reset() {
        currentX = 0f
        currentY = 0f
        stepCount = 0
        lastStepTime = 0L
    }

    fun getCurrentPosition(): PdrPosition {
        return PdrPosition(
            timestamp = System.currentTimeMillis(),
            xMeters = currentX,
            yMeters = currentY,
            headingDegrees = headingDegrees,
            stepCount = stepCount
        )
    }

    fun applyAnchorCorrection(anchorLat: Double, anchorLon: Double) {
        val currentGeo = metersToLatLon(currentX, currentY, anchorLat)
        offsetLat = anchorLat - currentGeo.first
        offsetLon = anchorLon - currentGeo.second
    }

    fun getAbsoluteGeoPosition(): Pair<Double, Double> {
        val geo = metersToLatLon(currentX, currentY, offsetLat)
        return Pair(offsetLat + geo.first, offsetLon + geo.second)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = System.currentTimeMillis()
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                lastAccel = event.values.clone()
                val magnitude = sqrt(ax * ax + ay * ay + az * az)
                detectStep(timestamp, magnitude)
                callback.onPdrSensorSample(timestamp, "{\"sensor\":\"accelerometer\",\"ax\":$ax,\"ay\":$ay,\"az\":$az,\"magnitude\":$magnitude}")
            }
            Sensor.TYPE_GYROSCOPE -> {
                callback.onPdrSensorSample(timestamp, "{\"sensor\":\"gyroscope\",\"gx\":${event.values[0]},\"gy\":${event.values[1]},\"gz\":${event.values[2]}}")
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                headingDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (headingDegrees < 0) headingDegrees += 360f
                callback.onPdrSensorSample(timestamp, "{\"sensor\":\"rotation_vector\",\"heading\":$headingDegrees}")
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagnet = event.values.clone()
                computeOrientationFromAccelMagnet()
                callback.onPdrSensorSample(timestamp, "{\"sensor\":\"magnetometer\",\"mx\":${event.values[0]},\"my\":${event.values[1]},\"mz\":${event.values[2]}}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 무시
    }

    private fun computeOrientationFromAccelMagnet() {
        if (lastAccel.any { it == 0f } || lastMagnet.any { it == 0f }) return
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotation, inclination, lastAccel, lastMagnet)) {
            SensorManager.getOrientation(rotation, orientation)
            headingDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (headingDegrees < 0) headingDegrees += 360f
        }
    }

    private fun detectStep(timestamp: Long, magnitude: Float) {
        val stepThreshold = 11.0f
        val minStepInterval = 400
        if (magnitude > stepThreshold && timestamp - lastStepTime > minStepInterval) {
            lastStepTime = timestamp
            stepCount += 1
            val stepLength = estimateStepLength(magnitude)
            val rad = Math.toRadians(headingDegrees.toDouble()).toFloat()
            currentX += stepLength * cos(rad)
            currentY += stepLength * sin(rad)
            val position = getCurrentPosition()
            callback.onPdrPositionUpdated(position)
        }
    }

    private fun estimateStepLength(magnitude: Float): Float {
        return 0.6f + ((magnitude - 9.8f).coerceIn(0f, 5f) * 0.05f)
    }

    private fun metersToLatLon(xMeters: Float, yMeters: Float, originLat: Double): Pair<Double, Double> {
        val latDelta = xMeters / 111320f
        val lonDelta = yMeters / (111320f * kotlin.math.cos(Math.toRadians(originLat)).toFloat())
        return Pair(latDelta.toDouble(), lonDelta.toDouble())
    }
}
