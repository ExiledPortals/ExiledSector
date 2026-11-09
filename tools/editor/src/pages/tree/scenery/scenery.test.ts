import { describe, expect, it } from 'vitest';
import { DECORATION_KINDS, hypershuntPosition, type Decoration, type HypershuntDecoration, type SerpentDecoration } from '../model/decorations.ts';
import { parallaxDriftFraction } from './imagePainter.ts';
import { hypershuntPose, orbitAngleDegreesAt, SERPENT_PARTS, SERPENT_SCALE, serpentPartPoses } from './orbitPoses.ts';
import { auroraStrips, bandOverlapsRect, quantisedSegments, ringSegmentCount, ringStrip, ringTileCount } from './radialBands.ts';
import { isOverTree, sceneryDrawOrder } from './sceneryDepth.ts';
import { parseHexColour, sceneryHash, starTinted, WHITE, wrapInto } from './sceneryMaths.ts';
import { isOnCanvas, sceneryViewport, screenX, screenY } from './sceneryViewport.ts';
import { darkPuff, shroudPuff } from './shroudPuffs.ts';
import { projectSphere, SPHERE_DETAIL, sphereOrientation } from './sphereMesh.ts';
import { createStarfield, placeFieldStar, STARFIELD_SPRITE_PATHS } from './starfield.ts';
import { fragmentPose, isFragmentInFront, threatFragmentCount } from './threatSwarm.ts';

const CONTEXT = { imagePath: 'graphics/backgrounds/static_images/hyperspace_cloud_01.png', region: undefined };

function created<TKind extends Decoration['kind']>(kind: TKind, id: string, x = 0, y = 0) {
  return DECORATION_KINDS[kind].create(id, { x, y }, CONTEXT);
}

describe('starfield', () => {
  it('scales the star count with the field area and keeps every star inside the wrapped field', () => {
    const small = createStarfield(960, 540);
    const large = createStarfield(1920, 1080);
    expect(large.stars.length).toBeGreaterThan(small.stars.length);
    expect(large.fieldWidth).toBeCloseTo(1920 * 1.2);
    expect(new Set(large.stars.map((star) => star.parallax))).toEqual(new Set([0.15, 0.3, 0.5]));
    expect(large.stars.every((star) => star.spriteIndex >= 0 && star.spriteIndex < STARFIELD_SPRITE_PATHS.length)).toBe(true);
    const accentShare = large.stars.filter((star) => star.accent).length / large.stars.length;
    expect(accentShare).toBeGreaterThan(0.3);
    expect(accentShare).toBeLessThan(0.7);
    for (const star of large.stars) {
      const placed = placeFieldStar(large, star, -12345, 6789, 3.5);
      expect(placed.fieldX).toBeGreaterThanOrEqual(0);
      expect(placed.fieldX).toBeLessThan(large.fieldWidth);
      expect(placed.fieldY).toBeGreaterThanOrEqual(0);
      expect(placed.fieldY).toBeLessThan(large.fieldHeight);
      expect(placed.alpha).toBeLessThanOrEqual(star.baseAlpha);
      expect(placed.alpha).toBeGreaterThanOrEqual(star.baseAlpha * 0.2 - 1e-9);
    }
  });

  it('keeps the same stars when the canvas is resized and moves nearer layers further with the pan', () => {
    const before = createStarfield(1000, 800);
    const after = createStarfield(1100, 800);
    expect(after.stars[0]).toEqual(before.stars[0]);
    const far = before.stars.find((star) => star.parallax === 0.15);
    const near = before.stars.find((star) => star.parallax === 0.5);
    const shiftOf = (star: typeof far) => {
      const resting = placeFieldStar(before, star!, 0, 0, 0);
      const panned = placeFieldStar(before, star!, 100, 0, 0);
      return wrapInto(panned.fieldX - resting.fieldX, before.fieldWidth);
    };
    expect(shiftOf(far)).toBeCloseTo(15);
    expect(shiftOf(near)).toBeCloseTo(50);
  });
});

