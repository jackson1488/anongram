import { AVATAR_GRADIENTS } from '../../core/crypto/identity';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { authStore } from '../auth/authStore';

export function createProfileView(): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });

  const render = () => {
    clear(root);
    const id = authStore.identity;
    if (!id) return;
    const grad = AVATAR_GRADIENTS[id.avatarIndex ?? 0];

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, 'Профиль'),
      ),
    );

    const scroll = el('div', { class: 'ag-scroll-list', style: 'padding: 8px 14px 96px;' });
    scroll.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card', style: 'align-items:center;text-align:center;' },
        el(
          'div',
          {
            class: 'ag-avatar-preview',
            style: `background:linear-gradient(135deg, ${grad[0]}, ${grad[1]});margin:4px auto;`,
          },
          id.displayName.slice(0, 1).toUpperCase(),
        ),
        el('h2', { style: 'margin:0;font-size:20px;' }, id.displayName),
        el('span', { style: 'font-size:13px;color:var(--ag-accent);' }, id.username),
      ),
    );

    const infoCard = el(
      'div',
      { class: 'ag-card ag-settings-card', style: 'margin-top:12px;' },
      el('div', { class: 'ag-section-label' }, 'Public ID'),
      el('code', { class: 'ag-ag1-code' }, id.publicId),
      el('div', { class: 'ag-section-label' }, 'Ed25519 Public Key'),
      el('code', { class: 'ag-ag1-code' }, id.ed25519PublicKey),
      el('div', { class: 'ag-section-label' }, 'Fingerprint'),
      el('code', { class: 'ag-ag1-code' }, id.fingerprint),
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--primary',
          onClick: async () => {
            await copyToClipboard(id.publicId);
            showToast('Public ID скопирован');
          },
        },
        icons.copy(16),
        'Копировать Public ID',
      ),
    );
    scroll.appendChild(infoCard);
    root.appendChild(scroll);
  };

  const unsub = authStore.subscribe(render);
  render();
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => unsub();
  return root;
}
