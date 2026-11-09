import type { HypershuntDecoration, OrbitFields, SerpentDecoration } from '../model/decorations.ts';
import { rgb, toDegrees, toRadians, type Rgba } from './sceneryMaths.ts';

const MIN_ORBIT_SECONDS = 1;

export function orbitDirection(orbit: OrbitFields): number {
  return orbit.clockwise ? 1 : -1;
}

export function orbitAngleDegreesAt(orbit: OrbitFields, seconds: number): number {
  return orbit.startAngle + (orbitDirection(orbit) * 360 * seconds) / Math.max(MIN_ORBIT_SECONDS, orbit.orbitSeconds);
}

export interface TreePose {
  treeX: number;
  treeY: number;
  spriteAngleDegrees: number;
}

export function hypershuntPose(hypershunt: HypershuntDecoration, seconds: number): TreePose {
  const angleDegrees = orbitAngleDegreesAt(hypershunt, seconds);
  const angle = toRadians(angleDegrees);
  return {
    treeX: hypershunt.x + hypershunt.orbitRadius * Math.cos(angle),
    treeY: hypershunt.y + hypershunt.orbitRadius * Math.sin(angle),
    spriteAngleDegrees: -(angleDegrees + 90),
  };
}

export interface SerpentPart {
  spritePath: string;
  glowPath: string;
  partWidth: number;
  partHeight: number;
  behindHead: number;
  imageBackOffset: number;
}

function serpentPart(name: string, partWidth: number, partHeight: number, behindHead: number, imageBackOffset: number): SerpentPart {
  return {
    spritePath: `graphics/fx/rat_genesis_serpent_${name}.png`,
    glowPath: `graphics/fx/rat_genesis_serpent_${name}_glow.png`,
    partWidth,
    partHeight,
    behindHead,
    imageBackOffset,
  };
}

export const SERPENT_PARTS: SerpentPart[] = [
  serpentPart('system', 160, 220, 456, 36),
  serpentPart('spine', 120, 100, 376, 0),
  serpentPart('Segment', 180, 160, 308, 0),
  serpentPart('spine', 120, 100, 236, 0),
  serpentPart('Segment', 180, 160, 168, 0),
  serpentPart('spine', 120, 100, 96, 0),
  serpentPart('head', 280, 240, 0, 0),
];

export const SERPENT_SCALE = 1.4;
export const SERPENT_BODY_ALPHA = 0.45;
export const SERPENT_GLOW_ALPHA = 0.25;
export const SERPENT_BODY_TINT: Rgba = rgb(150, 140, 175);
export const SERPENT_GLOW_TINT: Rgba = rgb(170, 60, 255);
export const SERPENT_REACH_PER_RADIUS = 1.3;
const SERPENT_MIN_ORBIT_RADIUS = 200;
const TANGENT_STEP_RADIANS = 0.002;
const WALK_STEP = 6;
const MAX_WALK_STEPS = 20000;

export interface SerpentPartPose {
  treeX: number;
  treeY: number;
  facingDegrees: number;
}

export function serpentOrbitRadius(serpent: SerpentDecoration): number {
  return Math.max(SERPENT_MIN_ORBIT_RADIUS, serpent.orbitRadius);
}

function wobblyOrbitPoint(serpent: SerpentDecoration, orbitRadius: number, angleRadians: number): { pointX: number; pointY: number } {
  const radius = orbitRadius * (1 + serpent.wobble * Math.sin(Math.trunc(serpent.wobbleWaves) * angleRadians + toRadians(serpent.wobblePhase)));
  return { pointX: serpent.x + radius * Math.cos(angleRadians), pointY: serpent.y + radius * Math.sin(angleRadians) };
}

function facingDegrees(serpent: SerpentDecoration, orbitRadius: number, angleRadians: number, pointX: number, pointY: number): number {
  const ahead = wobblyOrbitPoint(serpent, orbitRadius, angleRadians + orbitDirection(serpent) * TANGENT_STEP_RADIANS);
  return toDegrees(Math.atan2(ahead.pointY - pointY, ahead.pointX - pointX));
}

export function serpentPartPoses(serpent: SerpentDecoration, seconds: number): SerpentPartPose[] {
  const orbitRadius = serpentOrbitRadius(serpent);
  const poses: SerpentPartPose[] = new Array<SerpentPartPose>(SERPENT_PARTS.length);
  let angle = toRadians(orbitAngleDegreesAt(serpent, seconds));
  const angleStep = (orbitDirection(serpent) * WALK_STEP) / orbitRadius;
  let { pointX, pointY } = wobblyOrbitPoint(serpent, orbitRadius, angle);
  let walked = 0;
  let previousX = pointX;
  let previousY = pointY;
  let stepLength = 0;
  let stepsTaken = 0;
  for (let index = SERPENT_PARTS.length - 1; index >= 0; index--) {
    const target = SERPENT_PARTS[index].behindHead * SERPENT_SCALE;
    while (walked < target && stepsTaken++ < MAX_WALK_STEPS) {
      angle -= angleStep;
      previousX = pointX;
      previousY = pointY;
      ({ pointX, pointY } = wobblyOrbitPoint(serpent, orbitRadius, angle));
      stepLength = Math.hypot(pointX - previousX, pointY - previousY);
      walked += stepLength;
    }
    const backtrack = stepLength <= 0 ? 0 : (walked - target) / stepLength;
    poses[index] = {
      treeX: pointX + (previousX - pointX) * backtrack,
      treeY: pointY + (previousY - pointY) * backtrack,
      facingDegrees: facingDegrees(serpent, orbitRadius, angle, pointX, pointY),
    };
  }
  return poses;
}