describe('orbit poses', () => {
  it('starts the hypershunt where the editor handle sits and orbits a quarter turn per quarter period', () => {
    const hypershunt = { ...created('hypershunt', 'shunt', 100, -50), startAngle: 30, orbitSeconds: 400, clockwise: true } as HypershuntDecoration;
    const start = hypershuntPose(hypershunt, 0);
    expect(start.treeX).toBeCloseTo(hypershuntPosition(hypershunt).x);
    expect(start.treeY).toBeCloseTo(hypershuntPosition(hypershunt).y);
    expect(start.spriteAngleDegrees).toBeCloseTo(-120);
    expect(orbitAngleDegreesAt(hypershunt, 100)).toBeCloseTo(120);
    expect(orbitAngleDegreesAt({ ...hypershunt, clockwise: false }, 100)).toBeCloseTo(-60);
    expect(orbitAngleDegreesAt({ ...hypershunt, orbitSeconds: 0 }, 0.5)).toBeCloseTo(210);
  });

  it('spaces the serpent body along its path behind the head', () => {
    const serpent = { ...created('serpent', 'serpent'), wobble: 0 } as SerpentDecoration;
    const poses = serpentPartPoses(serpent, 12);
    expect(poses).toHaveLength(SERPENT_PARTS.length);
    const head = poses[poses.length - 1];
    expect(Math.hypot(head.treeX, head.treeY)).toBeCloseTo(serpent.orbitRadius, 3);
    for (let index = 0; index < poses.length - 1; index++) {
      const chord = Math.hypot(poses[index].treeX - head.treeX, poses[index].treeY - head.treeY);
      expect(chord).toBeLessThanOrEqual(SERPENT_PARTS[index].behindHead * SERPENT_SCALE + 1e-6);
      expect(chord).toBeGreaterThan(SERPENT_PARTS[index].behindHead * SERPENT_SCALE * 0.95);
    }
    const tangent = head.facingDegrees - (Math.atan2(head.treeY, head.treeX) * 180) / Math.PI;
    expect(Math.abs(Math.cos((tangent * Math.PI) / 180))).toBeLessThan(0.01);
  });

  it('stops walking a serpent whose path has collapsed to its centre', () => {
    const collapsed = { ...created('serpent', 'collapsed'), wobble: -1, wobbleWaves: 0, wobblePhase: 90 } as SerpentDecoration;
    expect(serpentPartPoses(collapsed, 0)).toHaveLength(SERPENT_PARTS.length);
  });
});

describe('threat swarm and shroud', () => {
  it('splits the swarm in front of and behind the hull and keeps fragments around it', () => {
    const hive = { ...created('threatUnit', 'hive'), unitType: 'hive' } as Decoration & { kind: 'threatUnit' };
    expect(threatFragmentCount(hive)).toBe(64);
    const inFront = Array.from({ length: 64 }, (_, index) => isFragmentInFront(index, 2)).filter(Boolean).length;
    expect(inFront).toBeGreaterThan(10);
    expect(inFront).toBeLessThan(54);
    const fragment = fragmentPose(hive, 0, 0, 5, 2);
    expect(Math.hypot(fragment.treeX, fragment.treeY)).toBeLessThan(hive.length * 1.1);
  });

  it('hashes into the unit interval the same way every time', () => {
    const rolls = Array.from({ length: 200 }, (_, index) => sceneryHash(index, 3, 7));
    expect(rolls.every((roll) => roll >= 0 && roll < 1)).toBe(true);
    expect(sceneryHash(11, 2, 5)).toBe(sceneryHash(11, 2, 5));
    expect(new Set(rolls.map((roll) => roll.toFixed(6))).size).toBeGreaterThan(190);
    const puff = shroudPuff(4, 9.5);
    expect(puff.cell).toBeGreaterThanOrEqual(0);
    expect(puff.cell).toBeLessThan(16);
    expect(puff.puffAlpha).toBeGreaterThanOrEqual(0);
    expect(darkPuff(4, 9.5).flash).toBe(0);
  });
});

