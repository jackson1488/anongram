import { i18nStore, registerI18nModule } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { MessageAttachment } from '../../models';

export interface AttachHandlers {
  /** Send a ready attachment (closes the panel). */
  send: (att: MessageAttachment) => void;
  close: () => void;
}

interface AttachPopupI18n {
  photoOrVideo: string;
  fileOrDoc: string;
  location: string;
  locationModalTitle: string;
  locationDetecting: string;
  locationDetected: string;
  locationFallbackHint: string;
  locationRefreshBtn: string;
  locationLabelField: string;
  locationLabelPlaceholder: string;
  locationSendBtn: string;
  locationCancelBtn: string;
  albumTitle: (count: number) => string;
}

const RU_ATTACH: AttachPopupI18n = {
  photoOrVideo: 'Фото или видео',
  fileOrDoc: 'Файл / Документ',
  location: 'Геопозиция',
  locationModalTitle: 'Отправка геопозиции',
  locationDetecting: 'Определение координат…',
  locationDetected: 'Текущие координаты',
  locationFallbackHint: 'Координаты узла (резервный режим)',
  locationRefreshBtn: 'Обновить GPS',
  locationLabelField: 'Подпись места (необязательно)',
  locationLabelPlaceholder: 'Например: Точка встречи',
  locationSendBtn: 'Отправить геопозицию',
  locationCancelBtn: 'Отмена',
  albumTitle: (n) => `Альбом (${n})`,
};

const EN_ATTACH: AttachPopupI18n = {
  photoOrVideo: 'Photo or Video',
  fileOrDoc: 'Document or File',
  location: 'Location',
  locationModalTitle: 'Share Location',
  locationDetecting: 'Detecting coordinates…',
  locationDetected: 'Current coordinates',
  locationFallbackHint: 'Relay coordinates (fallback mode)',
  locationRefreshBtn: 'Refresh GPS',
  locationLabelField: 'Place label (optional)',
  locationLabelPlaceholder: 'e.g. Meeting point',
  locationSendBtn: 'Send Location',
  locationCancelBtn: 'Cancel',
  albumTitle: (n) => `Album (${n})`,
};

registerI18nModule('attachPopup', {
  ru: {
    photoOrVideo: RU_ATTACH.photoOrVideo,
    fileOrDoc: RU_ATTACH.fileOrDoc,
    location: RU_ATTACH.location,
    locationModalTitle: RU_ATTACH.locationModalTitle,
    locationDetecting: RU_ATTACH.locationDetecting,
    locationDetected: RU_ATTACH.locationDetected,
    locationFallbackHint: RU_ATTACH.locationFallbackHint,
    locationRefreshBtn: RU_ATTACH.locationRefreshBtn,
    locationLabelField: RU_ATTACH.locationLabelField,
    locationLabelPlaceholder: RU_ATTACH.locationLabelPlaceholder,
    locationSendBtn: RU_ATTACH.locationSendBtn,
    locationCancelBtn: RU_ATTACH.locationCancelBtn,
  },
  en: {
    photoOrVideo: EN_ATTACH.photoOrVideo,
    fileOrDoc: EN_ATTACH.fileOrDoc,
    location: EN_ATTACH.location,
    locationModalTitle: EN_ATTACH.locationModalTitle,
    locationDetecting: EN_ATTACH.locationDetecting,
    locationDetected: EN_ATTACH.locationDetected,
    locationFallbackHint: EN_ATTACH.locationFallbackHint,
    locationRefreshBtn: EN_ATTACH.locationRefreshBtn,
    locationLabelField: EN_ATTACH.locationLabelField,
    locationLabelPlaceholder: EN_ATTACH.locationLabelPlaceholder,
    locationSendBtn: EN_ATTACH.locationSendBtn,
    locationCancelBtn: EN_ATTACH.locationCancelBtn,
  },
});

function attachT(): AttachPopupI18n {
  return i18nStore.language === 'ru' ? RU_ATTACH : EN_ATTACH;
}

const FALLBACK_COORDS = '42.8746° N, 74.5698° E';

