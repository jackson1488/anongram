import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { openActiveCallModal } from './ActiveCallOverlay';
import { callsStore, formatDuration } from './CallsStore';
import { callsT } from './i18n';
import './calls.css';

export { openActiveCallModal };

function formatCallTime(timestampMs: number): string {
  const s = callsT();
  const d = new Date(timestampMs);
  const now = new Date();
  const hhmm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  if (d.toDateString() === now.toDateString()) {
    return `${s.today}, ${hhmm}`;
  }
  const yest = new Date(now);
  yest.setDate(now.getDate() - 1);
  if (d.toDateString() === yest.toDateString()) {
    return `${s.yesterday}, ${hhmm}`;
  }
  const locale = i18nStore.language === 'ru' ? 'ru-RU' : 'en-US';
  return d.toLocaleDateString(locale, { day: 'numeric', month: 'short' });
}

export function createCallsView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let filter: 'all' | 'missed' = 'all';
  let query = '';

  const list = el('div', { class: 'ag-scroll-list' });
  const headerWrap = el('div', { class: 'ag-pane-header ag-calls-header' });
  const chipsRow = el('div', { class: 'ag-chips-row' });

  const searchInput = el('input', {
    type: 'text',
    placeholder: callsT().searchPlaceholder,
    value: query,
    onInput: (e: Event) => {
      query = (e.target as HTMLInputElement).value;
      renderList();
    },
  }) as HTMLInputElement;

  const searchWrap = el(
    'div',
    { class: 'ag-calls-search-wrap' },
    el('div', { class: 'ag-search-pill' }, icons.search(16), searchInput),
  );

  const renderList = () => {
    clear(list);
    const s = callsT();
    const q = query.trim().toLowerCase();
    const items = callsStore
      .getLogs()
      .filter((c) => (filter === 'missed' ? c.direction === 'missed' : true))
      .filter((c) => !q || c.peerName.toLowerCase().includes(q));

    if (items.length === 0) {
      list.appendChild(
        el(
          'div',
          { style: 'text-align:center;padding:36px 16px;color:var(--ag-text-muted);font-size:13px;' },
          s.emptyCalls,
        ),
      );
      return;
    }

    for (const item of items) {
      const dirLabel =
        item.direction === 'missed'
          ? s.missedLabel
          : item.direction === 'incoming'
            ? s.incoming
            : s.outgoing;
      const dur = formatDuration(item.durationSec);

      list.appendChild(
        el(
          'div',
          { class: 'ag-chat-row' },
          el('div', { class: 'ag-avatar' }, item.avatarEmoji),
          el(
            'div',
            { class: 'ag-chat-row__body' },
            el('div', { class: 'ag-chat-row__title' }, item.peerName),
            el(
              'div',
              {
                class: `ag-chat-row__preview ${item.direction === 'missed' ? 'ag-calls-row-missed' : ''}`,
              },
              `${dirLabel} • ${formatCallTime(item.timestampMs)}${dur ? ` (${dur})` : ''}`,
            ),
          ),
          el(
            'button',
            {
              class: 'ag-icon-btn',
              title: item.isVideo ? s.startVideoCall : s.startAudioCall,
              onClick: () => openActiveCallModal(item.peerName, item.avatarEmoji, item.isVideo),
            },
            item.isVideo ? icons.video(18) : icons.phone(18),
          ),
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => callsStore.deleteCall(item.id),
            },
            icons.trash(16),
          ),
        ),
      );
    }
  };

  const renderChrome = () => {
    const s = callsT();
    searchInput.placeholder = s.searchPlaceholder;
    clear(headerWrap);
    headerWrap.append(
      el('h1', { class: 'ag-pane-title' }, s.title),
      callsStore.getLogs().length > 0
        ? el(
            'button',
            {
              class: 'ag-chip',
              onClick: () => {
                callsStore.clearAll();
                showToast(s.clearedToast);
              },
            },
            icons.trash(13),
            s.clearHistory,
          )
        : el('span', null),
    );

    clear(chipsRow);
    chipsRow.append(
      el(
        'button',
        {
          class: `ag-chip ${filter === 'all' ? 'is-active' : ''}`,
          onClick: () => {
            filter = 'all';
            renderChrome();
            renderList();
          },
        },
        s.all,
      ),
      el(
        'button',
        {
          class: `ag-chip ${filter === 'missed' ? 'is-active' : ''}`,
          onClick: () => {
            filter = 'missed';
            renderChrome();
            renderList();
          },
        },
        s.missed,
      ),
    );
  };

  root.append(headerWrap, searchWrap, chipsRow, list);

  const onUpdate = () => {
    renderChrome();
    renderList();
  };

  const unsubCalls = callsStore.subscribe(onUpdate);
  const unsubI18n = i18nStore.subscribe(onUpdate);
  onUpdate();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubCalls();
    unsubI18n();
  };

  return root;
}
