/**
 * Pure TypeScript ISO/IEC 18004 QR Code encoder (Byte mode, EC Level M, Versions 1..6).
 * Generates a valid, scannable QR matrix and renders it as crisp SVG.
 */

interface VersionSpec {
  version: number;
  size: number;
  maxBytes: number;
  dataCodewords: number;
  blocks: number;
  blockData: number;
  blockEc: number;
  alignPos: number | null;
}

const VERSIONS_M: VersionSpec[] = [
  { version: 1, size: 21, maxBytes: 14, dataCodewords: 16, blocks: 1, blockData: 16, blockEc: 10, alignPos: null },
  { version: 2, size: 25, maxBytes: 26, dataCodewords: 28, blocks: 1, blockData: 28, blockEc: 16, alignPos: 18 },
  { version: 3, size: 29, maxBytes: 42, dataCodewords: 44, blocks: 1, blockData: 44, blockEc: 26, alignPos: 22 },
  { version: 4, size: 33, maxBytes: 62, dataCodewords: 64, blocks: 2, blockData: 32, blockEc: 18, alignPos: 26 },
  { version: 5, size: 37, maxBytes: 84, dataCodewords: 86, blocks: 2, blockData: 43, blockEc: 24, alignPos: 30 },
  { version: 6, size: 41, maxBytes: 106, dataCodewords: 108, blocks: 4, blockData: 27, blockEc: 16, alignPos: 34 },
];

// GF(256) with primitive polynomial 0x11D
const GF_EXP = new Uint8Array(512);
const GF_LOG = new Uint8Array(256);
(() => {
  let x = 1;
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x;
    GF_LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i++) {
    GF_EXP[i] = GF_EXP[i - 255];
  }
})();

function gfMul(a: number, b: number): number {
  if (a === 0 || b === 0) return 0;
  return GF_EXP[GF_LOG[a] + GF_LOG[b]];
}

function rsGeneratorPoly(degree: number): Uint8Array {
  let poly = new Uint8Array([1]);
  for (let i = 0; i < degree; i++) {
    const next = new Uint8Array(poly.length + 1);
    const factor = GF_EXP[i];
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], factor);
    }
    poly = next;
  }
  return poly;
}

function rsComputeEc(data: Uint8Array, ecCount: number): Uint8Array {
  const gen = rsGeneratorPoly(ecCount);
  const res = new Uint8Array(data.length + ecCount);
  res.set(data);
  for (let i = 0; i < data.length; i++) {
    const coef = res[i];
    if (coef !== 0) {
      for (let j = 0; j < gen.length; j++) {
        res[i + j] ^= gfMul(gen[j], coef);
      }
    }
  }
  return res.slice(data.length);
}

function encodeDataBytes(utf8: Uint8Array, spec: VersionSpec): Uint8Array {
  const totalBits = spec.dataCodewords * 8;
  const bits: number[] = [];
  const pushBits = (val: number, len: number) => {
    for (let i = len - 1; i >= 0; i--) {
      bits.push((val >> i) & 1);
    }
  };

  // Byte mode indicator (0100) + 8-bit length
  pushBits(0b0100, 4);
  pushBits(utf8.length, 8);
  for (let i = 0; i < utf8.length; i++) {
    pushBits(utf8[i], 8);
  }

  // Terminator (up to 4 zero bits)
  const rem = totalBits - bits.length;
  const termLen = Math.min(4, Math.max(0, rem));
  for (let i = 0; i < termLen; i++) bits.push(0);

  // Pad to byte boundary
  while (bits.length % 8 !== 0) bits.push(0);

  const codewords = new Uint8Array(spec.dataCodewords);
  let byteIdx = 0;
  for (let i = 0; i < bits.length; i += 8) {
    let b = 0;
    for (let j = 0; j < 8; j++) b = (b << 1) | bits[i + j];
    codewords[byteIdx++] = b;
  }

  const pads = [0xec, 0x11];
  let padIdx = 0;
  while (byteIdx < spec.dataCodewords) {
    codewords[byteIdx++] = pads[padIdx % 2];
    padIdx++;
  }
  return codewords;
}