describe('radial bands and the star sphere', () => {
  it('rounds segment counts like the game and tiles whole textures around a belt', () => {
    expect([1, 5, 6, 7, 8, 12, 13, 100].map(quantisedSegments)).toEqual([1, 6, 6, 8, 8, 12, 16, 128]);
    expect(ringSegmentCount(1400, 1700)).toBe(quantisedSegments(Math.round((Math.PI * 3100) / 5)));
    expect(ringTileCount(1400, 1700, 2)).toBe(Math.min(60, Math.round((Math.PI * 3100) / 600)));
    const strip = ringStrip(100, 200, 90, 2);
    expect(strip[0].innerX).toBeCloseTo(0);
    expect(strip[0].innerY).toBeCloseTo(100);
    expect(strip[strip.length - 1].outerTexY).toBeCloseTo(ringTileCount(100, 200, 2));
  });

  it('builds the two counter-rotating aurora bands', () => {
    const strips = auroraStrips(850 * 2.5, 40, 512, 512);
    expect(strips).toHaveLength(2);
    expect(strips[0].length).toBe(strips[1].length);
    expect(strips[0][0].innerTexX).toBeCloseTo(0.5);
    expect(strips[1][0].innerTexX).toBe(0);
    expect(Math.sign(strips[0][0].innerX)).toBe(-Math.sign(strips[1][0].innerX));
  });

  it('culls the sphere to one hemisphere at any spin', () => {
    for (const spin of [0, 45, 200]) {
      const sphere = projectSphere(sphereOrientation(-16, 10, spin));
      const shown = sphere.quadShown.reduce((sum, value) => sum + value, 0);
      expect(shown).toBeGreaterThan(SPHERE_DETAIL * SPHERE_DETAIL * 0.4);
      expect(shown).toBeLessThan(SPHERE_DETAIL * SPHERE_DETAIL * 0.6);
      const reach = Math.max(...sphere.unitX.map(Math.abs), ...sphere.unitY.map(Math.abs));
      expect(reach).toBeLessThanOrEqual(1 + 1e-9);
    }
  });

  it('finds belts that cross the canvas and skips ones that wrap around it', () => {
    expect(bandOverlapsRect(400, 300, 100, 200, 800, 600)).toBe(true);
    expect(bandOverlapsRect(-500, 300, 100, 200, 800, 600)).toBe(false);
    expect(bandOverlapsRect(400, 300, 5000, 6000, 800, 600)).toBe(false);
  });
});

describe('scenery viewport and colours', () => {
  it('puts tree points on the same canvas pixel as the SVG root transform', () => {
    const view = { panX: 120, panY: -40, zoom: 0.5 };
    const viewport = sceneryViewport(view, 800, 600);
    const treePoint = { x: 300, y: 200 };
    const svgX = 800 / 2 + view.panX + treePoint.x * view.zoom;
    const svgY = 600 / 2 + view.panY + treePoint.y * view.zoom;
    expect(screenX(viewport, treePoint.x)).toBeCloseTo(svgX);
    expect(600 - screenY(viewport, treePoint.y)).toBeCloseTo(svgY);
    expect(isOnCanvas(viewport, -10, 300, 20)).toBe(true);
    expect(isOnCanvas(viewport, -30, 300, 20)).toBe(false);
  });

  it('reads star colours like the game, keeping the spec alpha', () => {
    expect(parseHexColour('#00479e', WHITE)).toEqual({ red: 0, green: 0x47, blue: 0x9e, alpha: 255 });
    expect(parseHexColour('80ff0000', WHITE)).toEqual({ red: 255, green: 0, blue: 0, alpha: 0x80 });
    expect(parseHexColour('not a colour', WHITE)).toBe(WHITE);
    expect(starTinted('#ff4900', { red: 1, green: 2, blue: 3, alpha: 60 })).toEqual({ red: 255, green: 0x49, blue: 0, alpha: 60 });
    expect(starTinted(null, { red: 1, green: 2, blue: 3, alpha: 60 })).toEqual({ red: 1, green: 2, blue: 3, alpha: 60 });
  });

  it('drifts parallax images towards the view centre without overshooting', () => {
    expect(parallaxDriftFraction(500, 1, 100)).toBe(0);
    const drift = parallaxDriftFraction(500, 0.5, 100);
    expect(drift).toBeGreaterThan(0);
    expect(drift * 500).toBeLessThanOrEqual(100);
  });
});

describe('scenery draw order', () => {
  it('draws everything behind the tree except star glow, in the game depth order', () => {
    const decorations: Decoration[] = [
      created('hypershunt', 'shunt'),
      created('threatUnit', 'unit'),
      { ...created('image', 'front'), behindStars: false },
      created('star', 'sun'),
      { ...created('image', 'back'), behindStars: true },
      created('ringBelt', 'belt'),
      created('serpent', 'serpent'),
      created('shroudedMaw', 'maw'),
    ];
    const behind = sceneryDrawOrder(decorations, false).map((draw) => `${draw.depth}:${draw.decoration.id}`);
    expect(behind).toEqual([
      'behindStars:back',
      'starDisc:sun',
      'starAtmosphere:sun',
      'starAurora:sun',
      'belts:belt',
      'creatures:serpent',
      'foregroundImages:front',
      'units:unit',
      'units:maw',
      'units:shunt',
    ]);
    expect(sceneryDrawOrder(decorations, true).map((draw) => `${draw.depth}:${draw.decoration.id}`)).toEqual(['starGlow:sun']);
    expect(isOverTree('starGlow')).toBe(true);
    expect(isOverTree('units')).toBe(false);
  });
});
