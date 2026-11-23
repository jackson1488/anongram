/** Port of core/navigation/TransitionModels.kt */
export type TransitionType =
  | 'SPRING_SWIPE'
  | 'CUPERTINO_PARALLAX'
  | 'REAL_BOOK_3D'
  | 'CUBE_ROTATION_3D'
  | 'ROTATION_SPIN'
  | 'CASCADE'
  | 'WINDMILL'
  | 'PARTICLE_DISSOLVE'
  | 'CARD_STACK_PEEK'
  | 'PREDICTIVE_BACK'
  | 'INSTANT';

export function isBidirectional(type: TransitionType): boolean {
  switch (type) {
    case 'REAL_BOOK_3D':
    case 'CUBE_ROTATION_3D':
    case 'CASCADE':
    case 'WINDMILL':
    case 'PARTICLE_DISSOLVE':
      return true;
    default:
      return false;
  }
}

export interface NavigationTransitionConfig {
  transitionType: TransitionType;
  speedMultiplier: number;
  baseDurationMs: number;
  springStiffness: number;
  springDamping: number;
  enablePredictiveBackCombo: boolean;
  enableMiniCardCombo: boolean;
  enableBaseSmoothEnter: boolean;
  enableEnterAnimation: boolean;
  enableExitAnimation: boolean;
  allowFullWidthSwipe: boolean;
  swipeEdge: number;
  isPredictiveHolding: boolean;
  isSystemGestureActive: boolean;
}

export const defaultNavigationConfig: NavigationTransitionConfig = {
  transitionType: 'SPRING_SWIPE',
  speedMultiplier: 1,
  baseDurationMs: 340,
  springStiffness: 320,
  springDamping: 0.76,
  enablePredictiveBackCombo: false,
  enableMiniCardCombo: false,
  enableBaseSmoothEnter: true,
  enableEnterAnimation: true,
  enableExitAnimation: true,
  allowFullWidthSwipe: true,
  swipeEdge: 0,
  isPredictiveHolding: false,
  isSystemGestureActive: false,
};

export function effectiveDurationMs(c: NavigationTransitionConfig): number {
  if (c.transitionType === 'INSTANT') return 0;
  let base = c.baseDurationMs;
  switch (c.transitionType) {
    case 'PARTICLE_DISSOLVE':
      base = 540;
      break;
    case 'CASCADE':
    case 'WINDMILL':
      base = 420;
      break;
    case 'REAL_BOOK_3D':
    case 'CUBE_ROTATION_3D':
      base = 380;
      break;
  }
  const m = Math.min(2.5, Math.max(0.4, c.speedMultiplier));
  return Math.min(1400, Math.max(60, Math.floor(base / m)));
}
