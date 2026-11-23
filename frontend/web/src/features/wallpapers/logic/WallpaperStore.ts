import { WallpaperStorage } from '../data/WallpaperStorage';
import { PRESET_WALLPAPERS, type Wallpaper } from '../model/Wallpaper';

const VAR = '--ag-chat-wallpaper';
let current: Wallpaper = WallpaperStorage.load() ?? PRESET_WALLPAPERS[0];
const listeners = new Set<(w: Wallpaper) => void>();

/** Applies wallpaper to the CSS var --ag-chat-wallpaper (use `background: var(--ag-chat-wallpaper, none)`). */
export function applyWallpaper(w: Wallpaper = current): void {
  const root = document.documentElement;
  if (w.kind === 'none') root.style.removeProperty(VAR);
  else if (w.kind === 'image') root.style.setProperty(VAR, `center / cover no-repeat url("${w.value}")`);
  else root.style.setProperty(VAR, w.value);
}

export const wallpaperStore = {
  get current(): Wallpaper {
    return current;
  },
  set(w: Wallpaper): void {
    current = w;
    WallpaperStorage.save(w);
    applyWallpaper(w);
    for (const fn of listeners) fn(w);
  },
  subscribe(fn: (w: Wallpaper) => void): () => void {
    listeners.add(fn);
    return () => listeners.delete(fn);
  },
};
