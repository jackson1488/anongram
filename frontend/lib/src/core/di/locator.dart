import '../../features/settings/appearance/theme_controller.dart';

/// Simple, robust Dependency Injection Service Locator
class ServiceLocator {
  static final ServiceLocator _instance = ServiceLocator._internal();
  factory ServiceLocator() => _instance;
  ServiceLocator._internal();

  final Map<Type, dynamic> _services = {};

  void registerSingleton<T>(T service) {
    _services[T] = service;
  }

  T get<T>() {
    final service = _services[T];
    if (service == null) {
      throw Exception('Service of type $T is not registered in ServiceLocator');
    }
    return service as T;
  }

  static void initialize() {
    final locator = ServiceLocator();
    locator.registerSingleton<IThemeController>(ThemeController());
  }
}

/// Global shortcut for convenience
final sl = ServiceLocator();
