import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { audioPlayer } from '../../../player/audioPlayer';
import { chatStore } from '../../chatStore';
import type { ChatMessage, MessageAttachment } from '../../models';
import { openMediaViewer } from '../../viewer/MediaViewerOverlay';

export interface BubbleContext {
  chatId: string;
  selectionMode: boolean;
  selected: boolean;
  isDesktop: boolean;
  onContextMenu: (msg: ChatMessage, x: number, y: number) => void;
  onToggleSelect: (msg: ChatMessage) => void;
  onEnterSelect: (msg: ChatMessage) => void;
  onSwipeReply: (msg: ChatMessage) => void;
  onReplyJump: (messageId: string) => void;
}

function renderAttachment(msg: ChatMessage, att: MessageAttachment): HTMLElement {
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
}

const SWIPE_TRIGGER = 56;
const LONG_PRESS_MS = 420;

/** Message row: bubble + selection check + swipe-to-reply (touch) + context menu (right-click / long-press). */
export function createMessageRow(msg: ChatMessage, ctx: BubbleContext): HTMLElement {
  const dir = msg.isOutgoing ? 'is-outgoing' : 'is-incoming';

  const bubble = el(
    'div',
    { class: `ag-bubble ${dir}` },
    msg.replyTo
      ? el(
          'div',
          {
            class: 'ag-reply-strip',
            onClick: (e: Event) => {
              e.stopPropagation();
              ctx.onReplyJump(msg.replyTo!.messageId);
            },
          },
          el('strong', null, msg.replyTo.senderName),
          el('span', null, msg.replyTo.snippet),
        )
      : null,
    msg.attachment ? renderAttachment(msg, msg.attachment) : null,
    msg.text ? el('div', { class: 'ag-bubble__text' }, msg.text) : null,
    el(
      'div',
      { class: 'ag-bubble__footer' },
      msg.reactions && Object.keys(msg.reactions).length
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
                    chatStore.toggleReaction(ctx.chatId, msg.id, em);
                  },
                },
                `${em} ${count}`,
              ),
            ),
          )
        : null,
      el('span', { class: 'ag-bubble__time' }, msg.timestamp),
    ),
  );

  const check = el('span', { class: 'ag-cs-check' }, icons.check(14));
  const swipeIcon = el('span', { class: 'ag-cs-swipe-icon' }, icons.reply(18));
  const row = el(
    'div',
    {
      class: `ag-msg-row ${dir} ag-cs-row${ctx.selectionMode ? ' is-selecting' : ''}${ctx.selected ? ' is-selected' : ''}`,
      'data-msg-id': msg.id,
    },
    check,
    el('div', { class: 'ag-cs-bubble-wrap' }, swipeIcon, bubble),
  );

  // Click: selection toggle
  row.addEventListener('click', () => {
    if (ctx.selectionMode) ctx.onToggleSelect(msg);
  });

  // Desktop right click -> menu
  row.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    if (ctx.selectionMode) return;
    ctx.onContextMenu(msg, e.clientX, e.clientY);
  });

  // Touch: swipe-to-reply + long press
  let sx = 0;
  let sy = 0;
  let dx = 0;
  let tracking = false;
  let swiping = false;
  let pressTimer = 0;
  const wrap = bubble.parentElement as HTMLElement;
  const cancelPress = () => {
    if (pressTimer) {
      window.clearTimeout(pressTimer);
      pressTimer = 0;
    }
  };
  row.addEventListener(
    'touchstart',
    (e) => {
      const t = e.touches[0];
      sx = t.clientX;
      sy = t.clientY;
      dx = 0;
      tracking = true;
      swiping = false;
      cancelPress();
      pressTimer = window.setTimeout(() => {
        pressTimer = 0;
        tracking = false;
        if (ctx.selectionMode) return;
        if (navigator.vibrate) navigator.vibrate(12);
        ctx.onContextMenu(msg, sx, sy);
      }, LONG_PRESS_MS);
    },
    { passive: true },
  );
  row.addEventListener(
    'touchmove',
    (e) => {
      if (!tracking) return;
      const t = e.touches[0];
      const mx = t.clientX - sx;
      const my = t.clientY - sy;
      if (!swiping && (Math.abs(mx) > 8 || Math.abs(my) > 8)) cancelPress();
      if (!swiping && Math.abs(my) > Math.abs(mx)) {
        tracking = false;
        return;
      }
      if (ctx.selectionMode) return;
      if (mx < -8) swiping = true;
      if (swiping) {
        dx = Math.max(-80, Math.min(0, mx));
        wrap.style.transform = `translateX(${dx}px)`;
        row.style.setProperty('--ag-swipe-p', String(Math.min(1, -dx / SWIPE_TRIGGER)));
      }
    },
    { passive: true },
  );
  const end = () => {
    cancelPress();
    if (swiping && -dx >= SWIPE_TRIGGER) ctx.onSwipeReply(msg);
    wrap.style.transform = '';
    row.style.removeProperty('--ag-swipe-p');
    tracking = false;
    swiping = false;
    dx = 0;
  };
  row.addEventListener('touchend', end);
  row.addEventListener('touchcancel', end);

  return row;
}
