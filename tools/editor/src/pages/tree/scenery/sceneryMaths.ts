export const TWO_PI = Math.PI * 2;

export function toRadians(degrees: number): number {
  return (degrees * Math.PI) / 180;
}

export function toDegrees(radians: number): number {
  return (radians * 180) / Math.PI;
}

export function fraction(value: number): number {
  return value - Math.floor(value);
}

export function wrapInto(value: number, size: number): number {
  const wrapped = value % size;
  return wrapped < 0 ? wrapped + size : wrapped;
}

export function normalizeAngle(degrees: number): number {
  return wrapInto(degrees, 360);
}

export function sceneryHash(index: number, life: number, salt: number): number {
  let mixed = (Math.imul(index, 0x9e3779b1) + Math.imul(life, 0x85ebca77) + Math.imul(salt, 0xc2b2ae3d)) | 0;
  mixed ^= mixed >>> 16;
  mixed = Math.imul(mixed, 0x7feb352d);
  mixed ^= mixed >>> 15;
  mixed = Math.imul(mixed, 0x846ca68b);
  mixed ^= mixed >>> 16;
  return (mixed >>> 8) / (1 << 24);
}

export interface Rgba {
  red: number;
  green: number;
  blue: number;
  alpha: number;
}

export const WHITE: Rgba = { red: 255, green: 255, blue: 255, alpha: 255 };

export function rgb(red: number, green: number, blue: number, alpha = 255): Rgba {
  return { red, green, blue, alpha };
}

export function rgbaFromChannels(channels: number[] | undefined, fallback: Rgba): Rgba {
  if (!channels || channels.length < 3) {
    return fallback;
  }
  return { red: channels[0], green: channels[1], blue: channels[2], alpha: channels.length > 3 ? channels[3] : 255 };
}

export function parseHexColour(hex: string | null | undefined, fallback: Rgba): Rgba {
  if (!hex) {
    return fallback;
  }
  const digits = hex.startsWith('#') ? hex.substring(1) : hex;
  const argbDigits = digits.length === 6 ? `FF${digits}` : digits;
  if (!/^[0-9a-fA-F]{1,8}$/.test(argbDigits)) {
    return fallback;
  }
  const argb = Number.parseInt(argbDigits, 16);
  return { red: (argb >>> 16) & 0xff, green: (argb >>> 8) & 0xff, blue: argb & 0xff, alpha: (argb >>> 24) & 0xff };
}

export function starTinted(starHex: string | null | undefined, base: Rgba): Rgba {
  return { ...parseHexColour(starHex, base), alpha: base.alpha };
}
