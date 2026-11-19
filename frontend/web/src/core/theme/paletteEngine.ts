import type { ThemeSpec } from './themeSpec';

export interface RgbColor {
  r: number; // 0..1
  g: number; // 0..1
  b: number; // 0..1
  a: number; // 0..1
}

export interface AnonGramScheme {
  id: string;
  displayName: string;
  isDark: boolean;
  spec: ThemeSpec;
  accent: string;
  accentDim: string;
  accentSoft: string;
  onAccent: string;
  background: string;
  surface: string;
  card: string;
  cardBorder: string;
  islandBg: string;
  islandBorder: string;
  bannerBg: string;
  overlayScrim: string;
  inputBg: string;
  inputBorder: string;
  editorBg: string;
  editorSurface: string;
  emojiPanelBg: string;
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  divider: string;
  outgoingBubbleStart: string;
  outgoingBubbleEnd: string;
  outgoingBubbleBorder: string;
  outgoingText: string;
  incomingBubbleBg: string;
  incomingBubbleBorder: string;
  incomingText: string;
  replyStripIncoming: string;
  replyStripOutgoing: string;
  selectedBubbleBg: string;
  selectedGlow: string;
}

function clamp01(v: number): number {
  return Math.min(1, Math.max(0, v));
}

function rgb(r: number, g: number, b: number, a = 1): RgbColor {
  return { r: clamp01(r), g: clamp01(g), b: clamp01(b), a: clamp01(a) };
}

function withAlpha(c: RgbColor, a: number): RgbColor {
  return { ...c, a: clamp01(a) };
}

export function toCss(c: RgbColor): string {
  const r = Math.round(c.r * 255);
  const g = Math.round(c.g * 255);
  const b = Math.round(c.b * 255);
  if (c.a >= 0.999) {
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
  }
  return `rgba(${r}, ${g}, ${b}, ${Number(c.a.toFixed(3))})`;
}

function hueChannel(p: number, q: number, tRaw: number): number {
  let t = tRaw;
  if (t < 0) t += 1;
  if (t > 1) t -= 1;
  if (t < 1 / 6) return clamp01(p + (q - p) * 6 * t);
  if (t < 1 / 2) return clamp01(q);
  if (t < 2 / 3) return clamp01(p + (q - p) * (2 / 3 - t) * 6);
  return clamp01(p);
}

export function hslToRgb(h: number, s: number, l: number, alpha = 1): RgbColor {
  const hueNorm = (((h % 360) + 360) % 360) / 360;
  const sat = clamp01(s);
  const lig = clamp01(l);
  if (sat === 0) {
    return rgb(lig, lig, lig, alpha);
  }
  const q = lig < 0.5 ? lig * (1 + sat) : lig + sat - lig * sat;
  const p = 2 * lig - q;
  return rgb(
    hueChannel(p, q, hueNorm + 1 / 3),
    hueChannel(p, q, hueNorm),
    hueChannel(p, q, hueNorm - 1 / 3),
    alpha,
  );
}

function relativeLuminance(c: RgbColor): number {
  const lin = (v: number) =>
    v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  return 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b);
}

function contrastRatio(a: RgbColor, b: RgbColor): number {
  const la = relativeLuminance(a);
  const lb = relativeLuminance(b);
  const hi = Math.max(la, lb);
  const lo = Math.min(la, lb);
  return (hi + 0.05) / (lo + 0.05);
}

function bestOnColor(bg: RgbColor): RgbColor {
  const darkText = rgb(10 / 255, 12 / 255, 16 / 255);
  const lightText = rgb(1, 1, 1);
  return contrastRatio(bg, darkText) >= contrastRatio(bg, lightText)
    ? darkText
    : lightText;
}

function blendHalf(a: RgbColor, b: RgbColor): RgbColor {
  return rgb((a.r + b.r) * 0.5, (a.g + b.g) * 0.5, (a.b + b.b) * 0.5, 1);
}

