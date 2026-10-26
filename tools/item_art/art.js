const { PNG } = require('../editor/node_modules/pngjs');
const fs = require('fs');

const SIZE = 256;

function hex(h) {
  const v = parseInt(h.slice(1), 16);
  return [((v >> 16) & 255) / 255, ((v >> 8) & 255) / 255, (v & 255) / 255];
}

function load(path) {
  const png = PNG.sync.read(fs.readFileSync(path));
  const data = new Float32Array(png.width * png.height * 4);
  for (let i = 0; i < data.length; i++) data[i] = png.data[i] / 255;
  return { w: png.width, h: png.height, data };
}

function canvas(w = SIZE, h = SIZE) {
  return { w, h, rgb: new Float32Array(w * h * 3) };
}

function samplePremul(img, x, y) {
  const x0 = Math.floor(x), y0 = Math.floor(y);
  const fx = x - x0, fy = y - y0;
  const out = [0, 0, 0, 0];
  for (const [dx, dy, wgt] of [[0, 0, (1 - fx) * (1 - fy)], [1, 0, fx * (1 - fy)], [0, 1, (1 - fx) * fy], [1, 1, fx * fy]]) {
    const sx = x0 + dx, sy = y0 + dy;
    if (sx < 0 || sy < 0 || sx >= img.w || sy >= img.h || wgt === 0) continue;
    const i = (sy * img.w + sx) * 4;
    const a = img.data[i + 3] * wgt;
    out[0] += img.data[i] * a; out[1] += img.data[i + 1] * a; out[2] += img.data[i + 2] * a; out[3] += a;
  }
  return out;
}

function ramp(palette, t) {
  const stops = palette.map(hex);
  t = Math.max(0, Math.min(1, t)) * (stops.length - 1);
  const i = Math.min(stops.length - 2, Math.floor(t));
  const f = t - i;
  return [0, 1, 2].map((c) => stops[i][c] * (1 - f) + stops[i + 1][c] * f);
}

function background(cv, palette, { seed = 1, cx = 0.5, cy = 0.5, glow = 0.55, stars = 60, nebula = 0.35 } = {}) {
  let s = seed;
  const rand = () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
  const grid = 9, noise = [];
  for (let i = 0; i < grid * grid; i++) noise.push(rand());
  const valueNoise = (u, v) => {
    const x = u * (grid - 1), y = v * (grid - 1);
    const x0 = Math.min(grid - 2, Math.floor(x)), y0 = Math.min(grid - 2, Math.floor(y));
    const fx = x - x0, fy = y - y0;
    const sm = (t) => t * t * (3 - 2 * t);
    const a = noise[y0 * grid + x0], b = noise[y0 * grid + x0 + 1], c = noise[(y0 + 1) * grid + x0], d = noise[(y0 + 1) * grid + x0 + 1];
    return (a * (1 - sm(fx)) + b * sm(fx)) * (1 - sm(fy)) + (c * (1 - sm(fx)) + d * sm(fx)) * sm(fy);
  };
  for (let y = 0; y < cv.h; y++) {
    for (let x = 0; x < cv.w; x++) {
      const u = x / cv.w, v = y / cv.h;
      const dist = Math.hypot(u - cx, v - cy);
      const t = Math.max(0, glow - dist) / glow;
      const n = valueNoise(u, v);
      const col = ramp(palette, 0.08 + t * 0.32 + (n - 0.5) * nebula * 0.4);
      const i = (y * cv.w + x) * 3;
      for (let c = 0; c < 3; c++) cv.rgb[i + c] = col[c] * (0.35 + 0.65 * t + n * nebula * 0.3);
    }
  }
  for (let k = 0; k < stars; k++) {
    const x = Math.floor(rand() * cv.w), y = Math.floor(rand() * cv.h), b = 0.25 + rand() * 0.6;
    glowDot(cv, x, y, 1.2 + rand() * 1.5, hex(palette[palette.length - 1]), b);
  }
}

function glowDot(cv, px, py, radius, color, intensity) {
  const r = Math.ceil(radius * 3);
  for (let y = py - r; y <= py + r; y++) {
    for (let x = px - r; x <= px + r; x++) {
      if (x < 0 || y < 0 || x >= cv.w || y >= cv.h) continue;
      const d = Math.hypot(x - px, y - py);
      const a = intensity * Math.exp(-(d * d) / (2 * radius * radius));
      addLight(cv, x, y, color, a);
    }
  }
}

function addLight(cv, x, y, color, a) {
  const i = (y * cv.w + x) * 3;
  for (let c = 0; c < 3; c++) cv.rgb[i + c] = 1 - (1 - cv.rgb[i + c]) * (1 - Math.min(1, color[c] * a));
}

