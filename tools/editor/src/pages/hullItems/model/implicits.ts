import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { formatPool, parsePool, type PoolEntry } from './definitions.ts';

export const SUBROUTINE_KIND = 'subroutine';

export interface ImplicitRow {
  cells: string[];
  type: string;
  implicits: PoolEntry[];
}

export interface ImplicitsModel {
  table: CsvTable;
  rows: ImplicitRow[];
}

export function hullSocketKinds(vocabulary: Vocabulary): string[] {
  return vocabulary.hullItemKinds.filter((kind) => kind !== SUBROUTINE_KIND);
}

export function parseImplicitsCsv(text: string, vocabulary: Vocabulary): ImplicitsModel {
  const table = parseCsvTable(text);
  const rows = table.rows.map((cells): ImplicitRow => {
    const columns = trimmedCsvCells(table, cells, ['type', 'implicits']);
    return { cells, type: columns.type, implicits: parsePool(columns.implicits) };
  });
  for (const kind of hullSocketKinds(vocabulary)) {
    if (!rows.some((row) => row.type === kind)) {
      rows.push({ cells: [], type: kind, implicits: [] });
    }
  }
  return { table, rows };
}

export function serializeImplicitsCsv(model: ImplicitsModel): string {
  const savedRows = model.rows.filter((row) => row.cells.length > 0 || row.implicits.length > 0);
  return serializeCsvTable(csvTableFromRows(model.table, savedRows, (row) => ({ type: row.type, implicits: formatPool(row.implicits) })));
}

export function newImplicit(vocabulary: Vocabulary): PoolEntry {
  return { effect: vocabulary.effectNames[0] ?? '', min: '5', max: '10', weight: '' };
}

export function validateImplicits(model: ImplicitsModel, vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  const seenTypes = new Set<string>();
  for (const row of model.rows) {
    const label = row.type || '(no type)';
    if (row.type === SUBROUTINE_KIND) {
      issues.push(`${label}: subroutines never roll implicits`);
    } else if (!vocabulary.hullItemKinds.includes(row.type)) {
      issues.push(`${label}: type must be one of ${hullSocketKinds(vocabulary).join(', ')}`);
    }
    if (seenTypes.has(row.type)) {
      issues.push(`${label}: listed more than once`);
    }
    seenTypes.add(row.type);
    const seenEffects = new Set<string>();
    for (const implicit of row.implicits) {
      if (!vocabulary.effectNames.includes(implicit.effect)) {
        issues.push(`${label}: ${implicit.effect || '(no effect)'} is not a skill effect`);
      }
      if (seenEffects.has(implicit.effect)) {
        issues.push(`${label}: ${implicit.effect} is listed more than once`);
      }
      seenEffects.add(implicit.effect);
      if (implicit.hullValues) {
        issues.push(`${label}: ${implicit.effect} must roll a min-max range, not per-hull-size values`);
      } else if (!isNumberText(implicit.min) || !isNumberText(implicit.max)) {
        issues.push(`${label}: ${implicit.effect} needs a number for min and max`);
      }
      if (implicit.weight !== '') {
        issues.push(`${label}: ${implicit.effect} has a weight, but implicits always roll`);
      }
    }
  }
  return issues;
}
