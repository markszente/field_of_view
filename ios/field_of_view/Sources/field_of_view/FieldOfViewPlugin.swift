import Flutter
import UIKit
import AVFoundation

public class FieldOfViewPlugin: NSObject, FlutterPlugin, FieldOfViewHostApi {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let instance = FieldOfViewPlugin()
    FieldOfViewHostApiSetup.setUp(binaryMessenger: registrar.messenger(), api: instance)
  }

  func getFieldOfView() throws -> FieldOfViewResponse {
    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
      throw PigeonError(
        code: "CAMERA_UNAVAILABLE",
        message: "No back camera available",
        details: nil
      )
    }
    
    let format = device.activeFormat
    // Use geometric distortion corrected FOV for more accurate results (iOS 13+)
    let horizontalFov = Double(format.geometricDistortionCorrectedVideoFieldOfView)
    
    // Calculate vertical FOV based on horizontal FOV and aspect ratio
    let dimensions = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
    let aspectRatio = Double(dimensions.width) / Double(dimensions.height)
    let horizontalFovRadians = horizontalFov * .pi / 180.0
    let verticalFovRadians = 2.0 * atan(tan(horizontalFovRadians / 2.0) / aspectRatio)
    let verticalFov = verticalFovRadians * 180.0 / .pi
    
    return FieldOfViewResponse(
      horizontalFov: horizontalFov,
      verticalFov: verticalFov
    )
  }
}
