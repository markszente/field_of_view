# field_of_view

A Flutter plugin for camera overlays on iOS and Android: the angle the camera sees, and where it points.

## Features

- The horizontal and vertical field of view of the back camera, for the whole sensor or for the stream a running camera delivers
- Where the camera points: bearing, pitch and roll, as a stream, from the compass-fused or the gyroscope-only orientation
- No permissions required
- Strongly-typed API using Pigeon
- Supports iOS and Android

## Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  field_of_view: ^0.0.1
```

## Usage

```dart
import 'package:field_of_view/field_of_view.dart';

final fov = FieldOfView();

try {
  final response = await fov.getFieldOfView();
  print('Horizontal FOV: ${response.horizontalFov}°');
  print('Vertical FOV: ${response.verticalFov}°');
} on PlatformException catch (e) {
  print('Error: ${e.message}');
}
```

## Response

The `FieldOfViewResponse` contains:

| Property | Type | Description |
|----------|------|-------------|
| `horizontalFov` | `double` | Horizontal field of view in degrees |
| `verticalFov` | `double` | Vertical field of view in degrees |

> **Note:** The FOV values represent the camera sensor's native orientation (landscape), so horizontal FOV will typically be larger than vertical FOV, regardless of device orientation.

## Placing things on a camera preview

The whole sensor's angle is rarely what a preview shows. A 16:9 stream is a crop of a 4:3 sensor, and on iOS the angle depends on the format the capture session chose. Ask for the stream's own angle once the camera runs (with the `camera` package, `CameraDescription.name` is the id and `CameraValue.previewSize` the size):

```dart
final angles = await fov.getStreamFieldOfView(
  cameraId: description.name,
  width: 1280,
  height: 720,
);
```

Both angles are in the sensor's landscape orientation, whichever way the size is given.

## Where the camera points

```dart
await fov.setLocation(latitude: 47.5, longitude: 19.0); // Android: for true north
final subscription = fov.poses.listen((pose) {
  // pose.azimuth: bearing of the camera's axis, degrees clockwise, 0–360
  // pose.pitch:   degrees above the horizontal
  // pose.roll:    degrees about the camera's axis, positive with the right edge up
  // pose.accuracy: the platform's estimate of the heading error, or negative
});
```

| `HeadingSource` | Azimuth is from | Error |
|---|---|---|
| `fused` (default) | true north (`pose.trueNorth`; magnetic north until the position is known) | the compass's: often 5–15°, more near steel |
| `gyro` | wherever the device pointed when it was chosen | none from the compass; a slow drift |

`setHeadingSource(HeadingSource.gyro)` is for holding an alignment the user made by hand: take the difference between the last fused azimuth and the first gyro one as the offset. Every sample carries the source it was taken with.

- **iOS**: `CMMotionManager` device motion in the `xTrueNorthZVertical` frame (`xArbitraryZVertical` for `gyro`). Accuracy is the calibrated magnetic field's (5 / 15 / 30°).
- **Android**: Google Play services' fused orientation provider (`play-services-location` 21.2+), which calibrates the compass itself and behaves the same on every phone; accuracy is its heading error. Without Play services: `TYPE_ROTATION_VECTOR` plus the declination from `GeomagneticField`. That sensor is each manufacturer's own, and on a Redmi Note 11 its north stood up to 95° from the magnetometer's. `TYPE_GAME_ROTATION_VECTOR` for `gyro`.

## Error handling

The plugin throws `PlatformException` with the following error codes:

| Code | Description |
|------|-------------|
| `CAMERA_UNAVAILABLE` | No back camera found on the device |
| `SENSOR_UNAVAILABLE` | Could not retrieve sensor size information |
| `FOCAL_LENGTH_UNAVAILABLE` | Could not retrieve focal length information |

## How it works

- **iOS**: Uses [`AVCaptureDevice.Format.geometricDistortionCorrectedVideoFieldOfView`](https://developer.apple.com/documentation/avfoundation/avcapturedevice/format/3194616-geometricdistortioncorrectedvide) for horizontal FOV (corrected for lens distortion) and calculates vertical FOV from the aspect ratio. Requires iOS 13.0+.

- **Android**: Uses Camera2 API to read [`SENSOR_INFO_PHYSICAL_SIZE`](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#SENSOR_INFO_PHYSICAL_SIZE) and [`LENS_INFO_AVAILABLE_FOCAL_LENGTHS`](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#LENS_INFO_AVAILABLE_FOCAL_LENGTHS) from `CameraCharacteristics`, then calculates FOV using the pinhole camera model:

```
FOV = 2 × atan(sensorSize / (2 × focalLength))
```

