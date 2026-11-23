export type WallpaperKind = 'none' | 'css' | 'image';

export interface Wallpaper {
  id: string;
  kind: WallpaperKind;
  /** CSS `background` value for 'css'; data URL for 'image'. */
  value: string;
}

export const PRESET_WALLPAPERS: Wallpaper[] = [
  { id: 'none', kind: 'none', value: '' },
  { id: 'dusk', kind: 'css', value: 'linear-gradient(160deg,#1f2a44,#3b2a5a)' },
  { id: 'forest', kind: 'css', value: 'linear-gradient(160deg,#10281f,#2d5a3f)' },
  { id: 'sunset', kind: 'css', value: 'linear-gradient(160deg,#4a2330,#c2703d)' },
  { id: 'ocean', kind: 'css', value: 'linear-gradient(160deg,#0b2a45,#1d7a8c)' },
  { id: 'graphite', kind: 'css', value: 'linear-gradient(160deg,#18181b,#34343a)' },
];
