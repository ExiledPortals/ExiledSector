import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvCell, csvTableFromRows, parseCsvTable, serializeCsvTable, type CsvTable } from '../../../shared/csv.ts';
import { formatPercent, isNumberText } from '../../../shared/decimalText.ts';

export type ChartColumnKind = 'text' | 'integer' | 'number' | 'fraction' | 'flag' | 'choice';

export interface ChartColumn {
  column: string;
  label: string;
  kind: ChartColumnKind;
  help?: string;
  choices?: (vocabulary: Vocabulary) => string[];
}

export interface ChartTableRow {
  cells: string[];
  values: Record<string, string>;
}

export interface ChartTableModel {
  table: CsvTable;
  rows: ChartTableRow[];
}

export interface ChartTableSpec {
  resource: string;
  fileName: string;
  noun: string;
  keyColumn: string;
  columns: ChartColumn[];
  description: string;
  newRow: (model: ChartTableModel, vocabulary: Vocabulary) => Record<string, string>;
  issues: (model: ChartTableModel, vocabulary: Vocabulary) => string[];
  warnings: (model: ChartTableModel, vocabulary: Vocabulary) => string[];
  summary?: (row: ChartTableRow, vocabulary: Vocabulary) => string;
}

export const YES_FLAGS = ['yes', 'true', 'y', '1'];
export const NO_FLAGS = ['no', 'false', 'n', '0'];

export const CHART_TIER_COLUMNS = [
  'tier',
  'fleet_size',
  'fleets_min',
  'fleets_max',
  'officer_level_bonus',
  'officer_count_bonus',
  'fleet_quality',
  'fleet_strength_min',
  'fleet_strength_max',
  'cache_budget',
  'domain_budget',
  'station_budget',
  'derelict_budget',
  'boss',
  'warden_size',
  'weight_clear',
  'weight_boss',
  'note',
] as const;
export const CHART_POI_COLUMNS = ['id', 'kind', 'entity', 'salvage_spec', 'derelict_type', 'cost', 'weight', 'min_tier', 'max_tier', 'guarded', 'requires_mod', 'note'] as const;
export const CHART_FACTION_COLUMNS = ['faction', 'weight', 'fleet_size_bonus', 'fleet_quality_bonus', 'note'] as const;
export const CHART_BLOCKED_ABILITY_COLUMNS = ['ability_id', 'note'] as const;
export const CHART_DROP_COLUMNS = [
  'source',
  'chance',
  'extra_chance',
  'tier_up_chance',
  'currency_chance',
  'min_strength',
  'strength_per_tier',
  'max_tier',
  'note',
] as const;
export const ALWAYS_BLOCKED_ABILITY = 'fracture_jump';
const FLEET_STRENGTH_RANGE = [0, 300];
const MAX_CHARTS_PER_DROP = 4;

type ColumnDetails = Omit<ChartColumn, 'column'>;

function columnsOf(names: readonly string[], details: Record<string, ColumnDetails>): ChartColumn[] {
  return names.map((column) => ({ column, ...details[column] }));
}

const NOTE_COLUMN: ColumnDetails = { label: 'Note', kind: 'text', help: 'Not read by the game.' };

export function parseChartTable(text: string, columns: readonly ChartColumn[]): ChartTableModel {
  const table = parseCsvTable(text);
  return {
    table,
    rows: table.rows.map((cells) => ({ cells, values: Object.fromEntries(columns.map((column) => [column.column, csvCell(table, cells, column.column)])) })),
  };
}

export function serializeChartTable(model: ChartTableModel, columns: readonly ChartColumn[]): string {
  const written = columns.filter((column) => model.table.header.includes(column.column) || model.rows.some((row) => (row.values[column.column] ?? '') !== ''));
  return serializeCsvTable(csvTableFromRows(model.table, model.rows, (row) => Object.fromEntries(written.map((column) => [column.column, row.values[column.column] ?? '']))));
}

function value(row: ChartTableRow, column: string): string {
  return String(row.values[column] ?? '').trim();
}

function rowLabel(spec: Pick<ChartTableSpec, 'keyColumn'>, row: ChartTableRow, index: number): string {
  return value(row, spec.keyColumn) || `row ${index + 1}`;
}

function isFlagText(text: string): boolean {
  const lower = text.toLowerCase();
  return YES_FLAGS.includes(lower) || NO_FLAGS.includes(lower);
}

export function isYes(text: string): boolean {
  return YES_FLAGS.includes(text.trim().toLowerCase());
}

