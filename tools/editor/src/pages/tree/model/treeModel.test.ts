import { produce } from 'immer';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import {
  connectNodes,
  curveKey,
  deleteNodes,
  findCurve,
  isConnectorHidden,
  pairWormholes,
  renameNode,
  setCurveControl,
  snapCurveToStraightIfClose,
  toggleConnectorHidden,
  unlinkNodes,
} from './connectors.ts';
import {
  anchorShapeVertices,
  computeAlignmentSnap,
  countNodesWithinRadius,
  deleteAnchor,
  findSnapAnchor,
  positionOnAnchor,
  resyncNodesOnAnchor,
  setAnchorShape,
  snapAngleToTargets,
  snapNodeToAnchor,
} from './geometry.ts';
import { serializeUnlockConditions } from './serialization.ts';
import { replaceGroupTags } from './tags.ts';
import { nodeById, type SkillType, type TreeDocument, type TreeNode } from './types.ts';
import { validateTree } from './validation.ts';

const vocabularyFile = {
  effects: [{ name: 'HULL_PERCENT', statMode: 'PERCENT', lowerIsBetter: false }],
  tags: { theme: ['armor'], region: ['north', 'south'], requirement: ['req_civilian_hull'] },
  tiers: ['SMALL', 'NOTABLE', 'KEYSTONE', 'WORMHOLE', 'WORMHOLE_EXIT', 'ROOT', 'SOCKET'],
  unlockConditionTypes: ['blueprint'],
  blueprintCategories: ['hullmod'],
  itemTypes: [],
  itemKinds: [],
  itemUnlocks: [],
};
const vocabulary = buildVocabulary(vocabularyFile);

function node(id: string, x: number, y: number, connectedTo: string[] = [], extra: Partial<TreeNode> = {}): TreeNode {
  return { id, type: 'small', connectedTo, x, y, tags: ['north'], ...extra };
}

function makeDocument(nodes: TreeNode[], extra: Partial<TreeDocument> = {}): TreeDocument {
  return {
    types: [
      { id: 'small', name: 'Small', icon: '' },
      { id: 'hole', name: 'Hole', icon: '', tier: 'WORMHOLE' },
      { id: 'exit', name: 'Exit', icon: '', tier: 'WORMHOLE_EXIT' },
    ],
    nodes,
    curves: [],
    hiddenConnectors: [],
    anchors: [],
    images: [],
    stars: [],
    threatUnits: [],
    ...extra,
  };
}

describe('anchor geometry', () => {
  it('places points on circles and polygon edges', () => {
    const circle = { id: 'c', x: 100, y: 50, radius: 10 };
    const onCircle = positionOnAnchor(circle, 90);
    expect(onCircle.x).toBeCloseTo(100);
    expect(onCircle.y).toBeCloseTo(60);
    const square = { id: 's', x: 0, y: 0, radius: Math.SQRT2 * 10, shape: 'square' as const };
    expect(anchorShapeVertices(square)).toHaveLength(4);
    const onSquare = positionOnAnchor(square, 0);
    expect(onSquare.x).toBeCloseTo(10);
    expect(onSquare.y).toBeCloseTo(0);
    expect(anchorShapeVertices(circle)).toBeNull();
  });

  it('snaps angles near cardinal targets only', () => {
    expect(snapAngleToTargets(93, [0, 90, 180, 270])).toBe(90);
    expect(snapAngleToTargets(-2, [0, 90])).toBe(0);
    expect(snapAngleToTargets(80, [0, 90])).toBe(80);
  });

  it('snaps a dragged node onto a nearby ring and arcs connectors along it', () => {
    const anchor = { id: 'ring', x: 0, y: 0, radius: 100 };
    const document = makeDocument([node('a', 100, 0, ['b'], { orbitAnchorId: 'ring', orbitAngle: 0 }), node('b', 0, 0, ['a'])], { anchors: [anchor] });
    const next = produce(document, (draft) => {
      const moving = nodeById(draft, 'b')!;
      const target = findSnapAnchor(draft, moving, 3, 105);
      expect(target?.id).toBe('ring');
      snapNodeToAnchor(draft, moving, target!, 3, 105);
    });
    const snapped = nodeById(next, 'b')!;
    expect(snapped.orbitAnchorId).toBe('ring');
    expect(snapped.orbitAngle).toBe(90);
    expect([snapped.x, snapped.y]).toEqual([0, 100]);
    const curve = findCurve(next, 'a', 'b')!;
    expect(Math.hypot(curve.controlX, curve.controlY)).toBeCloseTo(100, 0);
  });

  it('keeps nodes on an anchor when it moves or changes shape, and frees them when it is deleted', () => {
    const anchor = { id: 'ring', x: 0, y: 0, radius: 100 };
    const document = makeDocument([node('a', 100, 0, [], { orbitAnchorId: 'ring', orbitAngle: 0 })], { anchors: [anchor] });
    const moved = produce(document, (draft) => {
      draft.anchors[0].x = 50;
      resyncNodesOnAnchor(draft, draft.anchors[0]);
    });
    expect(nodeById(moved, 'a')!.x).toBe(150);
    const reshaped = produce(moved, (draft) => {
      setAnchorShape(draft.anchors[0], 'rectangle');
    });
    expect(reshaped.anchors[0].aspectRatio).toBe(1.6);
    const circleAgain = produce(reshaped, (draft) => setAnchorShape(draft.anchors[0], 'circle'));
    expect(circleAgain.anchors[0].shape).toBeUndefined();
    const removed = produce(moved, (draft) => deleteAnchor(draft, 'ring'));
    expect(removed.anchors).toHaveLength(0);
    expect(nodeById(removed, 'a')!.orbitAnchorId).toBeUndefined();
  });

  it('aligns a node with the line between two neighbours', () => {
    const document = makeDocument([node('m', 0, 0, ['a', 'b']), node('a', -100, 0), node('b', 100, 0)]);
    expect(computeAlignmentSnap(document, document.nodes[0], 30, 4)).toEqual({ x: 30, y: 0 });
    expect(computeAlignmentSnap(document, document.nodes[0], 30, 40)).toBeNull();
  });
});

