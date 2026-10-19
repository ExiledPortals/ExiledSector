import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { WORMHOLE_ANCHOR_TYPE } from './anchors.ts';
import { familyEffectNames, isFamilyEffect, MODIFIER_FILE_NAMES, type ItemFamily } from './itemFamily.ts';

export const MODIFIER_COLUMNS = ['id', 'slot', 'item_types', 'lines', 'name'] as const;
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
const TIER_BAND_TEXT = /^t([^=]*)=(-?[^-]*)(?:-(.*))?$/;
const TIER_BAND_NUMBER = /^-?\d+(?:\.\d+)?$/;
const TIER_NUMBER = /^\d+$/;

export interface TierBand {
  tier: string;
  min: string;
  max: string;
  unparsedText?: string;
}

export interface RolledValues {
  min: string;
  max: string;
  hullValues?: string[];
  rangeBeforeHullValues?: { min: string; max: string };
  tierBands?: TierBand[];
  rangeBeforeTierBands?: { min: string; max: string };
  tierBandsBeforeRange?: TierBand[];
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

export function isTierBandText(text: string | undefined): boolean {
  const trimmed = String(text ?? '').trim();
  return trimmed.startsWith('t') && trimmed.includes('=');
}

export function parseTierBands(text: string): TierBand[] {
  return text
    .trim()
    .split('/')
    .map((bandText): TierBand => {
      const match = TIER_BAND_TEXT.exec(bandText.trim());
      return match && match[3] !== '' ? { tier: match[1], min: match[2], max: match[3] ?? '' } : { tier: '', min: '', max: '', unparsedText: bandText };
    });
}

export function formatTierBand(band: TierBand): string {
  return band.unparsedText ?? `t${band.tier}=${band.min}${band.max === '' ? '' : `-${band.max}`}`;
}

export function formatTierBands(bands: TierBand[]): string {
  return bands.map(formatTierBand).join('/');
}

export function parseModifierLine(text: string): ModifierLine {
  const parts = text.split(':').map((part) => part.trim());
  if (isTierBandText(parts[1])) {
    const line: ModifierLine = { effect: parts[0] ?? '', min: '', max: '', tierBands: parseTierBands(parts[1]) };
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
  if (line.tierBands) {
    return formatTierBands(line.tierBands);
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

function rangeSummary(min: string, max: string): string {
  return max === '' || min === max ? min : `${min} to ${max}`;
}

export function lineSummary(line: ModifierLine): string {
  if (line.tierBands) {
    return `${line.effect} ${line.tierBands.map((band) => band.unparsedText ?? `t${band.tier}+ ${rangeSummary(band.min, band.max)}`).join(', ')}`;
  }
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
    return { effect, min: '', max: '', tierBands: [{ tier: '1', min: '1', max: '2' }] };
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

function tierBandIssues(label: string, bands: TierBand[], vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  let previousTier = 0;
  bands.forEach((band, index) => {
    if (band.unparsedText !== undefined) {
      issues.push(`${label} has the tier band "${band.unparsedText.trim()}"; write each band as tN=value or tN=min-max`);
      return;
    }
    if (!TIER_NUMBER.test(band.tier)) {
      issues.push(`${label} has a tier band without a whole tier number`);
      return;
    }
    const tier = Number.parseInt(band.tier, 10);
    if (index === 0 && tier !== vocabulary.lowestChartTier) {
      issues.push(`${label} must start its first tier band at t${vocabulary.lowestChartTier}`);
    }
    if (tier <= previousTier) {
      issues.push(`${label} has tier bands that do not rise`);
    }
    if (tier > vocabulary.highestChartTier) {
      issues.push(`${label} has a band at t${tier}, above the highest tier ${vocabulary.highestChartTier}`);
    }
    previousTier = Math.max(previousTier, tier);
    if (!TIER_BAND_NUMBER.test(band.min) || (band.max !== '' && !TIER_BAND_NUMBER.test(band.max))) {
      issues.push(`${label} has a t${band.tier} value that is not a plain number`);
    } else if (band.max !== '' && Number.parseFloat(band.min) > Number.parseFloat(band.max)) {
      issues.push(`${label} has a t${band.tier} min above its max`);
    }
  });
  if (bands.length === 0) {
    issues.push(`${label} needs at least one tier band`);
  }
  return issues;
}

export function rolledValuesIssues(label: string, values: RolledValues, vocabulary?: Vocabulary): string[] {
  if (values.tierBands) {
    return vocabulary ? tierBandIssues(label, values.tierBands, vocabulary) : [];
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
    } else if (itemType === SUBROUTINE_TYPE) {
      issues.push(`${label}: subroutines do not roll modifiers, so remove ${SUBROUTINE_TYPE} from its item types`);
    } else if (itemType === WORMHOLE_ANCHOR_TYPE) {
      issues.push(`${label}: wormhole anchors do not roll modifiers, so remove ${WORMHOLE_ANCHOR_TYPE} from its item types`);
    } else if (!vocabulary.modifierScopes.includes(itemType)) {
      issues.push(`${label}: unknown item type ${itemType}`);
    }
  }
  if (modifier.itemTypes.includes(UNIQUE_SUBROUTINE_TYPE) && modifier.slot !== 'prefix') {
    issues.push(`${label}: unique subroutine lines are prefixes, so a modifier for ${UNIQUE_SUBROUTINE_TYPE} must be a prefix`);
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
        issues.push(`${label}: ${line.effect} has hull size values, but star charts have no hull size; use tier bands or a min-max range`);
      }
      if (family === 'ship' && line.tierBands) {
        issues.push(`${label}: ${line.effect} has tier bands, but only star charts have a tier`);
      }
      issues.push(...rolledValuesIssues(`${label}: ${line.effect}`, line, vocabulary));
    }
    const conventionIssue = idConventionIssue(modifier);
    if (conventionIssue) {
      issues.push(`${label}: ${conventionIssue}`);
    }
    if (modifier.itemTypes.length === 0) {
      issues.push(`${label}: pick at least one item type it rolls on`);
    } else {
      issues.push(...itemTypeIssues(label, modifier, vocabulary, family));
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
    delete values.tierBands;
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

export function togglePerTierBand(values: RolledValues, perTierBand: boolean): void {
  if (perTierBand) {
    const firstHullValue = values.hullValues?.[0];
    delete values.hullValues;
    delete values.rangeBeforeHullValues;
    const min = values.min || firstHullValue || '0';
    values.rangeBeforeTierBands = { min: values.min, max: values.max };
    values.tierBands = values.tierBandsBeforeRange ?? [{ tier: '1', min, max: values.max === values.min ? '' : values.max }];
    delete values.tierBandsBeforeRange;
  } else {
    values.tierBandsBeforeRange = values.tierBands;
    const firstBand = values.tierBands?.[0];
    const range = values.rangeBeforeTierBands ?? { min: firstBand?.min ?? '', max: firstBand?.max || firstBand?.min || '' };
    values.min = range.min;
    values.max = range.max;
    delete values.tierBands;
    delete values.rangeBeforeTierBands;
  }
}

export function addTierBand(values: RolledValues, highestTier: number): void {
  if (!values.tierBands) {
    return;
  }
  const last = values.tierBands[values.tierBands.length - 1];
  const lastTier = last ? Number.parseInt(last.tier, 10) || 0 : 0;
  values.tierBands.push({ tier: String(Math.min(highestTier, lastTier + 1)), min: last?.min ?? '1', max: last?.max ?? '' });
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

export function copiedModifier(
  modifiers: ItemModifier[],
  source: ItemModifier,
  qualifier: string,
  itemType: string,
  takenIds: ReadonlySet<string> = new Set(),
): ItemModifier {
  const lines = JSON.parse(JSON.stringify(source.lines)) as ModifierLine[];
  return {
    cells: [],
    id: uniqueModifierId(modifiers, suggestedModifierId(lines, qualifier), takenIds),
    slot: source.slot,
    itemTypes: [itemType],
    lines,
    name: source.name,
  };
}
