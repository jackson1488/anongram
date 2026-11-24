import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { inputT } from '../../input/i18n/InputI18n';
import { openVideoCircleRecorder } from '../../input/video/VideoCircleRecordOverlay';
import { mediaRecorderController } from '../../input/voice/MediaRecorderController';
import { createVoiceRecordingBar } from '../../input/voice/VoiceRecordingBar';
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

/** Opens a modal voice recorder backed by MediaRecorderController and VoiceRecordingBar. */
function openAttachVoiceRecorder(onSend: (att: MessageAttachment) => void): void {
  let finished = false;
  const s = inputT();

  const cleanupModal = () => {
    document.removeEventListener('keydown', onKeyDown);
    (voiceBar as unknown as { __cleanup?: () => void }).__cleanup?.();
    overlay.classList.remove('is-open');
    window.setTimeout(() => overlay.remove(), 200);
  };

  const handleCancel = () => {
    if (finished) return;
    finished = true;
    mediaRecorderController.cancel();
    cleanupModal();
  };

  const handleSend = async () => {
    if (finished) return;
    finished = true;
    const res = await mediaRecorderController.stopAndSave();
    cleanupModal();
    onSend({
      type: 'voice',
      title: inputT().voiceMessage,
      durationSec: Math.max(1, res.durationSec),
      previewUrl: res.dataUrl || undefined,
    });
  };

  const voiceBar = createVoiceRecordingBar({
    onCancel: handleCancel,
    onSend: () => {
      void handleSend();
    },
  });

  const header = el(
    'div',
    {
      style: 'display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:28px;',
    },
    el(
      'div',
      { style: 'display:flex;align-items:center;gap:8px;min-width:0;' },
      el('span', { style: 'display:inline-flex;color:var(--ag-accent);flex-shrink:0;' }, icons.mic(18)),
      el(
        'span',
        {
          style: 'font-size:15px;font-weight:700;color:var(--ag-text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;',
        },
        s.voiceRecording,
      ),
    ),
    el(
      'button',
      {
        type: 'button',
        class: 'ag-icon-btn',
        title: s.cancel,
        'aria-label': s.cancel,
        style: 'width:32px;height:32px;',
        onClick: handleCancel,
      },
      icons.close(16),
    ),
  );

  const dialog = el(
    'div',
    {
      class: 'ag-dialog',
      style: 'max-width:420px;overflow:visible;',
      role: 'dialog',
      'aria-modal': 'true',
      'aria-label': s.voiceRecording,
    },
    header,
    voiceBar,
  );

  const overlay = el('div', { class: 'ag-overlay ag-overlay--dialog' }, dialog);

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      handleCancel();
    }
  };

  overlay.addEventListener('mousedown', (e) => {
    if (e.target === overlay) handleCancel();
  });
  document.addEventListener('keydown', onKeyDown);
  document.body.appendChild(overlay);
  window.requestAnimationFrame(() => overlay.classList.add('is-open'));

  void mediaRecorderController.startVoice();
  mediaRecorderController.lock();
}