describe('connectors', () => {
  it('links, hides, curves, renames and unlinks nodes', () => {
    const document = makeDocument([node('a', 0, 0), node('b', 100, 0)]);
    const linked = produce(document, (draft) => {
      connectNodes(draft, draft.nodes[0], 'b');
      toggleConnectorHidden(draft, 'a', 'b');
      setCurveControl(draft, 'b', 'a', 50, 40);
    });
    expect(linked.nodes.map((entry) => entry.connectedTo)).toEqual([['b'], ['a']]);
    expect(isConnectorHidden(linked, 'b', 'a')).toBe(true);
    expect(curveKey('b', 'a')).toBe('a|b');

    const renamed = produce(linked, (draft) => renameNode(draft, draft.nodes[0], 'z'));
    expect(renamed.nodes[1].connectedTo).toEqual(['z']);
    expect(findCurve(renamed, 'z', 'b')).toBeDefined();
    expect(isConnectorHidden(renamed, 'z', 'b')).toBe(true);

    const unlinked = produce(renamed, (draft) => unlinkNodes(draft, draft.nodes[0], 'b'));
    expect(unlinked.nodes.map((entry) => entry.connectedTo)).toEqual([[], []]);
    expect(unlinked.curves).toHaveLength(0);
    expect(unlinked.hiddenConnectors).toHaveLength(0);
  });

  it('drops a curve that was dragged back close to straight', () => {
    const document = makeDocument([node('a', 0, 0, ['b']), node('b', 100, 0, ['a'])], { curves: [{ a: 'a', b: 'b', controlX: 52, controlY: 3 }] });
    expect(produce(document, (draft) => snapCurveToStraightIfClose(draft, 'a', 'b')).curves).toHaveLength(0);
  });

  it('deletes nodes with everything that refers to them', () => {
    const document = makeDocument(
      [node('a', 0, 0, ['b'], { type: 'hole', pairedWith: 'b' }), node('b', 100, 0, ['a'], { type: 'hole', pairedWith: 'a' })],
      { curves: [{ a: 'a', b: 'b', controlX: 1, controlY: 1 }], hiddenConnectors: [{ a: 'a', b: 'b' }] },
    );
    const next = produce(document, (draft) => deleteNodes(draft, ['b']));
    expect(next.nodes).toHaveLength(1);
    expect(next.nodes[0].connectedTo).toEqual([]);
    expect(next.nodes[0].pairedWith).toBeUndefined();
    expect(next.curves).toHaveLength(0);
    expect(next.hiddenConnectors).toHaveLength(0);
  });

  it('pairs wormholes both ways and releases old partners', () => {
    const document = makeDocument([node('a', 0, 0, [], { type: 'hole' }), node('b', 1, 0, [], { type: 'hole' }), node('c', 2, 0, [], { type: 'hole' })]);
    const paired = produce(document, (draft) => pairWormholes(draft, draft.nodes[0], 'b'));
    expect(paired.nodes.map((entry) => entry.pairedWith)).toEqual(['b', 'a', undefined]);
    const repaired = produce(paired, (draft) => pairWormholes(draft, draft.nodes[2], 'b'));
    expect(repaired.nodes.map((entry) => entry.pairedWith)).toEqual([undefined, 'c', 'b']);
    expect(validateTree(repaired, vocabulary)).toEqual(['Wormhole node "a" has no paired wormhole set']);
  });
});

