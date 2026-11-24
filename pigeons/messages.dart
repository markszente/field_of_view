import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartPackageName: 'field_of_view',
    dartOut: 'lib/src/messages.g.dart',
    swiftOut: 'ios/field_of_view/Sources/field_of_view/Messages.g.swift',
    kotlinOut:
        'android/src/main/kotlin/com/example/field_of_view/Messages.g.kt',
    kotlinOptions: KotlinOptions(package: 'com.example.field_of_view'),
  ),
)
/// Response containing field of view information from the camera.
class FieldOfViewResponse {
  FieldOfViewResponse({required this.horizontalFov, required this.verticalFov});

  /// Horizontal field of view in degrees.
  final double horizontalFov;

  /// Vertical field of view in degrees.
  final double verticalFov;
}

/// What the heading of a [PoseSample] is measured against.
enum HeadingSource {
  /// Gyroscope, accelerometer and compass fused: degrees from true north
  /// (see [PoseSample.trueNorth]), as wrong as the compass is.
  fused,

  /// Gyroscope and accelerometer only: degrees from wherever the device
  /// pointed when the source was chosen. No compass error, a slow drift.
  gyro,
}

/// Where the back camera points.
class PoseSample {
  PoseSample({
    required this.azimuth,
    required this.pitch,
    required this.roll,
    required this.accuracy,
    required this.source,
    required this.trueNorth,
    required this.timestampMs,
  });

  /// The bearing of the camera's axis, degrees clockwise, 0–360. From north
  /// with [HeadingSource.fused]; from an arbitrary direction with
  /// [HeadingSource.gyro].
  final double azimuth;

  /// The camera axis above the horizontal, degrees (−90 … 90).
  final double pitch;

  /// The rotation of the device about the camera's axis, degrees: 0 with
  /// the device upright in portrait, positive when its right edge is
  /// higher than its left.
  final double roll;

  /// The platform's estimate of the heading error, degrees; negative when
  /// it has none.
  final double accuracy;

  final HeadingSource source;

  /// Whether a fused azimuth is from true north. False when the platform
  /// could only give magnetic north (no position to work out the
  /// declination from).
  final bool trueNorth;

  final int timestampMs;
}

/// Host API for getting camera field of view information.
@HostApi()
abstract class FieldOfViewHostApi {
  /// Gets the field of view from the device's back camera, for the full
  /// sensor in its default format.
  FieldOfViewResponse getFieldOfView();

  /// The field of view of the stream a running camera delivers.
  ///
  /// [cameraId] is the platform's id of the camera (the `camera` package's
  /// `CameraDescription.name`); null picks the back wide camera. [width]
  /// and [height] are the stream's size in pixels, in the sensor's own
  /// (landscape) orientation. Call it after the camera has started: the
  /// angles depend on the format the session chose.
  FieldOfViewResponse getStreamFieldOfView(
    String? cameraId,
    int width,
    int height,
  );

  /// Chooses what the pose stream's azimuth is measured against.
  void setHeadingSource(HeadingSource source);

  /// Where the device is, for the magnetic declination (Android; iOS works
  /// it out from its own location).
  void setLocation(double latitude, double longitude, double altitude);
}

@EventChannelApi()
abstract class PoseEvents {
  /// The camera's direction, at the display's rate while listened to.
  PoseSample poseSamples();
}