function blurMask(mask, w, h, radius) {
  let cur = mask;
  for (let pass = 0; pass < 3; pass++) {
    const tmp = new Float32Array(w * h), out = new Float32Array(w * h);
    for (let y = 0; y < h; y++) {
      let acc = 0;
      for (let x = -radius; x < w + radius; x++) {
        if (x + radius < w) acc += cur[y * w + Math.max(0, x + radius)] * (x + radius >= 0 ? 1 : 0);
        if (x - radius - 1 >= 0) acc -= cur[y * w + x - radius - 1];
        if (x >= 0 && x < w) tmp[y * w + x] = acc / (2 * radius + 1);
      }
    }
    for (let x = 0; x < w; x++) {
      let acc = 0;
      for (let y = -radius; y < h + radius; y++) {
        if (y + radius < h && y + radius >= 0) acc += tmp[(y + radius) * w + x];
        if (y - radius - 1 >= 0) acc -= tmp[(y - radius - 1) * w + x];
        if (y >= 0 && y < h) out[y * w + x] = acc / (2 * radius + 1);
      }
    }
    cur = out;
  }
  return cur;
}

function sprite(cv, img, opts) {
  const { cx = cv.w / 2, cy = cv.h / 2, scale = 1, angle = 0, crop = null, palette = null, keep = 0, contrast = 1,
    lift = 0, alpha = 1, glow = null, flipX = false, feather = 0 } = opts;
  const c = crop || { x: 0, y: 0, w: img.w, h: img.h };
  const cos = Math.cos(-angle), sin = Math.sin(-angle);
  const layer = new Float32Array(cv.w * cv.h * 4);
  for (let y = 0; y < cv.h; y++) {
    for (let x = 0; x < cv.w; x++) {
      const dx = (x - cx) / scale, dy = (y - cy) / scale;
      let sx = dx * cos - dy * sin, sy = dx * sin + dy * cos;
      if (flipX) sx = -sx;
      sx += c.x + c.w / 2; sy += c.y + c.h / 2;
      if (sx < c.x - 1 || sy < c.y - 1 || sx > c.x + c.w || sy > c.y + c.h) continue;
      const p = samplePremul(img, sx - 0.5, sy - 0.5);
      if (feather > 0) {
        const edge = Math.min(sx - c.x, sy - c.y, c.x + c.w - sx, c.y + c.h - sy);
        const f = Math.max(0, Math.min(1, edge / feather));
        p[3] *= f * f * (3 - 2 * f);
      }
      if (p[3] <= 0.001) continue;
      let col = [p[0] / p[3], p[1] / p[3], p[2] / p[3]];
      if (palette) {
        const lum = 0.299 * col[0] + 0.587 * col[1] + 0.114 * col[2];
        const mapped = ramp(palette, Math.pow(Math.max(0, Math.min(1, (lum - 0.5) * contrast + 0.5 + lift)), 1));
        col = col.map((v, k) => v * keep + mapped[k] * (1 - keep));
      }
      const i = (y * cv.w + x) * 4;
      layer[i] = col[0]; layer[i + 1] = col[1]; layer[i + 2] = col[2]; layer[i + 3] = p[3] * alpha;
    }
  }
  if (glow) {
    const mask = new Float32Array(cv.w * cv.h);
    for (let i = 0; i < mask.length; i++) mask[i] = layer[i * 4 + 3];
    const blurred = blurMask(mask, cv.w, cv.h, glow.radius);
    const gc = hex(glow.color);
    for (let i = 0; i < blurred.length; i++) {
      if (blurred[i] > 0.002) addLight(cv, i % cv.w, Math.floor(i / cv.w), gc, blurred[i] * glow.strength);
    }
  }
  for (let i = 0; i < cv.w * cv.h; i++) {
    const a = layer[i * 4 + 3];
    if (a <= 0) continue;
    for (let k = 0; k < 3; k++) cv.rgb[i * 3 + k] = cv.rgb[i * 3 + k] * (1 - a) + layer[i * 4 + k] * a;
  }
  return layer;
}

function lightPath(cv, points, { width = 2, color = '#ffffff', intensity = 1 } = {}) {
  const col = hex(color);
  const field = new Float32Array(cv.w * cv.h);
  for (let s = 0; s < points.length - 1; s++) {
    const [ax, ay] = points[s], [bx, by] = points[s + 1];
    const minX = Math.max(0, Math.floor(Math.min(ax, bx) - width * 4)), maxX = Math.min(cv.w - 1, Math.ceil(Math.max(ax, bx) + width * 4));
    const minY = Math.max(0, Math.floor(Math.min(ay, by) - width * 4)), maxY = Math.min(cv.h - 1, Math.ceil(Math.max(ay, by) + width * 4));
    const lx = bx - ax, ly = by - ay, len2 = lx * lx + ly * ly || 1;
    for (let y = minY; y <= maxY; y++) {
      for (let x = minX; x <= maxX; x++) {
        const t = Math.max(0, Math.min(1, ((x - ax) * lx + (y - ay) * ly) / len2));
        const d = Math.hypot(x - (ax + t * lx), y - (ay + t * ly));
        const a = Math.exp(-(d * d) / (2 * width * width));
        field[y * cv.w + x] = Math.max(field[y * cv.w + x], a);
      }
    }
  }
  for (let i = 0; i < field.length; i++) {
    if (field[i] > 0.003) addLight(cv, i % cv.w, Math.floor(i / cv.w), col, field[i] * intensity);
  }
}

