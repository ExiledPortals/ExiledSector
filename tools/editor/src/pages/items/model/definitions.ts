import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, csvTableFromRows, dropCsvColumn, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { MODIFIER_REFERENCE_MARK, type ItemModifier } from './modifiers.ts';

export const SUBROUTINE_KIND = 'subroutine';
export const SUBROUTINE_TYPE = 'subroutine';
export const HULL_ITEM_TYPE = 'hull_item';
export const ITEM_TYPE_LABELS: Record<string, string> = { [SUBROUTINE_TYPE]: 'Subroutine', [HULL_ITEM_TYPE]: 'Hull item' };
export const SUBROUTINE_COLUMNS = ['id', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const HULL_ITEM_COLUMNS = ['id', 'section', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
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

export interface ItemDefinition {
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

export interface ItemDefinitionsModel {
  subroutineTable: CsvTable;
  hullItemTable: CsvTable;
  definitions: ItemDefinition[];
}

export function itemTypeOfKind(kind: string): string {
  return kind === SUBROUTINE_KIND ? SUBROUTINE_TYPE : HULL_ITEM_TYPE;
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

function parseDefinitionRows(table: CsvTable, kindOf: (cells: string[]) => string): ItemDefinition[] {
  return table.rows.map((cells): ItemDefinition => {
    const value = (column: string) => csvCell(table, cells, column);
    const textColumns = trimmedCsvCells(table, cells, ['id', 'name', 'icon', 'rarity', 'unique', 'description', 'unlock']);
    return {
      cells,
      ...textColumns,
      kind: kindOf(cells),
      prefixes: [...parsePool(value('prefixes')), ...parsePool(value('pool'))],
      suffixes: parsePool(value('suffixes')),
      loadedId: textColumns.id,
    };
  });
}

export function parseItemDefinitions(subroutinesText: string, hullItemsText: string): ItemDefinitionsModel {
  const subroutineTable = parseCsvTable(subroutinesText);
  const hullItemTable = parseCsvTable(hullItemsText);
  return {
    subroutineTable,
    hullItemTable,
    definitions: [
      ...parseDefinitionRows(subroutineTable, () => SUBROUTINE_KIND),
      ...parseDefinitionRows(hullItemTable, (cells) => csvCell(hullItemTable, cells, 'section').trim()),
    ],
  };
}

function serializeDefinitions(table: CsvTable, definitions: ItemDefinition[], columns: readonly string[]): string {
  const out = csvTableFromRows(table, definitions, (definition) =>
    Object.fromEntries(
      columns.map((column) => {
        if (column === 'prefixes' || column === 'suffixes') {
          return [column, formatPool(definition[column])];
        }
        return [column, column === 'section' ? definition.kind : definition[column as keyof ItemDefinition]];
      }),
    ),
  );
  dropCsvColumn(out, 'pool');
  return serializeCsvTable(out);
}

export function serializeSubroutinesCsv(model: ItemDefinitionsModel): string {
  const subroutines = model.definitions.filter((definition) => itemTypeOfKind(definition.kind) === SUBROUTINE_TYPE);
  return serializeDefinitions(model.subroutineTable, subroutines, SUBROUTINE_COLUMNS);
}

export function serializeHullItemsCsv(model: ItemDefinitionsModel): string {
  const hullItems = model.definitions.filter((definition) => itemTypeOfKind(definition.kind) === HULL_ITEM_TYPE);
  return serializeDefinitions(model.hullItemTable, hullItems, HULL_ITEM_COLUMNS);
}

export function setDefinitionKind(definition: ItemDefinition, kind: string): void {
  if (itemTypeOfKind(definition.kind) !== itemTypeOfKind(kind)) {
    definition.cells = [];
  }
  definition.kind = kind;
}

export function markDefinitionsSaved(model: Pick<ItemDefinitionsModel, 'definitions'>, saved: Pick<ItemDefinitionsModel, 'definitions'>): void {
  for (const definition of model.definitions) {
    const savedDefinition = saved.definitions.find((candidate) =>
      definition.loadedId ? candidate.loadedId === definition.loadedId : !candidate.loadedId && candidate.id === definition.id,
    );
    if (savedDefinition) {
      definition.loadedId = savedDefinition.id;
    }
  }
}

export function uniqueItemId(definitions: ItemDefinition[], base: string): string {
  let id = base;
  let suffix = 2;
  while (definitions.some((definition) => definition.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

function firstReference(catalogue: ItemModifier[], slot: string, itemType: string): PoolEntry[] {
  const modifier = catalogue.find((candidate) => candidate.slot === slot && candidate.itemTypes.includes(itemType));
  return modifier ? [{ modifierId: modifier.id, weight: '' }] : [];
}

export function newItemDefinition(definitions: ItemDefinition[], kind: string, catalogue: ItemModifier[]): ItemDefinition {
  const itemType = itemTypeOfKind(kind);
  return {
    cells: [],
    id: uniqueItemId(definitions, itemType === SUBROUTINE_TYPE ? 'new_subroutine' : 'new_hull_item'),
    kind,
    name: itemType === SUBROUTINE_TYPE ? 'New Subroutine' : 'New Hull Item',
    icon: '',
    rarity: '10',
    unique: '',
    description: '',
    unlock: '',
    prefixes: firstReference(catalogue, 'prefix', itemType),
    suffixes: firstReference(catalogue, 'suffix', itemType),
  };
}

function poolIssues(label: string, definition: ItemDefinition, catalogue: Map<string, ItemModifier>): string[] {
  const issues: string[] = [];
  const listed: ItemModifier[] = [];
  const itemType = itemTypeOfKind(definition.kind);
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
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} is not in item_modifiers.csv`);
        continue;
      }
      if (modifier.slot !== group.singular) {
        issues.push(`${label}: ${modifier.id} is a ${modifier.slot}, so it cannot be one of its ${group.key}`);
      }
      if (!modifier.itemTypes.includes(itemType)) {
        issues.push(`${label}: ${modifier.id} only rolls on ${modifier.itemTypes.join(', ') || 'no item type'}, not ${itemType}`);
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

export function validateItemDefinitions(definitions: ItemDefinition[], vocabulary: Vocabulary, catalogue: Map<string, ItemModifier>): string[] {
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
    if (!vocabulary.itemKinds.includes(definition.kind)) {
      issues.push(`${label}: kind must be one of ${vocabulary.itemKinds.join(', ')}`);
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
    if (definition.unlock && !vocabulary.itemUnlocks.includes(definition.unlock)) {
      issues.push(`${label}: unknown drop condition ${definition.unlock}`);
    }
    issues.push(...poolIssues(label, definition, catalogue));
  });
  return issues;
}

export function modifierQualifierFor(definition: Pick<ItemDefinition, 'id'>): string {
  return definition.id.replace(/^unique_/, '');
}
