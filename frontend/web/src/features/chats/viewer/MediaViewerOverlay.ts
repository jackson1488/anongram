import { copyToClipboard, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import type { ChatMessage, MessageAttachment } from '../models';
import { openForwardModal } from '../ui/components/ForwardModal';
import { ViewerI18n } from './i18n/ViewerI18n';
import './viewer.css';

const isImage = (a: MessageAttachment) => !!a.previewUrl && (a.type === 'photo' || a.type === 'album');

const TEXT_EXTENSIONS = new Set([
  'txt', 'json', 'md', 'csv', 'log', 'xml', 'html', 'css', 'js', 'ts', 'tsx', 'jsx',
  'kt', 'java', 'py', 'rs', 'go', 'sh', 'yml', 'yaml', 'toml', 'ini', 'sql', 'svg',
]);

function getExtension(title: string): string {
  const idx = title.lastIndexOf('.');
  return idx > 0 && idx < title.length - 1 ? title.slice(idx + 1).toLowerCase() : '';
}

function getMimeBadge(a: MessageAttachment): string {
  if (a.previewUrl?.startsWith('data:')) {
    const semi = a.previewUrl.indexOf(';');
    const comma = a.previewUrl.indexOf(',');
    const end = semi !== -1 && semi < comma ? semi : comma;
    const mime = end > 5 ? a.previewUrl.slice(5, end) : '';
    if (mime) {
      const short = mime.split('/').pop()?.replace('+xml', '') ?? mime;
      return short.toUpperCase();
    }
  }
  const ext = getExtension(a.title);
  if (ext) return ext.toUpperCase();
  return (a.type || ViewerI18n.current.file).toUpperCase();
}

function decodeTextDataUrl(a: MessageAttachment): string | null {
  const url = a.previewUrl;
  if (!url || !url.startsWith('data:')) return null;
  const comma = url.indexOf(',');
  if (comma < 0) return null;
  const header = url.slice(5, comma).toLowerCase();
  const mime = header.split(';')[0] ?? '';
  const ext = getExtension(a.title);
  const isTextMime =
    mime.startsWith('text/') ||
    mime === 'application/json' ||
    mime === 'application/javascript' ||
    mime === 'application/xml' ||
    mime === 'image/svg+xml' ||
    ((mime === '' || mime === 'application/octet-stream') && TEXT_EXTENSIONS.has(ext));
  if (!isTextMime) return null;

  const payload = url.slice(comma + 1);
  let text = '';
  try {
    if (header.includes(';base64')) {
      const bin = atob(payload);
      const bytes = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
      text = new TextDecoder('utf-8', { fatal: false }).decode(bytes);
    } else {
      text = decodeURIComponent(payload);
    }
  } catch {
    return null;
  }

  if (mime === 'application/json' || ext === 'json') {
    try {
      text = JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      // Keep raw text if JSON is malformed
    }
  }
  return text.length > 12000 ? `${text.slice(0, 12000)}\n…` : text;
}

function buildForwardMessage(attachment: MessageAttachment, sourceMessage?: ChatMessage): ChatMessage {
  if (sourceMessage) return sourceMessage;
  return {
    id: `att_${Date.now()}`,
    chatId: '',
    isOutgoing: true,
    senderName: '',
    text: '',
    timestamp: '',
    createdAt: Date.now(),
    attachment,
  };
}

function formatDuration(sec?: number): string {
  const total = Math.max(0, Math.round(sec ?? 0));
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function openDocumentViewer(attachment: MessageAttachment, sourceMessage?: ChatMessage): void {
  const s = ViewerI18n.current;
  const badge = getMimeBadge(attachment);
  const sizeSub = attachment.fileSizeLabel || attachment.subtitle || s.unknownSize;
  const textPreview = decodeTextDataUrl(attachment);

  const close = () => {
    document.removeEventListener('keydown', onKey);
    overlay.remove();
  };
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };

  let previewBlock: HTMLElement;
  if (textPreview !== null) {
    const copyBtn = el(
      'button',
      {
        class: 'ag-dv-copy-btn',
        onClick: async () => {
          const ok = await copyToClipboard(textPreview);
          if (ok) showToast(s.copied);
        },
      },
      s.copyText,
    );
    previewBlock = el(
      'div',
      { class: 'ag-dv-code-wrap' },
      el('div', { class: 'ag-dv-code-bar' }, el('span', null, badge), copyBtn),
      el('pre', { class: 'ag-dv-code' }, textPreview),
    );
  } else {
    previewBlock = el('div', { class: 'ag-dv-empty' }, s.previewUnavailable);
  }

  const downloadBtn = attachment.previewUrl
    ? el(
        'a',
        { class: 'ag-dv-btn ag-dv-btn--primary', href: attachment.previewUrl, download: attachment.title },
        tgIcons.download(18),
        s.download,
      )
    : el('button', { class: 'ag-dv-btn is-disabled', disabled: true }, tgIcons.download(18), s.download);

  const forwardBtn = el(
    'button',
    {
      class: 'ag-dv-btn',
      onClick: () => {
        const msg = buildForwardMessage(attachment, sourceMessage);
        close();
        openForwardModal([msg]);
      },
    },
    icons.forward(18),
    s.forwardAnon,
  );

  const sheet = el(
    'div',
    { class: 'ag-dv-sheet' },
    el('div', { class: 'ag-dv-grab' }),
    el(
      'div',
      { class: 'ag-dv-head' },
      el('div', { class: 'ag-dv-head-title' }, icons.fileText(20), el('span', null, attachment.title || s.file)),
      el('button', { class: 'ag-icon-btn', title: s.close, onClick: close }, icons.close(20)),
    ),
    el(
      'div',
      { class: 'ag-dv-body' },
      el(
        'div',
        { class: 'ag-dv-meta-card' },
        el(
          'div',
          { class: 'ag-dv-meta-info' },
          el('div', { class: 'ag-dv-meta-name' }, attachment.title || s.file),
          el('div', { class: 'ag-dv-meta-sub' }, sizeSub),
        ),
        el('span', { class: 'ag-dv-mime-badge' }, badge),
      ),
      previewBlock,
    ),
    el('div', { class: 'ag-dv-actions' }, forwardBtn, downloadBtn),
  );

  const overlay = el(
    'div',
    {
      class: 'ag-dv-overlay',
      onClick: (e: Event) => {
        if (e.target === overlay) close();
      },
    },
    sheet,
  );

  document.addEventListener('keydown', onKey);
  document.body.appendChild(overlay);
}

export function openMediaViewer(
  attachment: MessageAttachment,
  gallery?: MessageAttachment[],
  startIndex?: number,
  sourceMessage?: ChatMessage,
): void {
  const items = gallery && gallery.length > 0 ? gallery : [attachment];
  let index = startIndex ?? Math.max(0, items.indexOf(attachment));
  const s = ViewerI18n.current;

  const title = el('span', { class: 'ag-mv-title' });
  const counter = el('span', { class: 'ag-mv-counter' });
  const actions = el('div', { class: 'ag-mv-actions' });
  const stage = el('div', { class: 'ag-mv-stage' });
  const close = () => {
    document.removeEventListener('keydown', onKey);
    overlay.remove();
  };
  const overlay = el(
    'div',
    { class: 'ag-mv-overlay' },
    el(
      'div',
      { class: 'ag-mv-top' },
      el('div', { class: 'ag-mv-meta' }, title, counter),
      actions,
    ),
    stage,
  );

  let scale = 1, tx = 0, ty = 0;
  let img: HTMLImageElement | null = null;
  let zoomPillBtn: HTMLButtonElement | null = null;

  const syncZoomUi = () => {
    if (!zoomPillBtn) return;
    const rounded = Math.round(scale * 10) / 10;
    zoomPillBtn.textContent = `${rounded}x`;
    zoomPillBtn.classList.toggle('is-active', scale > 1.05);
  };

  const apply = () => {
    if (img) img.style.transform = `translate(${tx}px,${ty}px) scale(${scale})`;
    syncZoomUi();
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

  function renderTopActions(a: MessageAttachment): void {
    actions.replaceChildren();
    zoomPillBtn = null;

    if (isImage(a)) {
      const zoomOutBtn = el(
        'button',
        { class: 'ag-icon-btn', title: s.zoomOut, onClick: () => setScale(scale / 1.4) },
        tgIcons.minus(18),
      );
      zoomPillBtn = el(
        'button',
        {
          class: 'ag-mv-zoom-pill',
          title: s.zoomReset,
          onClick: () => setScale(scale > 1.05 ? 1 : 2),
        },
        '1x',
      ) as HTMLButtonElement;
      const zoomInBtn = el(
        'button',
        { class: 'ag-icon-btn', title: s.zoomIn, onClick: () => setScale(scale * 1.4) },
        tgIcons.plus(18),
      );
      actions.append(zoomOutBtn, zoomPillBtn, zoomInBtn);
    }

    if (a.previewUrl) {
      actions.appendChild(
        el(
          'a',
          { class: 'ag-icon-btn', href: a.previewUrl, download: a.title, title: s.download },
          tgIcons.download(18),
        ),
      );
    }

    actions.appendChild(
      el(
        'button',
        {
          class: 'ag-icon-btn',
          title: s.forwardAnon,
          onClick: () => {
            const msg = buildForwardMessage(a, sourceMessage);
            close();
            openForwardModal([msg]);
          },
        },
        icons.forward(18),
      ),
    );

    actions.appendChild(
      el('button', { class: 'ag-icon-btn', title: s.close, onClick: close }, icons.close(20)),
    );
  }

  function renderVideoCircle(a: MessageAttachment): HTMLElement {
    const playIconWrap = el('div', { class: 'ag-mv-circle-play-badge' }, icons.play(24));
    const playOverlay = el('div', { class: 'ag-mv-circle-play' }, playIconWrap);
    let playing = false;

    const wrap = el('div', { class: 'ag-mv-circle-wrap' });
    if (a.previewUrl) {
      const video = el('video', {
        class: 'ag-mv-circle-video',
        src: a.previewUrl,
        playsinline: true,
        loop: true,
        autoplay: true,
      }) as HTMLVideoElement;
      playing = true;
      wrap.classList.add('is-playing');
      playIconWrap.replaceChildren(icons.pause(24));
      video.addEventListener('play', () => {
        playing = true;
        wrap.classList.add('is-playing');
        playIconWrap.replaceChildren(icons.pause(24));
      });
      video.addEventListener('pause', () => {
        playing = false;
        wrap.classList.remove('is-playing');
        playIconWrap.replaceChildren(icons.play(24));
      });
      wrap.addEventListener('click', () => {
        if (video.paused) {
          void video.play();
        } else {
          video.pause();
        }
      });
      wrap.append(video, playOverlay);
    } else {
      const placeholder = el(
        'div',
        { class: 'ag-mv-circle-placeholder' },
        icons.video(44),
        el('div', { style: 'font-weight:600;font-size:14px;' }, a.title || s.videoCircle),
        el('div', { style: 'font-size:12px;' }, s.tapToPlay),
      );
      wrap.addEventListener('click', () => {
        playing = !playing;
        wrap.classList.toggle('is-playing', playing);
        playIconWrap.replaceChildren(playing ? icons.pause(24) : icons.play(24));
      });
      wrap.append(placeholder, playOverlay);
    }

    return el(
      'div',
      { class: 'ag-mv-circle-stage' },
      wrap,
      a.durationSec !== undefined ? el('span', { class: 'ag-mv-circle-duration' }, formatDuration(a.durationSec)) : null,
    );
  }

  function show(): void {
    const a = items[index];
    scale = 1; tx = 0; ty = 0; img = null;
    title.textContent = a.title;
    counter.textContent = items.length > 1 ? `${index + 1} / ${items.length}` : '';
    renderTopActions(a);
    stage.replaceChildren();
    let content: HTMLElement;
    if (isImage(a)) {
      const im = el('img', { src: a.previewUrl, alt: a.title, class: 'ag-mv-img', draggable: false });
      img = im;
      content = im;
      bindZoomPan(im);
    } else if (a.type === 'video_circle') {
      content = renderVideoCircle(a);
    } else {
      content = el(
        'div',
        { class: 'ag-mv-doc' },
        el('div', { class: 'ag-mv-badge' }, getMimeBadge(a)),
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
    im.addEventListener('dblclick', () => setScale(scale > 1.05 ? 1 : 2));
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
