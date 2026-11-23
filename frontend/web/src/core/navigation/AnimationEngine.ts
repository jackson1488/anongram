import {
  animateSpring,
  animateTween,
  PhysicsSprings,
  type AnimationHandle,
} from './PhysicsSprings';
import {
  effectiveDurationMs,
  isBidirectional,
  type NavigationTransitionConfig,
  type TransitionType,
} from './TransitionModels';
import { newLayer } from './transitions/ITransitionStyle';
import { TransitionStyleRegistry } from './transitions/TransitionStyleRegistry';

/**
 * Port of core/navigation/AnimationEngine.kt.
 * A TransitionScreen wraps an overlay element; progress 0 = fully shown, 1 = fully off-screen.
 */
export interface TransitionScreen {
  el: HTMLElement;
  progress: number;
  isExit: boolean;
  setProgress(p: number): void;
  setExiting(v: boolean): void;
  animation: AnimationHandle | null;
  config: () => NavigationTransitionConfig;
}

export function applyAnonGramTransition(
  el: HTMLElement,
  progress: number,
  isExit: boolean,
  config: NavigationTransitionConfig,
): void {
  const w = el.clientWidth || window.innerWidth;
  const h = el.clientHeight || window.innerHeight;
  if (config.transitionType === 'INSTANT' || (isExit && !config.enableExitAnimation)) {
    clearLayer(el);
    return;
  }
  if (!isExit && (!isBidirectional(config.transitionType) || !config.enableEnterAnimation)) {
    if (!config.enableBaseSmoothEnter) {
      clearLayer(el);
      return;
    }
    el.style.transform = progress === 0 ? '' : `translate3d(${progress * w}px,0,0)`;
    return;
  }
  let effective: TransitionType = config.transitionType;
  if (
    isExit &&
    config.isSystemGestureActive &&
    (effective === 'SPRING_SWIPE' || effective === 'CUPERTINO_PARALLAX')
  ) {
    if (config.enablePredictiveBackCombo) effective = 'PREDICTIVE_BACK';
    else if (config.enableMiniCardCombo) effective = 'CARD_STACK_PEEK';
  }
  const layer = newLayer(w, h);
  TransitionStyleRegistry.resolve(effective).transform(layer, progress, isExit, config);
  const hasCardCombo = config.enableMiniCardCombo || config.enablePredictiveBackCombo;
  if (hasCardCombo && isExit && effective !== 'SPRING_SWIPE' && effective !== 'CARD_STACK_PEEK' && effective !== 'PREDICTIVE_BACK') {
    const cs = Math.min(1, Math.max(0.82, 1 - progress * 0.14));
    layer.scaleX *= cs;
    layer.scaleY *= cs;
    layer.cornerRadius = progress * 28;
    layer.shadow = progress * 16;
  }
  el.style.transform =
    `translate3d(${layer.translationX}px,${layer.translationY}px,0) ` +
    `rotate(${layer.rotationZ}deg) scale(${layer.scaleX},${layer.scaleY})`;
  el.style.opacity = String(layer.alpha);
  el.style.borderRadius = layer.cornerRadius > 0.5 ? `${layer.cornerRadius}px` : '';
  el.style.overflow = layer.cornerRadius > 0.5 ? 'hidden' : '';
  el.style.boxShadow = layer.shadow > 0.1 ? `0 0 ${layer.shadow * 1.5}px rgba(0,0,0,0.38)` : '';
}

function clearLayer(el: HTMLElement): void {
  el.style.transform = '';
  el.style.opacity = '';
  el.style.borderRadius = '';
  el.style.overflow = '';
  el.style.boxShadow = '';
}

/** AnonGramTransitionScreen: container whose layer is driven by `progress`. */
export function createTransitionScreen(
  content: HTMLElement,
  config: () => NavigationTransitionConfig,
  className = '',
): TransitionScreen {
  const el = document.createElement('div');
  el.className = `ag-transition-screen ${className}`.trim();
  el.appendChild(content);
  const screen: TransitionScreen = {
    el,
    progress: 0,
    isExit: false,
    animation: null,
    config,
    setProgress(p) {
      screen.progress = p;
      applyAnonGramTransition(el, p, screen.isExit, config());
    },
    setExiting(v) {
      screen.isExit = v;
    },
  };
  return screen;
}

