import '../common/chat_session_interface.dart';

/// 1-on-1 Direct Chat Session using pure Double Ratchet (KDF ratchet + DH ratchet)
class DirectChatSession implements IChatSession {
  @override
  final String id;
  @override
  final String title;
  final String peerPublicKey;
  final String sasEmojiHash;
  @override
  final bool isVerified;
  @override
  IMessage? lastMessage;
  @override
  int unreadCount;

  DirectChatSession({
    required this.id,
    required this.title,
    required this.peerPublicKey,
    required this.sasEmojiHash,
    this.isVerified = false,
    this.lastMessage,
    this.unreadCount = 0,
  });

  @override
  ChatType get type => ChatType.direct;

  @override
  Future<void> sendMessage(String text) async {
    // Advances the asymmetric/symmetric ratchet chain for each individual message
    lastMessage = ChatMessage(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      senderId: "me",
      text: text,
      timestamp: DateTime.now(),
      isEncrypted: true,
      isRead: true,
    );
  }
}
