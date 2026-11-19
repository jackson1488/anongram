import { i18nStore } from '../../../core/i18n/i18nStore';
import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import type { MessageAttachment } from '../models';

export function openMediaViewer(attachment: MessageAttachment): void {
  const t = i18nStore.t;
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });

  let mediaContent: HTMLElement;
  if (attachment.previewUrl && (attachment.type === 'photo' || attachment.type === 'album')) {
    mediaContent = el('img', {
      src: attachment.previewUrl,
      alt: attachment.title,
      class: 'ag-viewer-img',
    });
  } else if (attachment.type === 'video_circle') {
    mediaContent = el(
      'div',
      { class: 'ag-viewer-circle' },
      icons.video(48),
      el('div', null, attachment.title),
    );
  } else {
    mediaContent = el(
      'div',
      { class: 'ag-viewer-doc' },
      el('div', { class: 'ag-viewer-doc__badge' }, attachment.type.toUpperCase()),
      el('h3', null, attachment.title),
      el('p', null, attachment.fileSizeLabel ?? attachment.subtitle ?? ''),
    );
  }

  overlay.appendChild(
    el(
      'div',
      { class: 'ag-viewer-box' },
      el(
        'div',
        { class: 'ag-viewer-top' },
        el('span', null, attachment.title),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: t.common.close,
            onClick: () => overlay.remove(),
          },
          icons.close(20),
        ),
      ),
      el('div', { class: 'ag-viewer-body' }, mediaContent),
    ),
  );

  document.body.appendChild(overlay);
}
