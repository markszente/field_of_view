## 0.1.0

* `getStreamFieldOfView`: the angle of the stream a running camera delivers (the sensor's active array and the stream's crop on Android, the session's format on iOS).
* `poses`: the camera's bearing, pitch and roll as a stream, with `setHeadingSource` (compass-fused or gyroscope only) and `setLocation` (true north on Android).
* Android reads the compass-fused pose from Play services' fused orientation provider, and the platform's rotation vector only without it.
* Removed the unused iOS `Helper`.

## 0.0.1

* Initial release.
