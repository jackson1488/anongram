/// Type of encryption and topology of a chat session
enum ChatType {
  direct,               // 1-on-1 pure Double Ratchet
  groupStrictPairwise,  // Group <= 30 with dedicated pairwise ratchets
  groupSenderKeys,      // Group > 30 with Sender Keys protocol
  otpBurn,              // Ephemeral self-destructing One-Time Pad
}

/// Abstract contract for a single message
abstract class IMessage {
  String get id;
  String get senderId;
  String get text;
  DateTime get timestamp;
  bool get isEncrypted;
  bool get isRead;
}

/// Concrete message implementation
class ChatMessage implements IMessage {
  @override
  final String id;
  @override
  final String senderId;
  @override
  final String text;
  @override
  final DateTime timestamp;
  @override
  final bool isEncrypted;
  @override
  final bool isRead;

  const ChatMessage({
    required this.id,
    required this.senderId,
    required this.text,
    required this.timestamp,
    this.isEncrypted = true,
    this.isRead = false,
  });
}

/// Polymorphic interface for all chat sessions (LSP / OCP)
abstract class IChatSession {
  String get id;
  String get title;
  ChatType get type;
  IMessage? get lastMessage;
  int get unreadCount;
  bool get isVerified;

  Future<void> sendMessage(String text);
}
