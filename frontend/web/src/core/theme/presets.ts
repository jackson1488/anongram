import { buildScheme, type AnonGramScheme } from './paletteEngine';
import {
  decodeThemeSpec,
  type BubbleColorStyle,
  type ThemePolarity,
  type ThemeTintMode,
} from './themeSpec';

function preset(
  id: string,
  name: string,
  polarity: ThemePolarity,
  tintMode: ThemeTintMode,
  bubbleStyle: BubbleColorStyle,
  hue: number,
  saturation = 0.86,
): AnonGramScheme {
  return buildScheme(id, name, {
    polarity,
    tintMode,
    bubbleStyle,
    hue,
    saturation,
  });
}

export const ANONGRAM_PRESETS: AnonGramScheme[] = [
  preset('cyber_stealth', 'Cyber Stealth', 'DARK', 'AMOLED_ACCENT', 'TINTED', 186, 0.96),
  preset('monochrome_noir', 'Ч/Б Noir', 'DARK', 'MONOCHROME', 'OUTLINE', 0, 0),
  preset('monochrome_paper', 'Б/Ч Paper', 'LIGHT', 'MONOCHROME', 'GRADIENT', 0, 0),
  preset('sakura_os_dark', 'Розовая ОС (Тёмная)', 'DARK', 'FULL_OS', 'GRADIENT', 336, 0.8),
  preset('sakura_os_light', 'Розовая ОС (Светлая)', 'LIGHT', 'FULL_OS', 'TINTED', 338, 0.74),
  preset('telegram_night', 'Telegram Ночь', 'DARK', 'FULL_OS', 'TINTED', 210, 0.72),
  preset('telegram_day', 'Telegram День', 'LIGHT', 'SOFT_OS', 'GRADIENT', 206, 0.88),
  preset('violet_ghost', 'Violet Ghost', 'DARK', 'SOFT_OS', 'TINTED', 260, 0.88),
  preset('neon_rose', 'Neon Rose', 'DARK', 'SOFT_OS', 'GRADIENT', 338, 0.84),
  preset('cobalt_os_dark', 'Кобальт ОС', 'DARK', 'FULL_OS', 'GRADIENT', 214, 0.88),
  preset('emerald_os_dark', 'Изумруд ОС', 'DARK', 'FULL_OS', 'GRADIENT', 154, 0.78),
  preset('mint_day', 'Мята День', 'LIGHT', 'SOFT_OS', 'TINTED', 166, 0.76),
  preset('lavender_day', 'Лаванда День', 'LIGHT', 'FULL_OS', 'GRADIENT', 265, 0.78),
  preset('gold_carbon', 'Золото на Чёрном', 'DARK', 'AMOLED_ACCENT', 'OUTLINE', 45, 0.94),
  preset('crimson_blood', 'Вишня ОС', 'DARK', 'FULL_OS', 'TINTED', 352, 0.76),
  preset('peach_daylight', 'Персик День', 'LIGHT', 'FULL_OS', 'GRADIENT', 18, 0.84),
  preset('arctic_frost', 'Арктика Свет', 'LIGHT', 'AMOLED_ACCENT', 'OUTLINE', 196, 0.86),
  preset('emerald_night', 'Emerald Night', 'DARK', 'AMOLED_ACCENT', 'TINTED', 158, 0.82),
  preset('solar_amber', 'Solar Amber', 'DARK', 'AMOLED_ACCENT', 'TINTED', 38, 0.92),
  preset('ruby_crimson', 'Ruby Crimson', 'DARK', 'SOFT_OS', 'TINTED', 0, 0.84),
  preset('electric_indigo', 'Electric Indigo', 'DARK', 'SOFT_OS', 'GRADIENT', 238, 0.86),
  preset('mint_aurora', 'Mint Aurora', 'DARK', 'AMOLED_ACCENT', 'TINTED', 173, 0.8),
  preset('sunset_coral', 'Sunset Coral', 'DARK', 'SOFT_OS', 'GRADIENT', 8, 0.9),
  preset('deep_amethyst', 'Deep Amethyst', 'DARK', 'FULL_OS', 'TINTED', 272, 0.82),
];

export function getSchemeById(id: string): AnonGramScheme {
  if (id.startsWith('custom:')) {
    const spec = decodeThemeSpec(id.slice('custom:'.length));
    if (spec) {
      return buildScheme(id, 'Custom', spec);
    }
  }
  return ANONGRAM_PRESETS.find((p) => p.id === id) ?? ANONGRAM_PRESETS[0];
}
