import type { PlanetSpec } from '../../../api/scenery.ts';
import type { StarDecoration } from '../model/decorations.ts';
import { drawBand, drawSprite, type PaintContext } from './paintContext.ts';
import {
  ATMOSPHERE_INNER_INSET_MULT,
  atmosphereStrip,
  AURORA_BAND_ALPHA,
  AURORA_DEGREES_PER_SECOND,
  auroraReach,
  auroraStrips,
} from './radialBands.ts';
import type { SceneryDepth } from './sceneryDepth.ts';
import { normalizeAngle, rgbaFromChannels, starTinted, WHITE, type Rgba } from './sceneryMaths.ts';
import { gridIndex, projectSphere, sphereOrientation, type ProjectedSphere } from './sphereMesh.ts';
import { isOnCanvas, screenX, screenY } from './sceneryViewport.ts';

const FALLBACK_STAR_TYPE = 'star_yellow';
const RIM_ALPHA_MULT = 0.37;
const RIM_STEP_PER_ZOOM = 0.25;
const AURORA_DETAIL_ZOOM = 2.5;

function specOf(context: PaintContext, starType: string): PlanetSpec | undefined {
  const specs = context.sceneryData?.planetSpecs;
  return specs?.[starType] ?? specs?.[FALLBACK_STAR_TYPE];
}

function starColour(star: StarDecoration, channels: number[] | undefined): Rgba {
  return starTinted(star.color, rgbaFromChannels(channels, WHITE));
}

function drawSphere(context: PaintContext, sphere: ProjectedSphere, centerX: number, centerY: number, radius: number): void {
  const { detail, unitX, unitY, texS, texT, quadShown } = sphere;
  for (let stack = 0; stack < detail; stack++) {
    for (let slice = 0; slice < detail; slice++) {
      if (!quadShown[stack * detail + slice]) {
        continue;
      }
      const first = gridIndex(detail, stack, slice);
      const second = gridIndex(detail, stack + 1, slice);
      const third = gridIndex(detail, stack + 1, slice + 1);
      const fourth = gridIndex(detail, stack, slice + 1);
      context.gl.quadThrough(
        centerX + unitX[first] * radius, centerY + unitY[first] * radius, texS[first], texT[first],
        centerX + unitX[second] * radius, centerY + unitY[second] * radius, texS[second], texT[second],
        centerX + unitX[third] * radius, centerY + unitY[third] * radius, texS[third], texT[third],
        centerX + unitX[fourth] * radius, centerY + unitY[fourth] * radius, texS[fourth], texT[fourth],
      );
    }
  }
}

function paintDisc(context: PaintContext, star: StarDecoration, spec: PlanetSpec): void {
  const texture = spec.texture ? context.gl.texture(spec.texture, 'repeat') : null;
  const { viewport } = context;
  const centerX = screenX(viewport, star.x);
  const centerY = screenY(viewport, star.y);
  const discRadius = star.radius * viewport.zoom;
  if (!texture || !isOnCanvas(viewport, centerX, centerY, discRadius + 0.5 * viewport.zoom)) {
    return;
  }
  const spinDegrees = normalizeAngle((spec.rotation ?? 0) * context.seconds);
  const sphere = projectSphere(sphereOrientation(spec.tilt ?? 0, spec.pitch ?? 0, spinDegrees));
  const colour = starColour(star, spec.planetColor);
  const rimStep = RIM_STEP_PER_ZOOM * viewport.zoom;
  context.gl.use(texture, 'normal');
  context.gl.colour(colour, 1);
  drawSphere(context, sphere, centerX, centerY, discRadius);
  context.gl.colour(colour, RIM_ALPHA_MULT);
  drawSphere(context, sphere, centerX, centerY, discRadius + rimStep);
  drawSphere(context, sphere, centerX, centerY, discRadius + rimStep * 2);
}

