import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { draftsStore } from '../../../drafts/draftsStore';
import { chatStore } from '../../chatStore';
import type { ChatThread } from '../../models';
import { mainMenuT } from '../i18n';
import { createChatAvatar } from './ChatAvatar';
import { createTrailingBadges } from './ChatTrailingBadges';

const SWIPE_SLOP_PX = 8;
const SWIPE_THRESHOLD_PX = 84;
const MAX_SWIPE_PX = 128;

export interface ChatListItemOptions {
  onSwipeArchive?: (() => void) | null;
}

function attachSwipeToArchive(
  row: HTMLElement,
  swipeBg: HTMLElement,
  selected: boolean,
  onClick: () => void,
  onSwipeArchive: () => void,
): void {
  let sx = 0;
  let sy = 0;
  let dx = 0;
  let tracking = false;
  let swiping = false;
  let suppressClick = false;
  let hapticFired = false;
  let resetTimer = 0;
  let clickResetTimer = 0;

  row.addEventListener('click', (e) => {
    if (suppressClick) {
      e.preventDefault();
      e.stopPropagation();
      suppressClick = false;
      return;
    }
    onClick();
  });

  row.addEventListener(
    'touchstart',
    (e: TouchEvent) => {
      suppressClick = false;
      if (window.innerWidth >= 840 || e.touches.length !== 1) {
        tracking = false;
        return;
      }
      if (resetTimer) {
        window.clearTimeout(resetTimer);
        resetTimer = 0;
      }
      const t = e.touches[0];
      sx = t.clientX;
      sy = t.clientY;
      dx = 0;
      tracking = true;
      swiping = false;
      hapticFired = false;
    },
    { passive: true },
  );

  row.addEventListener(
    'touchmove',
    (e: TouchEvent) => {
      if (!tracking || e.touches.length !== 1) return;
      const t = e.touches[0];
      const mx = t.clientX - sx;
      const my = t.clientY - sy;

      if (!swiping) {
        if (Math.abs(my) > SWIPE_SLOP_PX && Math.abs(my) >= Math.abs(mx)) {
          tracking = false;
          return;
        }
        if (mx > SWIPE_SLOP_PX) {
          tracking = false;
          return;
        }
        if (mx < -SWIPE_SLOP_PX && Math.abs(mx) > Math.abs(my) * 1.2) {
          swiping = true;
          suppressClick = true;
          row.style.transition = 'none';
        }
      }

      if (!swiping) return;
      dx = Math.max(-MAX_SWIPE_PX, Math.min(0, mx));
      row.style.transform = `translate3d(${dx}px, 0, 0)`;
      row.style.background = selected ? 'var(--ag-accent-soft)' : 'var(--ag-bg)';

      if (dx < -4) {
        const isReady = dx <= -SWIPE_THRESHOLD_PX;
        swipeBg.style.display = 'flex';
        swipeBg.style.opacity = isReady ? '0.96' : '0.72';
        if (isReady && !hapticFired) {
          hapticFired = true;
          navigator.vibrate?.(10);
        } else if (!isReady) {
          hapticFired = false;
        }
      } else {
        swipeBg.style.display = 'none';
      }
    },
    { passive: true },
  );

  const onEnd = (e: TouchEvent) => {
    const wasSwiping = swiping;
    const shouldTrigger = e.type === 'touchend' && wasSwiping && dx <= -SWIPE_THRESHOLD_PX;
    tracking = false;
    swiping = false;
    hapticFired = false;
    dx = 0;

    if (!wasSwiping) return;

    row.style.transition = 'transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1)';
    row.style.transform = 'translate3d(0, 0, 0)';

    if (clickResetTimer) window.clearTimeout(clickResetTimer);
    clickResetTimer = window.setTimeout(() => {
      suppressClick = false;
      clickResetTimer = 0;
    }, 140);

    resetTimer = window.setTimeout(() => {
      if (!swiping) {
        swipeBg.style.display = 'none';
        row.style.background = '';
        row.style.transition = '';
      }
      resetTimer = 0;
    }, 220);

    if (shouldTrigger) {
      onSwipeArchive();
    }
  };

  row.addEventListener('touchend', onEnd, { passive: true });
  row.addEventListener('touchcancel', onEnd, { passive: true });
}

