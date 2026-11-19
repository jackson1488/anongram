import { i18nStore } from '../../core/i18n/i18nStore';
import { themeStore } from '../../core/theme/themeStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';
import { createChatAppearanceView } from './ChatAppearanceView';
import { settingsStore, type SettingsPrefs } from './settingsStore';

export type SettingsRoute =
  | 'main'
  | 'search'
  | 'myQr'
  | 'account'
  | 'chatAppearance'
  | 'privacy'
  | 'security'
  | 'notifications'
  | 'dataStorage'
  | 'animations'
  | 'devices'
  | 'battery'
  | 'language'
  | 'modules';

function renderDeterministicQrSvg(seed: string): SVGSVGElement {
  const size = 21;
  const cells: string[] = [];
  let hash = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    hash ^= seed.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  const isFinder = (r: number, c: number) =>
    (r < 7 && c < 7) || (r < 7 && c >= size - 7) || (r >= size - 7 && c < 7);

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      let filled = false;
      if (isFinder(r, c)) {
        const lr = r < 7 ? r : r - (size - 7);
        const lc = c < 7 ? c : c - (size - 7);
        filled =
          lr === 0 || lr === 6 || lc === 0 || lc === 6 || (lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4);
      } else {
        const mix = Math.imul(hash ^ (r * 31 + c * 17), 2654435761) >>> 0;
        filled = (mix & 1) === 1;
      }
      if (filled) {
        cells.push(`<rect x="${c}" y="${r}" width="1" height="1"/>`);
      }
    }
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
  svg.setAttribute('class', 'ag-qr-svg');
  svg.setAttribute('fill', 'currentColor');
  svg.innerHTML = cells.join('');
  return svg;
}

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

