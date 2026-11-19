import { kvStore } from '../storage/kvStore';
import { buildScheme, type AnonGramScheme } from './paletteEngine';
import { getSchemeById } from './presets';
import {
  encodeThemeSpec,
  type SavedCustomTheme,
  type ThemeSpec,
} from './themeSpec';

type ThemeListener = (scheme: AnonGramScheme) => void;

const KEY_THEME_ID = 'theme.id';
const KEY_CUSTOM_THEMES = 'theme.custom_list';
const KEY_TEXT_SIZE = 'chat.text_size';

function applyCssVars(scheme: AnonGramScheme): void {
  const root = document.documentElement.style;
  root.setProperty('--ag-bg', scheme.background);
  root.setProperty('--ag-surface', scheme.surface);
  root.setProperty('--ag-card', scheme.card);
  root.setProperty('--ag-card-border', scheme.cardBorder);
  root.setProperty('--ag-island-bg', scheme.islandBg);
  root.setProperty('--ag-island-border', scheme.islandBorder);
  root.setProperty('--ag-banner-bg', scheme.bannerBg);
  root.setProperty('--ag-overlay-scrim', scheme.overlayScrim);
  root.setProperty('--ag-input-bg', scheme.inputBg);
  root.setProperty('--ag-input-border', scheme.inputBorder);
  root.setProperty('--ag-editor-bg', scheme.editorBg);
  root.setProperty('--ag-editor-surface', scheme.editorSurface);
  root.setProperty('--ag-emoji-bg', scheme.emojiPanelBg);
  root.setProperty('--ag-accent', scheme.accent);
  root.setProperty('--ag-accent-dim', scheme.accentDim);
  root.setProperty('--ag-accent-soft', scheme.accentSoft);
  root.setProperty('--ag-on-accent', scheme.onAccent);
  root.setProperty('--ag-text-primary', scheme.textPrimary);
  root.setProperty('--ag-text-secondary', scheme.textSecondary);
  root.setProperty('--ag-text-muted', scheme.textMuted);
  root.setProperty('--ag-divider', scheme.divider);
  root.setProperty('--ag-out-start', scheme.outgoingBubbleStart);
  root.setProperty('--ag-out-end', scheme.outgoingBubbleEnd);
  root.setProperty('--ag-out-border', scheme.outgoingBubbleBorder);
  root.setProperty('--ag-out-text', scheme.outgoingText);
  root.setProperty('--ag-in-bg', scheme.incomingBubbleBg);
  root.setProperty('--ag-in-border', scheme.incomingBubbleBorder);
  root.setProperty('--ag-in-text', scheme.incomingText);
  root.setProperty('--ag-reply-in', scheme.replyStripIncoming);
  root.setProperty('--ag-reply-out', scheme.replyStripOutgoing);
  root.setProperty('--ag-selected-bg', scheme.selectedBubbleBg);
  root.setProperty('--ag-selected-glow', scheme.selectedGlow);
  root.setProperty('color-scheme', scheme.isDark ? 'dark' : 'light');

  document.body.style.background = scheme.background;
  document.body.style.color = scheme.textPrimary;

  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) {
    meta.setAttribute('content', scheme.background);
  }
}

class ThemeStore {
  private current: AnonGramScheme;
  private customThemes: SavedCustomTheme[];
  private textSizePx: number;
  private listeners = new Set<ThemeListener>();

  constructor() {
    const savedId = kvStore.getString(KEY_THEME_ID, 'cyber_stealth');
    this.current = getSchemeById(savedId);
    this.customThemes = kvStore.getJson<SavedCustomTheme[]>(KEY_CUSTOM_THEMES, []);
    this.textSizePx = Math.min(30, Math.max(11, kvStore.getNumber(KEY_TEXT_SIZE, 15)));
    applyCssVars(this.current);
    document.documentElement.style.setProperty('--ag-msg-size', `${this.textSizePx}px`);
  }

  get scheme(): AnonGramScheme {
    return this.current;
  }

  get savedCustomThemes(): SavedCustomTheme[] {
    return this.customThemes;
  }

  get messageTextSize(): number {
    return this.textSizePx;
  }

  subscribe(fn: ThemeListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  selectPreset(id: string): void {
    this.current = getSchemeById(id);
    kvStore.setString(KEY_THEME_ID, this.current.id);
    applyCssVars(this.current);
    this.emit();
  }

  applyCustomSpec(spec: ThemeSpec, displayName = 'Custom'): void {
    const code = encodeThemeSpec(spec);
    const id = `custom:${code}`;
    this.current = buildScheme(id, displayName, spec);
    kvStore.setString(KEY_THEME_ID, id);
    applyCssVars(this.current);
    this.emit();
  }

  saveCustomTheme(name: string, spec: ThemeSpec): SavedCustomTheme {
    const code = encodeThemeSpec(spec);
    const item: SavedCustomTheme = {
      id: `saved_${Date.now()}`,
      name: name.trim() || code,
      spec,
    };
    this.customThemes = [item, ...this.customThemes.filter((t) => encodeThemeSpec(t.spec) !== code)];
    kvStore.setJson(KEY_CUSTOM_THEMES, this.customThemes);
    this.applyCustomSpec(spec, item.name);
    return item;
  }

  deleteCustomTheme(id: string): void {
    this.customThemes = this.customThemes.filter((t) => t.id !== id);
    kvStore.setJson(KEY_CUSTOM_THEMES, this.customThemes);
    this.emit();
  }

  setMessageTextSize(size: number): void {
    this.textSizePx = Math.min(30, Math.max(11, Math.round(size)));
    kvStore.setNumber(KEY_TEXT_SIZE, this.textSizePx);
    document.documentElement.style.setProperty('--ag-msg-size', `${this.textSizePx}px`);
    this.emit();
  }

  private emit(): void {
    for (const fn of this.listeners) {
      fn(this.current);
    }
  }
}

export const themeStore = new ThemeStore();
