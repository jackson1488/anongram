import 'package:flutter/widgets.dart';

/// Supported types of QR code actions
enum QrActionType {
  addFriend,       // AGQR:USER:
  linkDevice,      // AGQR:LINK:
  verifyAntiMitm,  // AGQR:VERIFY:
  unknown,
}

/// Abstract result returned after handling a scanned QR
class QrDispatchResult {
  final QrActionType type;
  final String title;
  final String description;
  final Map<String, dynamic> data;

  const QrDispatchResult({
    required this.type,
    required this.title,
    required this.description,
    this.data = const {},
  });

  factory QrDispatchResult.unknown(String raw) {
    return QrDispatchResult(
      type: QrActionType.unknown,
      title: "Неизвестный QR-код",
      description: "Код $raw не является допустимым объектом AnonGram",
    );
  }
}

/// Strategy Pattern contract for each dedicated QR sub-module (OCP)
abstract class IQrPayloadHandler {
  /// Check if this handler can parse the raw scanned string
  bool canHandle(String rawQrData);

  /// Execute domain action for this QR code
  Future<QrDispatchResult> handle(BuildContext context, String rawQrData);
}
