import fs from 'node:fs';
import { PNG } from 'pngjs';

const f32 = Math.fround;

export function roundHalfToEven(value: number): number {
  const floor = Math.floor(value);
  const fraction = value - floor;
  if (fraction > 0.5) {
    return floor + 1;
  }
  if (fraction < 0.5) {
    return floor;
  }
  return floor % 2 === 0 ? floor : floor + 1;
}

export interface Hsl {
  hue: number;
  saturation: number;
  lightness: number;
}

export function dotNetHsl(red: number, green: number, blue: number): Hsl {
  const r = f32(red / 255);
  const g = f32(green / 255);
  const b = f32(blue / 255);
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const lightness = f32(f32(max + min) / 2);
  if (red === green && green === blue) {
    return { hue: 0, saturation: 0, lightness };
  }
  const delta = f32(max - min);
  let hue: number;
  if (r === max) {
    hue = f32(f32(g - b) / delta);
  } else if (g === max) {
    hue = f32(2 + f32(f32(b - r) / delta));
  } else {
    hue = f32(4 + f32(f32(r - g) / delta));
  }
  hue = f32(hue * 60);
  if (hue < 0) {
    hue = f32(hue + 360);
  }
  const saturation = lightness <= 0.5 ? f32(delta / f32(max + min)) : f32(delta / f32(f32(2 - max) - min));
  return { hue, saturation, lightness };
}

function hueToRgbChannel(p: number, q: number, t: number): number {
  let wrapped = t;
  if (wrapped < 0) {
    wrapped += 1;
  }
  if (wrapped > 1) {
    wrapped -= 1;
  }
  if (wrapped < 1 / 6) {
    return p + (q - p) * 6 * wrapped;
  }
  if (wrapped < 0.5) {
    return q;
  }
  if (wrapped < 2 / 3) {
    return p + (q - p) * (2 / 3 - wrapped) * 6;
  }
  return p;
}

export function rgbFromHsl(hue: number, saturation: number, lightness: number): [number, number, number] {
  if (saturation <= 0) {
    const grey = roundHalfToEven(lightness * 255);
    return [grey, grey, grey];
  }
  const q = lightness < 0.5 ? lightness * (1 + saturation) : lightness + saturation - lightness * saturation;
  const p = 2 * lightness - q;
  const hueFraction = (((hue % 360) + 360) % 360) / 360;
  return [
    roundHalfToEven(hueToRgbChannel(p, q, hueFraction + 1 / 3) * 255),
    roundHalfToEven(hueToRgbChannel(p, q, hueFraction) * 255),
    roundHalfToEven(hueToRgbChannel(p, q, hueFraction - 1 / 3) * 255),
  ];
}

function readPng(filePath: string): PNG {
  return PNG.sync.read(fs.readFileSync(filePath));
}

function writePng(filePath: string, image: PNG): void {
  fs.writeFileSync(filePath, PNG.sync.write(image, { colorType: 6 }));
}

export function hueShiftPixels(data: Buffer, hueShift: number): void {
  for (let offset = 0; offset < data.length; offset += 4) {
    if (data[offset + 3] === 0) {
      continue;
    }
    const hsl = dotNetHsl(data[offset], data[offset + 1], data[offset + 2]);
    const [red, green, blue] = rgbFromHsl(hsl.hue + hueShift, hsl.saturation, hsl.lightness);
    data[offset] = red;
    data[offset + 1] = green;
    data[offset + 2] = blue;
  }
}

export function hueShiftImage(sourcePath: string, destinationPath: string, hueShift: number): void {
  const image = readPng(sourcePath);
  hueShiftPixels(image.data, hueShift);
  writePng(destinationPath, image);
}

const CIRCLE_SAMPLES_PER_AXIS = 4;

export function ellipseCoverage(x: number, y: number, width: number, height: number): number {
  const radiusX = width / 2;
  const radiusY = height / 2;
  let inside = 0;
  for (let sampleY = 0; sampleY < CIRCLE_SAMPLES_PER_AXIS; sampleY++) {
    for (let sampleX = 0; sampleX < CIRCLE_SAMPLES_PER_AXIS; sampleX++) {
      const pointX = (x + (sampleX + 0.5) / CIRCLE_SAMPLES_PER_AXIS - radiusX) / radiusX;
      const pointY = (y + (sampleY + 0.5) / CIRCLE_SAMPLES_PER_AXIS - radiusY) / radiusY;
      if (pointX * pointX + pointY * pointY <= 1) {
        inside++;
      }
    }
  }
  return inside / (CIRCLE_SAMPLES_PER_AXIS * CIRCLE_SAMPLES_PER_AXIS);
}

export function makeCircularImage(sourcePath: string, destinationPath: string): void {
  const image = readPng(sourcePath);
  for (let y = 0; y < image.height; y++) {
    for (let x = 0; x < image.width; x++) {
      const offset = (y * image.width + x) * 4;
      const coverage = ellipseCoverage(x, y, image.width, image.height);
      if (coverage <= 0) {
        image.data[offset] = 0;
        image.data[offset + 1] = 0;
        image.data[offset + 2] = 0;
        image.data[offset + 3] = 0;
      } else if (coverage < 1) {
        image.data[offset + 3] = Math.round(image.data[offset + 3] * coverage);
      }
    }
  }
  writePng(destinationPath, image);
}
