import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { PlayerI18n } from '../i18n/PlayerI18n';
import { audioPlayer } from '../logic/AudioPlayerController';
import { fmtTime } from './format';
import { openMusicFullScreenPlayer } from './MusicFullScreenPlayer';
import './player.css';

export function createMiniAudioPlayerBar(): HTMLElement {
  const bar = el('div', { class: 'ag-mini-player', style: 'display:none' });

  const render = () => {
    clear(bar);
    const track = audioPlayer.currentTrack;
    if (!track) {
      bar.style.display = 'none';
      return;
    }
    const s = PlayerI18n.current;
    bar.style.display = 'flex';
    const pct = track.durationSec > 0 ? Math.min(100, (audioPlayer.position / track.durationSec) * 100) : 0;
    bar.appendChild(
      el(
        'button',
        {
          class: 'ag-icon-btn',
          title: audioPlayer.playing ? s.pause : s.play,
          onClick: () => audioPlayer.togglePlayPause(),
        },
        audioPlayer.playing ? icons.pause(18) : icons.play(18),
      ),
    );
    const info = el(
      'div',
      {
        class: 'ag-mini-player__info',
        style: 'cursor:pointer',
        title: s.nowPlaying,
        onClick: () => openMusicFullScreenPlayer(),
      },
      el('div', { class: 'ag-mini-player__title' }, track.title),
      el('div', { class: 'ag-mini-player__sub' }, `${fmtTime(audioPlayer.position)} / ${fmtTime(track.durationSec)} • ${track.subtitle}`),
      el('div', { class: 'ag-mini-player__progress' }, el('div', { style: `width:${pct}%` })),
    );
    bar.appendChild(info);
    bar.appendChild(el('button', { class: 'ag-chip', title: s.speed, onClick: () => audioPlayer.cycleSpeed() }, `${audioPlayer.playbackSpeed}x`));
    bar.appendChild(el('button', { class: 'ag-icon-btn', title: s.close, onClick: () => audioPlayer.close() }, icons.close(18)));
  };

  const u1 = audioPlayer.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (bar as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };
  return bar;
}
