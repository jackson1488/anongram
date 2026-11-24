import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { audioPlayer } from '../../../player/audioPlayer';
import { chatStore } from '../../chatStore';
import { chatScreenT } from '../../i18n';
import type { ChatMessage, MessageAttachment } from '../../models';
import { openDocumentViewer, openMediaViewer } from '../../viewer/MediaViewerOverlay';

export interface BubbleContext {
  chatId: string;
  selectionMode: boolean;
  selected: boolean;
  isDesktop: boolean;
  isGroupFirst?: boolean;
  isGroupLast?: boolean;
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
          openMediaViewer(att, undefined, undefined, msg);
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
        if (att.type === 'file') {
          openDocumentViewer(att, msg);
        } else {
          openMediaViewer(att, undefined, undefined, msg);
        }
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

function makeHoverActionButton(
  cls: string,
  label: string,
  icon: SVGElement,
  onClick: (e: Event, btn: HTMLButtonElement) => void,
): HTMLButtonElement {
  const baseBg = 'color-mix(in srgb, var(--ag-surface) 84%, transparent)';
  const btn = el(
    'button',
    {
      type: 'button',
      class: cls,
      title: label,
      'aria-label': label,
      style: [
        'width:28px',
        'height:28px',
        'border-radius:9999px',
        'display:inline-flex',
        'align-items:center',
        'justify-content:center',
        `background:${baseBg}`,
        'border:1px solid var(--ag-island-border)',
        'color:var(--ag-text-secondary)',
        'backdrop-filter:blur(8px)',
        '-webkit-backdrop-filter:blur(8px)',
        'transition:background-color 0.12s ease,color 0.12s ease,transform 0.12s ease',
      ].join(';'),
    },
    icon,
  ) as HTMLButtonElement;

  btn.addEventListener('mouseenter', () => {
    btn.style.background = 'var(--ag-accent-soft)';
    btn.style.color = 'var(--ag-accent)';
  });
  btn.addEventListener('mouseleave', () => {
    btn.style.background = baseBg;
    btn.style.color = 'var(--ag-text-secondary)';
  });
  btn.addEventListener('click', (e) => onClick(e, btn));
  return btn;
}

function createDesktopHoverActions(
  msg: ChatMessage,
  ctx: BubbleContext,
): { wrap: HTMLElement; setVisible: (visible: boolean) => void } {
  const replyLabel = i18nStore.t.chats.replyAction;
  const moreLabel = chatScreenT().more;

  const quickReplyBtn = makeHoverActionButton(
    'ag-cs-hover-btn ag-cs-quick-reply',
    replyLabel,
    icons.reply(15),
    (e) => {
      e.stopPropagation();
      ctx.onSwipeReply(msg);
    },
  );
  const moreBtn = makeHoverActionButton(
    'ag-cs-hover-btn ag-cs-hover-more',
    moreLabel,
    icons.more(15),
    (e, btn) => {
      e.stopPropagation();
      const r = btn.getBoundingClientRect();
      ctx.onContextMenu(msg, r.left, r.bottom + 4);
    },
  );

  const wrap = el(
    'div',
    {
      class: 'ag-cs-hover-actions',
      style: [
        'display:inline-flex',
        'align-items:center',
        'gap:4px',
        'margin:0 6px',
        'align-self:center',
        'flex-shrink:0',
        'opacity:0',
        'pointer-events:none',
        'transform:scale(0.94)',
        'transition:opacity 0.14s ease,transform 0.14s ease',
      ].join(';'),
    },
    quickReplyBtn,
    moreBtn,
  );

  const setVisible = (visible: boolean) => {
    wrap.style.opacity = visible ? '1' : '0';
    wrap.style.pointerEvents = visible ? 'auto' : 'none';
    wrap.style.transform = visible ? 'scale(1)' : 'scale(0.94)';
  };

  return { wrap, setVisible };
}

const SWIPE_TRIGGER = 56;
const LONG_PRESS_MS = 420;

/** Message row: bubble + grouping corners + desktop hover quick-reply + selection check + swipe/context menu. */
export function createMessageRow(msg: ChatMessage, ctx: BubbleContext): HTMLElement {
  const dir = msg.isOutgoing ? 'is-outgoing' : 'is-incoming';
  const isFirst = ctx.isGroupFirst ?? true;
  const isLast = ctx.isGroupLast ?? true;
  const groupClass =
    isFirst && !isLast
      ? 'is-group-first'
      : !isFirst && !isLast
        ? 'is-group-mid'
        : !isFirst && isLast
          ? 'is-group-last'
          : '';
  const groupSuffix = groupClass ? ` ${groupClass}` : '';

  const bubble = el(
    'div',
    { class: `ag-bubble ${dir}${groupSuffix}` },
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
  const hoverActions = ctx.isDesktop && !ctx.selectionMode ? createDesktopHoverActions(msg, ctx) : null;

  const bubbleWrap = msg.isOutgoing
    ? el('div', { class: 'ag-cs-bubble-wrap' }, swipeIcon, hoverActions?.wrap, bubble)
    : el('div', { class: 'ag-cs-bubble-wrap' }, swipeIcon, bubble, hoverActions?.wrap);

  const row = el(
    'div',
    {
      class: `ag-msg-row ${dir}${groupSuffix} ag-cs-row${ctx.selectionMode ? ' is-selecting' : ''}${ctx.selected ? ' is-selected' : ''}`,
      'data-msg-id': msg.id,
    },
    check,
    bubbleWrap,
  );

  if (hoverActions) {
    row.addEventListener('mouseenter', () => hoverActions.setVisible(true));
    row.addEventListener('mouseleave', () => hoverActions.setVisible(false));
    row.addEventListener('focusin', () => hoverActions.setVisible(true));
    row.addEventListener('focusout', (e) => {
      if (!row.contains(e.relatedTarget as Node | null)) {
        hoverActions.setVisible(false);
      }
    });
  }

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
  const wrap = bubbleWrap;
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