function buildFinalBitStream(codewords: Uint8Array, spec: VersionSpec): number[] {
  const dataBlocks: Uint8Array[] = [];
  const ecBlocks: Uint8Array[] = [];

  for (let b = 0; b < spec.blocks; b++) {
    const start = b * spec.blockData;
    const block = codewords.slice(start, start + spec.blockData);
    dataBlocks.push(block);
    ecBlocks.push(rsComputeEc(block, spec.blockEc));
  }

  const interleaved: number[] = [];
  for (let i = 0; i < spec.blockData; i++) {
    for (let b = 0; b < spec.blocks; b++) {
      interleaved.push(dataBlocks[b][i]);
    }
  }
  for (let i = 0; i < spec.blockEc; i++) {
    for (let b = 0; b < spec.blocks; b++) {
      interleaved.push(ecBlocks[b][i]);
    }
  }

  const bits: number[] = [];
  for (const byte of interleaved) {
    for (let i = 7; i >= 0; i--) bits.push((byte >> i) & 1);
  }
  return bits;
}

function makeBaseMatrix(spec: VersionSpec): { modules: Int8Array[]; isFunction: boolean[][] } {
  const n = spec.size;
  const modules = Array.from({ length: n }, () => new Int8Array(n));
  const isFunction = Array.from({ length: n }, () => new Array<boolean>(n).fill(false));

  const setFunc = (r: number, c: number, dark: boolean) => {
    if (r < 0 || r >= n || c < 0 || c >= n) return;
    modules[r][c] = dark ? 1 : 0;
    isFunction[r][c] = true;
  };

  const drawFinder = (top: number, left: number) => {
    for (let dr = -1; dr <= 7; dr++) {
      for (let dc = -1; dc <= 7; dc++) {
        const r = top + dr;
        const c = left + dc;
        if (r < 0 || r >= n || c < 0 || c >= n) continue;
        const inOuter = dr >= 0 && dr <= 6 && dc >= 0 && dc <= 6;
        const isBorder = inOuter && (dr === 0 || dr === 6 || dc === 0 || dc === 6);
        const isCenter = dr >= 2 && dr <= 4 && dc >= 2 && dc <= 4;
        setFunc(r, c, isBorder || isCenter);
      }
    }
  };

  drawFinder(0, 0);
  drawFinder(0, n - 7);
  drawFinder(n - 7, 0);

  // Timing patterns
  for (let i = 8; i < n - 8; i++) {
    const dark = i % 2 === 0;
    setFunc(6, i, dark);
    setFunc(i, 6, dark);
  }

  // Alignment pattern (V2..V6 have a single non-overlapping alignment at (alignPos, alignPos))
  if (spec.alignPos !== null) {
    const pos = spec.alignPos;
    for (let dr = -2; dr <= 2; dr++) {
      for (let dc = -2; dc <= 2; dc++) {
        const dark = Math.max(Math.abs(dr), Math.abs(dc)) !== 1;
        setFunc(pos + dr, pos + dc, dark);
      }
    }
  }

  // Dark module
  setFunc(4 * spec.version + 9, 8, true);

  // Reserve format info areas
  for (let i = 0; i < 9; i++) {
    if (i !== 6) {
      isFunction[8][i] = true;
      isFunction[i][8] = true;
    }
  }
  for (let i = 0; i < 8; i++) {
    isFunction[8][n - 1 - i] = true;
    if (i < 7) isFunction[n - 1 - i][8] = true;
  }

  return { modules, isFunction };
}

function placeDataBits(modules: Int8Array[], isFunction: boolean[][], bits: number[]): void {
  const n = modules.length;
  let bitIdx = 0;
  let upward = true;

  for (let col = n - 1; col >= 1; col -= 2) {
    if (col === 6) col = 5;
    for (let step = 0; step < n; step++) {
      const r = upward ? n - 1 - step : step;
      for (const c of [col, col - 1]) {
        if (!isFunction[r][c]) {
          modules[r][c] = bitIdx < bits.length ? bits[bitIdx++] : 0;
        }
      }
    }
    upward = !upward;
  }
}

