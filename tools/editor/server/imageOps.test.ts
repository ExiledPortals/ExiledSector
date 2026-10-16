import { describe, expect, it } from 'vitest';
import { dotNetHsl, ellipseCoverage, hueShiftPixels, rgbFromHsl, roundHalfToEven } from './imageOps.ts';

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
});