function numberValue(row: ChartTableRow, column: string, fallback: number): number {
  const text = value(row, column);
  return text !== '' && isNumberText(text) ? Number.parseFloat(text) : fallback;
}

function columnFormatIssues(columns: readonly ChartColumn[], row: ChartTableRow, label: string, vocabulary: Vocabulary): string[] {
  const issues: string[] = [];
  for (const column of columns) {
    const text = value(row, column.column);
    if (text === '') {
      continue;
    }
    if ((column.kind === 'integer' || column.kind === 'number' || column.kind === 'fraction') && !isNumberText(text)) {
      issues.push(`${label}: ${column.column} "${text}" is not a number`);
    } else if (column.kind === 'flag' && !isFlagText(text)) {
      issues.push(`${label}: ${column.column} "${text}" must be yes or no`);
    } else if (column.kind === 'choice' && column.choices && !column.choices(vocabulary).includes(text.toLowerCase()) && !column.choices(vocabulary).includes(text.toUpperCase())) {
      issues.push(`${label}: ${column.column} must be one of ${column.choices(vocabulary).join(', ')}`);
    }
  }
  return issues;
}

function roundedIntegerWarnings(columns: readonly ChartColumn[], row: ChartTableRow, label: string): string[] {
  return columns
    .filter((column) => column.kind === 'integer')
    .filter((column) => {
      const text = value(row, column.column);
      return text !== '' && isNumberText(text) && !Number.isInteger(Number.parseFloat(text));
    })
    .map((column) => `${label}: ${column.column} ${value(row, column.column)} is rounded to a whole number`);
}

function duplicateKeyIssues(spec: Pick<ChartTableSpec, 'keyColumn'>, model: ChartTableModel, normalise: (key: string) => string = (key) => key): string[] {
  const issues: string[] = [];
  const seen = new Set<string>();
  model.rows.forEach((row) => {
    const key = normalise(value(row, spec.keyColumn));
    if (key === '') {
      return;
    }
    if (seen.has(key)) {
      issues.push(`${value(row, spec.keyColumn)}: listed more than once, so only its last row is used`);
    }
    seen.add(key);
  });
  return issues;
}

function negativeWarnings(row: ChartTableRow, label: string, columns: string[], floor = 0): string[] {
  return columns
    .filter((column) => value(row, column) !== '' && isNumberText(value(row, column)) && Number.parseFloat(value(row, column)) < floor)
    .map((column) => `${label}: ${column} below ${floor} counts as ${floor}`);
}

function tierSpanWarnings(row: ChartTableRow, label: string, vocabulary: Vocabulary): string[] {
  const warnings: string[] = [];
  for (const column of ['min_tier', 'max_tier']) {
    const text = value(row, column);
    if (text !== '' && isNumberText(text)) {
      const tier = Math.round(Number.parseFloat(text));
      if (tier < vocabulary.lowestChartTier || tier > vocabulary.highestChartTier) {
        warnings.push(`${label}: ${column} ${text} is outside tiers ${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier}, so it is clamped`);
      }
    }
  }
  const minTier = numberValue(row, 'min_tier', vocabulary.lowestChartTier);
  const maxTier = numberValue(row, 'max_tier', vocabulary.highestChartTier);
  if (Math.round(minTier) > Math.round(maxTier)) {
    warnings.push(`${label}: min_tier is above max_tier, so the game swaps them`);
  }
  return warnings;
}

export function tierSpanSummary(row: ChartTableRow, vocabulary: Vocabulary): string {
  const clamp = (tier: number) => Math.max(vocabulary.lowestChartTier, Math.min(vocabulary.highestChartTier, Math.round(tier)));
  const minTier = clamp(numberValue(row, 'min_tier', vocabulary.lowestChartTier));
  const maxTier = clamp(numberValue(row, 'max_tier', vocabulary.highestChartTier));
  return `tiers ${Math.min(minTier, maxTier)}-${Math.max(minTier, maxTier)}`;
}