/** Mobile: bottom grid (Android attach sheet). Desktop (>=840px): Telegram Web A style popup. */
export function createAttachMenu(h: AttachHandlers): HTMLElement {
  const t = i18nStore.t;
  let cleanup = () => {};

  const safeHandlers: AttachHandlers = {
    send: (att) => {
      cleanup();
      h.send(att);
    },
    close: () => {
      cleanup();
      h.close();
    },
  };

  const items: AttachItem[] = [
    { type: 'photo', label: t.input.attachPhoto, emoji: '🖼️', ico: () => icons.image(20), action: () => pickFile('photo', 'image/*', safeHandlers) },
    { type: 'album', label: t.input.attachAlbum, emoji: '🗂️', ico: () => icons.image(20), action: () => pickFile('album', 'image/*', safeHandlers) },
    { type: 'file', label: t.input.attachFile, emoji: '📄', ico: () => icons.fileText(20), action: () => pickFile('file', '*/*', safeHandlers) },
    { type: 'music', label: t.input.attachMusic, emoji: '🎵', ico: () => icons.music(20), action: () => pickFile('music', 'audio/*', safeHandlers) },
    {
      type: 'voice',
      label: t.input.attachVoice,
      emoji: '🎙️',
      ico: () => icons.mic(20),
      action: () => {
        safeHandlers.close();
        openAttachVoiceRecorder((att) => h.send(att));
      },
    },
    {
      type: 'video_circle',
      label: t.input.attachVideoCircle,
      emoji: '⭕',
      ico: () => icons.video(20),
      action: () => {
        safeHandlers.close();
        openVideoCircleRecorder({
          onSend: (dataUrl, durationSec) => {
            h.send({
              type: 'video_circle',
              title: inputT().videoCircle,
              durationSec: Math.max(1, durationSec),
              previewUrl: dataUrl || undefined,
            });
          },
        });
      },
    },
    { type: 'geo', label: t.input.attachGeo, emoji: '📍', ico: () => icons.mapPin(20), action: () => safeHandlers.send({ type: 'geo', title: t.input.attachGeo, subtitle: 'Encrypted Relay Node' }) },
    { type: 'contact', label: t.input.attachContact, emoji: '👤', ico: () => icons.user(20), action: () => safeHandlers.send({ type: 'contact', title: 'Ghost Contact', subtitle: '@ghost_node' }) },
  ];

  if (window.innerWidth >= 840) {
    const pop = el(
      'div',
      { class: 'ag-tgpop ag-attach-pop', role: 'menu' },
      ...items
        .filter((it) => it.type !== 'album')
        .map((it) =>
          el(
            'button',
            {
              type: 'button',
              class: 'ag-tgpop__item',
              role: 'menuitem',
              onMouseDown: (e: Event) => e.preventDefault(),
              onClick: it.action,
            },
            it.ico(),
            el('span', {}, it.label),
          ),
        ),
    );
    const wrap = el(
      'div',
      { class: 'ag-tgpop-wrap' },
      el('div', { class: 'ag-tgpop-backdrop', onClick: safeHandlers.close }),
      pop,
    );

    const positionPopup = () => {
      if (!wrap.isConnected) return;
      const dock = wrap.closest('.ag-cs-dock') ?? wrap.parentElement?.parentElement;
      const attachBtn = dock?.querySelector('.ag-composer__attach') as HTMLElement | null;
      const innerPill = dock?.querySelector('.ag-composer__inner-pill') as HTMLElement | null;
      const outerPill = dock?.querySelector('.ag-composer__pill') as HTMLElement | null;
      if (!attachBtn) return;
      const wrapRect = wrap.getBoundingClientRect();
      const btnRect = attachBtn.getBoundingClientRect();
      if (btnRect.width === 0 || btnRect.height === 0) return;

      const innerRect = innerPill?.getBoundingClientRect();
      const outerRect = outerPill?.getBoundingClientRect();
      const pillRect =
        innerRect && innerRect.height > 0
          ? innerRect
          : outerRect && outerRect.height > 0
            ? outerRect
            : undefined;
      const anchorTop = pillRect ? Math.min(btnRect.top, pillRect.top) : btnRect.top;
      const rawLeft = Math.max(8, Math.round(btnRect.left - wrapRect.left));
      const left =
        wrapRect.width > 0
          ? Math.min(Math.max(8, wrapRect.width - (pop.offsetWidth || 216) - 8), rawLeft)
          : rawLeft;
      const bottom = Math.max(8, Math.round(wrapRect.bottom - anchorTop + 8));

      pop.style.left = `${left}px`;
      pop.style.bottom = `${bottom}px`;
      pop.style.transformOrigin = 'bottom left';

      const availTop = anchorTop - 16;
      if (availTop > 120 && pop.scrollHeight > availTop) {
        pop.style.maxHeight = `${Math.round(availTop)}px`;
        pop.style.overflowY = 'auto';
      } else {
        pop.style.maxHeight = '';
        pop.style.overflowY = '';
      }
    };

    const onKey = (e: KeyboardEvent) => {
      if (!wrap.isConnected) {
        cleanup();
        return;
      }
      if (e.key === 'Escape') {
        e.stopPropagation();
        safeHandlers.close();
      }
    };
    const onResize = () => {
      if (!wrap.isConnected) {
        cleanup();
        return;
      }
      positionPopup();
    };
    cleanup = () => {
      document.removeEventListener('keydown', onKey);
      window.removeEventListener('resize', onResize);
    };

    document.addEventListener('keydown', onKey);
    window.addEventListener('resize', onResize, { passive: true });
    queueMicrotask(positionPopup);
    requestAnimationFrame(positionPopup);
    return wrap;
  }

  return el(
    'div',
    { class: 'ag-drawer-panel ag-attach-grid' },
    ...items.map((it) =>
      el(
        'button',
        { type: 'button', class: 'ag-attach-tile', onClick: it.action },
        el('span', { class: 'ag-attach-tile__icon' }, it.emoji),
        el('span', { class: 'ag-attach-tile__label' }, it.label),
      ),
    ),
  );
}
