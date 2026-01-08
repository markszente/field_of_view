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

/// Host API for getting camera field of view information.
@HostApi()
abstract class FieldOfViewHostApi {
  /// Gets the field of view from the device's back camera.
  FieldOfViewResponse getFieldOfView();
}
