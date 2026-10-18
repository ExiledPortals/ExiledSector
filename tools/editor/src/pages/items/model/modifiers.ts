import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';

export const MODIFIER_COLUMNS = ['id', 'slot', 'item_types', 'lines', 'name'] as const;
export const MODIFIER_SLOTS = ['implicit', 'prefix', 'suffix'] as const;
export type ModifierSlot = (typeof MODIFIER_SLOTS)[number];
export const MODIFIER_SLOT_LABELS: Record<ModifierSlot, string> = { implicit: 'Implicit', prefix: 'Prefix', suffix: 'Suffix' };
export const HYBRID_ID_PREFIX = 'HYBRID_';
export const MODIFIER_REFERENCE_MARK = '@';
export const HULL_SIZE_LABELS = ['Frigate', 'Destroyer', 'Cruiser', 'Capital'];
export const STAR_CHART_TYPE = 'star_chart';
const MODIFIER_ID_PATTERN = /^[A-Za-z0-9_]+$/;

export interface RolledValues {
  min: string;
  max: string;
  hullValues?: string[];
  rangeBeforeHullValues?: { min: string; max: string };
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
}

export function parseModifierLine(text: string): ModifierLine {
  const parts = text.split(':').map((part) => part.trim());
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

export function formatModifierLines(lines: ModifierLine[]): string {
  return lines
    .map((line) => {
      const values = line.hullValues ? line.hullValues.join('/') : `${line.min}:${line.max}`;
      return `${line.effect}:${values}${line.extraField === undefined ? '' : `:${line.extraField}`}`;
    })
    .join('; ');
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
      slot: columns.slot,
      itemTypes: parseItemTypes(columns.item_types),
      lines: parseModifierLines(columns.lines),
      name: columns.name,
      loadedId: columns.id,
    };
  });
  return { table, modifiers };
}

export function serializeModifiersCsv(model: ModifiersModel): string {
  return serializeCsvTable(
    csvTableFromRows(model.table, model.modifiers, (modifier) => ({
      id: modifier.id,
      slot: modifier.slot,
      item_types: modifier.itemTypes.join(';'),
      lines: formatModifierLines(modifier.lines),
      name: modifier.name,
    })),
  );
}

export function modifierById(modifiers: ItemModifier[]): Map<string, ItemModifier> {
  return new Map(modifiers.map((modifier) => [modifier.id, modifier]));
}

export function isHybrid(modifier: Pick<ItemModifier, 'lines'>): boolean {
  return modifier.lines.length > 1;
}

export function lineSummary(line: ModifierLine): string {
  const values = line.hullValues ? line.hullValues.join(' / ') : line.min === line.max ? line.min : `${line.min} to ${line.max}`;
  return `${line.effect} ${values}`;
}

export function modifierSummary(modifier: Pick<ItemModifier, 'lines'>): string {
  return modifier.lines.map(lineSummary).join(' + ');
}

function effectStem(effect: string): string {
  return effect.replace(/_(PERCENT|MULT|FLAT)$/, '');
}

export function suggestedModifierId(lines: ModifierLine[], qualifier = ''): string {
  const base = lines.length > 1 ? HYBRID_ID_PREFIX + lines.map((line) => effectStem(line.effect)).join('_') : (lines[0]?.effect ?? 'MODIFIER');
  return qualifier ? `${base}_${qualifier.toUpperCase()}` : base;
}

export function uniqueModifierId(modifiers: ItemModifier[], base: string): string {
  let id = base;
  let suffix = 2;
  while (modifiers.some((modifier) => modifier.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return id;
}

export function shipItemTypes(vocabulary: Vocabulary): string[] {
  return vocabulary.itemTypes.filter((itemType) => itemType !== STAR_CHART_TYPE);
}

export function newModifier(modifiers: ItemModifier[], slot: ModifierSlot, vocabulary: Vocabulary): ItemModifier {
  const lines = [{ effect: vocabulary.effectNames[0] ?? '', min: '1', max: '2' }];
  const itemTypes = slot === 'implicit' ? ['hull_item'] : shipItemTypes(vocabulary);
  return { cells: [], id: uniqueModifierId(modifiers, suggestedModifierId(lines, 'NEW')), slot, itemTypes, lines, name: '' };
}

export function idConventionIssue(modifier: Pick<ItemModifier, 'id' | 'lines'>): string | null {
  if (modifier.lines.length === 1) {
    const effect = modifier.lines[0].effect;
    return modifier.id === effect || modifier.id.startsWith(`${effect}_`) ? null : `has one effect, so its id must be ${effect} or ${effect}_<QUALIFIER>`;
  }
  return modifier.lines.length > 1 && !modifier.id.startsWith(HYBRID_ID_PREFIX) ? `has several effects, so its id must start with ${HYBRID_ID_PREFIX}` : null;
}

export function rolledValuesIssues(label: string, values: RolledValues): string[] {
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

export function validateModifiers(modifiers: ItemModifier[], vocabulary: Vocabulary, usage: Map<string, ModifierUsage>): string[] {
  const issues: string[] = [];
  const seenIds = new Set<string>();
  modifiers.forEach((modifier, index) => {
    const label = modifier.id || `row ${index + 1}`;
    if (!MODIFIER_ID_PATTERN.test(modifier.id)) {
      issues.push(`${label}: id must use only letters, digits and underscores`);
    }
    if (seenIds.has(modifier.id)) {
      issues.push(`${label}: id is used more than once`);
    }
    seenIds.add(modifier.id);
    if (!(MODIFIER_SLOTS as readonly string[]).includes(modifier.slot)) {
      issues.push(`${label}: slot must be one of ${MODIFIER_SLOTS.join(', ')}`);
    }
    if (modifier.lines.length === 0) {
      issues.push(`${label}: it has no lines`);
    }
    const seenEffects = new Set<string>();
    for (const line of modifier.lines) {
      if (!vocabulary.effectInfo.has(line.effect)) {
        issues.push(`${label}: unknown effect ${line.effect || '(none)'}`);
      }
      if (seenEffects.has(line.effect)) {
        issues.push(`${label}: ${line.effect} is listed more than once`);
      }
      seenEffects.add(line.effect);
      if (line.extraField !== undefined) {
        issues.push(`${label}: ${line.effect} has a weight; weights belong on the pool entry`);
      }
      if (modifier.slot === 'implicit' && line.hullValues) {
        issues.push(`${label}: ${line.effect} is on an implicit, so it must roll a min-max range`);
      }
      issues.push(...rolledValuesIssues(`${label}: ${line.effect}`, line));
    }
    const conventionIssue = idConventionIssue(modifier);
    if (conventionIssue) {
      issues.push(`${label}: ${conventionIssue}`);
    }
    if (modifier.itemTypes.length === 0) {
      issues.push(`${label}: pick at least one item type it rolls on`);
    }
    for (const itemType of modifier.itemTypes) {
      if (itemType === STAR_CHART_TYPE) {
        issues.push(`${label}: star chart modifiers belong in star_chart_modifiers.csv, not item_modifiers.csv`);
      } else if (!vocabulary.itemTypes.includes(itemType)) {
        issues.push(`${label}: unknown item type ${itemType}`);
      }
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

export function copiedModifier(modifiers: ItemModifier[], source: ItemModifier, qualifier: string, itemType: string): ItemModifier {
  const lines = JSON.parse(JSON.stringify(source.lines)) as ModifierLine[];
  return {
    cells: [],
    id: uniqueModifierId(modifiers, suggestedModifierId(lines, qualifier)),
    slot: source.slot,
    itemTypes: [itemType],
    lines,
    name: source.name,
  };
}
