import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { ANY_SYSTEM_TYPE, HULL_ITEM_TYPE } from './definitions.ts';
import type { ItemFamily } from './itemFamily.ts';
import { MODIFIER_REFERENCE_MARK, SUBROUTINE_TYPE } from './modifiers.ts';

export interface CorruptionPool {
  resource: string;
  fileName: string;
  label: string;
  itemType: string;
  itemNoun: string;
  copyQualifier: string;
}

export const SUBROUTINE_CORRUPTION_POOL: CorruptionPool = {
  resource: 'subroutine-corruption-implicits',
  fileName: 'subroutine_corruption_implicits.csv',
  label: 'Subroutine corruption pool',
  itemType: SUBROUTINE_TYPE,
  itemNoun: 'subroutines',
  copyQualifier: 'subroutine_implicit',
};

export const HULL_ITEM_CORRUPTION_POOL: CorruptionPool = {
  resource: 'hull-item-corruption-implicits',
  fileName: 'hull_item_corruption_implicits.csv',
  label: 'Hull item corruption pool',
  itemType: HULL_ITEM_TYPE,
  itemNoun: 'hull items',
  copyQualifier: 'corruption_implicit',
};

export const CORRUPTION_POOLS: readonly CorruptionPool[] = [SUBROUTINE_CORRUPTION_POOL, HULL_ITEM_CORRUPTION_POOL];

export interface ImplicitReference {
  modifierId: string;
  inlineText?: string;
}

export interface ImplicitRow {
  cells: string[];
  section: string;
  implicits: ImplicitReference[];
}

export interface ImplicitsModel {
  table: CsvTable;
  rows: ImplicitRow[];
}

export interface CorruptionPoolEntry {
  cells: string[];
  implicit: ImplicitReference;
}

export interface CorruptionPoolModel {
  pool: CorruptionPool;
  table: CsvTable;
  entries: CorruptionPoolEntry[];
}

export function sectionKinds(vocabulary: Vocabulary): string[] {
  return vocabulary.itemKinds.filter((kind) => vocabulary.itemKindTypes.get(kind) === HULL_ITEM_TYPE);
}

export function implicitGroups(family: ItemFamily, vocabulary: Vocabulary): string[] {
  return family === 'chart' ? [...vocabulary.chartSystemTypes, ANY_SYSTEM_TYPE] : sectionKinds(vocabulary);
}

export function parseImplicitReferences(text: string): ImplicitReference[] {
  return String(text ?? '')
    .split(';')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) =>
      entry.startsWith(MODIFIER_REFERENCE_MARK) ? { modifierId: entry.slice(MODIFIER_REFERENCE_MARK.length).trim() } : { modifierId: '', inlineText: entry },
    );
}

export function formatImplicitReferences(references: ImplicitReference[]): string {
  return references.map((reference) => reference.inlineText ?? MODIFIER_REFERENCE_MARK + reference.modifierId).join('; ');
}

function parseImplicitRows(table: CsvTable, groupColumn: 'section' | 'system_type', groups: string[]): ImplicitRow[] {
  const rows = table.rows.map((cells): ImplicitRow => {
    const columns = trimmedCsvCells(table, cells, [groupColumn, 'implicits']);
    return { cells, section: columns[groupColumn], implicits: parseImplicitReferences(columns.implicits) };
  });
  for (const group of groups) {
    if (!rows.some((row) => row.section === group)) {
      rows.push({ cells: [], section: group, implicits: [] });
    }
  }
  return rows;
}

function savedRows(model: ImplicitsModel): ImplicitRow[] {
  return model.rows.filter((row) => row.cells.length > 0 || row.implicits.length > 0);
}

export function parseImplicitsCsv(text: string, vocabulary: Vocabulary): ImplicitsModel {
  const table = parseCsvTable(text);
  return { table, rows: parseImplicitRows(table, 'section', sectionKinds(vocabulary)) };
}

export function serializeImplicitsCsv(model: ImplicitsModel): string {
  return serializeCsvTable(
    csvTableFromRows(model.table, savedRows(model), (row) => ({ section: row.section, implicits: formatImplicitReferences(row.implicits) })),
  );
}

export function parseStarChartImplicitsCsv(text: string, vocabulary: Vocabulary): ImplicitsModel {
  const table = parseCsvTable(text);
  return { table, rows: parseImplicitRows(table, 'system_type', implicitGroups('chart', vocabulary)) };
}

export function serializeStarChartImplicitsCsv(model: ImplicitsModel): string {
  return serializeCsvTable(
    csvTableFromRows(model.table, savedRows(model), (row) => ({ system_type: row.section, implicits: formatImplicitReferences(row.implicits) })),
  );
}

function poolReference(text: string): ImplicitReference {
  const references = parseImplicitReferences(text);
  if (references.length === 0) {
    return { modifierId: '' };
  }
  return references.length === 1 ? references[0] : { modifierId: '', inlineText: String(text).trim() };
}

export function parseCorruptionPoolCsv(text: string, pool: CorruptionPool = SUBROUTINE_CORRUPTION_POOL): CorruptionPoolModel {
  const table = parseCsvTable(text);
  return { pool, table, entries: table.rows.map((cells) => ({ cells, implicit: poolReference(trimmedCsvCells(table, cells, ['implicit']).implicit) })) };
}

export function serializeCorruptionPoolCsv(model: CorruptionPoolModel): string {
  return serializeCsvTable(csvTableFromRows(model.table, model.entries, (entry) => ({ implicit: formatImplicitReferences([entry.implicit]) })));
}
