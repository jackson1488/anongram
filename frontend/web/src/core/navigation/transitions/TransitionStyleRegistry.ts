import type { TransitionType } from '../TransitionModels';
import { CupertinoParallaxStyle } from './CupertinoParallaxStyle';
import type { ITransitionStyle } from './ITransitionStyle';
import { InstantStyle } from './InstantStyle';
import { SpringSwipeStyle } from './SpringSwipeStyle';

/**
 * Port of TransitionStyleRegistry.kt. Web implements the slide-family styles;
 * the exotic 3D/particle styles fall back to SPRING_SWIPE.
 */
export const TransitionStyleRegistry = {
  resolve(type: TransitionType): ITransitionStyle {
    switch (type) {
      case 'CUPERTINO_PARALLAX':
        return CupertinoParallaxStyle;
      case 'INSTANT':
        return InstantStyle;
      default:
        return SpringSwipeStyle;
    }
  },
};
