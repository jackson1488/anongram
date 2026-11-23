import './chatScreen.css';
import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../../core/ui/dom';
import { tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { draftsStore } from '../../drafts/draftsStore';
import { audioPlayer, createMiniAudioPlayerBar } from '../../player/audioPlayer';
import { chatStore } from '../chatStore';
import { chatScreenT } from '../i18n';
import type { ChatMessage, MessageAttachment, ReplyPreview } from '../models';
import { createChatTopBar } from './ChatTopBar';
import { createChatWallpaper } from './ChatWallpaper';
import { createAttachMenu } from './components/AttachMenu';
import { createComposer } from './components/Composer';
import { createDateChip } from './components/DateChip';
import { createEmojiPanel } from './components/EmojiPanel';
import { openForwardModal } from './components/ForwardModal';
import { createMessageRow } from './components/MessageBubble';
import { closeMessageContextMenu, showMessageContextMenu } from './components/MessageContextMenu';
import { createReplyBar } from './components/ReplyBar';
import { createSelectionTopBar } from './components/SelectionTopBar';

function makeReplyPreview(msg: ChatMessage): ReplyPreview {
  return {
    messageId: msg.id,
    senderName: msg.senderName,
    snippet: msg.text || msg.attachment?.title || '📎',
  };
}

export function createChatRoomView(
  chatId: string,
  onBackMobile: () => void,
): HTMLElement {
  chatStore.markRead(chatId);

  const initialDraft = draftsStore.getDraft(chatId);
  let replyTarget: ReplyPreview | null = null;
  if (initialDraft?.replyToMessageId) {
    const orig = chatStore.getMessages(chatId).find((m) => m.id === initialDraft.replyToMessageId);
    if (orig) replyTarget = makeReplyPreview(orig);
  }

  let openPanel: 'none' | 'emoji' | 'attach' = 'none';
  let selectionMode = false;
  const selectedIds = new Set<string>();
  let isAtBottom = true;
  let forceScrollBottom = true;

  const headerSlot = el('div');
  const miniPlayer = createMiniAudioPlayerBar();
  const wallpaper = createChatWallpaper();
  const messagesEl = el('div', { class: 'ag-messages-scroll' });
  const scrollDownBtn = el(
    'button',
    {
      class: 'ag-cs-scroll-down',
      title: chatScreenT().scrollDown,
      onClick: () => {
        messagesEl.scrollTo({ top: messagesEl.scrollHeight, behavior: 'smooth' });
      },
    },
    tgIcons.chevronDown(20),
  );
  const stage = el('div', { class: 'ag-cs-stage' }, wallpaper, messagesEl, scrollDownBtn);

  const replySlot = el('div');
  const drawerSlot = el('div');

  const syncDraft = (text: string) => {
    draftsStore.setDraft(chatId, text, replyTarget?.messageId, replyTarget?.snippet);
  };

  // Composer is created once and kept mounted so the input never loses focus on typing or sending.
  const composer = createComposer({
    initialText: initialDraft?.text ?? '',
    onTextChange: (text) => syncDraft(text),
    onSend: (text) => {
      chatStore.sendMessage(chatId, text, replyTarget ?? undefined);
      replyTarget = null;
      openPanel = 'none';
      forceScrollBottom = true;
      draftsStore.clearDraft(chatId);
      renderReply();
      renderDrawer();
    },
    onVoice: () => {
      chatStore.sendMessage(chatId, '', replyTarget ?? undefined, {
        type: 'voice',
        title: i18nStore.t.input.attachVoice,
        durationSec: 12,
      });
      replyTarget = null;
      forceScrollBottom = true;
      renderReply();
    },
    onToggleAttach: () => {
      openPanel = openPanel === 'attach' ? 'none' : 'attach';
      renderDrawer();
    },
    onToggleEmoji: () => {
      openPanel = openPanel === 'emoji' ? 'none' : 'emoji';
      renderDrawer();
    },
  });

  const dock = el('div', { class: 'ag-cs-dock' }, replySlot, composer.root, drawerSlot);
  const root = el('div', { class: 'ag-chat-room' }, headerSlot, miniPlayer, stage, dock);

  const updateScrollBtn = () => {
    const dist = messagesEl.scrollHeight - messagesEl.scrollTop - messagesEl.clientHeight;
    isAtBottom = dist < 64;
    scrollDownBtn.classList.toggle('is-visible', dist > 120);
  };
  messagesEl.addEventListener('scroll', updateScrollBtn, { passive: true });

  const scrollToBottomNow = () => {
    messagesEl.scrollTop = messagesEl.scrollHeight;
    updateScrollBtn();
  };

  const exitSelection = () => {
    if (!selectionMode && selectedIds.size === 0) return;
    selectionMode = false;
    selectedIds.clear();
    renderHeader();
    renderMessages();
  };

  const getSelectedMessages = (): ChatMessage[] =>
    chatStore.getMessages(chatId).filter((m) => selectedIds.has(m.id));

  const renderHeader = () => {
    clear(headerSlot);
    if (selectionMode) {
      const selected = getSelectedMessages();
      headerSlot.appendChild(
        createSelectionTopBar({
          count: selected.length,
          canReply: selected.length === 1,
          canCopy: selected.some((m) => Boolean(m.text)),
          onClose: exitSelection,
          onReply: () => {
            if (selected.length !== 1) return;
            replyTarget = makeReplyPreview(selected[0]);
            exitSelection();
            renderReply();
            syncDraft(composer.getText());
            composer.focus();
          },
          onCopy: async () => {
            const joined = selected
              .map((m) => m.text)
              .filter(Boolean)
              .join('\n');
            if (joined) {
              await copyToClipboard(joined);
              showToast(i18nStore.t.common.copied);
            }
            exitSelection();
          },
          onForward: () => {
            if (!selected.length) return;
            openForwardModal(selected, exitSelection);
          },
          onDelete: () => {
            const count = selected.length;
            for (const m of selected) chatStore.deleteMessage(chatId, m.id);
            showToast(chatScreenT().deletedToast(count));
            exitSelection();
          },
        }),
      );
      return;
    }

    const thread = chatStore.getThread(chatId);
    if (thread) {
      headerSlot.appendChild(createChatTopBar(thread, onBackMobile));
    }
  };

  const renderReply = () => {
    clear(replySlot);
    const bar = createReplyBar(replyTarget, () => {
      replyTarget = null;
      syncDraft(composer.getText());
      renderReply();
      composer.focus();
    });
    if (bar) replySlot.appendChild(bar);
  };

  const renderDrawer = () => {
    clear(drawerSlot);
    if (openPanel === 'emoji') {
      drawerSlot.appendChild(
        createEmojiPanel(
          (em) => composer.insertText(em),
          (st) => {
            chatStore.sendMessage(chatId, st, replyTarget ?? undefined);
            replyTarget = null;
            openPanel = 'none';
            forceScrollBottom = true;
            renderReply();
            renderDrawer();
            composer.focus();
          },
        ),
      );
    } else if (openPanel === 'attach') {
      drawerSlot.appendChild(
        createAttachMenu({
          send: (att: MessageAttachment) => {
            const text = composer.getText();
            chatStore.sendMessage(chatId, text, replyTarget ?? undefined, att);
            composer.setText('');
            replyTarget = null;
            openPanel = 'none';
            forceScrollBottom = true;
            draftsStore.clearDraft(chatId);
            renderReply();
            renderDrawer();
            composer.focus();
          },
          close: () => {
            openPanel = 'none';
            renderDrawer();
          },
        }),
      );
    }
  };

  const jumpToMessage = (messageId: string) => {
    const target = messagesEl.querySelector(`[data-msg-id="${CSS.escape(messageId)}"]`) as HTMLElement | null;
    if (!target) return;
    target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    target.classList.remove('is-flash');
    void target.offsetWidth;
    target.classList.add('is-flash');
    window.setTimeout(() => target.classList.remove('is-flash'), 1200);
  };

  const openMenuFor = (msg: ChatMessage, x: number, y: number) => {
    showMessageContextMenu(msg, x, y, {
      onReact: (em) => chatStore.toggleReaction(chatId, msg.id, em),
      onReply: () => {
        replyTarget = makeReplyPreview(msg);
        syncDraft(composer.getText());
        renderReply();
        composer.focus();
      },
      onCopy: async () => {
        if (!msg.text) return;
        await copyToClipboard(msg.text);
        showToast(i18nStore.t.common.copied);
      },
      onForward: () => openForwardModal([msg]),
      onSelect: () => {
        selectionMode = true;
        selectedIds.add(msg.id);
        renderHeader();
        renderMessages();
      },
      onDelete: () => chatStore.deleteMessage(chatId, msg.id),
    });
  };

  const renderMessages = () => {
    const keepBottom = forceScrollBottom || isAtBottom;
    const prevScrollTop = messagesEl.scrollTop;
    forceScrollBottom = false;

    clear(messagesEl);
    const messages = chatStore.getMessages(chatId);
    const isDesktop = window.innerWidth >= 840;
    let lastDay = '';

    for (const msg of messages) {
      const dayKey = new Date(msg.createdAt).toDateString();
      if (dayKey !== lastDay) {
        lastDay = dayKey;
        messagesEl.appendChild(createDateChip(msg.createdAt));
      }

      messagesEl.appendChild(
        createMessageRow(msg, {
          chatId,
          selectionMode,
          selected: selectedIds.has(msg.id),
          isDesktop,
          onContextMenu: openMenuFor,
          onToggleSelect: (m) => {
            if (selectedIds.has(m.id)) selectedIds.delete(m.id);
            else selectedIds.add(m.id);
            if (selectedIds.size === 0) selectionMode = false;
            renderHeader();
            renderMessages();
          },
          onEnterSelect: (m) => {
            selectionMode = true;
            selectedIds.add(m.id);
            renderHeader();
            renderMessages();
          },
          onSwipeReply: (m) => {
            replyTarget = makeReplyPreview(m);
            syncDraft(composer.getText());
            renderReply();
            composer.focus();
          },
          onReplyJump: jumpToMessage,
        }),
      );
    }

    if (keepBottom) {
      scrollToBottomNow();
    } else {
      messagesEl.scrollTop = prevScrollTop;
      updateScrollBtn();
    }
  };

  const unsubChats = chatStore.subscribe(() => {
    renderHeader();
    renderMessages();
  });

  let audioKey = `${audioPlayer.currentTrack?.id ?? ''}|${audioPlayer.playing}`;
  const unsubAudio = audioPlayer.subscribe(() => {
    // Ignore position ticks: only re-render message bubbles when track or play state changes.
    const nextKey = `${audioPlayer.currentTrack?.id ?? ''}|${audioPlayer.playing}`;
    if (nextKey !== audioKey) {
      audioKey = nextKey;
      renderMessages();
    }
  });

  const unsubI18n = i18nStore.subscribe(() => {
    const inp = composer.root.querySelector('.ag-msg-input') as HTMLInputElement | null;
    if (inp) inp.placeholder = i18nStore.t.input.placeholder;
    scrollDownBtn.title = chatScreenT().scrollDown;
    renderHeader();
    renderReply();
    renderMessages();
    renderDrawer();
  });

  renderHeader();
  renderReply();
  renderMessages();
  renderDrawer();
  requestAnimationFrame(scrollToBottomNow);

  if (window.innerWidth >= 840) {
    requestAnimationFrame(() => composer.focus());
  }

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubChats();
    unsubAudio();
    unsubI18n();
    closeMessageContextMenu();
  };

  return root;
}
