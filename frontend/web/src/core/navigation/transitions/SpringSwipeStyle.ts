import type { ITransitionStyle, LayerState } from './ITransitionStyle';
import type { NavigationTransitionConfig } from '../TransitionModels';

/** Port of SpringSwipeStyle.kt: flagship 1:1 swipe, card combos with spring landing. */
export const SpringSwipeStyle: ITransitionStyle = {
  type: 'SPRING_SWIPE',
  transform(l: LayerState, progress: number, isExit: boolean, config: NavigationTransitionConfig) {
    if (isExit && config.enablePredictiveBackCombo) {
      const edgeDir = config.swipeEdge === 1 ? -1 : 1;
      const cardScale = Math.min(1, Math.max(0.8, 1 - progress * 0.16));
      l.scaleX = l.scaleY = cardScale;
      l.cornerRadius = progress * 28;
      l.shadow = progress * 16;
      if (config.isPredictiveHolding) {
        l.translationX = edgeDir * progress * l.width * 0.08;
        l.alpha = 1;
      } else {
        l.translationX = edgeDir * progress * l.width;
        l.alpha = progress > 0.85 ? 1 - (progress - 0.85) / 0.15 : 1;
      }
    } else if (isExit && config.enableMiniCardCombo) {
      const cardScale = 1 - progress * 0.18;
      l.translationX =
        progress <= 0.65
          ? progress * l.width * 0.42
          : l.width * (0.273 + ((progress - 0.65) / 0.35) * 0.727);
      l.scaleX = l.scaleY = cardScale;
      l.cornerRadius = progress * 30;
      l.shadow = progress * 16;
      l.alpha = progress > 0.85 ? 1 - (progress - 0.85) / 0.15 : 1;
    } else {
      l.translationX = progress * l.width;
      l.shadow = isExit ? progress * 16 : 0;
    }
  },
};
