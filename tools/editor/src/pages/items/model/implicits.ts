import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { HULL_ITEM_TYPE } from './definitions.ts';
import { MODIFIER_REFERENCE_MARK, type ItemModifier } from './modifiers.ts';

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

export function sectionKinds(vocabulary: Vocabulary): string[] {
  return vocabulary.itemKinds.filter((kind) => vocabulary.itemKindTypes.get(kind) === HULL_ITEM_TYPE);
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
    const columns = trimmedCsvCells(table, cells, ['section', 'implicits']);
    return { cells, section: columns.section, implicits: parseImplicitReferences(columns.implicits) };
  });
  for (const kind of sectionKinds(vocabulary)) {
    if (!rows.some((row) => row.section === kind)) {
      rows.push({ cells: [], section: kind, implicits: [] });
    }
  }
  return { table, rows };
}

export function serializeImplicitsCsv(model: ImplicitsModel): string {
  const savedRows = model.rows.filter((row) => row.cells.length > 0 || row.implicits.length > 0);
  return serializeCsvTable(
    csvTableFromRows(model.table, savedRows, (row) => ({ section: row.section, implicits: formatImplicitReferences(row.implicits) })),
  );
}

export function newImplicit(catalogue: ItemModifier[]): ImplicitReference {
  return { modifierId: catalogue.find((modifier) => modifier.slot === 'implicit' && modifier.itemTypes.includes(HULL_ITEM_TYPE))?.id ?? '' };
}

export function validateImplicits(model: ImplicitsModel, vocabulary: Vocabulary, catalogue: Map<string, ItemModifier>): string[] {
  const issues: string[] = [];
  const seenSections = new Set<string>();
  const sections = sectionKinds(vocabulary);
  for (const row of model.rows) {
    const label = row.section || '(no section)';
    if (!sections.includes(row.section)) {
      issues.push(`${label}: section must be one of ${sections.join(', ')}`);
    }
    if (seenSections.has(row.section)) {
      issues.push(`${label}: listed more than once`);
    }
    seenSections.add(row.section);
    const listed: ItemModifier[] = [];
    for (const reference of row.implicits) {
      if (reference.inlineText !== undefined) {
        issues.push(`${label}: "${reference.inlineText}" is written inline; pick an implicit modifier instead`);
        continue;
      }
      const modifier = catalogue.get(reference.modifierId);
      if (!modifier) {
        issues.push(`${label}: ${reference.modifierId || '(no modifier)'} is not in item_modifiers.csv`);
        continue;
      }
      if (modifier.slot !== 'implicit') {
        issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, not an implicit`);
      }
      if (!modifier.itemTypes.includes(HULL_ITEM_TYPE)) {
        issues.push(`${label}: ${modifier.id} does not roll on hull items`);
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
