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
  }) as HTMLInputElement;
  input.value = o.initialText;

  const fab = el('button', { class: 'ag-send-fab', title: t.input.send, onMouseDown: (e: Event) => e.preventDefault() }) as HTMLButtonElement;
  const updateFab = () => {
    fab.replaceChildren(input.value.trim() ? icons.send(18) : icons.mic(18));
  };
  updateFab();

  const submit = () => {
    const text = input.value;
    if (!text.trim()) return;
    input.value = '';
    updateFab();
    o.onSend(text);
    input.focus();
  };

  input.addEventListener('input', () => {
    updateFab();
    o.onTextChange(input.value);
  });
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  });
  fab.addEventListener('click', () => {
    if (input.value.trim()) submit();
    else o.onVoice();
  });

  const root = el(
    'div',
    { class: 'ag-composer' },
    el('button', { class: 'ag-icon-btn', title: t.input.attach, onMouseDown: (e: Event) => e.preventDefault(), onClick: o.onToggleAttach }, icons.attach(20)),
    el(
      'div',
      { class: 'ag-composer__pill' },
      input,
      el('button', { class: 'ag-icon-btn', title: t.input.emoji, onMouseDown: (e: Event) => e.preventDefault(), onClick: o.onToggleEmoji }, icons.smile(20)),
    ),
    fab,
  );

  return {
    root,
    getText: () => input.value,
    setText: (text) => {
      input.value = text;
      updateFab();
    },
    insertText: (text) => {
      const s = input.selectionStart ?? input.value.length;
      const e = input.selectionEnd ?? s;
      input.value = input.value.slice(0, s) + text + input.value.slice(e);
      input.setSelectionRange(s + text.length, s + text.length);
      updateFab();
      o.onTextChange(input.value);
      input.focus();
    },
    focus: () => input.focus(),
  };
}
