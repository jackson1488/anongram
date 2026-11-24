import type { PlayerState, PlayerTrack, RepeatMode } from '../api/Track';

export type PlayableTrack = PlayerTrack & { previewUrl?: string };

const TIMER_STEP_SEC = 0.25;
const TIMER_INTERVAL_MS = 250;

/**
 * Controls audio playback for chat voice/music attachments and demo tracks.
 * Uses a real HTMLAudioElement whenever a playable URL is available, and falls
 * back to a smooth interval timer for seeded demo tracks without a URL.
 */
export class AudioPlayerController {
  private queueList: PlayableTrack[] = [];
  private order: number[] = [];
  private cursor = -1;
  private isPlaying = false;
  private positionSec = 0;
  private durationSec = 0;
  private speed = 1;
  private shuffleOn = false;
  private repeatMode: RepeatMode = 'off';
  private timerId: number | undefined;
  private audioEl: HTMLAudioElement | null = null;
  private audioCleanup: (() => void) | null = null;
  private pendingSeekSec: number | null = null;
  private pendingSeekFraction: number | null = null;
  private listeners = new Set<() => void>();

  get currentTrack(): PlayableTrack | null {
    const i = this.order[this.cursor];
    return i === undefined ? null : (this.queueList[i] ?? null);
  }
  get playing(): boolean { return this.isPlaying; }
  get position(): number { return this.positionSec; }
  get duration(): number { return this.durationSec; }
  get playbackSpeed(): number { return this.speed; }
  get shuffle(): boolean { return this.shuffleOn; }
  get repeat(): RepeatMode { return this.repeatMode; }
  get queue(): PlayableTrack[] { return this.order.map((i) => this.queueList[i]); }
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
  toggleTrack(track: PlayableTrack, queue?: PlayableTrack[]): void {
    if (this.currentTrack?.id === track.id) {
      this.togglePlayPause();
      return;
    }
    const existingIdx = this.queueList.findIndex((t) => t.id === track.id);
    if (queue && queue.length > 0) {
      this.queueList = queue.some((t) => t.id === track.id) ? queue.slice() : [...queue, track];
    } else if (existingIdx >= 0) {
      const prev = this.queueList[existingIdx];
      const sameUrl = (track.url ?? track.previewUrl) === (prev.url ?? prev.previewUrl);
      this.queueList[existingIdx] = {
        ...prev,
        ...track,
        url: track.url ?? prev.url,
        previewUrl: track.previewUrl ?? prev.previewUrl,
        durationSec: sameUrl && prev.durationSec > 0 ? prev.durationSec : track.durationSec,
      };
    } else {
      this.queueList = [track];
    }
    const idx = this.queueList.findIndex((t) => t.id === track.id);
    this.rebuildOrder(idx);
    this.startCurrent();
  }

  /** Pause active playback cleanly without tearing down the loaded track. */
  pause(): void {
    if (!this.currentTrack || !this.isPlaying) return;
    this.isPlaying = false;
    if (this.audioEl) {
      this.audioEl.pause();
    } else {
      this.clearTimer();
    }
    this.emit();
  }

  togglePlayPause(): void {
    if (!this.currentTrack) return;
    if (this.isPlaying) {
      this.pause();
      return;
    }
    this.isPlaying = true;
    const mediaUrl = this.resolveTrackUrl(this.currentTrack);
    if (this.audioEl) {
      this.audioEl.playbackRate = this.speed;
      this.playAudioElement(this.audioEl);
    } else if (mediaUrl && typeof Audio !== 'undefined') {
      // Re-mount if a previous audio element failed and was torn down.
      this.pendingSeekSec = this.positionSec > 0 ? this.positionSec : null;
      this.mountAudioElement(mediaUrl);
    } else {
      this.startTimer();
    }
    this.emit();
  }

  /** Seek to an absolute timestamp in seconds within the active track. */
  seek(sec: number): void {
    const t = this.currentTrack;
    if (!t || !Number.isFinite(sec)) return;
    const dur = this.getEffectiveDuration();
    const target = dur > 0 ? Math.max(0, Math.min(dur, sec)) : Math.max(0, sec);
    this.positionSec = target;
    if (this.audioEl) {
      if (this.audioEl.readyState >= 1) {
        this.pendingSeekSec = null;
        this.pendingSeekFraction = null;
        this.setAudioCurrentTime(this.audioEl, target);
      } else {
        // Defer seeking until loadedmetadata resolves track duration and seekability.
        this.pendingSeekSec = target;
        this.pendingSeekFraction = null;
      }
    }
    this.emit();
  }

