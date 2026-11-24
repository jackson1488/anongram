import { clear, el } from '../../../../core/ui/dom';
import { i18nStore } from '../../../../core/i18n/i18nStore';
import type { ChatFolder } from '../../models';

export function renderFolderChips(
  row: HTMLElement,
  current: ChatFolder,
  onSelect: (f: ChatFolder) => void,
): void {
  const t = i18nStore.t;
  clear(row);
  const folders: Array<{ id: ChatFolder; label: string }> = [
    { id: 'all', label: t.chats.folderAll },
    { id: 'unread', label: t.chats.folderUnread },
    { id: 'pinned', label: t.chats.folderPinned },
  ];
  for (const f of folders) {
    const active = current === f.id;
    row.appendChild(
      el(
        'button',
        {
          type: 'button',
          class: `ag-chip ${active ? 'is-active' : ''}`,
          'aria-pressed': String(active),
          onClick: () => onSelect(f.id),
        },
        f.label,
      ),
    );
  }
}
