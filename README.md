# field_of_view

A Flutter plugin to get the camera's field of view (FOV) on iOS and Android.

## Features

- Get horizontal and vertical field of view angles from the back camera
- No camera permissions required
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

## Platform-specific notes

### iOS

Add the camera usage description to your app's `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>This app needs camera access to determine the field of view.</string>
```

The plugin uses `AVCaptureDevice` to query camera characteristics. No runtime permission prompt is shown, but the usage description key must be present.

### Android

No permissions required. The plugin uses Camera2 API's `CameraCharacteristics` to read static sensor metadata.

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