  /** Seek to a normalized [0..1] fraction of the active track duration. */
  seekToFraction(fraction: number): void {
    if (!this.currentTrack || !Number.isFinite(fraction)) return;
    const clamped = Math.max(0, Math.min(1, fraction));
    const dur = this.getEffectiveDuration();
    if (this.audioEl && this.audioEl.readyState < 1) {
      this.pendingSeekFraction = clamped;
      this.pendingSeekSec = null;
      this.positionSec = dur > 0 ? clamped * dur : 0;
      this.emit();
      return;
    }
    if (dur > 0) {
      this.seek(clamped * dur);
      return;
    }
    this.positionSec = 0;
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
    this.durationSec = 0;
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

  private resolveTrackUrl(track: PlayableTrack | null): string | null {
    if (!track) return null;
    const raw = track.url ?? track.previewUrl;
    if (typeof raw !== 'string') return null;
    const url = raw.trim();
    if (!url) return null;
    if (url.startsWith('data:') && !url.startsWith('data:audio/') && !url.startsWith('data:application/ogg')) {
      return null;
    }
    return url;
  }

  private getEffectiveDuration(): number {
    if (this.audioEl && Number.isFinite(this.audioEl.duration) && this.audioEl.duration > 0) {
      return this.audioEl.duration;
    }
    if (this.durationSec > 0) {
      return this.durationSec;
    }
    const trackDur = this.currentTrack?.durationSec ?? 0;
    return Number.isFinite(trackDur) && trackDur > 0 ? trackDur : 0;
  }

  private applyTrackDuration(nextDuration: number): void {
    if (!Number.isFinite(nextDuration) || nextDuration <= 0) return;
    this.durationSec = nextDuration;
    if (this.positionSec > nextDuration) {
      this.positionSec = nextDuration;
    }
    const idx = this.order[this.cursor];
    if (idx !== undefined && this.queueList[idx]) {
      const cur = this.queueList[idx];
      if (cur.durationSec !== nextDuration) {
        this.queueList[idx] = { ...cur, durationSec: nextDuration };
      }
    }
  }

  private setAudioCurrentTime(audio: HTMLAudioElement, targetSec: number): void {
    try {
      audio.currentTime = targetSec;
    } catch {
      // Ignore seek failures while media buffer is initializing.
    }
  }

  private playAudioElement(audio: HTMLAudioElement): void {
    try {
      const playPromise = audio.play();
      if (playPromise && typeof playPromise.catch === 'function') {
        playPromise.catch((err: unknown) => {
          if (this.audioEl !== audio) return;
          const name = (err as { name?: string } | null)?.name;
          if (name === 'AbortError') return;
          if (name === 'NotSupportedError' || audio.error) {
            this.teardownAudio();
          }
          this.isPlaying = false;
          this.emit();
        });
      }
    } catch {
      if (this.audioEl === audio) {
        this.teardownAudio();
        this.isPlaying = false;
      }
    }
  }

  private mountAudioElement(url: string): void {
    const audio = new Audio();
    audio.preload = 'auto';
    audio.src = url;
    audio.playbackRate = this.speed;

    const isCurrentAudio = () => this.audioEl === audio;

    const syncDuration = () => {
      if (!isCurrentAudio()) return;
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        this.applyTrackDuration(audio.duration);
      }
    };

    const applyPendingSeek = () => {
      if (!isCurrentAudio()) return;
      const dur = this.getEffectiveDuration();
      let target: number | null = null;
      if (this.pendingSeekFraction !== null) {
        target = dur > 0 ? this.pendingSeekFraction * dur : 0;
        this.pendingSeekFraction = null;
      } else if (this.pendingSeekSec !== null) {
        target = dur > 0 ? Math.min(dur, this.pendingSeekSec) : this.pendingSeekSec;
        this.pendingSeekSec = null;
      }
      if (target !== null && Number.isFinite(target) && target >= 0) {
        this.positionSec = target;
        this.setAudioCurrentTime(audio, target);
      }
    };

    const onLoadedMetadata = () => {
      if (!isCurrentAudio()) return;
      // Re-apply playbackRate after metadata loads so WebKit/Safari preserves speed.
      audio.playbackRate = this.speed;
      syncDuration();
      applyPendingSeek();
      this.emit();
    };

    const onDurationChange = () => {
      if (!isCurrentAudio()) return;
      const prev = this.durationSec;
      syncDuration();
      if (this.durationSec !== prev) {
        this.emit();
      }
    };

    const onTimeUpdate = () => {
      if (!isCurrentAudio()) return;
      const current = Number.isFinite(audio.currentTime) && audio.currentTime >= 0 ? audio.currentTime : 0;
      // MediaRecorder streams (e.g. WebM/Opus) may report Infinity duration until complete.
      if (!Number.isFinite(audio.duration) && current > this.durationSec) {
        this.applyTrackDuration(current);
      }
      const dur = this.getEffectiveDuration();
      this.positionSec = dur > 0 ? Math.min(dur, current) : current;
      this.emit();
    };

    const onEnded = () => {
      if (!isCurrentAudio()) return;
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        this.applyTrackDuration(audio.duration);
      } else if (Number.isFinite(audio.currentTime) && audio.currentTime > 0 && this.durationSec <= 0) {
        this.applyTrackDuration(audio.currentTime);
      }
      this.positionSec = this.getEffectiveDuration();
      this.onTrackEnded();
    };

