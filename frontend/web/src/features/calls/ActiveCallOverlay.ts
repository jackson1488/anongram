import { clear, el } from '../../core/ui/dom';
import { icons, tgIcons } from '../../core/ui/icons';
import { callsStore, formatDuration } from './CallsStore';
import { callsT } from './i18n';

const SAS_EMOJIS = ['🛡️', '🔐', '⚡', '🕶️', '🔑', '💎', '🛰️', '🧅'];

function pickSasCode(peerName: string): string {
  let hash = 0;
  for (let i = 0; i < peerName.length; i++) {
    hash = (hash * 31 + peerName.charCodeAt(i)) >>> 0;
  }
  const out: string[] = [];
  for (let i = 0; i < 4; i++) {
    out.push(SAS_EMOJIS[(hash + i * 3) % SAS_EMOJIS.length]);
  }
  return out.join(' ');
}

export function openActiveCallModal(peerName: string, avatarEmoji: string, isVideo: boolean): void {
  let muted = false;
  let speaker = true;
  let cameraEnabled = isVideo;
  let elapsedSec = 0;
  let connected = false;

  const overlay = el('div', { class: 'ag-modal-overlay' });
  const sasCode = pickSasCode(peerName);

  const connectTimer = window.setTimeout(() => {
    connected = true;
    render();
  }, 1500);

  const tickTimer = window.setInterval(() => {
    if (connected) {
      elapsedSec += 1;
      const timerEl = overlay.querySelector('.ag-call-timer');
      if (timerEl) {
        timerEl.textContent = formatDuration(elapsedSec) || '00:00';
      }
    }
  }, 1000);

  const endCall = () => {
    window.clearTimeout(connectTimer);
    window.clearInterval(tickTimer);
    callsStore.addCall({
      peerName,
      avatarEmoji,
      direction: 'outgoing',
      isVideo,
      timestampMs: Date.now(),
      durationSec: elapsedSec,
    });
    overlay.remove();
  };

  const render = () => {
    clear(overlay);
    const s = callsT();

    const muteBtn = el(
      'button',
      {
        class: `ag-pill-btn ${muted ? 'ag-pill-btn--primary' : 'ag-pill-btn--secondary'}`,
        onClick: () => {
          muted = !muted;
          render();
        },
      },
      muted ? tgIcons.micOff(16) : icons.mic(16),
      muted ? s.unmute : s.mute,
    );

    const speakerBtn = el(
      'button',
      {
        class: `ag-pill-btn ${speaker ? 'ag-pill-btn--ghost' : 'ag-pill-btn--secondary'}`,
        onClick: () => {
          speaker = !speaker;
          render();
        },
      },
      speaker ? tgIcons.unmute(16) : tgIcons.mute(16),
      s.speaker,
    );

    const videoBtn = isVideo
      ? el(
          'button',
          {
            class: `ag-pill-btn ${cameraEnabled ? 'ag-pill-btn--ghost' : 'ag-pill-btn--secondary'}`,
            onClick: () => {
              cameraEnabled = !cameraEnabled;
              render();
            },
          },
          icons.video(16),
          cameraEnabled ? s.cameraOn : s.cameraOff,
        )
      : null;

    const endBtn = el(
      'button',
      {
        class: 'ag-call-end-btn',
        onClick: endCall,
      },
      icons.phone(16),
      s.endCall,
    );

    const actions = [muteBtn, speakerBtn];
    if (videoBtn) actions.push(videoBtn);
    actions.push(endBtn);

    overlay.appendChild(
      el(
        'div',
        { class: 'ag-card ag-call-overlay-card' },
        el('div', { class: 'ag-call-modal__badge' }, s.encryptedCall),
        el('div', { class: 'ag-call-modal__avatar' }, avatarEmoji),
        el('h2', { style: 'margin:0;font-size:22px;color:var(--ag-text-primary);' }, peerName),
        el(
          'p',
          { style: 'margin:0;color:var(--ag-text-secondary);font-size:14px;' },
          `${isVideo ? s.startVideoCall : s.startAudioCall} • ${connected ? s.connected : s.calling}`,
        ),
        connected
          ? el('div', { class: 'ag-call-timer' }, formatDuration(elapsedSec) || '00:00')
          : el('span', null),
        el('div', { class: 'ag-call-sas-pill' }, `${s.sasCodeLabel}: ${sasCode}`),
        el('div', { class: 'ag-call-modal__actions', style: 'flex-wrap:wrap;justify-content:center;' }, ...actions),
      ),
    );
  };

  render();
  document.body.appendChild(overlay);
}
