import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { lineEndingOf, withLineEnding } from '../../../shared/lineEndings.ts';
import { parseTreeDocument, serializeTreeFiles, serializeType } from './serialization.ts';

const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);
const PROJECT_ROOT = path.resolve(import.meta.dirname, '../../../../../..');

function readProjectFile(relativePath: string): string {
  const text = fs.readFileSync(path.join(PROJECT_ROOT, relativePath), 'utf8');
  return text.startsWith(BYTE_ORDER_MARK) ? text.slice(1) : text;
}

describe('tree files round-trip', () => {
  it('serialises skill_types.json and ship_skill_tree.json back to the same bytes', () => {
    const typesText = readProjectFile('data/skilltrees/skill_types.json');
    const treeText = readProjectFile('data/skilltrees/ship_skill_tree.json');
    const document = parseTreeDocument(typesText, treeText);
    const texts = serializeTreeFiles(document, { types: lineEndingOf(typesText), tree: lineEndingOf(treeText) });
    expect(texts.types).toBe(typesText);
    expect(texts.tree).toBe(treeText);
  });

  it('round-trips the tree file with and without decorations', () => {
    const typesText = readProjectFile('data/skilltrees/skill_types.json');
    const treeText = readProjectFile('data/skilltrees/ship_skill_tree.json');
    const lineEnding = lineEndingOf(treeText);
    const treeJson = JSON.parse(treeText) as Record<string, unknown>;
    const withoutDecorations = Object.fromEntries(Object.entries(treeJson).filter(([key]) => key !== 'decorations'));
    for (const json of [treeJson, withoutDecorations]) {
      const text = withLineEnding(JSON.stringify(json, null, '\t'), lineEnding);
      const texts = serializeTreeFiles(parseTreeDocument(typesText, text), { types: lineEndingOf(typesText), tree: lineEnding });
      expect(texts.tree).toBe(text);
    }
  });
});

describe('module share serialisation', () => {
  it('writes moduleShare last and leaves it out when the type has none', () => {
    const base = { id: 'a', name: 'A', icon: '', tags: ['armor'] };
    expect(Object.keys(serializeType({ ...base, moduleShare: 'all' }))).toEqual(['id', 'name', 'icon', 'tags', 'moduleShare']);
    expect(serializeType({ ...base, moduleShare: 'none' }).moduleShare).toBe('none');
    expect(serializeType(base)).not.toHaveProperty('moduleShare');
  });
});

describe('text serialisation', () => {
  it('writes the effect text after the flavour and leaves an empty list out', () => {
    const base = { id: 'a', name: 'A', icon: '', flavour: 'Old steel.', tags: ['armor'] };
    expect(Object.keys(serializeType({ ...base, effectText: ['Holds one subroutine.'] }))).toEqual(['id', 'name', 'icon', 'flavour', 'effectText', 'tags']);
    expect(serializeType({ ...base, effectText: [] })).not.toHaveProperty('effectText');
  });

  it('writes the automated flavour after the flavour and leaves an empty one out', () => {
    const base = { id: 'a', name: 'A', icon: '', flavour: 'Old steel.', tags: ['armor'] };
    const automated = serializeType({ ...base, flavourAutomated: 'The core logs old steel.', effectText: ['Holds one subroutine.'] });
    expect(Object.keys(automated)).toEqual(['id', 'name', 'icon', 'flavour', 'flavourAutomated', 'effectText', 'tags']);
    expect(automated.flavourAutomated).toBe('The core logs old steel.');
    expect(serializeType({ ...base, flavourAutomated: '' })).not.toHaveProperty('flavourAutomated');
  });
});

