import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';
import '../common/chat_session_interface.dart';
import '../direct/direct_chat_session.dart';
import '../group/group_chat_session.dart';
import '../otp/otp_burn_session.dart';

/// Screen 1: Primary Chats Screen rendering polymorphic IChatSession items
class ChatsListScreen extends StatefulWidget {
  const ChatsListScreen({super.key});

  @override
  State<ChatsListScreen> createState() => _ChatsListScreenState();
}

class _ChatsListScreenState extends State<ChatsListScreen> {
  late final List<IChatSession> _sessions;
  String _selectedFilter = "Все";

  @override
  void initState() {
    super.initState();
    _sessions = [
      DirectChatSession(
        id: "direct_alice",
        title: "Алиса",
        peerPublicKey: "0x7F2A...9C",
        sasEmojiHash: "🦊 🛡️ 🚀 🌊",
        isVerified: true,
        unreadCount: 1,
        lastMessage: ChatMessage(
          id: "m1",
          senderId: "alice",
          text: "Файл отчета зашифрован. Жду проверки SAS 🦊🚀",
          timestamp: DateTime.now(),
        ),
      ),
      GroupChatSession(
        id: "group_cypherpunk",
        title: "Cypherpunk Alpha 🛡️",
        memberCount: 28,
        unreadCount: 0,
        lastMessage: ChatMessage(
          id: "m2",
          senderId: "bob",
          text: "Режим StrictPairwise активен (<=30)",
          timestamp: DateTime.now().subtract(const Duration(minutes: 45)),
        ),
      ),
      OtpBurnSession(
        id: "otp_anon",
        title: "Аноним #409",
        burnTtl: const Duration(minutes: 5),
        unreadCount: 0,
        lastMessage: ChatMessage(
          id: "m3",
          senderId: "anon",
          text: "Сообщение исчезнет после открытия...",
          timestamp: DateTime.now().subtract(const Duration(hours: 1)),
        ),
      ),
    ];
  }

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);

    return Scaffold(
      backgroundColor: theme.bgDark,
      appBar: AppBar(
        backgroundColor: theme.surfaceDark,
        elevation: 0,
        title: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [theme.neonAccent, theme.secondaryAccent],
                ),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.shield, color: Colors.black, size: 20),
            ),
            const SizedBox(width: 10),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "AnonGram",
                  style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                Row(
                  children: [
                    Container(width: 6, height: 6, decoration: BoxDecoration(color: theme.secondaryAccent, shape: BoxShape.circle)),
                    const SizedBox(width: 4),
                    Text(
                      "E2EE Active • Onion 3-hop",
                      style: TextStyle(fontSize: 9, color: theme.secondaryAccent, fontFamily: 'monospace'),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.search, color: Colors.white70),
            onPressed: () {},
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter Chips
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: theme.surfaceDark.withOpacity(0.5),
            child: Row(
              children: ["Все", "Секретные 🔒", "Группы (до 300)", "Каналы"].map((filter) {
                final isSelected = filter == _selectedFilter;
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(filter, style: TextStyle(fontSize: 11, color: isSelected ? Colors.white : Colors.white60)),
                    selected: isSelected,
                    selectedColor: theme.secondaryAccent,
                    backgroundColor: theme.surfaceDark,
                    onSelected: (val) {
                      if (val) setState(() => _selectedFilter = filter);
                    },
                  ),
                );
              }).toList(),
            ),
          ),
          // Chats List
          Expanded(
            child: ListView.separated(
              itemCount: _sessions.length,
              separatorBuilder: (_, __) => Divider(color: Colors.white.withOpacity(0.05), height: 1),
              itemBuilder: (context, index) {
                final session = _sessions[index];
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  leading: Stack(
                    children: [
                      CircleAvatar(
                        radius: 22,
                        backgroundColor: theme.cardDark,
                        child: Text(
                          session.title.substring(0, 1),
                          style: TextStyle(color: theme.neonAccent, fontWeight: FontWeight.bold),
                        ),
                      ),
                      if (session.isVerified)
                        Positioned(
                          right: 0,
                          bottom: 0,
                          child: Container(
                            padding: const EdgeInsets.all(2),
                            decoration: const BoxDecoration(color: Colors.black, shape: BoxShape.circle),
                            child: Icon(Icons.verified, size: 12, color: theme.secondaryAccent),
                          ),
                        ),
                    ],
                  ),
                  title: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        session.title,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                      const Text(
                        "19:42",
                        style: TextStyle(color: Colors.white38, fontSize: 10),
                      ),
                    ],
                  ),
                  subtitle: Text(
                    session.lastMessage?.text ?? "Нет сообщений",
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(color: Colors.white.withOpacity(0.6), fontSize: 11),
                  ),
                  trailing: session.unreadCount > 0
                      ? Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: theme.secondaryAccent,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            "${session.unreadCount}",
                            style: const TextStyle(color: Colors.black, fontSize: 10, fontWeight: FontWeight.bold),
                          ),
                        )
                      : null,
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
