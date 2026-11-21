import type { ImageDecoration, RingBeltDecoration } from '../model/decorations.ts';
import { drawBand, drawSprite, type PaintContext } from './paintContext.ts';
import { bandOverlapsRect, ringStrip } from './radialBands.ts';
import { WHITE } from './sceneryMaths.ts';
import { isOnCanvas, screenX, screenY } from './sceneryViewport.ts';

const PARALLAX_REACH_PER_WIDTH = 0.25;

export function parallaxDriftFraction(distanceFromViewCenter: number, parallax: number, reach: number): number {
  if (parallax === 1 || distanceFromViewCenter <= 0 || reach <= 0) {
    return 0;
  }
  const drift = distanceFromViewCenter * (1 - parallax);
  return (reach * Math.tanh(drift / reach)) / distanceFromViewCenter;
}

export function paintImage(context: PaintContext, image: ImageDecoration): void {
  if (!image.imagePath) {
    return;
  }
  const { viewport } = context;
  const imageWidth = image.width * viewport.zoom;
  const imageHeight = image.height * viewport.zoom;
  const viewCenterX = viewport.canvasWidth / 2;
  const viewCenterY = viewport.canvasHeight / 2;
  let centerX = screenX(viewport, image.x);
  let centerY = screenY(viewport, image.y);
  const drift = parallaxDriftFraction(Math.hypot(viewCenterX - centerX, viewCenterY - centerY), image.parallax ?? 1, imageWidth * PARALLAX_REACH_PER_WIDTH);
  centerX += (viewCenterX - centerX) * drift;
  centerY += (viewCenterY - centerY) * drift;
  if (!isOnCanvas(viewport, centerX, centerY, Math.hypot(imageWidth, imageHeight) / 2)) {
    return;
  }
  const angleDegrees = -((image.rotation || 0) + (image.rotationSpeed || 0) * context.seconds);
  drawSprite(context, image.imagePath, { centerX, centerY, spriteWidth: imageWidth, spriteHeight: imageHeight, angleDegrees }, WHITE, 1, 'normal');
}

export function paintRingBelt(context: PaintContext, belt: RingBeltDecoration): void {
  const texture = belt.ringArtPath ? context.gl.texture(belt.ringArtPath, 'repeat') : null;
  const { viewport } = context;
  const innerRadius = belt.innerRadius * viewport.zoom;
  const outerRadius = belt.outerRadius * viewport.zoom;
  const centerX = screenX(viewport, belt.x);
  const centerY = screenY(viewport, belt.y);
  if (!texture || texture.imageWidth <= 0 || outerRadius <= innerRadius || !bandOverlapsRect(centerX, centerY, innerRadius, outerRadius, viewport.canvasWidth, viewport.canvasHeight)) {
    return;
  }
  const rotationDegrees = (belt.rotation || 0) + (belt.rotationSpeed || 0) * context.seconds;
  const pairs = ringStrip(innerRadius, outerRadius, rotationDegrees, texture.imageHeight / texture.imageWidth);
  drawBand(context, texture, { centerX, centerY, scale: 1, tint: WHITE, alphaMult: 1, blend: 'normal' }, pairs);
}
