import type { ITransitionStyle, LayerState } from './ITransitionStyle';

/** Port of CupertinoParallaxStyle.kt */
export const CupertinoParallaxStyle: ITransitionStyle = {
  type: 'CUPERTINO_PARALLAX',
  transform(l: LayerState, progress: number) {
    l.translationX = progress * l.width;
  },
};
