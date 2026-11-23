import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { allIcons as icons } from '../../../core/ui/icons';
import { iconButton, pillButton } from '../../../core/ui/components';
import { showToast } from '../../../core/ui/toast';
import { chatStore } from '../chatStore';
import type { ChatMessage, ChatThread } from '../models';
import { pendingForwardStore } from './PendingForwardStore';
import { openForwardPreviewOverlay } from './forward/ForwardPreviewOverlay';
import { toolsT } from './i18n/ToolsI18n';
import './tools.css';

export interface ForwardTargetPickerHandle {
  close(): void;
}

export function openForwardTargetPicker(opts: {
  messages: ChatMessage[];
  onDone?: () => void;
  onClose?: () => void;
}): ForwardTargetPickerHandle {
  pendingForwardStore.start(opts.messages);

  let query = '';
  const selectedIds = new Set<string>();
  let comment = '';
  let closed = false;

  const backdrop = el('div', { class: 'ag-fwd-modal-backdrop' });
  const screen = el('div', { class: 'ag-fwd-screen' });
  backdrop.appendChild(screen);

  const headerEl = el('div', { class: 'ag-fwd-header' });
  const bannerEl = el('div', { class: 'ag-fwd-anon-banner' });
  const searchWrap = el('div', { class: 'ag-fwd-search' });
  const chipsEl = el('div', { class: 'ag-fwd-chips' });
  const listEl = el('div', { class: 'ag-fwd-list' });
  const footerEl = el('div', { class: 'ag-fwd-footer' });

  const searchInput = el('input', {
    type: 'text',
    oninput: (e: Event) => {
      query = (e.target as HTMLInputElement).value;
      clearBtn.style.display = query.length > 0 ? 'inline-flex' : 'none';
      renderList();
    },
  }) as HTMLInputElement;

  const clearBtn = iconButton(
    icons.close(16),
    () => {
      query = '';
      searchInput.value = '';
      clearBtn.style.display = 'none';
      renderList();
    },
    '',
  );
  clearBtn.style.width = '28px';
  clearBtn.style.height = '28px';
  clearBtn.style.display = 'none';

  searchWrap.appendChild(
    el('div', { class: 'ag-search-pill' }, icons.search(18), searchInput, clearBtn),
  );

  const commentInput = el('input', {
    type: 'text',
    class: 'ag-fwd-comment',
    oninput: (e: Event) => {
      comment = (e.target as HTMLInputElement).value;
      pendingForwardStore.setComment(comment);
    },
  }) as HTMLInputElement;

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };

  function close(): void {
    if (closed) return;
    closed = true;
    unsubPending();
    unsubChats();
    unsubI18n();
    document.removeEventListener('keydown', onKey);
    pendingForwardStore.clear();
    backdrop.remove();
    opts.onClose?.();
  }

  backdrop.addEventListener('mousedown', (e) => {
    if (e.target === backdrop) close();
  });
  document.addEventListener('keydown', onKey);

  function executeForward(): void {
    const activeMessages = pendingForwardStore.getActiveMessages();
    if (selectedIds.size === 0 || activeMessages.length === 0) return;

    const trimmedComment = comment.trim();
    for (const targetChatId of selectedIds) {
      for (const msg of activeMessages) {
        // AnonGram Core Protocol: 100% anonymous forwarding (forwardedFrom = null)
        chatStore.forwardAnonymously(targetChatId, msg);
      }
      if (trimmedComment) {
        chatStore.sendMessage(targetChatId, trimmedComment);
      }
    }

    showToast(toolsT().forwardedToast(activeMessages.length));
    const onDone = opts.onDone;
    close();
    onDone?.();
  }

  function toggleTarget(chatId: string): void {
    if (selectedIds.has(chatId)) selectedIds.delete(chatId);
    else selectedIds.add(chatId);
    renderHeader();
    renderChips();
    renderList();
    renderFooter();
  }

  function renderHeader(): void {
    clear(headerEl);
    const s = toolsT();
    const activeCount = pendingForwardStore.getActiveMessages().length;

    headerEl.appendChild(iconButton(icons.arrowLeft(20), close, s.back));
    headerEl.appendChild(
      el(
        'div',
        { class: 'ag-fwd-header__title' },
        selectedIds.size > 0 ? s.selectedTargetsCount(selectedIds.size) : s.forwardTitle,
      ),
    );
    if (activeCount > 0) {
      headerEl.appendChild(
        pillButton(
          s.previewButton(activeCount),
          () => openForwardPreviewOverlay({ onCancelAll: close }),
          'ghost',
        ),
      );
    }
  }

  function renderBanner(): void {
    clear(bannerEl);
    const s = toolsT();
    bannerEl.append(icons.shield(16), el('span', {}, `${s.anonymousForwardBadge} · ${s.anonymousForwardHint}`));
  }

  function renderChips(): void {
    clear(chipsEl);
    if (selectedIds.size === 0) {
      chipsEl.style.display = 'none';
      return;
    }
    chipsEl.style.display = 'flex';
    const s = toolsT();

    for (const id of selectedIds) {
      const thread = chatStore.getThread(id);
      const label = thread?.isSavedMessages ? s.savedMessagesTitle : (thread?.title ?? id);
      const chipEl = el(
        'span',
        { class: 'ag-fwd-chip' },
        el('span', {}, label),
        el(
          'button',
          {
            type: 'button',
            class: 'ag-fwd-chip__remove',
            onclick: () => toggleTarget(id),
          },
          icons.close(12),
        ),
      );
      chipsEl.appendChild(chipEl);
    }
  }

  function renderRow(thread: ChatThread, isSavedRow = false): HTMLElement {
    const s = toolsT();
    const isSelected = selectedIds.has(thread.id);
    const title = isSavedRow ? s.savedMessagesTitle : thread.title;
    const sub = isSavedRow ? s.savedMessagesSubtitle : thread.statusText;

    return el(
      'div',
      {
        class: 'ag-fwd-row' + (isSelected ? ' is-selected' : ''),
        onclick: () => toggleTarget(thread.id),
      },
      isSavedRow
        ? el('div', { class: 'ag-fwd-row__avatar ag-fwd-row__avatar--saved' }, icons.bookmark(20))
        : el(
            'div',
            {
              class: 'ag-fwd-row__avatar',
              style: `background:${thread.avatarColor}22`,
            },
            thread.avatarEmoji,
          ),
      el(
        'div',
        { class: 'ag-fwd-row__body' },
        el('div', { class: 'ag-fwd-row__title' }, title),
        el('div', { class: 'ag-fwd-row__sub' }, sub),
      ),
      el('div', { class: 'ag-fwd-row__check' }, isSelected ? icons.check(14) : null),
    );
  }

  function renderList(): void {
    clear(listEl);
    const s = toolsT();
    searchInput.placeholder = s.searchChatsPlaceholder;

    const q = query.trim().toLowerCase();
    const allThreads = chatStore.threads;
    const savedThread = allThreads.find((t) => t.isSavedMessages);
    const otherThreads = allThreads.filter((t) => !t.isSavedMessages);

    const showSaved =
      savedThread &&
      (!q ||
        savedThread.title.toLowerCase().includes(q) ||
        s.savedMessagesTitle.toLowerCase().includes(q) ||
        s.savedMessagesSubtitle.toLowerCase().includes(q));

    const filteredOthers = q
      ? otherThreads.filter(
          (t) =>
            t.title.toLowerCase().includes(q) ||
            t.statusText.toLowerCase().includes(q),
        )
      : otherThreads;

    if (!showSaved && filteredOthers.length === 0) {
      listEl.appendChild(el('div', { class: 'ag-fwd-empty' }, s.nothingFound));
      return;
    }

    if (showSaved && savedThread) {
      listEl.appendChild(renderRow(savedThread, true));
    }
    for (const thread of filteredOthers) {
      listEl.appendChild(renderRow(thread, false));
    }
  }

  function renderFooter(): void {
    clear(footerEl);
    if (selectedIds.size === 0) {
      footerEl.style.display = 'none';
      return;
    }
    footerEl.style.display = 'flex';
    const s = toolsT();
    commentInput.placeholder = s.addCommentPlaceholder;

    footerEl.append(
      commentInput,
      pillButton(s.sendButton(selectedIds.size), executeForward, 'primary', icons.send(16)),
    );
  }

  const renderAll = () => {
    if (!pendingForwardStore.getState()) {
      close();
      return;
    }
    renderHeader();
    renderBanner();
    renderChips();
    renderList();
    renderFooter();
  };

  screen.append(headerEl, bannerEl, searchWrap, chipsEl, listEl, footerEl);
  renderAll();

  const unsubPending = pendingForwardStore.subscribe(() => {
    if (!pendingForwardStore.getState()) close();
    else renderHeader();
  });
  const unsubChats = chatStore.subscribe(renderList);
  const unsubI18n = i18nStore.subscribe(renderAll);

  document.body.appendChild(backdrop);

  return { close };
}
