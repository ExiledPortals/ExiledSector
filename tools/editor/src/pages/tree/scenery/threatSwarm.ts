import type { ThreatUnitDecoration } from '../model/decorations.ts';
import { fraction, rgb, TWO_PI, type Rgba } from './sceneryMaths.ts';

export const THREAT_FRAGMENT_SPRITE_PATH = 'graphics/ships/threat/fragment_single.png';
export const THREAT_FRAGMENT_GLOW_PATH = 'graphics/fx/hit_glow.png';
export const THREAT_GLOW_TINT: Rgba = rgb(130, 155, 145);
export const THREAT_GLOW_SIZE_MULT = 3;
export const THREAT_GLOW_ALPHA = 0.35;
export const THREAT_FRAGMENT_LENGTHS = 0.05;
const FABRICATOR_FRAGMENTS = 40;
const HIVE_FRAGMENTS = 64;
const SWAY_DEGREES = 4;
const SWAY_SECONDS = 23;
const DRIFT_LENGTHS = 0.03;
const DRIFT_SECONDS = 31;
const SWARM_INNER_LENGTHS = 0.55;
const SWARM_DEPTH_LENGTHS = 0.45;
const MIN_ORBIT_SECONDS = 9;
const EXTRA_ORBIT_SECONDS = 14;
const WOBBLE_LENGTHS = 0.06;
const GOLDEN_FRACTION = 0.618034;

export function threatFragmentCount(unit: ThreatUnitDecoration): number {
  return unit.unitType === 'hive' ? HIVE_FRAGMENTS : FABRICATOR_FRAGMENTS;
}

export function threatSwarmReach(unit: ThreatUnitDecoration): number {
  return unit.length * (SWARM_INNER_LENGTHS + SWARM_DEPTH_LENGTHS + WOBBLE_LENGTHS);
}

export function threatUnitDriftedCentre(unit: ThreatUnitDecoration, seconds: number): { treeX: number; treeY: number } {
  const driftPhase = (TWO_PI * seconds) / DRIFT_SECONDS;
  return {
    treeX: unit.x + Math.cos(driftPhase) * unit.length * DRIFT_LENGTHS,
    treeY: unit.y + Math.sin(driftPhase * 2) * unit.length * DRIFT_LENGTHS,
  };
}

export function threatUnitSwayDegrees(seconds: number): number {
  return SWAY_DEGREES * Math.sin((TWO_PI * seconds) / SWAY_SECONDS);
}

export function fragmentOrbitAngle(index: number, seconds: number): number {
  const seed = fraction(index * GOLDEN_FRACTION);
  const direction = index % 3 === 0 ? -1 : 1;
  const orbitSeconds = MIN_ORBIT_SECONDS + EXTRA_ORBIT_SECONDS * fraction(seed * 7.31);
  return seed * TWO_PI + (direction * TWO_PI * seconds) / orbitSeconds;
}

export function isFragmentInFront(index: number, seconds: number): boolean {
  return Math.sin(fragmentOrbitAngle(index, seconds)) > 0;
}

function fragmentOrbitRadius(unit: ThreatUnitDecoration, index: number, seconds: number): number {
  const depth = fraction(index * 0.754878);
  const wobble = Math.sin(seconds * (0.7 + depth) + index) * WOBBLE_LENGTHS;
  return unit.length * (SWARM_INNER_LENGTHS + SWARM_DEPTH_LENGTHS * depth + wobble);
}

export interface FragmentPose {
  treeX: number;
  treeY: number;
  spinDegrees: number;
}

export function fragmentPose(unit: ThreatUnitDecoration, centreX: number, centreY: number, index: number, seconds: number): FragmentPose {
  const angle = fragmentOrbitAngle(index, seconds);
  const radius = fragmentOrbitRadius(unit, index, seconds);
  const tilt = 0.45 + 0.25 * fraction(index * 0.414214);
  return {
    treeX: centreX + Math.cos(angle) * radius,
    treeY: centreY + Math.sin(angle) * radius * tilt,
    spinDegrees: fraction(index * 0.56984) * 360 + seconds * (40 + 60 * fraction(index * 0.324718)),
  };
}
