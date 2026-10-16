import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, dropCsvColumn, emptyCsvTable, parseCsvTable, serializeCsvTable, setCsvCell, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';

export const SOCKETABLE_COLUMNS = ['id', 'kind', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const HULL_SIZE_LABELS = ['Frigate', 'Destroyer', 'Cruiser', 'Capital'];
export const UNLOCK_LABELS: Record<string, string> = {
  found_onslaught_mk1: 'Found the Onslaught Mk.I in the Abyss',
  found_gate_hauler: 'Found the Gate Hauler',
  found_planetkiller: 'Recovered the first Planetkiller',
  defeated_ziggurat: 'Defeated the Ziggurat at Alpha Site',
};

export type AffixGroup = 'prefixes' | 'suffixes';
export const AFFIX_GROUPS: { key: AffixGroup; label: string; singular: string }[] = [
  { key: 'prefixes', label: 'Prefixes', singular: 'prefix' },
  { key: 'suffixes', label: 'Suffixes', singular: 'suffix' },
];

export interface PoolEntry {
  effect: string;
  min: string;
  max: string;
  hullValues?: string[];
  weight: string;
  rangeBeforeHullValues?: { min: string; max: string };
}

export interface SocketableDefinition {
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

export interface SocketablesModel {
  table: CsvTable;
  definitions: SocketableDefinition[];
}

export function parsePool(text: string): PoolEntry[] {
  return String(text ?? '')
    .split(';')
    .map((entry) => entry.trim())
    .filter(Boolean)
    .map((entry) => {
      const parts = entry.split(':').map((part) => part.trim());
      if ((parts[1] ?? '').includes('/')) {
        return {
          effect: parts[0] ?? '',
          min: '',
          max: '',
          hullValues: parts[1].split('/').map((value) => value.trim()),
          weight: parts.length > 2 ? parts[2] : '',
        };
      }
      return { effect: parts[0] ?? '', min: parts[1] ?? '', max: parts[2] ?? '', weight: parts.length > 3 ? parts[3] : '' };
    });
}

export function formatPool(pool: PoolEntry[]): string {
  return pool
    .map((entry) => {
      const values = entry.hullValues ? entry.hullValues.join('/') : `${entry.min}:${entry.max}`;
      return `${entry.effect}:${values}${entry.weight === '' ? '' : `:${entry.weight}`}`;
    })
    .join('; ');
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

export function parseSocketablesCsv(text: string): SocketablesModel {
  const table = parseCsvTable(text);
  const definitions = table.rows.map((cells): SocketableDefinition => {
    const value = (column: string) => csvCell(table, cells, column);
    const id = value('id').trim();
    return {
      cells,
      id,
      kind: value('kind').trim(),
      name: value('name').trim(),
      icon: value('icon').trim(),
      rarity: value('rarity').trim(),
      unique: value('unique').trim(),
      description: value('description').trim(),
      prefixes: [...parsePool(value('prefixes')), ...parsePool(value('pool'))],
      suffixes: parsePool(value('suffixes')),
      unlock: value('unlock').trim(),
      loadedId: id,
    };
  });
  return { table, definitions };
}

export function serializeSocketablesCsv(model: SocketablesModel): string {
  const out = emptyCsvTable(model.table);
  for (const definition of model.definitions) {
    const cells = definition.cells.slice();
    for (const column of SOCKETABLE_COLUMNS) {
      const value = column === 'prefixes' || column === 'suffixes' ? formatPool(definition[column]) : definition[column];
      setCsvCell(out, cells, column, value);
    }
    out.rows.push(cells);
  }
  dropCsvColumn(out, 'pool');
  return serializeCsvTable(out);
}

export function markDefinitionsSaved(model: SocketablesModel, saved: SocketablesModel): void {
  for (const definition of model.definitions) {
    const savedDefinition = saved.definitions.find((candidate) =>
      definition.loadedId ? candidate.loadedId === definition.loadedId : !candidate.loadedId && candidate.id === definition.id,
    );
    if (savedDefinition) {
      definition.loadedId = savedDefinition.id;
    }
  }
}

export function uniqueSocketableId(definitions: SocketableDefinition[], base: string): string {
  let id = base;
  let suffix = 2;
  while (definitions.some((definition) => definition.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

export function newSocketable(definitions: SocketableDefinition[], kind: string, vocabulary: Vocabulary): SocketableDefinition {
  return {
    cells: [],
    id: uniqueSocketableId(definitions, 'new_socketable'),
    kind,
    name: 'New Socketable',
    icon: '',
    rarity: '10',
    unique: '',
    description: '',
    unlock: '',
    prefixes: [{ effect: vocabulary.effectNames[0] ?? '', min: '1', max: '2', weight: '' }],
    suffixes: [{ effect: vocabulary.effectNames[1] ?? '', min: '1', max: '2', weight: '' }],
  };
}

export function validateSocketables(definitions: SocketableDefinition[], vocabulary: Vocabulary): string[] {
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
    if (!vocabulary.socketableKinds.includes(definition.kind)) {
      issues.push(`${label}: kind must be one of ${vocabulary.socketableKinds.join(', ')}`);
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
    if (definition.unlock && !vocabulary.socketableUnlocks.includes(definition.unlock)) {
      issues.push(`${label}: unknown drop condition ${definition.unlock}`);
    }
    const poolEffects = new Set<string>();
    for (const entry of [...definition.prefixes, ...definition.suffixes]) {
      if (poolEffects.has(entry.effect)) {
        issues.push(`${label}: ${entry.effect} is listed more than once across its prefixes and suffixes`);
      }
      poolEffects.add(entry.effect);
      if (!vocabulary.effectInfo.has(entry.effect)) {
        issues.push(`${label}: unknown effect ${entry.effect}`);
      }
      if (!(poolWeight(entry) > 0)) {
        issues.push(`${label}: ${entry.effect} needs a weight above zero`);
      }
      if (entry.hullValues) {
        if (entry.hullValues.length !== HULL_SIZE_LABELS.length) {
          issues.push(`${label}: ${entry.effect} needs exactly four hull size values`);
        }
        if (!entry.hullValues.every(isNumberText)) {
          issues.push(`${label}: ${entry.effect} has a hull size value that is not a number`);
        }
      } else if (!isNumberText(entry.min) || !isNumberText(entry.max)) {
        issues.push(`${label}: ${entry.effect} needs a number for both min and max`);
      } else if (Number.parseFloat(entry.min) > Number.parseFloat(entry.max)) {
        issues.push(`${label}: ${entry.effect} has a min above its max`);
      }
    }
  });
  return issues;
}

export function togglePerHullSize(entry: PoolEntry, perHullSize: boolean): void {
  if (perHullSize) {
    entry.rangeBeforeHullValues = { min: entry.min, max: entry.max };
    entry.hullValues = HULL_SIZE_LABELS.map(() => entry.min || '0');
  } else {
    const range = entry.rangeBeforeHullValues ?? { min: entry.hullValues?.[0] ?? '', max: entry.hullValues?.[0] ?? '' };
    entry.min = range.min;
    entry.max = range.max;
    delete entry.hullValues;
    delete entry.rangeBeforeHullValues;
  }
}

export function setHullValue(entry: PoolEntry, hullIndex: number, value: string): void {
  if (!entry.hullValues) {
    return;
  }
  entry.hullValues[hullIndex] = value;
  while (entry.hullValues.length > HULL_SIZE_LABELS.length && entry.hullValues[entry.hullValues.length - 1] === '') {
    entry.hullValues.pop();
  }
}
