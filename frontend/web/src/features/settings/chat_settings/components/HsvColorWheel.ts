import { el } from '../../../../core/ui/dom';

export interface HsvWheelOptions {
  hue: number;
  saturation: number;
  size?: number;
  onChange: (hue: number, saturation: number) => void;
}

function hsvToRgbBytes(h: number, s: number, v: number): [number, number, number] {
  const c = v * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = v - c;
  let r = 0;
  let g = 0;
  let b = 0;
  if (h < 60) [r, g, b] = [c, x, 0];
  else if (h < 120) [r, g, b] = [x, c, 0];
  else if (h < 180) [r, g, b] = [0, c, x];
  else if (h < 240) [r, g, b] = [0, x, c];
  else if (h < 300) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  return [
    Math.round((r + m) * 255),
    Math.round((g + m) * 255),
    Math.round((b + m) * 255),
  ];
}

export function createHsvColorWheel(opts: HsvWheelOptions): HTMLElement {
  const size = opts.size ?? 196;
  const radius = size / 2;
  let currentHue = opts.hue;
  let currentSat = opts.saturation;

  const wrap = el('div', {
    class: 'ag-wheel-wrap',
    style: `width:${size}px;height:${size}px;`,
  });
  const canvas = el('canvas', { width: size, height: size }) as HTMLCanvasElement;
  const knob = el('div', { class: 'ag-wheel-knob' });
  wrap.appendChild(canvas);
  wrap.appendChild(knob);

  const ctx = canvas.getContext('2d');
  if (ctx) {
    const img = ctx.createImageData(size, size);
    const data = img.data;
    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        const dx = x - radius;
        const dy = y - radius;
        const dist = Math.sqrt(dx * dx + dy * dy);
        const idx = (y * size + x) * 4;
        if (dist <= radius) {
          const angle = ((Math.atan2(dy, dx) * 180) / Math.PI + 360) % 360;
          const sat = Math.min(1, dist / radius);
          const [r, g, b] = hsvToRgbBytes(angle, sat, 1);
          data[idx] = r;
          data[idx + 1] = g;
          data[idx + 2] = b;
          data[idx + 3] = 255;
        } else {
          data[idx + 3] = 0;
        }
      }
    }
    ctx.putImageData(img, 0, 0);
  }

  const updateKnob = () => {
    const rad = (currentHue * Math.PI) / 180;
    const dist = currentSat * (radius - 8);
    const kx = radius + Math.cos(rad) * dist;
    const ky = radius + Math.sin(rad) * dist;
    knob.style.left = `${kx}px`;
    knob.style.top = `${ky}px`;
  };

  const handlePointer = (clientX: number, clientY: number) => {
    const rect = canvas.getBoundingClientRect();
    const dx = clientX - (rect.left + rect.width / 2);
    const dy = clientY - (rect.top + rect.height / 2);
    const r = rect.width / 2;
    const dist = Math.min(r, Math.sqrt(dx * dx + dy * dy));
    currentHue = ((Math.atan2(dy, dx) * 180) / Math.PI + 360) % 360;
    currentSat = Math.min(1, Math.max(0, dist / r));
    updateKnob();
    opts.onChange(currentHue, currentSat);
  };

  let dragging = false;
  canvas.addEventListener('pointerdown', (e) => {
    dragging = true;
    canvas.setPointerCapture(e.pointerId);
    handlePointer(e.clientX, e.clientY);
  });
  canvas.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    handlePointer(e.clientX, e.clientY);
  });
  canvas.addEventListener('pointerup', () => {
    dragging = false;
  });
  canvas.addEventListener('pointercancel', () => {
    dragging = false;
  });

  updateKnob();
  return wrap;
}
