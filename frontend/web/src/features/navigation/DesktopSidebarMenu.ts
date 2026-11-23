import { AVATAR_GRADIENTS } from '../../core/crypto/identity';
import { themeStore } from '../../core/theme/themeStore';
import { el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { authStore } from '../auth/authStore';
import { NavigationI18n } from './i18n';

export interface DesktopSidebarMenuProps {
  archivedCount: number;
  onSavedMessages: () => void;
  onOpenArchive: () => void;
  onOpenContacts: () => void;
  onOpenCalls: () => void;
  onOpenServices: () => void;
  onOpenQrHub: () => void;
  onOpenSettings: () => void;
}

/**
 * Desktop (>=840px) Telegram Web A style compact hamburger dropdown menu.
 */
export function openDesktopSidebarMenu(props: DesktopSidebarMenuProps): () => void {
  const existing = document.querySelector('.ag-pop-scrim');
  if (existing) existing.remove();

  const s = NavigationI18n.current;
  const id = authStore.identity;
  const grad = AVATAR_GRADIENTS[id?.avatarIndex ?? 0];
  const displayName = id?.displayName ?? s.defaultDisplayName;

  const scrim = el('div', {
    class: 'ag-pop-scrim',
    onClick: (e: Event) => {
      if (e.target === scrim) close();
    },
    onContextMenu: (e: Event) => {
      e.preventDefault();
      close();
    },
  });

  const close = () => {
    scrim.remove();
  };

  const item = (
    icon: SVGElement,
    label: string,
    action: () => void,
    badge?: string,
    trailing?: HTMLElement,
  ) =>
    el(
      'button',
      {
        class: 'ag-pop__item',
        onClick: () => {
          if (!trailing) close();
          action();
        },
      },
      icon,
      el('span', { class: 'ag-pop__label' }, label),
      badge ? el('span', { class: 'ag-pop__badge' }, badge) : null,
      trailing ?? null,
    );

  const nightSwitch = el(
    'span',
    { class: `ag-switch ag-switch--sm ${themeStore.scheme.isDark ? 'is-on' : ''}` },
    el('span', { class: 'ag-switch-thumb ag-switch__thumb' }),
  );

  const panel = el(
    'div',
    { class: 'ag-pop' },
    el(
      'div',
      { class: 'ag-pop__account' },
      el(
        'div',
        {
          class: 'ag-pop__avatar',
          style: `background:linear-gradient(135deg, ${grad[0]}, ${grad[1]});`,
        },
        displayName.slice(0, 1).toUpperCase(),
      ),
      el('span', { class: 'ag-pop__label' }, displayName),
    ),
    el('div', { class: 'ag-pop__sep' }),
    item(icons.bookmark(18), s.savedMessages, props.onSavedMessages),
    item(
      icons.archive(18),
      s.archiveChats,
      props.onOpenArchive,
      props.archivedCount > 0 ? String(props.archivedCount) : undefined,
    ),
    item(icons.users(18), s.contacts, props.onOpenContacts),
    item(icons.phone(18), s.calls, props.onOpenCalls),
    item(icons.shield(18), s.services, props.onOpenServices),
    item(icons.qr(18), s.qrHub, props.onOpenQrHub),
    el('div', { class: 'ag-pop__sep' }),
    item(icons.settings(18), s.settings, props.onOpenSettings),
    item(
      icons.moon(18),
      s.nightMode,
      () => {
        themeStore.selectPreset(themeStore.scheme.isDark ? 'telegram_day' : 'telegram_night');
        nightSwitch.classList.toggle('is-on', themeStore.scheme.isDark);
      },
      undefined,
      nightSwitch,
    ),
    el('div', { class: 'ag-pop__foot' }, s.footerVersion),
  );

  scrim.appendChild(panel);
  document.body.appendChild(scrim);
  requestAnimationFrame(() => scrim.classList.add('is-open'));

  return close;
}
