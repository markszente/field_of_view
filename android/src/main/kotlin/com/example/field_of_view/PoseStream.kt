package com.example.field_of_view

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * The camera's direction from the rotation-vector sensors.
 *
 * [HeadingSource.FUSED] reads TYPE_ROTATION_VECTOR (gyroscope, accelerometer
 * and compass) and adds the magnetic declination, so the bearing is from
 * true north once [setLocation] was called. [HeadingSource.GYRO] reads
 * TYPE_GAME_ROTATION_VECTOR, which never looks at the compass: its bearing
 * is from an arbitrary direction and only drifts.
 */
class PoseStream(context: Context) : PoseSamplesStreamHandler(), SensorEventListener {

    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sink: PigeonEventSink<PoseSample>? = null
    private var wanted = HeadingSource.FUSED
    private var listening: HeadingSource? = null
    private var declination: Float? = null
    private var compassStatus = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private val matrix = FloatArray(9)
    private val vector = FloatArray(4)

    override fun onListen(p0: Any?, sink: PigeonEventSink<PoseSample>) {
        this.sink = sink
        start()
    }

    override fun onCancel(p0: Any?) {
        stop()
        sink = null
    }

    fun setSource(source: HeadingSource) {
        if (source == wanted) return
        wanted = source
        if (sink != null) start()
    }

    fun setLocation(latitude: Double, longitude: Double, altitude: Double) {
        declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination
    }

    fun stop() {
        sensors.unregisterListener(this)
        listening = null
    }

    private fun start() {
        stop()
        // A phone without the gyro-only sensor keeps the fused one.
        val gyro = if (wanted == HeadingSource.GYRO) {
            sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        } else {
            null
        }
        val sensor = gyro ?: sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            sink?.error("SENSOR_UNAVAILABLE", "No rotation vector sensor", null)
            return
        }
        listening = if (gyro != null) HeadingSource.GYRO else HeadingSource.FUSED
        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val source = listening ?: return
        // Some devices hand over five values and refuse more than four back.
        val n = minOf(event.values.size, 4)
        System.arraycopy(event.values, 0, vector, 0, n)
        SensorManager.getRotationMatrixFromVector(matrix, if (n == 4) vector else vector.copyOf(n))
        val (rawAzimuth, pitch, roll) = Geometry.cameraDirection(matrix)

        var azimuth = rawAzimuth
        var accuracy = -1.0
        var trueNorth = false
        if (source == HeadingSource.FUSED) {
            declination?.let {
                azimuth = ((azimuth + it) % 360.0 + 360.0) % 360.0
                trueNorth = true
            }
            // The sensor's own estimate of the heading error, in radians,
            // where it gives one; else what the compass's status says.
            accuracy = if (event.values.size >= 5 && event.values[4] >= 0f) {
                Math.toDegrees(event.values[4].toDouble())
            } else {
                when (compassStatus) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 5.0
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 15.0
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 30.0
                    else -> 45.0
                }
            }
        }
        sink?.success(
            PoseSample(
                azimuth = azimuth,
                pitch = pitch,
                roll = roll,
                accuracy = accuracy,
                source = source,
                trueNorth = trueNorth,
                timestampMs = event.timestamp / 1_000_000,
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_ROTATION_VECTOR) compassStatus = accuracy
    }
}
