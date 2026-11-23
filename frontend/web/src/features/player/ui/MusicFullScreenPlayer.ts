import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { PlayerI18n } from '../i18n/PlayerI18n';
import { audioPlayer } from '../logic/AudioPlayerController';
import { fmtTime } from './format';
import './player.css';

let openOverlay: HTMLElement | null = null;

export function openMusicFullScreenPlayer(): void {
  if (openOverlay || !audioPlayer.currentTrack) return;
  const body = el('div', { class: 'ag-pl-sheet' });
  const overlay = el('div', { class: 'ag-pl-overlay ag-pl-full' }, body);
  openOverlay = overlay;

  const close = () => {
    unsub();
    unsubI18n();
    document.removeEventListener('keydown', onKey);
    overlay.remove();
    openOverlay = null;
  };
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
    else if (e.key === 'ArrowRight') audioPlayer.next();
    else if (e.key === 'ArrowLeft') audioPlayer.previous();
    else if (e.key === ' ' && !(e.target instanceof HTMLInputElement)) {
      e.preventDefault();
      audioPlayer.togglePlayPause();
    }
  };

  const render = () => {
    const t = audioPlayer.currentTrack;
    if (!t) {
      close();
      return;
    }
    const s = PlayerI18n.current;
    clear(body);
    body.appendChild(
      el('div', { class: 'ag-pl-top' }, el('span', null, s.nowPlaying), el('button', { class: 'ag-icon-btn', title: s.close, onClick: close }, icons.close(20))),
    );
    const cover = el('div', { class: 'ag-pl-cover' }, t.coverUrl ? null : icons.music(72));
    if (t.coverUrl) cover.style.backgroundImage = `url("${t.coverUrl}")`;
    body.appendChild(cover);
    body.appendChild(el('div', { class: 'ag-pl-title' }, t.title));
    body.appendChild(el('div', { class: 'ag-pl-sub' }, t.subtitle));
    const seek = el('input', { type: 'range', class: 'ag-pl-seek', min: 0, max: Math.max(1, t.durationSec), step: 1, value: Math.floor(audioPlayer.position) });
    seek.addEventListener('input', () => audioPlayer.seek(Number(seek.value)));
    body.appendChild(seek);
    body.appendChild(el('div', { class: 'ag-pl-times' }, el('span', null, fmtTime(audioPlayer.position)), el('span', null, fmtTime(t.durationSec))));
    body.appendChild(
      el(
        'div',
        { class: 'ag-pl-controls' },
        el('button', { class: `ag-pl-btn${audioPlayer.shuffle ? ' is-active' : ''}`, title: s.shuffle, onClick: () => audioPlayer.toggleShuffle() }, '⇄'),
        el('button', { class: 'ag-pl-btn', title: s.prev, onClick: () => audioPlayer.previous() }, '⏮'),
        el('button', { class: 'ag-pl-play', title: audioPlayer.playing ? s.pause : s.play, onClick: () => audioPlayer.togglePlayPause() }, audioPlayer.playing ? icons.pause(28) : icons.play(28)),
        el('button', { class: 'ag-pl-btn', title: s.next, onClick: () => audioPlayer.next() }, '⏭'),
        el('button', { class: `ag-pl-btn${audioPlayer.repeat !== 'off' ? ' is-active' : ''}`, title: s.repeat, onClick: () => audioPlayer.cycleRepeat() }, audioPlayer.repeat === 'one' ? '🔂' : '🔁'),
      ),
    );
    body.appendChild(el('button', { class: 'ag-pl-btn', title: s.speed, onClick: () => audioPlayer.cycleSpeed() }, `${audioPlayer.playbackSpeed}x`));
  };

  const unsub = audioPlayer.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  document.addEventListener('keydown', onKey);
  render();
  document.body.appendChild(overlay);
}
