import { kvStore } from '../../../core/storage/kvStore';
import type { Wallpaper } from '../model/Wallpaper';

const KEY = 'wallpaper.current';

export const WallpaperStorage = {
  load(): Wallpaper | null {
    try {
      const raw = kvStore.getString(KEY, '');
      return raw ? (JSON.parse(raw) as Wallpaper) : null;
    } catch {
      return null;
    }
  },
  save(w: Wallpaper): void {
    try {
      kvStore.setString(KEY, JSON.stringify(w));
    } catch {
      /* quota exceeded */
    }
  },
};
