import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { lineEndingOf, withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';
import { parseTreeDocument, serializeTreeFiles } from './serialization.ts';
import { ATLAS_TREE_FILES, SHIP_TREE_FILES } from './treeFiles.ts';

const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);
const PROJECT_ROOT = path.resolve(import.meta.dirname, '../../../../../..');

function readProjectFile(relativePath: string): string {
  const text = fs.readFileSync(path.join(PROJECT_ROOT, relativePath), 'utf8');
  return text.startsWith(BYTE_ORDER_MARK) ? text.slice(1) : text;
}

const ATLAS_TYPES_JSON = {
  skillTypes: [
    { id: 'atlas_root', name: 'Wormhole Gateway', icon: 'graphics/icons/root.png', tier: 'ROOT' },
    {
      id: 'wider_charts',
      name: 'Wider Charts',
      icon: 'graphics/icons/small.png',
      effects: [
        { effect: 'CHART_FLEET_SIZE_PERCENT', magnitude: 5 },
        { effect: 'CHART_EXTRA_FLEETS', magnitude: 1 },
      ],
      flavour: 'The charts grow.',
      todo: 'Balance the fleet size.',
      tags: ['charts'],
    },
    { id: 'chart_notable', name: 'Notable', icon: 'graphics/icons/notable.png', tier: 'NOTABLE', effects: [{ effect: 'CHART_BOSS_SIZE_PERCENT', magnitude: 10 }] },
    { id: 'chart_keystone', name: 'Keystone', icon: 'graphics/icons/keystone.png', tier: 'KEYSTONE', flavour: 'All or nothing.' },
    { id: 'chart_wormhole', name: 'Wormhole', icon: 'graphics/icons/wormhole.png', tier: 'WORMHOLE' },
  ],
};

const ATLAS_TREE_JSON = {
  volumes: ['crafting', 'hull_items', 'star_charts'],
  nodes: [
    { id: 'atlas_root', type: 'atlas_root', connectedTo: ['wider_charts_1', 'chart_wormhole_1'], x: 0, y: 0, tags: ['star_charts'] },
    {
      id: 'wider_charts_1',
      type: 'wider_charts',
      connectedTo: ['atlas_root', 'chart_notable_1'],
      x: 150,
      y: 0,
      orbitAnchorId: 'anchor_1',
      orbitAngle: 90,
      tags: ['crafting'],
    },
    { id: 'chart_notable_1', type: 'chart_notable', connectedTo: ['wider_charts_1'], x: 300, y: -40, tags: ['crafting'] },
    {
      id: 'chart_keystone_1',
      type: 'chart_keystone',
      connectedTo: [],
      x: -400,
      y: 200,
      ringBeltPath: 'graphics/planets/rings_ice0.png',
      ringBeltColor: '#80c0ff',
      ringBeltWidth: 1.4,
      tags: ['hull_items'],
    },
    { id: 'chart_wormhole_1', type: 'chart_wormhole', connectedTo: ['atlas_root'], x: 0, y: 300, wormholeColor: '#aa66ff', pairedWith: 'chart_wormhole_2', tags: ['star_charts'] },
    { id: 'chart_wormhole_2', type: 'chart_wormhole', connectedTo: [], x: -2000, y: 1200, wormholeColor: '#aa66ff', pairedWith: 'chart_wormhole_1', tags: ['hull_items'] },
  ],
  connectorCurves: [{ a: 'atlas_root', b: 'wider_charts_1', controlX: 70, controlY: -60 }],
  hiddenConnectors: [{ a: 'atlas_root', b: 'chart_wormhole_1' }],
  orbitAnchors: [
    { id: 'anchor_1', x: 150, y: 150, radius: 150 },
    { id: 'anchor_2', x: -400, y: 200, radius: 220, shape: 'rectangle', aspectRatio: 1.8, rotation: 30 },
  ],
  decorations: [
    {
      kind: 'star',
      id: 'crafting_star',
      x: 300,
      y: 0,
      radius: 400,
      starType: 'star_orange',
      color: '#f08a3c',
      overlayPath: 'graphics/fx/crest.png',
      overlaySize: 0.9,
      overlayAlpha: 0.5,
      tags: ['crafting'],
    },
    {
      kind: 'image',
      id: 'crafting_nebula',
      x: 300,
      y: 0,
      width: 3200,
      height: 2400,
      imagePath: 'graphics/backgrounds/nebula.png',
      rotation: 15,
      rotationSpeed: 0.5,
      behindStars: true,
      parallax: 0.8,
      tags: ['crafting'],
    },
    {
      kind: 'ringBelt',
      id: 'hull_belt',
      x: -400,
      y: 200,
      innerRadius: 900,
      outerRadius: 1200,
      ringArtPath: 'graphics/planets/rings_asteroids0.png',
      rotation: 10,
      rotationSpeed: 0.3,
      tags: ['hull_items'],
    },
  ],
};

