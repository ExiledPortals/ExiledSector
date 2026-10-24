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

  it('round-trips the tree file with and without threat units', () => {
    const typesText = readProjectFile('data/skilltrees/skill_types.json');
    const treeText = readProjectFile('data/skilltrees/ship_skill_tree.json');
    const lineEnding = lineEndingOf(treeText);
    const treeJson = JSON.parse(treeText) as Record<string, unknown>;
    const withoutThreatUnits = Object.fromEntries(Object.entries(treeJson).filter(([key]) => key !== 'threatUnits'));
    const withThreatUnits = {
      ...withoutThreatUnits,
      threatUnits: [
        { id: 'threat_unit_1', x: -41802, y: 18563, unitType: 'fabricator', length: 600, tags: ['threat'] },
        { id: 'threat_unit_2', x: 10, y: -20, unitType: 'hive', length: 450.5, rotation: 35 },
      ],
    };
    for (const json of [withoutThreatUnits, withThreatUnits]) {
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

describe('threat unit serialisation', () => {
  const typesText = JSON.stringify({ skillTypes: [{ id: 'small', name: 'Small', icon: '' }] });
  const nodes = [{ id: 'a', type: 'small', connectedTo: [], x: 0, y: 0 }];

  function serializedTree(treeJson: Record<string, unknown>): Record<string, unknown> {
    const document = parseTreeDocument(typesText, JSON.stringify(treeJson));
    return JSON.parse(serializeTreeFiles(document, { types: '\n', tree: '\n' }).tree) as Record<string, unknown>;
  }

  it('writes threat units after stars with keys in loader order, omitting zero rotation and empty tags', () => {
    const out = serializedTree({
      nodes,
      threatUnits: [
        { tags: ['threat'], rotation: 90, length: 700, unitType: 'hive', y: 2, x: 1, id: 'threat_unit_1' },
        { id: 'threat_unit_2', x: 3, y: 4, unitType: 'fabricator', length: 600, rotation: 0, tags: [] },
      ],
      stars: [{ id: 'star_1', x: 0, y: 0, radius: 400, starType: 'star_yellow' }],
    });
    expect(Object.keys(out)).toEqual(['nodes', 'stars', 'threatUnits']);
    const threatUnits = out.threatUnits as Record<string, unknown>[];
    expect(Object.keys(threatUnits[0])).toEqual(['id', 'x', 'y', 'unitType', 'length', 'rotation', 'tags']);
    expect(threatUnits[1]).toEqual({ id: 'threat_unit_2', x: 3, y: 4, unitType: 'fabricator', length: 600 });
  });

  it('leaves threatUnits out when there are none', () => {
    expect(serializedTree({ nodes, threatUnits: [] })).not.toHaveProperty('threatUnits');
    expect(parseTreeDocument(typesText, JSON.stringify({ nodes })).threatUnits).toEqual([]);
  });

  it('writes behindStars only on static images that draw behind the stars', () => {
    const out = serializedTree({
      nodes,
      staticImages: [
        { id: 'backdrop', x: 0, y: 0, width: 3200, height: 3200, imagePath: 'a.png', behindStars: true, tags: ['tania'] },
        { id: 'cloud', x: 1, y: 1, width: 100, height: 100, imagePath: 'b.png' },
      ],
    });
    const images = out.staticImages as Record<string, unknown>[];
    expect(Object.keys(images[0])).toEqual(['id', 'x', 'y', 'width', 'height', 'imagePath', 'behindStars', 'tags']);
    expect(images[1]).toEqual({ id: 'cloud', x: 1, y: 1, width: 100, height: 100, imagePath: 'b.png' });
  });

  it('keeps a star overlay after the colour and leaves it out of stars without one', () => {
    const out = serializedTree({
      nodes,
      stars: [
        { id: 'crest_star', x: 0, y: 0, radius: 400, starType: 'star_orange', color: '#f08a3c', overlayPath: 'graphics/fx/crest.png', overlaySize: 0.95, overlayAlpha: 0.45, tags: ['tania'] },
        { id: 'plain_star', x: 1, y: 1, radius: 200, starType: 'star_yellow' },
      ],
    });
    const stars = out.stars as Record<string, unknown>[];
    expect(Object.keys(stars[0])).toEqual(['id', 'x', 'y', 'radius', 'starType', 'color', 'overlayPath', 'overlaySize', 'overlayAlpha', 'tags']);
    expect(stars[0].overlayAlpha).toBe(0.45);
    expect(stars[1]).toEqual({ id: 'plain_star', x: 1, y: 1, radius: 200, starType: 'star_yellow' });
  });
});
