import { toRadians, TWO_PI } from './sceneryMaths.ts';

export const RING_PIXELS_PER_SEGMENT = 5;
export const RING_MAX_TILE_COUNT = 60;
export const ATMOSPHERE_SEGMENTS = 64;
export const ATMOSPHERE_INNER_INSET_MULT = 0.4;
export const ATMOSPHERE_OUTER_TEX_Y = 0.99;
export const AURORA_INNER_RADIUS_MULT = 1.1;
export const AURORA_OUTER_RADIUS_MULT = 1.6;
export const AURORA_BAND_ALPHA = 25;
export const AURORA_BAND_WIDTH_IN_TEXTURE = 256;
export const AURORA_DEGREES_PER_SECOND = 18;
const AURORA_MIN_THICKNESS = 10;
const AURORA_PIXELS_PER_SEGMENT = 50;
const AURORA_SHORTEN_MULT = 0.85;
const AURORA_VERTEX_WOBBLE_SHARE = 0.33;
const TEXTURE_EDGE_INSET = 0.001;

export function quantisedSegments(segments: number): number {
  if (segments <= 1) {
    return 1;
  }
  const powerOfTwo = 2 ** Math.floor(Math.log2(segments));
  if (segments === powerOfTwo) {
    return segments;
  }
  const midStep = powerOfTwo + powerOfTwo / 2;
  return segments <= midStep ? midStep : powerOfTwo * 2;
}

export function bandCircumference(innerRadius: number, outerRadius: number): number {
  return (TWO_PI * (innerRadius + outerRadius)) / 2;
}

export function ringSegmentCount(innerRadius: number, outerRadius: number): number {
  return quantisedSegments(Math.round(bandCircumference(innerRadius, outerRadius) / RING_PIXELS_PER_SEGMENT));
}

export function ringTileCount(innerRadius: number, outerRadius: number, textureAspect: number): number {
  const thickness = outerRadius - innerRadius;
  const circumference = bandCircumference(innerRadius, outerRadius);
  return Math.min(RING_MAX_TILE_COUNT, Math.max(1, Math.round(circumference / (thickness * textureAspect))));
}

export function bandOverlapsRect(centreX: number, centreY: number, innerRadius: number, outerRadius: number, rectWidth: number, rectHeight: number): boolean {
  const nearest = Math.hypot(centreX - Math.max(0, Math.min(centreX, rectWidth)), centreY - Math.max(0, Math.min(centreY, rectHeight)));
  if (nearest > outerRadius) {
    return false;
  }
  const farthest = Math.max(Math.hypot(centreX, centreY), Math.hypot(rectWidth - centreX, centreY), Math.hypot(centreX, rectHeight - centreY), Math.hypot(rectWidth - centreX, rectHeight - centreY));
  return farthest >= innerRadius;
}

export interface BandPair {
  innerX: number;
  innerY: number;
  innerTexX: number;
  innerTexY: number;
  outerX: number;
  outerY: number;
  outerTexX: number;
  outerTexY: number;
}

export function auroraReach(detailRadius: number): number {
  const inner = detailRadius * AURORA_INNER_RADIUS_MULT;
  const thickness = Math.max(detailRadius * AURORA_OUTER_RADIUS_MULT - inner, AURORA_MIN_THICKNESS);
  return inner + thickness * (1 + AURORA_SHORTEN_MULT) + AURORA_PIXELS_PER_SEGMENT * AURORA_VERTEX_WOBBLE_SHARE;
}