export const CHART_TIER_SPEC: ChartTableSpec = {
  resource: 'star-chart-tiers',
  fileName: 'star_chart_tiers.csv',
  noun: 'chart tier',
  keyColumn: 'tier',
  description:
    'One row per tier from 1 to 15. A blank value, or a missing tier, uses the built-in value. Fleet size and warden size are percents of a standard fleet of 50 fleet points (about a vanilla heavy patrol); each site budget is spent on points of interest of its kind, in the order domain, stations, derelicts, caches, until the 24-site cap; a leftover smaller than the cheapest site of its kind buys one more with the matching chance; the two weights pick the objective.',
  columns: columnsOf(CHART_TIER_COLUMNS, {
    tier: { label: 'Tier', kind: 'integer' },
    fleet_size: {
      label: 'Fleet size %',
      kind: 'number',
      help: 'Size of each enemy fleet, where 100% is a standard fleet of 50 fleet points (about a vanilla heavy patrol). Scales the number and hull size of ships, capped at 600% per fleet.',
    },
    fleets_min: { label: 'Fleets min', kind: 'integer' },
    fleets_max: {
      label: 'Fleets max',
      kind: 'integer',
      help: 'Fleets rolled past the fleet limit (a LunaLib setting) merge into the fleets that spawn: 75% of their size makes those fleets larger and 25% makes them stronger. Crowded fleets always spawn.',
    },
    officer_level_bonus: { label: 'Officer levels', kind: 'integer', help: 'Extra levels on enemy officers' },
    officer_count_bonus: { label: 'Extra officers', kind: 'integer' },
    fleet_quality: { label: 'Fleet quality %', kind: 'number', help: 'Controls d-mods and weapon quality, 0 to 150; 100% means no d-mods' },
    fleet_strength_min: { label: 'Fleet strength % min', kind: 'integer', help: 'Enemy Exiled Sector skill tree size, 0 to 300 percent' },
    fleet_strength_max: { label: 'Fleet strength % max', kind: 'integer' },
    cache_budget: { label: 'Cache budget', kind: 'number', help: 'Spent on cache points of interest by their cost' },
    domain_budget: {
      label: 'Domain budget',
      kind: 'number',
      help: 'Spent on Domain points of interest (probes, survey ships, motherships) by their cost; these can bring Domain drone defenders',
    },
    station_budget: { label: 'Station budget', kind: 'number', help: 'Spent on station points of interest by their cost' },
    derelict_budget: { label: 'Derelict budget', kind: 'number', help: 'Spent on derelict ship points of interest by their cost' },
    boss: { label: 'Boss', kind: 'flag' },
    warden_size: { label: 'Warden size %', kind: 'number', help: 'Size of the warden fleet, where 100% is a standard fleet of 50 fleet points, capped at 1200%' },
    weight_clear: { label: 'Clear weight', kind: 'number' },
    weight_boss: { label: 'Boss weight', kind: 'number' },
    note: NOTE_COLUMN,
  }),
  newRow: (model, vocabulary) => {
    const used = new Set(model.rows.map((row) => value(row, 'tier')));
    let tier = vocabulary.lowestChartTier;
    while (used.has(String(tier)) && tier < vocabulary.highestChartTier) {
      tier++;
    }
    return { tier: String(tier) };
  },
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = `tier ${value(row, 'tier') || `row ${index + 1}`}`;
      const tierText = value(row, 'tier');
      const tier = isNumberText(tierText) ? Math.round(Number.parseFloat(tierText)) : Number.NaN;
      if (!(tier >= vocabulary.lowestChartTier && tier <= vocabulary.highestChartTier)) {
        issues.push(`${label}: tier must be a whole number from ${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier}`);
      }
      issues.push(...columnFormatIssues(CHART_TIER_SPEC.columns.slice(1), row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_TIER_SPEC, model, (key) => (isNumberText(key) ? String(Math.round(Number.parseFloat(key))) : key))];
  },
  warnings: (model, vocabulary) => {
    const warnings: string[] = [];
    const present = new Set<number>();
    model.rows.forEach((row, index) => {
      const label = `tier ${value(row, 'tier') || `row ${index + 1}`}`;
      present.add(Math.round(numberValue(row, 'tier', 0)));
      warnings.push(...roundedIntegerWarnings(CHART_TIER_SPEC.columns, row, label));
      const builtIn = vocabulary.chartTierDefaults.find((tierDefault) => tierDefault.tier === Math.round(numberValue(row, 'tier', 0)));
      warnings.push(
        ...negativeWarnings(row, label, [
          'fleet_size',
          'officer_level_bonus',
          'officer_count_bonus',
          'fleet_quality',
          'cache_budget',
          'domain_budget',
          'station_budget',
          'derelict_budget',
          'warden_size',
          'weight_clear',
          'weight_boss',
        ]),
      );
      warnings.push(...negativeWarnings(row, label, ['fleets_min'], 1));
      const fleetsMin = Math.max(1, Math.round(numberValue(row, 'fleets_min', builtIn?.fleetsMin ?? 1)));
      if (Math.round(numberValue(row, 'fleets_max', builtIn?.fleetsMax ?? Number.POSITIVE_INFINITY)) < fleetsMin) {
        warnings.push(`${label}: fleets_max is below fleets_min${value(row, 'fleets_min') === '' ? ` (built-in ${fleetsMin})` : ''}, so it is raised to fleets_min`);
      }
      const strengthMin = Math.round(numberValue(row, 'fleet_strength_min', builtIn?.fleetStrengthMin ?? FLEET_STRENGTH_RANGE[0]));
      const strengthMax = Math.round(numberValue(row, 'fleet_strength_max', builtIn?.fleetStrengthMax ?? Number.POSITIVE_INFINITY));
      if (strengthMin > strengthMax) {
        warnings.push(
          `${label}: fleet_strength_min (${strengthMin}) is above fleet_strength_max (${strengthMax}), counting built-in values for blanks, so the game swaps them`,
        );
      }
      for (const column of ['fleet_strength_min', 'fleet_strength_max']) {
        const percent = numberValue(row, column, FLEET_STRENGTH_RANGE[0]);
        if (percent < FLEET_STRENGTH_RANGE[0] || percent > FLEET_STRENGTH_RANGE[1]) {
          warnings.push(`${label}: ${column} is clamped to ${FLEET_STRENGTH_RANGE[0]}-${FLEET_STRENGTH_RANGE[1]}`);
        }
      }
      const wardenSize = numberValue(row, 'warden_size', builtIn?.wardenSize ?? 0);
      if (isYes(value(row, 'boss')) && !(wardenSize > 0)) {
        warnings.push(`${label}: boss is yes but warden_size is ${value(row, 'warden_size') === '' ? 'blank and the built-in value is 0' : '0'}, so there is no boss`);
      }
    });
    const missing: number[] = [];
    for (let tier = vocabulary.lowestChartTier; tier <= vocabulary.highestChartTier; tier++) {
      if (!present.has(tier)) {
        missing.push(tier);
      }
    }
    if (missing.length > 0) {
      warnings.push(`No row for tier ${missing.join(', ')}; the game uses built-in values for ${missing.length === 1 ? 'it' : 'them'}`);
    }
    return warnings;
  },
};

