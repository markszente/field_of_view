import AVFoundation
import Flutter
import UIKit

public class FieldOfViewPlugin: NSObject, FlutterPlugin, FieldOfViewHostApi {
  private let poses = PoseStream()

  public static func register(with registrar: FlutterPluginRegistrar) {
    let instance = FieldOfViewPlugin()
    FieldOfViewHostApiSetup.setUp(binaryMessenger: registrar.messenger(), api: instance)
    PoseSamplesStreamHandler.register(with: registrar.messenger(), streamHandler: instance.poses)
  }

  private func backCamera(_ cameraId: String?) throws -> AVCaptureDevice {
    if let id = cameraId, let device = AVCaptureDevice(uniqueID: id) {
      return device
    }
    guard
      let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back)
    else {
      throw PigeonError(code: "CAMERA_UNAVAILABLE", message: "No back camera available", details: nil)
    }
    return device
  }

  func getFieldOfView() throws -> FieldOfViewResponse {
    let format = try backCamera(nil).activeFormat
    // Use geometric distortion corrected FOV for more accurate results (iOS 13+)
    let horizontalFov = Double(format.geometricDistortionCorrectedVideoFieldOfView)
    let dimensions = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
    let aspectRatio = Double(dimensions.width) / Double(dimensions.height)
    let verticalFov = 2.0 * atan(tan(horizontalFov * .pi / 360.0) / aspectRatio) * 180.0 / .pi
    return FieldOfViewResponse(horizontalFov: horizontalFov, verticalFov: verticalFov)
  }

  /// The angles of the stream a running session delivers. The capture
  /// device is shared by every session on it, so once another plugin has
  /// started the camera its `activeFormat` here is the one that session
  /// chose. A stream of another shape than the format is a centred crop.
  func getStreamFieldOfView(cameraId: String?, width: Int64, height: Int64) throws
    -> FieldOfViewResponse
  {
    let device = try backCamera(cameraId)
    let format = device.activeFormat
    let fullHorizontal = Double(
      device.isGeometricDistortionCorrectionEnabled
        ? format.geometricDistortionCorrectedVideoFieldOfView
        : format.videoFieldOfView)
    let dimensions = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
    let formatAspect = Double(dimensions.width) / Double(dimensions.height)
    let long = Double(max(width, height))
    let short = Double(min(width, height))
    let streamAspect = short > 0 ? long / short : formatAspect

    let tanHorizontalFull = tan(fullHorizontal * .pi / 360.0)
    let tanVerticalFull = tanHorizontalFull / formatAspect
    let tanHorizontal: Double
    let tanVertical: Double
    if streamAspect >= formatAspect {
      tanHorizontal = tanHorizontalFull
      tanVertical = tanHorizontalFull / streamAspect
    } else {
      tanVertical = tanVerticalFull
      tanHorizontal = tanVerticalFull * streamAspect
    }
    return FieldOfViewResponse(
      horizontalFov: 2.0 * atan(tanHorizontal) * 180.0 / .pi,
      verticalFov: 2.0 * atan(tanVertical) * 180.0 / .pi)
  }

  func setHeadingSource(source: HeadingSource) throws {
    poses.setSource(source)
  }

  /// iOS works the declination out from its own location.
  func setLocation(latitude: Double, longitude: Double, altitude: Double) throws {}
}
