export type RecordingMode = 'none' | 'voice' | 'video_circle';

export interface RecorderState {
  mode: RecordingMode;
  active: boolean;
  locked: boolean;
  elapsedSec: number;
  level: number;
  stream: MediaStream | null;
}

export interface RecordedMediaResult {
  dataUrl: string;
  durationSec: number;
  mimeType: string;
}

export type RecorderListener = (state: RecorderState) => void;

const INITIAL_STATE: RecorderState = {
  mode: 'none',
  active: false,
  locked: false,
  elapsedSec: 0,
  level: 0,
  stream: null,
};

function pickAudioMimeType(): string {
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return '';
  }
  const candidates = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/ogg;codecs=opus',
    'audio/mp4',
  ];
  return candidates.find((type) => MediaRecorder.isTypeSupported(type)) ?? '';
}

function pickVideoMimeType(): string {
  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return '';
  }
  const candidates = [
    'video/webm;codecs=vp9,opus',
    'video/webm;codecs=vp8,opus',
    'video/webm',
    'video/mp4',
  ];
  return candidates.find((type) => MediaRecorder.isTypeSupported(type)) ?? '';
}

function isPermissionError(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const name = (err as { name?: string }).name ?? '';
  return name === 'NotAllowedError' || name === 'PermissionDeniedError' || name === 'SecurityError';
}

function stopMediaStream(stream: MediaStream | null): void {
  if (!stream) return;
  for (const track of stream.getTracks()) {
    try {
      track.stop();
    } catch {
      // Ignore already-stopped track errors
    }
  }
}

function blobToDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve) => {
    if (!blob || blob.size === 0) {
      resolve('');
      return;
    }
    const reader = new FileReader();
    reader.onloadend = () => {
      resolve(typeof reader.result === 'string' ? reader.result : '');
    };
    reader.onerror = () => resolve('');
    reader.readAsDataURL(blob);
  });
}

function bindStreamToVideo(videoEl: HTMLVideoElement | null, stream: MediaStream | null): void {
  if (!videoEl) return;
  videoEl.muted = true;
  videoEl.playsInline = true;
  videoEl.autoplay = true;
  videoEl.srcObject = stream;
  if (stream) {
    void videoEl.play().catch(() => {});
  }
}

export class MediaRecorderController {
  private current: RecorderState = { ...INITIAL_STATE };
  private listeners = new Set<RecorderListener>();

  private sessionId = 0;
  private startTimeMs = 0;
  private fallbackMode = false;
  private currentFacing: 'user' | 'environment' = 'user';

  private recorder: MediaRecorder | null = null;
  private chunks: Blob[] = [];
  private activeMimeType = '';

  private audioCtx: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private analyserData: Uint8Array<ArrayBuffer> | null = null;

  private boundVideoEl: HTMLVideoElement | null = null;
  private timerIntervalId: number | null = null;
  private levelRafId: number | null = null;
  private lastLevelEmitMs = 0;

  get state(): RecorderState {
    return { ...this.current };
  }

  getState(): RecorderState {
    return { ...this.current };
  }

  get isFallback(): boolean {
    return this.fallbackMode;
  }

  get facingMode(): 'user' | 'environment' {
    return this.currentFacing;
  }

  subscribe(cb: RecorderListener): () => void {
    this.listeners.add(cb);
    return () => {
      this.listeners.delete(cb);
    };
  }

  attachVideoElement(videoEl: HTMLVideoElement | null): void {
    this.boundVideoEl = videoEl;
    if (videoEl && this.current.stream) {
      bindStreamToVideo(videoEl, this.current.stream);
    }
  }

