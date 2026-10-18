import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { CHART_TABLE_SPECS, parseChartTable, serializeChartTable } from './chartTables.ts';
import { parseCraftingCsv, serializeCraftingCsv } from './crafting.ts';
import {
  parseFamilyDefinitions,
  parseItemDefinitions,
  parseStarChartDefinitions,
  serializeFamilyDefinitions,
  serializeHullItemsCsv,
  serializeStarChartsCsv,
  serializeSubroutinesCsv,
} from './definitions.ts';
import { parseSalvageCsv, serializeSalvageCsv } from './drops.ts';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { parseImplicitsCsv, parseStarChartImplicitsCsv, serializeImplicitsCsv, serializeStarChartImplicitsCsv } from './implicits.ts';
import { parseModifiersCsv, serializeModifiersCsv } from './modifiers.ts';
import { serializeRareNames, serializeStarChartNames } from './names.ts';

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

  it('item_modifiers.csv', () => {
    const text = readData(path.join(configDir, 'item_modifiers.csv'));
    expect(serializeModifiersCsv(parseModifiersCsv(text))).toBe(text);
  });

  it('item_names.json', () => {
    const text = readData(path.join(configDir, 'item_names.json'));
    expect(serializeRareNames(JSON.parse(text), lineEndingOf(text))).toBe(text);
  });

  it('item_crafting.csv', () => {
    const text = readData(path.join(configDir, 'item_crafting.csv'));
    const parsed = parseCraftingCsv(text);
    expect(serializeCraftingCsv(parsed.table, parsed.rows)).toBe(text);
  });

  it('commodities.csv', () => {
    const text = readData(path.join(repositoryRoot, 'data', 'campaign', 'commodities.csv'));
    expect(serializeCsvTable(parseCsvTable(text))).toBe(text);
  });

  it('special_items.csv', () => {
    const text = readData(path.join(repositoryRoot, 'data', 'campaign', 'special_items.csv'));
    expect(serializeCsvTable(parseCsvTable(text))).toBe(text);
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

  it('star_chart_modifiers.csv', () => {
    const text = readData(path.join(configDir, 'star_chart_modifiers.csv'));
    expect(serializeModifiersCsv(parseModifiersCsv(text))).toBe(text);
  });

  it('star_chart_implicits.csv', () => {
    const text = readData(path.join(configDir, 'star_chart_implicits.csv'));
    expect(serializeStarChartImplicitsCsv(parseStarChartImplicitsCsv(text, vocabulary))).toBe(text);
  });

  it('star_chart_names.json', () => {
    const text = readData(path.join(configDir, 'star_chart_names.json'));
    expect(serializeStarChartNames(JSON.parse(text), lineEndingOf(text))).toBe(text);
  });

  it('every chart table', () => {
    for (const spec of CHART_TABLE_SPECS) {
      const text = readData(path.join(configDir, spec.fileName));
      expect(serializeChartTable(parseChartTable(text, spec.columns), spec.columns), spec.fileName).toBe(text);
    }
  });

  it('every salvage file', () => {
    const compatDir = path.join(configDir, 'compat', 'salvage');
    const salvageFiles = [path.join(configDir, 'item_salvage.csv'), ...fs.readdirSync(compatDir).map((name) => path.join(compatDir, name))];
    for (const salvageFile of salvageFiles) {
      const text = readData(salvageFile);
      expect(serializeSalvageCsv(parseSalvageCsv(text)), path.basename(salvageFile)).toBe(text);
    }
  });
});
