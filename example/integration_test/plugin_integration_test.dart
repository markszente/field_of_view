// This is a basic Flutter integration test.
//
// Since integration tests run in a full Flutter application, they can interact
// with the host side of a plugin implementation, unlike Dart unit tests.
//
// For more information about Flutter integration tests, please see
// https://flutter.dev/to/integration-testing

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:field_of_view/field_of_view.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('getFieldOfView test', (WidgetTester tester) async {
    final FieldOfView plugin = FieldOfView();
    final FieldOfViewResponse response = await plugin.getFieldOfView();
    // Verify that FOV values are positive (valid camera data)
    expect(response.horizontalFov, greaterThan(0));
    expect(response.verticalFov, greaterThan(0));
  });
}
