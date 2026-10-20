import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { ANY_SYSTEM_TYPE, HULL_ITEM_TYPE, normalisedSystemType } from './definitions.ts';
import { MODIFIER_FILE_NAMES, type ItemFamily } from './itemFamily.ts';
import { MODIFIER_REFERENCE_MARK, STAR_CHART_TYPE, SUBROUTINE_TYPE, type ItemModifier } from './modifiers.ts';

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

export function implicitItemType(family: ItemFamily): string {
  return family === 'chart' ? STAR_CHART_TYPE : HULL_ITEM_TYPE;
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

export function parseFamilyImplicits(family: ItemFamily, text: string, vocabulary: Vocabulary): ImplicitsModel {
  return family === 'chart' ? parseStarChartImplicitsCsv(text, vocabulary) : parseImplicitsCsv(text, vocabulary);
}

export function serializeFamilyImplicits(family: ItemFamily, model: ImplicitsModel): string {
  return family === 'chart' ? serializeStarChartImplicitsCsv(model) : serializeImplicitsCsv(model);
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

export function newImplicit(catalogue: ItemModifier[], family: ItemFamily = 'ship'): ImplicitReference {
  return newImplicitOf(catalogue, implicitItemType(family));
}

export function newImplicitOf(catalogue: ItemModifier[], itemType: string): ImplicitReference {
  return { modifierId: catalogue.find((modifier) => modifier.slot === 'implicit' && modifier.itemTypes.includes(itemType))?.id ?? '' };
}

export function validateCorruptionPool(model: CorruptionPoolModel, catalogue: Map<string, ItemModifier>): string[] {
  const issues: string[] = [];
  const listed: ItemModifier[] = [];
  const label = model.pool.label;
  for (const { implicit } of model.entries) {
    if (implicit.inlineText !== undefined) {
      issues.push(`${label}: "${implicit.inlineText}" is not a single @ID reference; pick an implicit modifier instead`);
      continue;
    }
    if (implicit.modifierId.includes(':')) {
      issues.push(`${label}: @${implicit.modifierId} has a weight, but every pool entry is equally likely`);
      continue;
    }
    const modifier = catalogue.get(implicit.modifierId);
    if (!modifier) {
      issues.push(`${label}: ${implicit.modifierId || '(no modifier)'} is not in ${MODIFIER_FILE_NAMES.ship}`);
      continue;
    }
    if (modifier.slot !== 'implicit') {
      issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, not an implicit`);
    }
    if (!modifier.itemTypes.includes(model.pool.itemType)) {
      issues.push(`${label}: ${modifier.id} does not roll on ${model.pool.itemNoun}`);
    }
    for (const earlier of listed) {
      if (earlier.id === modifier.id) {
        issues.push(`${label}: ${modifier.id} is listed more than once`);
      } else if (earlier.lines.some((line) => modifier.lines.some((other) => other.effect === line.effect))) {
        issues.push(`${label}: ${earlier.id} and ${modifier.id} share an effect`);
      }
    }
    listed.push(modifier);
  }
  return issues;
}

function groupIssues(row: ImplicitRow, family: ItemFamily, vocabulary: Vocabulary, seenGroups: Set<string>): string[] {
  const label = row.section || '(no section)';
  const issues: string[] = [];
  if (family === 'chart') {
    const systemType = normalisedSystemType(row.section, vocabulary);
    if (systemType === null) {
      issues.push(`${label}: system type must be ${ANY_SYSTEM_TYPE} or one of ${vocabulary.chartSystemTypes.join(', ')}`);
    } else if (seenGroups.has(systemType)) {
      issues.push(`${label}: listed more than once`);
    }
    seenGroups.add(systemType ?? row.section);
    return issues;
  }
  const sections = sectionKinds(vocabulary);
  if (!sections.includes(row.section)) {
    issues.push(`${label}: section must be one of ${sections.join(', ')}`);
  }
  if (seenGroups.has(row.section)) {
    issues.push(`${label}: listed more than once`);
  }
  seenGroups.add(row.section);
  return issues;
}

export function validateImplicits(model: ImplicitsModel, vocabulary: Vocabulary, catalogue: Map<string, ItemModifier>, family: ItemFamily = 'ship'): string[] {
  const issues: string[] = [];
  const seenGroups = new Set<string>();
  const itemType = implicitItemType(family);
  const fileName = MODIFIER_FILE_NAMES[family];
  for (const row of savedRows(model)) {
    const label = row.section || '(no section)';
    issues.push(...groupIssues(row, family, vocabulary, seenGroups));
    const listed: ItemModifier[] = [];
    for (const reference of row.implicits) {
      if (reference.inlineText !== undefined) {
        issues.push(`${label}: "${reference.inlineText}" is written inline; pick an implicit modifier instead`);
        continue;
      }
      if (reference.modifierId.includes(':')) {
        issues.push(`${label}: @${reference.modifierId} has a weight, but implicits always roll`);
        continue;
      }
      const modifier = catalogue.get(reference.modifierId);
      if (!modifier) {
        issues.push(`${label}: ${reference.modifierId || '(no modifier)'} is not in ${fileName}`);
        continue;
      }
      if (modifier.slot !== 'implicit') {
        issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, not an implicit`);
      }
      if (!modifier.itemTypes.includes(itemType)) {
        issues.push(`${label}: ${modifier.id} does not roll on ${family === 'chart' ? 'star charts' : 'hull items'}`);
      }
      for (const earlier of listed) {
        if (earlier.id === modifier.id) {
          issues.push(`${label}: ${modifier.id} is listed more than once`);
        } else if (family === 'ship' && earlier.lines.some((line) => modifier.lines.some((other) => other.effect === line.effect))) {
          issues.push(`${label}: ${earlier.id} and ${modifier.id} share an effect`);
        }
      }
      listed.push(modifier);
    }
  }
  return issues;
}
