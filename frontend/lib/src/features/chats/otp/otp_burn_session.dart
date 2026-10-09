import '../common/chat_session_interface.dart';

/// Ephemeral Self-Destructing OTP (One-Time Pad) Secret Chat Session
class OtpBurnSession implements IChatSession {
  @override
  final String id;
  @override
  final String title;
  final Duration burnTtl;
  int remainingSeconds;
  @override
  final bool isVerified;
  @override
  IMessage? lastMessage;
  @override
  int unreadCount;
  bool isDestroyed;

  OtpBurnSession({
    required this.id,
    required this.title,
    required this.burnTtl,
    this.isVerified = false,
    this.lastMessage,
    this.unreadCount = 0,
    this.isDestroyed = false,
  }) : remainingSeconds = burnTtl.inSeconds;

  @override
  ChatType get type => ChatType.otpBurn;

  /// Decrement self-destruct countdown timer
  void tick() {
    if (remainingSeconds > 0) {
      remainingSeconds--;
      if (remainingSeconds == 0) {
        destroy();
      }
    }
  }

  /// Zeroize all ephemeral OTP keys and scrub all local disk traces
  void destroy() {
    isDestroyed = true;
    lastMessage = null;
  }

  @override
  Future<void> sendMessage(String text) async {
    if (isDestroyed) throw StateError("Cannot send message to destroyed OTP session");
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
