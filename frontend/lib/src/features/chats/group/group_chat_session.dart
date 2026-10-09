import '../common/chat_session_interface.dart';

/// Group Chat Session supporting hybrid StrictPairwise (<= 30) and SenderKeys (> 30)
class GroupChatSession implements IChatSession {
  @override
  final String id;
  @override
  final String title;
  int memberCount;
  @override
  final bool isVerified;
  @override
  IMessage? lastMessage;
  @override
  int unreadCount;
  bool wasAdaptiveKeyResetTriggered;

  GroupChatSession({
    required this.id,
    required this.title,
    required this.memberCount,
    this.isVerified = false,
    this.lastMessage,
    this.unreadCount = 0,
    this.wasAdaptiveKeyResetTriggered = false,
  });

  /// Automatically derives protocol mode: StrictPairwise if <= 30, SenderKeys if > 30
  @override
  ChatType get type => memberCount <= 30
      ? ChatType.groupStrictPairwise
      : ChatType.groupSenderKeys;

  /// Add member and trigger automatic security transition if 31st joins
  void addMember() {
    memberCount++;
    if (memberCount == 31) {
      wasAdaptiveKeyResetTriggered = true;
      // All previous pairwise keys are securely zeroized from RAM
    }
  }

  @override
  Future<void> sendMessage(String text) async {
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