export function createSettingsDetailView(
  route: SettingsRoute,
  onNavigate: (r: SettingsRoute) => void,
  onBackMobile: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-settings-detail' });

  const renderToggleRow = (label: string, key: keyof SettingsPrefs) => {
    const enabled = settingsStore.all[key];
    return el(
      'div',
      {
        class: 'ag-settings-row',
        onClick: () => {
          settingsStore.toggle(key);
          render();
        },
      },
      el('span', { class: 'ag-settings-row__label' }, label),
      el('div', { class: `ag-switch ${enabled ? 'is-on' : ''}` }, el('div', { class: 'ag-switch__thumb' })),
    );
  };

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const titles: Record<SettingsRoute, string> = {
      main: t.settings.title,
      search: t.common.search,
      myQr: t.settings.myQr,
      account: t.settings.account,
      chatAppearance: t.settings.chatAppearance,
      privacy: t.settings.privacy,
      security: t.settings.security,
      notifications: t.settings.notifications,
      dataStorage: t.settings.dataStorage,
      animations: t.settings.animations,
      devices: t.settings.devices,
      battery: t.settings.battery,
      language: t.settings.language,
      modules: t.settings.modules,
    };

    root.appendChild(
      el(
        'div',
        { class: 'ag-chat-header' },
        el('button', { class: 'ag-icon-btn ag-mobile-only', onClick: onBackMobile }, icons.back(22)),
        el('div', { class: 'ag-chat-header__title' }, titles[route]),
      ),
    );

    const body = el('div', { class: 'ag-settings-scroll' });

    if (route === 'chatAppearance') {
      body.appendChild(createChatAppearanceView());
    } else if (route === 'myQr') {
      const id = authStore.identity;
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-qr-card' },
          renderDeterministicQrSvg(id?.publicId ?? 'anongram'),
          el('h3', { style: 'margin:0;' }, id?.displayName ?? 'Ghost'),
          el('code', { class: 'ag-ag1-code' }, id?.publicId ?? 'ag_anonymous'),
          el('p', { style: 'margin:0;font-size:13px;color:var(--ag-text-secondary);text-align:center;' }, t.settings.qrHint),
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              onClick: async () => {
                await copyToClipboard(id?.publicId ?? '');
                showToast(t.common.copied);
              },
            },
            icons.copy(16),
            t.common.copy,
          ),
        ),
      );
    } else if (route === 'search') {
      const qInput = el('input', {
        type: 'text',
        placeholder: t.settings.searchPlaceholder,
        onInput: () => renderSearchResults(),
      }) as HTMLInputElement;
      const resBox = el('div', { class: 'ag-card ag-settings-group' });
      const entries: Array<{ route: SettingsRoute; label: string }> = [
        { route: 'chatAppearance', label: t.settings.chatAppearance },
        { route: 'account', label: t.settings.account },
        { route: 'privacy', label: t.settings.privacy },
        { route: 'security', label: t.settings.security },
        { route: 'notifications', label: t.settings.notifications },
        { route: 'dataStorage', label: t.settings.dataStorage },
        { route: 'animations', label: t.settings.animations },
        { route: 'devices', label: t.settings.devices },
        { route: 'battery', label: t.settings.battery },
        { route: 'language', label: t.settings.language },
        { route: 'modules', label: t.settings.modules },
        { route: 'myQr', label: t.settings.myQr },
      ];
      const renderSearchResults = () => {
        clear(resBox);
        const q = qInput.value.trim().toLowerCase();
        for (const item of entries.filter((e) => !q || e.label.toLowerCase().includes(q))) {
          resBox.appendChild(
            el(
              'button',
              { class: 'ag-settings-row', onClick: () => onNavigate(item.route) },
              el('span', { class: 'ag-settings-row__label' }, item.label),
              icons.chevronRight(16),
            ),
          );
        }
      };
      renderSearchResults();
      body.appendChild(el('div', { class: 'ag-search-pill', style: 'margin-bottom:12px' }, icons.search(16), qInput));
      body.appendChild(resBox);
    } else if (route === 'account') {
      const id = authStore.identity;
      const nameIn = el('input', { class: 'ag-input', value: id?.displayName ?? '' }) as HTMLInputElement;
      const userIn = el('input', { class: 'ag-input', value: id?.username ?? '' }) as HTMLInputElement;
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-card' },
          el('div', { class: 'ag-section-label' }, t.auth.displayNameLabel),
          nameIn,
          el('div', { class: 'ag-section-label' }, t.auth.usernameLabel),
          userIn,
          el('div', { class: 'ag-section-label' }, 'Fingerprint'),
          el('code', { class: 'ag-ag1-code' }, id?.fingerprint ?? ''),
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              onClick: () => {
                authStore.updateProfile(nameIn.value, userIn.value);
                showToast(t.common.save);
              },
            },
            t.common.save,
          ),
        ),
      );
    } else if (route === 'language') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          (['ru', 'en'] as const).map((lang) =>
            el(
              'button',
              {
                class: 'ag-settings-row',
                onClick: () => i18nStore.setLanguage(lang),
              },
              el('span', { class: 'ag-settings-row__label' }, t.settings.languageNames[lang]),
              i18nStore.language === lang ? icons.check(18) : null,
            ),
          ).reduce((frag, node) => {
            frag.appendChild(node);
            return frag;
          }, document.createDocumentFragment()) as unknown as HTMLElement,
        ),
      );
    } else if (route === 'privacy') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Скрывать статус «в сети»', 'hideOnlineStatus'),
          renderToggleRow('Защита от скриншотов', 'blockScreenshots'),
        ),
      );
    } else if (route === 'security') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Код-пароль на вход', 'passcodeEnabled'),
          renderToggleRow('Биометрическая разблокировка', 'biometricLock'),
        ),
      );
    } else if (route === 'notifications') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Push-уведомления', 'pushNotifications'),
          renderToggleRow('Звуки в приложении', 'inAppSounds'),
        ),
      );
    } else if (route === 'dataStorage') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Автозагрузка медиа', 'autoDownloadMedia'),
        ),
      );
    } else if (route === 'animations') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Анимации интерфейса', 'animationsEnabled'),
          renderToggleRow('Анимации сообщений', 'messageAnimations'),
        ),
      );
    } else if (route === 'battery') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Режим энергосбережения', 'powerSavingMode'),
        ),
      );
    } else if (route === 'modules') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-group' },
          renderToggleRow('Модуль распознавания голоса', 'voiceModule'),
          renderToggleRow('Модуль локального перевода', 'translationModule'),
        ),
      );
    } else if (route === 'devices') {
      body.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-card' },
          el('strong', null, 'AnonGram Web (Current Session)'),
          el('span', { style: 'font-size:13px;color:var(--ag-text-secondary)' }, navigator.userAgent.slice(0, 72)),
        ),
      );
    }

    root.appendChild(body);
  };

  const u1 = i18nStore.subscribe(render);
  const u2 = settingsStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
