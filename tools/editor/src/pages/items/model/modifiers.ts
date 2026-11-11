import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { themeTagIssues } from '../../../shared/themeTags.ts';
import { WORMHOLE_ANCHOR_TYPE } from './anchors.ts';
import { DOCTRINE_TYPE } from './doctrines.ts';
import { familyEffectNames, familyTagScope, isFamilyEffect, MODIFIER_FILE_NAMES, type ItemFamily } from './itemFamily.ts';

export const MODIFIER_COLUMNS = ['id', 'affix', 'scopes', 'lines', 'name'] as const;
export const EXCLUSIVE_GROUPS_COLUMN = 'exclusive_groups';
export const TAGS_COLUMN = 'tags';
export const TAGLESS = 'none';
export const MODIFIER_SLOTS = ['implicit', 'prefix', 'suffix'] as const;
export type ModifierSlot = (typeof MODIFIER_SLOTS)[number];
export const MODIFIER_SLOT_LABELS: Record<ModifierSlot, string> = { implicit: 'Implicit', prefix: 'Prefix', suffix: 'Suffix' };
export const HYBRID_ID_PREFIX = 'HYBRID_';
export const MODIFIER_REFERENCE_MARK = '@';
export const HULL_SIZE_LABELS = ['Frigate', 'Destroyer', 'Cruiser', 'Capital'];
export const STAR_CHART_TYPE = 'star_chart';
export const SUBROUTINE_TYPE = 'subroutine';
export const UNIQUE_SUBROUTINE_TYPE = 'unique_subroutine';
const SHIP_ITEM_TYPE = 'hull_item';
const MODIFIER_ID_PATTERN = /^[A-Za-z0-9_]+$/;
const TIER_GROUP_TEXT = /^([^=]*)=(-?[^-]*)(?:-(.*))?$/;
const TIER_GROUP_NUMBER = /^-?\d+(?:\.\d+)?$/;
const TIER_GROUP_VALUE_SEPARATOR = '=';

export interface TierGroupValue {
  group: string;
  min: string;
  max: string;
  unparsedText?: string;
}

export interface RolledValues {
  min: string;
  max: string;
  hullValues?: string[];
  rangeBeforeHullValues?: { min: string; max: string };
  tierGroups?: TierGroupValue[];
  rangeBeforeTierGroups?: { min: string; max: string };
  tierGroupsBeforeRange?: TierGroupValue[];
  switchedOffTierGroups?: TierGroupValue[];
}

export interface ModifierLine extends RolledValues {
  effect: string;
  extraField?: string;
}

export interface ItemModifier {
  cells: string[];
  id: string;
  slot: string;
  itemTypes: string[];
  lines: ModifierLine[];
  name: string;
  exclusiveGroups?: string[];
  tags?: string[];
  loadedId?: string;
}

export interface ModifiersModel {
  table: CsvTable;
  modifiers: ItemModifier[];
}

export interface ModifierUsage {
  items: string[];
  nonUniqueItems: string[];
  itemTypes: string[];
  implicitSections: string[];
  doctrineArchives?: string[];
  corruptionPools?: string[];
}

export function isTierGroupText(text: string | undefined): boolean {
  return String(text ?? '').includes(TIER_GROUP_VALUE_SEPARATOR);
}

export function parseTierGroups(text: string): TierGroupValue[] {
  return text
    .trim()
    .split('/')
    .map((groupText): TierGroupValue => {
      const match = TIER_GROUP_TEXT.exec(groupText.trim());
      return match && match[3] !== '' ? { group: match[1], min: match[2], max: match[3] ?? '' } : { group: '', min: '', max: '', unparsedText: groupText };
    });
}

export function formatTierGroup(groupValue: TierGroupValue): string {
  return groupValue.unparsedText ?? `${groupValue.group}${TIER_GROUP_VALUE_SEPARATOR}${groupValue.min}${groupValue.max === '' ? '' : `-${groupValue.max}`}`;
}

export function formatTierGroups(groupValues: TierGroupValue[]): string {
  return groupValues.map(formatTierGroup).join('/');
}

export function tierGroupIds(vocabulary: Vocabulary): string[] {
  return vocabulary.chartTierGroups.map((group) => group.id);
}

export function isReadableTierGroup(groupValue: TierGroupValue, groupIds: readonly string[]): boolean {
  return groupValue.unparsedText === undefined && groupIds.includes(groupValue.group);
}

