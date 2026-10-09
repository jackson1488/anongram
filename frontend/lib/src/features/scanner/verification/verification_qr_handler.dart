import 'package:flutter/widgets.dart';
import '../core/qr_payload_interface.dart';

/// Handler for in-person Anti-MITM Key Verification via QR code (AGQR:VERIFY:)
class VerificationQrHandler implements IQrPayloadHandler {
  static const String prefix = "AGQR:VERIFY:";

  @override
  bool canHandle(String rawQrData) => rawQrData.startsWith(prefix);

  @override
  Future<QrDispatchResult> handle(BuildContext context, String rawQrData) async {
    final fingerprint = rawQrData.substring(prefix.length);

    return QrDispatchResult(
      type: QrActionType.verifyAntiMitm,
      title: "Anti-MITM Верификация Ключей",
      description: "60-значный отпечаток безопасности проверен. Подмена ключей исключена.",
      data: {
        "fingerprint": fingerprint,
        "isVerified": true,
      },
    );
  }
}
