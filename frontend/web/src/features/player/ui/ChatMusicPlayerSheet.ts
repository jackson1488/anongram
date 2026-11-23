import { i18nStore } from '../../../core/i18n/i18nStore';
import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { PlayerI18n } from '../i18n/PlayerI18n';
import type { PlayerTrack } from '../api/Track';
import { audioPlayer } from '../logic/AudioPlayerController';
import { fmtTime } from './format';
import './player.css';

/** Queue sheet listing all music of the current chat. */
export function openChatMusicPlayerSheet(tracks: PlayerTrack[]): void {
  const s0 = PlayerI18n.current;
  const titleSpan = el('span', null, `${s0.musicInChat} • ${s0.queue}`);
  const closeBtn = el('button', { class: 'ag-icon-btn', title: s0.close, onClick: () => close() }, icons.close(20));
  const list = el('div', { class: 'ag-pl-list' });
  const overlay = el(
    'div',
    { class: 'ag-pl-overlay', onClick: (e: Event) => { if (e.target === overlay) close(); } },
    el(
      'div',
      { class: 'ag-pl-sheet' },
      el('div', { class: 'ag-pl-top' }, titleSpan, closeBtn),
      list,
    ),
  );
  const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close(); };
  const render = () => {
    const s = PlayerI18n.current;
    titleSpan.textContent = `${s.musicInChat} • ${s.queue}`;
    closeBtn.title = s.close;
    list.replaceChildren();
    if (tracks.length === 0) list.appendChild(el('div', { class: 'ag-pl-sub' }, s.emptyQueue));
    for (const t of tracks) {
      const cur = audioPlayer.currentTrack?.id === t.id;
      list.appendChild(
        el('button', { class: `ag-pl-row${cur ? ' is-current' : ''}`, onClick: () => audioPlayer.toggleTrack(t, tracks) },
          el('span', null, `${cur && audioPlayer.playing ? '▶ ' : ''}${t.title}`),
          el('small', null, fmtTime(t.durationSec))),
      );
    }
  };
  const unsub = audioPlayer.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  function close() { unsub(); unsubI18n(); document.removeEventListener('keydown', onKey); overlay.remove(); }
  document.addEventListener('keydown', onKey);
  render();
  document.body.appendChild(overlay);
}
