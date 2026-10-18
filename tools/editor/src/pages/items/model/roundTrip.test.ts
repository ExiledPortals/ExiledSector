import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { parseCraftingCsv, serializeCraftingCsv } from './crafting.ts';
import { parseItemDefinitions, serializeHullItemsCsv, serializeSubroutinesCsv } from './definitions.ts';
import { parseSalvageCsv, serializeSalvageCsv } from './drops.ts';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { parseModifiersCsv, serializeModifiersCsv } from './modifiers.ts';
import { serializeRareNames } from './names.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');
const configDir = path.join(repositoryRoot, 'data', 'config', 'exiledSector');

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

  it('every salvage file', () => {
    const compatDir = path.join(configDir, 'compat', 'salvage');
    const salvageFiles = [path.join(configDir, 'item_salvage.csv'), ...fs.readdirSync(compatDir).map((name) => path.join(compatDir, name))];
    for (const salvageFile of salvageFiles) {
      const text = readData(salvageFile);
      expect(serializeSalvageCsv(parseSalvageCsv(text)), path.basename(salvageFile)).toBe(text);
    }
  });
});
