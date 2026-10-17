import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, csvTableFromRows, dropCsvColumn, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { MODIFIER_REFERENCE_MARK, type HullItemModifier } from './modifiers.ts';

export const HULL_ITEM_COLUMNS = ['id', 'kind', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const UNLOCK_LABELS: Record<string, string> = {
  found_onslaught_mk1: 'Found the Onslaught Mk.I in the Abyss',
  found_gate_hauler: 'Found the Gate Hauler',
  found_planetkiller: 'Recovered the first Planetkiller',
  defeated_ziggurat: 'Defeated the Ziggurat at Alpha Site',
};

export type AffixGroup = 'prefixes' | 'suffixes';
export const AFFIX_GROUPS: { key: AffixGroup; label: string; singular: 'prefix' | 'suffix' }[] = [
  { key: 'prefixes', label: 'Prefixes', singular: 'prefix' },
  { key: 'suffixes', label: 'Suffixes', singular: 'suffix' },
];

export interface PoolEntry {
  modifierId: string;
  weight: string;
  inlineText?: string;
}

export interface HullItemDefinition {
  cells: string[];
  id: string;
  kind: string;
  name: string;
  icon: string;
  rarity: string;
  unique: string;
  description: string;
  prefixes: PoolEntry[];
  suffixes: PoolEntry[];
  unlock: string;
  loadedId?: string;
}

export interface HullItemsModel {
  table: CsvTable;
  definitions: HullItemDefinition[];
}

export function parsePoolEntry(text: string): PoolEntry {
  if (!text.startsWith(MODIFIER_REFERENCE_MARK)) {
    return { modifierId: '', weight: '', inlineText: text };
  }
  const [modifierId, ...weightParts] = text.slice(MODIFIER_REFERENCE_MARK.length).split(':');
  return { modifierId: modifierId.trim(), weight: weightParts.join(':').trim() };
}

export function parsePool(text: string): PoolEntry[] {
  return String(text ?? '')
    .split(';')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map(parsePoolEntry);
}

export function formatPoolEntry(entry: PoolEntry): string {
  if (entry.inlineText !== undefined) {
    return entry.inlineText;
  }
  return MODIFIER_REFERENCE_MARK + entry.modifierId + (entry.weight === '' ? '' : `:${entry.weight}`);
}

export function formatPool(pool: PoolEntry[]): string {
  return pool.map(formatPoolEntry).join('; ');
}

export function poolWeight(entry: PoolEntry): number {
  return entry.weight === '' ? 1 : Number.parseFloat(entry.weight);
}

export function poolShares(pool: PoolEntry[]): number[] {
  const weights = pool.map((entry) => Math.max(0, poolWeight(entry) || 0));
  const total = weights.reduce((sum, weight) => sum + weight, 0);
  return weights.map((weight) => (total > 0 ? weight / total : 0));
}

export function isTrueText(text: string): boolean {
  return String(text ?? '').trim().toLowerCase() === 'true';
}

export function parseHullItemsCsv(text: string): HullItemsModel {
  const table = parseCsvTable(text);
  const definitions = table.rows.map((cells): HullItemDefinition => {
    const value = (column: string) => csvCell(table, cells, column);
    const textColumns = trimmedCsvCells(table, cells, ['id', 'kind', 'name', 'icon', 'rarity', 'unique', 'description', 'unlock']);
    return {
      cells,
      ...textColumns,
      prefixes: [...parsePool(value('prefixes')), ...parsePool(value('pool'))],
      suffixes: parsePool(value('suffixes')),
      loadedId: textColumns.id,
    };
  });
  return { table, definitions };
}

export function serializeHullItemsCsv(model: HullItemsModel): string {
  const out = csvTableFromRows(model.table, model.definitions, (definition) =>
    Object.fromEntries(
      HULL_ITEM_COLUMNS.map((column) => [column, column === 'prefixes' || column === 'suffixes' ? formatPool(definition[column]) : definition[column]]),
    ),
  );
  dropCsvColumn(out, 'pool');
  return serializeCsvTable(out);
}

export function markDefinitionsSaved(model: HullItemsModel, saved: HullItemsModel): void {
  for (const definition of model.definitions) {
    const savedDefinition = saved.definitions.find((candidate) =>
      definition.loadedId ? candidate.loadedId === definition.loadedId : !candidate.loadedId && candidate.id === definition.id,
    );
    if (savedDefinition) {
      definition.loadedId = savedDefinition.id;
    }
  }
}

export function uniqueHullItemId(definitions: HullItemDefinition[], base: string): string {
  let id = base;
  let suffix = 2;
  while (definitions.some((definition) => definition.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

function firstReference(catalogue: HullItemModifier[], slot: string): PoolEntry[] {
  const modifier = catalogue.find((candidate) => candidate.slot === slot);
  return modifier ? [{ modifierId: modifier.id, weight: '' }] : [];
}

export function newHullItem(definitions: HullItemDefinition[], kind: string, catalogue: HullItemModifier[]): HullItemDefinition {
  return {
    cells: [],
    id: uniqueHullItemId(definitions, 'new_hull_item'),
    kind,
    name: 'New Hull Item',
    icon: '',
    rarity: '10',
    unique: '',
    description: '',
    unlock: '',
    prefixes: firstReference(catalogue, 'prefix'),
    suffixes: firstReference(catalogue, 'suffix'),
  };
}

function poolIssues(label: string, definition: HullItemDefinition, catalogue: Map<string, HullItemModifier>): string[] {
  const issues: string[] = [];
  const listed: HullItemModifier[] = [];
  for (const group of AFFIX_GROUPS) {
    for (const entry of definition[group.key]) {
      if (entry.inlineText !== undefined) {
        issues.push(`${label}: "${entry.inlineText}" is written inline; pick a modifier instead`);
        continue;
      }
      if (!(poolWeight(entry) > 0)) {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} needs a weight above zero`);
      }
      const modifier = catalogue.get(entry.modifierId);
      if (!modifier) {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} is not in hull_item_modifiers.csv`);
        continue;
      }
      if (modifier.slot !== group.singular) {
        issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, so it cannot be one of its ${group.key}`);
      }
      for (const earlier of listed) {
        if (earlier.id === modifier.id) {
          issues.push(`${label}: ${modifier.id} is listed more than once`);
        } else if (earlier.lines.length === 1 && modifier.lines.length === 1 && earlier.lines[0].effect === modifier.lines[0].effect) {
          issues.push(`${label}: ${modifier.lines[0].effect} is listed twice, as ${earlier.id} and ${modifier.id}`);
        }
      }
      listed.push(modifier);
    }
  }
  return issues;
}

export function validateHullItems(definitions: HullItemDefinition[], vocabulary: Vocabulary, catalogue: Map<string, HullItemModifier>): string[] {
  const issues: string[] = [];
  const seenIds = new Set<string>();
  definitions.forEach((definition, index) => {
    const label = definition.id || `row ${index + 1}`;
    if (!/^[a-z0-9_]+$/.test(definition.id)) {
      issues.push(`${label}: id must use only lowercase letters, digits and underscores`);
    }
    if (seenIds.has(definition.id)) {
      issues.push(`${label}: id is used more than once`);
    }
    seenIds.add(definition.id);
    if (!vocabulary.hullItemKinds.includes(definition.kind)) {
      issues.push(`${label}: kind must be one of ${vocabulary.hullItemKinds.join(', ')}`);
    }
    if (isUnshippedImagePath(definition.icon)) {
      issues.push(`${label}: icon ${definition.icon} is in a folder that is not deployed`);
    }
    if (definition.prefixes.length === 0 && definition.suffixes.length === 0) {
      issues.push(`${label}: it has no prefixes or suffixes`);
    }
    if (!(Number.parseFloat(definition.rarity) >= 0)) {
      issues.push(`${label}: rarity must be zero or more`);
    }
    if (definition.unlock && !vocabulary.hullItemUnlocks.includes(definition.unlock)) {
      issues.push(`${label}: unknown drop condition ${definition.unlock}`);
    }
    issues.push(...poolIssues(label, definition, catalogue));
  });
  return issues;
}
