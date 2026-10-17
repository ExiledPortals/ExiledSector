import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { MODIFIER_REFERENCE_MARK, type HullItemModifier } from './modifiers.ts';

export const SUBROUTINE_KIND = 'subroutine';

export interface ImplicitReference {
  modifierId: string;
  inlineText?: string;
}

export interface ImplicitRow {
  cells: string[];
  type: string;
  implicits: ImplicitReference[];
}

export interface ImplicitsModel {
  table: CsvTable;
  rows: ImplicitRow[];
}

export function hullSocketKinds(vocabulary: Vocabulary): string[] {
  return vocabulary.hullItemKinds.filter((kind) => kind !== SUBROUTINE_KIND);
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

export function parseImplicitsCsv(text: string, vocabulary: Vocabulary): ImplicitsModel {
  const table = parseCsvTable(text);
  const rows = table.rows.map((cells): ImplicitRow => {
    const columns = trimmedCsvCells(table, cells, ['type', 'implicits']);
    return { cells, type: columns.type, implicits: parseImplicitReferences(columns.implicits) };
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
  return serializeCsvTable(
    csvTableFromRows(model.table, savedRows, (row) => ({ type: row.type, implicits: formatImplicitReferences(row.implicits) })),
  );
}

export function newImplicit(catalogue: HullItemModifier[]): ImplicitReference {
  return { modifierId: catalogue.find((modifier) => modifier.slot === 'implicit')?.id ?? '' };
}

export function validateImplicits(model: ImplicitsModel, vocabulary: Vocabulary, catalogue: Map<string, HullItemModifier>): string[] {
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
    const listed: HullItemModifier[] = [];
    for (const reference of row.implicits) {
      if (reference.inlineText !== undefined) {
        issues.push(`${label}: "${reference.inlineText}" is written inline; pick an implicit modifier instead`);
        continue;
      }
      const modifier = catalogue.get(reference.modifierId);
      if (!modifier) {
        issues.push(`${label}: ${reference.modifierId || '(no modifier)'} is not in hull_item_modifiers.csv`);
        continue;
      }
      if (modifier.slot !== 'implicit') {
        issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, not an implicit`);
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
  }
  return issues;
}