export function parseModifierLine(text: string): ModifierLine {
  const parts = text.split(':').map((part) => part.trim());
  if (isTierGroupText(parts[1])) {
    const line: ModifierLine = { effect: parts[0] ?? '', min: '', max: '', tierGroups: parseTierGroups(parts[1]) };
    if (parts.length > 2) {
      line.extraField = parts.slice(2).join(':');
    }
    return line;
  }
  if ((parts[1] ?? '').includes('/')) {
    const line: ModifierLine = { effect: parts[0] ?? '', min: '', max: '', hullValues: parts[1].split('/').map((value) => value.trim()) };
    if (parts.length > 2) {
      line.extraField = parts.slice(2).join(':');
    }
    return line;
  }
  const line: ModifierLine = { effect: parts[0] ?? '', min: parts[1] ?? '', max: parts[2] ?? '' };
  if (parts.length > 3) {
    line.extraField = parts.slice(3).join(':');
  }
  return line;
}

export function parseModifierLines(text: string): ModifierLine[] {
  return String(text ?? '')
    .split(';')
    .map((line) => line.trim())
    .filter(Boolean)
    .map(parseModifierLine);
}

function lineValuesText(line: ModifierLine): string {
  if (line.tierGroups) {
    return formatTierGroups(line.tierGroups);
  }
  return line.hullValues ? line.hullValues.join('/') : `${line.min}:${line.max}`;
}

export function formatModifierLines(lines: ModifierLine[]): string {
  return lines.map((line) => `${line.effect}:${lineValuesText(line)}${line.extraField === undefined ? '' : `:${line.extraField}`}`).join('; ');
}

export function parseItemTypes(text: string): string[] {
  return String(text ?? '')
    .split(';')
    .map((itemType) => itemType.trim())
    .filter(Boolean);
}

export function parseModifiersCsv(text: string): ModifiersModel {
  const table = parseCsvTable(text);
  const modifiers = table.rows.map((cells): ItemModifier => {
    const columns = trimmedCsvCells(table, cells, MODIFIER_COLUMNS);
    return {
      cells,
      id: columns.id,
      slot: columns.affix,
      itemTypes: parseItemTypes(columns.scopes),
      lines: parseModifierLines(columns.lines),
      name: columns.name,
      exclusiveGroups: parseItemTypes(csvCell(table, cells, EXCLUSIVE_GROUPS_COLUMN)),
      tags: parseItemTypes(csvCell(table, cells, TAGS_COLUMN)),
      loadedId: columns.id,
    };
  });
  return { table, modifiers };
}

export function serializeModifiersCsv(model: ModifiersModel): string {
  const writesGroups =
    model.table.header.includes(EXCLUSIVE_GROUPS_COLUMN) || model.modifiers.some((modifier) => (modifier.exclusiveGroups ?? []).length > 0);
  const writesTags = model.table.header.includes(TAGS_COLUMN) || model.modifiers.some((modifier) => (modifier.tags ?? []).length > 0);
  return serializeCsvTable(
    csvTableFromRows(model.table, model.modifiers, (modifier) => ({
      id: modifier.id,
      affix: modifier.slot,
      scopes: modifier.itemTypes.join(';'),
      lines: formatModifierLines(modifier.lines),
      name: modifier.name,
      ...(writesGroups ? { exclusive_groups: (modifier.exclusiveGroups ?? []).join(';') } : {}),
      ...(writesTags ? { tags: (modifier.tags ?? []).join(';') } : {}),
    })),
  );
}

export function modifierById(modifiers: ItemModifier[]): Map<string, ItemModifier> {
  return new Map(modifiers.map((modifier) => [modifier.id, modifier]));
}

export function isHybrid(modifier: Pick<ItemModifier, 'lines'>): boolean {
  return modifier.lines.length > 1;
}

function rangeSummary(min: string, max: string): string {
  return max === '' || min === max ? min : `${min} to ${max}`;
}

export function lineSummary(line: ModifierLine): string {
  if (line.tierGroups) {
    return `${line.effect} ${line.tierGroups.map((groupValue) => groupValue.unparsedText ?? `${groupValue.group} ${rangeSummary(groupValue.min, groupValue.max)}`).join(', ')}`;
  }
  const values = line.hullValues ? line.hullValues.join(' / ') : line.min === line.max ? line.min : `${line.min} to ${line.max}`;
  return `${line.effect} ${values}`;
}

