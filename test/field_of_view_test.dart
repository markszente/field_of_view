import 'package:flutter_test/flutter_test.dart';
import 'package:field_of_view/field_of_view.dart';
import 'package:field_of_view/src/messages.g.dart';
import 'package:flutter/services.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('FieldOfView', () {
    late FieldOfView fieldOfView;

    setUp(() {
      fieldOfView = FieldOfView();
    });

    test('getFieldOfView returns FieldOfViewResponse from host', () async {
      // Set up mock handler for the Pigeon channel
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMessageHandler(
            'dev.flutter.pigeon.field_of_view.FieldOfViewHostApi.getFieldOfView',
            (ByteData? message) async {
              // Return a mock response: [horizontalFov, verticalFov] wrapped in result
              final response = FieldOfViewResponse(
                horizontalFov: 60.0,
                verticalFov: 45.0,
              );
              final codec = FieldOfViewHostApi.pigeonChannelCodec;
              return codec.encodeMessage([response]);
            },
          );

      final response = await fieldOfView.getFieldOfView();

      expect(response.horizontalFov, 60.0);
      expect(response.verticalFov, 45.0);
    });

    test('getFieldOfView throws PlatformException on error', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMessageHandler(
            'dev.flutter.pigeon.field_of_view.FieldOfViewHostApi.getFieldOfView',
            (ByteData? message) async {
              // Return an error response: [code, message, details]
              final codec = FieldOfViewHostApi.pigeonChannelCodec;
              return codec.encodeMessage([
                'CAMERA_UNAVAILABLE',
                'No back camera available',
                null,
              ]);
            },
          );

      expect(
        () => fieldOfView.getFieldOfView(),
        throwsA(isA<PlatformException>()),
      );
    });
  });

  group('FieldOfViewResponse', () {
    test('equality works correctly', () {
      final response1 = FieldOfViewResponse(
        horizontalFov: 60.0,
        verticalFov: 45.0,
      );
      final response2 = FieldOfViewResponse(
        horizontalFov: 60.0,
        verticalFov: 45.0,
      );
      final response3 = FieldOfViewResponse(
        horizontalFov: 70.0,
        verticalFov: 50.0,
      );

      expect(response1, equals(response2));
      expect(response1, isNot(equals(response3)));
    });

    test('hashCode is consistent with equality', () {
      final response1 = FieldOfViewResponse(
        horizontalFov: 60.0,
        verticalFov: 45.0,
      );
      final response2 = FieldOfViewResponse(
        horizontalFov: 60.0,
        verticalFov: 45.0,
      );

      expect(response1.hashCode, equals(response2.hashCode));
    });
  });
}
