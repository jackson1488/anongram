import { el } from '../../core/ui/dom';
import { CameraI18n } from './i18n/CameraI18n';
import './camera.css';

/** Opens a camera capture overlay; resolves with a JPEG blob or null. */
export function openCamera(): Promise<Blob | null> {
  return new Promise((resolve) => {
    const s = CameraI18n.current;
    let facing: 'user' | 'environment' = 'environment';
    let stream: MediaStream | null = null;
    const video = el('video', { class: 'ag-cam-video', autoplay: true, playsinline: true, muted: true });
    const msg = el('div', { class: 'ag-cam-msg', style: 'display:none' }, s.denied);
    const done = (b: Blob | null) => {
      stream?.getTracks().forEach((t) => t.stop());
      document.removeEventListener('keydown', onKey);
      overlay.remove();
      resolve(b);
    };
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') done(null); };
    const start = async () => {
      stream?.getTracks().forEach((t) => t.stop());
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: facing }, audio: false });
        video.srcObject = stream;
        msg.style.display = 'none';
      } catch {
        msg.style.display = 'block';
      }
    };
    const overlay = el(
      'div',
      { class: 'ag-cam-overlay' },
      video,
      msg,
      el(
        'div',
        { class: 'ag-cam-bar' },
        el('button', { class: 'ag-cam-btn', onClick: () => done(null) }, s.close),
        el('button', {
          class: 'ag-cam-shot',
          title: s.capture,
          onClick: () => {
            if (!video.videoWidth) return;
            const c = document.createElement('canvas');
            c.width = video.videoWidth; c.height = video.videoHeight;
            c.getContext('2d')!.drawImage(video, 0, 0);
            c.toBlob((b) => done(b), 'image/jpeg', 0.9);
          },
        }),
        el('button', { class: 'ag-cam-btn', onClick: () => { facing = facing === 'user' ? 'environment' : 'user'; void start(); } }, s.flip),
      ),
    );
    document.addEventListener('keydown', onKey);
    document.body.appendChild(overlay);
    void start();
  });
}
