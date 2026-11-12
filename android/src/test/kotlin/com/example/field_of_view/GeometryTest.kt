package com.example.field_of_view

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

internal class GeometryTest {
    private fun close(a: Double, b: Double, tolerance: Double = 0.05) =
        assertTrue(abs(a - b) <= tolerance, "$a is not within $tolerance of $b")

    // A 4:3 sensor, 6.4 × 4.8 mm behind a 4.8 mm lens, read out whole.
    private fun fov(streamW: Int, streamH: Int, activeW: Int = 4000, activeH: Int = 3000) =
        Geometry.streamFieldOfView(6.4f, 4.8f, 4000, 3000, activeW, activeH, 4.8f, streamW, streamH)

    @Test
    fun aStreamOfTheSensorsShapeSeesTheWholeSensor() {
        val (h, v) = fov(1440, 1080)
        close(h, 67.38)
        close(v, 53.13)
    }

    @Test
    fun aWiderStreamIsCroppedTopAndBottom() {
        val (h, v) = fov(1920, 1080)
        close(h, 67.38)
        close(v, 41.11)
    }

    @Test
    fun aPortraitSizeIsReadAsItsLandscapeStream() {
        val (h, v) = fov(1080, 1920)
        close(h, 67.38)
        close(v, 41.11)
    }

    @Test
    fun theActiveArrayNarrowsTheAngle() {
        val (h, _) = fov(1440, 1080, activeW = 3600, activeH = 2700)
        close(h, 61.93)
    }

    @Test
    fun uprightFacingNorth() {
        // Device x east, y up, z south (out of the screen, towards the user).
        val (azimuth, pitch, roll) = Geometry.cameraDirection(
            floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        )
        close(azimuth, 0.0)
        close(pitch, 0.0)
        close(roll, 0.0)
    }

    @Test
    fun uprightFacingEast() {
        // Device x south, y up, z west.
        val (azimuth, pitch, _) = Geometry.cameraDirection(
            floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f)
        )
        close(azimuth, 90.0)
        close(pitch, 0.0)
    }

    @Test
    fun lyingFlatTheCameraLooksDown() {
        val (_, pitch, _) = Geometry.cameraDirection(
            floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        )
        close(pitch, -90.0)
    }

    @Test
    fun rightEdgeUpIsAPositiveRoll() {
        // Facing north, turned 90° so the right edge points up: x up, y west.
        val (azimuth, _, roll) = Geometry.cameraDirection(
            floatArrayOf(0f, -1f, 0f, 0f, 0f, -1f, 1f, 0f, 0f)
        )
        close(azimuth, 0.0)
        close(roll, 90.0)
    }
}
