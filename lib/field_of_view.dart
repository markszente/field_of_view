import 'package:field_of_view/src/messages.g.dart';

export 'package:field_of_view/src/messages.g.dart'
    show FieldOfViewResponse, HeadingSource, PoseSample;

/// What an augmented-reality overlay needs from the device besides the
/// camera picture: the angle the camera sees, and where it points.
///
/// None of it needs a permission.
///
/// ```dart
/// final fov = FieldOfView();
/// final angles = await fov.getStreamFieldOfView(width: 1280, height: 720);
/// fov.poses.listen((pose) => print('${pose.azimuth}° ${pose.pitch}°'));
/// ```
class FieldOfView {
  final FieldOfViewHostApi _api = FieldOfViewHostApi();

  /// The field of view of the back camera's whole sensor in its default
  /// format, in the sensor's native (landscape) orientation.
  ///
  /// A running camera's stream usually sees less: use
  /// [getStreamFieldOfView] to place things on a preview.
  ///
  /// Throws a [PlatformException] if:
  /// - No back camera is available (`CAMERA_UNAVAILABLE`)
  /// - Sensor information cannot be retrieved (`SENSOR_UNAVAILABLE`)
  /// - Focal length information is unavailable (`FOCAL_LENGTH_UNAVAILABLE`)
  Future<FieldOfViewResponse> getFieldOfView() => _api.getFieldOfView();

  /// The field of view of the stream a running camera delivers, in the
  /// sensor's native (landscape) orientation whichever way [width] and
  /// [height] are given.
  ///
  /// [cameraId] is the platform's id of the camera — the `camera` package's
  /// `CameraDescription.name` — and the back wide camera when left out.
  /// Call it after the camera has started: on iOS the angle depends on the
  /// format the session chose, on Android on how the stream's shape crops
  /// the sensor.
  Future<FieldOfViewResponse> getStreamFieldOfView({
    String? cameraId,
    required int width,
    required int height,
  }) => _api.getStreamFieldOfView(cameraId, width, height);

  /// Where the back camera points, about 50–60 times a second while
  /// listened to. One listener at a time.
  Stream<PoseSample> get poses => poseSamples();

  /// What [poses]' azimuth is measured against: the compass
  /// ([HeadingSource.fused], the default) or the gyroscope alone
  /// ([HeadingSource.gyro]). Every sample says which it was taken with; the
  /// first of the new source may arrive a few samples after the call.
  Future<void> setHeadingSource(HeadingSource source) =>
      _api.setHeadingSource(source);

  /// Where the device is, so a fused azimuth can be from true north. Needed
  /// on Android (until it is called, samples have
  /// [PoseSample.trueNorth] false); iOS uses its own location.
  Future<void> setLocation({
    required double latitude,
    required double longitude,
    double altitude = 0,
  }) => _api.setLocation(latitude, longitude, altitude);
}