describe('decoration serialisation', () => {
  const typesText = JSON.stringify({ skillTypes: [{ id: 'small', name: 'Small', icon: '' }] });
  const nodes = [{ id: 'a', type: 'small', connectedTo: [], x: 0, y: 0 }];

  function serializedDecorations(decorations: Record<string, unknown>[]): Record<string, unknown>[] {
    const document = parseTreeDocument(typesText, JSON.stringify({ nodes, decorations }));
    const tree = JSON.parse(serializeTreeFiles(document, { types: '\n', tree: '\n' }).tree) as Record<string, unknown>;
    return (tree.decorations ?? []) as Record<string, unknown>[];
  }

  it('writes every kind into one decorations list grouped by kind, kind first and keys in loader order', () => {
    const out = serializedDecorations([
      { kind: 'threatUnit', tags: ['threat'], rotation: 90, length: 700, unitType: 'hive', y: 2, x: 1, id: 'threat_unit_1' },
      { kind: 'serpent', id: 'worm', x: 0, y: 0, orbitRadius: 1663, orbitSeconds: 190, clockwise: false, startAngle: 180, wobble: 0.15, wobbleWaves: 7, wobblePhase: 74.5, tags: ['abyss'] },
      { kind: 'ringBelt', id: 'belt', x: 0, y: 0, innerRadius: 2900, outerRadius: 3250, ringArtPath: 'graphics/planets/ring_band_dust.png', rotationSpeed: 0.6 },
      { kind: 'star', id: 'star_1', x: 0, y: 0, radius: 400, starType: 'star_yellow' },
    ]);
    expect(out.map((decoration) => decoration.kind)).toEqual(['star', 'ringBelt', 'threatUnit', 'serpent']);
    expect(Object.keys(out[2])).toEqual(['kind', 'id', 'x', 'y', 'unitType', 'length', 'rotation', 'tags']);
    expect(Object.keys(out[1])).toEqual(['kind', 'id', 'x', 'y', 'innerRadius', 'outerRadius', 'ringArtPath', 'rotationSpeed']);
    expect(Object.keys(out[3])).toEqual(['kind', 'id', 'x', 'y', 'orbitRadius', 'orbitSeconds', 'clockwise', 'startAngle', 'wobble', 'wobbleWaves', 'wobblePhase', 'tags']);
  });

  it('leaves out zero rotations, empty tags, plain serpent orbits and the list itself when there is nothing in it', () => {
    const out = serializedDecorations([
      { kind: 'threatUnit', id: 'threat_unit_2', x: 3, y: 4, unitType: 'fabricator', length: 600, rotation: 0, tags: [] },
      { kind: 'serpent', id: 'plain', x: 0, y: 0, orbitRadius: 500, orbitSeconds: 100, clockwise: true, startAngle: 0, wobble: 0 },
    ]);
    expect(out[0]).toEqual({ kind: 'threatUnit', id: 'threat_unit_2', x: 3, y: 4, unitType: 'fabricator', length: 600 });
    expect(out[1]).toEqual({ kind: 'serpent', id: 'plain', x: 0, y: 0, orbitRadius: 500, orbitSeconds: 100, clockwise: true });
    const empty = JSON.parse(serializeTreeFiles(parseTreeDocument(typesText, JSON.stringify({ nodes })), { types: '\n', tree: '\n' }).tree) as Record<string, unknown>;
    expect(empty).not.toHaveProperty('decorations');
  });

  it('writes behindStars and a parallax only on images that need them', () => {
    const out = serializedDecorations([
      { kind: 'image', id: 'haze', x: 0, y: 0, width: 6400, height: 6400, imagePath: 'a.png', behindStars: true, parallax: 0.8, tags: ['tania'] },
      { kind: 'image', id: 'cloud', x: 1, y: 1, width: 100, height: 100, imagePath: 'b.png', parallax: 1 },
    ]);
    expect(Object.keys(out[0])).toEqual(['kind', 'id', 'x', 'y', 'width', 'height', 'imagePath', 'behindStars', 'parallax', 'tags']);
    expect(out[1]).toEqual({ kind: 'image', id: 'cloud', x: 1, y: 1, width: 100, height: 100, imagePath: 'b.png' });
  });

  it('keeps a star overlay after the colour and leaves it out of stars without one', () => {
    const out = serializedDecorations([
      { kind: 'star', id: 'crest_star', x: 0, y: 0, radius: 400, starType: 'star_orange', color: '#f08a3c', overlayPath: 'graphics/fx/crest.png', overlaySize: 0.95, overlayAlpha: 0.45, tags: ['tania'] },
      { kind: 'star', id: 'plain_star', x: 1, y: 1, radius: 200, starType: 'star_yellow' },
    ]);
    expect(Object.keys(out[0])).toEqual(['kind', 'id', 'x', 'y', 'radius', 'starType', 'color', 'overlayPath', 'overlaySize', 'overlayAlpha', 'tags']);
    expect(out[1]).toEqual({ kind: 'star', id: 'plain_star', x: 1, y: 1, radius: 200, starType: 'star_yellow' });
  });

  it('refuses to load an unknown kind or a retired per-kind list rather than dropping them on save', () => {
    expect(() => parseTreeDocument(typesText, JSON.stringify({ nodes, decorations: [{ kind: 'comet', id: 'c', x: 0, y: 0 }] }))).toThrow(/unknown kind "comet"/);
    expect(() => parseTreeDocument(typesText, JSON.stringify({ nodes, stars: [{ id: 's', x: 0, y: 0, radius: 100 }] }))).toThrow(/still has stars/);
  });
});
