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
  PoseSample? _pose;
  String? _error;
  final _fieldOfViewPlugin = FieldOfView();
  StreamSubscription<PoseSample>? _poses;

  @override
  void initState() {
    super.initState();
    initPlatformState();
    _poses = _fieldOfViewPlugin.poses.listen(
      (pose) => setState(() => _pose = pose),
      onError: (Object e) => setState(() => _error = 'No pose: $e'),
    );
  }

  @override
  void dispose() {
    _poses?.cancel();
    super.dispose();
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
                    const SizedBox(height: 24),
                    if (_pose != null)
                      Text(
                        'Azimuth ${_pose!.azimuth.toStringAsFixed(1)}°'
                        '${_pose!.trueNorth ? '' : ' (magnetic)'}\n'
                        'Pitch ${_pose!.pitch.toStringAsFixed(1)}°   '
                        'Roll ${_pose!.roll.toStringAsFixed(1)}°\n'
                        'Accuracy ±${_pose!.accuracy.toStringAsFixed(0)}°',
                        textAlign: TextAlign.center,
                      ),
                  ],
                )
              : const CircularProgressIndicator(),
        ),
      ),
    );
  }
}
