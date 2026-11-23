import type { ITransitionStyle } from './ITransitionStyle';

/** Port of InstantStyle.kt: 0 ms turbo mode, no transform. */
export const InstantStyle: ITransitionStyle = {
  type: 'INSTANT',
  transform() {},
};