function effectStem(effect: string): string {
  return effect.replace(/_(PERCENT|MULT|FLAT)$/, '');
}

export function suggestedModifierId(lines: ModifierLine[], qualifier = ''): string {
  const base = lines.length > 1 ? HYBRID_ID_PREFIX + lines.map((line) => effectStem(line.effect)).join('_') : (lines[0]?.effect ?? 'MODIFIER');
  return qualifier ? `${base}_${qualifier.toUpperCase()}` : base;
}

export function uniqueModifierId(modifiers: ItemModifier[], base: string, takenIds: ReadonlySet<string> = new Set()): string {
  let id = base;
  let suffix = 2;
  while (takenIds.has(id) || modifiers.some((modifier) => modifier.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

export function shipItemTypes(vocabulary: Vocabulary): string[] {
  return vocabulary.modifierScopes.filter((itemType) => itemType !== STAR_CHART_TYPE && itemType !== SUBROUTINE_TYPE && itemType !== WORMHOLE_ANCHOR_TYPE);
}

export function familyItemTypes(family: ItemFamily, vocabulary: Vocabulary): string[] {
  return family === 'chart' ? [STAR_CHART_TYPE] : shipItemTypes(vocabulary);
}

export function newModifierLine(family: ItemFamily, vocabulary: Vocabulary, slot: string): ModifierLine {
  const effect = familyEffectNames(family, vocabulary)[0] ?? '';
  if (family === 'chart' && slot !== 'implicit') {
    return { effect, min: '', max: '', tierGroups: tierGroupIds(vocabulary).map((group) => ({ group, min: '1', max: '2' })) };
  }
  return { effect, min: '1', max: '2' };
}

export function newModifier(
  modifiers: ItemModifier[],
  slot: ModifierSlot,
  vocabulary: Vocabulary,
  family: ItemFamily = 'ship',
  takenIds: ReadonlySet<string> = new Set(),
): ItemModifier {
  const lines = [newModifierLine(family, vocabulary, slot)];
  const itemTypes = family === 'chart' ? [STAR_CHART_TYPE] : [SHIP_ITEM_TYPE];
  return { cells: [], id: uniqueModifierId(modifiers, suggestedModifierId(lines, 'NEW'), takenIds), slot, itemTypes, lines, name: '' };
}

export function idConventionIssue(modifier: Pick<ItemModifier, 'id' | 'lines'>): string | null {
  if (modifier.lines.length === 1) {
    const effect = modifier.lines[0].effect;
    return modifier.id === effect || modifier.id.startsWith(`${effect}_`) ? null : `has one effect, so its id must be ${effect} or ${effect}_<QUALIFIER>`;
  }
  return modifier.lines.length > 1 && !modifier.id.startsWith(HYBRID_ID_PREFIX) ? `has several effects, so its id must start with ${HYBRID_ID_PREFIX}` : null;
}

function tierGroupListText(groups: readonly string[]): string {
  return `[${groups.join(', ')}]`;
}

function validTierGroupListsText(groupIds: readonly string[]): string {
  const lists = groupIds.map((_, index) => tierGroupListText(groupIds.slice(index)));
  return lists.length > 1 ? `${lists.slice(0, -1).join(', ')} or ${lists[lists.length - 1]}` : (lists[0] ?? '');
}

function tierGroupIssues(label: string, groupValues: TierGroupValue[], vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  const groupIds = tierGroupIds(vocabulary);
  for (const groupValue of groupValues) {
    if (!isReadableTierGroup(groupValue, groupIds)) {
      issues.push(`${label} has the tier group "${formatTierGroup(groupValue).trim()}"; write each group as ${groupIds.join(', ')} followed by =value or =min-max`);
    } else if (!TIER_GROUP_NUMBER.test(groupValue.min) || (groupValue.max !== '' && !TIER_GROUP_NUMBER.test(groupValue.max))) {
      issues.push(`${label} has a ${groupValue.group} value that is not a plain number`);
    } else if (groupValue.max !== '' && Number.parseFloat(groupValue.min) > Number.parseFloat(groupValue.max)) {
      issues.push(`${label} has a ${groupValue.group} min above its max`);
    }
  }
  if (groupValues.length === 0) {
    issues.push(`${label} needs at least one tier group`);
  } else if (groupValues.every((groupValue) => isReadableTierGroup(groupValue, groupIds))) {
    const listed = groupValues.map((groupValue) => groupValue.group);
    if (tierGroupListText(listed) !== tierGroupListText(groupIds.slice(groupIds.indexOf(listed[0])))) {
      issues.push(`${label} has the tier groups ${tierGroupListText(listed)}; list them in order without gaps up to ${groupIds[groupIds.length - 1]}, as ${validTierGroupListsText(groupIds)}`);
    }
  }
  return issues;
}

function readableTierGroupList(line: ModifierLine, groupIds: readonly string[]): string | null {
  return line.tierGroups && line.tierGroups.every((groupValue) => isReadableTierGroup(groupValue, groupIds))
    ? tierGroupListText(line.tierGroups.map((groupValue) => groupValue.group))
    : null;
}

function mismatchedTierGroupIssues(label: string, lines: ModifierLine[], vocabulary: Vocabulary): string[] {
  const groupIds = tierGroupIds(vocabulary);
  const grouped = lines.flatMap((line) => {
    const groups = readableTierGroupList(line, groupIds);
    return groups === null ? [] : [{ effect: line.effect, groups }];
  });
  const firstGroups = grouped[0]?.groups;
  return grouped
    .slice(1)
    .filter((line) => line.groups !== firstGroups)
    .map((line) => `${label}: it gives ${line.effect} the tier groups ${line.groups}, but its other lines use ${firstGroups}; every line of a modifier needs the same groups`);
}

export function rolledValuesIssues(label: string, values: RolledValues, vocabulary?: Vocabulary): string[] {
  if (values.tierGroups) {
    return vocabulary ? tierGroupIssues(label, values.tierGroups, vocabulary) : [];
  }
  if (values.hullValues) {
    const issues: string[] = [];
    if (values.hullValues.length !== HULL_SIZE_LABELS.length) {
      issues.push(`${label} needs exactly four hull size values`);
    }
    if (!values.hullValues.every(isNumberText)) {
      issues.push(`${label} has a hull size value that is not a number`);
    }
    return issues;
  }
  if (!isNumberText(values.min) || !isNumberText(values.max)) {
    return [`${label} needs a number for both min and max`];
  }
  return Number.parseFloat(values.min) > Number.parseFloat(values.max) ? [`${label} has a min above its max`] : [];
}

function itemTypeIssues(label: string, modifier: ItemModifier, vocabulary: Vocabulary, family: ItemFamily): string[] {
  if (family === 'chart') {
    return modifier.itemTypes.length === 1 && modifier.itemTypes[0] === STAR_CHART_TYPE
      ? []
      : [`${label}: star chart modifiers must list only ${STAR_CHART_TYPE} as their item type`];
  }
  const issues: string[] = [];
  for (const itemType of modifier.itemTypes) {
    if (itemType === STAR_CHART_TYPE) {
      issues.push(`${label}: star chart modifiers belong in star_chart_modifiers.csv, not item_modifiers.csv`);
    } else if (itemType === SUBROUTINE_TYPE && modifier.slot !== 'implicit') {
      issues.push(`${label}: subroutines only roll corrupted implicits, so remove ${SUBROUTINE_TYPE} from its item types or make it an implicit`);
    } else if (itemType === WORMHOLE_ANCHOR_TYPE) {
      issues.push(`${label}: wormhole anchors do not roll modifiers, so remove ${WORMHOLE_ANCHOR_TYPE} from its item types`);
    } else if (!vocabulary.modifierScopes.includes(itemType)) {
      issues.push(`${label}: unknown item type ${itemType}`);
    }
  }
  if (modifier.itemTypes.includes(UNIQUE_SUBROUTINE_TYPE) && modifier.slot !== 'prefix') {
    issues.push(`${label}: unique subroutine lines are prefixes, so a modifier for ${UNIQUE_SUBROUTINE_TYPE} must be a prefix`);
  }
  if (modifier.itemTypes.includes(DOCTRINE_TYPE) && modifier.slot !== 'prefix') {
    issues.push(`${label}: doctrine lines are prefixes, so a modifier for ${DOCTRINE_TYPE} must be a prefix`);
  }
  return issues;
}

export function validateModifiers(
  modifiers: ItemModifier[],
  vocabulary: Vocabulary,
  usage: Map<string, ModifierUsage>,
  family: ItemFamily = 'ship',
  otherFileIds: ReadonlySet<string> = new Set(),
): string[] {
  const issues: string[] = [];
  const seenIds = new Set<string>();
  const otherFile = MODIFIER_FILE_NAMES[family === 'chart' ? 'ship' : 'chart'];
  modifiers.forEach((modifier, index) => {
    const label = modifier.id || `row ${index + 1}`;
    if (!MODIFIER_ID_PATTERN.test(modifier.id)) {
      issues.push(`${label}: id must use only letters, digits and underscores`);
    }
    if (seenIds.has(modifier.id)) {
      issues.push(`${label}: id is used more than once`);
    }
    seenIds.add(modifier.id);
    if (otherFileIds.has(modifier.id)) {
      issues.push(`${label}: ${otherFile} already has this id, and ids must be unique across both modifier files`);
    }
    if (!(MODIFIER_SLOTS as readonly string[]).includes(modifier.slot)) {
      issues.push(`${label}: slot must be one of ${MODIFIER_SLOTS.join(', ')}`);
    }
    if (modifier.lines.length === 0) {
      issues.push(`${label}: it has no lines`);
    }
    const seenEffects = new Set<string>();
    for (const line of modifier.lines) {
      if (!isFamilyEffect(family, vocabulary, line.effect)) {
        issues.push(`${label}: unknown ${family === 'chart' ? 'star chart ' : ''}effect ${line.effect || '(none)'}`);
      }
      if (seenEffects.has(line.effect)) {
        issues.push(`${label}: ${line.effect} is listed more than once`);
      }
      seenEffects.add(line.effect);
      if (line.extraField !== undefined && !/^:*$/.test(line.extraField)) {
        issues.push(`${label}: ${line.effect} has a weight; weights belong on the pool entry`);
      }
      if (modifier.slot === 'implicit' && line.hullValues) {
        issues.push(`${label}: ${line.effect} is on an implicit, so it must roll a min-max range`);
      }
      if (family === 'chart' && line.hullValues) {
        issues.push(`${label}: ${line.effect} has hull size values, but star charts have no hull size; use tier groups or a min-max range`);
      }
      if (family === 'ship' && line.tierGroups) {
        issues.push(`${label}: ${line.effect} has tier groups, but only star charts have a tier`);
      }
      issues.push(...rolledValuesIssues(`${label}: ${line.effect}`, line, vocabulary));
    }
    issues.push(...mismatchedTierGroupIssues(label, modifier.lines, vocabulary));
    const conventionIssue = idConventionIssue(modifier);
    if (conventionIssue) {
      issues.push(`${label}: ${conventionIssue}`);
    }
    if (modifier.itemTypes.length === 0) {
      issues.push(`${label}: pick at least one item type it rolls on`);
    } else {
      issues.push(...itemTypeIssues(label, modifier, vocabulary, family));
    }
    for (const group of modifier.exclusiveGroups ?? []) {
      if (!MODIFIER_ID_PATTERN.test(group)) {
        issues.push(`${label}: exclusive group "${group}" must be letters, digits and underscores`);
      }
    }
    const tags = modifier.tags ?? [];
    if (tags.length > 1 && tags.includes(TAGLESS)) {
      issues.push(`${label}: ${TAGLESS} marks a modifier with no tags, so it cannot sit beside other tags`);
    }
    for (const tag of tags) {
      if (!MODIFIER_ID_PATTERN.test(tag)) {
        issues.push(`${label}: tag "${tag}" must be letters, digits and underscores`);
      }
    }
    if (!tags.includes(TAGLESS)) {
      const wellFormedTags = tags.filter((tag) => MODIFIER_ID_PATTERN.test(tag));
      issues.push(...themeTagIssues(vocabulary, familyTagScope(family), wellFormedTags).map((issue) => `${label}: ${issue}`));
    }
    const modifierUsage = usage.get(modifier.id);
    if (modifierUsage && modifierUsage.nonUniqueItems.length > 0 && !modifier.name.trim()) {
      issues.push(`${label}: ${modifierUsage.nonUniqueItems.join(', ')} can roll it, so it needs a name for their common names`);
    }
    for (const itemType of modifierUsage?.itemTypes ?? []) {
      if (!modifier.itemTypes.includes(itemType)) {
        issues.push(`${label}: a ${itemType} rolls it, so item types must include ${itemType}`);
      }
    }
  });
  return issues;
}

export function togglePerHullSize(values: RolledValues, perHullSize: boolean): void {
  if (perHullSize) {
    delete values.tierGroups;
    values.rangeBeforeHullValues = { min: values.min, max: values.max };
    values.hullValues = HULL_SIZE_LABELS.map(() => values.min || '0');
  } else {
    const range = values.rangeBeforeHullValues ?? { min: values.hullValues?.[0] ?? '', max: values.hullValues?.[0] ?? '' };
    values.min = range.min;
    values.max = range.max;
    delete values.hullValues;
    delete values.rangeBeforeHullValues;
  }
}

export function togglePerTierGroup(values: RolledValues, perTierGroup: boolean, groupIds: readonly string[]): void {
  if (perTierGroup) {
    const firstHullValue = values.hullValues?.[0];
    delete values.hullValues;
    delete values.rangeBeforeHullValues;
    const min = values.min || firstHullValue || '0';
    const max = values.max === values.min ? '' : values.max;
    values.rangeBeforeTierGroups = { min: values.min, max: values.max };
    values.tierGroups = values.tierGroupsBeforeRange ?? groupIds.map((group) => ({ group, min, max }));
    delete values.tierGroupsBeforeRange;
  } else {
    values.tierGroupsBeforeRange = values.tierGroups;
    const firstGroup = values.tierGroups?.[0];
    const range = values.rangeBeforeTierGroups ?? { min: firstGroup?.min ?? '', max: firstGroup?.max || firstGroup?.min || '' };
    values.min = range.min;
    values.max = range.max;
    delete values.tierGroups;
    delete values.rangeBeforeTierGroups;
  }
}

export function tierGroupValue(values: RolledValues, group: string): TierGroupValue | undefined {
  return values.tierGroups?.find((groupValue) => groupValue.unparsedText === undefined && groupValue.group === group);
}

export function setTierGroupEnabled(values: RolledValues, groupIds: readonly string[], group: string, enabled: boolean): void {
  const groupIndex = groupIds.indexOf(group);
  if (!values.tierGroups || groupIndex < 0) {
    return;
  }
  const listed = values.tierGroups.filter((groupValue) => isReadableTierGroup(groupValue, groupIds));
  const unreadable = values.tierGroups.filter((groupValue) => !isReadableTierGroup(groupValue, groupIds));
  const listedIndexes = listed.map((groupValue) => groupIds.indexOf(groupValue.group));
  const lowestIndex = listedIndexes.length > 0 ? Math.min(...listedIndexes) : groupIds.length - 1;
  const newLowestIndex = enabled ? Math.min(lowestIndex, groupIndex) : Math.min(Math.max(lowestIndex, groupIndex + 1), groupIds.length - 1);
  const switchedOff = values.switchedOffTierGroups ?? [];
  const nearest = listed.find((groupValue) => groupValue.group === groupIds[lowestIndex]) ?? listed[0];
  const enabledGroups = groupIds.slice(newLowestIndex).map(
    (id): TierGroupValue =>
      listed.find((groupValue) => groupValue.group === id) ??
      switchedOff.find((groupValue) => groupValue.group === id) ?? { group: id, min: nearest?.min ?? '1', max: nearest?.max ?? '' },
  );
  const isEnabled = (groupValue: TierGroupValue) => enabledGroups.some((enabledGroup) => enabledGroup.group === groupValue.group);
  const turnedOff = listed.filter((groupValue) => !isEnabled(groupValue));
  const remembered = [...turnedOff, ...switchedOff.filter((groupValue) => !isEnabled(groupValue) && !turnedOff.some((off) => off.group === groupValue.group))];
  values.tierGroups = [...enabledGroups, ...unreadable];
  if (remembered.length > 0) {
    values.switchedOffTierGroups = remembered;
  } else {
    delete values.switchedOffTierGroups;
  }
}

export function setHullValue(values: RolledValues, hullIndex: number, value: string): void {
  if (!values.hullValues) {
    return;
  }
  values.hullValues[hullIndex] = value;
  while (values.hullValues.length > HULL_SIZE_LABELS.length && values.hullValues[values.hullValues.length - 1] === '') {
    values.hullValues.pop();
  }
}

export function markModifiersSaved(model: ModifiersModel, saved: ModifiersModel): void {
  for (const modifier of model.modifiers) {
    const savedModifier = saved.modifiers.find((candidate) =>
      modifier.loadedId ? candidate.loadedId === modifier.loadedId : !candidate.loadedId && candidate.id === modifier.id,
    );
    if (savedModifier) {
      modifier.loadedId = savedModifier.id;
    }
  }
}