function formatFileSize(bytes: number): string {
  const sizeKb = Math.max(1, Math.round(bytes / 1024));
  return sizeKb > 1024 ? `${(sizeKb / 1024).toFixed(1)} MB` : `${sizeKb} KB`;
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve) => {
    // For larger files (> 12 MB), use object URL immediately to avoid memory spikes
    if (file.size > 12 * 1024 * 1024) {
      resolve(URL.createObjectURL(file));
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string' && reader.result.length > 0) {
        resolve(reader.result);
      } else {
        resolve(URL.createObjectURL(file));
      }
    };
    reader.onerror = () => resolve(URL.createObjectURL(file));
    reader.readAsDataURL(file);
  });
}

function formatCoords(lat: number, lon: number): string {
  const ns = lat >= 0 ? 'N' : 'S';
  const ew = lon >= 0 ? 'E' : 'W';
  return `${Math.abs(lat).toFixed(4)}° ${ns}, ${Math.abs(lon).toFixed(4)}° ${ew}`;
}

/**
 * 1. Photo or Video picker (accepts images & videos, multiple selection)
 * - 1 file selected -> sends 'photo' (with isVideo when video)
 * - multiple files selected -> sends 'album' with itemCount
 */
function pickPhotoOrVideo(h: AttachHandlers): void {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*,video/*';
  input.multiple = true;
  input.onchange = async () => {
    const files = input.files ? Array.from(input.files) : [];
    if (files.length === 0) return;

    const first = files[0];
    const previewUrl = await readFileAsDataUrl(first);
    const totalBytes = files.reduce((acc, f) => acc + f.size, 0);
    const sizeLabel = formatFileSize(totalBytes);

    if (files.length === 1) {
      const isVideo = first.type.startsWith('video/');
      h.send({
        type: 'photo',
        title: first.name,
        fileSizeLabel: sizeLabel,
        previewUrl,
        isVideo,
      });
    } else {
      const s = attachT();
      h.send({
        type: 'album',
        title: s.albumTitle(files.length),
        fileSizeLabel: sizeLabel,
        previewUrl,
        itemCount: files.length,
      });
    }
  };
  input.click();
}

/**
 * 2. Document / File picker (accepts any file)
 * Sends 'file' with real filename, file size label, and previewUrl dataURL.
 */
function pickDocumentFile(h: AttachHandlers): void {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '*/*';
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    const previewUrl = await readFileAsDataUrl(file);
    const sizeLabel = formatFileSize(file.size);
    h.send({
      type: 'file',
      title: file.name,
      fileSizeLabel: sizeLabel,
      previewUrl,
    });
  };
  input.click();
}

/**
 * 3. Location modal/sheet:
 * Attempts `navigator.geolocation.getCurrentPosition` with instant fallback coordinates
 * (`42.8746° N, 74.5698° E`) + optional place label input + "Отправить геопозицию" / "Send Location".
 */
