package com.bulgyeong.safetyapp.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FallDetectionManager(
    private val context: Context,
    private val onFallDetected: (Double) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val maxSamples = 150
    private val windowQueue = ArrayDeque<SensorSample>()

    private var currentAcc = FloatArray(3)
    private var currentGyro = FloatArray(3)

    private var tfliteInterpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd("fall_detection_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            tfliteInterpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback heuristics will be used if model load fails
        }
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            currentAcc = event.values.clone()
            val accMagnitude = sqrt(
                currentAcc[0] * currentAcc[0] +
                currentAcc[1] * currentAcc[1] +
                currentAcc[2] * currentAcc[2]
            )

            // 1차 트리거 임계치: 3g (3 * 9.81f = 29.43f)
            val threshold3g = 29.43f
            if (accMagnitude >= threshold3g) {
                runFallInference()
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            currentGyro = event.values.clone()
        }

        val sample = SensorSample(
            timestamp = System.currentTimeMillis(),
            ax = currentAcc[0], ay = currentAcc[1], az = currentAcc[2],
            gx = currentGyro[0], gy = currentGyro[1], gz = currentGyro[2]
        )
        windowQueue.addLast(sample)
        if (windowQueue.size > maxSamples) {
            windowQueue.removeFirst()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun runFallInference() {
        if (windowQueue.size < 30) return

        var fallProbability = 0.0

        if (tfliteInterpreter != null) {
            try {
                val inputVal = Array(1) { Array(maxSamples) { FloatArray(6) } }
                val currentQueueList = windowQueue.toList()
                for (i in 0 until maxSamples) {
                    if (i < currentQueueList.size) {
                        val s = currentQueueList[i]
                        inputVal[0][i][0] = s.ax
                        inputVal[0][i][1] = s.ay
                        inputVal[0][i][2] = s.az
                        inputVal[0][i][3] = s.gx
                        inputVal[0][i][4] = s.gy
                        inputVal[0][i][5] = s.gz
                    }
                }
                val outputVal = Array(1) { FloatArray(1) }
                tfliteInterpreter?.run(inputVal, outputVal)
                fallProbability = outputVal[0][0].toDouble()
            } catch (e: Exception) {
                e.printStackTrace()
                fallProbability = runHeuristicInference()
            }
        } else {
            fallProbability = runHeuristicInference()
        }

        if (fallProbability >= 0.85) {
            onFallDetected(fallProbability)
        }
    }

    private fun runHeuristicInference(): Double {
        val samples = windowQueue.toList()
        if (samples.isEmpty()) return 0.0

        var minAcc = Float.MAX_VALUE
        var maxAcc = Float.MIN_VALUE
        var maxGyroEnergy = Float.MIN_VALUE

        for (s in samples) {
            val accMag = sqrt(s.ax * s.ax + s.ay * s.ay + s.az * s.az)
            if (accMag < minAcc) minAcc = accMag
            if (accMag > maxAcc) maxAcc = accMag

            val gyroMag = sqrt(s.gx * s.gx + s.gy * s.gy + s.gz * s.gz)
            if (gyroMag > maxGyroEnergy) maxGyroEnergy = gyroMag
        }

        val g = 9.81f
        var score = 0.0

        if (minAcc < 0.5f * g) {
            score += 0.35
        }
        if (maxAcc > 3.0f * g) {
            score += 0.35
        }
        if (maxGyroEnergy > 2.5f) {
            score += 0.20
        }

        if (samples.size >= 10) {
            val lastSamples = samples.takeLast(10)
            var deltaAcc = 0.0f
            for (i in 0 until lastSamples.size - 1) {
                deltaAcc += sqrt(
                    (lastSamples[i].ax - lastSamples[i+1].ax) * (lastSamples[i].ax - lastSamples[i+1].ax) +
                    (lastSamples[i].ay - lastSamples[i+1].ay) * (lastSamples[i].ay - lastSamples[i+1].ay) +
                    (lastSamples[i].az - lastSamples[i+1].az) * (lastSamples[i].az - lastSamples[i+1].az)
                )
            }
            if (deltaAcc < 0.8f * g) {
                score += 0.10
            }
        }

        return score
    }
}

data class SensorSample(
    val timestamp: Long,
    val ax: Float, val ay: Float, val az: Float,
    val gx: Float, val gy: Float, val gz: Float
)
