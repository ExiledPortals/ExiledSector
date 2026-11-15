import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';

export const PHASED_TRAITS_RESOURCE = 'phased-planet-traits';
export const PHASED_TRAITS_FILE_NAME = 'phased_planet_traits.csv';
export const PHASED_TRAIT_COLUMNS = ['id', 'condition', 'weight', 'regions', 'anomalies', 'excludes', 'notes'] as const;
const ENTRY_SEPARATOR = ';';
const WEIGHT_SEPARATOR = ':';

export interface PhasedTrait {
  cells: string[];
  id: string;
  condition: string;
  weight: string;
  regions: string;
  anomalies: string;
  excludes: string;
  notes: string;
  loadedId?: string;
}

export interface PhasedTraitsModel {
  table: CsvTable;
  traits: PhasedTrait[];
}

export function parsePhasedTraitsCsv(text: string): PhasedTraitsModel {
  const table = parseCsvTable(text);
  const traits = table.rows.map((cells): PhasedTrait => {
    const columns = trimmedCsvCells(table, cells, PHASED_TRAIT_COLUMNS);
    return { cells, ...columns, loadedId: columns.id };
  });
  return { table, traits };
}

function traitColumnValues(trait: PhasedTrait): Record<(typeof PHASED_TRAIT_COLUMNS)[number], string> {
  return {
    id: trait.id,
    condition: trait.condition,
    weight: trait.weight,
    regions: trait.regions,
    anomalies: trait.anomalies,
    excludes: trait.excludes,
    notes: trait.notes,
  };
}

export function serializePhasedTraitsCsv(model: PhasedTraitsModel): string {
  return serializeCsvTable(csvTableFromRows(model.table, model.traits, traitColumnValues));
}

function entries(text: string): string[] {
  return text
    .split(ENTRY_SEPARATOR)
    .map((entry) => entry.trim())
    .filter((entry) => entry !== '');
}

function weightIssues(trait: PhasedTrait, column: 'regions' | 'anomalies', text: string): string[] {
  const issues: string[] = [];
  for (const entry of entries(text)) {
    const separator = entry.indexOf(WEIGHT_SEPARATOR);
    const id = separator < 0 ? entry : entry.slice(0, separator).trim();
    const multiplier = separator < 0 ? 1 : Number(entry.slice(separator + 1).trim());
    if (id === '' || !Number.isFinite(multiplier) || multiplier < 0) {
      issues.push(`${trait.id}: ${column} entry "${entry}" should be id:multiplier with a multiplier of 0 or more`);
    }
  }
  return issues;
}

export function validatePhasedTraits(traits: readonly PhasedTrait[], conditionIds: ReadonlySet<string>): string[] {
  const issues: string[] = [];
  const ids = new Set(traits.map((trait) => trait.id));
  const seen = new Set<string>();
  for (const trait of traits) {
    if (trait.id === '') {
      issues.push('a phased planet trait has no id');
      continue;
    }
    if (seen.has(trait.id)) {
      issues.push(`${trait.id}: the id is used twice`);
    }
    seen.add(trait.id);
    if (!conditionIds.has(trait.condition)) {
      issues.push(`${trait.id}: condition "${trait.condition}" is not in market_conditions.csv`);
    }
    const weight = trait.weight === '' ? 1 : Number(trait.weight);
    if (!Number.isFinite(weight) || weight < 0) {
      issues.push(`${trait.id}: weight "${trait.weight}" should be a number of 0 or more`);
    }
    issues.push(...weightIssues(trait, 'regions', trait.regions), ...weightIssues(trait, 'anomalies', trait.anomalies));
    for (const excluded of entries(trait.excludes)) {
      if (!ids.has(excluded)) {
        issues.push(`${trait.id}: excludes "${excluded}", which is not a trait`);
      }
    }
  }
  return issues;
}

export function marketConditionIds(marketConditionsText: string): Set<string> {
  const table = parseCsvTable(marketConditionsText);
  return new Set(table.rows.map((cells) => trimmedCsvCells(table, cells, ['id'] as const).id).filter((id) => id !== ''));
}
