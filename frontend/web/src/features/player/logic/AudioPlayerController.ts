import type { PlayerState, PlayerTrack, RepeatMode } from '../api/Track';

export class AudioPlayerController {
  private queueList: PlayerTrack[] = [];
  private order: number[] = [];
  private cursor = -1;
  private isPlaying = false;
  private positionSec = 0;
  private speed = 1;
  private shuffleOn = false;
  private repeatMode: RepeatMode = 'off';
  private timerId: number | undefined;
  private audioEl: HTMLAudioElement | null = null;
  private listeners = new Set<() => void>();

  get currentTrack(): PlayerTrack | null {
    const i = this.order[this.cursor];
    return i === undefined ? null : (this.queueList[i] ?? null);
  }
  get playing(): boolean { return this.isPlaying; }
  get position(): number { return this.positionSec; }
  get playbackSpeed(): number { return this.speed; }
  get shuffle(): boolean { return this.shuffleOn; }
  get repeat(): RepeatMode { return this.repeatMode; }
  get queue(): PlayerTrack[] { return this.order.map((i) => this.queueList[i]); }
  get queueIndex(): number { return this.cursor; }

  get state(): PlayerState {
    return {
      track: this.currentTrack,
      queue: this.queue,
      index: this.cursor,
      playing: this.isPlaying,
      position: this.positionSec,
      speed: this.speed,
      shuffle: this.shuffleOn,
      repeat: this.repeatMode,
    };
  }

  /** Start a track; optionally set a queue (e.g. all music of the chat). */
  toggleTrack(track: PlayerTrack, queue?: PlayerTrack[]): void {
    if (this.currentTrack?.id === track.id) {
      this.togglePlayPause();
      return;
    }
    if (queue && queue.length > 0) {
      this.queueList = queue.some((t) => t.id === track.id) ? queue.slice() : [...queue, track];
    } else if (!this.queueList.some((t) => t.id === track.id)) {
      this.queueList = [track];
    }
    const idx = this.queueList.findIndex((t) => t.id === track.id);
    this.rebuildOrder(idx);
    this.startCurrent();
  }

  togglePlayPause(): void {
    if (!this.currentTrack) return;
    this.isPlaying = !this.isPlaying;
    if (this.audioEl) {
      if (this.isPlaying) this.audioEl.play().catch(() => {});
      else this.audioEl.pause();
    }
    if (this.isPlaying) this.startTimer();
    else this.clearTimer();
    this.emit();
  }

  seek(sec: number): void {
    const t = this.currentTrack;
    if (!t) return;
    this.positionSec = Math.max(0, Math.min(t.durationSec, sec));
    if (this.audioEl) this.audioEl.currentTime = this.positionSec;
    this.emit();
  }

  next(): void {
    if (this.queueList.length === 0) return;
    if (this.cursor + 1 < this.order.length) {
      this.cursor += 1;
    } else if (this.repeatMode === 'all') {
      this.cursor = 0;
    } else {
      this.close();
      return;
    }
    this.startCurrent();
  }

  previous(): void {
    if (this.positionSec > 3 || this.cursor <= 0) {
      this.seek(0);
      return;
    }
    this.cursor -= 1;
    this.startCurrent();
  }

  toggleShuffle(): void {
    this.shuffleOn = !this.shuffleOn;
    const curIdx = this.order[this.cursor] ?? -1;
    this.rebuildOrder(curIdx);
    this.emit();
  }

  cycleRepeat(): void {
    this.repeatMode = this.repeatMode === 'off' ? 'all' : this.repeatMode === 'all' ? 'one' : 'off';
    this.emit();
  }

  cycleSpeed(): void {
    this.speed = this.speed === 1 ? 1.5 : this.speed === 1.5 ? 2 : 1;
    if (this.audioEl) this.audioEl.playbackRate = this.speed;
    this.emit();
  }

  close(): void {
    this.stopInternal();
    this.queueList = [];
    this.order = [];
    this.cursor = -1;
    this.positionSec = 0;
    this.emit();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private rebuildOrder(currentQueueIdx: number): void {
    const idxs = this.queueList.map((_, i) => i);
    if (this.shuffleOn) {
      const rest = idxs.filter((i) => i !== currentQueueIdx);
      for (let i = rest.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [rest[i], rest[j]] = [rest[j], rest[i]];
      }
      this.order = currentQueueIdx >= 0 ? [currentQueueIdx, ...rest] : rest;
      this.cursor = currentQueueIdx >= 0 ? 0 : -1;
    } else {
      this.order = idxs;
      this.cursor = currentQueueIdx;
    }
  }

  private startCurrent(): void {
    this.stopInternal();
    const track = this.currentTrack;
    if (!track) {
      this.emit();
      return;
    }
    this.positionSec = 0;
    this.isPlaying = true;
    if (track.url) {
      const a = new Audio(track.url);
      a.playbackRate = this.speed;
      a.onended = () => this.onTrackEnded();
      a.play().catch(() => {});
      this.audioEl = a;
    }
    this.startTimer();
    this.emit();
  }

  private onTrackEnded(): void {
    if (this.repeatMode === 'one') {
      this.startCurrent();
    } else {
      this.next();
    }
  }

  private startTimer(): void {
    this.clearTimer();
    this.timerId = window.setInterval(() => {
      const t = this.currentTrack;
      if (!t || !this.isPlaying) return;
      if (this.audioEl && this.audioEl.currentTime > 0) {
        this.positionSec = this.audioEl.currentTime;
      } else {
        this.positionSec += 0.25 * this.speed;
      }
      if (!this.audioEl && this.positionSec >= t.durationSec) {
        this.onTrackEnded();
        return;
      }
      this.positionSec = Math.min(t.durationSec, this.positionSec);
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
      this.audioEl.onended = null;
      this.audioEl.pause();
      this.audioEl = null;
    }
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const audioPlayer = new AudioPlayerController();
