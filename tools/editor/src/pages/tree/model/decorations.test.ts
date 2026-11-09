import { describe, expect, it } from 'vitest';
import {
  DECORATION_KINDS,
  decorationHandles,
  decorationReach,
  dragDecorationHandle,
  parseDecorations,
  serializeDecoration,
  type Decoration,
  type DecorationHandle,
  type HypershuntDecoration,
  type SerpentDecoration,
} from './decorations.ts';

const CONTEXT = { imagePath: null, region: 'north' };

function handleNamed(decoration: Decoration, name: string): DecorationHandle {
  const handle = decorationHandles(decoration).find((candidate) => candidate.name === name);
  if (!handle) {
    throw new Error(`no ${name} handle`);
  }
  return handle;
}

function handleGap(decoration: Decoration, first: string, second: string): number {
  const firstHandle = handleNamed(decoration, first);
  const secondHandle = handleNamed(decoration, second);
  return Math.hypot(firstHandle.x - secondHandle.x, firstHandle.y - secondHandle.y);
}

function expectPoint(handle: DecorationHandle, x: number, y: number): void {
  expect(handle.x).toBeCloseTo(x);
  expect(handle.y).toBeCloseTo(y);
}

function newSerpent(): SerpentDecoration {
  return DECORATION_KINDS.serpent.create('serpent_1', { x: 100, y: 200 }, CONTEXT);
}

function newHypershunt(): HypershuntDecoration {
  return DECORATION_KINDS.hypershunt.create('hypershunt_1', { x: 100, y: 200 }, CONTEXT);
}

describe('orbit decorations', () => {
  it('keeps the radius handle apart from the start handle so both can be dragged', () => {
    for (const orbit of [newSerpent(), newHypershunt()]) {
      for (const startAngle of [0, 90, 213]) {
        orbit.startAngle = startAngle;
        expect(handleGap(orbit, 'radius', 'start'), `${orbit.kind} at ${startAngle}`).toBeGreaterThan(orbit.orbitRadius);
      }
    }
  });

  it('drags the radius without moving the start angle, and the start angle without changing the radius', () => {
    for (const orbit of [newSerpent(), newHypershunt()]) {
      expectPoint(handleNamed(orbit, 'radius'), orbit.x - orbit.orbitRadius, orbit.y);
      dragDecorationHandle(orbit, 'radius', { x: orbit.x - 2000, y: orbit.y });
      expect([orbit.orbitRadius, orbit.startAngle]).toEqual([2000, 0]);
      dragDecorationHandle(orbit, 'start', { x: orbit.x, y: orbit.y + 50 });
      expect([orbit.orbitRadius, orbit.startAngle]).toEqual([2000, 90]);
    }
  });

  it('resizes the hypershunt from its own handle', () => {
    const hypershunt = newHypershunt();
    expect(handleGap(hypershunt, 'size', 'start')).toBeCloseTo(hypershunt.size / 2);
    expect(handleGap(hypershunt, 'size', 'radius')).toBeGreaterThan(hypershunt.orbitRadius);
    dragDecorationHandle(hypershunt, 'size', { x: hypershunt.x + hypershunt.orbitRadius + 200, y: hypershunt.y });
    expect(hypershunt.size).toBe(400);
    expect(hypershunt.orbitRadius).toBe(1100);
  });

  it('reads a missing clockwise flag per kind and writes serpents always, hypershunts only when clockwise', () => {
    const [serpent, hypershunt] = parseDecorations([
      { kind: 'serpent', id: 's', x: 1, y: 2, orbitRadius: 10, orbitSeconds: 20 },
      { kind: 'hypershunt', id: 'h', x: 1, y: 2, orbitRadius: 10, orbitSeconds: 20, size: 300 },
    ], 'ship_skill_tree.json');
    expect(serpent).toMatchObject({ clockwise: true, startAngle: 0 });
    expect(hypershunt).toMatchObject({ clockwise: false, startAngle: 0 });

    expect(serializeDecoration(serpent)).toEqual({ kind: 'serpent', id: 's', x: 1, y: 2, orbitRadius: 10, orbitSeconds: 20, clockwise: true });
    expect(serializeDecoration({ ...(serpent as SerpentDecoration), clockwise: false, startAngle: 45 })).toEqual({
      kind: 'serpent',
      id: 's',
      x: 1,
      y: 2,
      orbitRadius: 10,
      orbitSeconds: 20,
      clockwise: false,
      startAngle: 45,
    });
    expect(Object.keys(serializeDecoration(hypershunt))).toEqual(['kind', 'id', 'x', 'y', 'orbitRadius', 'orbitSeconds', 'size']);
    expect(Object.keys(serializeDecoration({ ...(hypershunt as HypershuntDecoration), clockwise: true, startAngle: 30 }))).toEqual([
      'kind',
      'id',
      'x',
      'y',
      'orbitRadius',
      'orbitSeconds',
      'clockwise',
      'startAngle',
      'size',
    ]);
  });
});

describe('decoration creation and reach', () => {
  it('creates every kind at the point, tagged with the region', () => {
    for (const spec of Object.values(DECORATION_KINDS)) {
      expect(spec.create(`${spec.idPrefix}_1`, { x: 5, y: 6 }, CONTEXT)).toMatchObject({ kind: spec.kind, id: `${spec.idPrefix}_1`, x: 5, y: 6, tags: ['north'] });
    }
    expect(DECORATION_KINDS.star.create('star_1', { x: 0, y: 0 }, { imagePath: null, region: undefined }).tags).toBeUndefined();
  });

  it('measures how far each decoration reaches from its centre', () => {
    expect(decorationReach(DECORATION_KINDS.ringBelt.create('belt', { x: 0, y: 0 }, CONTEXT))).toBe(1700);
    expect(decorationReach(newHypershunt())).toBe(1100 + 150);
    const serpent = newSerpent();
    expect(decorationReach(serpent)).toBeCloseTo(1500 * 1.12);
  });
});

describe('length and rotation handles', () => {
  it('puts the resize handle along the length and the rotate handle past the top edge', () => {
    const unit = DECORATION_KINDS.threatUnit.create('unit', { x: 0, y: 0 }, CONTEXT);
    expectPoint(handleNamed(unit, 'resize'), 0, 300);
    expectPoint(handleNamed(unit, 'rotate'), 0, -324);
    const maw = DECORATION_KINDS.shroudedMaw.create('maw', { x: 0, y: 0 }, CONTEXT);
    expectPoint(handleNamed(maw, 'resize'), 310, 0);
    dragDecorationHandle(maw, 'resize', { x: 0, y: 400 });
    expect(maw.length).toBe(800);
    dragDecorationHandle(maw, 'rotate', { x: 100, y: 0 });
    expect(maw.rotation).toBe(90);
  });
});
