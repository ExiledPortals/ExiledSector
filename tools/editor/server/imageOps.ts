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

export interface RgbaImage {
  data: Buffer;
  width: number;
  height: number;
}

const RIGHT_ANGLE = 90;

function normalisedDegrees(degrees: number): number {
  return ((degrees % 360) + 360) % 360;
}

function rotateByRightAngles(source: RgbaImage, quarterTurns: number): RgbaImage {
  const swapsSides = quarterTurns % 2 === 1;
  const width = swapsSides ? source.height : source.width;
  const height = swapsSides ? source.width : source.height;
  const data = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      let sourceX = x;
      let sourceY = y;
      if (quarterTurns === 1) {
        sourceX = y;
        sourceY = source.height - 1 - x;
      } else if (quarterTurns === 2) {
        sourceX = source.width - 1 - x;
        sourceY = source.height - 1 - y;
      } else if (quarterTurns === 3) {
        sourceX = source.width - 1 - y;
        sourceY = x;
      }
      source.data.copy(data, (y * width + x) * 4, (sourceY * source.width + sourceX) * 4, (sourceY * source.width + sourceX) * 4 + 4);
    }
  }
  return { data, width, height };
}

function premultipliedPixel(source: RgbaImage, x: number, y: number): [number, number, number, number] {
  if (x < 0 || y < 0 || x >= source.width || y >= source.height) {
    return [0, 0, 0, 0];
  }
  const offset = (y * source.width + x) * 4;
  const alpha = source.data[offset + 3] / 255;
  return [source.data[offset] * alpha, source.data[offset + 1] * alpha, source.data[offset + 2] * alpha, source.data[offset + 3]];
}

function bilinearSample(source: RgbaImage, x: number, y: number): [number, number, number, number] {
  const left = Math.floor(x);
  const top = Math.floor(y);
  const fractionX = x - left;
  const fractionY = y - top;
  const corners = [
    [premultipliedPixel(source, left, top), (1 - fractionX) * (1 - fractionY)],
    [premultipliedPixel(source, left + 1, top), fractionX * (1 - fractionY)],
    [premultipliedPixel(source, left, top + 1), (1 - fractionX) * fractionY],
    [premultipliedPixel(source, left + 1, top + 1), fractionX * fractionY],
  ] as const;
  const sum = [0, 0, 0, 0];
  for (const [pixel, weight] of corners) {
    for (let channel = 0; channel < 4; channel++) {
      sum[channel] += pixel[channel] * weight;
    }
  }
  const alpha = sum[3];
  if (alpha <= 0) {
    return [0, 0, 0, 0];
  }
  const unpremultiply = 255 / alpha;
  return [Math.round(sum[0] * unpremultiply), Math.round(sum[1] * unpremultiply), Math.round(sum[2] * unpremultiply), Math.round(alpha)];
}

function rotateByAnyAngle(source: RgbaImage, degrees: number): RgbaImage {
  const radians = (degrees * Math.PI) / 180;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  const centreX = source.width / 2;
  const centreY = source.height / 2;
  const data = Buffer.alloc(source.data.length);
  for (let y = 0; y < source.height; y++) {
    for (let x = 0; x < source.width; x++) {
      const offsetX = x + 0.5 - centreX;
      const offsetY = y + 0.5 - centreY;
      const sourceX = offsetX * cos + offsetY * sin + centreX - 0.5;
      const sourceY = -offsetX * sin + offsetY * cos + centreY - 0.5;
      data.set(bilinearSample(source, sourceX, sourceY), (y * source.width + x) * 4);
    }
  }
  return { data, width: source.width, height: source.height };
}

export function rotatePixels(source: RgbaImage, degrees: number): RgbaImage {
  const clockwise = normalisedDegrees(degrees);
  if (clockwise % RIGHT_ANGLE === 0) {
    return rotateByRightAngles(source, clockwise / RIGHT_ANGLE);
  }
  return rotateByAnyAngle(source, clockwise);
}

export function rotateImage(sourcePath: string, destinationPath: string, degrees: number): void {
  const rotated = rotatePixels(readPng(sourcePath), degrees);
  const image = new PNG({ width: rotated.width, height: rotated.height });
  rotated.data.copy(image.data);
  writePng(destinationPath, image);
}