function runAnimation(
  screen: TransitionScreen,
  to: number,
  cfg: NavigationTransitionConfig,
): Promise<void> {
  screen.animation?.cancel();
  const from = screen.progress;
  const onFrame = (v: number) => screen.setProgress(v);
  const handle =
    cfg.transitionType === 'SPRING_SWIPE'
      ? animateSpring(
          from,
          to,
          PhysicsSprings.screenSpringSpec(cfg.springDamping, cfg.springStiffness * cfg.speedMultiplier),
          onFrame,
        )
      : animateTween(from, to, effectiveDurationMs(cfg), onFrame);
  screen.animation = handle;
  return handle.finished.then(() => {
    if (screen.animation === handle) screen.animation = null;
  });
}

/** Opening: place the screen under the enter animation (progress 1 -> 0). */
export async function openOverlay(
  screen: TransitionScreen,
  show: () => void,
): Promise<void> {
  const cfg = screen.config();
  const t = cfg.transitionType;
  const skipEnter =
    t === 'INSTANT' ||
    (isBidirectional(t) && !cfg.enableEnterAnimation && !cfg.enableBaseSmoothEnter) ||
    (!isBidirectional(t) && !cfg.enableBaseSmoothEnter);
  screen.setExiting(false);
  if (skipEnter) {
    screen.setProgress(0);
    show();
    return;
  }
  screen.setProgress(1);
  show();
  await new Promise<void>((r) => requestAnimationFrame(() => r()));
  await runAnimation(screen, 0, cfg);
}

/** Closing: exit animation (progress -> 1), then hide. */
export async function closeOverlay(screen: TransitionScreen, hide: () => void): Promise<void> {
  const cfg = screen.config();
  screen.setExiting(true);
  if (cfg.transitionType !== 'INSTANT') await runAnimation(screen, 1, cfg);
  hide();
  screen.setExiting(false);
}

/** Cancelled gesture: spring back to 0. */
export async function snapBackOverlay(screen: TransitionScreen, done: () => void): Promise<void> {
  await runAnimation(screen, 0, screen.config());
  done();
}

/**
 * Swipe left->right from anywhere (1:1 swipeBackGesture). Ignores vertical scroll & right-to-left.
 * Returns disposer.
 */
export function swipeBackGesture(
  target: HTMLElement,
  opts: {
    enabled: () => boolean;
    onDragStart: () => void;
    onDragProgress: (p: number) => void;
    onDragEnd: (shouldClose: boolean) => void;
  },
): () => void {
  const slop = 8;
  let id = -1;
  let sx = 0;
  let sy = 0;
  let tracking = false;
  let rejected = false;
  let dx = 0;
  const down = (e: PointerEvent) => {
    if (e.pointerType === 'mouse' || !opts.enabled()) return;
    id = e.pointerId;
    sx = e.clientX;
    sy = e.clientY;
    dx = 0;
    tracking = rejected = false;
  };
  const move = (e: PointerEvent) => {
    if (e.pointerId !== id || rejected) return;
    dx = e.clientX - sx;
    const dy = e.clientY - sy;
    if (!tracking) {
      const t = e.target as HTMLElement | null;
      if (t?.closest('input,textarea,[contenteditable="true"]') && Math.abs(dx) < slop * 4) return;
      if (Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx)) rejected = true;
      else if (dx < -slop) rejected = true;
      else if (dx > slop * 0.8 && dx > Math.abs(dy) * 1.15) {
        tracking = true;
        try {
          target.setPointerCapture(id);
        } catch {
          /* noop */
        }
        opts.onDragStart();
      }
    }
    if (tracking) opts.onDragProgress(Math.min(1, Math.max(0, dx / Math.max(1, target.clientWidth))));
  };
  const up = (e: PointerEvent) => {
    if (e.pointerId !== id) return;
    id = -1;
    if (tracking) opts.onDragEnd(dx / Math.max(1, target.clientWidth) > 0.25);
    tracking = false;
  };
  target.addEventListener('pointerdown', down);
  target.addEventListener('pointermove', move);
  target.addEventListener('pointerup', up);
  target.addEventListener('pointercancel', up);
  return () => {
    target.removeEventListener('pointerdown', down);
    target.removeEventListener('pointermove', move);
    target.removeEventListener('pointerup', up);
    target.removeEventListener('pointercancel', up);
  };
}
