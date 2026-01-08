import 'package:field_of_view/src/messages.g.dart';

export 'package:field_of_view/src/messages.g.dart' show FieldOfViewResponse;

/// A Flutter plugin to get the camera's field of view (FOV) on iOS and Android.
///
/// This plugin retrieves the horizontal and vertical field of view angles
/// from the device's back-facing camera sensor without requiring camera
/// permissions at runtime.
///
/// Example usage:
/// ```dart
/// final fov = FieldOfView();
/// final response = await fov.getFieldOfView();
/// print('Horizontal FOV: ${response.horizontalFov}°');
/// print('Vertical FOV: ${response.verticalFov}°');
/// ```
class FieldOfView {
  final FieldOfViewHostApi _api = FieldOfViewHostApi();

  /// Gets the field of view from the device's back-facing camera.
  ///
  /// Returns a [FieldOfViewResponse] containing:
  /// - [FieldOfViewResponse.horizontalFov]: The horizontal field of view in degrees
  /// - [FieldOfViewResponse.verticalFov]: The vertical field of view in degrees
  ///
  /// The FOV values represent the camera sensor's native orientation (landscape),
  /// so horizontal FOV will typically be larger than vertical FOV.
  ///
  /// Throws a [PlatformException] if:
  /// - No back camera is available (`CAMERA_UNAVAILABLE`)
  /// - Sensor information cannot be retrieved (`SENSOR_UNAVAILABLE`)
  /// - Focal length information is unavailable (`FOCAL_LENGTH_UNAVAILABLE`)
  Future<FieldOfViewResponse> getFieldOfView() => _api.getFieldOfView();
}
