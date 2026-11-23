import { i18nStore } from '../../../core/i18n/i18nStore';
import type { TransitionType } from '../../../core/navigation/TransitionModels';
import { clear, el } from '../../../core/ui/dom';
import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';
import { settingsStore, type AnimationSpeedOption } from '../settingsStore';

const SPEEDS: AnimationSpeedOption[] = ['0.5x', '1.0x', '1.5x', '2.0x'];
const TRANSITIONS: TransitionType[] = [
  'SPRING_SWIPE',
  'CUPERTINO_PARALLAX',
  'CARD_STACK_PEEK',
  'CUBE_ROTATION_3D',
  'REAL_BOOK_3D',
  'INSTANT',
];

export function createAnimationsSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    root.appendChild(
      createSettingsToggleGroup([
        [s.animationsEnabled, 'animationsEnabled'],
        [s.messageAnimations, 'messageAnimations'],
        [s.screenTransitionsEnabled, 'screenTransitionsEnabled'],
        [s.springPhysicsEnabled, 'springPhysicsEnabled'],
        [s.reduceMotion, 'reduceMotion'],
      ]),
    );

    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, s.animationSpeedTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...SPEEDS.map((spd) =>
            el(
              'button',
              {
                class: `ag-chip ${prefs.animationSpeed === spd ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('animationSpeed', spd),
              },
              spd,
            ),
          ),
        ),
        el('div', { class: 'ag-section-label' }, s.transitionStyleTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...TRANSITIONS.map((tr) =>
            el(
              'button',
              {
                class: `ag-chip ${prefs.transitionType === tr ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('transitionType', tr),
              },
              s.transitionStyles[tr],
            ),
          ),
        ),
      ),
    );
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
