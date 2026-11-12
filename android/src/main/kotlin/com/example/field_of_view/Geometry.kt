package com.example.field_of_view

import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2

/** The angles a stream sees, and where a rotation matrix points the camera. Pure maths. */
object Geometry {

    /**
     * The horizontal and vertical field of view, in degrees, of a stream of
     * [streamWidth] × [streamHeight] pixels.
     *
     * The sensor's physical size belongs to its whole pixel array; the camera
     * reads the active array out of it, and a stream of another aspect ratio
     * is a centred crop of that. So the full-sensor angle is too wide for
     * every stream but one of the sensor's own shape.
     */
    fun streamFieldOfView(
        sensorWidthMm: Float,
        sensorHeightMm: Float,
        pixelArrayWidth: Int,
        pixelArrayHeight: Int,
        activeWidth: Int,
        activeHeight: Int,
        focalLengthMm: Float,
        streamWidth: Int,
        streamHeight: Int,
    ): Pair<Double, Double> {
        var activeW = sensorWidthMm.toDouble()
        var activeH = sensorHeightMm.toDouble()
        if (pixelArrayWidth > 0 && pixelArrayHeight > 0 && activeWidth > 0 && activeHeight > 0) {
            activeW *= activeWidth.toDouble() / pixelArrayWidth
            activeH *= activeHeight.toDouble() / pixelArrayHeight
        }
        // The stream in the sensor's own landscape orientation.
        val long = maxOf(streamWidth, streamHeight).toDouble()
        val short = minOf(streamWidth, streamHeight).toDouble()
        val streamAspect = if (short > 0) long / short else activeW / activeH
        val activeAspect = activeW / activeH
        val usedW: Double
        val usedH: Double
        if (streamAspect >= activeAspect) {
            usedW = activeW
            usedH = activeW / streamAspect
        } else {
            usedH = activeH
            usedW = activeH * streamAspect
        }
        return Pair(
            Math.toDegrees(2.0 * atan(usedW / (2.0 * focalLengthMm))),
            Math.toDegrees(2.0 * atan(usedH / (2.0 * focalLengthMm))),
        )
    }

    /**
     * Where the back camera points, from the 3×3 row-major matrix
     * `SensorManager.getRotationMatrixFromVector` gives (device → world;
     * world x east, y north, z up): the bearing of its axis (degrees
     * clockwise from the world's north, 0–360), its pitch above the
     * horizontal, and the device's roll about it (positive with the right
     * edge higher than the left).
     */
    fun cameraDirection(r: FloatArray): Triple<Double, Double, Double> {
        // The camera looks along the device's −z: minus the third column.
        val east = -r[2].toDouble()
        val north = -r[5].toDouble()
        val up = -r[8].toDouble()
        var azimuth = Math.toDegrees(atan2(east, north))
        if (azimuth < 0) azimuth += 360.0
        val pitch = Math.toDegrees(asin(up.coerceIn(-1.0, 1.0)))
        // How far the device's x and y axes lean up.
        val roll = Math.toDegrees(atan2(r[6].toDouble(), r[7].toDouble()))
        return Triple(azimuth, pitch, roll)
    }
}
