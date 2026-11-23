import { el } from '../../../core/ui/dom';
import { allIcons as icons } from '../../../core/ui/icons';
import { openBottomSheet, pillButton, type ModalHandle } from '../../../core/ui/components';
import { archiveT } from './i18n/ArchiveI18n';
import './archive.css';

function infoRow(icon: SVGElement, title: string, description: string): HTMLElement {
  return el(
    'div',
    { class: 'ag-archive-info__row' },
    el('div', { class: 'ag-archive-info__row-icon' }, icon),
    el(
      'div',
      {},
      el('div', { class: 'ag-archive-info__row-title' }, title),
      el('div', { class: 'ag-archive-info__row-desc' }, description),
    ),
  );
}

export function openArchiveInfoBottomSheet(onOpenSettings: () => void): ModalHandle {
  const s = archiveT();
  let handle: ModalHandle;

  const content = el(
    'div',
    { class: 'ag-archive-info' },
    el('div', { class: 'ag-archive-info__badge' }, icons.archive(28)),
    el('div', { class: 'ag-archive-info__title' }, s.thisIsYourArchiveTitle),
    el('div', { class: 'ag-archive-info__subtitle' }, s.thisIsYourArchiveSubtitlePrefix),
    el(
      'button',
      {
        class: 'ag-archive-info__change',
        type: 'button',
        onclick: () => {
          handle.close();
          onOpenSettings();
        },
      },
      `${s.changeLink} ›`,
    ),
    el(
      'div',
      { class: 'ag-archive-info__rows' },
      infoRow(icons.archive(20), s.infoArchiveChatsTitle, s.infoArchiveChatsDesc),
      infoRow(icons.eyeOff(20), s.infoHideArchiveTitle, s.infoHideArchiveDesc),
      infoRow(icons.bellOff(20), s.infoNotificationsTitle, s.infoNotificationsDesc),
    ),
  );

  const okBtn = pillButton(s.okButton, () => handle.close(), 'primary');
  okBtn.classList.add('ag-archive-info__ok');
  content.appendChild(okBtn);

  handle = openBottomSheet(content);
  return handle;
}
