import './chatScreen.css';
import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../../core/ui/dom';
import { tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { draftsStore } from '../../drafts/draftsStore';
import { audioPlayer, createMiniAudioPlayerBar } from '../../player/audioPlayer';
import { settingsStore } from '../../settings/settingsStore';
import { openWallpaperPicker } from '../../wallpapers';
import { chatStore } from '../chatStore';
import { chatScreenT } from '../i18n';
import { openExternalLink } from '../links/InAppBrowserHost';
import type { ChatMessage, MessageAttachment, ReplyPreview } from '../models';
import { createInChatSearchBar } from '../search/ChatSearchScreen';
import { localMessageTranslator as localTranslator } from '../tools/translate/LocalMessageTranslator';
import { createUserProfileScreen } from '../user_profile/api/UserProfileScreen';
import { blockedUsersStore } from '../user_profile/data/BlockedUsersStore';
import { userProfileT } from '../user_profile/i18n/UserProfileI18n';
import { createChatTopBar } from './ChatTopBar';
import { createChatWallpaper } from './ChatWallpaper';
import { createAttachMenu } from './components/AttachMenu';
import { createComposer } from './components/Composer';
import { createDateChip } from './components/DateChip';
import { openDeleteMessageDialog } from './components/DeleteMessageDialog';
import { createEmojiPanel } from './components/EmojiPanel';
import { openForwardModal } from './components/ForwardModal';
import { createMessageRow } from './components/MessageBubble';
import { closeMessageContextMenu, showMessageContextMenu } from './components/MessageContextMenu';
import { createPinnedMessageBar } from './components/PinnedMessageBar';
import { createReplyBar } from './components/ReplyBar';
import { createSelectionTopBar } from './components/SelectionTopBar';

type ChatStoreExtra = typeof chatStore & {
  clearHistory?: (chatId: string) => void;
  deleteChat?: (chatId: string) => void;
};

function sameCalendarDay(a: ChatMessage, b: ChatMessage): boolean {
  return new Date(a.createdAt).toDateString() === new Date(b.createdAt).toDateString();
}

function clearChatMessages(chatId: string): void {
  const store = chatStore as ChatStoreExtra;
  if (typeof store.clearHistory === 'function') {
    store.clearHistory(chatId);
    return;
  }
  for (const m of [...chatStore.getMessages(chatId)]) {
    chatStore.deleteMessage(chatId, m.id);
  }
}

function deleteChatThread(chatId: string): void {
  const store = chatStore as ChatStoreExtra;
  if (typeof store.deleteChat === 'function') {
    store.deleteChat(chatId);
    return;
  }
  clearChatMessages(chatId);
  const thread = chatStore.getThread(chatId);
  if (thread && !thread.isArchived) {
    chatStore.toggleArchive(chatId);
  }
}

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
  chatStore.purgeExpiredAutoDelete(chatId);
  chatStore.markRead(chatId);

  const initialDraft = draftsStore.getDraft(chatId);
  let replyTarget: ReplyPreview | null = null;
  if (initialDraft?.replyToMessageId) {
    const orig = chatStore.getMessages(chatId).find((m) => m.id === initialDraft.replyToMessageId);
    if (orig) replyTarget = makeReplyPreview(orig);
  }

  let editingMessage: ChatMessage | null = null;
  let openPanel: 'none' | 'emoji' | 'attach' = 'none';
  let selectionMode = false;
  let searchOpen = false;
  let profileOpen = false;
  const selectedIds = new Set<string>();
  let isAtBottom = true;
  let forceScrollBottom = true;

  const headerSlot = el('div');
  const searchSlot = el('div');
  const pinnedSlot = el('div');
  const miniPlayer = createMiniAudioPlayerBar();
  const wallpaper = createChatWallpaper();
  const messagesInner = el('div', {
    class: 'ag-messages-inner',
    style: 'display:flex;flex-direction:column;width:100%;max-width:720px;margin:auto auto 0;',
  });
  const messagesEl = el('div', { class: 'ag-messages-scroll' }, messagesInner);
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
  const blockedSlot = el('div');
  const drawerSlot = el('div');

  const syncDraft = (text: string) => {
    draftsStore.setDraft(chatId, text, replyTarget?.messageId, replyTarget?.snippet);
  };

  const getEditTitle = (): string =>
    i18nStore.language === 'ru' ? 'Редактирование' : 'Edit message';

  const restoreDraftAfterEdit = () => {
    const saved = draftsStore.getDraft(chatId);
    composer.setText(saved?.text ?? '');
    if (!replyTarget && saved?.replyToMessageId) {
      const orig = chatStore.getMessages(chatId).find((m) => m.id === saved.replyToMessageId);
      if (orig) {
        replyTarget = makeReplyPreview(orig);
        renderReply();
      }
    }
  };

  const cancelEditing = () => {
    if (!editingMessage) return;
    editingMessage = null;
    composer.clearEditing();
    restoreDraftAfterEdit();
  };

  const startEditing = (msg: ChatMessage) => {
    if (blockedUsersStore.isBlocked(chatId) || !msg.isOutgoing || !msg.text.trim()) return;
    editingMessage = msg;
    replyTarget = null;
    renderReply();
    composer.setEditing(getEditTitle(), msg.text);
    composer.focus();
  };

  const sendRecordedMedia = (
    type: 'voice' | 'video_circle',
    durationSec: number,
    previewUrl?: string,
  ) => {
    if (blockedUsersStore.isBlocked(chatId)) return;
    if (editingMessage) {
      editingMessage = null;
      composer.clearEditing();
    }
    const title =
      type === 'voice' ? i18nStore.t.input.attachVoice : i18nStore.t.input.attachVideoCircle;
    chatStore.sendMessage(chatId, '', replyTarget ?? undefined, {
      type,
      title,
      durationSec,
      previewUrl,
    });
    replyTarget = null;
    openPanel = 'none';
    forceScrollBottom = true;
    draftsStore.clearReplyTarget(chatId);
    renderReply();
    renderDrawer();
  };

  const confirmDeleteMessages = (msgs: ChatMessage[], afterDelete?: () => void) => {
    if (!msgs.length) return;
    const thread = chatStore.getThread(chatId);
    const canDeleteForAll = !thread?.isSavedMessages && msgs.every((m) => m.isOutgoing);
    openDeleteMessageDialog(msgs.length, canDeleteForAll, (_forEveryone) => {
      for (const m of msgs) {
        chatStore.deleteMessage(chatId, m.id);
      }
      showToast(chatScreenT().deletedToast(msgs.length));
      afterDelete?.();
    });
  };

  // Composer is created once and kept mounted so the input never loses focus on typing or sending.
  const composer = createComposer({
    initialText: initialDraft?.text ?? '',
    onTextChange: (text) => {
      if (!editingMessage) syncDraft(text);
    },
    onSend: (text) => {
      if (blockedUsersStore.isBlocked(chatId)) return;
      if (editingMessage) {
        const target = editingMessage;
        editingMessage = null;
        composer.clearEditing();
        chatStore.editMessage(chatId, target.id, text);
        if (localTranslator.getMessageTranslation(target.id) !== null) {
          localTranslator.clearMessageTranslation(target.id);
        }
        restoreDraftAfterEdit();
        openPanel = 'none';
        renderDrawer();
        return;
      }
      chatStore.sendMessage(chatId, text, replyTarget ?? undefined);
      replyTarget = null;
      openPanel = 'none';
      forceScrollBottom = true;
      draftsStore.clearDraft(chatId);
      renderReply();
      renderDrawer();
    },
    onCancelEdit: () => {
      editingMessage = null;
      restoreDraftAfterEdit();
    },
    onVoice: () => sendRecordedMedia('voice', 12),
    onSendVoiceRecorded: (dataUrl, durationSec) => sendRecordedMedia('voice', durationSec, dataUrl),
    onSendVideoCircleRecorded: (dataUrl, durationSec) =>
      sendRecordedMedia('video_circle', durationSec, dataUrl),
    onToggleAttach: () => {
      if (blockedUsersStore.isBlocked(chatId)) return;
      openPanel = openPanel === 'attach' ? 'none' : 'attach';
      renderDrawer();
    },
    onToggleEmoji: () => {
      if (blockedUsersStore.isBlocked(chatId)) return;
      openPanel = openPanel === 'emoji' ? 'none' : 'emoji';
      renderDrawer();
    },
  });

  const dock = el('div', { class: 'ag-cs-dock' }, replySlot, composer.root, blockedSlot, drawerSlot);
  const mainCol = el(
    'div',
    { style: 'flex:1;min-width:0;min-height:0;display:flex;flex-direction:column;' },
    headerSlot,
    searchSlot,
    pinnedSlot,
    miniPlayer,
    stage,
    dock,
  );
  const profileSlot = el('div', { style: 'display:none;' });
  const root = el('div', { class: 'ag-chat-room', style: 'flex-direction:row;' }, mainCol, profileSlot);

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
            cancelEditing();
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
          onDelete: () => confirmDeleteMessages(selected, exitSelection),
        }),
      );
      return;
    }

    const thread = chatStore.getThread(chatId);
    if (thread) {
      const actions = {
        onOpenProfile: () => {
          profileOpen = !profileOpen;
          renderProfile();
        },
        onToggleSearch: () => {
          searchOpen = !searchOpen;
          renderSearchBar();
        },
        onClearHistory: () => {
          clearChatMessages(chatId);
          exitSelection();
          showToast(userProfileT().clearedHistoryToast);
        },
        onDeleteChat: () => {
          if (profileOpen) {
            profileOpen = false;
            renderProfile();
          }
          deleteChatThread(chatId);
          draftsStore.clearDraft(chatId);
          showToast(userProfileT().deletedChatToast);
          onBackMobile();
        },
        onToggleMute: () => {
          const nowMuted = blockedUsersStore.toggleMute(chatId);
          const s = userProfileT();
          showToast(nowMuted ? s.mutedToast : s.unmutedToast);
        },
        onOpenWallpaper: () => {
          openWallpaperPicker();
        },
      };
      headerSlot.appendChild(createChatTopBar(thread, onBackMobile, actions));
    }
  };

  const renderSearchBar = () => {
    (searchSlot.firstElementChild as { __cleanup?: () => void } | null)?.__cleanup?.();
    clear(searchSlot);
    if (!searchOpen) return;
    const bar = createInChatSearchBar(
      chatId,
      (msgId) => jumpToMessage(msgId),
      () => {
        searchOpen = false;
        renderSearchBar();
        composer.focus();
      },
    );
    searchSlot.appendChild(bar);
    const inp = bar.querySelector('.ag-inchat-search__input') as HTMLInputElement | null;
    inp?.focus();
  };

  const renderProfile = () => {
    clear(profileSlot);
    if (!profileOpen) {
      profileSlot.style.display = 'none';
      return;
    }
    const isDesk = window.innerWidth >= 840;
    profileSlot.style.display = isDesk ? 'flex' : 'block';
    if (!isDesk) {
      profileSlot.style.cssText = 'position:fixed;inset:0;z-index:75;display:flex;flex-direction:column;';
    } else {
      profileSlot.style.cssText = 'display:flex;height:100%;';
    }
    profileSlot.appendChild(
      createUserProfileScreen(
        chatId,
        () => {
          profileOpen = false;
          renderProfile();
        },
        { desktopColumn: isDesk },
      ),
    );
  };

  pinnedSlot.appendChild(createPinnedMessageBar(chatId, (msgId) => jumpToMessage(msgId)));

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
            if (blockedUsersStore.isBlocked(chatId)) return;
            if (editingMessage) {
              editingMessage = null;
              composer.clearEditing();
            }
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
            if (blockedUsersStore.isBlocked(chatId)) return;
            if (editingMessage) {
              editingMessage = null;
              composer.clearEditing();
            }
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
    const canEdit = msg.isOutgoing && Boolean(msg.text.trim());
    const canTranslate = Boolean(msg.text.trim()) && settingsStore.all.showTranslateButton;
    showMessageContextMenu(msg, x, y, {
      onReact: (em) => chatStore.toggleReaction(chatId, msg.id, em),
      onReply: () => {
        cancelEditing();
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
      onEdit: canEdit ? () => startEditing(msg) : undefined,
      onTranslate: canTranslate
        ? () => {
            if (localTranslator.getMessageTranslation(msg.id) !== null) {
              localTranslator.clearMessageTranslation(msg.id);
              return;
            }
            void localTranslator
              .translateMessage(msg.id, msg.text)
              .then(() => {
                renderMessages();
              })
              .catch(() => {
                showToast(
                  i18nStore.language === 'ru'
                    ? 'Не удалось перевести сообщение'
                    : 'Failed to translate message',
                );
              });
          }
        : undefined,
      onForward: () => openForwardModal([msg]),
      onSelect: () => {
        selectionMode = true;
        selectedIds.add(msg.id);
        renderHeader();
        renderMessages();
      },
      onDelete: () => confirmDeleteMessages([msg]),
    });
  };

  const renderMessages = () => {
    const keepBottom = forceScrollBottom || isAtBottom;
    const prevScrollTop = messagesEl.scrollTop;
    forceScrollBottom = false;

    clear(messagesInner);
    const messages = chatStore.getMessages(chatId);
    const isDesktop = window.innerWidth >= 840;
    let lastDay = '';

    for (let i = 0; i < messages.length; i++) {
      const msg = messages[i];
      const dayKey = new Date(msg.createdAt).toDateString();
      if (dayKey !== lastDay) {
        lastDay = dayKey;
        messagesInner.appendChild(createDateChip(msg.createdAt));
      }

      const prev = i > 0 ? messages[i - 1] : null;
      const next = i < messages.length - 1 ? messages[i + 1] : null;
      const isGroupFirst = !(prev && prev.isOutgoing === msg.isOutgoing && sameCalendarDay(prev, msg));
      const isGroupLast = !(next && next.isOutgoing === msg.isOutgoing && sameCalendarDay(msg, next));

      messagesInner.appendChild(
        createMessageRow(msg, {
          chatId,
          selectionMode,
          selected: selectedIds.has(msg.id),
          isDesktop,
          isGroupFirst,
          isGroupLast,
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
            cancelEditing();
            replyTarget = makeReplyPreview(m);
            syncDraft(composer.getText());
            renderReply();
            composer.focus();
          },
          onReplyJump: jumpToMessage,
          onTranscribeVoice: (m) => {
            if (localTranslator.isVoiceTranscribing(m.id)) return;
            const durationSec = m.attachment?.durationSec ?? 12;
            const lang = i18nStore.language === 'ru' ? 'ru' : 'en';
            void localTranslator
              .transcribeVoice(m.id, durationSec, m.attachment?.previewUrl, lang)
              .then(() => {
                renderMessages();
              })
              .catch(() => {
                showToast(
                  i18nStore.language === 'ru'
                    ? 'Не удалось расшифровать голосовое'
                    : 'Failed to transcribe voice message',
                );
              });
          },
          onOpenLink: (url) => openExternalLink(url),
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

  const renderBlockedDock = () => {
    const blocked = blockedUsersStore.isBlocked(chatId);
    composer.root.style.display = blocked ? 'none' : '';
    replySlot.style.display = blocked ? 'none' : '';
    drawerSlot.style.display = blocked ? 'none' : '';
    clear(blockedSlot);
    if (!blocked) return;
    if (openPanel !== 'none') {
      openPanel = 'none';
      renderDrawer();
    }
    const s = chatScreenT();
    const unblockBtn = el(
      'button',
      {
        type: 'button',
        style: [
          'border:none',
          'background:var(--ag-accent)',
          'color:var(--ag-on-accent)',
          'border-radius:9999px',
          'padding:8px 18px',
          'font-size:14px',
          'font-weight:600',
          'cursor:pointer',
          'flex-shrink:0',
        ].join(';'),
        onClick: () => {
          blockedUsersStore.unblock(chatId);
          composer.focus();
        },
      },
      s.unblockUser,
    );
    const banner = el(
      'div',
      {
        class: 'ag-cs-blocked-banner',
        style: [
          'display:flex',
          'align-items:center',
          'justify-content:space-between',
          'gap:12px',
          'padding:10px 16px',
          'background:var(--ag-island-bg)',
          'border-top:1px solid var(--ag-divider)',
          'backdrop-filter:blur(12px)',
          '-webkit-backdrop-filter:blur(12px)',
        ].join(';'),
      },
      el(
        'span',
        {
          style: 'font-size:14px;color:var(--ag-text-secondary);min-width:0;',
        },
        s.blockedBannerText,
      ),
      unblockBtn,
    );
    blockedSlot.appendChild(banner);
  };

  const autoTranslateIncomingMessages = () => {
    if (!settingsStore.all.autoTranslateIncoming) return;
    const configuredTarget = localTranslator.getTargetLang();
    const effectiveTarget =
      !configuredTarget || configuredTarget === 'auto' ? i18nStore.language : configuredTarget;
    for (const msg of chatStore.getMessages(chatId)) {
      if (msg.isOutgoing || !msg.text.trim()) continue;
      if (localTranslator.getMessageTranslation(msg.id) !== null) continue;
      if (localTranslator.isMessageTranslating(msg.id)) continue;
      if (localTranslator.detectLanguage(msg.text) === effectiveTarget) continue;
      void localTranslator.translateMessage(msg.id, msg.text, effectiveTarget);
    }
  };

  const unsubChats = chatStore.subscribe(() => {
    if (
      editingMessage &&
      !chatStore.getMessages(chatId).some((m) => m.id === editingMessage!.id)
    ) {
      cancelEditing();
    }
    renderHeader();
    renderMessages();
    autoTranslateIncomingMessages();
  });

  const unsubBlocked = blockedUsersStore.subscribe(() => {
    chatStore.purgeExpiredAutoDelete(chatId);
    if (blockedUsersStore.isBlocked(chatId)) {
      cancelEditing();
    }
    renderHeader();
    renderBlockedDock();
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
    renderBlockedDock();
    autoTranslateIncomingMessages();
  });

  const unsubTranslator = localTranslator.subscribe(() => {
    renderMessages();
  });

  const unsubSettings = settingsStore.subscribe(() => {
    autoTranslateIncomingMessages();
    renderMessages();
  });

  renderHeader();
  renderReply();
  renderMessages();
  renderDrawer();
  renderBlockedDock();
  autoTranslateIncomingMessages();
  requestAnimationFrame(scrollToBottomNow);

  if (window.innerWidth >= 840 && !blockedUsersStore.isBlocked(chatId)) {
    requestAnimationFrame(() => composer.focus());
  }

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubChats();
    unsubBlocked();
    unsubAudio();
    unsubI18n();
    unsubTranslator();
    unsubSettings();
    (wallpaper as unknown as { __cleanup?: () => void }).__cleanup?.();
    (miniPlayer as unknown as { __cleanup?: () => void }).__cleanup?.();
    (pinnedSlot.firstElementChild as unknown as { __cleanup?: () => void } | null)?.__cleanup?.();
    (searchSlot.firstElementChild as unknown as { __cleanup?: () => void } | null)?.__cleanup?.();
    closeMessageContextMenu();
  };

  return root;
}
