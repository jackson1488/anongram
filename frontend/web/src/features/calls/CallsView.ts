import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';

interface CallLogItem {
  id: string;
  peerName: string;
  avatarEmoji: string;
  avatarColor: string;
  direction: 'incoming' | 'outgoing' | 'missed';
  isVideo: boolean;
  timeLabel: string;
  durationLabel: string;
}

const CALL_LOGS: CallLogItem[] = [
  {
    id: 'c1',
    peerName: 'Ghost Node #42',
    avatarEmoji: '🕶️',
    avatarColor: '#8B5CF6',
    direction: 'outgoing',
    isVideo: false,
    timeLabel: 'Сегодня, 17:40',
    durationLabel: '04:12',
  },
  {
    id: 'c2',
    peerName: 'Maya Cipher',
    avatarEmoji: '⚡',
    avatarColor: '#EC4899',
    direction: 'missed',
    isVideo: true,
    timeLabel: 'Вчера, 22:15',
    durationLabel: '',
  },
  {
    id: 'c3',
    peerName: 'AnonGram Core',
    avatarEmoji: '🛡️',
    avatarColor: '#06B6D4',
    direction: 'incoming',
    isVideo: false,
    timeLabel: '14 нояб.',
    durationLabel: '11:05',
  },
];

export function openActiveCallModal(peerName: string, avatarEmoji: string, isVideo: boolean): void {
  const t = i18nStore.t;
  let muted = false;
  let speaker = true;
  const overlay = el('div', { class: 'ag-modal-overlay' });

  const render = () => {
    clear(overlay);
    const muteBtn = el(
      'button',
      {
        class: `ag-pill-btn ${muted ? 'ag-pill-btn--primary' : 'ag-pill-btn--secondary'}`,
        onClick: () => {
          muted = !muted;
          render();
        },
      },
      t.calls.mute,
    );
    const speakerBtn = el(
      'button',
      {
        class: `ag-pill-btn ${speaker ? 'ag-pill-btn--ghost' : 'ag-pill-btn--secondary'}`,
        onClick: () => {
          speaker = !speaker;
          render();
        },
      },
      t.calls.speaker,
    );
    const endBtn = el(
      'button',
      {
        class: 'ag-pill-btn',
        style: 'background:#ef4444;color:#fff;',
        onClick: () => overlay.remove(),
      },
      t.calls.endCall,
    );

    overlay.appendChild(
      el(
        'div',
        { class: 'ag-call-modal ag-card' },
        el('div', { class: 'ag-call-modal__badge' }, t.calls.encryptedCall),
        el('div', { class: 'ag-call-modal__avatar' }, avatarEmoji),
        el('h2', { style: 'margin:0;font-size:22px;' }, peerName),
        el('p', { style: 'margin:0;color:var(--ag-text-secondary);font-size:14px;' }, `${isVideo ? t.calls.startVideoCall : t.calls.startAudioCall} • ${t.calls.calling}`),
        el('div', { class: 'ag-call-modal__actions' }, muteBtn, speakerBtn, endBtn),
      ),
    );
  };

  render();
  document.body.appendChild(overlay);
}

export function createCallsView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let filter: 'all' | 'missed' = 'all';

  const render = () => {
    clear(root);
    const t = i18nStore.t;

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, t.calls.title),
      ),
    );

    root.appendChild(
      el(
        'div',
        { class: 'ag-chips-row' },
        el(
          'button',
          {
            class: `ag-chip ${filter === 'all' ? 'is-active' : ''}`,
            onClick: () => {
              filter = 'all';
              render();
            },
          },
          t.calls.all,
        ),
        el(
          'button',
          {
            class: `ag-chip ${filter === 'missed' ? 'is-active' : ''}`,
            onClick: () => {
              filter = 'missed';
              render();
            },
          },
          t.calls.missed,
        ),
      ),
    );

    const list = el('div', { class: 'ag-scroll-list' });
    const items = CALL_LOGS.filter((c) => (filter === 'missed' ? c.direction === 'missed' : true));
    for (const item of items) {
      const dirLabel =
        item.direction === 'missed'
          ? t.calls.missedLabel
          : item.direction === 'incoming'
            ? t.calls.incoming
            : t.calls.outgoing;

      list.appendChild(
        el(
          'div',
          { class: 'ag-chat-row' },
          el('div', { class: 'ag-avatar', style: `background:${item.avatarColor}22;border-color:${item.avatarColor}55` }, item.avatarEmoji),
          el(
            'div',
            { class: 'ag-chat-row__body' },
            el('div', { class: 'ag-chat-row__title' }, item.peerName),
            el(
              'div',
              {
                class: 'ag-chat-row__preview',
                style: item.direction === 'missed' ? 'color:#ef4444' : '',
              },
              `${dirLabel} • ${item.timeLabel}${item.durationLabel ? ` (${item.durationLabel})` : ''}`,
            ),
          ),
          el(
            'button',
            {
              class: 'ag-icon-btn',
              title: item.isVideo ? t.calls.startVideoCall : t.calls.startAudioCall,
              onClick: () => openActiveCallModal(item.peerName, item.avatarEmoji, item.isVideo),
            },
            item.isVideo ? icons.video(18) : icons.phone(18),
          ),
        ),
      );
    }
    root.appendChild(list);
  };

  i18nStore.subscribe(render);
  render();
  return root;
}
