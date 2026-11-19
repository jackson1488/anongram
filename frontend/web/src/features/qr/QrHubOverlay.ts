import { copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';

function renderQrSvg(seed: string): SVGSVGElement {
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
      if (filled) cells.push(`<rect x="${c}" y="${r}" width="1" height="1"/>`);
    }
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
  svg.setAttribute('class', 'ag-qr-svg');
  svg.setAttribute('fill', 'currentColor');
  svg.innerHTML = cells.join('');
  return svg;
}

export function openQrHubOverlay(): void {
  const id = authStore.identity;
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });

  const peerInput = el('input', {
    class: 'ag-input',
    placeholder: 'Вставьте Public ID контакта (ag_...)',
  }) as HTMLInputElement;

  overlay.appendChild(
    el(
      'div',
      { class: 'ag-card ag-qr-card', style: 'max-width:390px;width:100%;' },
      el(
        'div',
        { style: 'width:100%;display:flex;align-items:center;justify-content:space-between;' },
        el('strong', { style: 'font-size:17px;' }, 'Stealth QR Hub'),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
      renderQrSvg(id?.publicId ?? 'anongram'),
      el('code', { class: 'ag-ag1-code', style: 'width:100%;text-align:center;' }, id?.publicId ?? 'ag_anonymous'),
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--primary',
          style: 'width:100%;',
          onClick: async () => {
            await copyToClipboard(id?.publicId ?? '');
            showToast('Stealth ID скопирован');
          },
        },
        icons.copy(16),
        'Скопировать мой Stealth ID',
      ),
      el(
        'div',
        { style: 'width:100%;display:flex;gap:8px;margin-top:4px;' },
        peerInput,
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--secondary',
            onClick: () => {
              if (!peerInput.value.trim()) return;
              showToast(`Запрос рукопожатия отправлен: ${peerInput.value.trim()}`);
              overlay.remove();
            },
          },
          'Добавить',
        ),
      ),
    ),
  );

  document.body.appendChild(overlay);
}