function openLocationModal(onSend: (att: MessageAttachment) => void): void {
  const s = attachT();
  let currentCoords = FALLBACK_COORDS;
  let closed = false;

  const statusTextEl = el(
    'span',
    {
      style: 'font-size:12px;color:var(--ag-text-secondary);line-height:1.3;',
    },
    s.locationDetecting,
  );

  const coordsValueEl = el(
    'strong',
    {
      style:
        'font-size:14.5px;font-weight:700;color:var(--ag-text-primary);font-variant-numeric:tabular-nums;letter-spacing:0.01em;',
    },
    currentCoords,
  );

  const requestGeo = () => {
    statusTextEl.textContent = attachT().locationDetecting;
    if (!('geolocation' in navigator) || typeof navigator.geolocation?.getCurrentPosition !== 'function') {
      currentCoords = FALLBACK_COORDS;
      coordsValueEl.textContent = currentCoords;
      statusTextEl.textContent = attachT().locationFallbackHint;
      return;
    }
    try {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          if (closed) return;
          currentCoords = formatCoords(pos.coords.latitude, pos.coords.longitude);
          coordsValueEl.textContent = currentCoords;
          statusTextEl.textContent = attachT().locationDetected;
        },
        () => {
          if (closed) return;
          currentCoords = FALLBACK_COORDS;
          coordsValueEl.textContent = currentCoords;
          statusTextEl.textContent = attachT().locationFallbackHint;
        },
        { enableHighAccuracy: false, timeout: 4500, maximumAge: 60000 },
      );
    } catch {
      currentCoords = FALLBACK_COORDS;
      coordsValueEl.textContent = currentCoords;
      statusTextEl.textContent = attachT().locationFallbackHint;
    }
  };

  const placeInput = el('input', {
    type: 'text',
    class: 'ag-input',
    placeholder: s.locationLabelPlaceholder,
    autocomplete: 'off',
    'aria-label': s.locationLabelField,
    style: [
      'width:100%',
      'height:42px',
      'padding:0 14px',
      'border-radius:20px',
      'background:var(--ag-surface)',
      'border:1px solid var(--ag-card-border)',
      'color:var(--ag-text-primary)',
      'font-size:14px',
      'outline:none',
    ].join(';'),
  }) as HTMLInputElement;

  const closeModal = () => {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKeyDown);
    backdrop.remove();
  };

  const handleSubmit = () => {
    if (closed) return;
    const customLabel = placeInput.value.trim();
    const curS = attachT();
    const title = customLabel || curS.location;
    const subtitle = currentCoords;
    closeModal();
    onSend({
      type: 'geo',
      title,
      subtitle,
    });
  };

  placeInput.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.isComposing) {
      e.preventDefault();
      handleSubmit();
    }
  });

  const header = el(
    'div',
    {
      style: 'display:flex;align-items:center;justify-content:space-between;gap:10px;',
    },
    el(
      'div',
      { style: 'display:flex;align-items:center;gap:10px;min-width:0;' },
      el(
        'span',
        {
          style: [
            'width:36px',
            'height:36px',
            'border-radius:9999px',
            'background:var(--ag-accent-soft)',
            'color:var(--ag-accent)',
            'display:inline-flex',
            'align-items:center',
            'justify-content:center',
            'flex-shrink:0',
          ].join(';'),
        },
        icons.mapPin(18),
      ),
      el(
        'h3',
        {
          style:
            'margin:0;font-size:16.5px;font-weight:700;color:var(--ag-text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;',
        },
        s.locationModalTitle,
      ),
    ),
    el(
      'button',
      {
        type: 'button',
        class: 'ag-icon-btn',
        title: s.locationCancelBtn,
        'aria-label': s.locationCancelBtn,
        style: 'width:32px;height:32px;',
        onClick: closeModal,
      },
      icons.close(16),
    ),
  );

  const coordsCard = el(
    'div',
    {
      style: [
        'display:flex',
        'align-items:center',
        'justify-content:space-between',
        'gap:10px',
        'padding:12px 14px',
        'border-radius:18px',
        'background:var(--ag-surface)',
        'border:1px solid var(--ag-card-border)',
      ].join(';'),
    },
    el(
      'div',
      { style: 'display:flex;flex-direction:column;gap:3px;min-width:0;' },
      statusTextEl,
      coordsValueEl,
    ),
    el(
      'button',
      {
        type: 'button',
        title: s.locationRefreshBtn,
        'aria-label': s.locationRefreshBtn,
        style: [
          'height:32px',
          'padding:0 12px',
          'border-radius:9999px',
          'border:1px solid var(--ag-island-border)',
          'background:var(--ag-card)',
          'color:var(--ag-accent)',
          'font-size:12px',
          'font-weight:600',
          'cursor:pointer',
          'flex-shrink:0',
        ].join(';'),
        onClick: requestGeo,
      },
      s.locationRefreshBtn,
    ),
  );

  const labelFieldGroup = el(
    'div',
    { style: 'display:flex;flex-direction:column;gap:6px;' },
    el(
      'label',
      {
        style: 'font-size:12px;font-weight:600;color:var(--ag-text-secondary);padding-left:4px;',
      },
      s.locationLabelField,
    ),
    placeInput,
  );

  const actionsRow = el(
    'div',
    {
      style: 'display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:4px;',
    },
    el(
      'button',
      {
        type: 'button',
        class: 'ag-delete-dialog__cancel',
        style: [
          'height:40px',
          'padding:0 16px',
          'border-radius:9999px',
          'border:none',
          'background:transparent',
          'color:var(--ag-text-secondary)',
          'font-size:14px',
          'font-weight:600',
          'cursor:pointer',
        ].join(';'),
        onClick: closeModal,
      },
      s.locationCancelBtn,
    ),
    el(
      'button',
      {
        type: 'button',
        style: [
          'height:40px',
          'padding:0 18px',
          'border-radius:9999px',
          'border:none',
          'background:var(--ag-accent)',
          'color:var(--ag-on-accent)',
          'font-size:14px',
          'font-weight:700',
          'display:inline-flex',
          'align-items:center',
          'gap:8px',
          'cursor:pointer',
        ].join(';'),
        onClick: handleSubmit,
      },
      icons.mapPin(16),
      el('span', null, s.locationSendBtn),
    ),
  );

  const dialog = el(
    'div',
    {
      class: 'ag-delete-dialog',
      role: 'dialog',
      'aria-modal': 'true',
      'aria-label': s.locationModalTitle,
      style: 'max-width:380px;',
    },
    header,
    coordsCard,
    labelFieldGroup,
    actionsRow,
  );

  const backdrop = el(
    'div',
    {
      class: 'ag-delete-dialog-backdrop',
      onMouseDown: (e: Event) => {
        if (e.target === backdrop) closeModal();
      },
    },
    dialog,
  );

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      closeModal();
    }
  };

  document.addEventListener('keydown', onKeyDown);
  document.body.appendChild(backdrop);
  requestGeo();
  window.requestAnimationFrame(() => placeInput.focus());
}

