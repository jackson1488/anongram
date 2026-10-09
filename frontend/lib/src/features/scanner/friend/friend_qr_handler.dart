import 'package:flutter/widgets.dart';
import '../core/qr_payload_interface.dart';

/// Handler for adding a contact via scanned QR code (AGQR:USER:)
class FriendQrHandler implements IQrPayloadHandler {
  static const String prefix = "AGQR:USER:";

  @override
  bool canHandle(String rawQrData) => rawQrData.startsWith(prefix);

  @override
  Future<QrDispatchResult> handle(BuildContext context, String rawQrData) async {
    final payload = rawQrData.substring(prefix.length);
    final parts = payload.split(":");
    final pubKey = parts.isNotEmpty ? parts[0] : "unknown";
    final nickname = parts.length > 1 ? parts[1] : "Anonymous";

    return QrDispatchResult(
      type: QrActionType.addFriend,
      title: "Добавить Контакт",
      description: "Обнаружен пользователь $nickname ($pubKey)",
      data: {
        "publicKey": pubKey,
        "nickname": nickname,
      },
    );
  }
}
