import { kvStore } from '../../../core/storage/kvStore';

const KEY_WALLPAPER = 'chat.wallpaper';

/**
 * Wallpaper hook (Android ChatWallpaper). The wallpaper layer is a dedicated element behind messages;
 * it paints `var(--ag-chat-wallpaper)` (set by theme/settings) or a CSS image saved under `chat.wallpaper`.
 */
export function createChatWallpaper(): HTMLElement {
  const layer = document.createElement('div');
  layer.className = 'ag-cs-wallpaper';
  applyChatWallpaper(layer);
  return layer;
}

export function applyChatWallpaper(layer: HTMLElement): void {
  const saved = kvStore.getString(KEY_WALLPAPER, '');
  if (saved) layer.style.backgroundImage = saved.startsWith('url(') ? saved : `url("${saved}")`;
  else layer.style.removeProperty('background-image');
}