export const CHART_POI_SPEC: ChartTableSpec = {
  resource: 'chart-pois',
  fileName: 'chart_poi.csv',
  noun: 'point of interest',
  keyColumn: 'id',
  description:
    'Loot points of interest the generator places in a system. The kind is the site category: each one spends its own tier budget by cost and picks by weight. Derelicts need a derelict type.',
  columns: columnsOf(CHART_POI_COLUMNS, {
    id: { label: 'Id', kind: 'text' },
    kind: { label: 'Kind', kind: 'choice', choices: (vocabulary) => vocabulary.chartPoiKinds },
    entity: { label: 'Entity type', kind: 'text', help: 'Custom entity type id from data/config/custom_entities.json' },
    salvage_spec: { label: 'Salvage spec', kind: 'text', help: 'Optional salvage spec override' },
    derelict_type: { label: 'Derelict type', kind: 'choice', choices: (vocabulary) => vocabulary.chartDerelictTypes },
    cost: { label: 'Cost', kind: 'number' },
    weight: { label: 'Weight', kind: 'number' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    guarded: { label: 'Guarded', kind: 'flag' },
    requires_mod: {
      label: 'Requires mod',
      kind: 'text',
      help: 'Optional mod id; the site is only placed while that mod is enabled (for IndEvo, also while its derelict stations are turned on)',
    },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', kind: 'cache', entity: '', cost: '1', weight: '1', min_tier: '1', max_tier: '15', guarded: 'no' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_POI_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs an id`);
      }
      const kind = value(row, 'kind').toLowerCase();
      if (!vocabulary.chartPoiKinds.includes(kind)) {
        issues.push(`${label}: kind must be one of ${vocabulary.chartPoiKinds.join(', ')}`);
      }
      if (value(row, 'entity') === '') {
        issues.push(`${label}: needs an entity type`);
      }
      if (kind === 'derelict' && !vocabulary.chartDerelictTypes.includes(value(row, 'derelict_type').toUpperCase())) {
        issues.push(`${label}: a derelict needs a derelict type, one of ${vocabulary.chartDerelictTypes.join(', ')}`);
      }
      issues.push(...columnFormatIssues(CHART_POI_SPEC.columns.filter((column) => column.kind !== 'choice'), row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_POI_SPEC, model)];
  },
  warnings: (model, vocabulary) => {
    const warnings: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_POI_SPEC, row, index);
      warnings.push(...roundedIntegerWarnings(CHART_POI_SPEC.columns, row, label), ...tierSpanWarnings(row, label, vocabulary));
      if (!(numberValue(row, 'cost', 1) > 0) || !(numberValue(row, 'weight', 1) > 0)) {
        warnings.push(`${label}: a cost or weight of 0 or less means it is never placed`);
      }
    });
    return warnings;
  },
  summary: tierSpanSummary,
};

