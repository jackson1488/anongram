import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { AttachmentType, MessageAttachment } from '../../models';

export interface AttachHandlers {
  /** Send a ready attachment (closes the panel). */
  send: (att: MessageAttachment) => void;
  close: () => void;
}

interface AttachItem {
  type: AttachmentType;
  label: string;
  emoji: string;
  ico: () => SVGElement;
  action: () => void;
}

function pickFile(type: AttachmentType, accept: string, h: AttachHandlers): void {
  const input = document.createElement('input');
  input.type = 'file';
  if (accept) input.accept = accept;
  input.onchange = () => {
    const file = input.files?.[0];
    if (!file) return;
    const previewUrl =
      type === 'photo' || type === 'album' || type === 'music' ? URL.createObjectURL(file) : undefined;
    const sizeKb = Math.max(1, Math.round(file.size / 1024));
    const sizeLabel = sizeKb > 1024 ? `${(sizeKb / 1024).toFixed(1)} MB` : `${sizeKb} KB`;
    h.send({
      type,
      title: file.name,
      fileSizeLabel: sizeLabel,
      durationSec: type === 'music' ? 180 : undefined,
      previewUrl,
    });
  };
  input.click();
}

/** Mobile: bottom grid (Android attach sheet). Desktop (>=840px): Telegram Web A style popup. */
export function createAttachMenu(h: AttachHandlers): HTMLElement {
  const t = i18nStore.t;
  const items: AttachItem[] = [
    { type: 'photo', label: t.input.attachPhoto, emoji: '🖼️', ico: () => icons.image(20), action: () => pickFile('photo', 'image/*', h) },
    { type: 'album', label: t.input.attachAlbum, emoji: '🗂️', ico: () => icons.image(20), action: () => pickFile('album', 'image/*', h) },
    { type: 'file', label: t.input.attachFile, emoji: '📄', ico: () => icons.fileText(20), action: () => pickFile('file', '*/*', h) },
    { type: 'music', label: t.input.attachMusic, emoji: '🎵', ico: () => icons.music(20), action: () => pickFile('music', 'audio/*', h) },
    { type: 'voice', label: t.input.attachVoice, emoji: '🎙️', ico: () => icons.mic(20), action: () => h.send({ type: 'voice', title: t.input.attachVoice, durationSec: 12 }) },
    { type: 'video_circle', label: t.input.attachVideoCircle, emoji: '⭕', ico: () => icons.video(20), action: () => h.send({ type: 'video_circle', title: t.input.attachVideoCircle, durationSec: 9 }) },
    { type: 'geo', label: t.input.attachGeo, emoji: '📍', ico: () => icons.mapPin(20), action: () => h.send({ type: 'geo', title: t.input.attachGeo, subtitle: 'Encrypted Relay Node' }) },
    { type: 'contact', label: t.input.attachContact, emoji: '👤', ico: () => icons.user(20), action: () => h.send({ type: 'contact', title: 'Ghost Contact', subtitle: '@ghost_node' }) },
  ];

  if (window.innerWidth >= 840) {
    return el(
      'div',
      { class: 'ag-tgpop-wrap' },
      el('div', { class: 'ag-tgpop-backdrop', onClick: h.close }),
      el(
        'div',
        { class: 'ag-tgpop ag-attach-pop' },
        ...items
          .filter((it) => it.type !== 'album')
          .map((it) => el('button', { class: 'ag-tgpop__item', onClick: it.action }, it.ico(), el('span', {}, it.label))),
      ),
    );
  }
  return el(
    'div',
    { class: 'ag-drawer-panel ag-attach-grid' },
    ...items.map((it) =>
      el(
        'button',
        { class: 'ag-attach-tile', onClick: it.action },
        el('span', { class: 'ag-attach-tile__icon' }, it.emoji),
        el('span', { class: 'ag-attach-tile__label' }, it.label),
      ),
    ),
  );
}
