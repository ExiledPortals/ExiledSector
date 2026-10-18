import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, csvTableFromRows, dropCsvColumn, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { MODIFIER_FILE_NAMES, type ItemFamily } from './itemFamily.ts';
import { MODIFIER_REFERENCE_MARK, STAR_CHART_TYPE, type ItemModifier } from './modifiers.ts';

export const SUBROUTINE_KIND = 'subroutine';
export const SUBROUTINE_TYPE = 'subroutine';
export const HULL_ITEM_TYPE = 'hull_item';
export const STAR_CHART_KIND = 'star_chart';
export const ANY_SYSTEM_TYPE = 'any';
export const ITEM_TYPE_LABELS: Record<string, string> = { [SUBROUTINE_TYPE]: 'Subroutine', [HULL_ITEM_TYPE]: 'Hull item', [STAR_CHART_TYPE]: 'Star chart' };
export const SUBROUTINE_COLUMNS = ['id', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const HULL_ITEM_COLUMNS = ['id', 'section', 'name', 'icon', 'rarity', 'unique', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const STAR_CHART_COLUMNS = ['id', 'system_type', 'name', 'icon', 'rarity', 'unique', 'min_tier', 'description', 'prefixes', 'suffixes', 'unlock'] as const;
export const UNLOCK_LABELS: Record<string, string> = {
  found_onslaught_mk1: 'Found the Onslaught Mk.I in the Abyss',
  found_gate_hauler: 'Found the Gate Hauler',
  found_planetkiller: 'Recovered the first Planetkiller',
  defeated_ziggurat: 'Defeated the Ziggurat at Alpha Site',
};
const TIER_RANGE_TEXT = /^t(\d+)(?:(\+)|-(\d+))?$/;

export type AffixGroup = 'prefixes' | 'suffixes';
export const AFFIX_GROUPS: { key: AffixGroup; label: string; singular: 'prefix' | 'suffix' }[] = [
  { key: 'prefixes', label: 'Prefixes', singular: 'prefix' },
  { key: 'suffixes', label: 'Suffixes', singular: 'suffix' },
];

export interface PoolEntry {
  modifierId: string;
  weight: string;
  emptyWeightField?: boolean;
  tiers?: string;
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
  systemType?: string;
  minTier?: string;
  loadedId?: string;
}

export interface ItemDefinitionsModel {
  subroutineTable: CsvTable;
  hullItemTable: CsvTable;
  definitions: ItemDefinition[];
}

export interface StarChartDefinitionsModel {
  starChartTable: CsvTable;
  definitions: ItemDefinition[];
}

export interface FamilyDefinitions {
  family: ItemFamily;
  tables: Record<string, CsvTable>;
  definitions: ItemDefinition[];
  otherFamilyIds: string[];
}

export function itemTypeOfKind(kind: string): string {
  if (kind === SUBROUTINE_KIND) {
    return SUBROUTINE_TYPE;
  }
  return kind === STAR_CHART_KIND ? STAR_CHART_TYPE : HULL_ITEM_TYPE;
}

export function parsePoolEntry(text: string): PoolEntry {
  if (!text.startsWith(MODIFIER_REFERENCE_MARK)) {
    return { modifierId: '', weight: '', inlineText: text };
  }
  const [modifierId, ...fields] = text.slice(MODIFIER_REFERENCE_MARK.length).split(':');
  const entry: PoolEntry = { modifierId: modifierId.trim(), weight: '' };
  if (fields.length > 0 && fields[fields.length - 1].trim().startsWith('t')) {
    entry.tiers = (fields.pop() ?? '').trim();
  }
  entry.weight = fields.join(':').trim();
  if (fields.length > 0 && entry.weight === '') {
    entry.emptyWeightField = true;
  }
  return entry;
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
  const weightText = entry.weight === '' ? (entry.emptyWeightField ? ':' : '') : `:${entry.weight}`;
  return MODIFIER_REFERENCE_MARK + entry.modifierId + weightText + (entry.tiers === undefined ? '' : `:${entry.tiers}`);
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

export function parseStarChartDefinitions(starChartsText: string): StarChartDefinitionsModel {
  const starChartTable = parseCsvTable(starChartsText);
  const definitions = parseDefinitionRows(starChartTable, () => STAR_CHART_KIND).map((definition) => ({
    ...definition,
    systemType: csvCell(starChartTable, definition.cells, 'system_type').trim(),
    minTier: csvCell(starChartTable, definition.cells, 'min_tier').trim(),
  }));
  return { starChartTable, definitions };
}

function definitionColumnValue(definition: ItemDefinition, column: string): unknown {
  switch (column) {
    case 'prefixes':
      return formatPool(definition.prefixes);
    case 'suffixes':
      return formatPool(definition.suffixes);
    case 'section':
      return definition.kind;
    case 'system_type':
      return definition.systemType ?? '';
    case 'min_tier':
      return definition.minTier ?? '';
    default:
      return definition[column as keyof ItemDefinition];
  }
}

function serializeDefinitions(table: CsvTable, definitions: ItemDefinition[], columns: readonly string[]): string {
  const out = csvTableFromRows(table, definitions, (definition) => Object.fromEntries(columns.map((column) => [column, definitionColumnValue(definition, column)])));
  dropCsvColumn(out, 'pool');
  return serializeCsvTable(out);
}

export function serializeSubroutinesCsv(model: Pick<ItemDefinitionsModel, 'subroutineTable' | 'definitions'>): string {
  const subroutines = model.definitions.filter((definition) => itemTypeOfKind(definition.kind) === SUBROUTINE_TYPE);
  return serializeDefinitions(model.subroutineTable, subroutines, SUBROUTINE_COLUMNS);
}

export function serializeHullItemsCsv(model: Pick<ItemDefinitionsModel, 'hullItemTable' | 'definitions'>): string {
  const hullItems = model.definitions.filter((definition) => itemTypeOfKind(definition.kind) === HULL_ITEM_TYPE);
  return serializeDefinitions(model.hullItemTable, hullItems, HULL_ITEM_COLUMNS);
}

export function serializeStarChartsCsv(model: StarChartDefinitionsModel): string {
  const starCharts = model.definitions.filter((definition) => itemTypeOfKind(definition.kind) === STAR_CHART_TYPE);
  return serializeDefinitions(model.starChartTable, starCharts, STAR_CHART_COLUMNS);
}

export function parseFamilyDefinitions(family: ItemFamily, texts: Record<string, string>): FamilyDefinitions {
  if (family === 'chart') {
    const charts = parseStarChartDefinitions(texts['star-charts']);
    const shipIds =
      texts.subroutines !== undefined && texts['hull-items'] !== undefined
        ? parseItemDefinitions(texts.subroutines, texts['hull-items']).definitions.map((definition) => definition.id)
        : [];
    return { family, tables: { 'star-charts': charts.starChartTable }, definitions: charts.definitions, otherFamilyIds: shipIds };
  }
  const items = parseItemDefinitions(texts.subroutines, texts['hull-items']);
  const chartIds = texts['star-charts'] !== undefined ? parseStarChartDefinitions(texts['star-charts']).definitions.map((definition) => definition.id) : [];
  return {
    family,
    tables: { subroutines: items.subroutineTable, 'hull-items': items.hullItemTable },
    definitions: items.definitions,
    otherFamilyIds: chartIds,
  };
}

export function serializeFamilyDefinitions(model: FamilyDefinitions): Record<string, string> {
  if (model.family === 'chart') {
    return { 'star-charts': serializeStarChartsCsv({ starChartTable: model.tables['star-charts'], definitions: model.definitions }) };
  }
  return {
    subroutines: serializeSubroutinesCsv({ subroutineTable: model.tables.subroutines, definitions: model.definitions }),
    'hull-items': serializeHullItemsCsv({ hullItemTable: model.tables['hull-items'], definitions: model.definitions }),
  };
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

export function uniqueItemId(definitions: ItemDefinition[], base: string, takenIds: readonly string[] = []): string {
  let id = base;
  let suffix = 2;
  while (takenIds.includes(id) || definitions.some((definition) => definition.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

function firstReference(catalogue: ItemModifier[], slot: string, itemType: string): PoolEntry[] {
  const modifier = catalogue.find((candidate) => candidate.slot === slot && candidate.itemTypes.includes(itemType));
  return modifier ? [{ modifierId: modifier.id, weight: '' }] : [];
}

function newDefinitionNaming(itemType: string): { id: string; name: string } {
  if (itemType === SUBROUTINE_TYPE) {
    return { id: 'new_subroutine', name: 'New Subroutine' };
  }
  return itemType === STAR_CHART_TYPE ? { id: 'new_star_chart', name: 'New Star Chart' } : { id: 'new_hull_item', name: 'New Hull Item' };
}

export function newItemDefinition(definitions: ItemDefinition[], kind: string, catalogue: ItemModifier[], takenIds: readonly string[] = []): ItemDefinition {
  const itemType = itemTypeOfKind(kind);
  const naming = newDefinitionNaming(itemType);
  const definition: ItemDefinition = {
    cells: [],
    id: uniqueItemId(definitions, naming.id, takenIds),
    kind,
    name: naming.name,
    icon: '',
    rarity: '10',
    unique: '',
    description: '',
    unlock: '',
    prefixes: firstReference(catalogue, 'prefix', itemType),
    suffixes: firstReference(catalogue, 'suffix', itemType),
  };
  if (itemType === STAR_CHART_TYPE) {
    definition.systemType = 'SINGLE';
    definition.minTier = '';
  }
  return definition;
}

export function tierRangeIssue(text: string, vocabulary: Vocabulary): string | null {
  const match = TIER_RANGE_TEXT.exec(text.trim());
  if (!match) {
    return `has the tier range "${text}"; write it as tN, tN-M or tN+`;
  }
  const minTier = Number.parseInt(match[1], 10);
  const maxTier = match[3] === undefined ? minTier : Number.parseInt(match[3], 10);
  if (minTier < vocabulary.lowestChartTier || minTier > vocabulary.highestChartTier) {
    return `has the tier range "${text}", but tiers run from ${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier}`;
  }
  if (match[2] === undefined && (maxTier < minTier || maxTier > vocabulary.highestChartTier)) {
    return `has the tier range "${text}", whose highest tier must be from t${minTier} to t${vocabulary.highestChartTier}`;
  }
  return null;
}

function poolIssues(label: string, definition: ItemDefinition, catalogue: Map<string, ItemModifier>, vocabulary: Vocabulary, family: ItemFamily): string[] {
  const issues: string[] = [];
  const listed: ItemModifier[] = [];
  const itemType = itemTypeOfKind(definition.kind);
  const fileName = MODIFIER_FILE_NAMES[family];
  for (const group of AFFIX_GROUPS) {
    for (const entry of definition[group.key]) {
      if (entry.inlineText !== undefined) {
        issues.push(`${label}: "${entry.inlineText}" is written inline; pick a modifier instead`);
        continue;
      }
      if (entry.emptyWeightField && entry.weight === '') {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} has an empty weight after its colon; write a weight or remove the colon`);
      } else if (entry.weight !== '' && !isNumberText(entry.weight)) {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} has the weight "${entry.weight}", which is not a number`);
      } else if (!(poolWeight(entry) > 0)) {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} needs a weight above zero`);
      }
      if (entry.tiers !== undefined) {
        const tierIssue = itemType === STAR_CHART_TYPE ? tierRangeIssue(entry.tiers, vocabulary) : 'has a tier range, but only star charts have a tier';
        if (tierIssue) {
          issues.push(`${label}: ${entry.modifierId || '(no modifier)'} ${tierIssue}`);
        }
      }
      const modifier = catalogue.get(entry.modifierId);
      if (!modifier) {
        issues.push(`${label}: ${entry.modifierId || '(no modifier)'} is not in ${fileName}`);
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

export function normalisedSystemType(text: string | undefined, vocabulary: Vocabulary): string | null {
  const trimmed = String(text ?? '').trim();
  if (!trimmed || trimmed.toLowerCase() === ANY_SYSTEM_TYPE) {
    return ANY_SYSTEM_TYPE;
  }
  const upper = trimmed.toUpperCase();
  return vocabulary.chartSystemTypes.includes(upper) ? upper : null;
}

export function clearedChartTier(unlock: string, vocabulary: Vocabulary): number | null {
  if (!unlock.startsWith(vocabulary.clearedChartTierPrefix)) {
    return null;
  }
  const tierText = unlock.slice(vocabulary.clearedChartTierPrefix.length);
  const tier = /^[+-]?\d+$/.test(tierText) ? Number.parseInt(tierText, 10) : Number.NaN;
  return tier >= vocabulary.lowestChartTier && tier <= vocabulary.highestChartTier ? tier : null;
}

function starChartIssues(label: string, definition: ItemDefinition, vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  if (normalisedSystemType(definition.systemType, vocabulary) === null) {
    issues.push(`${label}: system type must be ${ANY_SYSTEM_TYPE} or one of ${vocabulary.chartSystemTypes.join(', ')}`);
  }
  const minTier = String(definition.minTier ?? '').trim();
  const minTierNumber = /^[+-]?\d+$/.test(minTier) ? Number.parseInt(minTier, 10) : Number.NaN;
  if (minTier !== '' && !(minTierNumber >= vocabulary.lowestChartTier && minTierNumber <= vocabulary.highestChartTier)) {
    issues.push(`${label}: minimum tier must be blank or a whole number from ${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier}`);
  }
  if (definition.unlock && !vocabulary.itemUnlocks.includes(definition.unlock) && clearedChartTier(definition.unlock, vocabulary) === null) {
    issues.push(
      `${label}: unknown drop condition ${definition.unlock}; use an unlock id or ${vocabulary.clearedChartTierPrefix}N with N from ${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier}`,
    );
  }
  return issues;
}

export function validateItemDefinitions(
  definitions: ItemDefinition[],
  vocabulary: Vocabulary,
  catalogue: Map<string, ItemModifier>,
  family: ItemFamily = 'ship',
  otherFamilyIds: readonly string[] = [],
): string[] {
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
    if (otherFamilyIds.includes(definition.id)) {
      issues.push(`${label}: id is already used by ${family === 'chart' ? 'a hull item or subroutine' : 'a star chart'}`);
    }
    if (family === 'chart') {
      issues.push(...starChartIssues(label, definition, vocabulary));
    } else if (!vocabulary.itemKinds.includes(definition.kind)) {
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
    if (family === 'ship' && definition.unlock && !vocabulary.itemUnlocks.includes(definition.unlock)) {
      issues.push(`${label}: unknown drop condition ${definition.unlock}`);
    }
    issues.push(...poolIssues(label, definition, catalogue, vocabulary, family));
  });
  return issues;
}

export function definitionWarnings(definitions: ItemDefinition[], family: ItemFamily): string[] {
  const warnings: string[] = [];
  for (const definition of definitions) {
    const label = definition.id || '(no id)';
    const unique = isTrueText(definition.unique);
    if (!unique && definition.unlock) {
      warnings.push(`${label}: the drop condition ${definition.unlock} only applies to uniques, so it is ignored`);
    }
    if (family === 'chart' && unique) {
      const gated = AFFIX_GROUPS.flatMap((group) => definition[group.key]).filter((entry) => entry.tiers !== undefined);
      if (gated.length > 0) {
        warnings.push(`${label}: a unique chart rolls every entry at every tier, so the tier ranges on ${gated.map((entry) => entry.modifierId).join(', ')} are ignored`);
      }
    }
  }
  return warnings;
}

export function modifierQualifierFor(definition: Pick<ItemDefinition, 'id'>): string {
  return definition.id.replace(/^unique_/, '');
}
