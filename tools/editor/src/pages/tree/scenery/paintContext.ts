import type { SceneryData } from '../../../api/scenery.ts';
import type { SceneryBlend, SceneryGl, SceneryTexture } from './sceneryGl.ts';
import type { Rgba } from './sceneryMaths.ts';
import type { BandPair } from './radialBands.ts';
import type { SceneryViewport } from './sceneryViewport.ts';

export interface PaintContext {
  gl: SceneryGl;
  viewport: SceneryViewport;
  seconds: number;
  sceneryData: SceneryData | undefined;
}

export interface SpritePlacement {
  centerX: number;
  centerY: number;
  spriteWidth: number;
  spriteHeight: number;
  angleDegrees: number;
}

export function drawSprite(context: PaintContext, spritePath: string, placement: SpritePlacement, tint: Rgba, alphaMult: number, blend: SceneryBlend): void {
  const texture = context.gl.texture(spritePath);
  if (texture) {
    drawTexture(context, texture, placement, tint, alphaMult, blend);
  }
}

export function drawTexture(context: PaintContext, texture: SceneryTexture, placement: SpritePlacement, tint: Rgba, alphaMult: number, blend: SceneryBlend): void {
  if (alphaMult <= 0) {
    return;
  }
  context.gl.use(texture, blend);
  context.gl.colour(tint, alphaMult);
  context.gl.quadAtCenter(placement.centerX, placement.centerY, placement.spriteWidth, placement.spriteHeight, placement.angleDegrees);
}

export interface BandPlacement {
  centerX: number;
  centerY: number;
  scale: number;
  tint: Rgba;
  alphaMult: number;
  blend: SceneryBlend;
}

export function drawBand(context: PaintContext, texture: SceneryTexture, band: BandPlacement, pairs: readonly BandPair[]): void {
  if (band.alphaMult <= 0) {
    return;
  }
  const gl = context.gl;
  gl.use(texture, band.blend);
  gl.colour(band.tint, band.alphaMult);
  gl.beginStrip();
  for (const pair of pairs) {
    gl.stripPair(
      band.centerX + pair.innerX * band.scale,
      band.centerY + pair.innerY * band.scale,
      pair.innerTexX,
      pair.innerTexY,
      band.centerX + pair.outerX * band.scale,
      band.centerY + pair.outerY * band.scale,
      pair.outerTexX,
      pair.outerTexY,
    );
  }
}
