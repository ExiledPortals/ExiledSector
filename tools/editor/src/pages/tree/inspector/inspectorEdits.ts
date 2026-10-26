import { normalizeDegrees } from '../model/geometry.ts';

export function degreesOrZero(value: number): number {
  return normalizeDegrees(Number.isNaN(value) ? 0 : value);
}

export function numberOrZero(value: number): number {
  return Number.isNaN(value) ? 0 : value;
}

export function setTags(target: { tags?: string[] }, tags: string[]): void {
  if (tags.length) {
    target.tags = tags;
  } else {
    delete target.tags;
  }
}
