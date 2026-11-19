import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { openActiveCallModal } from '../calls/CallsView';
import { audioPlayer, createMiniAudioPlayerBar } from '../player/audioPlayer';
import { chatStore } from './chatStore';
import type { AttachmentType, ChatMessage, MessageAttachment, ReplyPreview } from './models';
import { openMediaViewer } from './viewer/MediaViewerOverlay';

const EMOJIS = ['👍', '🔥', '❤️', '😂', '😮', '😢', '🎉', '🛡️', '⚡', '🕶️', '🔐', '💎', '🚀', '✨', '👌', '🤝'];
const STICKERS = ['🦊', '🐉', '🛸', '🕶️', '⚡', '🔐', '💎', '🌌'];

function openForwardModal(sourceMessage: ChatMessage): void {
  const t = i18nStore.t;
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });

  const threads = chatStore.threads;
  overlay.appendChild(
    el(
      'div',
      { class: 'ag-card ag-forward-modal' },
      el(
        'div',
        { class: 'ag-forward-modal__head' },
        el('h3', { style: 'margin:0;font-size:17px;' }, t.chats.forwardAnonTitle),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
      el('p', { style: 'margin:0 0 10px;font-size:13px;color:var(--ag-text-secondary);' }, t.chats.forwardSelectChat),
      el(
        'div',
        { class: 'ag-forward-list' },
        ...threads.map((thread) =>
          el(
            'div',
            {
              class: 'ag-chat-row',
              onClick: () => {
                chatStore.forwardAnonymously(thread.id, sourceMessage);
                overlay.remove();
                showToast(t.chats.forwardedAnonToast);
              },
            },
            el('div', { class: 'ag-avatar', style: `background:${thread.avatarColor}22` }, thread.avatarEmoji),
            el('div', { class: 'ag-chat-row__title' }, thread.title),
          ),
        ),
      ),
    ),
  );
  document.body.appendChild(overlay);
}

