import { i18nStore } from '../../../core/i18n/i18nStore';
import { copyToClipboard, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { authStore } from '../../auth/authStore';

function renderDeterministicQrSvg(seed: string): SVGSVGElement {
  const size = 21;
  const cells: string[] = [];
  let hash = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    hash ^= seed.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  const isFinder = (r: number, c: number) =>
    (r < 7 && c < 7) || (r < 7 && c >= size - 7) || (r >= size - 7 && c < 7);

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      let filled = false;
      if (isFinder(r, c)) {
        const lr = r < 7 ? r : r - (size - 7);
        const lc = c < 7 ? c : c - (size - 7);
        filled =
          lr === 0 || lr === 6 || lc === 0 || lc === 6 || (lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4);
      } else {
        const mix = Math.imul(hash ^ (r * 31 + c * 17), 2654435761) >>> 0;
        filled = (mix & 1) === 1;
      }
      if (filled) {
        cells.push(`<rect x="${c}" y="${r}" width="1" height="1"/>`);
      }
    }
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
  svg.setAttribute('class', 'ag-qr-svg');
  svg.setAttribute('fill', 'currentColor');
  svg.innerHTML = cells.join('');
  return svg;
}

export function createMyQrCodeScreen(): HTMLElement {
  const t = i18nStore.t;
  const id = authStore.identity;
  return el(
    'div',
    { class: 'ag-card ag-qr-card' },
    renderDeterministicQrSvg(id?.publicId ?? 'anongram'),
    el('h3', { style: 'margin:0;' }, id?.displayName ?? 'Ghost'),
    el('code', { class: 'ag-ag1-code' }, id?.publicId ?? 'ag_anonymous'),
    el('p', { style: 'margin:0;font-size:13px;color:var(--ag-text-secondary);text-align:center;' }, t.settings.qrHint),
    el(
      'button',
      {
        class: 'ag-pill-btn ag-pill-btn--primary',
        onClick: async () => {
          await copyToClipboard(id?.publicId ?? '');
          showToast(t.common.copied);
        },
      },
      icons.copy(16),
      t.common.copy,
    ),
  );
}