export const CHART_FACTION_SPEC: ChartTableSpec = {
  resource: 'chart-factions',
  fileName: 'chart_factions.csv',
  noun: 'chart faction',
  keyColumn: 'faction',
  description:
    'Which factions can inhabit a system. Every Star Chart rolls one of them by weight as its implicit when it drops, and keeps it. A chart whose faction is not installed gets a random one when its wormhole opens. The fleet size bonus adds percentage points to the tier fleet size and warden size; the fleet quality bonus adds percentage points to the tier fleet quality.',
  columns: columnsOf(CHART_FACTION_COLUMNS, {
    faction: { label: 'Faction id', kind: 'text' },
    weight: { label: 'Weight', kind: 'number' },
    fleet_size_bonus: { label: 'Fleet size bonus', kind: 'number', help: 'Percentage points added to fleet size and warden size' },
    fleet_quality_bonus: { label: 'Fleet quality bonus', kind: 'number', help: 'Percentage points added to fleet quality' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ faction: '', weight: '1', fleet_size_bonus: '0', fleet_quality_bonus: '0' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_FACTION_SPEC, row, index);
      if (value(row, 'faction') === '') {
        issues.push(`${label}: needs a faction id`);
      }
      issues.push(...columnFormatIssues(CHART_FACTION_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_FACTION_SPEC, model)];
  },
  warnings: (model) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(CHART_FACTION_SPEC, row, index);
      const warnings = roundedIntegerWarnings(CHART_FACTION_SPEC.columns, row, label);
      if (!(numberValue(row, 'weight', 1) > 0)) {
        warnings.push(`${label}: a weight of 0 or less means it is never picked`);
      }
      return warnings;
    }),
  summary: (row) => `weight ${value(row, 'weight') || '1'}`,
};

