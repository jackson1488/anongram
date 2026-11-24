import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';

export interface ComposerOptions {
  initialText: string;
  onTextChange: (text: string) => void;
  onSend: (text: string) => void;
  onVoice: () => void;
  onToggleAttach: () => void;
  onToggleEmoji: () => void;
}

export interface ComposerHandle {
  root: HTMLElement;
  getText: () => string;
  setText: (text: string) => void;
  insertText: (text: string) => void;
  focus: () => void;
}

/** Composer is built once per chat and updated in place, so the input never loses focus. */
export function createComposer(o: ComposerOptions): ComposerHandle {
  const t = i18nStore.t;

  const input = el('input', {
    type: 'text',
    class: 'ag-msg-input',
    placeholder: t.input.placeholder,
    value: o.initialText,
    autocomplete: 'off',
    enterkeyhint: 'send',
    'aria-label': t.input.placeholder,
  }) as HTMLInputElement;
  input.value = o.initialText;

  const attachBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn ag-composer__attach',
      title: t.input.attach,
      'aria-label': t.input.attach,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => o.onToggleAttach(),
    },
    icons.attach(20),
  ) as HTMLButtonElement;

  const emojiBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn ag-composer__emoji',
      title: t.input.emoji,
      'aria-label': t.input.emoji,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => o.onToggleEmoji(),
    },
    icons.smile(20),
  ) as HTMLButtonElement;

  const sendIcon = icons.send(20);
  const micIcon = icons.mic(20);

  const actionBtn = el('button', {
    type: 'button',
    class: 'ag-send-fab',
    title: t.input.voiceRecord,
    'aria-label': t.input.voiceRecord,
    onMouseDown: (e: Event) => e.preventDefault(),
  }) as HTMLButtonElement;

  const pill = el('div', { class: 'ag-composer__pill' }, attachBtn, input, emojiBtn);
  const root = el('div', { class: 'ag-composer' }, pill, actionBtn);

  let lastHasText: boolean | null = null;
  const syncState = () => {
    const hasText = input.value.trim().length > 0;
    root.classList.toggle('is-has-text', hasText);
    if (hasText !== lastHasText) {
      lastHasText = hasText;
      const cur = i18nStore.t.input;
      const label = hasText ? cur.send : cur.voiceRecord;
      actionBtn.title = label;
      actionBtn.setAttribute('aria-label', label);
      actionBtn.replaceChildren(hasText ? sendIcon : micIcon);
    }
  };
  syncState();

  const submit = () => {
    const text = input.value;
    if (!text.trim()) return;
    input.value = '';
    syncState();
    o.onSend(text);
    input.focus();
  };

  input.addEventListener('input', () => {
    syncState();
    o.onTextChange(input.value);
  });
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      submit();
    }
  });
  actionBtn.addEventListener('click', () => {
    if (input.value.trim()) submit();
    else o.onVoice();
  });

  return {
    root,
    getText: () => input.value,
    setText: (text) => {
      input.value = text;
      syncState();
    },
    insertText: (text) => {
      const s = input.selectionStart ?? input.value.length;
      const e = input.selectionEnd ?? s;
      input.value = input.value.slice(0, s) + text + input.value.slice(e);
      input.setSelectionRange(s + text.length, s + text.length);
      syncState();
      o.onTextChange(input.value);
      input.focus();
    },
    focus: () => input.focus(),
  };
}
