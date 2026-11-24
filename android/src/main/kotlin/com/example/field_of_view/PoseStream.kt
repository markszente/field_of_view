package com.example.field_of_view

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationListener
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.FusedOrientationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executor

/**
 * The camera's direction.
 *
 * [HeadingSource.FUSED] reads Google Play services' fused orientation
 * provider, which calibrates the compass itself and is the same on every
 * phone. The platform's own TYPE_ROTATION_VECTOR is each manufacturer's:
 * on a Redmi Note 11 (2026-11-24) its north stood 95° from the
 * magnetometer's with the phone lying flat and 19° with it upright, while
 * Google Maps on the same phone agreed with the magnetometer. It is read
 * only where Play services cannot be (no Play services, or the request
 * fails), with the magnetic declination added once [setLocation] was
 * called.
 *
 * [HeadingSource.GYRO] reads TYPE_GAME_ROTATION_VECTOR, which never looks
 * at the compass: its bearing is from an arbitrary direction and only
 * drifts.
 */
class PoseStream(private val context: Context) :
    PoseSamplesStreamHandler(), SensorEventListener, DeviceOrientationListener {

    private val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sink: PigeonEventSink<PoseSample>? = null
    private var wanted = HeadingSource.FUSED
    private var listening: HeadingSource? = null
    private var declination: Float? = null
    private var compassStatus = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private val matrix = FloatArray(9)
    private val vector = FloatArray(4)

    private var orientation: FusedOrientationProviderClient? = null

    // Counts the starts, so that a request that fails after the stream has
    // moved on changes nothing.
    private var generation = 0

    // The plugin's callbacks arrive on the thread the stream was listened
    // to on; Play services' are sent there too.
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val executor = Executor { handler.post(it) }

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
        generation++
        sensors.unregisterListener(this)
        orientation?.removeOrientationUpdates(this)
        orientation = null
        listening = null
    }

    private fun start() {
        stop()
        if (wanted == HeadingSource.GYRO) {
            // A phone without the gyro-only sensor keeps the fused source.
            val gyro = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            if (gyro != null) {
                listening = HeadingSource.GYRO
                sensors.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
                return
            }
        }
        val started = generation
        try {
            val client = LocationServices.getFusedOrientationProviderClient(context)
            val request = DeviceOrientationRequest.Builder(
                DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT
            ).build()
            orientation = client
            listening = HeadingSource.FUSED
            client.requestOrientationUpdates(request, executor, this).addOnFailureListener {
                Log.w(TAG, "fused orientation unavailable, reading the rotation vector", it)
                if (generation == started) {
                    orientation = null
                    startRotationVector()
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "fused orientation unavailable, reading the rotation vector", e)
            orientation = null
            startRotationVector()
        }
    }

    private fun startRotationVector() {
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            listening = null
            sink?.error("SENSOR_UNAVAILABLE", "No rotation vector sensor", null)
            return
        }
        listening = HeadingSource.FUSED
        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Play services' orientation: device → east-north-up, as the rotation vector's. */
    override fun onDeviceOrientationChanged(sample: DeviceOrientation) {
        if (orientation == null || listening != HeadingSource.FUSED) return
        SensorManager.getRotationMatrixFromVector(matrix, sample.attitude)
        val (azimuth, pitch, roll) = Geometry.cameraDirection(matrix)
        sink?.success(
            PoseSample(
                azimuth = azimuth,
                pitch = pitch,
                roll = roll,
                accuracy = sample.headingErrorDegrees.toDouble(),
                source = HeadingSource.FUSED,
                // Its north is the true one once it knows the declination,
                // which it takes from the device's location itself.
                trueNorth = true,
                timestampMs = sample.elapsedRealtimeNs / 1_000_000,
            )
        )
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

    private companion object {
        const val TAG = "FieldOfView"
    }
}
