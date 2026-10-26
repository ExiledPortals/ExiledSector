import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseWormholeAnchorsCsv, serializeWormholeAnchorsCsv } from './anchors.ts';
import { parseDoctrineArchivesCsv, serializeDoctrineArchivesCsv } from './doctrines.ts';
import {
  parseFamilyDefinitions,
  parseItemDefinitions,
  parseStarChartDefinitions,
  serializeFamilyDefinitions,
  serializeHullItemsCsv,
  serializeStarChartsCsv,
  serializeSubroutinesCsv,
} from './definitions.ts';
import { parseImplicitsCsv, parseStarChartImplicitsCsv, serializeImplicitsCsv, serializeStarChartImplicitsCsv } from './implicits.ts';
import { parseModifiersCsv, serializeModifiersCsv } from './modifiers.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');
const configDir = path.join(repositoryRoot, 'data', 'config', 'exiledSector');

const vocabulary = buildVocabulary(JSON.parse(fs.readFileSync(path.join(repositoryRoot, 'tools', 'editor_vocabulary.json'), 'utf8')));

function readData(fullPath: string): string {
  const text = fs.readFileSync(fullPath, 'utf8');
  return text.startsWith(String.fromCharCode(0xfeff)) ? text.slice(1) : text;
}

describe('saving unchanged item data reproduces the files byte for byte', () => {
  it('subroutines.csv and hull_items.csv', () => {
    const subroutinesText = readData(path.join(configDir, 'subroutines.csv'));
    const hullItemsText = readData(path.join(configDir, 'hull_items.csv'));
    const model = parseItemDefinitions(subroutinesText, hullItemsText);
    expect(serializeSubroutinesCsv(model)).toBe(subroutinesText);
    expect(serializeHullItemsCsv(model)).toBe(hullItemsText);
  });

  it('keeps every unique subroutine line, source and socket limit through a save', () => {
    const model = parseItemDefinitions(readData(path.join(configDir, 'subroutines.csv')), readData(path.join(configDir, 'hull_items.csv')));
    const uniques = model.definitions.filter((definition) => definition.subroutineKind === 'UNIQUE');
    const resaved = parseItemDefinitions(serializeSubroutinesCsv(model), serializeHullItemsCsv(model)).definitions;

    expect(uniques.length).toBeGreaterThan(0);
    for (const unique of uniques) {
      const saved = resaved.find((definition) => definition.id === unique.id);
      expect(unique.lines?.length, unique.id).toBeGreaterThan(0);
      expect(unique.source, unique.id).not.toBe('');
      expect(saved?.lines, unique.id).toEqual(unique.lines);
      expect(saved?.source, unique.id).toBe(unique.source);
      expect(unique.socketLimit, unique.id).toBe('1');
      expect(saved?.socketLimit, unique.id).toBe(unique.socketLimit);
    }
  });

  it('keeps every modspec cache exclusion, node count chance and drop chance through a save', () => {
    const model = parseItemDefinitions(readData(path.join(configDir, 'subroutines.csv')), readData(path.join(configDir, 'hull_items.csv')));
    const caches = model.definitions.filter((definition) => definition.subroutineKind === 'MODSPEC_CACHE');
    const resaved = parseItemDefinitions(serializeSubroutinesCsv(model), serializeHullItemsCsv(model)).definitions;

    expect(caches.length).toBeGreaterThan(0);
    for (const cache of caches) {
      const saved = resaved.find((definition) => definition.id === cache.id);
      expect(Number.parseFloat(cache.dropChance || cache.salvageChance || '0'), cache.id).toBeGreaterThan(0);
      expect(cache.nodeCountChances, cache.id).not.toBe('');
      expect(saved?.excludedNodes, cache.id).toBe(cache.excludedNodes);
      expect(saved?.nodeCountChances, cache.id).toBe(cache.nodeCountChances);
      expect(saved?.dropChance, cache.id).toBe(cache.dropChance);
      expect(saved?.salvageChance, cache.id).toBe(cache.salvageChance);
    }
  });

  it('item_modifiers.csv', () => {
    const text = readData(path.join(configDir, 'item_modifiers.csv'));
    expect(serializeModifiersCsv(parseModifiersCsv(text))).toBe(text);
  });

  it('hull_item_implicits.csv', () => {
    const text = readData(path.join(configDir, 'hull_item_implicits.csv'));
    expect(serializeImplicitsCsv(parseImplicitsCsv(text, vocabulary))).toBe(text);
  });

  it('star_charts.csv', () => {
    const text = readData(path.join(configDir, 'star_charts.csv'));
    expect(serializeStarChartsCsv(parseStarChartDefinitions(text))).toBe(text);
    const texts = Object.fromEntries(['subroutines', 'hull_items', 'star_charts'].map((name) => [name.replace('_', '-'), readData(path.join(configDir, `${name}.csv`))]));
    expect(serializeFamilyDefinitions(parseFamilyDefinitions('chart', texts))).toEqual({ 'star-charts': text });
    expect(serializeFamilyDefinitions(parseFamilyDefinitions('ship', texts))).toEqual({ subroutines: texts.subroutines, 'hull-items': texts['hull-items'] });
  });

  it('wormhole_anchors.csv', () => {
    const text = readData(path.join(configDir, 'wormhole_anchors.csv'));
    expect(serializeWormholeAnchorsCsv(parseWormholeAnchorsCsv(text))).toBe(text);
  });

  it('doctrine_archives.csv', () => {
    const text = readData(path.join(configDir, 'doctrine_archives.csv'));
    const model = parseDoctrineArchivesCsv(text);
    expect(model.archives.length).toBeGreaterThan(0);
    expect(serializeDoctrineArchivesCsv(model)).toBe(text);
  });

  it('star_chart_modifiers.csv', () => {
    const text = readData(path.join(configDir, 'star_chart_modifiers.csv'));
    expect(serializeModifiersCsv(parseModifiersCsv(text))).toBe(text);
  });

  it('star_chart_implicits.csv', () => {
    const text = readData(path.join(configDir, 'star_chart_implicits.csv'));
    expect(serializeStarChartImplicitsCsv(parseStarChartImplicitsCsv(text, vocabulary))).toBe(text);
  });
});
