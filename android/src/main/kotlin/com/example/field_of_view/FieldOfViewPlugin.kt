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
    private var poses: PoseStream? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        FieldOfViewHostApi.setUp(flutterPluginBinding.binaryMessenger, this)
        poses = PoseStream(context).also {
            PoseSamplesStreamHandler.register(flutterPluginBinding.binaryMessenger, it)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        FieldOfViewHostApi.setUp(binding.binaryMessenger, null)
        poses?.stop()
        poses = null
    }

    override fun setHeadingSource(source: HeadingSource) {
        poses?.setSource(source)
    }

    override fun setLocation(latitude: Double, longitude: Double, altitude: Double) {
        poses?.setLocation(latitude, longitude, altitude)
    }

    override fun getStreamFieldOfView(cameraId: String?, width: Long, height: Long): FieldOfViewResponse {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId?.takeIf { it in cameraManager.cameraIdList }
            ?: cameraManager.cameraIdList.firstOrNull { candidate ->
                cameraManager.getCameraCharacteristics(candidate)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
            ?: throw FlutterError("CAMERA_UNAVAILABLE", "No back camera available", null)
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val sensorSize: SizeF = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            ?: throw FlutterError("SENSOR_UNAVAILABLE", "Could not get sensor size", null)
        val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()
            ?: throw FlutterError("FOCAL_LENGTH_UNAVAILABLE", "No focal lengths available", null)
        val pixels = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val active = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val (horizontal, vertical) = Geometry.streamFieldOfView(
            sensorWidthMm = sensorSize.width,
            sensorHeightMm = sensorSize.height,
            pixelArrayWidth = pixels?.width ?: 0,
            pixelArrayHeight = pixels?.height ?: 0,
            activeWidth = active?.width() ?: 0,
            activeHeight = active?.height() ?: 0,
            focalLengthMm = focalLength,
            streamWidth = width.toInt(),
            streamHeight = height.toInt(),
        )
        return FieldOfViewResponse(horizontalFov = horizontal, verticalFov = vertical)
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