  async startVoice(): Promise<void> {
    this.cleanupSession(false);
    const sid = ++this.sessionId;
    this.fallbackMode = false;
    this.startTimeMs = Date.now();
    this.chunks = [];
    this.activeMimeType = '';

    this.current = {
      mode: 'voice',
      active: true,
      locked: false,
      elapsedSec: 0,
      level: 0,
      stream: null,
    };
    this.emit();
    this.startElapsedTimer(sid);

    if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      this.enterFallback(sid);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      if (sid !== this.sessionId || !this.current.active) {
        stopMediaStream(stream);
        return;
      }

      this.setupAudioAnalyser(stream, sid);
      this.setupMediaRecorder(stream, pickAudioMimeType());
      this.current = { ...this.current, stream };
      this.emit();
    } catch {
      if (sid !== this.sessionId || !this.current.active) return;
      this.enterFallback(sid);
    }
  }

  async startVideoCircle(videoEl?: HTMLVideoElement): Promise<void> {
    this.cleanupSession(false);
    const sid = ++this.sessionId;
    this.fallbackMode = false;
    this.currentFacing = 'user';
    this.startTimeMs = Date.now();
    this.boundVideoEl = videoEl ?? null;
    this.chunks = [];
    this.activeMimeType = '';

    this.current = {
      mode: 'video_circle',
      active: true,
      locked: false,
      elapsedSec: 0,
      level: 0,
      stream: null,
    };
    this.emit();
    this.startElapsedTimer(sid);

    if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      this.enterFallback(sid);
      return;
    }

    let stream: MediaStream | null = null;
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: 480, height: 480 },
        audio: true,
      });
    } catch (err) {
      if (!isPermissionError(err)) {
        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'user', width: 480, height: 480 },
          });
        } catch {
          stream = null;
        }
      }
    }

    if (sid !== this.sessionId || !this.current.active) {
      stopMediaStream(stream);
      return;
    }

    if (!stream) {
      this.enterFallback(sid);
      return;
    }

    bindStreamToVideo(this.boundVideoEl, stream);
    if (stream.getAudioTracks().length > 0) {
      this.setupAudioAnalyser(stream, sid);
    } else {
      this.startSyntheticLevelLoop(sid);
    }
    this.setupMediaRecorder(stream, pickVideoMimeType());

    this.current = { ...this.current, stream };
    this.emit();
  }

  async flipCamera(videoEl?: HTMLVideoElement): Promise<'user' | 'environment'> {
    const nextFacing: 'user' | 'environment' =
      this.currentFacing === 'user' ? 'environment' : 'user';
    this.currentFacing = nextFacing;

    if (!this.current.active || this.current.mode !== 'video_circle') {
      return this.currentFacing;
    }

    if (videoEl) {
      this.boundVideoEl = videoEl;
    }

    if (this.fallbackMode || typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      this.emit();
      return this.currentFacing;
    }

    const sid = this.sessionId;
    const hadAudio = (this.current.stream?.getAudioTracks().length ?? 0) > 0;
    const prevFacing: 'user' | 'environment' = nextFacing === 'user' ? 'environment' : 'user';

    // Stop previous recorder & stream cleanly before opening the opposite camera (required on mobile)
    this.discardActiveRecorder();
    stopMediaStream(this.current.stream);
    bindStreamToVideo(this.boundVideoEl, null);
    this.closeAudioContext();

    const tryAcquire = async (facing: 'user' | 'environment'): Promise<MediaStream | null> => {
      try {
        return await navigator.mediaDevices.getUserMedia({
          video: { facingMode: facing, width: 480, height: 480 },
          audio: hadAudio,
        });
      } catch {
        if (hadAudio) {
          try {
            return await navigator.mediaDevices.getUserMedia({
              video: { facingMode: facing, width: 480, height: 480 },
            });
          } catch {
            return null;
          }
        }
        return null;
      }
    };

    let newStream = await tryAcquire(nextFacing);
    if (!newStream) {
      this.currentFacing = prevFacing;
      newStream = await tryAcquire(prevFacing);
    }

    if (sid !== this.sessionId || !this.current.active) {
      stopMediaStream(newStream);
      return this.currentFacing;
    }

    if (!newStream) {
      this.enterFallback(sid);
      return this.currentFacing;
    }

    this.chunks = [];
    bindStreamToVideo(this.boundVideoEl, newStream);
    if (newStream.getAudioTracks().length > 0) {
      this.setupAudioAnalyser(newStream, sid);
    }
    this.setupMediaRecorder(newStream, pickVideoMimeType());
    this.current = { ...this.current, stream: newStream };
    this.emit();
    return this.currentFacing;
  }

  lock(): void {
    if (!this.current.active || this.current.locked) return;
    this.current = { ...this.current, locked: true };
    this.emit();
  }

  async stopAndSave(): Promise<RecordedMediaResult> {
    if (!this.current.active) {
      return { dataUrl: '', durationSec: 0, mimeType: '' };
    }

    ++this.sessionId;
    const mode = this.current.mode;
    const wasFallback = this.fallbackMode;
    const elapsed = this.current.elapsedSec;
    const durationSec = Math.max(
      1,
      elapsed,
      this.startTimeMs > 0 ? Math.round((Date.now() - this.startTimeMs) / 1000) : 1,
    );
    const defaultMime = mode === 'video_circle' ? 'video/webm' : 'audio/webm';
    const recorder = this.recorder;
    const mimeHint = this.activeMimeType || recorder?.mimeType || defaultMime;

    this.stopTimersAndAnalyser();

    if (recorder && recorder.state !== 'inactive') {
      await new Promise<void>((resolve) => {
        let settled = false;
        const done = () => {
          if (settled) return;
          settled = true;
          window.clearTimeout(safetyTimer);
          resolve();
        };
        const safetyTimer = window.setTimeout(done, 1500);
        const prevOnStop = recorder.onstop;
        recorder.onstop = (ev) => {
          if (typeof prevOnStop === 'function') {
            try {
              prevOnStop.call(recorder, ev);
            } catch {
              // ignore
            }
          }
          done();
        };
        try {
          recorder.stop();
        } catch {
          done();
        }
      });
    }

    const collectedChunks = [...this.chunks];
    this.recorder = null;
    this.chunks = [];
    this.activeMimeType = '';

    bindStreamToVideo(this.boundVideoEl, null);
    this.boundVideoEl = null;
    stopMediaStream(this.current.stream);
    this.closeAudioContext();
    this.fallbackMode = false;
    this.startTimeMs = 0;

    this.current = { ...INITIAL_STATE };
    this.emit();

    if (wasFallback || collectedChunks.length === 0) {
      return { dataUrl: '', durationSec, mimeType: mimeHint };
    }

    const mimeType = collectedChunks[0]?.type || mimeHint;
    const blob = new Blob(collectedChunks, { type: mimeType });
    if (blob.size === 0) {
      return { dataUrl: '', durationSec, mimeType };
    }

    const dataUrl = await blobToDataUrl(blob);
    return {
      dataUrl,
      durationSec,
      mimeType: blob.type || mimeType,
    };
  }

  cancel(): void {
    ++this.sessionId;
    this.cleanupSession(true);
  }

  private cleanupSession(notify: boolean): void {
    this.stopTimersAndAnalyser();
    this.discardActiveRecorder();
    bindStreamToVideo(this.boundVideoEl, null);
    this.boundVideoEl = null;
    stopMediaStream(this.current.stream);
    this.closeAudioContext();
    this.chunks = [];
    this.activeMimeType = '';
    this.fallbackMode = false;
    this.startTimeMs = 0;

    const wasActive = this.current.active || this.current.mode !== 'none' || this.current.stream !== null;
    this.current = { ...INITIAL_STATE };
    if (notify && wasActive) {
      this.emit();
    }
  }

  private discardActiveRecorder(): void {
    const rec = this.recorder;
    this.recorder = null;
    if (!rec) return;
    rec.ondataavailable = null;
    rec.onstop = null;
    rec.onerror = null;
    if (rec.state !== 'inactive') {
      try {
        rec.stop();
      } catch {
        // Ignore stop error on cancelled recorder
      }
    }
  }

  private setupMediaRecorder(stream: MediaStream, preferredMime: string): void {
    if (typeof MediaRecorder === 'undefined') {
      this.fallbackMode = true;
      return;
    }

    try {
      const options = preferredMime ? { mimeType: preferredMime } : undefined;
      const recorder = options ? new MediaRecorder(stream, options) : new MediaRecorder(stream);
      this.activeMimeType = recorder.mimeType || preferredMime;
      recorder.ondataavailable = (ev: BlobEvent) => {
        if (ev.data && ev.data.size > 0) {
          this.chunks.push(ev.data);
        }
      };
      recorder.onerror = () => {
        // Keep session active in timer fallback if MediaRecorder throws mid-stream
      };
      recorder.start(250);
      this.recorder = recorder;
    } catch {
      this.recorder = null;
      this.fallbackMode = true;
    }
  }

  private setupAudioAnalyser(stream: MediaStream, sid: number): void {
    const AudioCtx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) {
      this.startSyntheticLevelLoop(sid);
      return;
    }

    try {
      const ctx = new AudioCtx();
      if (ctx.state === 'suspended') {
        void ctx.resume().catch(() => {});
      }
      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.65;
      source.connect(analyser);

      this.audioCtx = ctx;
      this.analyser = analyser;
      this.analyserData = new Uint8Array(new ArrayBuffer(analyser.frequencyBinCount));

      const tick = (now: number) => {
        if (sid !== this.sessionId || !this.current.active || !this.analyser || !this.analyserData) {
          return;
        }
        if (now - this.lastLevelEmitMs >= 40) {
          this.lastLevelEmitMs = now;
          this.analyser.getByteTimeDomainData(this.analyserData);
          let sumSq = 0;
          const len = this.analyserData.length;
          for (let i = 0; i < len; i++) {
            const norm = (this.analyserData[i] - 128) / 128;
            sumSq += norm * norm;
          }
          const rms = len > 0 ? Math.sqrt(sumSq / len) : 0;
          const nextLevel = Math.min(1, Math.max(0, rms * 3.4));
          const smoothed = this.current.level * 0.35 + nextLevel * 0.65;
          this.current = { ...this.current, level: Number(smoothed.toFixed(3)) };
          this.emit();
        }
        this.levelRafId = window.requestAnimationFrame(tick);
      };
      this.levelRafId = window.requestAnimationFrame(tick);
    } catch {
      this.closeAudioContext();
      this.startSyntheticLevelLoop(sid);
    }
  }

  private enterFallback(sid: number): void {
    this.fallbackMode = true;
    this.current = { ...this.current, stream: null };
    this.startSyntheticLevelLoop(sid);
    this.emit();
  }

  private startElapsedTimer(sid: number): void {
    if (this.timerIntervalId !== null) {
      window.clearInterval(this.timerIntervalId);
    }
    this.timerIntervalId = window.setInterval(() => {
      if (sid !== this.sessionId || !this.current.active) return;
      const sec = Math.max(0, Math.floor((Date.now() - this.startTimeMs) / 1000));
      if (sec !== this.current.elapsedSec) {
        this.current = { ...this.current, elapsedSec: sec };
        this.emit();
      }
    }, 200);
  }

  private startSyntheticLevelLoop(sid: number): void {
    if (this.levelRafId !== null) {
      window.cancelAnimationFrame(this.levelRafId);
      this.levelRafId = null;
    }
    const tick = (now: number) => {
      if (sid !== this.sessionId || !this.current.active) return;
      if (now - this.lastLevelEmitMs >= 55) {
        this.lastLevelEmitMs = now;
        const t = now * 0.005;
        const synth = 0.14 + 0.18 * Math.abs(Math.sin(t)) + 0.1 * Math.abs(Math.sin(t * 2.3));
        this.current = { ...this.current, level: Number(Math.min(1, synth).toFixed(3)) };
        this.emit();
      }
      this.levelRafId = window.requestAnimationFrame(tick);
    };
    this.levelRafId = window.requestAnimationFrame(tick);
  }

  private stopTimersAndAnalyser(): void {
    if (this.timerIntervalId !== null) {
      window.clearInterval(this.timerIntervalId);
      this.timerIntervalId = null;
    }
    if (this.levelRafId !== null) {
      window.cancelAnimationFrame(this.levelRafId);
      this.levelRafId = null;
    }
    this.analyser = null;
    this.analyserData = null;
    this.lastLevelEmitMs = 0;
  }

  private closeAudioContext(): void {
    const ctx = this.audioCtx;
    this.audioCtx = null;
    if (ctx) {
      void ctx.close().catch(() => {});
    }
  }

  private emit(): void {
    const snapshot = this.getState();
    for (const cb of this.listeners) {
      cb(snapshot);
    }
  }
}

export const mediaRecorderController = new MediaRecorderController();
