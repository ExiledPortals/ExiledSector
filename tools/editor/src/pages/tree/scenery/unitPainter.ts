import { HYPERSHUNT_SPRITE_PATH, threatUnitSprite, type HypershuntDecoration, type SerpentDecoration, type ShroudedMawDecoration, type ThreatUnitDecoration } from '../model/decorations.ts';
import { hypershuntPose, SERPENT_BODY_ALPHA, SERPENT_BODY_TINT, SERPENT_GLOW_ALPHA, SERPENT_GLOW_TINT, SERPENT_PARTS, SERPENT_REACH_PER_RADIUS, SERPENT_SCALE, serpentOrbitRadius, serpentPartPoses } from './orbitPoses.ts';
import { drawSprite, drawTexture, type PaintContext } from './paintContext.ts';
import type { SceneryTexture } from './sceneryGl.ts';
import { toRadians, WHITE, type Rgba } from './sceneryMaths.ts';
import { isOnCanvas, screenX, screenY } from './sceneryViewport.ts';
import {
  darkPuff,
  MAW_BODY_ALPHA,
  MAW_BODY_HEIGHT_PER_LENGTH,
  MAW_BODY_SPRITE_PATH,
  MAW_BODY_WARP_LENGTHS,
  MAW_DARK_ALPHA,
  MAW_DARK_PUFFS,
  MAW_DARKNESS_TINT,
  MAW_FLASH_SIZE_MULT,
  MAW_GLOW_FORWARD_LENGTHS,
  MAW_GLOW_SPRITE_PATH,
  MAW_GLOW_WARP_LENGTHS,
  MAW_PART_GLOW_TINT,
  MAW_REACH_LENGTHS,
  MAW_SHEET_SIDE,
  MAW_SHROUD_ALPHA,
  MAW_SHROUD_FLASH_TINT,
  MAW_SHROUD_PUFFS,
  MAW_SHROUD_SHEET_PATH,
  MAW_SHROUD_TINT,
  MAW_SPOTS_FORWARD_LENGTHS,
  MAW_SPOTS_HEIGHT_LENGTHS,
  MAW_SPOTS_SPRITE_PATH,
  MAW_SPOTS_WIDTH_LENGTHS,
  MAW_WARP_VERTICES,
  mawDrift,
  mawGlowAlpha,
  mawSpotsAlpha,
  mawSwayDegrees,
  shroudPuff,
  warpOffset,
  type ShroudPuff,
} from './shroudPuffs.ts';
import {
  fragmentPose,
  isFragmentInFront,
  THREAT_FRAGMENT_GLOW_PATH,
  THREAT_FRAGMENT_LENGTHS,
  THREAT_FRAGMENT_SPRITE_PATH,
  THREAT_GLOW_ALPHA,
  THREAT_GLOW_SIZE_MULT,
  THREAT_GLOW_TINT,
  threatFragmentCount,
  threatSwarmReach,
  threatUnitDriftedCentre,
  threatUnitSwayDegrees,
} from './threatSwarm.ts';

export function paintHypershunt(context: PaintContext, hypershunt: HypershuntDecoration): void {
  const { viewport } = context;
  const pose = hypershuntPose(hypershunt, context.seconds);
  const centerX = screenX(viewport, pose.treeX);
  const centerY = screenY(viewport, pose.treeY);
  const spriteSize = hypershunt.size * viewport.zoom;
  if (isOnCanvas(viewport, centerX, centerY, spriteSize)) {
    drawSprite(context, HYPERSHUNT_SPRITE_PATH, { centerX, centerY, spriteWidth: spriteSize, spriteHeight: spriteSize, angleDegrees: pose.spriteAngleDegrees }, WHITE, 1, 'normal');
  }
}

export function paintSerpent(context: PaintContext, serpent: SerpentDecoration): void {
  const { viewport } = context;
  const reach = serpentOrbitRadius(serpent) * SERPENT_REACH_PER_RADIUS + SERPENT_PARTS[0].behindHead * SERPENT_SCALE;
  if (!isOnCanvas(viewport, screenX(viewport, serpent.x), screenY(viewport, serpent.y), reach * viewport.zoom)) {
    return;
  }
  const poses = serpentPartPoses(serpent, context.seconds);
  SERPENT_PARTS.forEach((part, index) => {
    const pose = poses[index];
    const facing = toRadians(pose.facingDegrees);
    const centerX = screenX(viewport, pose.treeX - Math.cos(facing) * part.imageBackOffset * SERPENT_SCALE);
    const centerY = screenY(viewport, pose.treeY - Math.sin(facing) * part.imageBackOffset * SERPENT_SCALE);
    const spriteWidth = part.partWidth * SERPENT_SCALE * viewport.zoom;
    const spriteHeight = part.partHeight * SERPENT_SCALE * viewport.zoom;
    if (!isOnCanvas(viewport, centerX, centerY, Math.max(spriteWidth, spriteHeight))) {
      return;
    }
    const placement = { centerX, centerY, spriteWidth, spriteHeight, angleDegrees: -pose.facingDegrees - 90 };
    drawSprite(context, part.spritePath, placement, SERPENT_BODY_TINT, SERPENT_BODY_ALPHA, 'normal');
    drawSprite(context, part.glowPath, placement, SERPENT_GLOW_TINT, SERPENT_GLOW_ALPHA, 'additive');
  });
}

