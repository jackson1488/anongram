import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons, tgIcons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';
import { chatStore } from '../chats/chatStore';
import { contactMonogram, contactsStore } from '../contacts/ContactsStore';
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

interface ParsedStealthPeer {
  rawCleaned: string;
  chatId: string;
  title: string;
  emoji: string;
  color: string;
  fingerprint: string;
  isStealthId: boolean;
}

const PEER_COLORS = ['#8B5CF6', '#0EA5E9', '#10B981', '#EC4899', '#F59E0B', '#6366F1'];

function qrExtraT() {
  const ru = i18nStore.language === 'ru';
  return {
    openChatBtn: ru ? 'Открыть чат' : 'Open Chat',
    chatOpenedToast: (name: string) => (ru ? `Чат открыт: ${name}` : `Chat opened: ${name}`),
    invalidStealthId: ru
      ? 'Введите корректный Stealth ID (ag_...) или @username'
      : 'Enter a valid Stealth ID (ag_...) or @username',
    readyToOpenHint: (name: string) =>
      ru ? `Готово к переписке: ${name}` : `Ready to message: ${name}`,
    scannedReady: ru ? 'QR-код распознан' : 'QR code detected',
    scanAgain: ru ? 'Сканировать снова' : 'Scan again',
  };
}

function normalizeRawPeer(raw: string): string {
  return raw
    .trim()
    .replace(/^anongram:\/\/id\//i, '')
    .replace(/^ag:\/\/id\//i, '')
    .replace(/^ag:\/\//i, '')
    .split(/[?#]/)[0]
    .trim();
}

function pickColor(seed: string): string {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  }
  return PEER_COLORS[hash % PEER_COLORS.length];
}

function parseStealthPeer(raw: string): ParsedStealthPeer | null {
  const cleaned = normalizeRawPeer(raw);
  if (!cleaned || /\s/.test(cleaned) || /:\/\//.test(cleaned)) {
    return null;
  }

  const isStealthId = /^ag_[a-z0-9_-]{3,64}$/i.test(cleaned);
  const isHandle =
    /^@?[a-z0-9а-яё_.-]{3,48}$/i.test(cleaned) && /[a-z0-9а-яё]/i.test(cleaned);
  if (!isStealthId && !isHandle) {
    return null;
  }

  const lower = cleaned.toLowerCase();
  const handleNormalized = `@${cleaned.replace(/^@+/, '').toLowerCase()}`;
  const matchedContact = contactsStore
    .getAll()
    .find(
      (c) =>
        c.id.toLowerCase() === lower ||
        c.fingerprint.toLowerCase() === lower ||
        c.handle.toLowerCase() === handleNormalized,
    );

  if (matchedContact) {
    return {
      rawCleaned: cleaned,
      chatId: matchedContact.id,
      title: matchedContact.name,
      emoji: matchedContact.emoji || contactMonogram(matchedContact.name),
      color: matchedContact.color,
      fingerprint: matchedContact.fingerprint,
      isStealthId,
    };
  }

  const slug = cleaned.replace(/^@+/, '').toLowerCase();
  const candidateChatId = isStealthId ? cleaned : `peer_${slug}`;
  const matchedThread = chatStore.threads.find(
    (t) =>
      t.id.toLowerCase() === lower ||
      t.id.toLowerCase() === candidateChatId.toLowerCase() ||
      t.title.toLowerCase() === lower ||
      t.fingerprint?.toLowerCase() === lower,
  );

  if (matchedThread) {
    return {
      rawCleaned: cleaned,
      chatId: matchedThread.id,
      title: matchedThread.title,
      emoji: matchedThread.avatarEmoji,
      color: matchedThread.avatarColor,
      fingerprint: matchedThread.fingerprint ?? cleaned,
      isStealthId,
    };
  }

  const title =
    isStealthId && cleaned.length > 16
      ? `${cleaned.slice(0, 8)}…${cleaned.slice(-4)}`
      : cleaned;

  return {
    rawCleaned: cleaned,
    chatId: candidateChatId,
    title,
    emoji: isStealthId ? '🕶️' : contactMonogram(title),
    color: pickColor(cleaned),
    fingerprint: cleaned,
    isStealthId,
  };
}

function openThreadInShell(chatId: string): void {
  if (!document.querySelector('.ag-mm')) {
    const mobileChatsTab = document.querySelector<HTMLButtonElement>(
      '.ag-island-bar .ag-island__tab:first-child',
    );
    if (mobileChatsTab) {
      mobileChatsTab.click();
    } else {
      const desktopBackBtn = document.querySelector<HTMLButtonElement>(
        '.ag-left-pane > .ag-chat-header > .ag-icon-btn:first-child',
      );
      desktopBackBtn?.click();
    }
  }

  const archiveBackBtn = document.querySelector<HTMLButtonElement>(
    '.ag-mm .ag-archive-header .ag-icon-btn',
  );
  if (archiveBackBtn && archiveBackBtn.getClientRects().length > 0) {
    archiveBackBtn.click();
  }

  const searchInput = document.querySelector<HTMLInputElement>('.ag-mm .ag-search-row input');
  if (searchInput && searchInput.value.length > 0) {
    searchInput.value = '';
    searchInput.dispatchEvent(new Event('input', { bubbles: true }));
  }

  const allChip = document.querySelector<HTMLButtonElement>(
    '.ag-mm .ag-chips-row .ag-chip:first-child',
  );
  if (allChip && !allChip.classList.contains('is-active')) {
    allChip.click();
  }

  const visibleThreads = chatStore.threads.filter((t) => !t.isArchived);
  const idx = visibleThreads.findIndex((t) => t.id === chatId);
  const rows = document.querySelectorAll<HTMLElement>('.ag-mm .ag-scroll-list .ag-chat-row');
  if (idx >= 0 && rows[idx]) {
    rows[idx].click();
  }
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
  let peerInputValue = '';
  let scannedValue: string | null = null;
  let syncPeerActionUi: ((raw: string) => void) | null = null;

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

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      closeOverlay();
    }
  };

  const closeOverlay = () => {
    document.removeEventListener('keydown', onKeyDown);
    stopCamera();
    unsubI18n?.();
    overlay.remove();
  };

  const handleAddPeer = (raw: string) => {
    const peer = parseStealthPeer(raw);
    if (!peer) {
      showToast(qrExtraT().invalidStealthId);
      return;
    }
    const s = qrT();
    const alreadyExists = contactsStore
      .getAll()
      .some((c) => c.fingerprint.toLowerCase() === peer.fingerprint.toLowerCase());
    if (!alreadyExists) {
      contactsStore.addContact({
        firstName: peer.title,
        fingerprint: peer.fingerprint,
      });
    }
    showToast(s.addedToast(peer.title));
    closeOverlay();
  };

  const handleOpenChat = (raw: string) => {
    const peer = parseStealthPeer(raw);
    const extra = qrExtraT();
    if (!peer) {
      showToast(extra.invalidStealthId);
      return;
    }
    const existing = chatStore.getThread(peer.chatId);
    if (existing?.isArchived) {
      chatStore.toggleArchive(existing.id);
    }
    const threadId = chatStore.ensureChat(
      peer.chatId,
      peer.title,
      peer.emoji,
      peer.color,
      false,
    );
    closeOverlay();
    showToast(extra.chatOpenedToast(peer.title));
    window.dispatchEvent(new CustomEvent('ag:open-chat', { detail: { chatId: threadId } }));
    if (options?.onOpenChat) {
      options.onOpenChat(threadId);
    } else {
      openThreadInShell(threadId);
    }
  };

  const renderScannedState = (container: HTMLElement, value: string) => {
    const extra = qrExtraT();
    clear(container);
    container.appendChild(
      el(
        'div',
        { class: 'ag-qr-fallback' },
        tgIcons.checkCircle(28),
        el('strong', { style: 'color:var(--ag-text-primary);' }, extra.scannedReady),
        el(
          'code',
          {
            class: 'ag-ag1-code',
            style: 'max-width:100%;overflow:hidden;text-overflow:ellipsis;',
          },
          value,
        ),
        el(
          'button',
          {
            type: 'button',
            class: 'ag-pill-btn ag-pill-btn--secondary',
            style: 'min-height:36px;border-radius:9999px;',
            onClick: () => {
              scannedValue = null;
              void startScanner(container);
            },
          },
          tgIcons.refresh(15),
          extra.scanAgain,
        ),
      ),
    );
  };

  const startScanner = async (container: HTMLElement) => {
    const s = qrT();
    stopCamera();
    if (scannedValue) {
      renderScannedState(container, scannedValue);
      return;
    }
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
              const cleaned = normalizeRawPeer(val) || val.trim();
              peerInputValue = cleaned;
              scannedValue = cleaned;
              stopCamera();
              const inputEl = box.querySelector<HTMLInputElement>('.ag-qr-manual-row .ag-input');
              if (inputEl) inputEl.value = cleaned;
              syncPeerActionUi?.(cleaned);
              showToast(s.scanDetectedToast(cleaned));
              renderScannedState(container, cleaned);
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
              type: 'button',
              class: 'ag-pill-btn ag-pill-btn--secondary',
              style: 'min-height:36px;border-radius:9999px;',
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
    const extra = qrExtraT();
    const id = authStore.identity;
    const publicId = id?.publicId ?? 'ag_anonymous';
    const qrPayload = `anongram://id/${publicId}`;

    const peerInput = el('input', {
      type: 'text',
      class: 'ag-input',
      value: peerInputValue,
      placeholder: s.manualPlaceholder,
      onInput: (e: Event) => {
        peerInputValue = (e.target as HTMLInputElement).value;
        scannedValue = null;
        syncPeerActionUi?.(peerInputValue);
      },
      onKeyDown: (e: Event) => {
        if ((e as KeyboardEvent).key === 'Enter') {
          e.preventDefault();
          handleOpenChat(peerInput.value);
        }
      },
    }) as HTMLInputElement;
    peerInput.value = peerInputValue;

    const addBtn = el(
      'button',
      {
        type: 'button',
        class: 'ag-pill-btn ag-pill-btn--secondary',
        style: 'border-radius:9999px;',
        onClick: () => handleAddPeer(peerInput.value),
      },
      s.addBtn,
    ) as HTMLButtonElement;

    const openChatBtn = el(
      'button',
      {
        type: 'button',
        class: 'ag-pill-btn ag-pill-btn--primary',
        style: 'width:100%;border-radius:9999px;',
        onClick: () => handleOpenChat(peerInput.value),
      },
      icons.chats(16),
      extra.openChatBtn,
    ) as HTMLButtonElement;

    const statusHint = el('div', {
      style: 'width:100%;font-size:12px;text-align:center;min-height:16px;',
    });

    syncPeerActionUi = (raw: string) => {
      const parsed = parseStealthPeer(raw);
      const valid = Boolean(parsed);
      openChatBtn.disabled = !valid;
      openChatBtn.style.opacity = valid ? '1' : '0.45';
      openChatBtn.style.cursor = valid ? 'pointer' : 'not-allowed';
      addBtn.disabled = !valid;
      addBtn.style.opacity = valid ? '1' : '0.55';
      if (parsed) {
        statusHint.style.color = 'var(--ag-accent)';
        statusHint.textContent = extra.readyToOpenHint(parsed.title);
      } else if (raw.trim().length > 0) {
        statusHint.style.color = 'var(--ag-danger)';
        statusHint.textContent = extra.invalidStealthId;
      } else {
        statusHint.style.color = 'var(--ag-text-secondary)';
        statusHint.textContent = '';
      }
    };

    box.append(
      el(
        'div',
        { class: 'ag-qr-head' },
        el('strong', { style: 'font-size:17px;color:var(--ag-text-primary);' }, s.title),
        el('button', { type: 'button', class: 'ag-icon-btn', onClick: closeOverlay }, icons.close(18)),
      ),
      el(
        'div',
        { class: 'ag-qr-tabs' },
        el(
          'button',
          {
            type: 'button',
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
            type: 'button',
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
            type: 'button',
            class: 'ag-pill-btn ag-pill-btn--primary',
            style: 'width:100%;border-radius:9999px;',
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
            type: 'button',
            class: 'ag-qr-contacts-btn',
            onClick: () => {
              closeOverlay();
              openContactsOverlay((chatId) => {
                if (options?.onOpenChat) {
                  options.onOpenChat(chatId);
                } else {
                  openThreadInShell(chatId);
                }
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
      el('div', { class: 'ag-qr-manual-row' }, peerInput, addBtn),
      statusHint,
      openChatBtn,
    );
    syncPeerActionUi(peerInputValue);
  };

  document.addEventListener('keydown', onKeyDown);
  unsubI18n = i18nStore.subscribe(render);
  render();
  document.body.appendChild(overlay);
}
