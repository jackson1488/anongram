/** Port of core/navigation/PhysicsSprings.kt: easing curves + Compose-style spring simulation. */

export type Easing = (t: number) => number;

/** Same algorithm as Compose CubicBezierEasing. */
export function cubicBezier(x1: number, y1: number, x2: number, y2: number): Easing {
  const cx = 3 * x1;
  const bx = 3 * (x2 - x1) - cx;
  const ax = 1 - cx - bx;
  const cy = 3 * y1;
  const by = 3 * (y2 - y1) - cy;
  const ay = 1 - cy - by;
  const sampleX = (t: number) => ((ax * t + bx) * t + cx) * t;
  const sampleY = (t: number) => ((ay * t + by) * t + cy) * t;
  return (x: number) => {
    if (x <= 0) return 0;
    if (x >= 1) return 1;
    let lo = 0;
    let hi = 1;
    let t = x;
    for (let i = 0; i < 24; i++) {
      const v = sampleX(t);
      if (Math.abs(v - x) < 1e-5) break;
      if (v < x) lo = t;
      else hi = t;
      t = (lo + hi) / 2;
    }
    return sampleY(t);
  };
}

export const PhysicsSprings = {
  /** Flagship overshoot curve: Cubic(0.22, 1.25, 0.36, 1.0) */
  OvershootEasing: cubicBezier(0.22, 1.25, 0.36, 1.0),
  /** Natural decelerate curve */
  DecelerateEasing: cubicBezier(0.0, 0.0, 0.2, 1.0),
  /** CSS equivalents for plain CSS transitions. */
  OvershootCss: 'cubic-bezier(0.22, 1.25, 0.36, 1)',
  DecelerateCss: 'cubic-bezier(0, 0, 0.2, 1)',
  screenSpringSpec(damping = 0.76, stiffness = 320): SpringSpec {
    return { damping, stiffness };
  },
};

export interface SpringSpec {
  /** damping ratio (Compose dampingRatio) */
  damping: number;
  stiffness: number;
}

export interface AnimationHandle {
  cancel(): void;
  finished: Promise<void>;
}

const REST_DELTA = 0.0008;

/** Animates a scalar with a damped spring (mass = 1) à la Compose spring(). */
export function animateSpring(
  from: number,
  to: number,
  spec: SpringSpec,
  onFrame: (v: number) => void,
  initialVelocity = 0,
): AnimationHandle {
  let raf = 0;
  let done = false;
  let resolve!: () => void;
  const finished = new Promise<void>((r) => (resolve = r));
  const k = spec.stiffness;
  const c = 2 * spec.damping * Math.sqrt(k);
  let x = from;
  let v = initialVelocity;
  let last = performance.now();
  const step = (now: number) => {
    if (done) return;
    let dt = Math.min(0.032, (now - last) / 1000);
    last = now;
    // sub-step for stability
    const n = Math.max(1, Math.ceil(dt / 0.004));
    const h = dt / n;
    for (let i = 0; i < n; i++) {
      const a = -k * (x - to) - c * v;
      v += a * h;
      x += v * h;
    }
    if (Math.abs(x - to) < REST_DELTA && Math.abs(v) < 0.02) {
      x = to;
      onFrame(x);
      done = true;
      resolve();
      return;
    }
    onFrame(x);
    raf = requestAnimationFrame(step);
  };
  raf = requestAnimationFrame(step);
  return {
    finished,
    cancel() {
      if (done) return;
      done = true;
      cancelAnimationFrame(raf);
      resolve();
    },
  };
}

/** Duration-based tween (Compose tween()). */
export function animateTween(
  from: number,
  to: number,
  durationMs: number,
  onFrame: (v: number) => void,
  easing: Easing = PhysicsSprings.DecelerateEasing,
): AnimationHandle {
  let raf = 0;
  let done = false;
  let resolve!: () => void;
  const finished = new Promise<void>((r) => (resolve = r));
  const start = performance.now();
  const step = (now: number) => {
    if (done) return;
    const t = durationMs <= 0 ? 1 : Math.min(1, (now - start) / durationMs);
    onFrame(from + (to - from) * easing(t));
    if (t >= 1) {
      done = true;
      resolve();
      return;
    }
    raf = requestAnimationFrame(step);
  };
  raf = requestAnimationFrame(step);
  return {
    finished,
    cancel() {
      if (done) return;
      done = true;
      cancelAnimationFrame(raf);
      resolve();
    },
  };
}