function paintSwarm(context: PaintContext, unit: ThreatUnitDecoration, centreX: number, centreY: number, inFront: boolean): void {
  const fragmentTexture = context.gl.texture(THREAT_FRAGMENT_SPRITE_PATH);
  const glowTexture = context.gl.texture(THREAT_FRAGMENT_GLOW_PATH);
  if (!fragmentTexture || !glowTexture) {
    return;
  }
  const { viewport, seconds } = context;
  const fragmentSize = unit.length * THREAT_FRAGMENT_LENGTHS * viewport.zoom;
  const fragments = Array.from({ length: threatFragmentCount(unit) }, (_, index) => index)
    .filter((index) => isFragmentInFront(index, seconds) === inFront)
    .map((index) => fragmentPose(unit, centreX, centreY, index, seconds));
  const glowSize = fragmentSize * THREAT_GLOW_SIZE_MULT;
  for (const fragment of fragments) {
    const placement = { centerX: screenX(viewport, fragment.treeX), centerY: screenY(viewport, fragment.treeY), spriteWidth: glowSize, spriteHeight: glowSize, angleDegrees: 0 };
    drawTexture(context, glowTexture, placement, THREAT_GLOW_TINT, THREAT_GLOW_ALPHA, 'additive');
  }
  for (const fragment of fragments) {
    const placement = { centerX: screenX(viewport, fragment.treeX), centerY: screenY(viewport, fragment.treeY), spriteWidth: fragmentSize, spriteHeight: fragmentSize, angleDegrees: fragment.spinDegrees };
    drawTexture(context, fragmentTexture, placement, WHITE, 1, 'normal');
  }
}

export function paintThreatUnit(context: PaintContext, unit: ThreatUnitDecoration): void {
  const { viewport, seconds } = context;
  const centre = threatUnitDriftedCentre(unit, seconds);
  const centerX = screenX(viewport, centre.treeX);
  const centerY = screenY(viewport, centre.treeY);
  if (!isOnCanvas(viewport, centerX, centerY, threatSwarmReach(unit) * viewport.zoom)) {
    return;
  }
  paintSwarm(context, unit, centre.treeX, centre.treeY, false);
  const hull = context.gl.texture(threatUnitSprite(unit.unitType).spritePath);
  if (hull) {
    const aspect = hull.imageHeight <= 0 ? 1 : hull.imageWidth / hull.imageHeight;
    const placement = {
      centerX,
      centerY,
      spriteWidth: unit.length * aspect * viewport.zoom,
      spriteHeight: unit.length * viewport.zoom,
      angleDegrees: -((unit.rotation || 0) + threatUnitSwayDegrees(seconds)),
    };
    drawTexture(context, hull, placement, WHITE, 1, 'normal');
  }
  paintSwarm(context, unit, centre.treeX, centre.treeY, true);
}

interface MawPlacement {
  centerX: number;
  centerY: number;
  screenLength: number;
  angleDegrees: number;
  cos: number;
  sin: number;
}

function mawPointX(placement: MawPlacement, side: number, forward: number): number {
  return placement.centerX + (side * placement.cos - forward * placement.sin) * placement.screenLength;
}

function mawPointY(placement: MawPlacement, side: number, forward: number): number {
  return placement.centerY + (side * placement.sin + forward * placement.cos) * placement.screenLength;
}

interface WarpedMesh {
  texture: SceneryTexture;
  forwardOffset: number;
  warpLengths: number;
  tint: Rgba;
  alphaMult: number;
  additive: boolean;
}

function paintWarped(context: PaintContext, placement: MawPlacement, mesh: WarpedMesh): void {
  const gl = context.gl;
  gl.use(mesh.texture, mesh.additive ? 'additive' : 'normal');
  gl.colour(mesh.tint, mesh.alphaMult);
  const span = MAW_WARP_VERTICES - 1;
  const meshX: number[] = [];
  const meshY: number[] = [];
  for (let column = 0; column <= span; column++) {
    for (let row = 0; row <= span; row++) {
      const vertex = column * MAW_WARP_VERTICES + row;
      const side = column / span - 0.5 + mesh.warpLengths * warpOffset(vertex, 0, context.seconds);
      const forward = (row / span - 0.5) * MAW_BODY_HEIGHT_PER_LENGTH + mesh.forwardOffset + mesh.warpLengths * warpOffset(vertex, 1, context.seconds);
      meshX[vertex] = mawPointX(placement, side, forward);
      meshY[vertex] = mawPointY(placement, side, forward);
    }
  }
  for (let column = 0; column < span; column++) {
    for (let row = 0; row < span; row++) {
      const corners = [
        [column, row],
        [column, row + 1],
        [column + 1, row + 1],
        [column + 1, row],
      ].map(([cornerColumn, cornerRow]) => ({ vertex: cornerColumn * MAW_WARP_VERTICES + cornerRow, texX: cornerColumn / span, texY: cornerRow / span }));
      const [first, second, third, fourth] = corners;
      gl.quadThrough(
        meshX[first.vertex], meshY[first.vertex], first.texX, first.texY,
        meshX[second.vertex], meshY[second.vertex], second.texX, second.texY,
        meshX[third.vertex], meshY[third.vertex], third.texX, third.texY,
        meshX[fourth.vertex], meshY[fourth.vertex], fourth.texX, fourth.texY,
      );
    }
  }
}

