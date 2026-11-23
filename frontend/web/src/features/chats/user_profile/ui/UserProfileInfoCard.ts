import { copyToClipboard, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import type { AutoDeleteConfig, PeerCryptoMetadata } from '../model/UserProfileModels';

export interface UserProfileInfoCardProps {
  meta: PeerCryptoMetadata;
  autoDelete: AutoDeleteConfig;
  isMuted: boolean;
  strings: UserProfileStrings;
  onOpenAutoDelete: () => void;
  onToggleNotifications: () => void;
}

function createCopyRow(
  iconNode: SVGSVGElement,
  value: string,
  label: string,
  copiedToast: string,
  monospace = false,
): HTMLElement {
  return el(
    'button',
    {
      class: 'ag-up-info-row',
      onClick: () => {
        void copyToClipboard(value).then((ok) => {
          if (ok) showToast(copiedToast);
        });
      },
    },
    el('span', { class: 'ag-up-info-row__icon' }, iconNode),
    el(
      'div',
      { class: 'ag-up-info-row__body' },
      el('div', { class: 'ag-up-info-row__value' + (monospace ? ' is-mono' : '') }, value),
      el('div', { class: 'ag-up-info-row__label' }, label),
    ),
    el('span', { class: 'ag-up-info-row__trailing' }, icons.copy(16)),
  );
}

export function createUserProfileInfoCard(props: UserProfileInfoCardProps): HTMLElement {
  const { meta, autoDelete, isMuted, strings, onOpenAutoDelete, onToggleNotifications } = props;

  const autoDeleteRow = el(
    'button',
    {
      class: 'ag-up-info-row',
      onClick: onOpenAutoDelete,
    },
    el('span', { class: 'ag-up-info-row__icon' }, tgIcons.clock(18)),
    el(
      'div',
      { class: 'ag-up-info-row__body' },
      el('div', { class: 'ag-up-info-row__value' }, strings.formatAutoDeleteValue(autoDelete)),
      el('div', { class: 'ag-up-info-row__label' }, strings.labelAutoDelete),
    ),
    el('span', { class: 'ag-up-info-row__trailing' }, icons.chevronRight(17)),
  );

  const notificationsRow = el(
    'button',
    {
      class: 'ag-up-info-row',
      role: 'switch',
      'aria-checked': String(!isMuted),
      onClick: onToggleNotifications,
    },
    el(
      'span',
      { class: 'ag-up-info-row__icon' },
      isMuted ? tgIcons.bellOff(18) : tgIcons.bell(18),
    ),
    el(
      'div',
      { class: 'ag-up-info-row__body' },
      el('div', { class: 'ag-up-info-row__value' }, strings.labelNotifications),
      el(
        'div',
        { class: 'ag-up-info-row__label' },
        isMuted ? strings.notificationsMuted : strings.notificationsEnabled,
      ),
    ),
    el(
      'span',
      { class: 'ag-up-switch' + (!isMuted ? ' is-on' : '') },
      el('span', { class: 'ag-up-switch__thumb' }),
    ),
  );

  return el(
    'div',
    { class: 'ag-up-card' },
    createCopyRow(tgIcons.at(18), meta.publicId, strings.labelPublicId, strings.copiedToast, true),
    el('div', { class: 'ag-up-divider' }),
    createCopyRow(
      tgIcons.key(18),
      meta.fingerprint,
      strings.labelFingerprint,
      strings.copiedToast,
      true,
    ),
    el('div', { class: 'ag-up-divider' }),
    createCopyRow(icons.user(18), meta.username, strings.labelUsername, strings.copiedToast, false),
    el('div', { class: 'ag-up-divider' }),
    autoDeleteRow,
    el('div', { class: 'ag-up-divider' }),
    notificationsRow,
  );
}
