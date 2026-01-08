package com.example.field_of_view

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SizeF
import io.flutter.embedding.engine.plugins.FlutterPlugin
import kotlin.math.atan

/** FieldOfViewPlugin */
class FieldOfViewPlugin : FlutterPlugin, FieldOfViewHostApi {

    private lateinit var context: Context

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        FieldOfViewHostApi.setUp(flutterPluginBinding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        FieldOfViewHostApi.setUp(binding.binaryMessenger, null)
    }

    override fun getFieldOfView(): FieldOfViewResponse {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Find the back-facing camera
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: throw FlutterError("CAMERA_UNAVAILABLE", "No back camera available", null)

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        // Get physical sensor size and focal lengths
        val sensorSize: SizeF = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            ?: throw FlutterError("SENSOR_UNAVAILABLE", "Could not get sensor size", null)

        val focalLengths: FloatArray = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?: throw FlutterError("FOCAL_LENGTH_UNAVAILABLE", "Could not get focal lengths", null)

        if (focalLengths.isEmpty()) {
            throw FlutterError("FOCAL_LENGTH_UNAVAILABLE", "No focal lengths available", null)
        }

        // Use the first available focal length (typically the default)
        val focalLength = focalLengths[0]

        // Calculate field of view using: FOV = 2 * atan(sensorSize / (2 * focalLength))
        val horizontalFov = Math.toDegrees(2.0 * atan((sensorSize.width / (2.0 * focalLength)).toDouble()))
        val verticalFov = Math.toDegrees(2.0 * atan((sensorSize.height / (2.0 * focalLength)).toDouble()))

        return FieldOfViewResponse(
            horizontalFov = horizontalFov,
            verticalFov = verticalFov
        )
    }
}
