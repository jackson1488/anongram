import { themeStore } from '../../../../core/theme/themeStore';
import { el } from '../../../../core/ui/dom';

/**
 * 1-to-1 Canvas port of Android WelcomeShieldAnimation.kt:
 * - Outer shield contour with radial aura glow
 * - Vertical gradient fill from scheme.card -> scheme.surface
 * - Inner subtle glowing contour
 * - Accent gradient stroke
 * - Origami paper plane (left wing, right wing, bottom fold, outline)
 * - Smooth pulse scale & glow alpha animation
 */
export function createWelcomeShieldAnimation(size = 130): HTMLElement {
  const wrap = el('div', {
    class: 'ag-shield-canvas-wrap',
    style: `width:${size}px;height:${size}px;`,
  });
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  const canvas = el('canvas', {
    width: size * dpr,
    height: size * dpr,
    style: `width:${size}px;height:${size}px;`,
  }) as HTMLCanvasElement;
  wrap.appendChild(canvas);

  let rafId = 0;
  const start = performance.now();

  const drawShieldPath = (ctx: CanvasRenderingContext2D, w: number, h: number) => {
    ctx.beginPath();
    ctx.moveTo(w * 0.5, h * 0.08);
    ctx.bezierCurveTo(w * 0.82, h * 0.08, w * 0.88, h * 0.22, w * 0.88, h * 0.44);
    ctx.bezierCurveTo(w * 0.88, h * 0.70, w * 0.58, h * 0.87, w * 0.5, h * 0.95);
    ctx.bezierCurveTo(w * 0.42, h * 0.87, w * 0.12, h * 0.70, w * 0.12, h * 0.44);
    ctx.bezierCurveTo(w * 0.12, h * 0.22, w * 0.18, h * 0.08, w * 0.5, h * 0.08);
    ctx.closePath();
  };

  const drawInnerShieldPath = (ctx: CanvasRenderingContext2D, w: number, h: number) => {
    ctx.beginPath();
    ctx.moveTo(w * 0.5, h * 0.13);
    ctx.bezierCurveTo(w * 0.77, h * 0.13, w * 0.82, h * 0.24, w * 0.82, h * 0.43);
    ctx.bezierCurveTo(w * 0.82, h * 0.66, w * 0.56, h * 0.82, w * 0.5, h * 0.89);
    ctx.bezierCurveTo(w * 0.44, h * 0.82, w * 0.18, h * 0.66, w * 0.18, h * 0.43);
    ctx.bezierCurveTo(w * 0.18, h * 0.24, w * 0.23, h * 0.13, w * 0.5, h * 0.13);
    ctx.closePath();
  };

  const renderFrame = (now: number) => {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const scheme = themeStore.scheme;
    const elapsed = (now - start) / 1000;
    const pulseScale = 1 + Math.sin((elapsed * Math.PI * 2) / 4.4) * 0.04;
    const glowAlpha = 0.65 + Math.sin((elapsed * Math.PI * 2) / 3.6) * 0.25;

    ctx.save();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.scale(dpr, dpr);
    ctx.translate(size / 2, size / 2);
    ctx.scale(pulseScale, pulseScale);
    ctx.translate(-size / 2, -size / 2);

    const w = size;
    const h = size;

    // Aura glow
    drawShieldPath(ctx, w, h);
    const aura = ctx.createRadialGradient(w * 0.5, h * 0.5, 4, w * 0.5, h * 0.5, w * 0.62);
    aura.addColorStop(0, scheme.accent);
    aura.addColorStop(0.5, scheme.accentSoft);
    aura.addColorStop(1, 'transparent');
    ctx.save();
    ctx.globalAlpha = glowAlpha * 0.42;
    ctx.fillStyle = aura;
    ctx.fill();
    ctx.restore();

    // Shield fill
    drawShieldPath(ctx, w, h);
    const shieldGrad = ctx.createLinearGradient(0, 0, 0, h);
    shieldGrad.addColorStop(0, scheme.card);
    shieldGrad.addColorStop(1, scheme.surface);
    ctx.fillStyle = shieldGrad;
    ctx.fill();

    // Inner subtle contour
    drawInnerShieldPath(ctx, w, h);
    ctx.save();
    ctx.globalAlpha = 0.35;
    ctx.strokeStyle = scheme.accent;
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.restore();

    // Outer gradient stroke
    drawShieldPath(ctx, w, h);
    const strokeGrad = ctx.createLinearGradient(0, 0, w, h);
    strokeGrad.addColorStop(0, scheme.accent);
    strokeGrad.addColorStop(0.5, scheme.accentDim);
    strokeGrad.addColorStop(1, scheme.accent);
    ctx.strokeStyle = strokeGrad;
    ctx.lineWidth = 4.2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();

    // Origami paper plane
    const noseX = w * 0.68;
    const noseY = h * 0.31;
    const leftWingX = w * 0.32;
    const leftWingY = h * 0.52;
    const rightWingX = w * 0.54;
    const rightWingY = h * 0.70;
    const notchX = w * 0.49;
    const notchY = h * 0.51;

    // Left wing
    ctx.beginPath();
    ctx.moveTo(noseX, noseY);
    ctx.lineTo(leftWingX, leftWingY);
    ctx.lineTo(notchX, notchY);
    ctx.closePath();
    ctx.fillStyle = scheme.accentDim;
    ctx.fill();

    // Right wing
    ctx.beginPath();
    ctx.moveTo(noseX, noseY);
    ctx.lineTo(w * 0.71, h * 0.34);
    ctx.lineTo(rightWingX, rightWingY);
    ctx.lineTo(notchX, notchY);
    ctx.closePath();
    const wingGrad = ctx.createLinearGradient(leftWingX, leftWingY, noseX, noseY);
    wingGrad.addColorStop(0, scheme.textPrimary);
    wingGrad.addColorStop(1, scheme.accent);
    ctx.fillStyle = wingGrad;
    ctx.fill();

    // Bottom fold
    ctx.beginPath();
    ctx.moveTo(notchX, notchY);
    ctx.lineTo(rightWingX, rightWingY);
    ctx.lineTo(w * 0.44, h * 0.56);
    ctx.closePath();
    ctx.save();
    ctx.globalAlpha = 0.75;
    ctx.fillStyle = scheme.accentDim;
    ctx.fill();
    ctx.restore();

    // Plane outline
    ctx.beginPath();
    ctx.moveTo(leftWingX, leftWingY);
    ctx.lineTo(noseX, noseY);
    ctx.lineTo(rightWingX, rightWingY);
    ctx.lineTo(notchX, notchY);
    ctx.closePath();
    ctx.save();
    ctx.globalAlpha = 0.85;
    ctx.strokeStyle = scheme.textPrimary;
    ctx.lineWidth = 1.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();
    ctx.restore();

    ctx.restore();
    rafId = requestAnimationFrame(renderFrame);
  };

  rafId = requestAnimationFrame(renderFrame);
  (wrap as unknown as { __cleanup?: () => void }).__cleanup = () => {
    if (rafId) cancelAnimationFrame(rafId);
  };

  return wrap;
}
