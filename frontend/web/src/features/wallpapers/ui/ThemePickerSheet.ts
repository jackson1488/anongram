import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { cleanImageFile } from '../data/MetadataCleaner';
import { WallpaperI18n } from '../i18n/WallpaperI18n';
import { wallpaperStore } from '../logic/WallpaperStore';
import { PRESET_WALLPAPERS, type Wallpaper } from '../model/Wallpaper';
import './wallpapers.css';

export function openWallpaperPicker(): void {
  const s = WallpaperI18n.current;
  const grid = el('div', { class: 'ag-wp-grid' });
  const file = el('input', { type: 'file', accept: 'image/*', style: 'display:none' });
  const close = () => {
    document.removeEventListener('keydown', onKey);
    overlay.remove();
  };
  const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close(); };
  const overlay = el(
    'div',
    { class: 'ag-wp-overlay', onClick: (e: Event) => { if (e.target === overlay) close(); } },
    el(
      'div',
      { class: 'ag-wp-sheet' },
      el('div', { class: 'ag-wp-top' }, el('span', null, s.title), el('button', { class: 'ag-icon-btn', title: s.close, onClick: close }, icons.close(20))),
      grid,
      el('button', { class: 'ag-wp-btn', onClick: () => file.click() }, s.upload),
      file,
    ),
  );
  const render = () => {
    grid.replaceChildren();
    const cur = wallpaperStore.current;
    const all: Wallpaper[] = [...PRESET_WALLPAPERS];
    if (cur.kind === 'image') all.push(cur);
    for (const w of all) {
      const b = el('button', { class: `ag-wp-item${cur.id === w.id ? ' is-active' : ''}`, onClick: () => { wallpaperStore.set(w); render(); } }, w.kind === 'none' ? s.none : null);
      if (w.kind === 'css') b.style.background = w.value;
      if (w.kind === 'image') b.style.backgroundImage = `url("${w.value}")`;
      grid.appendChild(b);
    }
  };
  file.addEventListener('change', async () => {
    const f = file.files?.[0];
    if (!f) return;
    const value = await cleanImageFile(f);
    wallpaperStore.set({ id: `img-${Date.now()}`, kind: 'image', value });
    render();
  });
  document.addEventListener('keydown', onKey);
  render();
  document.body.appendChild(overlay);
}
