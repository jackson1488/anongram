import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons, tgIcons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';
import { contactsStore } from '../contacts/ContactsStore';
import { openContactsOverlay } from '../contacts/ContactsOverlay';
import { qrT } from './i18n';
import { createQrSvg } from './qrcode';
import './qr.css';

interface BarcodeDetectorLike {
  detect(source: HTMLVideoElement): Promise<Array<{ rawValue?: string }>>;
}

declare global {
  interface Window {
    BarcodeDetector?: new (opts?: { formats?: string[] }) => BarcodeDetectorLike;
  }
}

export interface QrHubOptions {
  onOpenChat?: (chatId: string) => void;
}

export function openQrHubOverlay(options?: QrHubOptions): void {
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) closeOverlay();
    },
  });

  const box = el('div', { class: 'ag-card ag-qr-modal' });
  overlay.appendChild(box);

  let activeTab: 'my' | 'scan' = 'my';
  let activeStream: MediaStream | null = null;
  let scanTimer: number | null = null;
  let unsubI18n: (() => void) | null = null;

  const stopCamera = () => {
    if (scanTimer !== null) {
      window.clearInterval(scanTimer);
      scanTimer = null;
    }
    if (activeStream) {
      for (const track of activeStream.getTracks()) {
        track.stop();
      }
      activeStream = null;
    }
  };

  const closeOverlay = () => {
    stopCamera();
    unsubI18n?.();
    overlay.remove();
  };

  const handleAddPeer = (raw: string) => {
    const cleaned = raw.trim().replace(/^anongram:\/\/id\//i, '').replace(/^ag:\/\//i, '');
    if (!cleaned) return;
    const s = qrT();
    const shortName = cleaned.length > 14 ? `${cleaned.slice(0, 8)}…${cleaned.slice(-4)}` : cleaned;
    contactsStore.addContact({
      firstName: shortName,
      fingerprint: cleaned,
    });
    showToast(s.addedToast(shortName));
    closeOverlay();
  };

  const startScanner = async (container: HTMLElement) => {
    const s = qrT();
    stopCamera();
    clear(container);
    container.appendChild(
      el('div', { class: 'ag-qr-fallback' }, icons.video(28), el('span', null, s.cameraStarting)),
    );

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      clear(container);
      container.appendChild(
        el(
          'div',
          { class: 'ag-qr-fallback' },
          icons.alert(28),
          el('strong', null, s.cameraUnavailable),
          el('span', null, s.cameraPermissionHint),
        ),
      );
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false,
      });
      activeStream = stream;
      clear(container);

      const video = el('video', {
        class: 'ag-qr-video',
        autoplay: 'true',
        playsinline: 'true',
        muted: 'true',
      }) as HTMLVideoElement;
      video.srcObject = stream;
      void video.play().catch(() => {});

      container.append(video, el('div', { class: 'ag-qr-reticle' }));

      if (window.BarcodeDetector) {
        const detector = new window.BarcodeDetector({ formats: ['qr_code'] });
        scanTimer = window.setInterval(async () => {
          if (!activeStream || video.readyState < 2) return;
          try {
            const codes = await detector.detect(video);
            const val = codes.find((c) => c.rawValue)?.rawValue;
            if (val) {
              showToast(s.scanDetectedToast(val));
              handleAddPeer(val);
            }
          } catch {
            // ignore transient frame decode error
          }
        }, 450);
      }
    } catch {
      clear(container);
      container.appendChild(
        el(
          'div',
          { class: 'ag-qr-fallback' },
          icons.alert(28),
          el('strong', null, s.cameraUnavailable),
          el('span', null, s.cameraPermissionHint),
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--secondary',
              style: 'min-height:36px;',
              onClick: () => void startScanner(container),
            },
            tgIcons.refresh(15),
            s.retryCamera,
          ),
        ),
      );
    }
  };

  const render = () => {
    stopCamera();
    clear(box);
    const s = qrT();
    const id = authStore.identity;
    const publicId = id?.publicId ?? 'ag_anonymous';
    const qrPayload = `anongram://id/${publicId}`;

    const peerInput = el('input', {
      class: 'ag-input',
      placeholder: s.manualPlaceholder,
      onKeyDown: (e: Event) => {
        if ((e as KeyboardEvent).key === 'Enter') handleAddPeer(peerInput.value);
      },
    }) as HTMLInputElement;

    box.append(
      el(
        'div',
        { class: 'ag-qr-head' },
        el('strong', { style: 'font-size:17px;color:var(--ag-text-primary);' }, s.title),
        el('button', { class: 'ag-icon-btn', onClick: closeOverlay }, icons.close(18)),
      ),
      el(
        'div',
        { class: 'ag-qr-tabs' },
        el(
          'button',
          {
            class: `ag-qr-tab ${activeTab === 'my' ? 'is-active' : ''}`,
            onClick: () => {
              if (activeTab === 'my') return;
              activeTab = 'my';
              render();
            },
          },
          s.tabMyQr,
        ),
        el(
          'button',
          {
            class: `ag-qr-tab ${activeTab === 'scan' ? 'is-active' : ''}`,
            onClick: () => {
              if (activeTab === 'scan') return;
              activeTab = 'scan';
              render();
            },
          },
          s.tabScan,
        ),
      ),
    );

    if (activeTab === 'my') {
      box.append(
        el('div', { class: 'ag-qr-frame' }, createQrSvg(qrPayload)),
        el('code', { class: 'ag-ag1-code', style: 'width:100%;text-align:center;' }, publicId),
        el('span', { style: 'font-size:12px;color:var(--ag-text-secondary);' }, s.showCodeHint),
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--primary',
            style: 'width:100%;',
            onClick: async () => {
              await copyToClipboard(publicId);
              showToast(s.copiedToast);
            },
          },
          icons.copy(16),
          s.copyStealthId,
        ),
        el(
          'button',
          {
            class: 'ag-qr-contacts-btn',
            onClick: () => {
              closeOverlay();
              openContactsOverlay((chatId) => {
                options?.onOpenChat?.(chatId);
              });
            },
          },
          el('div', { class: 'ag-qr-contacts-icon' }, icons.users(18)),
          el(
            'div',
            { style: 'display:flex;flex-direction:column;gap:2px;flex:1;' },
            el('strong', { style: 'font-size:13px;color:var(--ag-text-primary);' }, s.myContactsTitle),
            el('span', { style: 'font-size:11px;color:var(--ag-text-secondary);' }, s.myContactsSub),
          ),
          icons.chevronRight(16),
        ),
      );
    } else {
      const scannerBox = el('div', { class: 'ag-qr-scanner-box' });
      box.append(
        scannerBox,
        el('span', { style: 'font-size:12px;color:var(--ag-text-secondary);text-align:center;' }, s.scanHint),
      );
      void startScanner(scannerBox);
    }

    box.append(
      el(
        'div',
        { class: 'ag-qr-manual-row' },
        peerInput,
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--secondary',
            onClick: () => handleAddPeer(peerInput.value),
          },
          s.addBtn,
        ),
      ),
    );
  };

  unsubI18n = i18nStore.subscribe(render);
  render();
  document.body.appendChild(overlay);
}