/**
 * Compact 3-item popup menu above the paperclip button on BOTH mobile (<840px) and desktop (>=840px):
 * 1. Фото или видео (Photo or Video)
 * 2. Файл / Документ (Document or File)
 * 3. Геопозиция (Location)
 */
export function createAttachMenu(h: AttachHandlers): HTMLElement {
  const s = attachT();
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

  const menuItems: Array<{
    label: string;
    ico: () => SVGElement;
    action: () => void;
  }> = [
    {
      label: s.photoOrVideo,
      ico: () => icons.image(20),
      action: () => pickPhotoOrVideo(safeHandlers),
    },
    {
      label: s.fileOrDoc,
      ico: () => icons.fileText(20),
      action: () => pickDocumentFile(safeHandlers),
    },
    {
      label: s.location,
      ico: () => icons.mapPin(20),
      action: () => {
        safeHandlers.close();
        openLocationModal((att) => h.send(att));
      },
    },
  ];

  const pop = el(
    'div',
    { class: 'ag-tgpop ag-attach-pop', role: 'menu' },
    ...menuItems.map((it) =>
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
        el('span', null, it.label),
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
    const innerRect = innerPill?.getBoundingClientRect();
    const outerRect = outerPill?.getBoundingClientRect();

    const isMobile = window.innerWidth < 840;
    const pillRect = isMobile
      ? outerRect && outerRect.height > 0
        ? outerRect
        : innerRect && innerRect.height > 0
          ? innerRect
          : undefined
      : innerRect && innerRect.height > 0
        ? innerRect
        : outerRect && outerRect.height > 0
          ? outerRect
          : undefined;

    const hasBtnRect = btnRect.width > 0 && btnRect.height > 0;
    const anchorTop = pillRect
      ? hasBtnRect
        ? Math.min(btnRect.top, pillRect.top)
        : pillRect.top
      : btnRect.top;

    const popW = pop.offsetWidth || 224;
    const bottom = Math.max(8, Math.round(wrapRect.bottom - anchorTop + 8));
    pop.style.bottom = `${bottom}px`;

    if (isMobile) {
      // On mobile (<840px), the paperclip sits on the right side of .ag-composer__pill
      const anchorRight = hasBtnRect
        ? btnRect.right
        : pillRect
          ? pillRect.right - 44
          : wrapRect.right - 52;
      const rawLeft = Math.round(anchorRight - wrapRect.left - popW);
      const maxLeft = wrapRect.width > 0 ? Math.max(8, wrapRect.width - popW - 8) : rawLeft;
      const left = Math.max(8, Math.min(maxLeft, rawLeft));
      pop.style.left = `${left}px`;
      pop.style.right = 'auto';
      pop.style.transformOrigin = 'bottom right';
    } else {
      // On desktop (>=840px), the paperclip sits on the left side of .ag-composer__inner-pill
      const anchorLeft = hasBtnRect
        ? btnRect.left
        : pillRect
          ? pillRect.left
          : wrapRect.left + 12;
      const rawLeft = Math.max(8, Math.round(anchorLeft - wrapRect.left));
      const left =
        wrapRect.width > 0
          ? Math.min(Math.max(8, wrapRect.width - popW - 8), rawLeft)
          : rawLeft;
      pop.style.left = `${left}px`;
      pop.style.right = 'auto';
      pop.style.transformOrigin = 'bottom left';
    }

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
