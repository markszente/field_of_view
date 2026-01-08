//
//  Helper.swift
//
//
//  Created by Mark Szente on 08/01/2026.
//

import Foundation
import AVFoundation

class Helper {
    
    /// Gets the field of view from the back wide angle camera
    /// Returns a dictionary with horizontal_fov and vertical_fov
    static func getFieldOfView() -> [String: Any] {
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
            return [
                "horizontal_fov": 0.0,
                "vertical_fov": 0.0
            ]
        }
        
        let format = device.activeFormat
        let horizontalFov = format.videoFieldOfView
        
        // Calculate vertical FOV based on horizontal FOV and aspect ratio
        let dimensions = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
        let aspectRatio = Double(dimensions.width) / Double(dimensions.height)
        let horizontalFovRadians = Double(horizontalFov) * .pi / 180.0
        let verticalFovRadians = 2.0 * atan(tan(horizontalFovRadians / 2.0) / aspectRatio)
        let verticalFov = verticalFovRadians * 180.0 / .pi
        
        return [
            "horizontal_fov": Double(horizontalFov),
            "vertical_fov": verticalFov
        ]
    }
}
