import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons, tgIcons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';
import { profileT } from './i18n';
import './profile.css';

function shortPublicId(raw: string): string {
  if (raw.length >= 16) {
    return `${raw.slice(0, 8)}…${raw.slice(-6)}`;
  }
  return raw;
}

function profileRow(title: string, value: string): HTMLElement {
  return el(
    'div',
    { class: 'ag-profile-row' },
    el('div', { class: 'ag-profile-row__title' }, title),
    el('div', { class: 'ag-profile-row__value' }, value),
  );
}

export function createProfileView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });

  const render = () => {
    clear(root);
    const id = authStore.identity;
    if (!id) return;
    const s = profileT();

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, s.title),
      ),
    );

    const scroll = el('div', { class: 'ag-scroll-list ag-profile-scroll' });

    scroll.appendChild(
      el(
        'div',
        { class: 'ag-profile-hero' },
        el('div', { class: 'ag-profile-badge-icon' }, id.displayName.slice(0, 1).toUpperCase()),
        el(
          'div',
          { class: 'ag-profile-hero__meta' },
          el('h2', { class: 'ag-profile-hero__name' }, id.displayName),
          el('div', { class: 'ag-profile-hero__sub' }, `${id.username} • ${shortPublicId(id.publicId)} (Ed25519)`),
        ),
      ),
    );

    scroll.appendChild(profileRow(s.seedTitle, s.seedValue));
    scroll.appendChild(profileRow(s.quantumTitle, s.quantumValue));
    scroll.appendChild(profileRow(s.verifyTitle, s.verifyValue));
    scroll.appendChild(profileRow(s.accountTitle, s.accountValue));

    scroll.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, s.publicIdLabel),
        el('code', { class: 'ag-ag1-code' }, id.publicId),
        el('div', { class: 'ag-section-label' }, s.ed25519Label),
        el('code', { class: 'ag-ag1-code' }, id.ed25519PublicKey),
        el('div', { class: 'ag-section-label' }, s.fingerprintLabel),
        el('code', { class: 'ag-ag1-code' }, id.fingerprint),
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--primary',
            onClick: async () => {
              await copyToClipboard(id.publicId);
              showToast(s.copiedPublicId);
            },
          },
          icons.copy(16),
          s.copyPublicId,
        ),
      ),
    );

    scroll.appendChild(
      el(
        'button',
        {
          class: 'ag-profile-logout-btn',
          onClick: () => {
            if (window.confirm(s.logoutConfirm)) {
              authStore.logout();
            }
          },
        },
        tgIcons.logout(16),
        s.logoutBtn,
      ),
    );

    root.appendChild(scroll);
  };

  const unsubAuth = authStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubAuth();
    unsubI18n();
  };
  return root;
}
