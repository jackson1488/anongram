import { kvStore } from '../../../core/storage/kvStore';
import { themeStore } from '../../../core/theme/themeStore';
import { wallpaperStore } from '../../wallpapers';

const KEY_WALLPAPER = 'chat.wallpaper';

function hasCustomWallpaperVar(): boolean {
  const inlineVal = document.documentElement.style.getPropertyValue('--ag-chat-wallpaper').trim();
  const cssVal =
    inlineVal || getComputedStyle(document.documentElement).getPropertyValue('--ag-chat-wallpaper').trim();
  return Boolean(cssVal && cssVal !== 'none');
}

/**
 * Wallpaper hook (Android ChatWallpaper). The wallpaper layer is a dedicated element behind messages;
 * it paints `var(--ag-chat-wallpaper)` (set by theme/settings) or a CSS image saved under `chat.wallpaper`.
 * When no custom wallpaper is selected (`--ag-chat-wallpaper` is `none`), applies `.is-default-pattern`.
 */
export function createChatWallpaper(): HTMLElement {
  const layer = document.createElement('div');
  layer.className = 'ag-cs-wallpaper';
  const sync = () => applyChatWallpaper(layer);
  sync();
  const unsubWallpaper = wallpaperStore.subscribe(sync);
  const unsubTheme = themeStore.subscribe(sync);
  (layer as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubWallpaper();
    unsubTheme();
  };
  return layer;
}

export function applyChatWallpaper(layer: HTMLElement): void {
  const saved = kvStore.getString(KEY_WALLPAPER, '');
  if (saved) {
    layer.style.backgroundImage = saved.startsWith('url(') ? saved : `url("${saved}")`;
    layer.classList.remove('is-default-pattern');
    return;
  }
  layer.style.removeProperty('background-image');
  const isDefault = !hasCustomWallpaperVar() && wallpaperStore.current.kind === 'none';
  layer.classList.toggle('is-default-pattern', isDefault);
}
