import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { inputT } from '../i18n/InputI18n';
import '../inputRecording.css';
import { mediaRecorderController, type RecorderState } from './MediaRecorderController';

export interface VoiceRecordingBarOptions {
  onCancel: () => void;
  onSend: () => void;
}

const EQ_BAR_COUNT = 16;
const EQ_ENVELOPE = [
  0.34, 0.48, 0.65, 0.78, 0.9, 0.96, 0.84, 1.0,
  0.94, 0.82, 0.95, 0.86, 0.72, 0.58, 0.44, 0.32,
];

function formatTimer(totalSec: number): string {
  const safe = Math.max(0, Math.floor(totalSec));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${mins}:${String(secs).padStart(2, '0')}`;
}

/**
 * Reactive voice recording bar matching Android VoiceRecordingBar.kt:
 * pulsing --ag-danger dot, monospace 0:00 timer, live level equalizer,
 * CANCEL / ОТМЕНА action, and round accent Send button with reactive halo.
 */
export function createVoiceRecordingBar(opts: VoiceRecordingBarOptions): HTMLElement {
  const s = inputT();

  const noticeText = el('span', null, s.micPermissionFallback);
  const noticePill = el(
    'div',
    {
      class: 'ag-voice-rec-notice',
      role: 'status',
      'aria-live': 'polite',
      hidden: !mediaRecorderController.isFallback,
    },
    icons.alert(13),
    noticeText,
  );

  const dot = el('span', { class: 'ag-voice-rec-dot', 'aria-hidden': 'true' });
  const timerEl = el('span', { class: 'ag-voice-rec-timer' }, '0:00');

  const eqBars: HTMLElement[] = [];
  const eqWrap = el('div', { class: 'ag-voice-rec-eq', 'aria-hidden': 'true' });
  for (let i = 0; i < EQ_BAR_COUNT; i++) {
    const bar = el('span', { class: 'ag-voice-rec-eq-bar' });
    eqBars.push(bar);
    eqWrap.appendChild(bar);
  }

  const cancelBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-voice-rec-cancel',
      title: s.cancel,
      'aria-label': s.cancel,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: (e: Event) => {
        e.stopPropagation();
        opts.onCancel();
      },
    },
    s.cancelUpper,
  ) as HTMLButtonElement;

  const sendHalo = el('span', { class: 'ag-voice-rec-send-halo', 'aria-hidden': 'true' });
  const sendBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-voice-rec-send',
      title: s.send,
      'aria-label': s.send,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: (e: Event) => {
        e.stopPropagation();
        opts.onSend();
      },
    },
    icons.send(20),
  ) as HTMLButtonElement;

  const sendWrap = el('div', { class: 'ag-voice-rec-send-wrap' }, sendHalo, sendBtn);
  const bar = el(
    'div',
    {
      class: 'ag-voice-rec-bar',
      role: 'region',
      'aria-label': s.voiceRecording,
    },
    dot,
    timerEl,
    eqWrap,
    cancelBtn,
    sendWrap,
  );

  const wrap = el('div', { class: 'ag-voice-rec-wrap' }, noticePill, bar);

  let phase = 0;

  const syncStrings = () => {
    const cur = inputT();
    noticeText.textContent = cur.micPermissionFallback;
    cancelBtn.textContent = cur.cancelUpper;
    cancelBtn.title = cur.cancel;
    cancelBtn.setAttribute('aria-label', cur.cancel);
    sendBtn.title = cur.send;
    sendBtn.setAttribute('aria-label', cur.send);
    bar.setAttribute('aria-label', cur.voiceRecording);
  };

  const syncState = (state: RecorderState) => {
    timerEl.textContent = formatTimer(state.elapsedSec);
    noticePill.hidden = !mediaRecorderController.isFallback;

    const lvl = Math.min(1, Math.max(0, state.level));
    phase += 0.45 + lvl * 0.6;

    for (let i = 0; i < eqBars.length; i++) {
      const env = EQ_ENVELOPE[i] ?? 0.6;
      const wave = 0.55 + 0.45 * Math.sin(phase + i * 0.72);
      const scaleY = Math.min(1, Math.max(0.16, 0.16 + lvl * env * wave * 0.92));
      const barEl = eqBars[i];
      barEl.style.transform = `scaleY(${scaleY.toFixed(3)})`;
      barEl.style.opacity = String(Number((0.48 + scaleY * 0.52).toFixed(2)));
    }

    const haloScale = 1 + lvl * 0.22;
    sendHalo.style.transform = `scale(${haloScale.toFixed(3)})`;
  };

  const unsubRec = mediaRecorderController.subscribe(syncState);
  const unsubLang = i18nStore.subscribe(syncStrings);
  syncStrings();
  syncState(mediaRecorderController.getState());

  (wrap as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubRec();
    unsubLang();
  };

  return wrap;
}
