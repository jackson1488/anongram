import { i18nStore } from '../../../core/i18n/i18nStore';
import { el } from '../../../core/ui/dom';
import { showToast } from '../../../core/ui/toast';
import { authStore } from '../../auth/authStore';
import { SettingsI18n } from '../i18n';

export function createAccountSettingsScreen(): HTMLElement {
  const t = i18nStore.t;
  const id = authStore.identity;
  const nameIn = el('input', { class: 'ag-input', value: id?.displayName ?? '' }) as HTMLInputElement;
  const userIn = el('input', { class: 'ag-input', value: id?.username ?? '' }) as HTMLInputElement;
  return el(
    'div',
    { class: 'ag-card ag-settings-card' },
    el('div', { class: 'ag-section-label' }, t.auth.displayNameLabel),
    nameIn,
    el('div', { class: 'ag-section-label' }, t.auth.usernameLabel),
    userIn,
    el('div', { class: 'ag-section-label' }, SettingsI18n.current.fingerprint),
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
  );
}
