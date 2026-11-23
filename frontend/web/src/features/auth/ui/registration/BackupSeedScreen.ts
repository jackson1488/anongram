import type { UserIdentity } from '../../../../core/crypto/identity';
import { copyToClipboard, el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { authStore } from '../../authStore';
import { authT } from '../../i18n';

function openMemoryQuiz(identity: UserIdentity): void {
  const t = authT();
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });
  const w3 = el('input', { class: 'ag-input', placeholder: t.quizWord(3) }) as HTMLInputElement;
  const w7 = el('input', { class: 'ag-input', placeholder: t.quizWord(7) }) as HTMLInputElement;
  const err = el('div', { class: 'ag-auth-error' });
  overlay.appendChild(
    el(
      'div',
      { class: 'ag-card ag-quiz-modal' },
      el('h3', { style: 'margin:0;font-size:18px;' }, t.quizTitle),
      el('p', { class: 'ag-auth-desc' }, t.quizDesc),
      w3,
      w7,
      err,
      el(
        'div',
        { style: 'display:flex;gap:10px;' },
        el('button', { class: 'ag-cta-outline', style: 'height:46px;', onClick: () => overlay.remove() }, t.cancel),
        el(
          'button',
          {
            class: 'ag-cta-gradient',
            style: 'height:46px;',
            onClick: () => {
              const a = w3.value.trim().toLowerCase();
              const b = w7.value.trim().toLowerCase();
              if (a === identity.seedPhrase[2] && b === identity.seedPhrase[6]) {
                overlay.remove();
                authStore.confirmIdentity(identity);
              } else {
                err.textContent = t.quizMismatch;
              }
            },
          },
          t.confirm,
        ),
      ),
    ),
  );
  document.body.appendChild(overlay);
  w3.focus();
}

export function createBackupSeedScreen(identity: UserIdentity, onBack: () => void): HTMLElement {
  const t = authT();
  return el(
    'div',
    { class: 'ag-auth-container' },
    el(
      'div',
      { style: 'display:flex;flex-direction:column;gap:14px;' },
      el(
        'div',
        { class: 'ag-auth-toolbar' },
        el('button', { class: 'ag-icon-btn', onClick: onBack }, icons.back(22)),
        el('h2', null, t.backupTitle),
      ),
      el('div', { class: 'ag-warning-banner' }, t.backupWarning),
      el(
        'div',
        { class: 'ag-seed-grid' },
        ...identity.seedPhrase.map((word, idx) =>
          el('div', { class: 'ag-seed-word' }, el('span', null, `${idx + 1}.`), el('strong', null, word)),
        ),
      ),
    ),
    el(
      'div',
      { class: 'ag-auth-bottom' },
      el(
        'button',
        {
          class: 'ag-cta-outline',
          onClick: async () => {
            await copyToClipboard(identity.seedPhrase.join(' '));
            showToast(t.wordsCopied);
          },
        },
        icons.copy(16),
        t.copyWords,
      ),
      el('button', { class: 'ag-cta-gradient', onClick: () => openMemoryQuiz(identity) }, t.confirmCta),
    ),
  );
}
