import CoreMotion
import Flutter
import Foundation

/// The camera's direction from Core Motion's device motion.
///
/// `fused` uses the true-north reference frame (the magnetic one when the
/// device cannot place itself); `gyro` the arbitrary one, which never looks
/// at the compass: its bearing is from wherever the device pointed and only
/// drifts.
final class PoseStream: PoseSamplesStreamHandler {
  private let motion = CMMotionManager()
  private var sink: PigeonEventSink<PoseSample>?
  private var wanted = HeadingSource.fused

  override func onListen(withArguments arguments: Any?, sink: PigeonEventSink<PoseSample>) {
    self.sink = sink
    start()
  }

  override func onCancel(withArguments arguments: Any?) {
    motion.stopDeviceMotionUpdates()
    sink = nil
  }

  func setSource(_ source: HeadingSource) {
    guard source != wanted else { return }
    wanted = source
    if sink != nil { start() }
  }

  private func start() {
    motion.stopDeviceMotionUpdates()
    guard motion.isDeviceMotionAvailable else {
      sink?.error(code: "SENSOR_UNAVAILABLE", message: "Device motion is not available", details: nil)
      return
    }
    let available = CMMotionManager.availableAttitudeReferenceFrames()
    let source = wanted
    let frame: CMAttitudeReferenceFrame
    var trueNorth = false
    if source == .gyro {
      frame = .xArbitraryZVertical
    } else if available.contains(.xTrueNorthZVertical) {
      frame = .xTrueNorthZVertical
      trueNorth = true
    } else {
      frame = .xMagneticNorthZVertical
    }
    motion.deviceMotionUpdateInterval = 1.0 / 60.0
    // Lets iOS show its own compass calibration prompt when it needs one.
    motion.showsDeviceMovementDisplay = source == .fused
    motion.startDeviceMotionUpdates(using: frame, to: .main) { [weak self] data, _ in
      guard let self = self, let data = data, let sink = self.sink else { return }
      let (azimuth, pitch, roll) = PoseStream.cameraDirection(data.attitude.rotationMatrix)
      var accuracy = -1.0
      if source == .fused {
        switch data.magneticField.accuracy {
        case .high: accuracy = 5
        case .medium: accuracy = 15
        case .low: accuracy = 30
        default: accuracy = -1
        }
      }
      sink.success(
        PoseSample(
          azimuth: azimuth,
          pitch: pitch,
          roll: roll,
          accuracy: accuracy,
          source: source,
          trueNorth: source == .fused && trueNorth,
          timestampMs: Int64(data.timestamp * 1000)))
    }
  }

  /// Where the back camera points, from the attitude's rotation matrix.
  ///
  /// The matrix takes a vector of the reference frame (x north, y west,
  /// z up) into the device's, so its rows are the device's axes in the
  /// reference frame. The camera looks along the device's −z: minus the
  /// third row. Returns the bearing (degrees clockwise from the frame's x,
  /// 0–360), the pitch above the horizontal, and the roll about the camera
  /// axis (positive with the right edge higher than the left).
  static func cameraDirection(_ m: CMRotationMatrix) -> (Double, Double, Double) {
    let north = -m.m31
    let east = m.m32
    let up = -m.m33
    var azimuth = atan2(east, north) * 180 / .pi
    if azimuth < 0 { azimuth += 360 }
    let pitch = asin(max(-1, min(1, up))) * 180 / .pi
    // How far the device's x and y axes lean up.
    let roll = atan2(m.m13, m.m23) * 180 / .pi
    return (azimuth, pitch, roll)
  }
}
