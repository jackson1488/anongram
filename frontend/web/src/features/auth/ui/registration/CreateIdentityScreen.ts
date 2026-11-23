import type { UserIdentity } from '../../../../core/crypto/identity';
import { AVATAR_GRADIENTS } from '../../../../core/crypto/identity';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { authStore } from '../../authStore';
import { authT } from '../../i18n';

export interface CreateIdentityProps {
  onBack: () => void;
  onDone: (identity: UserIdentity) => void;
}

/** Create-identity screen: nickname input is never re-created while typing. */
export function createCreateIdentityScreen(props: CreateIdentityProps): HTMLElement {
  const t = authT();
  let nickname = 'GhostWalker';
  let avatarIndex = 0;
  let genStep = 0;
  let draft: UserIdentity | null = null;

  const preview = el('div', { class: 'ag-avatar-preview' });
  const swatches = el('div', { class: 'ag-avatar-swatches' });
  const nameInput = el('input', {
    class: 'ag-input',
    value: nickname,
    placeholder: t.namePlaceholder,
    onInput: (e: Event) => {
      nickname = (e.target as HTMLInputElement).value;
      updatePreview();
    },
  }) as HTMLInputElement;
  const stepsBox = el('div', { class: 'ag-pipeline-card' });
  const bottom = el('div', { class: 'ag-auth-bottom' });

  const updatePreview = () => {
    const g = AVATAR_GRADIENTS[avatarIndex];
    preview.style.background = `linear-gradient(135deg, ${g[0]}, ${g[1]})`;
    preview.textContent = (nickname.trim() || 'G').slice(0, 1).toUpperCase();
  };

  const renderSwatches = () => {
    swatches.replaceChildren(
      ...AVATAR_GRADIENTS.map((g, idx) =>
        el('button', {
          class: `ag-avatar-swatch ${avatarIndex === idx ? 'is-selected' : ''}`,
          style: `background:linear-gradient(135deg, ${g[0]}, ${g[1]});`,
          onClick: () => {
            avatarIndex = idx;
            renderSwatches();
            updatePreview();
          },
        }),
      ),
    );
  };

  const renderSteps = () => {
    const labels = [t.step1, t.step2, t.step3, t.step4];
    stepsBox.replaceChildren(
      ...labels.map((label, idx) =>
        el(
          'div',
          { class: `ag-pipeline-step ${genStep > idx ? 'is-done' : ''}` },
          genStep > idx ? icons.check(15) : icons.shield(15),
          el('span', null, label),
        ),
      ),
      ...(draft ? [el('code', { class: 'ag-ag1-code', style: 'margin-top:4px;' }, draft.publicId)] : []),
    );
  };

  const renderBottom = () => {
    bottom.replaceChildren(
      draft
        ? el('button', { class: 'ag-cta-gradient', onClick: () => props.onDone(draft!) }, t.toBackup)
        : el('button', { class: 'ag-cta-gradient', onClick: startPipeline }, genStep > 0 ? t.generating : t.generateKeys),
    );
  };

  const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));
  const startPipeline = async () => {
    if (genStep > 0 && genStep < 4) return;
    for (const s of [1, 2, 3]) {
      genStep = s;
      renderSteps();
      renderBottom();
      await wait(320);
    }
    nameInput.disabled = true;
    draft = await authStore.generateDraftIdentity(nickname, avatarIndex);
    genStep = 4;
    renderSteps();
    renderBottom();
  };

  updatePreview();
  renderSwatches();
  renderSteps();
  renderBottom();

  return el(
    'div',
    { class: 'ag-auth-container' },
    el(
      'div',
      { style: 'display:flex;flex-direction:column;gap:14px;' },
      el(
        'div',
        { class: 'ag-auth-toolbar' },
        el('button', { class: 'ag-icon-btn', onClick: props.onBack }, icons.back(22)),
        el('h2', null, t.createTitle),
      ),
      preview,
      swatches,
      nameInput,
      stepsBox,
    ),
    bottom,
  );
}
