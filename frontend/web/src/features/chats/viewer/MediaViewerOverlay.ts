import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import type { MessageAttachment } from '../models';
import { ViewerI18n } from './i18n/ViewerI18n';
import './viewer.css';

const isImage = (a: MessageAttachment) => !!a.previewUrl && (a.type === 'photo' || a.type === 'album');

export function openMediaViewer(attachment: MessageAttachment, gallery?: MessageAttachment[], startIndex?: number): void {
  const items = gallery && gallery.length > 0 ? gallery : [attachment];
  let index = startIndex ?? Math.max(0, items.indexOf(attachment));
  const s = ViewerI18n.current;

  const title = el('span', { class: 'ag-mv-title' });
  const counter = el('span', { class: 'ag-mv-counter' });
  const stage = el('div', { class: 'ag-mv-stage' });
  const close = () => {
    document.removeEventListener('keydown', onKey);
    overlay.remove();
  };
  const overlay = el(
    'div',
    { class: 'ag-mv-overlay' },
    el('div', { class: 'ag-mv-top' }, title, counter, el('button', { class: 'ag-icon-btn', title: s.close, onClick: close }, icons.close(20))),
    stage,
  );

  let scale = 1, tx = 0, ty = 0;
  let img: HTMLImageElement | null = null;
  const apply = () => {
    if (img) img.style.transform = `translate(${tx}px,${ty}px) scale(${scale})`;
  };
  const setScale = (v: number) => {
    scale = Math.max(1, Math.min(6, v));
    if (scale === 1) { tx = 0; ty = 0; }
    apply();
  };
  const go = (d: number) => {
    const n = index + d;
    if (n < 0 || n >= items.length) return;
    index = n;
    show();
  };

  function show(): void {
    const a = items[index];
    scale = 1; tx = 0; ty = 0; img = null;
    title.textContent = a.title;
    counter.textContent = items.length > 1 ? `${index + 1} / ${items.length}` : '';
    stage.replaceChildren();
    let content: HTMLElement;
    if (isImage(a)) {
      const im = el('img', { src: a.previewUrl, alt: a.title, class: 'ag-mv-img', draggable: false });
      img = im;
      content = im;
      bindZoomPan(im);
    } else if (a.type === 'video_circle') {
      content = el('div', { class: 'ag-mv-doc' }, icons.video(48), el('div', null, a.title));
    } else {
      content = el(
        'div',
        { class: 'ag-mv-doc' },
        el('div', { class: 'ag-mv-badge' }, (a.type || s.file).toUpperCase()),
        el('h3', null, a.title),
        el('p', null, a.fileSizeLabel ?? a.subtitle ?? ''),
        a.previewUrl ? el('a', { href: a.previewUrl, download: a.title }, s.download) : null,
      );
    }
    stage.appendChild(content);
    if (items.length > 1) {
      if (index > 0) stage.appendChild(el('button', { class: 'ag-mv-nav prev', title: s.prev, onClick: () => go(-1) }, '‹'));
      if (index < items.length - 1) stage.appendChild(el('button', { class: 'ag-mv-nav next', title: s.next, onClick: () => go(1) }, '›'));
    }
  }

  function bindZoomPan(im: HTMLImageElement): void {
    im.addEventListener('wheel', (e) => {
      e.preventDefault();
      setScale(scale * (e.deltaY < 0 ? 1.15 : 1 / 1.15));
    }, { passive: false });
    im.addEventListener('dblclick', () => setScale(scale > 1 ? 1 : 2.5));
    const pts = new Map<number, { x: number; y: number }>();
    let startDist = 0, startScale = 1, swipeStartX = 0, swipeStartY = 0;
    im.addEventListener('pointerdown', (e) => {
      im.setPointerCapture(e.pointerId);
      pts.set(e.pointerId, { x: e.clientX, y: e.clientY });
      swipeStartX = e.clientX; swipeStartY = e.clientY;
      if (pts.size === 2) {
        const [p, q] = [...pts.values()];
        startDist = Math.hypot(p.x - q.x, p.y - q.y);
        startScale = scale;
      }
    });
    im.addEventListener('pointermove', (e) => {
      const prev = pts.get(e.pointerId);
      if (!prev) return;
      pts.set(e.pointerId, { x: e.clientX, y: e.clientY });
      if (pts.size === 2) {
        const [p, q] = [...pts.values()];
        setScale(startScale * (Math.hypot(p.x - q.x, p.y - q.y) / (startDist || 1)));
      } else if (scale > 1) {
        tx += e.clientX - prev.x;
        ty += e.clientY - prev.y;
        apply();
      }
    });
    const end = (e: PointerEvent) => {
      pts.delete(e.pointerId);
      if (pts.size === 0 && scale === 1) {
        const dx = e.clientX - swipeStartX, dy = e.clientY - swipeStartY;
        if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy)) go(dx < 0 ? 1 : -1);
        else if (dy > 120 && Math.abs(dy) > Math.abs(dx)) close();
      }
    };
    im.addEventListener('pointerup', end);
    im.addEventListener('pointercancel', (e) => pts.delete(e.pointerId));
  }

  function onKey(e: KeyboardEvent): void {
    if (e.key === 'Escape') close();
    else if (e.key === 'ArrowLeft') go(-1);
    else if (e.key === 'ArrowRight') go(1);
    else if (e.key === '+' || e.key === '=') setScale(scale * 1.25);
    else if (e.key === '-') setScale(scale / 1.25);
  }

  stage.addEventListener('click', (e) => {
    if (e.target === stage) close();
  });
  document.addEventListener('keydown', onKey);
  show();
  document.body.appendChild(overlay);
}