describe('validation and tags', () => {
  it('reports broken references, wormholes and tag problems', () => {
    const document = makeDocument([
      node('a', 0, 0, ['ghost'], { type: 'missing' }),
      node('a', 0, 0, [], { tags: [] }),
      node('w', 0, 0, [], { type: 'hole', tags: ['north', 'south', 'bogus'] }),
    ]);
    document.types[0].tags = ['north'];
    document.types[0].effects = [{ effect: 'NOPE', magnitude: 1 }];
    document.types[0].icon = 'graphics/unused/x.png';
    expect(validateTree(document, vocabulary)).toEqual([
      'Duplicate node id: a',
      'Node "a" uses unknown type "missing"',
      'Node "a" connection "ghost" does not exist',
      'Wormhole node "w" has no paired wormhole set',
      'Type "small" icon graphics/unused/x.png is in a folder that is not deployed',
      'Type "small" uses unknown effect "NOPE"',
      'Node "a" has no region tag',
      'Node "w" has 2 region tags (north, south), needs exactly one',
      'Node "w" has unknown tag "bogus"',
      'Type "small" carries region tag "north", region tags belong on nodes',
    ]);
    expect(validateTree(makeDocument([node('a', 0, 0)]), vocabulary)).toEqual([]);
  });

  it('accepts a wormhole exit with no partner and flags one that was given a pair', () => {
    expect(validateTree(makeDocument([node('far', 0, 0, [], { type: 'exit' })]), vocabulary)).toEqual([]);
    expect(
      validateTree(makeDocument([node('far', 0, 0, ['near'], { type: 'exit', pairedWith: 'near' }), node('near', 1, 0, ['far'], { type: 'hole', pairedWith: 'far' })]), vocabulary),
    ).toEqual([
      'Wormhole exit "far" is paired with "near"; exits are opened by a wormhole anchor and take no pair',
      'Wormhole node "near" is paired with "far", which is not a wormhole-tier node',
    ]);
  });

  it('flags a node that reuses an id retired in node_replacements.csv', () => {
    const document = makeDocument([node('a', 0, 0), node('old_1', 10, 0)]);
    expect(validateTree(document, vocabulary, new Set(['old_1']))).toEqual([
      'Node id "old_1" was retired in node_replacements.csv; give the node a new id',
    ]);
  });

  it('replaces only the tags of one group and keeps the rest in order', () => {
    expect(replaceGroupTags(vocabulary, ['armor', 'north', 'req_civilian_hull'], 'region', ['south'])).toEqual(['armor', 'req_civilian_hull', 'south']);
    expect(replaceGroupTags(vocabulary, ['bogus', 'armor'], 'unknown', [])).toEqual(['armor']);
  });

  it('keeps parameterless unlock conditions and drops parameterised ones that lost their parameters', () => {
    expect(
      serializeUnlockConditions([
        { type: 'nightcrossPascalVisited' },
        { type: 'characterStat' },
        { type: 'memoryFlag', key: 'someFlag' },
        { type: 'blueprint', id: 'heavyarmor' },
        { type: '' },
      ]),
    ).toEqual([
      { type: 'nightcrossPascalVisited' },
      { type: 'memoryFlag', key: 'someFlag' },
      { type: 'blueprint', category: 'hullmod', id: 'heavyarmor' },
    ]);
  });

  it('reads the parameterless unlock condition list from the vocabulary and tolerates an older file without it', () => {
    expect(vocabulary.parameterlessUnlockConditionTypes).toEqual([]);
    const withGates = buildVocabulary({ ...vocabularyFile, parameterlessUnlockConditionTypes: ['ratPlayerExoship'] });
    expect(withGates.parameterlessUnlockConditionTypes).toEqual(['ratPlayerExoship']);
  });
});

describe('socket radius counts', () => {
  it('counts nodes by tier within the radius, leaving out the centre, the Exoship and ExoTech nodes', () => {
    const typesById = new Map<string, SkillType>([
      ['small_a', { id: 'small_a', name: 'Small', tier: 'SMALL' } as SkillType],
      ['notable_a', { id: 'notable_a', name: 'Notable', tier: 'NOTABLE' } as SkillType],
      ['socket_a', { id: 'socket_a', name: 'Socket', tier: 'SOCKET' } as SkillType],
    ]);
    const node = (id: string, type: string, x: number, y: number, tags?: string[]): TreeNode => ({ id, type, connectedTo: [], x, y, tags });
    const socket = node('socket', 'socket_a', 0, 0);
    const nodes = [
      socket,
      node('near_small', 'small_a', 300, 400),
      node('near_notable', 'notable_a', -100, 0),
      node('far_small', 'small_a', 501, 0),
      node('exoship', 'exoship', 10, 10),
      node('carried', 'small_a', 20, 20, ['exotech']),
    ];

    const counts = countNodesWithinRadius(nodes, typesById, socket, 500);

    expect(Object.fromEntries(counts)).toEqual({ SMALL: 1, NOTABLE: 1 });
  });
});
