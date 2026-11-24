import { i18nStore, registerI18nModule, tr } from '../../../core/i18n/i18nStore';
import { themeStore } from '../../../core/theme/themeStore';
import { clear, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { authStore } from '../../auth/authStore';
import type { SettingsRoute } from '../navigation/SettingsRoute';

registerI18nModule('settingsMain', {
  ru: {
    accountsSection: 'Аккаунты',
    logoutShort: 'Выход',
    avatarUpdated: 'Фото профиля обновлено',
    accountTitle: 'Аккаунт',
    accountSub: 'Номер, имя пользователя, «О себе»',
    chatAppearanceTitle: 'Настройки чатов',
    chatAppearanceSub: 'Обои, ночной режим, анимации',
    privacyTitle: 'Конфиденциальность',
    privacySub: 'Время захода, устройства, ключи доступа',
    securityTitle: 'Безопасность',
    securitySub: 'Код-пароль, биометрия, квантовая защита',
    notificationsTitle: 'Уведомления',
    notificationsSub: 'Звуки, звонки, счётчик сообщений',
    dataStorageTitle: 'Данные и память',
    dataStorageSub: 'Настройки загрузки медиафайлов',
    animationsTitle: 'Анимации',
    animationsSub: 'Плавные переходы, размытие Glass, эффекты',
    devicesTitle: 'Устройства',
    devicesSub: 'Управление активными сеансами',
    batteryTitle: 'Энергосбережение',
    batterySub: 'Экономия энергии при низком заряде',
    languageTitle: 'Язык',
    languageSub: 'Русский / English',
    modulesTitle: 'Модули',
    modulesSub: 'Расшифровка ГС, локальный перевод, языковые пакеты',
    quantumCoreTitle: 'AnonGram Quantum Core',
    quantumCoreSub: 'Пост-квантовое шифрование ML-DSA-87',
  },
  en: {
    accountsSection: 'Accounts',
    logoutShort: 'Log Out',
    avatarUpdated: 'Profile photo updated',
    accountTitle: 'Account',
    accountSub: 'Number, username, bio',
    chatAppearanceTitle: 'Chat Settings',
    chatAppearanceSub: 'Wallpapers, night mode, animations',
    privacyTitle: 'Privacy',
    privacySub: 'Last seen, devices, passkeys',
    securityTitle: 'Security',
    securitySub: 'Passcode, biometrics, quantum protection',
    notificationsTitle: 'Notifications',
    notificationsSub: 'Sounds, calls, badge counter',
    dataStorageTitle: 'Data and Storage',
    dataStorageSub: 'Media download settings',
    animationsTitle: 'Animations',
    animationsSub: 'Smooth transitions, Glass blur, effects',
    devicesTitle: 'Devices',
    devicesSub: 'Manage active sessions',
    batteryTitle: 'Power Saving',
    batterySub: 'Save battery on low charge',
    languageTitle: 'Language',
    languageSub: 'Русский / English',
    modulesTitle: 'Modules',
    modulesSub: 'Voice transcription, local translation, language packs',
    quantumCoreTitle: 'AnonGram Quantum Core',
    quantumCoreSub: 'Post-quantum encryption ML-DSA-87',
  },
});

const SVG_NS = 'http://www.w3.org/2000/svg';

function badgeIcon(paths: string, size = 20): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '2');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.innerHTML = paths;
  return svg;
}

const settingsBadgeIcons = {
  dataPie: (s = 20) =>
    badgeIcon(
      '<path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/>',
      s,
    ),
  sparkles: (s = 20) =>
    badgeIcon(
      '<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>',
      s,
    ),
  devices: (s = 20) =>
    badgeIcon(
      '<rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>',
      s,
    ),
  battery: (s = 20) =>
    badgeIcon(
      '<rect x="2" y="7" width="16" height="10" rx="2" ry="2"/><line x1="22" y1="11" x2="22" y2="13"/><line x1="6" y1="11" x2="6" y2="13"/><line x1="10" y1="11" x2="10" y2="13"/>',
      s,
    ),
  modules: (s = 20) =>
    badgeIcon(
      '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/>',
      s,
    ),
};

export function pickAndResizeAvatar(onReady: (dataUrl: string) => void): void {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.style.display = 'none';
  input.onchange = () => {
    const file = input.files?.[0];
    input.remove();
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const rawUrl = typeof reader.result === 'string' ? reader.result : '';
      if (!rawUrl) return;
      const img = new Image();
      img.onload = () => {
        const maxSide = 256;
        const scale = Math.min(1, maxSide / Math.max(img.width || 1, img.height || 1));
        const w = Math.max(1, Math.round((img.width || maxSide) * scale));
        const h = Math.max(1, Math.round((img.height || maxSide) * scale));
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(img, 0, 0, w, h);
          const scaledUrl = canvas.toDataURL('image/jpeg', 0.86);
          onReady(scaledUrl);
        } else {
          onReady(rawUrl);
        }
      };
      img.onerror = () => onReady(rawUrl);
      img.src = rawUrl;
    };
    reader.readAsDataURL(file);
  };
  document.body.appendChild(input);
  input.click();
}

