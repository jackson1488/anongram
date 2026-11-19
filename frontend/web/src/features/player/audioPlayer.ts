import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';

export interface PlayerTrack {
  id: string;
  title: string;
  subtitle: string;
  durationSec: number;
  url?: string;
}

class AudioPlayerStore {
  private track: PlayerTrack | null = null;
  private isPlaying = false;
  private positionSec = 0;
  private speed = 1;
  private timerId: number | undefined;
  private audioEl: HTMLAudioElement | null = null;
  private listeners = new Set<() => void>();

  get currentTrack(): PlayerTrack | null {
    return this.track;
  }

  get playing(): boolean {
    return this.isPlaying;
  }

  get position(): number {
    return this.positionSec;
  }

  get playbackSpeed(): number {
    return this.speed;
  }

  toggleTrack(track: PlayerTrack): void {
    if (this.track?.id === track.id) {
      this.togglePlayPause();
      return;
    }
    this.stopInternal();
    this.track = track;
    this.positionSec = 0;
    this.isPlaying = true;
    if (track.url) {
      this.audioEl = new Audio(track.url);
      this.audioEl.playbackRate = this.speed;
      this.audioEl.play().catch(() => {});
      this.audioEl.onended = () => this.close();
    }
    this.startTimer();
    this.emit();
  }

  togglePlayPause(): void {
    if (!this.track) return;
    this.isPlaying = !this.isPlaying;
    if (this.audioEl) {
      if (this.isPlaying) {
        this.audioEl.play().catch(() => {});
      } else {
        this.audioEl.pause();
      }
    }
    if (this.isPlaying) {
      this.startTimer();
    } else {
      this.clearTimer();
    }
    this.emit();
  }

  cycleSpeed(): void {
    this.speed = this.speed === 1 ? 1.5 : this.speed === 1.5 ? 2 : 1;
    if (this.audioEl) this.audioEl.playbackRate = this.speed;
    this.emit();
  }

  close(): void {
    this.stopInternal();
    this.track = null;
    this.positionSec = 0;
    this.emit();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private startTimer(): void {
    this.clearTimer();
    this.timerId = window.setInterval(() => {
      if (!this.track || !this.isPlaying) return;
      this.positionSec = Math.min(this.track.durationSec, this.positionSec + 0.25 * this.speed);
      if (this.positionSec >= this.track.durationSec) {
        this.close();
        return;
      }
      this.emit();
    }, 250);
  }

  private clearTimer(): void {
    if (this.timerId !== undefined) {
      window.clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }

  private stopInternal(): void {
    this.clearTimer();
    this.isPlaying = false;
    if (this.audioEl) {
      this.audioEl.pause();
      this.audioEl = null;
    }
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const audioPlayer = new AudioPlayerStore();

export function createMiniAudioPlayerBar(): HTMLElement {
  const bar = el('div', { class: 'ag-mini-player', style: 'display:none' });

  const render = () => {
    clear(bar);
    const track = audioPlayer.currentTrack;
    if (!track) {
      bar.style.display = 'none';
      return;
    }
    bar.style.display = 'flex';
    const pct = track.durationSec > 0 ? Math.min(100, (audioPlayer.position / track.durationSec) * 100) : 0;
    const fmt = (s: number) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;

    bar.appendChild(
      el(
        'button',
        { class: 'ag-icon-btn', onClick: () => audioPlayer.togglePlayPause() },
        audioPlayer.playing ? icons.pause(18) : icons.play(18),
      ),
    );
    bar.appendChild(
      el(
        'div',
        { class: 'ag-mini-player__info' },
        el('div', { class: 'ag-mini-player__title' }, track.title),
        el('div', { class: 'ag-mini-player__sub' }, `${fmt(audioPlayer.position)} / ${fmt(track.durationSec)} • ${track.subtitle}`),
        el('div', { class: 'ag-mini-player__progress' }, el('div', { style: `width:${pct}%` })),
      ),
    );
    bar.appendChild(
      el(
        'button',
        { class: 'ag-chip', onClick: () => audioPlayer.cycleSpeed() },
        `${audioPlayer.playbackSpeed}x`,
      ),
    );
    bar.appendChild(
      el(
        'button',
        { class: 'ag-icon-btn', onClick: () => audioPlayer.close() },
        icons.close(18),
      ),
    );
  };

  audioPlayer.subscribe(render);
  render();
  return bar;
}
