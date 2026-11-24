import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { mainMenuT } from '../i18n';

export interface SearchRowHandle {
  root: HTMLElement;
  input: HTMLInputElement;
  refreshLabels: () => void;
}

/** Persistent search row: input is created once and never re-created (keeps focus). */
export function createSearchRow(
  onQuery: (q: string) => void,
  onOpenMenu?: () => void,
): SearchRowHandle {
  const input = el('input', {
    type: 'text',
    'aria-label': i18nStore.t.chats.searchPlaceholder,
  }) as HTMLInputElement;
  const clearBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn',
      title: i18nStore.t.common.close,
      'aria-label': i18nStore.t.common.close,
      style: 'width:28px;height:28px;display:none;',
      onClick: () => {
        input.value = '';
        clearBtn.style.display = 'none';
        onQuery('');
        input.focus();
      },
    },
    icons.close(14),
  );
  input.addEventListener('input', () => {
    clearBtn.style.display = input.value ? 'inline-flex' : 'none';
    onQuery(input.value);
  });
  const root = el('div', { class: 'ag-search-wrap ag-search-row' });
  const menuBtn = onOpenMenu
    ? el(
        'button',
        {
          type: 'button',
          class: 'ag-icon-btn',
          title: mainMenuT().menu,
          'aria-label': mainMenuT().menu,
          onClick: () => onOpenMenu(),
        },
        icons.menu(22),
      )
    : null;
  if (menuBtn) root.appendChild(menuBtn);
  root.appendChild(
    el('div', { class: 'ag-search-pill', style: 'flex:1;min-width:0;' }, icons.search(16), input, clearBtn),
  );
  return {
    root,
    input,
    refreshLabels: () => {
      const mt = mainMenuT();
      const t = i18nStore.t;
      input.setAttribute('aria-label', t.chats.searchPlaceholder);
      clearBtn.title = t.common.close;
      clearBtn.setAttribute('aria-label', t.common.close);
      clearBtn.style.display = input.value ? 'inline-flex' : 'none';
      if (menuBtn) {
        menuBtn.title = mt.menu;
        menuBtn.setAttribute('aria-label', mt.menu);
      }
    },
  };
}