function paintPuff(context: PaintContext, placement: MawPlacement, puff: ShroudPuff, sizeMult: number, tint: Rgba, alphaMult: number): void {
  if (puff.puffAlpha <= 0) {
    return;
  }
  const puffSize = puff.puffSize * sizeMult * placement.screenLength;
  const cellShare = 1 / MAW_SHEET_SIDE;
  const cellLeft = (puff.cell % MAW_SHEET_SIDE) * cellShare;
  const cellBottom = Math.floor(puff.cell / MAW_SHEET_SIDE) * cellShare;
  context.gl.colour(tint, alphaMult * puff.puffAlpha);
  context.gl.quadAtCenter(
    mawPointX(placement, puff.side, puff.forward),
    mawPointY(placement, puff.side, puff.forward),
    puffSize,
    puffSize,
    placement.angleDegrees + puff.spinDegrees,
    cellLeft,
    cellBottom,
    cellLeft + cellShare,
    cellBottom + cellShare,
  );
}

export function paintShroudedMaw(context: PaintContext, maw: ShroudedMawDecoration): void {
  const { viewport, seconds, gl } = context;
  const { driftX, driftY } = mawDrift(seconds, maw.length);
  const centerX = screenX(viewport, maw.x + driftX);
  const centerY = screenY(viewport, maw.y + driftY);
  const screenLength = maw.length * viewport.zoom;
  if (!isOnCanvas(viewport, centerX, centerY, screenLength * MAW_REACH_LENGTHS)) {
    return;
  }
  const angleDegrees = -((maw.rotation || 0) + mawSwayDegrees(seconds));
  const angle = toRadians(angleDegrees);
  const placement: MawPlacement = { centerX, centerY, screenLength, angleDegrees, cos: Math.cos(angle), sin: Math.sin(angle) };

  const body = gl.texture(MAW_BODY_SPRITE_PATH);
  if (body) {
    paintWarped(context, placement, { texture: body, forwardOffset: 0, warpLengths: MAW_BODY_WARP_LENGTHS, tint: WHITE, alphaMult: MAW_BODY_ALPHA, additive: false });
  }
  const sheet = gl.texture(MAW_SHROUD_SHEET_PATH);
  if (sheet) {
    gl.use(sheet, 'normal');
    for (let index = 0; index < MAW_SHROUD_PUFFS; index++) {
      paintPuff(context, placement, shroudPuff(index, seconds), 1, MAW_SHROUD_TINT, MAW_SHROUD_ALPHA);
    }
    for (let index = 0; index < MAW_DARK_PUFFS; index++) {
      paintPuff(context, placement, darkPuff(index, seconds), 1, MAW_DARKNESS_TINT, MAW_DARK_ALPHA);
    }
  }
  const glow = gl.texture(MAW_GLOW_SPRITE_PATH);
  if (glow) {
    paintWarped(context, placement, { texture: glow, forwardOffset: MAW_GLOW_FORWARD_LENGTHS, warpLengths: MAW_GLOW_WARP_LENGTHS, tint: MAW_PART_GLOW_TINT, alphaMult: mawGlowAlpha(seconds), additive: true });
  }
  const spots = gl.texture(MAW_SPOTS_SPRITE_PATH);
  if (spots) {
    const spotsPlacement = {
      centerX: mawPointX(placement, 0, MAW_SPOTS_FORWARD_LENGTHS),
      centerY: mawPointY(placement, 0, MAW_SPOTS_FORWARD_LENGTHS),
      spriteWidth: MAW_SPOTS_WIDTH_LENGTHS * screenLength,
      spriteHeight: MAW_SPOTS_HEIGHT_LENGTHS * screenLength,
      angleDegrees,
    };
    drawTexture(context, spots, spotsPlacement, MAW_PART_GLOW_TINT, mawSpotsAlpha(seconds), 'additive');
  }
  if (sheet) {
    gl.use(sheet, 'additive');
    for (let index = 0; index < MAW_SHROUD_PUFFS; index++) {
      const puff = shroudPuff(index, seconds);
      if (puff.flash > 0) {
        paintPuff(context, placement, puff, MAW_FLASH_SIZE_MULT, MAW_SHROUD_FLASH_TINT, puff.flash);
      }
    }
  }
}
