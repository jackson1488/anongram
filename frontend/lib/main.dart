import 'package:flutter/material.dart';
import 'src/core/di/locator.dart';
import 'app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize modular DI Service Locator
  ServiceLocator.initialize();

  runApp(const AnonGramApp());
}
