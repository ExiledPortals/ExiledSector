import { rgb, sceneryHash, TWO_PI, wrapInto, type Rgba } from './sceneryMaths.ts';

export const STARFIELD_SPRITE_PATHS = [
  'graphics/backgrounds/star0.png',
  'graphics/backgrounds/star1.png',
  'graphics/backgrounds/star2.png',
  'graphics/backgrounds/star3.png',
];

export const STARFIELD_ACCENT_COLOUR: Rgba = rgb(120, 200, 255);
export const STARFIELD_FIELD_SCALE = 1.2;

const REFERENCE_AREA = 1920 * 1080;
const MIN_STARS_PER_LAYER = 4;

interface StarLayer {
  baseCount: number;
  parallax: number;
  baseSize: number;
  baseAlpha: number;
}

const STAR_LAYERS: StarLayer[] = [
  { baseCount: 70, parallax: 0.15, baseSize: 8, baseAlpha: 0.45 },
  { baseCount: 45, parallax: 0.3, baseSize: 12, baseAlpha: 0.65 },
  { baseCount: 25, parallax: 0.5, baseSize: 18, baseAlpha: 0.9 },
];

export interface FieldStar {
  spriteIndex: number;
  fieldShareX: number;
  fieldShareY: number;
  parallax: number;
  spriteSize: number;
  baseAlpha: number;
  twinkleSpeed: number;
  twinklePhase: number;
  accent: boolean;
}

export interface Starfield {
  fieldWidth: number;
  fieldHeight: number;
  stars: FieldStar[];
}

function layerStar(layer: StarLayer, layerIndex: number, starIndex: number): FieldStar {
  const roll = (salt: number) => sceneryHash(starIndex, layerIndex, salt);
  return {
    spriteIndex: Math.min(STARFIELD_SPRITE_PATHS.length - 1, Math.floor(roll(1) * STARFIELD_SPRITE_PATHS.length)),
    fieldShareX: roll(2),
    fieldShareY: roll(3),
    parallax: layer.parallax,
    spriteSize: layer.baseSize * (0.75 + roll(4) * 0.5),
    baseAlpha: layer.baseAlpha * (0.8 + roll(5) * 0.2),
    twinkleSpeed: 0.5 + roll(6),
    twinklePhase: roll(7) * TWO_PI,
    accent: roll(8) < 0.5,
  };
}

export function createStarfield(canvasWidth: number, canvasHeight: number): Starfield {
  const fieldWidth = Math.max(1, canvasWidth * STARFIELD_FIELD_SCALE);
  const fieldHeight = Math.max(1, canvasHeight * STARFIELD_FIELD_SCALE);
  const areaScale = (fieldWidth * fieldHeight) / REFERENCE_AREA;
  const stars = STAR_LAYERS.flatMap((layer, layerIndex) =>
    Array.from({ length: Math.max(MIN_STARS_PER_LAYER, Math.round(layer.baseCount * areaScale)) }, (_, starIndex) => layerStar(layer, layerIndex, starIndex)),
  );
  return { fieldWidth, fieldHeight, stars };
}

export interface PlacedFieldStar {
  fieldX: number;
  fieldY: number;
  alpha: number;
}

export function placeFieldStar(starfield: Starfield, star: FieldStar, gamePanX: number, gamePanY: number, seconds: number): PlacedFieldStar {
  const twinkle = 0.6 + 0.4 * Math.sin(seconds * star.twinkleSpeed + star.twinklePhase);
  return {
    fieldX: wrapInto(star.fieldShareX * starfield.fieldWidth + gamePanX * star.parallax, starfield.fieldWidth),
    fieldY: wrapInto(star.fieldShareY * starfield.fieldHeight + gamePanY * star.parallax, starfield.fieldHeight),
    alpha: star.baseAlpha * twinkle,
  };
}