export function createSettingsListPane(
  getRoute: () => SettingsRoute,
  onNavigate: (route: SettingsRoute) => void,
  onBack?: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-list-pane ag-settings-main-pane' });
  let menuOpen = false;

  const closeMenu = () => {
    if (!menuOpen) return;
    menuOpen = false;
    const pop = root.querySelector('.ag-settings-menu-popover');
    pop?.classList.remove('is-open');
  };

  const onDocClick = (e: MouseEvent) => {
    if (!menuOpen) return;
    const target = e.target as Node | null;
    const wrap = root.querySelector('.ag-settings-topbar__actions');
    if (wrap && target && wrap.contains(target)) return;
    closeMenu();
  };
  document.addEventListener('click', onDocClick);

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const identity = authStore.identity;
    const current = getRoute();

    const triggerAvatarUpload = () => {
      pickAndResizeAvatar((avatarUrl) => {
        authStore.updateProfile({ avatarUrl });
        showToast(tr('settingsMain:avatarUpdated'));
      });
    };

    const popover = el(
      'div',
      { class: `ag-settings-menu-popover${menuOpen ? ' is-open' : ''}` },
      el(
        'button',
        {
          type: 'button',
          class: 'ag-settings-menu-item ag-settings-menu-item--danger',
          onClick: (e) => {
            e.stopPropagation();
            closeMenu();
            authStore.logout();
          },
        },
        tgIcons.logout(17),
        el('span', null, `${tr('settingsMain:logoutShort')} / Log Out`),
      ),
    );

    // Top bar: Left circular glass pill ( QR ) (+ optional desktop <- back), centered bold Настройки, right capsule glass pill ( 🔍 ⋮ )
    const leftGroup = el(
      'div',
      { style: 'display:inline-flex;align-items:center;gap:6px;flex-shrink:0;' },
      ...(onBack
        ? [
            el(
              'button',
              {
                type: 'button',
                class: 'ag-settings-glass-circle',
                title: t.common.back,
                onClick: onBack,
              },
              icons.back(20),
            ),
          ]
        : []),
      el(
        'button',
        {
          type: 'button',
          class: 'ag-settings-glass-circle',
          title: t.settings.myQr,
          onClick: () => onNavigate('myQr'),
        },
        icons.qr(20),
      ),
    );

    root.appendChild(
      el(
        'div',
        { class: 'ag-settings-topbar' },
        leftGroup,
        el('h1', { class: 'ag-settings-topbar__title' }, t.settings.title),
        el(
          'div',
          { class: 'ag-settings-topbar__actions' },
          el(
            'div',
            { class: 'ag-settings-glass-capsule' },
            el(
              'button',
              {
                type: 'button',
                class: 'ag-settings-capsule-btn',
                title: t.common.search,
                onClick: () => onNavigate('search'),
              },
              icons.search(19),
            ),
            el(
              'button',
              {
                type: 'button',
                class: 'ag-settings-capsule-btn',
                title: tr('settingsMain:logoutShort'),
                onClick: (e) => {
                  e.stopPropagation();
                  menuOpen = !menuOpen;
                  popover.classList.toggle('is-open', menuOpen);
                },
              },
              icons.more(19),
            ),
          ),
          popover,
        ),
      ),
    );

    const scroll = el('div', { class: 'ag-scroll-list ag-settings-main-scroll' });

    if (identity) {
      const initial = (identity.displayName.trim().slice(0, 1) || 'G').toUpperCase();
      const handleUpper = identity.username.toUpperCase();

      // Centered Hero Avatar (88px) with bottom-right 28px cyan camera badge
      const heroAvatarMedia = identity.avatarUrl
        ? el('img', {
            class: 'ag-settings-hero__avatar-img',
            src: identity.avatarUrl,
            alt: identity.displayName,
          })
        : el('span', { class: 'ag-settings-hero__avatar-letter' }, initial);

      scroll.appendChild(
        el(
          'div',
          { class: 'ag-settings-hero' },
          el(
            'button',
            {
              type: 'button',
              class: 'ag-settings-hero__avatar-wrap',
              onClick: triggerAvatarUpload,
            },
            el('div', { class: 'ag-settings-hero__avatar' }, heroAvatarMedia),
            el('span', { class: 'ag-settings-hero__cam-badge' }, tgIcons.camera(15)),
          ),
          el('div', { class: 'ag-settings-hero__name' }, identity.displayName),
          el(
            'div',
            { class: 'ag-settings-hero__sub' },
            `${identity.publicId} • ${handleUpper}`,
          ),
        ),
      );

      // Аккаунты Card
      const miniAvatarMedia = identity.avatarUrl
        ? el('img', {
            class: 'ag-settings-account-row__avatar-img',
            src: identity.avatarUrl,
            alt: identity.displayName,
          })
        : el('span', null, initial);

      scroll.appendChild(
        el(
          'div',
          { class: 'ag-card ag-settings-accounts-card' },
          el('div', { class: 'ag-settings-accounts-card__title' }, tr('settingsMain:accountsSection')),
          el(
            'button',
            {
              type: 'button',
              class: 'ag-settings-account-row',
              onClick: () => onNavigate('account'),
            },
            el('div', { class: 'ag-settings-account-row__avatar' }, miniAvatarMedia),
            el('span', { class: 'ag-settings-account-row__name' }, identity.displayName),
            el('span', { class: 'ag-settings-account-row__check' }, icons.check(18)),
          ),
        ),
      );
    }

    // Main Settings Group Card with Colored Rounded-Square Icon Badges (38x38px, border-radius: 11px)
    const items: Array<{
      route: SettingsRoute;
      tone: string;
      icon: SVGElement;
      title: string;
      sub: string;
    }> = [
      {
        route: 'account',
        tone: 'blue',
        icon: icons.user(20),
        title: tr('settingsMain:accountTitle'),
        sub: tr('settingsMain:accountSub'),
      },
      {
        route: 'chatAppearance',
        tone: 'orange',
        icon: icons.chats(20),
        title: tr('settingsMain:chatAppearanceTitle'),
        sub: tr('settingsMain:chatAppearanceSub'),
      },
      {
        route: 'privacy',
        tone: 'green',
        icon: icons.lock(20),
        title: tr('settingsMain:privacyTitle'),
        sub: tr('settingsMain:privacySub'),
      },
      {
        route: 'security',
        tone: 'emerald',
        icon: icons.shield(20),
        title: tr('settingsMain:securityTitle'),
        sub: tr('settingsMain:securitySub'),
      },
      {
        route: 'notifications',
        tone: 'red',
        icon: tgIcons.bell(20),
        title: tr('settingsMain:notificationsTitle'),
        sub: tr('settingsMain:notificationsSub'),
      },
      {
        route: 'dataStorage',
        tone: 'indigo',
        icon: settingsBadgeIcons.dataPie(20),
        title: tr('settingsMain:dataStorageTitle'),
        sub: tr('settingsMain:dataStorageSub'),
      },
      {
        route: 'animations',
        tone: 'pink',
        icon: settingsBadgeIcons.sparkles(20),
        title: tr('settingsMain:animationsTitle'),
        sub: tr('settingsMain:animationsSub'),
      },
      {
        route: 'devices',
        tone: 'teal',
        icon: settingsBadgeIcons.devices(20),
        title: tr('settingsMain:devicesTitle'),
        sub: tr('settingsMain:devicesSub'),
      },
      {
        route: 'battery',
        tone: 'amber',
        icon: settingsBadgeIcons.battery(20),
        title: tr('settingsMain:batteryTitle'),
        sub: tr('settingsMain:batterySub'),
      },
      {
        route: 'language',
        tone: 'purple',
        icon: icons.globe(20),
        title: tr('settingsMain:languageTitle'),
        sub: tr('settingsMain:languageSub'),
      },
      {
        route: 'modules',
        tone: 'cyan',
        icon: settingsBadgeIcons.modules(20),
        title: tr('settingsMain:modulesTitle'),
        sub: tr('settingsMain:modulesSub'),
      },
    ];

    const group = el('div', { class: 'ag-card ag-settings-group ag-settings-group--v2' });
    for (const item of items) {
      group.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: `ag-settings-row ag-settings-row--v2 ${current === item.route ? 'is-active' : ''}`,
            onClick: () => onNavigate(item.route),
          },
          el(
            'span',
            { class: `ag-settings-badge-icon ag-settings-badge-icon--${item.tone}` },
            item.icon,
          ),
          el(
            'span',
            { class: 'ag-settings-row__text' },
            el('span', { class: 'ag-settings-row__title' }, item.title),
            el('span', { class: 'ag-settings-row__sub' }, item.sub),
          ),
        ),
      );
    }
    scroll.appendChild(group);

    // Bottom Card: AnonGram Quantum Core
    scroll.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-group ag-settings-group--v2 ag-settings-quantum-card' },
        el(
          'button',
          {
            type: 'button',
            class: 'ag-settings-row ag-settings-row--v2',
            onClick: () => onNavigate('security'),
          },
          el(
            'span',
            { class: 'ag-settings-badge-icon ag-settings-badge-icon--cyan' },
            tgIcons.star(20),
          ),
          el(
            'span',
            { class: 'ag-settings-row__text' },
            el('span', { class: 'ag-settings-row__title' }, tr('settingsMain:quantumCoreTitle')),
            el('span', { class: 'ag-settings-row__sub' }, tr('settingsMain:quantumCoreSub')),
          ),
        ),
      ),
    );

    root.appendChild(scroll);
  };

  const u1 = i18nStore.subscribe(render);
  const u2 = themeStore.subscribe(render);
  const u3 = authStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    document.removeEventListener('click', onDocClick);
    u1();
    u2();
    u3();
  };

  return root;
}
