import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:field_of_view/field_of_view.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FieldOfViewResponse? _fovResponse;
  String? _error;
  final _fieldOfViewPlugin = FieldOfView();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    FieldOfViewResponse? fovResponse;
    String? error;
    try {
      fovResponse = await _fieldOfViewPlugin.getFieldOfView();
    } on PlatformException catch (e) {
      error = 'Failed to get FOV: ${e.message}';
    }

    if (!mounted) return;

    setState(() {
      _fovResponse = fovResponse;
      _error = error;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Field of View Example')),
        body: Center(
          child: _error != null
              ? Text(_error!)
              : _fovResponse != null
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      'Horizontal FOV: ${_fovResponse!.horizontalFov.toStringAsFixed(2)}°',
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Vertical FOV: ${_fovResponse!.verticalFov.toStringAsFixed(2)}°',
                    ),
                  ],
                )
              : const CircularProgressIndicator(),
        ),
      ),
    );
  }
}