export function buildScheme(
  id: string,
  displayName: string,
  spec: ThemeSpec,
): AnonGramScheme {
  const isDark = spec.polarity === 'DARK';
  const hue = ((spec.hue % 360) + 360) % 360;
  const rawSat = clamp01(spec.saturation);
  const isMono = spec.tintMode === 'MONOCHROME';
  const effSat = isMono ? 0 : rawSat;

  const bgSat =
    spec.tintMode === 'MONOCHROME'
      ? 0
      : spec.tintMode === 'AMOLED_ACCENT'
        ? effSat * 0.06
        : spec.tintMode === 'SOFT_OS'
          ? effSat * 0.24
          : effSat * 0.4;

  const cardSat =
    spec.tintMode === 'MONOCHROME'
      ? 0
      : spec.tintMode === 'AMOLED_ACCENT'
        ? effSat * 0.11
        : spec.tintMode === 'SOFT_OS'
          ? effSat * 0.32
          : effSat * 0.48;

  const bgL = isDark
    ? spec.tintMode === 'MONOCHROME'
      ? 0.02
      : spec.tintMode === 'AMOLED_ACCENT'
        ? 0.024
        : spec.tintMode === 'SOFT_OS'
          ? 0.055
          : 0.105
    : spec.tintMode === 'MONOCHROME'
      ? 0.94
      : spec.tintMode === 'AMOLED_ACCENT'
        ? 0.965
        : spec.tintMode === 'SOFT_OS'
          ? 0.935
          : 0.895;

  const surfaceL = isDark ? bgL + 0.032 : Math.min(0.985, bgL + 0.03);
  const cardL = isDark ? bgL + 0.052 : 0.992;
  const islandL = isDark ? bgL + 0.068 : 0.996;
  const incomingL = isDark ? bgL + 0.088 : 0.996;

  const background = hslToRgb(hue, bgSat, bgL);
  const surface = hslToRgb(hue, cardSat * 0.85, surfaceL);
  const card = hslToRgb(hue, cardSat, cardL);
  const islandBg = hslToRgb(hue, cardSat, islandL);
  const editorBg = hslToRgb(
    hue,
    bgSat,
    isDark ? bgL + 0.01 : Math.min(0.99, bgL + 0.02),
  );
  const editorSurface = hslToRgb(hue, cardSat, isDark ? bgL + 0.075 : 0.995);
  const emojiPanelBg = hslToRgb(
    hue,
    cardSat * 0.9,
    isDark ? bgL + 0.045 : Math.min(0.985, bgL + 0.04),
  );
  const bannerBg = hslToRgb(
    hue,
    cardSat,
    isDark ? bgL + 0.095 : 0.99,
    0.96,
  );
  const overlayScrim = isDark
    ? rgb(4 / 255, 5 / 255, 8 / 255, 0.8)
    : rgb(16 / 255, 20 / 255, 28 / 255, 0.53);

  const borderAlpha =
    isDark && spec.tintMode === 'FULL_OS'
      ? 0.24
      : isDark
        ? 0.14
        : spec.tintMode === 'FULL_OS'
          ? 0.22
          : 0.14;

  const cardBorder = isDark
    ? hslToRgb(hue, effSat * 0.7, 0.78, borderAlpha)
    : hslToRgb(hue, effSat * 0.55, 0.32, borderAlpha);

  const islandBorder = isDark
    ? hslToRgb(hue, effSat * 0.75, 0.8, Math.min(0.32, borderAlpha + 0.04))
    : hslToRgb(hue, effSat * 0.6, 0.3, Math.min(0.28, borderAlpha + 0.04));

  const accentSat = isMono ? 0 : Math.max(0.55, effSat);
  const accentL = isDark ? (isMono ? 0.93 : 0.59) : isMono ? 0.12 : 0.44;
  const accentDimL = isDark ? (isMono ? 0.72 : 0.48) : isMono ? 0.26 : 0.36;

  const accent = hslToRgb(hue, accentSat, accentL);
  const accentDim = hslToRgb((hue + 12) % 360, accentSat * 0.92, accentDimL);
  const accentSoft = withAlpha(accent, isDark ? 0.17 : 0.13);
  const onAccent = bestOnColor(accent);

  const textPrimary = isDark
    ? rgb(0xf6 / 255, 0xf8 / 255, 0xfc / 255)
    : rgb(0x10 / 255, 0x13 / 255, 0x1a / 255);
  const textSecondary = isDark
    ? hslToRgb(hue, bgSat * 0.45, 0.72)
    : hslToRgb(hue, bgSat * 0.55, 0.34);
  const textMuted = isDark
    ? hslToRgb(hue, bgSat * 0.4, 0.52)
    : hslToRgb(hue, bgSat * 0.45, 0.5);
  const divider = isDark
    ? rgb(1, 1, 1, 0.094)
    : rgb(0, 0, 0, 0.094);

  const incomingBubbleBg = hslToRgb(hue, cardSat, incomingL);
  const incomingBubbleBorder = isDark
    ? hslToRgb(hue, effSat * 0.6, 0.75, 0.13)
    : hslToRgb(hue, effSat * 0.5, 0.32, 0.15);
  const incomingText = textPrimary;

  let outgoingStart: RgbColor;
  let outgoingEnd: RgbColor;
  let outgoingBorder: RgbColor;
  let outgoingText: RgbColor;

  if (spec.bubbleStyle === 'GRADIENT') {
    outgoingStart = hslToRgb(
      hue,
      isMono ? 0 : Math.max(0.54, effSat),
      isDark ? (isMono ? 0.88 : 0.5) : isMono ? 0.14 : 0.46,
    );
    outgoingEnd = hslToRgb(
      (hue + 14) % 360,
      isMono ? 0 : Math.max(0.48, effSat),
      isDark ? (isMono ? 0.72 : 0.39) : isMono ? 0.24 : 0.38,
    );
    outgoingBorder = rgb(1, 1, 1, isDark ? 0.16 : 0.1);
    outgoingText = bestOnColor(blendHalf(outgoingStart, outgoingEnd));
  } else if (spec.bubbleStyle === 'TINTED') {
    const tintL = isDark
      ? spec.tintMode === 'FULL_OS'
        ? 0.28
        : spec.tintMode === 'MONOCHROME'
          ? 0.2
          : 0.23
      : 0.855;
    const tintSat = isMono ? 0 : Math.max(0.36, effSat * 0.68);
    outgoingStart = hslToRgb(hue, tintSat, tintL);
    outgoingEnd = hslToRgb(
      (hue + 8) % 360,
      tintSat * 0.9,
      Math.max(0.14, tintL - 0.03),
    );
    outgoingBorder = withAlpha(accent, isDark ? 0.44 : 0.38);
    outgoingText = textPrimary;
  } else {
    outgoingStart = islandBg;
    outgoingEnd = islandBg;
    outgoingBorder = withAlpha(accent, 0.86);
    outgoingText = textPrimary;
  }

  const replyStripIncoming = isDark
    ? hslToRgb(hue, cardSat * 0.8, bgL + 0.13)
    : hslToRgb(hue, cardSat * 0.6, 0.94);
  const replyStripOutgoing =
    spec.bubbleStyle === 'GRADIENT'
      ? withAlpha(outgoingText, 0.12)
      : withAlpha(accent, 0.14);

  const violetGlow = rgb(0x8b / 255, 0x5c / 255, 0xf6 / 255);
  const selectedGlow = isMono
    ? accent
    : Math.abs(hue - 262) < 35
      ? violetGlow
      : accent;
  const selectedBubbleBg = isDark
    ? hslToRgb(hue, isMono ? 0 : 0.45, 0.26)
    : hslToRgb(hue, isMono ? 0 : 0.48, 0.83);

  return {
    id,
    displayName,
    isDark,
    spec,
    accent: toCss(accent),
    accentDim: toCss(accentDim),
    accentSoft: toCss(accentSoft),
    onAccent: toCss(onAccent),
    background: toCss(background),
    surface: toCss(surface),
    card: toCss(card),
    cardBorder: toCss(cardBorder),
    islandBg: toCss(islandBg),
    islandBorder: toCss(islandBorder),
    bannerBg: toCss(bannerBg),
    overlayScrim: toCss(overlayScrim),
    inputBg: toCss(islandBg),
    inputBorder: toCss(islandBorder),
    editorBg: toCss(editorBg),
    editorSurface: toCss(editorSurface),
    emojiPanelBg: toCss(emojiPanelBg),
    textPrimary: toCss(textPrimary),
    textSecondary: toCss(textSecondary),
    textMuted: toCss(textMuted),
    divider: toCss(divider),
    outgoingBubbleStart: toCss(outgoingStart),
    outgoingBubbleEnd: toCss(outgoingEnd),
    outgoingBubbleBorder: toCss(outgoingBorder),
    outgoingText: toCss(outgoingText),
    incomingBubbleBg: toCss(incomingBubbleBg),
    incomingBubbleBorder: toCss(incomingBubbleBorder),
    incomingText: toCss(incomingText),
    replyStripIncoming: toCss(replyStripIncoming),
    replyStripOutgoing: toCss(replyStripOutgoing),
    selectedBubbleBg: toCss(selectedBubbleBg),
    selectedGlow: toCss(selectedGlow),
  };
}
