import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { inputT } from '../i18n/InputI18n';
import '../inputRecording.css';
import {
  mediaRecorderController,
  type RecorderState,
} from '../voice/MediaRecorderController';

export interface VideoCircleRecorderOptions {
  onSend: (dataUrl: string, durationSec: number) => void;
  onCancel?: () => void;
}

export interface VideoCircleRecorderHandle {
  close(): void;
}

const SVG_NS = 'http://www.w3.org/2000/svg';
const MAX_CIRCLE_DURATION_SEC = 60;
const PROGRESS_RADIUS = 48;
const PROGRESS_CIRCUMFERENCE = 2 * Math.PI * PROGRESS_RADIUS;

function formatTimer(totalSec: number): string {
  const safe = Math.max(0, Math.floor(totalSec));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${mins}:${String(secs).padStart(2, '0')}`;
}

function createSwitchCameraIcon(size = 20): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '2');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.innerHTML =
    '<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>' +
    '<path d="M9 13a3 3 0 0 1 5.12-2.12L15 12"/>' +
    '<path d="M15 13a3 3 0 0 1-5.12 2.12L9 14"/>';
  return svg;
}

function createProgressSvg(): { svg: SVGSVGElement; arc: SVGCircleElement } {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('class', 'ag-vc-progress-svg');
  svg.setAttribute('viewBox', '0 0 100 100');

  const track = document.createElementNS(SVG_NS, 'circle');
  track.setAttribute('class', 'ag-vc-progress-track');
  track.setAttribute('cx', '50');
  track.setAttribute('cy', '50');
  track.setAttribute('r', String(PROGRESS_RADIUS));

  const arc = document.createElementNS(SVG_NS, 'circle');
  arc.setAttribute('class', 'ag-vc-progress-arc');
  arc.setAttribute('cx', '50');
  arc.setAttribute('cy', '50');
  arc.setAttribute('r', String(PROGRESS_RADIUS));
  arc.setAttribute('stroke-dasharray', String(PROGRESS_CIRCUMFERENCE));
  arc.setAttribute('stroke-dashoffset', String(PROGRESS_CIRCUMFERENCE));

  svg.append(track, arc);
  return { svg, arc };
}

function readThemeVar(name: string, fallbackEl: HTMLElement): string {
  const val = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  if (val) return val;
  return getComputedStyle(fallbackEl).color;
}

/**
 * Full-screen blurred Video Circle recorder overlay (1:1 Android VideoCircleRecordOverlay.kt):
 * circular live <video> feed (or pulsing themed avatar canvas when camera is unavailable),
 * circular SVG accent progress ring, 0:00 timer badge, Flip/Lock pill, Cancel & Send ➤ controls.
 */
export function openVideoCircleRecorder(
  opts: VideoCircleRecorderOptions,
): VideoCircleRecorderHandle {
  let closed = false;
  let finishing = false;
  let canvasRafId: number | null = null;
  let currentLevel = 0;

  const s = inputT();

  const statusDot = el('span', { class: 'ag-voice-rec-dot', 'aria-hidden': 'true' });
  const statusText = el('span', null, s.videoCircleRecording);
  const statusPill = el('div', { class: 'ag-vc-status-pill' }, statusDot, statusText);
  const topBar = el('div', { class: 'ag-vc-top' }, statusPill);

  const videoEl = el('video', {
    class: 'ag-vc-video is-mirrored',
    autoplay: true,
    muted: true,
    playsinline: true,
    hidden: true,
  }) as HTMLVideoElement;
  videoEl.muted = true;
  videoEl.playsInline = true;

  const canvasEl = el('canvas', {
    class: 'ag-vc-canvas',
    width: 480,
    height: 480,
  }) as HTMLCanvasElement;

  const circleEl = el(
    'div',
    {
      class: 'ag-vc-circle',
      title: s.cameraPreview,
      onClick: (e: Event) => {
        const me = e as MouseEvent;
        const rect = circleEl.getBoundingClientRect();
        const x = me.clientX - rect.left;
        const y = me.clientY - rect.top;
        const ring = el('span', {
          class: 'ag-vc-focus-ring',
          style: `left:${x}px;top:${y}px;`,
        });
        circleEl.appendChild(ring);
        window.requestAnimationFrame(() => ring.classList.add('is-fading'));
        window.setTimeout(() => ring.remove(), 650);
      },
    },
    videoEl,
    canvasEl,
  );

  const { svg: progressSvg, arc: progressArc } = createProgressSvg();
  const stage = el(
    'div',
    { class: 'ag-vc-stage' },
    el('div', { class: 'ag-vc-halo-outer', 'aria-hidden': 'true' }),
    el('div', { class: 'ag-vc-halo-inner', 'aria-hidden': 'true' }),
    progressSvg,
    circleEl,
  );

  const timerDot = el('span', { class: 'ag-voice-rec-dot', 'aria-hidden': 'true' });
  const timerText = el('span', { class: 'ag-vc-timer-text' }, '0:00');
  const timerBadge = el('div', { class: 'ag-vc-timer-badge' }, timerDot, timerText);

  const center = el('div', { class: 'ag-vc-center' }, stage, timerBadge);

  const flipBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-vc-ctrl-btn',
      title: s.switchCamera,
      'aria-label': s.switchCamera,
      onClick: () => {
        if (finishing) return;
        void mediaRecorderController.flipCamera(videoEl).then((facing) => {
          videoEl.classList.toggle('is-mirrored', facing === 'user');
        });
      },
    },
    createSwitchCameraIcon(20),
  ) as HTMLButtonElement;

  const lockBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-vc-ctrl-btn',
      title: s.recordingLock,
      'aria-label': s.recordingLock,
      'aria-pressed': 'false',
      onClick: () => {
        if (finishing) return;
        mediaRecorderController.lock();
      },
    },
    tgIcons.unlock(19),
  ) as HTMLButtonElement;

  const pillGroup = el('div', { class: 'ag-vc-pill-group' }, flipBtn, lockBtn);

  const cancelBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-vc-cancel-btn',
      title: s.cancel,
      'aria-label': s.cancel,
      onClick: () => handleCancel(),
    },
    icons.close(18),
    el('span', null, s.cancel),
  ) as HTMLButtonElement;

  const sendBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-vc-send-btn',
      title: s.send,
      'aria-label': s.send,
      onClick: () => {
        void handleSend();
      },
    },
    icons.send(22),
  ) as HTMLButtonElement;

  const actions = el('div', { class: 'ag-vc-actions' }, cancelBtn, sendBtn);
  const controls = el('div', { class: 'ag-vc-controls' }, pillGroup, actions);

  const overlay = el(
    'div',
    {
      class: 'ag-vc-overlay',
      role: 'dialog',
      'aria-modal': 'true',
      'aria-label': s.videoCircleRecording,
      onClick: (e: Event) => e.stopPropagation(),
      onPointerDown: (e: Event) => e.stopPropagation(),
    },
    topBar,
    center,
    controls,
  );

  const stopCanvasLoop = () => {
    if (canvasRafId !== null) {
      window.cancelAnimationFrame(canvasRafId);
      canvasRafId = null;
    }
  };

  const startCanvasLoop = () => {
    if (canvasRafId !== null || closed) return;
    const ctx = canvasEl.getContext('2d');
    if (!ctx) return;

    const draw = (now: number) => {
      if (closed || !videoEl.hidden) {
        canvasRafId = null;
        return;
      }
      const w = canvasEl.width;
      const h = canvasEl.height;
      const cx = w / 2;
      const cy = h / 2;

      const bgColor = readThemeVar('--ag-surface', overlay);
      const cardColor = readThemeVar('--ag-card', overlay);
      const accentColor = readThemeVar('--ag-accent', overlay);
      const accentSoft = readThemeVar('--ag-accent-soft', overlay);
      const textPrimary = readThemeVar('--ag-text-primary', overlay);

      ctx.clearRect(0, 0, w, h);

      const grad = ctx.createRadialGradient(cx, cy * 0.85, 24, cx, cy, w * 0.55);
      grad.addColorStop(0, cardColor);
      grad.addColorStop(1, bgColor);
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, w, h);

      const t = now * 0.0024;
      const pulse = 0.5 + 0.5 * Math.sin(t * 1.4);
      const lvl = Math.min(1, Math.max(0, currentLevel));

      // Concentric pulsing rings driven by live/synthetic audio level
      for (let i = 0; i < 3; i++) {
        const phase = (t * 0.45 + i / 3) % 1;
        const r = 74 + phase * 112 + lvl * 28;
        ctx.save();
        ctx.globalAlpha = Math.max(0.04, (1 - phase) * (0.28 + lvl * 0.35));
        ctx.strokeStyle = accentColor;
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();
      }

      // Inner avatar medallion
      const badgeRadius = 64 + pulse * 4 + lvl * 12;
      ctx.save();
      ctx.fillStyle = accentSoft;
      ctx.beginPath();
      ctx.arc(cx, cy, badgeRadius + 14, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = accentColor;
      ctx.globalAlpha = 0.18 + lvl * 0.16;
      ctx.beginPath();
      ctx.arc(cx, cy, badgeRadius, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      // Crisp centered avatar silhouette
      ctx.save();
      ctx.strokeStyle = textPrimary;
      ctx.fillStyle = textPrimary;
      ctx.globalAlpha = 0.86;
      ctx.lineWidth = 4;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.arc(cx, cy - 18, 20, 0, Math.PI * 2);
      ctx.fill();

      ctx.beginPath();
      ctx.arc(cx, cy + 44, 38, Math.PI * 1.15, Math.PI * 1.85);
      ctx.stroke();
      ctx.restore();

      canvasRafId = window.requestAnimationFrame(draw);
    };

    canvasRafId = window.requestAnimationFrame(draw);
  };

  const syncStrings = () => {
    const cur = inputT();
    const isFallback = mediaRecorderController.isFallback;
    statusText.textContent = isFallback ? cur.cameraPermissionFallback : cur.videoCircleRecording;
    statusPill.classList.toggle('is-fallback', isFallback);
    circleEl.title = cur.cameraPreview;
    flipBtn.title = cur.switchCamera;
    flipBtn.setAttribute('aria-label', cur.switchCamera);
    const isLocked = mediaRecorderController.getState().locked;
    const lockTitle = isLocked ? cur.recordingLocked : cur.recordingLock;
    lockBtn.title = lockTitle;
    lockBtn.setAttribute('aria-label', lockTitle);
    cancelBtn.title = cur.cancel;
    cancelBtn.setAttribute('aria-label', cur.cancel);
    const cancelLabel = cancelBtn.querySelector('span');
    if (cancelLabel) cancelLabel.textContent = cur.cancel;
    sendBtn.title = cur.send;
    sendBtn.setAttribute('aria-label', cur.send);
    overlay.setAttribute('aria-label', cur.videoCircleRecording);
  };

  const syncState = (state: RecorderState) => {
    currentLevel = state.level;
    timerText.textContent = formatTimer(state.elapsedSec);

    const progress = Math.min(1, Math.max(0, state.elapsedSec / MAX_CIRCLE_DURATION_SEC));
    progressArc.style.strokeDashoffset = String(PROGRESS_CIRCUMFERENCE * (1 - progress));

    const hasStream = Boolean(state.stream && state.stream.getVideoTracks().length > 0);
    if (hasStream) {
      videoEl.hidden = false;
      canvasEl.hidden = true;
      videoEl.classList.toggle('is-mirrored', mediaRecorderController.facingMode === 'user');
      if (videoEl.paused) {
        void videoEl.play().catch(() => {});
      }
      stopCanvasLoop();
    } else {
      videoEl.hidden = true;
      canvasEl.hidden = false;
      startCanvasLoop();
    }

    lockBtn.classList.toggle('is-locked', state.locked);
    lockBtn.setAttribute('aria-pressed', String(state.locked));
    lockBtn.replaceChildren(state.locked ? icons.lock(19) : tgIcons.unlock(19));

    syncStrings();

    if (state.active && state.mode === 'video_circle' && state.elapsedSec >= MAX_CIRCLE_DURATION_SEC) {
      void handleSend();
    }
  };

  const cleanupDom = () => {
    if (closed) return;
    closed = true;
    stopCanvasLoop();
    unsubRec();
    unsubLang();
    document.removeEventListener('keydown', onKeyDown);
    overlay.classList.remove('is-open');
    window.setTimeout(() => overlay.remove(), 180);
  };

  const handleCancel = () => {
    if (finishing) return;
    finishing = true;
    mediaRecorderController.cancel();
    cleanupDom();
    opts.onCancel?.();
  };

  const handleSend = async () => {
    if (finishing) return;
    finishing = true;
    sendBtn.disabled = true;
    cancelBtn.disabled = true;
    const result = await mediaRecorderController.stopAndSave();
    cleanupDom();
    opts.onSend(result.dataUrl, result.durationSec);
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      handleCancel();
    }
  };

  const unsubRec = mediaRecorderController.subscribe(syncState);
  const unsubLang = i18nStore.subscribe(syncStrings);
  document.addEventListener('keydown', onKeyDown);
  document.body.appendChild(overlay);
  window.requestAnimationFrame(() => overlay.classList.add('is-open'));

  startCanvasLoop();
  syncStrings();
  void mediaRecorderController.startVideoCircle(videoEl);

  return {
    close: handleCancel,
  };
}
