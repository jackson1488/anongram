import type { NavigationTransitionConfig, TransitionType } from '../TransitionModels';

/** Mutable "graphics layer" equivalent applied to the element as CSS. */
export interface LayerState {
  translationX: number;
  translationY: number;
  scaleX: number;
  scaleY: number;
  rotationZ: number;
  alpha: number;
  /** corner radius in px, 0 = none */
  cornerRadius: number;
  shadow: number;
  width: number;
  height: number;
}

export interface ITransitionStyle {
  readonly type: TransitionType;
  transform(layer: LayerState, progress: number, isExit: boolean, config: NavigationTransitionConfig): void;
}

export function newLayer(width: number, height: number): LayerState {
  return {
    translationX: 0,
    translationY: 0,
    scaleX: 1,
    scaleY: 1,
    rotationZ: 0,
    alpha: 1,
    cornerRadius: 0,
    shadow: 0,
    width,
    height,
  };
}
