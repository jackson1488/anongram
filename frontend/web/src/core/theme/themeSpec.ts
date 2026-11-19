export type ThemePolarity = 'DARK' | 'LIGHT';

export type ThemeTintMode = 'MONOCHROME' | 'AMOLED_ACCENT' | 'SOFT_OS' | 'FULL_OS';

export type BubbleColorStyle = 'GRADIENT' | 'TINTED' | 'OUTLINE';

export interface ThemeSpec {
  polarity: ThemePolarity;
  tintMode: ThemeTintMode;
  bubbleStyle: BubbleColorStyle;
  hue: number;
  saturation: number;
}

export interface SavedCustomTheme {
  id: string;
  name: string;
  spec: ThemeSpec;
}

export const TINT_MODES: ThemeTintMode[] = [
  'MONOCHROME',
  'AMOLED_ACCENT',
  'SOFT_OS',
  'FULL_OS',
];

export const BUBBLE_STYLES: BubbleColorStyle[] = [
  'GRADIENT',
  'TINTED',
  'OUTLINE',
];

export const DEFAULT_THEME_SPEC: ThemeSpec = {
  polarity: 'DARK',
  tintMode: 'AMOLED_ACCENT',
  bubbleStyle: 'TINTED',
  hue: 186,
  saturation: 0.92,
};

export function encodeThemeSpec(spec: ThemeSpec): string {
  const p = spec.polarity === 'DARK' ? 'D' : 'L';
  const t = Math.max(0, TINT_MODES.indexOf(spec.tintMode));
  const b = Math.max(0, BUBBLE_STYLES.indexOf(spec.bubbleStyle));
  const h = Math.floor(((spec.hue % 360) + 360) % 360);
  const s = Math.floor(Math.min(1, Math.max(0, spec.saturation)) * 100);
  return `AG1:${p}:${t}:${b}:${h}:${s}`;
}

export function decodeThemeSpec(raw: string): ThemeSpec | null {
  const trimmed = raw.trim();
  if (!trimmed.startsWith('AG1:')) return null;
  const parts = trimmed.slice(4).split(':');
  if (parts.length !== 5) return null;

  const pRaw = parts[0].toUpperCase();
  const polarity: ThemePolarity | null =
    pRaw === 'D' || pRaw === 'DARK'
      ? 'DARK'
      : pRaw === 'L' || pRaw === 'LIGHT'
        ? 'LIGHT'
        : null;
  if (!polarity) return null;

  const tIdx = Number.parseInt(parts[1], 10);
  const bIdx = Number.parseInt(parts[2], 10);
  const hRaw = Number.parseInt(parts[3], 10);
  const sRaw = Number.parseInt(parts[4], 10);
  if (
    !Number.isFinite(tIdx) ||
    !Number.isFinite(bIdx) ||
    !Number.isFinite(hRaw) ||
    !Number.isFinite(sRaw)
  ) {
    return null;
  }

  const tintMode = TINT_MODES[tIdx];
  const bubbleStyle = BUBBLE_STYLES[bIdx];
  if (!tintMode || !bubbleStyle) return null;

  return {
    polarity,
    tintMode,
    bubbleStyle,
    hue: Math.min(359, Math.max(0, hRaw)),
    saturation: Math.min(100, Math.max(0, sRaw)) / 100,
  };
}
