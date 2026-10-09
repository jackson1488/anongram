import 'package:flutter/widgets.dart';
import '../core/qr_payload_interface.dart';

/// Handler for linking an auxiliary device (Desktop / Web / Tablet) via QR code (AGQR:LINK:)
class DeviceLinkQrHandler implements IQrPayloadHandler {
  static const String prefix = "AGQR:LINK:";

  @override
  bool canHandle(String rawQrData) => rawQrData.startsWith(prefix);

  @override
  Future<QrDispatchResult> handle(BuildContext context, String rawQrData) async {
    final sessionToken = rawQrData.substring(prefix.length);

    return QrDispatchResult(
      type: QrActionType.linkDevice,
      title: "Связать Устройство",
      description: "Запрос на передачу защищенных ключей на новый клиент (Web/Desktop)",
      data: {
        "sessionToken": sessionToken,
        "validForSeconds": 60,
      },
    );
  }
}
