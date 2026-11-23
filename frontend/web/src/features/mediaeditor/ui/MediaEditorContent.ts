import { el } from '../../../core/ui/dom';
import { MediaEditorI18n } from '../i18n/MediaEditorI18n';
import type { EditedMedia, EditableMediaItem } from '../api/EditedMedia';
import './mediaeditor.css';

type Mode = 'crop' | 'draw';

/** Canvas crop/draw editor. Resolves with edited image or null if cancelled. */
export function openMediaEditor(item: EditableMediaItem): Promise<EditedMedia | null> {
  return new Promise((resolve) => {
    const s = MediaEditorI18n.current;
    const canvas = el('canvas');
    const ctx = canvas.getContext('2d')!;
    let mode: Mode = 'draw';
    let base: ImageBitmap | HTMLCanvasElement | null = null;
    const strokes: { x: number; y: number }[][] = [];
    let crop: { x: number; y: number; w: number; h: number } | null = null;
    let drag: { x: number; y: number } | null = null;

    const finish = (r: EditedMedia | null) => { overlay.remove(); resolve(r); };
    const draw = () => {
      if (!base) return;
      ctx.drawImage(base, 0, 0);
      ctx.lineCap = 'round'; ctx.lineJoin = 'round';
      ctx.lineWidth = Math.max(4, canvas.width / 150);
      ctx.strokeStyle = getComputedStyle(document.documentElement).getPropertyValue('--ag-accent').trim() || '#fff';
      for (const st of strokes) {
        ctx.beginPath();
        st.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)));
        ctx.stroke();
      }
      if (crop) {
        ctx.save();
        ctx.fillStyle = 'rgba(0,0,0,.5)';
        ctx.beginPath();
        ctx.rect(0, 0, canvas.width, canvas.height);
        ctx.rect(crop.x, crop.y, crop.w, crop.h);
        ctx.fill('evenodd');
        ctx.restore();
      }
    };
    const pos = (e: PointerEvent) => {
      const r = canvas.getBoundingClientRect();
      return { x: ((e.clientX - r.left) / r.width) * canvas.width, y: ((e.clientY - r.top) / r.height) * canvas.height };
    };
    canvas.addEventListener('pointerdown', (e) => {
      canvas.setPointerCapture(e.pointerId);
      drag = pos(e);
      if (mode === 'draw') strokes.push([drag]);
      else crop = { x: drag.x, y: drag.y, w: 0, h: 0 };
    });
    canvas.addEventListener('pointermove', (e) => {
      if (!drag) return;
      const p = pos(e);
      if (mode === 'draw') strokes[strokes.length - 1].push(p);
      else crop = { x: Math.min(drag.x, p.x), y: Math.min(drag.y, p.y), w: Math.abs(p.x - drag.x), h: Math.abs(p.y - drag.y) };
      draw();
    });
    canvas.addEventListener('pointerup', () => { drag = null; });

    const modeBtn = (m: Mode, label: string) => {
      const b = el('button', { class: `ag-me-btn${mode === m ? ' is-active' : ''}`, onClick: () => { mode = m; bar.querySelectorAll('[data-mode]').forEach((n) => n.classList.toggle('is-active', n.getAttribute('data-mode') === m)); } }, label);
      b.setAttribute('data-mode', m);
      return b;
    };
    const bar = el(
      'div',
      { class: 'ag-me-bar' },
      el('button', { class: 'ag-me-btn', onClick: () => finish(null) }, s.cancel),
      modeBtn('crop', s.crop),
      modeBtn('draw', s.draw),
      el('button', { class: 'ag-me-btn', onClick: () => { strokes.pop(); crop = null; draw(); } }, s.undo),
      el('span', { class: 'sp' }),
      el('button', {
        class: 'ag-me-btn primary',
        onClick: () => {
          const c = crop && crop.w > 8 && crop.h > 8 ? crop : null;
          crop = null;
          draw();
          const out = document.createElement('canvas');
          const r = c ?? { x: 0, y: 0, w: canvas.width, h: canvas.height };
          out.width = r.w; out.height = r.h;
          out.getContext('2d')!.drawImage(canvas, r.x, r.y, r.w, r.h, 0, 0, r.w, r.h);
          out.toBlob((blob) => finish(blob ? { id: item.id, blob } : null), 'image/jpeg', 0.9);
        },
      }, s.done),
    );
    const overlay = el('div', { class: 'ag-me-overlay' }, bar, el('div', { class: 'ag-me-stage' }, canvas));
    document.body.appendChild(overlay);
    createImageBitmap(item.source).then((bmp) => {
      base = bmp;
      canvas.width = bmp.width;
      canvas.height = bmp.height;
      draw();
    }).catch(() => finish(null));
  });
}
