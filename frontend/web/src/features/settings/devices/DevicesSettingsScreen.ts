import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { SettingsI18n } from '../i18n';
import { settingsStore, type DeviceSession } from '../settingsStore';

export function createDevicesSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const renderSessionRow = (session: DeviceSession, showTerminate: boolean) => {
    const s = SettingsI18n.current;
    return el(
      'div',
      { class: 'ag-session-row' },
      el('div', { class: 'ag-session-row__icon' }, icons.phone(20)),
      el(
        'div',
        { class: 'ag-session-row__info' },
        el('div', { class: 'ag-session-row__title' }, session.deviceName),
        el('div', { class: 'ag-session-row__meta' }, `${session.platform} • ${session.location}`),
      ),
      session.isCurrent
        ? el('span', { class: 'ag-badge-pill' }, s.onlineStatus)
        : showTerminate
          ? el(
              'button',
              {
                class: 'ag-chip ag-danger-btn',
                onClick: () => {
                  settingsStore.terminateSession(session.id);
                  showToast(s.sessionTerminatedToast);
                },
              },
              s.terminateSession,
            )
          : el('span', { class: 'ag-settings-row__badge' }, session.lastActive),
    );
  };

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const allSessions = settingsStore.sessions;
    const current = allSessions.find((x) => x.isCurrent) ?? allSessions[0];
    const others = allSessions.filter((x) => !x.isCurrent);

    // 1. Current Session
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, s.currentSession),
        renderSessionRow(current, false),
      ),
    );

    // 2. Other Active Sessions
    const othersCard = el(
      'div',
      { class: 'ag-card ag-settings-card' },
      el('div', { class: 'ag-section-label' }, s.otherSessionsTitle),
    );

    if (others.length === 0) {
      othersCard.appendChild(
        el('div', { style: 'font-size:13px;color:var(--ag-text-secondary);padding:6px 0' }, s.noOtherSessions),
      );
    } else {
      const list = el('div', { class: 'ag-settings-group', style: 'margin:0' });
      for (const sess of others) {
        list.appendChild(renderSessionRow(sess, true));
      }
      othersCard.appendChild(list);
      othersCard.appendChild(
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--secondary ag-danger-btn',
            onClick: () => {
              if (window.confirm(s.terminateOthersConfirm)) {
                settingsStore.terminateOtherSessions();
                showToast(s.sessionTerminatedToast);
              }
            },
          },
          s.terminateOthersBtn,
        ),
      );
    }

    root.appendChild(othersCard);
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
