import { findSuggestions, isValidWord, normalizeSeed, validateMnemonic } from '../../../../core/crypto/bip39';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { authStore } from '../../authStore';
import { authT } from '../../i18n';

/** Restore screen: 12 inputs are created once; updates are in-place so focus is never lost. */
export function createRestoreAccountScreen(onBack: () => void): HTMLElement {
  const t = authT();
  const words = Array.from({ length: 12 }, () => '');
  let active = 0;

  const error = el('div', { class: 'ag-auth-error' });
  const sugRow = el('div', { class: 'ag-suggestions-row' });
  const inputs: HTMLInputElement[] = [];
  const slots: HTMLElement[] = [];

  const setActive = (idx: number) => {
    active = idx;
    slots.forEach((s, i) => s.classList.toggle('is-active', i === idx));
    renderSuggestions();
  };

  const applyAll = (list: string[]) => {
    list.slice(0, 12).forEach((w, i) => {
      words[i] = w;
      inputs[i].value = w;
    });
    error.textContent = '';
    renderSuggestions();
  };

  function renderSuggestions(): void {
    sugRow.replaceChildren(
      ...findSuggestions(words[active] || '', 6).map((sug) =>
        el(
          'button',
          {
            class: 'ag-chip',
            // keep focus on the input while tapping a chip
            onMouseDown: (e: Event) => e.preventDefault(),
            onClick: () => {
              words[active] = sug;
              inputs[active].value = sug;
              const next = Math.min(active + 1, 11);
              inputs[next].focus();
              setActive(next);
            },
          },
          sug,
        ),
      ),
    );
  }

  for (let idx = 0; idx < 12; idx++) {
    const inp = el('input', {
      type: 'text',
      placeholder: t.wordPlaceholder,
      autocomplete: 'off',
      autocapitalize: 'none',
      spellcheck: 'false',
      onFocus: () => setActive(idx),
      onKeydown: (e: Event) => {
        const k = e as KeyboardEvent;
        if ((k.key === ' ' || k.key === 'Enter') && idx < 11) {
          k.preventDefault();
          inputs[idx + 1].focus();
        }
      },
      onInput: (e: Event) => {
        const val = (e.target as HTMLInputElement).value;
        const split = normalizeSeed(val);
        if (split.length > 1) {
          applyAll(split);
          inputs[Math.min(split.length, 11)].focus();
          return;
        }
        words[idx] = val.trim().toLowerCase();
        error.textContent = '';
        renderSuggestions();
      },
    }) as HTMLInputElement;
    inputs.push(inp);
    slots.push(el('div', { class: `ag-restore-slot ${idx === 0 ? 'is-active' : ''}` }, el('span', null, `${idx + 1}.`), inp));
  }

  const submit = async () => {
    if (words.some((w) => !w.trim())) {
      error.textContent = t.restoreFillAll;
      return;
    }
    const bad = words.findIndex((w) => !isValidWord(w));
    if (bad >= 0) {
      error.textContent = t.restoreInvalidWord(bad + 1);
      return;
    }
    if (!(await validateMnemonic(words))) {
      error.textContent = t.restoreBadChecksum;
      return;
    }
    await authStore.restoreFromSeed(words.join(' '));
  };

  return el(
    'div',
    { class: 'ag-auth-container' },
    el(
      'div',
      { style: 'display:flex;flex-direction:column;gap:12px;' },
      el(
        'div',
        { class: 'ag-auth-toolbar' },
        el('button', { class: 'ag-icon-btn', onClick: onBack }, icons.back(22)),
        el('h2', null, t.restoreTitle),
      ),
      el('p', { class: 'ag-auth-desc' }, t.restoreDesc),
      el('div', { class: 'ag-restore-grid' }, ...slots),
      sugRow,
      error,
    ),
    el(
      'div',
      { class: 'ag-auth-bottom' },
      el(
        'div',
        { style: 'display:flex;gap:10px;' },
        el(
          'button',
          {
            class: 'ag-cta-outline',
            style: 'flex:1;',
            onClick: async () => {
              try {
                const pasted = normalizeSeed(await navigator.clipboard.readText());
                if (pasted.length >= 12) {
                  applyAll(pasted);
                  showToast(t.pasted);
                } else {
                  showToast(t.pasteShort);
                }
              } catch {
                showToast(t.pasteFail);
              }
            },
          },
          t.paste,
        ),
        el(
          'button',
          {
            class: 'ag-cta-outline',
            style: 'width:54px;',
            onClick: () => applyAll(Array.from({ length: 12 }, () => '')),
          },
          icons.trash(18),
        ),
      ),
      el('button', { class: 'ag-cta-gradient', onClick: submit }, t.restoreCta2),
    ),
  );
}
