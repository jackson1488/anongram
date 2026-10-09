import 'package:flutter/widgets.dart';
import 'qr_payload_interface.dart';

/// Central QR Dispatcher delegating scanned data to polymorphic handlers
class QrPayloadDispatcher {
  final List<IQrPayloadHandler> handlers;

  const QrPayloadDispatcher(this.handlers);

  /// Dispatch raw QR code to the matching sub-module handler
  Future<QrDispatchResult> dispatch(BuildContext context, String rawQrData) async {
    for (final handler in handlers) {
      if (handler.canHandle(rawQrData)) {
        return await handler.handle(context, rawQrData);
      }
    }
    return QrDispatchResult.unknown(rawQrData);
  }
}