    const onError = () => {
      if (!isCurrentAudio()) return;
      this.teardownAudio();
      this.clearTimer();
      this.isPlaying = false;
      this.positionSec = 0;
      this.emit();
    };

    const onPlay = () => {
      if (!isCurrentAudio()) return;
      audio.playbackRate = this.speed;
      if (!this.isPlaying) {
        this.isPlaying = true;
        this.emit();
      }
    };

    const onPause = () => {
      if (!isCurrentAudio() || audio.ended) return;
      if (this.isPlaying) {
        this.isPlaying = false;
        this.emit();
      }
    };

    audio.addEventListener('loadedmetadata', onLoadedMetadata);
    audio.addEventListener('durationchange', onDurationChange);
    audio.addEventListener('timeupdate', onTimeUpdate);
    audio.addEventListener('ended', onEnded);
    audio.addEventListener('error', onError);
    audio.addEventListener('play', onPlay);
    audio.addEventListener('pause', onPause);

    this.audioEl = audio;
    this.audioCleanup = () => {
      audio.removeEventListener('loadedmetadata', onLoadedMetadata);
      audio.removeEventListener('durationchange', onDurationChange);
      audio.removeEventListener('timeupdate', onTimeUpdate);
      audio.removeEventListener('ended', onEnded);
      audio.removeEventListener('error', onError);
      audio.removeEventListener('play', onPlay);
      audio.removeEventListener('pause', onPause);
      try {
        audio.pause();
      } catch {
        // Ignore pause errors during teardown.
      }
      audio.removeAttribute('src');
      try {
        audio.load();
      } catch {
        // Ignore reset errors in restricted environments.
      }
    };

    this.playAudioElement(audio);
  }

  private startCurrent(): void {
    this.stopInternal();
    const track = this.currentTrack;
    if (!track) {
      this.durationSec = 0;
      this.positionSec = 0;
      this.emit();
      return;
    }
    this.positionSec = 0;
    this.durationSec = Number.isFinite(track.durationSec) && track.durationSec > 0 ? track.durationSec : 0;
    this.isPlaying = true;

    const mediaUrl = this.resolveTrackUrl(track);
    if (mediaUrl && typeof Audio !== 'undefined') {
      this.mountAudioElement(mediaUrl);
    } else {
      this.startTimer();
    }
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
      if (!t || !this.isPlaying || this.audioEl) return;
      const dur = this.getEffectiveDuration();
      this.positionSec += TIMER_STEP_SEC * this.speed;
      if (this.positionSec >= dur) {
        this.positionSec = Math.max(0, dur);
        this.onTrackEnded();
        return;
      }
      this.emit();
    }, TIMER_INTERVAL_MS);
  }

  private clearTimer(): void {
    if (this.timerId !== undefined) {
      window.clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }

  private teardownAudio(): void {
    this.pendingSeekSec = null;
    this.pendingSeekFraction = null;
    this.audioEl = null;
    if (this.audioCleanup) {
      const cleanup = this.audioCleanup;
      this.audioCleanup = null;
      cleanup();
    }
  }

  private stopInternal(): void {
    this.clearTimer();
    this.isPlaying = false;
    this.teardownAudio();
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const audioPlayer = new AudioPlayerController();