function paintOverlay(context: PaintContext, star: StarDecoration, spec: PlanetSpec): void {
  const texture = star.overlayPath ? context.gl.texture(star.overlayPath) : null;
  if (!texture || texture.imageWidth <= 0) {
    return;
  }
  const { viewport } = context;
  const centerX = screenX(viewport, star.x);
  const centerY = screenY(viewport, star.y);
  const overlayWidth = star.radius * 2 * (star.overlaySize ?? 1) * viewport.zoom;
  const overlayHeight = (overlayWidth * texture.imageHeight) / texture.imageWidth;
  if (isOnCanvas(viewport, centerX, centerY, Math.max(overlayWidth, overlayHeight) / 2)) {
    context.gl.use(texture, 'normal');
    context.gl.colour(starColour(star, spec.planetColor), star.overlayAlpha ?? 1);
    context.gl.quadAtCenter(centerX, centerY, overlayWidth, overlayHeight, 0);
  }
}

function paintAtmosphere(context: PaintContext, star: StarDecoration, spec: PlanetSpec): void {
  const atmosphereThickness = spec.atmosphereThickness ?? 0;
  const texture = context.sceneryData && atmosphereThickness > 0 ? context.gl.texture(context.sceneryData.sprites.atmosphere) : null;
  if (!texture) {
    return;
  }
  const { viewport } = context;
  const thickness = Math.max(star.radius * atmosphereThickness, spec.atmosphereThicknessMin ?? 0) * viewport.zoom;
  const innerRadius = star.radius * viewport.zoom - thickness * ATMOSPHERE_INNER_INSET_MULT;
  const outerRadius = innerRadius + thickness;
  const centerX = screenX(viewport, star.x);
  const centerY = screenY(viewport, star.y);
  if (thickness > 0 && isOnCanvas(viewport, centerX, centerY, outerRadius)) {
    drawBand(context, texture, { centerX, centerY, scale: 1, tint: starColour(star, spec.atmosphereColor), alphaMult: 1, blend: 'additive' }, atmosphereStrip(innerRadius, outerRadius));
  }
}

function paintAurora(context: PaintContext, star: StarDecoration, spec: PlanetSpec): void {
  const texture = context.sceneryData ? context.gl.texture(context.sceneryData.sprites.aurora, 'repeat') : null;
  if (!texture) {
    return;
  }
  const { viewport } = context;
  const detailRadius = star.radius * AURORA_DETAIL_ZOOM;
  const scale = viewport.zoom / AURORA_DETAIL_ZOOM;
  const centerX = screenX(viewport, star.x);
  const centerY = screenY(viewport, star.y);
  if (!isOnCanvas(viewport, centerX, centerY, auroraReach(detailRadius) * scale)) {
    return;
  }
  const tint = { ...starColour(star, spec.starCoronaColor), alpha: AURORA_BAND_ALPHA };
  const phaseAngle = normalizeAngle(AURORA_DEGREES_PER_SECOND * context.seconds);
  for (const strip of auroraStrips(detailRadius, phaseAngle, texture.imageWidth, texture.imageHeight)) {
    drawBand(context, texture, { centerX, centerY, scale, tint, alphaMult: 1, blend: 'additive' }, strip);
  }
}

function paintGlow(context: PaintContext, star: StarDecoration, spec: PlanetSpec): void {
  if (!spec.starCoronaSprite) {
    return;
  }
  const { viewport } = context;
  const starRadius = star.radius * viewport.zoom;
  const haloRadius = star.radius * (spec.starCoronaSizeMult ?? 0) * viewport.zoom;
  const centerX = screenX(viewport, star.x);
  const centerY = screenY(viewport, star.y);
  if (haloRadius > starRadius && isOnCanvas(viewport, centerX, centerY, haloRadius)) {
    const placement = { centerX, centerY, spriteWidth: haloRadius * 2, spriteHeight: haloRadius * 2, angleDegrees: 0 };
    drawSprite(context, spec.starCoronaSprite, placement, starColour(star, spec.starCoronaColor), 1, 'additive');
  }
}

export function paintStar(context: PaintContext, star: StarDecoration, depth: SceneryDepth): void {
  const spec = specOf(context, star.starType);
  if (!spec) {
    return;
  }
  if (depth === 'starDisc') {
    paintDisc(context, star, spec);
    paintOverlay(context, star, spec);
  } else if (depth === 'starAtmosphere') {
    paintAtmosphere(context, star, spec);
  } else if (depth === 'starAurora') {
    paintAurora(context, star, spec);
  } else if (depth === 'starGlow') {
    paintGlow(context, star, spec);
  }
}
