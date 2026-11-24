import { i18nStore, registerI18nModule, tr } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { authStore } from '../../auth/authStore';
import { SettingsI18n } from '../i18n';
import { pickAndResizeAvatar } from '../main/SettingsMainScreen';

registerI18nModule('accountSettings', {
  ru: {
    photoHint: 'Нажмите на аватар, чтобы загрузить новое фото профиля',
    changePhotoBtn: 'Изменить фото',
    bioLabel: 'О себе',
    bioPlaceholder: 'Расскажите немного о себе...',
    savedToast: 'Профиль сохранён',
    avatarSavedToast: 'Фото профиля обновлено',
  },
  en: {
    photoHint: 'Tap avatar to upload a new profile photo',
    changePhotoBtn: 'Change Photo',
    bioLabel: 'Bio',
    bioPlaceholder: 'A few words about yourself...',
    savedToast: 'Profile saved',
    avatarSavedToast: 'Profile photo updated',
  },
});

export function createAccountSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const id = authStore.identity;
    const initial = (id?.displayName.trim().slice(0, 1) || 'G').toUpperCase();

    const triggerAvatarPick = () => {
      pickAndResizeAvatar((avatarUrl) => {
        authStore.updateProfile({ avatarUrl });
        showToast(tr('accountSettings:avatarSavedToast'));
      });
    };

    const avatarMedia = id?.avatarUrl
      ? el('img', {
          class: 'ag-settings-hero__avatar-img',
          src: id.avatarUrl,
          alt: id.displayName,
        })
      : el('span', { class: 'ag-settings-hero__avatar-letter' }, initial);

    const nameIn = el('input', {
      class: 'ag-input',
      value: id?.displayName ?? '',
      placeholder: t.auth.displayNamePlaceholder,
    }) as HTMLInputElement;

    const userIn = el('input', {
      class: 'ag-input',
      value: id?.username ?? '',
      placeholder: t.auth.usernamePlaceholder,
    }) as HTMLInputElement;

    const bioIn = el('textarea', {
      class: 'ag-input ag-account-bio-input',
      rows: '3',
      placeholder: tr('accountSettings:bioPlaceholder'),
    }) as HTMLTextAreaElement;
    bioIn.value = id?.bio ?? '';

    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card ag-account-hero-card' },
        el(
          'div',
          { class: 'ag-settings-hero ag-settings-hero--compact' },
          el(
            'button',
            {
              type: 'button',
              class: 'ag-settings-hero__avatar-wrap',
              onClick: triggerAvatarPick,
            },
            el('div', { class: 'ag-settings-hero__avatar' }, avatarMedia),
            el('span', { class: 'ag-settings-hero__cam-badge' }, tgIcons.camera(15)),
          ),
          el('div', { class: 'ag-settings-hero__name' }, id?.displayName ?? ''),
          el('div', { class: 'ag-settings-hero__sub' }, tr('accountSettings:photoHint')),
          el(
            'button',
            {
              type: 'button',
              class: 'ag-pill-btn ag-pill-btn--secondary',
              onClick: triggerAvatarPick,
            },
            tgIcons.camera(16),
            el('span', null, tr('accountSettings:changePhotoBtn')),
          ),
        ),
      ),
    );

    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, t.auth.displayNameLabel),
        nameIn,
        el('div', { class: 'ag-section-label' }, t.auth.usernameLabel),
        userIn,
        el('div', { class: 'ag-section-label' }, tr('accountSettings:bioLabel')),
        bioIn,
        el('div', { class: 'ag-section-label' }, SettingsI18n.current.fingerprint),
        el('code', { class: 'ag-ag1-code' }, id?.fingerprint ?? ''),
        el(
          'button',
          {
            type: 'button',
            class: 'ag-pill-btn ag-pill-btn--primary',
            onClick: () => {
              authStore.updateProfile({
                displayName: nameIn.value,
                username: userIn.value,
                bio: bioIn.value,
              });
              showToast(tr('accountSettings:savedToast'));
            },
          },
          t.common.save,
        ),
      ),
    );
  };

  const u1 = authStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