export function createChatListItemCard(
  thread: ChatThread,
  selected: boolean,
  onClick: () => void,
  options?: ChatListItemOptions,
): HTMLElement {
  const mt = mainMenuT();
  const mobile = window.innerWidth < 840;
  const last = chatStore.getLastMessage(thread.id);
  const draft = draftsStore.getDraft(thread.id);
  const preview = draft
    ? el(
        'span',
        { class: 'ag-chat-row__preview' },
        el('span', { class: 'ag-draft-prefix' }, mt.draft),
        draft.text || draft.replySnippet || mt.draftReply,
      )
    : el(
        'span',
        { class: 'ag-chat-row__preview' },
        last
          ? last.attachment && !last.text
            ? `📎 ${last.attachment.title}`
            : last.text
          : i18nStore.t.chats.noMessages,
      );

  const pinEl =
    mobile && thread.isPinned
      ? el(
          'span',
          {
            class: 'ag-pin-icon ag-chat-row__pin',
            'aria-hidden': 'true',
            style:
              'display:inline-flex;align-items:center;flex-shrink:0;color:var(--ag-accent);opacity:0.85;transform:rotate(35deg);',
          },
          icons.pin(13),
        )
      : null;

  const swipeHandler =
    options?.onSwipeArchive === null
      ? null
      : (options?.onSwipeArchive ?? (() => chatStore.toggleArchive(thread.id)));

  const row = el(
    'div',
    {
      class: `ag-chat-row ${selected ? 'is-selected' : ''}`,
      style: mobile && swipeHandler ? 'position:relative;z-index:1;touch-action:pan-y;' : undefined,
      onClick: mobile && swipeHandler ? undefined : onClick,
    },
    createChatAvatar(thread),
    el(
      'div',
      { class: 'ag-chat-row__body' },
      el(
        'div',
        { class: 'ag-chat-row__top' },
        el(
          'div',
          { style: 'display:flex;align-items:center;gap:5px;min-width:0;flex:1;' },
          pinEl,
          el('span', { class: 'ag-chat-row__title', style: 'min-width:0;' }, thread.title),
          thread.isOtpBurn ? el('span', { class: 'ag-otp-badge', style: 'flex-shrink:0;' }, mt.otp) : null,
        ),
        el('span', { class: 'ag-chat-row__time' }, last?.timestamp ?? ''),
      ),
      el('div', { class: 'ag-chat-row__bottom' }, preview, createTrailingBadges(thread)),
    ),
  );

  if (!mobile || !swipeHandler) {
    return row;
  }

  const swipeBg = el(
    'div',
    {
      class: 'ag-chat-swipe-bg',
      'aria-hidden': 'true',
      style:
        'position:absolute;inset:0;display:none;align-items:center;justify-content:flex-end;padding:0 20px;background:var(--ag-accent);color:var(--ag-on-accent);border-radius:18px;pointer-events:none;',
    },
    el(
      'div',
      { style: 'display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;' },
      thread.isArchived ? tgIcons.upload(22) : icons.archive(22),
      el(
        'span',
        { style: 'font-size:11.5px;font-weight:600;line-height:1.1;' },
        thread.isArchived ? mt.restore : mt.toArchive,
      ),
    ),
  );

  attachSwipeToArchive(row, swipeBg, selected, onClick, swipeHandler);

  return el(
    'div',
    {
      class: 'ag-chat-swipe-wrap',
      style: 'position:relative;overflow:hidden;border-radius:18px;flex-shrink:0;',
    },
    swipeBg,
    row,
  );
}