export const CHART_BLOCKED_ABILITY_SPEC: ChartTableSpec = {
  resource: 'chart-blocked-abilities',
  fileName: 'chart_blocked_abilities.csv',
  noun: 'blocked ability',
  keyColumn: 'ability_id',
  description: `Abilities that are switched off while the player is inside a chart system, so other mods' escape abilities cannot leave it. ${ALWAYS_BLOCKED_ABILITY} (Transverse Jump) is always blocked, listed or not.`,
  columns: columnsOf(CHART_BLOCKED_ABILITY_COLUMNS, {
    ability_id: { label: 'Ability id', kind: 'text', help: 'The ability id from data/campaign/abilities.csv' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ ability_id: '' }),
  issues: (model) =>
    model.rows.flatMap((row, index) => (value(row, 'ability_id') === '' ? [`row ${index + 1}: needs an ability id`] : [])),
  warnings: (model) => duplicateKeyIssues(CHART_BLOCKED_ABILITY_SPEC, model).map((issue) => issue.replace('so only its last row is used', 'which changes nothing')),
};

function dropSource(row: ChartTableRow, vocabulary: Vocabulary) {
  return vocabulary.chartDropSources.find((source) => source.id === value(row, 'source').toLowerCase());
}

export function dropTierSummary(row: ChartTableRow, vocabulary: Vocabulary): string {
  const source = dropSource(row, vocabulary);
  const maxTier = Math.max(vocabulary.lowestChartTier, Math.min(vocabulary.highestChartTier, Math.round(numberValue(row, 'max_tier', vocabulary.highestChartTier))));
  const chance = `${formatPercent(Math.max(0, numberValue(row, 'chance', 0)) * 100)}%`;
  if (!source) {
    return '';
  }
  if (source.insideSystem) {
    return `${chance} at the run tier, up to t${maxTier}`;
  }
  const minStrength = Math.max(0, numberValue(row, 'min_strength', 0));
  const perTier = Math.max(0, numberValue(row, 'strength_per_tier', 0));
  return `${chance} from ${formatPercent(minStrength)}${perTier > 0 ? `, +1 tier per ${formatPercent(perTier)} more` : ''}, up to t${maxTier}`;
}

export const CHART_DROP_SPEC: ChartTableSpec = {
  resource: 'chart-drops',
  fileName: 'chart_drops.csv',
  noun: 'chart drop',
  keyColumn: 'source',
  description:
    'One row per drop source. Chance is the expected number of charts (1 = one chart, 0.04 = a 4% chance); extra chance adds a second chart; tier-up is each chart\'s chance to be one tier higher; the currency chance gives a chart-only cipher and only works inside chart systems. Outside chart systems, strength is enemy deployment points for battles and player level for salvage and tech mining: the drop needs at least min strength and gains a tier per strength-per-tier above it. A missing source never drops charts.',
  columns: columnsOf(CHART_DROP_COLUMNS, {
    source: { label: 'Source', kind: 'choice', choices: (vocabulary) => vocabulary.chartDropSources.map((source) => source.id) },
    chance: { label: 'Chance %', kind: 'fraction' },
    extra_chance: { label: 'Extra %', kind: 'fraction' },
    tier_up_chance: { label: 'Tier up %', kind: 'fraction' },
    currency_chance: { label: 'Cipher %', kind: 'fraction' },
    min_strength: { label: 'Min strength', kind: 'number' },
    strength_per_tier: { label: 'Strength per tier', kind: 'number' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    note: NOTE_COLUMN,
  }),
  newRow: (model, vocabulary) => {
    const used = new Set(model.rows.map((row) => value(row, 'source').toLowerCase()));
    return { source: vocabulary.chartDropSources.find((source) => !used.has(source.id))?.id ?? '', chance: '0', max_tier: String(vocabulary.highestChartTier) };
  },
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_DROP_SPEC, row, index);
      if (!dropSource(row, vocabulary)) {
        issues.push(`${label}: source must be one of ${vocabulary.chartDropSources.map((source) => source.id).join(', ')}`);
      }
      issues.push(...columnFormatIssues(CHART_DROP_SPEC.columns.filter((column) => column.kind !== 'choice'), row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_DROP_SPEC, model, (key) => key.toLowerCase())];
  },
  warnings: (model, vocabulary) => {
    const warnings: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_DROP_SPEC, row, index);
      const source = dropSource(row, vocabulary);
      warnings.push(...roundedIntegerWarnings(CHART_DROP_SPEC.columns, row, label));
      warnings.push(...negativeWarnings(row, label, ['chance', 'extra_chance', 'tier_up_chance', 'currency_chance', 'min_strength', 'strength_per_tier']));
      if (numberValue(row, 'chance', 0) > MAX_CHARTS_PER_DROP) {
        warnings.push(`${label}: at most ${MAX_CHARTS_PER_DROP} charts drop at once, so a chance above ${MAX_CHARTS_PER_DROP * 100}% is capped`);
      }
      if (numberValue(row, 'tier_up_chance', 0) > 1) {
        warnings.push(`${label}: tier_up_chance above 100% counts as 100%`);
      }
      if (source && !source.insideSystem && numberValue(row, 'currency_chance', 0) > 0) {
        warnings.push(`${label}: ${source.id} is outside a chart system, where chart currencies never drop, so the game reads currency_chance as 0`);
      }
      const maxTierText = value(row, 'max_tier');
      if (maxTierText !== '' && isNumberText(maxTierText)) {
        const maxTier = Math.round(Number.parseFloat(maxTierText));
        if (maxTier < vocabulary.lowestChartTier || maxTier > vocabulary.highestChartTier) {
          warnings.push(`${label}: max_tier is clamped to ${vocabulary.lowestChartTier}-${vocabulary.highestChartTier}`);
        }
      }
    });
    const listed = new Set(model.rows.map((row) => value(row, 'source').toLowerCase()));
    const missing = vocabulary.chartDropSources.filter((source) => !listed.has(source.id)).map((source) => source.id);
    if (missing.length > 0) {
      warnings.push(`No row for ${missing.join(', ')}, so ${missing.length === 1 ? 'it never drops' : 'they never drop'} charts`);
    }
    return warnings;
  },
  summary: dropTierSummary,
};

export const CHART_TABLE_SPECS: ChartTableSpec[] = [CHART_TIER_SPEC, CHART_POI_SPEC, CHART_FACTION_SPEC, CHART_DROP_SPEC, CHART_BLOCKED_ABILITY_SPEC];
