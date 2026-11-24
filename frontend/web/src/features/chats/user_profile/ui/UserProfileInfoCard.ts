import { copyToClipboard, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { blockedUsersStore } from '../data/BlockedUsersStore';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import type { AutoDeleteConfig, PeerCryptoMetadata } from '../model/UserProfileModels';
import { openAutoDeleteCustomSheet } from './AutoDeleteCustomSheet';

export interface UserProfileInfoCardProps {
  chatId?: string;
  meta: PeerCryptoMetadata;
  autoDelete: AutoDeleteConfig;
  isMuted: boolean;
  strings: UserProfileStrings;
  onOpenAutoDelete?: () => void;
  onToggleNotifications?: () => void;
}

async function copyTextReliable(value: string): Promise<boolean> {
  if (await copyToClipboard(value)) return true;
  try {
    const ta = document.createElement('textarea');
    ta.value = value;
    ta.setAttribute('readonly', '');
    ta.style.cssText = 'position:fixed;left:-9999px;top:-9999px;opacity:0;';
    document.body.appendChild(ta);
    ta.select();
    const ok = document.execCommand('copy');
    ta.remove();
    return ok;
  } catch {
    return false;
  }
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
      type: 'button',
      class: 'ag-up-info-row',
      onClick: () => {
        void copyTextReliable(value).then((ok) => {
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
  const { chatId, meta, autoDelete, isMuted, strings, onOpenAutoDelete, onToggleNotifications } = props;

  const autoDeleteRow = el(
    'button',
    {
      type: 'button',
      class: 'ag-up-info-row',
      onClick: () => {
        if (onOpenAutoDelete) {
          onOpenAutoDelete();
        } else if (chatId) {
          openAutoDeleteCustomSheet({
            current: blockedUsersStore.getAutoDelete(chatId),
            strings,
            onApply: (cfg) => {
              blockedUsersStore.setAutoDelete(chatId, cfg);
              showToast(strings.autoDeleteUpdatedToast);
            },
          });
        }
      },
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
      type: 'button',
      class: 'ag-up-info-row',
      role: 'switch',
      'aria-checked': String(!isMuted),
      onClick: () => {
        if (onToggleNotifications) {
          onToggleNotifications();
        } else if (chatId) {
          const nowMuted = blockedUsersStore.toggleMute(chatId);
          showToast(nowMuted ? strings.mutedToast : strings.unmutedToast);
        }
      },
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
