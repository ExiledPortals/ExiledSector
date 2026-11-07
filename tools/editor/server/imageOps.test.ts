import { describe, expect, it } from 'vitest';
import { dotNetHsl, ellipseCoverage, hueShiftImage, hueShiftPixels, makeCircularImage, rgbFromHsl, rotateImage, roundHalfToEven, type RgbaImage } from './imageOps.ts';

const POWERSHELL_REFERENCES: [number, number, number, number, [number, number, number]][] = [
  [255, 0, 0, 90, [128, 255, 0]],
  [12, 200, 77, 240, [200, 77, 12]],
  [128, 128, 128, 33, [128, 128, 128]],
  [250, 251, 0, 180, [1, 0, 251]],
  [17, 3, 240, -75, [3, 240, 195]],
  [200, 150, 100, 359.5, [200, 149, 100]],
  [1, 2, 3, 90, [3, 1, 3]],
  [255, 255, 254, 10, [255, 255, 254]],
  [64, 32, 192, 240, [32, 192, 64]],
  [99, 140, 7, 123.25, [7, 92, 140]],
];

describe('hue shift', () => {
  it('matches the PowerShell editor server pixel for pixel', () => {
    for (const [red, green, blue, shift, expected] of POWERSHELL_REFERENCES) {
      const hsl = dotNetHsl(red, green, blue);
      expect(rgbFromHsl(hsl.hue + shift, hsl.saturation, hsl.lightness), `${red},${green},${blue} shifted ${shift}`).toEqual(expected);
    }
  });

  it('rounds halves to even like .NET', () => {
    expect(roundHalfToEven(2.5)).toBe(2);
    expect(roundHalfToEven(3.5)).toBe(4);
    expect(roundHalfToEven(127.5)).toBe(128);
    expect(roundHalfToEven(1.4)).toBe(1);
  });

  it('leaves fully transparent pixels and alpha untouched', () => {
    const pixels = Buffer.from([255, 0, 0, 0, 255, 0, 0, 77]);
    hueShiftPixels(pixels, 90);
    expect([...pixels]).toEqual([255, 0, 0, 0, 128, 255, 0, 77]);
  });
});

describe('circular mask', () => {
  it('keeps the centre, clears the corners and softens the edge', () => {
    expect(ellipseCoverage(5, 5, 10, 10)).toBe(1);
    expect(ellipseCoverage(0, 0, 10, 10)).toBe(0);
    const edge = ellipseCoverage(0, 2, 10, 10);
    expect(edge).toBeGreaterThan(0);
    expect(edge).toBeLessThan(1);
  });

  it('masks a copy of the pixels and leaves the source untouched', () => {
    const source = solidImage(10, 10, [10, 20, 30, 255]);
    const circular = makeCircularImage(source);
    expect([...circular.data.subarray(0, 4)]).toEqual([0, 0, 0, 0]);
    const centre = (5 * 10 + 5) * 4;
    expect([...circular.data.subarray(centre, centre + 4)]).toEqual([10, 20, 30, 255]);
    const edge = (2 * 10) * 4;
    expect(circular.data[edge + 3]).toBeGreaterThan(0);
    expect(circular.data[edge + 3]).toBeLessThan(255);
    expect(source.data.every((channel, index) => channel === [10, 20, 30, 255][index % 4])).toBe(true);
  });
});

describe('hue shift image', () => {
  it('shifts a copy of the pixels and leaves the source untouched', () => {
    const source = solidImage(2, 2, [255, 0, 0, 255]);
    const shifted = hueShiftImage(source, 120);
    expect([...shifted.data.subarray(0, 4)]).toEqual([0, 255, 0, 255]);
    expect([...source.data.subarray(0, 4)]).toEqual([255, 0, 0, 255]);
  });
});

function solidImage(width: number, height: number, rgba: number[]): RgbaImage {
  const data = Buffer.alloc(width * height * 4);
  for (let offset = 0; offset < data.length; offset += 4) {
    data.set(rgba, offset);
  }
  return { data, width, height };
}

function labelledImage(width: number, height: number): RgbaImage {
  const data = Buffer.alloc(width * height * 4);
  for (let index = 0; index < width * height; index++) {
    data.set([index, 0, 0, 255], index * 4);
  }
  return { data, width, height };
}

function labels(image: RgbaImage): number[][] {
  const rows: number[][] = [];
  for (let y = 0; y < image.height; y++) {
    rows.push(Array.from({ length: image.width }, (_, x) => image.data[(y * image.width + x) * 4]));
  }
  return rows;
}

describe('rotation', () => {
  const source = labelledImage(3, 2);

  it('turns quarter turns clockwise without resampling and swaps the sides', () => {
    expect(labels(rotateImage(source, 0))).toEqual([[0, 1, 2], [3, 4, 5]]);
    expect(labels(rotateImage(source, 90))).toEqual([[3, 0], [4, 1], [5, 2]]);
    expect(labels(rotateImage(source, 180))).toEqual([[5, 4, 3], [2, 1, 0]]);
    expect(labels(rotateImage(source, 270))).toEqual([[2, 5], [1, 4], [0, 3]]);
    expect(labels(rotateImage(source, -90))).toEqual(labels(rotateImage(source, 270)));
    expect(labels(rotateImage(source, 450))).toEqual(labels(rotateImage(source, 90)));
  });

  it('keeps the size for other angles, clears the corners and keeps colour at soft edges', () => {
    const red: RgbaImage = { data: Buffer.alloc(8 * 8 * 4), width: 8, height: 8 };
    for (let offset = 0; offset < red.data.length; offset += 4) {
      red.data.set([255, 0, 0, 255], offset);
    }
    const rotated = rotateImage(red, 45);
    expect([rotated.width, rotated.height]).toEqual([8, 8]);
    expect(rotated.data[3]).toBe(0);
    expect([...rotated.data.subarray((4 * 8 + 4) * 4, (4 * 8 + 4) * 4 + 4)]).toEqual([255, 0, 0, 255]);
    for (let offset = 0; offset < rotated.data.length; offset += 4) {
      if (rotated.data[offset + 3] > 0) {
        expect([rotated.data[offset], rotated.data[offset + 1], rotated.data[offset + 2]]).toEqual([255, 0, 0]);
      }
    }
  });
});