export function createChatRoomView(
  chatId: string,
  onBackMobile: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-chat-room' });
  let replyTarget: ReplyPreview | null = null;
  let openPanel: 'none' | 'emoji' | 'attach' = 'none';
  let draftText = '';
  let contextMenuEl: HTMLElement | null = null;

  const closeContextMenu = () => {
    if (contextMenuEl) {
      contextMenuEl.remove();
      contextMenuEl = null;
    }
  };

  const showMessageMenu = (msg: ChatMessage, anchorX: number, anchorY: number) => {
    closeContextMenu();
    const t = i18nStore.t;
    const menu = el('div', {
      class: 'ag-context-menu ag-card',
      style: `left:${Math.min(window.innerWidth - 210, Math.max(12, anchorX))}px;top:${Math.min(window.innerHeight - 240, Math.max(12, anchorY))}px;`,
    });

    const quickReacts = el(
      'div',
      { class: 'ag-quick-reacts' },
      ...['👍', '🔥', '❤️', '😂', '🛡️'].map((em) =>
        el(
          'button',
          {
            class: 'ag-quick-react-btn',
            onClick: () => {
              chatStore.toggleReaction(chatId, msg.id, em);
              closeContextMenu();
            },
          },
          em,
        ),
      ),
    );
    menu.appendChild(quickReacts);

    const addItem = (iconNode: SVGElement, label: string, onClick: () => void, danger = false) => {
      menu.appendChild(
        el(
          'button',
          {
            class: `ag-menu-item ${danger ? 'is-danger' : ''}`,
            onClick: () => {
              closeContextMenu();
              onClick();
            },
          },
          iconNode,
          el('span', null, label),
        ),
      );
    };

    addItem(icons.reply(16), t.chats.replyAction, () => {
      replyTarget = {
        messageId: msg.id,
        senderName: msg.senderName,
        snippet: msg.text || msg.attachment?.title || '📎',
      };
      render();
    });
    addItem(icons.forward(16), t.chats.forwardAction, () => openForwardModal(msg));
    if (msg.text) {
      addItem(icons.copy(16), t.chats.copyAction, async () => {
        await copyToClipboard(msg.text);
        showToast(t.common.copied);
      });
    }
    addItem(icons.trash(16), t.chats.deleteAction, () => chatStore.deleteMessage(chatId, msg.id), true);

    document.body.appendChild(menu);
    contextMenuEl = menu;
    const onDocClick = (e: Event) => {
      if (!menu.contains(e.target as Node)) {
        closeContextMenu();
        document.removeEventListener('mousedown', onDocClick);
      }
    };
    window.setTimeout(() => document.addEventListener('mousedown', onDocClick), 10);
  };

  const triggerFileAttach = (type: AttachmentType, accept: string) => {
    const input = document.createElement('input');
    input.type = 'file';
    if (accept) input.accept = accept;
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) return;
      const previewUrl =
        type === 'photo' || type === 'album' || type === 'music'
          ? URL.createObjectURL(file)
          : undefined;
      const sizeKb = Math.max(1, Math.round(file.size / 1024));
      const sizeLabel = sizeKb > 1024 ? `${(sizeKb / 1024).toFixed(1)} MB` : `${sizeKb} KB`;
      chatStore.sendMessage(chatId, draftText, replyTarget ?? undefined, {
        type,
        title: file.name,
        fileSizeLabel: sizeLabel,
        durationSec: type === 'music' ? 180 : undefined,
        previewUrl,
      });
      draftText = '';
      replyTarget = null;
      openPanel = 'none';
      render();
    };
    input.click();
  };

  const renderAttachmentBubble = (msg: ChatMessage, att: MessageAttachment): HTMLElement => {
    const isAudio = att.type === 'voice' || att.type === 'music';
    if (isAudio) {
      const playingThis = audioPlayer.currentTrack?.id === msg.id && audioPlayer.playing;
      return el(
        'div',
        {
          class: 'ag-att-audio',
          onClick: (e: Event) => {
            e.stopPropagation();
            audioPlayer.toggleTrack({
              id: msg.id,
              title: att.title,
              subtitle: msg.senderName,
              durationSec: att.durationSec ?? 24,
              url: att.previewUrl,
            });
          },
        },
        el('div', { class: 'ag-att-audio__btn' }, playingThis ? icons.pause(16) : icons.play(16)),
        el(
          'div',
          { class: 'ag-att-audio__meta' },
          el('strong', null, att.title),
          el('span', null, `${att.durationSec ?? 24}s`),
        ),
      );
    }

    if (att.previewUrl && (att.type === 'photo' || att.type === 'album')) {
      return el(
        'div',
        {
          class: 'ag-att-photo',
          onClick: (e: Event) => {
            e.stopPropagation();
            openMediaViewer(att);
          },
        },
        el('img', { src: att.previewUrl, alt: att.title }),
      );
    }

    return el(
      'div',
      {
        class: 'ag-att-card',
        onClick: (e: Event) => {
          e.stopPropagation();
          openMediaViewer(att);
        },
      },
      el('span', { class: 'ag-att-card__badge' }, att.type.toUpperCase()),
      el(
        'div',
        { class: 'ag-att-card__info' },
        el('strong', null, att.title),
        att.fileSizeLabel || att.subtitle ? el('span', null, att.fileSizeLabel ?? att.subtitle ?? '') : null,
      ),
    );
  };

  const miniPlayer = createMiniAudioPlayerBar();

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const thread = chatStore.getThread(chatId);
    if (!thread) return;

    // Header
    root.appendChild(
      el(
        'div',
        { class: 'ag-chat-header' },
        el(
          'button',
          { class: 'ag-icon-btn ag-mobile-only', onClick: onBackMobile },
          icons.back(22),
        ),
        el(
          'div',
          { class: 'ag-avatar ag-avatar--sm', style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55` },
          thread.avatarEmoji,
        ),
        el(
          'div',
          { class: 'ag-chat-header__meta' },
          el('div', { class: 'ag-chat-header__title' }, thread.title),
          el('div', { class: 'ag-chat-header__sub' }, thread.statusText),
        ),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: t.calls.startAudioCall,
            onClick: () => openActiveCallModal(thread.title, thread.avatarEmoji, false),
          },
          icons.phone(19),
        ),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: t.chats.pinAction,
            onClick: () => chatStore.togglePin(thread.id),
          },
          icons.pin(18),
        ),
      ),
    );

    root.appendChild(miniPlayer);

    // Messages
    const messagesScroll = el('div', { class: 'ag-messages-scroll' });
    const messages = chatStore.getMessages(chatId);
    for (const msg of messages) {
      const bubble = el(
        'div',
        {
          class: `ag-bubble ${msg.isOutgoing ? 'is-outgoing' : 'is-incoming'}`,
          onContextMenu: (e: Event) => {
            e.preventDefault();
            const me = e as MouseEvent;
            showMessageMenu(msg, me.clientX, me.clientY);
          },
        },
        msg.replyTo
          ? el(
              'div',
              { class: 'ag-reply-strip' },
              el('strong', null, msg.replyTo.senderName),
              el('span', null, msg.replyTo.snippet),
            )
          : null,
        msg.attachment ? renderAttachmentBubble(msg, msg.attachment) : null,
        msg.text ? el('div', { class: 'ag-bubble__text' }, msg.text) : null,
        el(
          'div',
          { class: 'ag-bubble__footer' },
          msg.reactions
            ? el(
                'div',
                { class: 'ag-reactions' },
                ...Object.entries(msg.reactions).map(([em, count]) =>
                  el(
                    'button',
                    {
                      class: 'ag-reaction-chip',
                      onClick: (e: Event) => {
                        e.stopPropagation();
                        chatStore.toggleReaction(chatId, msg.id, em);
                      },
                    },
                    `${em} ${count}`,
                  ),
                ),
              )
            : null,
          el('span', { class: 'ag-bubble__time' }, msg.timestamp),
          el(
            'button',
            {
              class: 'ag-bubble__more',
              onClick: (e: Event) => {
                e.stopPropagation();
                const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                showMessageMenu(msg, rect.left, rect.bottom + 6);
              },
            },
            icons.more(14),
          ),
        ),
      );

      messagesScroll.appendChild(
        el('div', { class: `ag-msg-row ${msg.isOutgoing ? 'is-outgoing' : 'is-incoming'}` }, bubble),
      );
    }
    root.appendChild(messagesScroll);
    requestAnimationFrame(() => {
      messagesScroll.scrollTop = messagesScroll.scrollHeight;
    });

    // Reply bar
    if (replyTarget) {
      root.appendChild(
        el(
          'div',
          { class: 'ag-reply-bar' },
          el(
            'div',
            { class: 'ag-reply-bar__body' },
            el('strong', null, `${t.chats.replyTo} ${replyTarget.senderName}`),
            el('span', null, replyTarget.snippet),
          ),
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => {
                replyTarget = null;
                render();
              },
            },
            icons.close(16),
          ),
        ),
      );
    }

    // Input bar
    const input = el('input', {
      type: 'text',
      class: 'ag-msg-input',
      placeholder: t.input.placeholder,
      value: draftText,
      onInput: (e: Event) => {
        draftText = (e.target as HTMLInputElement).value;
      },
      onKeyDown: (e: Event) => {
        const ke = e as KeyboardEvent;
        if (ke.key === 'Enter' && !ke.shiftKey) {
          ke.preventDefault();
          sendCurrent();
        }
      },
    }) as HTMLInputElement;

    const sendCurrent = () => {
      if (!draftText.trim()) return;
      chatStore.sendMessage(chatId, draftText, replyTarget ?? undefined);
      draftText = '';
      replyTarget = null;
      openPanel = 'none';
      render();
    };

    root.appendChild(
      el(
        'div',
        { class: 'ag-composer' },
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: t.input.attach,
            onClick: () => {
              openPanel = openPanel === 'attach' ? 'none' : 'attach';
              render();
            },
          },
          icons.attach(20),
        ),
        el(
          'div',
          { class: 'ag-composer__pill' },
          input,
          el(
            'button',
            {
              class: 'ag-icon-btn',
              title: t.input.emoji,
              onClick: () => {
                openPanel = openPanel === 'emoji' ? 'none' : 'emoji';
                render();
              },
            },
            icons.smile(20),
          ),
        ),
        el(
          'button',
          {
            class: 'ag-send-fab',
            title: t.input.send,
            onClick: () => {
              if (draftText.trim()) {
                sendCurrent();
              } else {
                chatStore.sendMessage(chatId, '', replyTarget ?? undefined, {
                  type: 'voice',
                  title: t.input.attachVoice,
                  durationSec: 7,
                });
                replyTarget = null;
                render();
              }
            },
          },
          draftText.trim() ? icons.send(18) : icons.mic(18),
        ),
      ),
    );

    if (openPanel === 'emoji') {
      root.appendChild(
        el(
          'div',
          { class: 'ag-drawer-panel' },
          el(
            'div',
            { class: 'ag-stickers-row' },
            ...STICKERS.map((st) =>
              el(
                'button',
                {
                  class: 'ag-sticker-btn',
                  onClick: () => {
                    chatStore.sendMessage(chatId, st, replyTarget ?? undefined);
                    replyTarget = null;
                    openPanel = 'none';
                    render();
                  },
                },
                st,
              ),
            ),
          ),
          el(
            'div',
            { class: 'ag-emoji-grid' },
            ...EMOJIS.map((em) =>
              el(
                'button',
                {
                  class: 'ag-emoji-btn',
                  onClick: () => {
                    draftText += em;
                    input.value = draftText;
                    input.focus();
                  },
                },
                em,
              ),
            ),
          ),
        ),
      );
    } else if (openPanel === 'attach') {
      const attachItems: Array<{ label: string; emoji: string; action: () => void }> = [
        { label: t.input.attachPhoto, emoji: '🖼️', action: () => triggerFileAttach('photo', 'image/*') },
        { label: t.input.attachAlbum, emoji: '🗂️', action: () => triggerFileAttach('album', 'image/*') },
        { label: t.input.attachFile, emoji: '📄', action: () => triggerFileAttach('file', '*/*') },
        { label: t.input.attachMusic, emoji: '🎵', action: () => triggerFileAttach('music', 'audio/*') },
        {
          label: t.input.attachVoice,
          emoji: '🎙️',
          action: () => {
            chatStore.sendMessage(chatId, '', undefined, { type: 'voice', title: t.input.attachVoice, durationSec: 12 });
            openPanel = 'none';
            render();
          },
        },
        {
          label: t.input.attachVideoCircle,
          emoji: '⭕',
          action: () => {
            chatStore.sendMessage(chatId, '', undefined, { type: 'video_circle', title: t.input.attachVideoCircle, durationSec: 9 });
            openPanel = 'none';
            render();
          },
        },
        {
          label: t.input.attachGeo,
          emoji: '📍',
          action: () => {
            chatStore.sendMessage(chatId, '', undefined, { type: 'geo', title: t.input.attachGeo, subtitle: 'Encrypted Relay Node' });
            openPanel = 'none';
            render();
          },
        },
        {
          label: t.input.attachContact,
          emoji: '👤',
          action: () => {
            chatStore.sendMessage(chatId, '', undefined, { type: 'contact', title: 'Ghost Contact', subtitle: '@ghost_node' });
            openPanel = 'none';
            render();
          },
        },
      ];

      root.appendChild(
        el(
          'div',
          { class: 'ag-drawer-panel ag-attach-grid' },
          ...attachItems.map((it) =>
            el(
              'button',
              { class: 'ag-attach-tile', onClick: it.action },
              el('span', { class: 'ag-attach-tile__icon' }, it.emoji),
              el('span', { class: 'ag-attach-tile__label' }, it.label),
            ),
          ),
        ),
      );
    }
  };

  const unsubChats = chatStore.subscribe(render);
  const unsubAudio = audioPlayer.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubChats();
    unsubAudio();
    unsubI18n();
    closeContextMenu();
  };

  return root;
}
