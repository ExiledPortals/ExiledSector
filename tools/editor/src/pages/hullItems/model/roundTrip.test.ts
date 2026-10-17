import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { parseCraftingCsv, serializeCraftingCsv } from './crafting.ts';
import { parseHullItemsCsv, serializeHullItemsCsv } from './definitions.ts';
import { parseSalvageCsv, serializeSalvageCsv } from './drops.ts';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { parseAffixesCsv, serializeAffixesCsv, serializeRareNames } from './names.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');
const configDir = path.join(repositoryRoot, 'data', 'config', 'exiledSector');

function readData(fullPath: string): string {
  const text = fs.readFileSync(fullPath, 'utf8');
  return text.startsWith(String.fromCharCode(0xfeff)) ? text.slice(1) : text;
}

describe('saving unchanged socketable data reproduces the files byte for byte', () => {
  it('socketables.csv', () => {
    const text = readData(path.join(configDir, 'socketables.csv'));
    expect(serializeHullItemsCsv(parseHullItemsCsv(text))).toBe(text);
  });

  it('socketable_affixes.csv', () => {
    const text = readData(path.join(configDir, 'socketable_affixes.csv'));
    const parsed = parseAffixesCsv(text);
    expect(serializeAffixesCsv(parsed.table, parsed.rows)).toBe(text);
  });

  it('socketable_names.json', () => {
    const text = readData(path.join(configDir, 'socketable_names.json'));
    expect(serializeRareNames(JSON.parse(text), lineEndingOf(text))).toBe(text);
  });

  it('socketable_crafting.csv', () => {
    const text = readData(path.join(configDir, 'socketable_crafting.csv'));
    const parsed = parseCraftingCsv(text);
    expect(serializeCraftingCsv(parsed.table, parsed.rows)).toBe(text);
  });

  it('commodities.csv', () => {
    const text = readData(path.join(repositoryRoot, 'data', 'campaign', 'commodities.csv'));
    expect(serializeCsvTable(parseCsvTable(text))).toBe(text);
  });

  it('every salvage file', () => {
    const compatDir = path.join(configDir, 'compat', 'salvage');
    const salvageFiles = [path.join(configDir, 'socketable_salvage.csv'), ...fs.readdirSync(compatDir).map((name) => path.join(compatDir, name))];
    for (const salvageFile of salvageFiles) {
      const text = readData(salvageFile);
      expect(serializeSalvageCsv(parseSalvageCsv(text)), path.basename(salvageFile)).toBe(text);
    }
  });
});