function maskCondition(mask: number, r: number, c: number): boolean {
  switch (mask) {
    case 0: return (r + c) % 2 === 0;
    case 1: return r % 2 === 0;
    case 2: return c % 3 === 0;
    case 3: return (r + c) % 3 === 0;
    case 4: return (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0;
    case 5: return ((r * c) % 2) + ((r * c) % 3) === 0;
    case 6: return (((r * c) % 2) + ((r * c) % 3)) % 2 === 0;
    default: return (((r + c) % 2) + ((r * c) % 3)) % 2 === 0;
  }
}

function computeFormatBits(mask: number): number {
  // EC Level M = 0b00
  const data = mask & 0b111;
  let rem = data << 10;
  for (let i = 14; i >= 10; i--) {
    if ((rem >> i) & 1) {
      rem ^= 0x537 << (i - 10);
    }
  }
  return ((data << 10) | rem) ^ 0x5412;
}

function applyFormatInfo(modules: Int8Array[], mask: number): void {
  const n = modules.length;
  const fmt = computeFormatBits(mask);

  const bit = (idx: number) => (fmt >> idx) & 1;

  // Top-left horizontal & vertical
  for (let i = 0; i <= 5; i++) modules[8][i] = bit(i);
  modules[8][7] = bit(6);
  modules[8][8] = bit(7);
  modules[7][8] = bit(8);
  for (let i = 9; i <= 14; i++) modules[14 - i][8] = bit(i);

  // Bottom-left & top-right
  for (let i = 0; i <= 7; i++) modules[8][n - 1 - i] = bit(i);
  for (let i = 8; i <= 14; i++) modules[n - 15 + i][8] = bit(i);
}

function penaltyScore(modules: Int8Array[]): number {
  const n = modules.length;
  let score = 0;

  // N1: 5+ consecutive same-color modules in rows/cols
  for (let r = 0; r < n; r++) {
    let runLen = 1;
    for (let c = 1; c < n; c++) {
      if (modules[r][c] === modules[r][c - 1]) {
        runLen++;
      } else {
        if (runLen >= 5) score += 3 + (runLen - 5);
        runLen = 1;
      }
    }
    if (runLen >= 5) score += 3 + (runLen - 5);
  }

  for (let c = 0; c < n; c++) {
    let runLen = 1;
    for (let r = 1; r < n; r++) {
      if (modules[r][c] === modules[r - 1][c]) {
        runLen++;
      } else {
        if (runLen >= 5) score += 3 + (runLen - 5);
        runLen = 1;
      }
    }
    if (runLen >= 5) score += 3 + (runLen - 5);
  }

  // N2: 2x2 blocks
  for (let r = 0; r < n - 1; r++) {
    for (let c = 0; c < n - 1; c++) {
      const v = modules[r][c];
      if (v === modules[r][c + 1] && v === modules[r + 1][c] && v === modules[r + 1][c + 1]) {
        score += 3;
      }
    }
  }

  // N4: dark ratio
  let dark = 0;
  for (let r = 0; r < n; r++) {
    for (let c = 0; c < n; c++) {
      if (modules[r][c] === 1) dark++;
    }
  }
  const pct = (dark * 100) / (n * n);
  score += Math.floor(Math.abs(pct - 50) / 5) * 10;

  return score;
}

export function encodeQrMatrix(text: string): boolean[][] {
  const utf8All = new TextEncoder().encode(text);
  const spec =
    VERSIONS_M.find((v) => utf8All.length <= v.maxBytes) ?? VERSIONS_M[VERSIONS_M.length - 1];
  const payload = utf8All.length > spec.maxBytes ? utf8All.slice(0, spec.maxBytes) : utf8All;

  const codewords = encodeDataBytes(payload, spec);
  const bits = buildFinalBitStream(codewords, spec);
  const { modules: base, isFunction } = makeBaseMatrix(spec);
  placeDataBits(base, isFunction, bits);

  let bestMask = 0;
  let bestScore = Infinity;
  let bestModules: Int8Array[] = base;

  for (let mask = 0; mask < 8; mask++) {
    const candidate = base.map((row) => new Int8Array(row));
    for (let r = 0; r < spec.size; r++) {
      for (let c = 0; c < spec.size; c++) {
        if (!isFunction[r][c] && maskCondition(mask, r, c)) {
          candidate[r][c] ^= 1;
        }
      }
    }
    applyFormatInfo(candidate, mask);
    const sc = penaltyScore(candidate);
    if (sc < bestScore) {
      bestScore = sc;
      bestMask = mask;
      bestModules = candidate;
    }
  }

  void bestMask;
  return bestModules.map((row) => Array.from(row, (v) => v === 1));
}

export function createQrSvg(text: string, quietZone = 2): SVGSVGElement {
  const matrix = encodeQrMatrix(text);
  const n = matrix.length;
  const total = n + quietZone * 2;

  const paths: string[] = [];
  for (let r = 0; r < n; r++) {
    for (let c = 0; c < n; c++) {
      if (matrix[r][c]) {
        paths.push(`M${c + quietZone},${r + quietZone}h1v1h-1z`);
      }
    }
  }

  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${total} ${total}`);
  svg.setAttribute('class', 'ag-qr-svg');
  svg.setAttribute('shape-rendering', 'crispEdges');
  svg.setAttribute('aria-label', 'AnonGram QR Code');

  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', paths.join(''));
  path.setAttribute('fill', 'currentColor');
  svg.appendChild(path);

  return svg;
}
