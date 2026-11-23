import { i18nStore } from '../../../core/i18n/i18nStore';
import { themeStore } from '../../../core/theme/themeStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { authStore } from '../../auth/authStore';
import type { SettingsRoute } from '../navigation/SettingsRoute';

export function createSettingsListPane(
  getRoute: () => SettingsRoute,
  onNavigate: (route: SettingsRoute) => void,
): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const identity = authStore.identity;
    const current = getRoute();

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, t.settings.title),
        el(
          'div',
          { style: 'display:flex;gap:4px;' },
          el(
            'button',
            { class: 'ag-icon-btn', title: t.common.search, onClick: () => onNavigate('search') },
            icons.search(19),
          ),
          el(
            'button',
            { class: 'ag-icon-btn', title: t.settings.myQr, onClick: () => onNavigate('myQr') },
            icons.qr(19),
          ),
        ),
      ),
    );

    const scroll = el('div', { class: 'ag-scroll-list' });

    // Profile card
    if (identity) {
      scroll.appendChild(
        el(
          'div',
          {
            class: 'ag-card ag-profile-card',
            onClick: () => onNavigate('account'),
          },
          el('div', { class: 'ag-avatar', style: 'background:var(--ag-accent-soft);color:var(--ag-accent)' }, identity.displayName.slice(0, 1).toUpperCase()),
          el(
            'div',
            { class: 'ag-profile-card__info' },
            el('strong', null, identity.displayName),
            el('span', null, `${identity.username} • ${identity.publicId}`),
          ),
          icons.chevronRight(18),
        ),
      );
    }

    const items: Array<{ route: SettingsRoute; icon: SVGElement; label: string; badge?: string }> = [
      { route: 'chatAppearance', icon: icons.palette(18), label: t.settings.chatAppearance, badge: themeStore.scheme.displayName },
      { route: 'account', icon: icons.shield(18), label: t.settings.account },
      { route: 'privacy', icon: icons.lock(18), label: t.settings.privacy },
      { route: 'security', icon: icons.lock(18), label: t.settings.security },
      { route: 'notifications', icon: icons.chats(18), label: t.settings.notifications },
      { route: 'dataStorage', icon: icons.more(18), label: t.settings.dataStorage },
      { route: 'animations', icon: icons.chats(18), label: t.settings.animations },
      { route: 'devices', icon: icons.phone(18), label: t.settings.devices },
      { route: 'battery', icon: icons.shield(18), label: t.settings.battery },
      { route: 'language', icon: icons.globe(18), label: t.settings.language, badge: t.settings.languageNames[i18nStore.language] },
      { route: 'modules', icon: icons.settings(18), label: t.settings.modules },
    ];

    const group = el('div', { class: 'ag-card ag-settings-group' });
    for (const item of items) {
      group.appendChild(
        el(
          'button',
          {
            class: `ag-settings-row ${current === item.route ? 'is-active' : ''}`,
            onClick: () => onNavigate(item.route),
          },
          el('span', { class: 'ag-settings-row__icon' }, item.icon),
          el('span', { class: 'ag-settings-row__label' }, item.label),
          item.badge ? el('span', { class: 'ag-settings-row__badge' }, item.badge) : null,
          icons.chevronRight(16),
        ),
      );
    }
    scroll.appendChild(group);

    scroll.appendChild(
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--secondary ag-logout-btn',
          onClick: () => {
            if (window.confirm(t.settings.logoutConfirm)) {
              authStore.logout();
            }
          },
        },
        t.settings.logout,
      ),
    );

    root.appendChild(scroll);
  };

  const u1 = i18nStore.subscribe(render);
  const u2 = themeStore.subscribe(render);
  const u3 = authStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