function arc(cx, cy, r, a0, a1, steps = 48) {
  const pts = [];
  for (let i = 0; i <= steps; i++) {
    const a = a0 + (a1 - a0) * (i / steps);
    pts.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]);
  }
  return pts;
}

function bolt(x0, y0, x1, y1, seed, jag = 10, depth = 5) {
  let s = seed;
  const rand = () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff) - 0.5;
  let pts = [[x0, y0], [x1, y1]];
  let amp = jag;
  for (let d = 0; d < depth; d++) {
    const next = [pts[0]];
    for (let i = 0; i < pts.length - 1; i++) {
      const [ax, ay] = pts[i], [bx, by] = pts[i + 1];
      const nx = -(by - ay), ny = bx - ax, len = Math.hypot(nx, ny) || 1;
      const off = rand() * amp;
      next.push([(ax + bx) / 2 + (nx / len) * off, (ay + by) / 2 + (ny / len) * off], pts[i + 1]);
    }
    pts = next;
    amp *= 0.55;
  }
  return pts;
}

function hexLattice(cv, { size = 14, color = '#88ddff', intensity = 0.5, width = 0.9, mask = () => 1 } = {}) {
  const col = hex(color);
  const h = size * Math.sqrt(3);
  for (let y = 0; y < cv.h; y++) {
    for (let x = 0; x < cv.w; x++) {
      const m = mask(x, y);
      if (m <= 0) continue;
      let qx = x / (size * 1.5), best = 1e9, second = 1e9;
      for (let col_ = Math.floor(qx) - 1; col_ <= Math.floor(qx) + 2; col_++) {
        const ox = col_ * size * 1.5, oy0 = (col_ & 1) ? h / 2 : 0;
        for (let row = Math.floor((y - oy0) / h) - 1; row <= Math.floor((y - oy0) / h) + 2; row++) {
          const d = Math.hypot(x - ox, y - (row * h + oy0));
          if (d < best) { second = best; best = d; } else if (d < second) second = d;
        }
      }
      const edge = Math.exp(-((second - best) ** 2) / (2 * width * width * 4));
      if (edge > 0.01) addLight(cv, x, y, col, edge * intensity * m);
    }
  }
}

function vignette(cv, strength = 0.6) {
  for (let y = 0; y < cv.h; y++) {
    for (let x = 0; x < cv.w; x++) {
      const d = Math.hypot(x / cv.w - 0.5, y / cv.h - 0.5) / 0.7071;
      const f = 1 - strength * Math.pow(d, 2.2);
      const i = (y * cv.w + x) * 3;
      for (let c = 0; c < 3; c++) cv.rgb[i + c] *= f;
    }
  }
}

function toPng(cv, outSize) {
  const factor = cv.w / outSize;
  const png = new PNG({ width: outSize, height: outSize });
  for (let y = 0; y < outSize; y++) {
    for (let x = 0; x < outSize; x++) {
      const acc = [0, 0, 0];
      for (let sy = 0; sy < factor; sy++) for (let sx = 0; sx < factor; sx++) {
        const i = ((y * factor + sy) * cv.w + x * factor + sx) * 3;
        for (let c = 0; c < 3; c++) acc[c] += cv.rgb[i + c];
      }
      const o = (y * outSize + x) * 4;
      for (let c = 0; c < 3; c++) o >= 0 && (png.data[o + c] = Math.round(255 * Math.max(0, Math.min(1, acc[c] / (factor * factor)))));
      png.data[o + 3] = 255;
    }
  }
  return png;
}

function save(cv, path, outSize = 128) {
  fs.writeFileSync(path, PNG.sync.write(toPng(cv, outSize)));
}

function sheet(canvases, path) {
  const tile = SIZE, gap = 8;
  const png = new PNG({ width: canvases.length * (tile + gap) + gap, height: tile + gap * 2 });
  png.data.fill(40);
  for (let i = 3; i < png.data.length; i += 4) png.data[i] = 255;
  canvases.forEach((cv, k) => {
    for (let y = 0; y < tile; y++) for (let x = 0; x < tile; x++) {
      const s = (y * cv.w + x) * 3, o = ((y + gap) * png.width + gap + k * (tile + gap) + x) * 4;
      for (let c = 0; c < 3; c++) png.data[o + c] = Math.round(255 * Math.max(0, Math.min(1, cv.rgb[s + c])));
    }
  });
  fs.writeFileSync(path, PNG.sync.write(png));
}

module.exports = { load, canvas, background, sprite, lightPath, arc, bolt, hexLattice, vignette, glowDot, save, sheet, hex, SIZE };
