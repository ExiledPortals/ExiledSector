import { rgb, sceneryHash, TWO_PI, type Rgba } from './sceneryMaths.ts';

export const MAW_BODY_SPRITE_PATH = 'graphics/ships/shroud/maw_base.png';
export const MAW_GLOW_SPRITE_PATH = 'graphics/ships/shroud/maw_glow.png';
export const MAW_SPOTS_SPRITE_PATH = 'graphics/ships/shroud/spot_cluster.png';
export const MAW_SHROUD_SHEET_PATH = 'graphics/ships/shroud/shrouded_blobs.png';
export const MAW_SHEET_SIDE = 4;
export const MAW_SHROUD_PUFFS = 120;
export const MAW_DARK_PUFFS = 60;
export const MAW_REACH_LENGTHS = 0.9;
export const MAW_WARP_VERTICES = 5;
export const MAW_BODY_HEIGHT_PER_LENGTH = 256 / 384;
export const MAW_GLOW_FORWARD_LENGTHS = 0.078;
export const MAW_SPOTS_WIDTH_LENGTHS = 0.52;
export const MAW_SPOTS_HEIGHT_LENGTHS = 0.427;
export const MAW_SPOTS_FORWARD_LENGTHS = 0.013;
export const MAW_BODY_WARP_LENGTHS = 0.028;
export const MAW_GLOW_WARP_LENGTHS = 0.02;
export const MAW_FLASH_SIZE_MULT = 1.2;
export const MAW_BODY_ALPHA = 0.9;
export const MAW_SHROUD_ALPHA = 0.85;
export const MAW_DARK_ALPHA = 0.85;
export const MAW_SHROUD_TINT: Rgba = rgb(175, 0, 28);
export const MAW_SHROUD_FLASH_TINT: Rgba = rgb(200, 10, 20);
export const MAW_PART_GLOW_TINT: Rgba = rgb(255, 0, 50);
export const MAW_DARKNESS_TINT: Rgba = rgb(6, 0, 3);

const SHROUD_RADIUS_LENGTHS = 0.52;
const FORWARD_SQUASH = 0.65;
const SHROUD_PUFF_LENGTHS = 0.335;
const DARK_PUFF_LENGTHS = 0.42;
const DARK_RADIUS_SHARE = 0.72;
const MIN_SPAWN_SHARE = 0.4;
const MAX_SPAWN_SHARE = 0.8;
const OUTWARD_DRIFT = 0.35;
const SWIRL_RADIANS_PER_SECOND = 0.12;
const MIN_PUFF_SECONDS = 6;
const EXTRA_PUFF_SECONDS = 6;
const MIN_DARK_SECONDS = 4;
const EXTRA_DARK_SECONDS = 4;
const SPIN_DEGREES_PER_SECOND = 20;
const FLASH_SLOT_SECONDS = 4;
const FLASH_SECONDS = 0.9;
const FLASH_CHANCE = 0.15;
const MIN_WARP_RATE = 0.5;
const EXTRA_WARP_RATE = 0.7;
const SWAY_DEGREES = 3;
const SWAY_SECONDS = 26;
const DRIFT_LENGTHS = 0.02;
const DRIFT_SECONDS = 37;
const GLOW_ALPHA = 0.35;
const GLOW_PULSE = 0.1;
const GLOW_PULSE_SECONDS = 5;
const SPOTS_ALPHA = 0.45;
const SPOTS_PULSE = 0.3;
const SPOTS_PULSE_SECONDS = 3.5;

export interface ShroudPuff {
  side: number;
  forward: number;
  puffSize: number;
  spinDegrees: number;
  puffAlpha: number;
  flash: number;
  cell: number;
}

function sheetCell(index: number, life: number): number {
  return Math.floor(sceneryHash(index, life, 17) * MAW_SHEET_SIDE * MAW_SHEET_SIDE);
}

function flashAt(index: number, seconds: number): number {
  const slotted = seconds / FLASH_SLOT_SECONDS + sceneryHash(index, 0, 15);
  const slot = Math.floor(slotted);
  const intoSlotSeconds = (slotted - slot) * FLASH_SLOT_SECONDS;
  if (intoSlotSeconds >= FLASH_SECONDS || sceneryHash(index, slot, 16) >= FLASH_CHANCE) {
    return 0;
  }
  return Math.sin((Math.PI * intoSlotSeconds) / FLASH_SECONDS);
}