function fileText(json: unknown, lineEnding: LineEnding): string {
  return withLineEnding(JSON.stringify(json, null, '\t'), lineEnding);
}

function hasMixedLineEndings(text: string): boolean {
  return /\r\n/.test(text) && /(^|[^\r])\n/.test(text);
}

describe('atlas tree files round-trip', () => {
  it.each<[string, LineEnding]>([
    ['LF', '\n'],
    ['CRLF', '\r\n'],
  ])('serialises every atlas field back to the same bytes with %s line endings', (_name, lineEnding) => {
    const typesText = fileText(ATLAS_TYPES_JSON, lineEnding);
    const treeText = fileText(ATLAS_TREE_JSON, lineEnding);
    const document = parseTreeDocument(ATLAS_TREE_FILES, typesText, treeText);
    expect(document.volumes).toEqual(ATLAS_TREE_JSON.volumes);
    expect(document.types.find((type) => type.id === 'wider_charts')?.tags).toEqual(['charts']);
    const texts = serializeTreeFiles(ATLAS_TREE_FILES, document, { types: lineEndingOf(typesText), tree: lineEndingOf(treeText) });
    expect(texts.types).toBe(typesText);
    expect(texts.tree).toBe(treeText);
    expect(hasMixedLineEndings(texts.types) || hasMixedLineEndings(texts.tree)).toBe(false);
  });

  it('keeps an empty volumes list and writes it first', () => {
    const treeText = fileText({ volumes: [], nodes: [{ id: 'atlas_root', type: 'atlas_root', connectedTo: [], x: 0, y: 0 }] }, '\n');
    const document = parseTreeDocument(ATLAS_TREE_FILES, fileText(ATLAS_TYPES_JSON, '\n'), treeText);
    const texts = serializeTreeFiles(ATLAS_TREE_FILES, document, { types: '\n', tree: '\n' });
    expect(texts.tree).toBe(treeText);
    expect(Object.keys(JSON.parse(texts.tree) as Record<string, unknown>)[0]).toBe('volumes');
  });

  it('loads the atlas files in the project and saves them without losing anything', () => {
    const typesText = readProjectFile('data/skilltrees/atlas_tree_types.json');
    const treeText = readProjectFile('data/skilltrees/atlas_tree.json');
    const lineEndings = { types: lineEndingOf(typesText), tree: lineEndingOf(treeText) };
    const document = parseTreeDocument(ATLAS_TREE_FILES, typesText, treeText);
    const texts = serializeTreeFiles(ATLAS_TREE_FILES, document, lineEndings);
    expect(JSON.parse(texts.types)).toEqual(JSON.parse(typesText));
    expect(parseTreeDocument(ATLAS_TREE_FILES, texts.types, texts.tree)).toEqual(document);
    expect(serializeTreeFiles(ATLAS_TREE_FILES, parseTreeDocument(ATLAS_TREE_FILES, texts.types, texts.tree), lineEndings)).toEqual(texts);
    expect(JSON.parse(texts.tree)).toHaveProperty('volumes');
    expect(hasMixedLineEndings(texts.types) || hasMixedLineEndings(texts.tree)).toBe(false);
  });

  it('names the atlas files in load errors and refuses volumes the ship tree would drop', () => {
    const typesText = fileText(ATLAS_TYPES_JSON, '\n');
    expect(() => parseTreeDocument(ATLAS_TREE_FILES, typesText, '{"volumes":[],"nodes":[]}')).toThrow('atlas_tree.json has no nodes');
    expect(() => parseTreeDocument(ATLAS_TREE_FILES, typesText, '{"volumes":"crafting","nodes":[{"id":"a","type":"atlas_root"}]}')).toThrow(/not a list of volume ids/);
    expect(() => parseTreeDocument(ATLAS_TREE_FILES, '{"skillTypes":[]}', fileText(ATLAS_TREE_JSON, '\n'))).toThrow('atlas_tree_types.json has no skill types');
    expect(() => parseTreeDocument(SHIP_TREE_FILES, typesText, fileText(ATLAS_TREE_JSON, '\n'))).toThrow(/ship_skill_tree.json has a volumes list/);
  });
});
