import { i18nStore } from '../../../core/i18n/i18nStore';
import { openMenu } from '../../../core/ui/Menu';
import { clear, el } from '../../../core/ui/dom';
import { allIcons as icons } from '../../../core/ui/icons';
import { iconButton, pillButton, screenSurface } from '../../../core/ui/components';
import { showToast } from '../../../core/ui/toast';
import { chatStore } from '../chatStore';
import type { ChatThread } from '../models';
import { openArchiveInfoBottomSheet } from './ArchiveInfoBottomSheet';
import { archiveT } from './i18n/ArchiveI18n';
import './archive.css';

export function createArchiveScreen(
  onBack: () => void,
  onOpenChat: (chatId: string) => void,
  onOpenSettings: () => void,
): HTMLElement {
  const root = screenSurface();
  let query = '';
  let selectionMode = false;
  const selectedIds = new Set<string>();

  const headerEl = el('div', { class: 'ag-archive-header' });
  const searchWrap = el('div', { class: 'ag-archive-search-wrap' });
  const listWrap = el('div', { class: 'ag-archive-list' });

  const searchInput = el('input', {
    type: 'text',
    oninput: (e: Event) => {
      query = (e.target as HTMLInputElement).value;
      renderList();
      updateClearBtn();
    },
  }) as HTMLInputElement;

  const clearBtn = iconButton(
    icons.close(16),
    () => {
      query = '';
      searchInput.value = '';
      renderList();
      updateClearBtn();
    },
    '',
  );
  clearBtn.style.width = '28px';
  clearBtn.style.height = '28px';
  clearBtn.style.display = 'none';

  function updateClearBtn(): void {
    clearBtn.style.display = query.length > 0 ? 'inline-flex' : 'none';
  }

  const searchPill = el(
    'div',
    { class: 'ag-search-pill' },
    icons.search(18),
    searchInput,
    clearBtn,
  );
  searchWrap.appendChild(searchPill);

  function exitSelection(): void {
    selectionMode = false;
    selectedIds.clear();
    renderHeader();
    renderList();
  }

  function unarchiveSelected(): void {
    const ids = [...selectedIds];
    if (ids.length === 0) return;
    for (const id of ids) {
      const t = chatStore.getThread(id);
      if (t?.isArchived) chatStore.toggleArchive(id);
    }
    showToast(archiveT().unarchivedToast(ids.length));
    exitSelection();
  }

  function renderHeader(): void {
    clear(headerEl);
    const s = archiveT();
    const archivedCount = chatStore.threads.filter((t) => t.isArchived).length;

    if (selectionMode) {
      headerEl.appendChild(iconButton(icons.close(20), exitSelection, s.cancelSelection));
      headerEl.appendChild(
        el('div', { class: 'ag-archive-header__title' }, s.selectedCount(selectedIds.size)),
      );
      if (selectedIds.size > 0) {
        headerEl.appendChild(
          pillButton(s.unarchive, unarchiveSelected, 'primary', icons.upload(16)),
        );
      }
      return;
    }

    headerEl.appendChild(iconButton(icons.arrowLeft(20), onBack, s.back));
    headerEl.appendChild(el('div', { class: 'ag-archive-header__title' }, s.archiveTitle));
    if (archivedCount > 0) {
      headerEl.appendChild(el('span', { class: 'ag-archive-header__count' }, String(archivedCount)));
    }

    const moreBtn = iconButton(icons.more(20), () => {
      openMenu(moreBtn, [
        {
          label: s.archiveSettingsMenu,
          icon: () => icons.settings(18),
          onClick: onOpenSettings,
        },
        {
          label: s.howItWorksMenu,
          icon: () => icons.help(18),
          onClick: () => openArchiveInfoBottomSheet(onOpenSettings),
        },
        ...(archivedCount > 0
          ? [
              { separator: true },
              {
                label: s.selectChats,
                icon: () => icons.select(18),
                onClick: () => {
                  selectionMode = true;
                  selectedIds.clear();
                  renderHeader();
                  renderList();
                },
              },
            ]
          : []),
      ]);
    });
    headerEl.appendChild(moreBtn);
  }

  function renderRow(thread: ChatThread): HTMLElement {
    const s = archiveT();
    const last = chatStore.getLastMessage(thread.id);
    const isSelected = selectedIds.has(thread.id);

    const row = el('div', {
      class: 'ag-archive-row' + (selectionMode && isSelected ? ' is-selected' : ''),
      onclick: () => {
        if (selectionMode) {
          if (selectedIds.has(thread.id)) selectedIds.delete(thread.id);
          else selectedIds.add(thread.id);
          renderHeader();
          renderList();
        } else {
          onOpenChat(thread.id);
        }
      },
      oncontextmenu: (e: Event) => {
        e.preventDefault();
        const me = e as MouseEvent;
        openMenu(null, [
          {
            label: s.openChat,
            icon: () => icons.chats(18),
            onClick: () => onOpenChat(thread.id),
          },
          {
            label: s.unarchive,
            icon: () => icons.upload(18),
            onClick: () => {
              chatStore.toggleArchive(thread.id);
              showToast(s.unarchivedToast(1));
            },
          },
        ], { x: me.clientX, y: me.clientY });
      },
    });

    if (selectionMode) {
      row.appendChild(
        el('div', { class: 'ag-archive-row__check' }, isSelected ? icons.check(14) : null),
      );
    }

    row.appendChild(
      el(
        'div',
        {
          class: 'ag-archive-row__avatar',
          style: `background:${thread.avatarColor}22`,
        },
        thread.avatarEmoji,
      ),
    );

    const previewText = last?.text || last?.attachment?.title || thread.statusText;
    row.appendChild(
      el(
        'div',
        { class: 'ag-archive-row__body' },
        el(
          'div',
          { class: 'ag-archive-row__top' },
          el('div', { class: 'ag-archive-row__title' }, thread.title),
          last ? el('div', { class: 'ag-archive-row__time' }, last.timestamp) : null,
        ),
        el('div', { class: 'ag-archive-row__preview' }, previewText),
      ),
    );

    if (!selectionMode) {
      const unarchiveBtn = iconButton(
        icons.upload(18),
        (e: Event) => {
          e.stopPropagation();
          chatStore.toggleArchive(thread.id);
          showToast(s.unarchivedToast(1));
        },
        s.unarchive,
      );
      row.appendChild(unarchiveBtn);
    }

    return row;
  }

  function renderList(): void {
    clear(listWrap);
    const s = archiveT();
    searchInput.placeholder = s.searchChatsPlaceholder;

    const allArchived = chatStore.threads.filter((t) => t.isArchived);
    const q = query.trim().toLowerCase();
    const filtered = q
      ? allArchived.filter((t) => {
          const lastText = chatStore.getLastMessage(t.id)?.text.toLowerCase() ?? '';
          return t.title.toLowerCase().includes(q) || lastText.includes(q);
        })
      : allArchived;

    if (filtered.length === 0) {
      listWrap.appendChild(
        el(
          'div',
          { class: 'ag-archive-empty' },
          el('div', { class: 'ag-archive-empty__icon' }, icons.archive(30)),
          el(
            'div',
            { class: 'ag-archive-empty__title' },
            q ? s.nothingFound : s.archiveEmptyTitle,
          ),
          !q ? el('div', { class: 'ag-archive-empty__hint' }, s.archiveEmptyHint) : null,
        ),
      );
      return;
    }

    for (const thread of filtered) {
      listWrap.appendChild(renderRow(thread));
    }
  }

  const renderAll = () => {
    renderHeader();
    renderList();
  };

  root.append(headerEl, searchWrap, listWrap);
  renderAll();

  const unsubChats = chatStore.subscribe(renderAll);
  const unsubI18n = i18nStore.subscribe(renderAll);

  (root as HTMLElement & { destroy?: () => void }).destroy = () => {
    unsubChats();
    unsubI18n();
  };

  return root;
}