export function auroraStrips(detailRadius: number, phaseAngleDegrees: number, textureWidth: number, textureHeight: number): BandPair[][] {
  const radius = detailRadius * AURORA_INNER_RADIUS_MULT;
  const outerRadius = Math.max(detailRadius * AURORA_OUTER_RADIUS_MULT, radius + AURORA_MIN_THICKNESS);
  const thickness = outerRadius - radius;
  const segments = Math.max(1, Math.round(bandCircumference(radius, outerRadius) / AURORA_PIXELS_PER_SEGMENT));
  const anglePerSegment = TWO_PI / segments;
  const rawTexPerSegment = ((AURORA_PIXELS_PER_SEGMENT / textureHeight) * AURORA_BAND_WIDTH_IN_TEXTURE) / thickness;
  const texPerSegment = Math.max(1, Math.round(rawTexPerSegment * segments)) / segments;
  const pulseMax = thickness * AURORA_SHORTEN_MULT;
  let texProgress = 0;
  return [1, 0].map((bandIndex, iteration) => {
    const pairs: BandPair[] = [];
    const turn = iteration === 0 ? 1 : -1;
    const innerTexX = (bandIndex * AURORA_BAND_WIDTH_IN_TEXTURE) / textureWidth;
    const outerTexX = ((bandIndex + 1) * AURORA_BAND_WIDTH_IN_TEXTURE) / textureWidth - TEXTURE_EDGE_INSET;
    for (let step = 0; step < segments + 1; step++) {
      const segmentIndex = step % segments;
      const phaseRadians =
        iteration === 0 ? toRadians(phaseAngleDegrees) + segmentIndex * anglePerSegment * 10 : toRadians(-phaseAngleDegrees) + segmentIndex * anglePerSegment * 5;
      const pulseAmount = Math.sin(phaseRadians) * pulseMax;
      const innerRadius = Math.max(radius * 0.9, radius - pulseAmount * 0.1);
      const outerBandRadius = radius + thickness - pulseAmount;
      const theta = anglePerSegment * segmentIndex;
      const cos = Math.cos(theta) * turn;
      const sin = Math.sin(theta) * turn;
      const wobbleX = Math.cos(phaseRadians) * AURORA_PIXELS_PER_SEGMENT * AURORA_VERTEX_WOBBLE_SHARE * turn;
      const wobbleY = Math.sin(phaseRadians) * AURORA_PIXELS_PER_SEGMENT * AURORA_VERTEX_WOBBLE_SHARE * turn;
      pairs.push({
        innerX: cos * innerRadius,
        innerY: sin * innerRadius,
        innerTexX,
        innerTexY: texProgress,
        outerX: cos * outerBandRadius + wobbleX,
        outerY: sin * outerBandRadius + wobbleY,
        outerTexX,
        outerTexY: texProgress,
      });
      texProgress += texPerSegment;
    }
    return pairs;
  });
}

export function ringStrip(innerRadius: number, outerRadius: number, rotationDegrees: number, textureAspect: number): BandPair[] {
  const segments = ringSegmentCount(innerRadius, outerRadius);
  const texPerSegment = ringTileCount(innerRadius, outerRadius, textureAspect) / segments;
  const rotation = toRadians(rotationDegrees);
  const pairs: BandPair[] = [];
  for (let step = 0; step <= segments; step++) {
    const angle = (TWO_PI * (step % segments)) / segments + rotation;
    const cos = Math.cos(angle);
    const sin = Math.sin(angle);
    const texY = texPerSegment * step;
    pairs.push({ innerX: cos * innerRadius, innerY: sin * innerRadius, innerTexX: 0, innerTexY: texY, outerX: cos * outerRadius, outerY: sin * outerRadius, outerTexX: 1, outerTexY: texY });
  }
  return pairs;
}

export function atmosphereStrip(innerRadius: number, outerRadius: number): BandPair[] {
  const pairs: BandPair[] = [];
  for (let step = 0; step <= ATMOSPHERE_SEGMENTS; step++) {
    const angle = (TWO_PI * (step % ATMOSPHERE_SEGMENTS)) / ATMOSPHERE_SEGMENTS;
    pairs.push({
      innerX: Math.cos(angle) * innerRadius,
      innerY: Math.sin(angle) * innerRadius,
      outerX: Math.cos(angle) * outerRadius,
      outerY: Math.sin(angle) * outerRadius,
      innerTexX: 0,
      innerTexY: 0,
      outerTexX: 0,
      outerTexY: ATMOSPHERE_OUTER_TEX_Y,
    });
  }
  return pairs;
}
