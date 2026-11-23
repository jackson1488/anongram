import { el } from '../../../../core/ui/dom';

const EMOJIS = ['👍', '🔥', '❤️', '😂', '😮', '😢', '🎉', '🛡️', '⚡', '🕶️', '🔐', '💎', '🚀', '✨', '👌', '🤝'];
const STICKERS = ['🦊', '🐉', '🛸', '🕶️', '⚡', '🔐', '💎', '🌌'];

export function createEmojiPanel(onEmoji: (em: string) => void, onSticker: (st: string) => void): HTMLElement {
  return el(
    'div',
    { class: 'ag-drawer-panel' },
    el(
      'div',
      { class: 'ag-stickers-row' },
      ...STICKERS.map((st) => el('button', { class: 'ag-sticker-btn', onClick: () => onSticker(st) }, st)),
    ),
    el(
      'div',
      { class: 'ag-emoji-grid' },
      ...EMOJIS.map((em) =>
        el(
          'button',
          {
            class: 'ag-emoji-btn',
            // keep input focus
            onMouseDown: (e: Event) => e.preventDefault(),
            onClick: () => onEmoji(em),
          },
          em,
        ),
      ),
    ),
  );
}