export function shroudPuff(index: number, seconds: number): ShroudPuff {
  const lifeSeconds = MIN_PUFF_SECONDS + EXTRA_PUFF_SECONDS * sceneryHash(index, 0, 3);
  const aged = seconds / lifeSeconds + sceneryHash(index, 0, 4);
  const life = Math.floor(aged);
  const age = aged - life;
  const spawnShare = MIN_SPAWN_SHARE + (MAX_SPAWN_SHARE - MIN_SPAWN_SHARE) * Math.sqrt(sceneryHash(index, life, 5));
  const radiusShare = spawnShare + (1 - spawnShare) * OUTWARD_DRIFT * age;
  const swirl = (index % 2 === 0 ? 1 : -1) * SWIRL_RADIANS_PER_SECOND * seconds;
  const angle = TWO_PI * sceneryHash(index, life, 6) + swirl;
  return {
    side: Math.cos(angle) * radiusShare * SHROUD_RADIUS_LENGTHS,
    forward: Math.sin(angle) * radiusShare * SHROUD_RADIUS_LENGTHS * FORWARD_SQUASH,
    puffSize: SHROUD_PUFF_LENGTHS * (0.75 + 0.5 * sceneryHash(index, life, 7)) * (0.85 + 0.3 * age),
    spinDegrees: 360 * sceneryHash(index, life, 8) + (sceneryHash(index, life, 9) - 0.5) * 2 * SPIN_DEGREES_PER_SECOND * age * lifeSeconds,
    puffAlpha: Math.sin(Math.PI * age),
    flash: flashAt(index, seconds),
    cell: sheetCell(index, life),
  };
}

export function darkPuff(index: number, seconds: number): ShroudPuff {
  const lifeSeconds = MIN_DARK_SECONDS + EXTRA_DARK_SECONDS * sceneryHash(index, 0, 10);
  const aged = seconds / lifeSeconds + sceneryHash(index, 0, 11);
  const life = Math.floor(aged);
  const age = aged - life;
  const radiusShare = DARK_RADIUS_SHARE * Math.sqrt(sceneryHash(index, life, 12));
  const angle = TWO_PI * sceneryHash(index, life, 13);
  return {
    side: Math.cos(angle) * radiusShare * SHROUD_RADIUS_LENGTHS,
    forward: Math.sin(angle) * radiusShare * SHROUD_RADIUS_LENGTHS * FORWARD_SQUASH,
    puffSize: DARK_PUFF_LENGTHS * (0.9 + 0.2 * age),
    spinDegrees: 360 * sceneryHash(index, life, 14),
    puffAlpha: Math.sin(Math.PI * age),
    flash: 0,
    cell: sheetCell(index + MAW_SHROUD_PUFFS, life),
  };
}

export function warpOffset(vertex: number, axis: number, seconds: number): number {
  const rate = MIN_WARP_RATE + EXTRA_WARP_RATE * sceneryHash(vertex, axis, 1);
  return Math.sin(seconds * rate + TWO_PI * sceneryHash(vertex, axis, 2));
}

export function mawDrift(seconds: number, bodyLength: number): { driftX: number; driftY: number } {
  const driftPhase = (TWO_PI * seconds) / DRIFT_SECONDS;
  return { driftX: Math.cos(driftPhase) * bodyLength * DRIFT_LENGTHS, driftY: Math.sin(driftPhase * 2) * bodyLength * DRIFT_LENGTHS };
}

export function mawSwayDegrees(seconds: number): number {
  return SWAY_DEGREES * Math.sin((TWO_PI * seconds) / SWAY_SECONDS);
}

export function mawGlowAlpha(seconds: number): number {
  return GLOW_ALPHA + GLOW_PULSE * Math.sin((TWO_PI * seconds) / GLOW_PULSE_SECONDS);
}

export function mawSpotsAlpha(seconds: number): number {
  return SPOTS_ALPHA + SPOTS_PULSE * Math.sin((TWO_PI * seconds) / SPOTS_PULSE_SECONDS + 1);
}
