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
  'level_min',
  'level_max',
  'cache_budget',
  'domain_budget',
  'station_budget',
  'derelict_budget',
  'boss',
  'boss_size',
  'weight_clear',
  'weight_boss',
  'colony_item_chance',
  'note',
] as const;
export const CHART_POI_COLUMNS = ['id', 'kind', 'entity', 'salvage_spec', 'derelict_type', 'cost', 'weight', 'min_tier', 'max_tier', 'guarded', 'requires_mod', 'planet_categories', 'note'] as const;
export const CHART_FACTION_COLUMNS = ['faction', 'weight', 'note'] as const;
export const CHART_BOSS_COLUMNS = ['id', 'weight', 'min_tier', 'max_tier', 'fleet_share', 'note'] as const;
export const CHART_BOSS_STATION_COLUMNS = ['variant', 'factions', 'min_tier', 'max_tier', 'weight', 'note'] as const;
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
export const CHART_COLONY_ITEM_COLUMNS = ['id', 'value', 'note'] as const;
export const CHART_EXPANSION_COLUMNS = ['id', 'weight', 'min_tier', 'max_tier', 'requires_mod', 'icon', 'intel', 'note'] as const;
export const ATLAS_REGION_COLUMNS = [
  'id',
  'weight',
  'min_steps',
  'max_steps',
  'weight_per_step',
  'size',
  'density',
  'neighbours',
  'requires_mod',
  'unlock',
  'layouts',
  'factions',
  'markers',
  'backgrounds',
  'nebula_backgrounds',
  'nebula_chance',
  'light_tint',
  'light_tint_strength',
  'stars',
  'planets',
  'keep_ruins',
  'clouds',
  'walls',
  'storms',
  'cloud_colour',
  'decorations',
  'decoration_density',
  'music',
  'note',
] as const;
export const ATLAS_PHENOMENON_STARS = ['black_hole', 'star_neutron'];
export const ATLAS_PHENOMENON_CATEGORIES = ['cat_magfield'];
export const ATLAS_SYSTEM_TYPES = ['SINGLE', 'BINARY_CLOSE', 'BINARY_FAR', 'TRINARY_2CLOSE', 'TRINARY_1CLOSE_1FAR', 'TRINARY_2FAR'];
export const ATLAS_NORMAL_REGION = 'normal';
export const ATLAS_LAYOUT_COLUMNS = ['id', 'weight', 'system_types', 'min_orbits', 'max_orbits', 'max_orbit_radius', 'terrain', 'feature', 'note'] as const;
export const ATLAS_MARKER_COLUMNS = ['id', 'chance', 'chance_per_step', 'max_chance', 'note'] as const;
export const ATLAS_DECORATION_COLUMNS = [
  'id',
  'sprite',
  'glow',
  'columns',
  'rows',
  'size',
  'size_jitter',
  'count',
  'spread',
  'spin',
  'drift',
  'colour',
  'glow_colour',
  'alpha',
  'pulse',
  'ping',
  'scatter',
  'note',
] as const;
export const ATLAS_FEATURE_COLUMNS = ['id', 'note'] as const;
export const ATLAS_NODE_EFFECT_COLUMNS = ['id', 'effects', 'note'] as const;
export const ATLAS_NODE_EFFECT_IDS = ['powerful_boss', 'infested', 'cleansed', 'sensor_array', 'nexus', 'bastion'];
export const CHART_PHENOMENON_COLUMNS = ['id', 'weight', 'min_tier', 'max_tier', 'requires_mod', 'icon', 'kind', 'note'] as const;
export const CHART_PHENOMENON_KINDS = ['star', 'overlay'];
export const ATLAS_MARKER_IDS = ['void', 'powerful_boss', 'expansion', 'phenomenon'];
export const ATLAS_SPECIAL_COLUMNS = ['id', 'kind', 'placement', 'chance', 'min_steps', 'radius', 'count', 'steps', 'requires_mod', 'unlock', 'faction', 'note'] as const;
export const ATLAS_SPECIAL_PLACEMENTS = ['chunk', 'fixed'];
export const ATLAS_SPECIAL_KINDS = ['infestation', 'sensor_array', 'bastion', 'unique'];
const ATLAS_MOST_RADIUS = 7;
export const CHART_COLONY_ITEM_CEILING_ID = 'ceiling';
export const CHART_LOOT_COLUMNS =['id', 'drop_type', 'scope', 'group', 'mode', 'amount', 'min_tier', 'note'] as const;
export const CHART_LOOT_MODES = ['random', 'value'];
export const ALWAYS_BLOCKED_ABILITY = 'fracture_jump';
const DEFAULT_MAX_LEVEL = 50;

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
    'One row per tier from 1 to 15. A blank value, or a missing tier, uses the built-in value. Fleet size and boss size are percents of a standard fleet of 50 fleet points (about a vanilla heavy patrol); each site budget is spent on points of interest of its kind, in the order domain, stations, derelicts, caches, until the 24-site cap; a leftover smaller than the cheapest site of its kind buys one more with the matching chance; the two weights pick the objective.',
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
    level_min: {
      label: 'Level min',
      kind: 'integer',
      help: 'Lowest enemy ship level (skill tree nodes besides the root), clamped to the max level setting. Boss ships roll one level higher.',
    },
    level_max: { label: 'Level max', kind: 'integer' },
    cache_budget: { label: 'Cache budget', kind: 'number', help: 'Spent on cache points of interest by their cost' },
    domain_budget: {
      label: 'Domain budget',
      kind: 'number',
      help: 'Spent on Domain points of interest (probes, survey ships, motherships) by their cost; these can bring Domain drone defenders',
    },
    station_budget: { label: 'Station budget', kind: 'number', help: 'Spent on station points of interest by their cost' },
    derelict_budget: { label: 'Derelict budget', kind: 'number', help: 'Spent on derelict ship points of interest by their cost' },
    boss: { label: 'Boss', kind: 'flag' },
    boss_size: { label: 'Boss size %', kind: 'number', help: 'Size of the boss fleet, where 100% is a standard fleet of 50 fleet points, capped at 1200%' },
    weight_clear: { label: 'Clear weight', kind: 'number' },
    weight_boss: { label: 'Boss weight', kind: 'number' },
    colony_item_chance: {
      label: 'Colony item %',
      kind: 'fraction',
      help: 'The chance that a run of this tier hides one colony item, before bonuses and the soft ceiling in chart_colony_items.csv; blank means none',
    },
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
          'boss_size',
          'weight_clear',
          'weight_boss',
          'colony_item_chance',
        ]),
      );
      if (numberValue(row, 'colony_item_chance', 0) > 1) {
        warnings.push(`${label}: colony_item_chance above 100% counts as 100%; it is a fraction, so 0.04 is 4%`);
      }
      warnings.push(...negativeWarnings(row, label, ['fleets_min'], 1));
      const fleetsMin = Math.max(1, Math.round(numberValue(row, 'fleets_min', builtIn?.fleetsMin ?? 1)));
      if (Math.round(numberValue(row, 'fleets_max', builtIn?.fleetsMax ?? Number.POSITIVE_INFINITY)) < fleetsMin) {
        warnings.push(`${label}: fleets_max is below fleets_min${value(row, 'fleets_min') === '' ? ` (built-in ${fleetsMin})` : ''}, so it is raised to fleets_min`);
      }
      const levelMin = Math.round(numberValue(row, 'level_min', builtIn?.levelMin ?? 0));
      const levelMax = Math.round(numberValue(row, 'level_max', builtIn?.levelMax ?? Number.POSITIVE_INFINITY));
      if (levelMin > levelMax) {
        warnings.push(`${label}: level_min (${levelMin}) is above level_max (${levelMax}), counting built-in values for blanks, so the game swaps them`);
      }
      warnings.push(...negativeWarnings(row, label, ['level_min', 'level_max']));
      for (const column of ['level_min', 'level_max']) {
        if (numberValue(row, column, 0) > DEFAULT_MAX_LEVEL) {
          warnings.push(`${label}: ${column} is above the default max level (${DEFAULT_MAX_LEVEL}), so it is clamped to the max level setting`);
        }
      }
      const bossSize = numberValue(row, 'boss_size', builtIn?.bossSize ?? 0);
      if (isYes(value(row, 'boss')) && !(bossSize > 0)) {
        warnings.push(`${label}: boss is yes but boss_size is ${value(row, 'boss_size') === '' ? 'blank and the built-in value is 0' : '0'}, so there is no boss`);
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
    planet_categories: {
      label: 'Planet categories',
      kind: 'text',
      help: 'Optional planet categories separated by ;, such as cat_barren; cat_lava. The site is only placed at a planet of one of them, and is left out when the system has none',
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
    'Which factions can inhabit a system. Atlas faction territories are picked from these by weight, and a chart opened without the atlas gets one at random. A system whose faction is not installed gets a random one when its wormhole opens.',
  columns: columnsOf(CHART_FACTION_COLUMNS, {
    faction: { label: 'Faction id', kind: 'text' },
    weight: { label: 'Weight', kind: 'number' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ faction: '', weight: '1' }),
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

export const CHART_BOSS_SPEC: ChartTableSpec = {
  resource: 'chart-bosses',
  fileName: 'chart_bosses.csv',
  noun: 'boss type',
  keyColumn: 'id',
  description:
    "Kinds of Star Chart boss. A run with a boss picks one of the kinds allowed at its tier by weight. A station boss is a station from chart_boss_stations.csv with a guard fleet beside it, and is only picked when the run's faction has a station at that tier. Fleet share scales the tier's boss size for the boss's fleet: the whole boss for a fleet boss, the guard for a station boss. With no rows every boss is a fleet.",
  columns: columnsOf(CHART_BOSS_COLUMNS, {
    id: { label: 'Boss type', kind: 'choice', choices: (vocabulary) => vocabulary.chartBossTypes },
    weight: { label: 'Weight', kind: 'number' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    fleet_share: { label: 'Fleet share', kind: 'number', help: "Multiplies the tier's boss size for this boss's fleet. Empty is 1" },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', weight: '1', min_tier: '1', max_tier: '15', fleet_share: '1' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_BOSS_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a boss type`);
      }
      issues.push(...columnFormatIssues(CHART_BOSS_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_BOSS_SPEC, model)];
  },
  warnings: (model, vocabulary) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(CHART_BOSS_SPEC, row, index);
      const warnings = [...roundedIntegerWarnings(CHART_BOSS_SPEC.columns, row, label), ...tierSpanWarnings(row, label, vocabulary)];
      if (!(numberValue(row, 'weight', 1) > 0)) {
        warnings.push(`${label}: a weight of 0 or less means it is never picked`);
      }
      if (!(numberValue(row, 'fleet_share', 1) > 0)) {
        warnings.push(`${label}: a fleet share of 0 or less gives its fleet the smallest size`);
      }
      return warnings;
    }),
  summary: (row, vocabulary) => `weight ${value(row, 'weight') || '1'}, ${tierSpanSummary(row, vocabulary)}`,
};

export const CHART_BOSS_STATION_SPEC: ChartTableSpec = {
  resource: 'chart-boss-stations',
  fileName: 'chart_boss_stations.csv',
  noun: 'boss station',
  keyColumn: 'variant',
  description:
    "Stations a station boss can be. A run picks by weight among the rows that list its faction and allow its tier; a faction with no such row uses the rows without factions. Rows whose variant the game has not loaded are skipped.",
  columns: columnsOf(CHART_BOSS_STATION_COLUMNS, {
    variant: { label: 'Station variant', kind: 'text', help: 'A station variant id, such as station2_Standard' },
    factions: { label: 'Factions', kind: 'text', help: 'Faction ids separated by ;. Empty serves every faction without a row of its own at that tier' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    weight: { label: 'Weight', kind: 'number' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ variant: '', factions: '', min_tier: '1', max_tier: '15', weight: '1' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_BOSS_STATION_SPEC, row, index);
      if (value(row, 'variant') === '') {
        issues.push(`${label}: needs a station variant id`);
      }
      issues.push(...columnFormatIssues(CHART_BOSS_STATION_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_BOSS_STATION_SPEC, model)];
  },
  warnings: (model, vocabulary) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(CHART_BOSS_STATION_SPEC, row, index);
      const warnings = [...roundedIntegerWarnings(CHART_BOSS_STATION_SPEC.columns, row, label), ...tierSpanWarnings(row, label, vocabulary)];
      if (!(numberValue(row, 'weight', 1) > 0)) {
        warnings.push(`${label}: a weight of 0 or less means it is never picked`);
      }
      return warnings;
    }),
  summary: (row, vocabulary) => `${value(row, 'factions') || 'any faction'}, ${tierSpanSummary(row, vocabulary)}`,
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
    'One row per drop source. Chance is the expected number of charts (1 = one chart, 0.04 = a 4% chance); extra chance adds a second chart; tier-up is each chart\'s chance to be one tier higher; the currency chance gives a chart-only cipher and only works inside chart systems. Outside chart systems, strength is difficulty-scaled enemy deployment points for battles and player level for salvage and tech mining: the drop needs at least min strength and gains a tier per strength-per-tier above it. Battle chance is per 100 deployment points and its tier comes from the strongest defeated NPC level, or from strength per tier while the skill tree is switched off. A missing source never drops charts.',
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

export const CHART_LOOT_SPEC: ChartTableSpec = {
  resource: 'chart-loot',
  fileName: 'chart_loot.csv',
  noun: 'chart loot row',
  keyColumn: 'id',
  description:
    'Vanilla loot in chart systems. A row with no scope puts a vanilla drop group under a drop type, so that type\'s bonus scales every site entry from that group (groups that are not listed, such as colony items, are never scaled). A row with a scope adds a base entry: to every site of that category, to every chart fleet (fleet), or to the boss on top of its fleet rows (boss). Random rows roll the group amount times; value rows give that much value from it. Fleet and boss amounts are per 100 fleet points, and every base entry is scaled by its drop type bonus. Groups the game has not loaded are skipped.',
  columns: columnsOf(CHART_LOOT_COLUMNS, {
    id: { label: 'Id', kind: 'text', help: 'Any unique name; by convention the group for mappings and scope_group for base entries' },
    drop_type: { label: 'Drop type', kind: 'choice', choices: (vocabulary) => vocabulary.chartLootDropTypes },
    scope: { label: 'Scope', kind: 'choice', choices: (vocabulary) => vocabulary.chartLootScopes },
    group: { label: 'Drop group', kind: 'text', help: 'A vanilla (or modded) drop group from drop_groups.csv' },
    mode: { label: 'Mode', kind: 'choice', choices: () => CHART_LOOT_MODES },
    amount: { label: 'Amount', kind: 'number', help: 'Chances for random rows, value for value rows; per 100 fleet points for fleet and boss' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', drop_type: 'weapon', scope: '', group: '', mode: '', amount: '', min_tier: '' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    const mappedTypes = new Map<string, string>();
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_LOOT_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs an id`);
      }
      if (value(row, 'group') === '') {
        issues.push(`${label}: needs a drop group`);
      } else if (vocabulary.chartColonyItemGroups.includes(value(row, 'group'))) {
        issues.push(`${label}: group ${value(row, 'group')} holds colony items, which only come from the run's single capped roll`);
      }
      const dropType = value(row, 'drop_type').toLowerCase();
      if (!vocabulary.chartLootDropTypes.includes(dropType)) {
        issues.push(`${label}: drop_type must be one of ${vocabulary.chartLootDropTypes.join(', ')}`);
      }
      issues.push(...columnFormatIssues(CHART_LOOT_SPEC.columns.filter((column) => column.column !== 'drop_type'), row, label, vocabulary));
      if (value(row, 'scope') === '') {
        const mapped = mappedTypes.get(value(row, 'group'));
        if (mapped !== undefined && mapped !== dropType) {
          issues.push(`${label}: group ${value(row, 'group')} already belongs to ${mapped}`);
        }
        mappedTypes.set(value(row, 'group'), mapped ?? dropType);
      } else {
        if (!CHART_LOOT_MODES.includes(value(row, 'mode').toLowerCase())) {
          issues.push(`${label}: a base entry needs a mode, random or value`);
        }
        if (!(numberValue(row, 'amount', 0) > 0)) {
          issues.push(`${label}: a base entry needs an amount above 0`);
        }
      }
    });
    return [...issues, ...duplicateKeyIssues(CHART_LOOT_SPEC, model)];
  },
  warnings: (model, vocabulary) => {
    const warnings: string[] = [];
    const mappedGroups = new Map(model.rows.filter((row) => value(row, 'scope') === '').map((row) => [value(row, 'group'), value(row, 'drop_type').toLowerCase()]));
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_LOOT_SPEC, row, index);
      warnings.push(...roundedIntegerWarnings(CHART_LOOT_SPEC.columns, row, label));
      if (value(row, 'scope') === '' && (value(row, 'mode') !== '' || value(row, 'amount') !== '')) {
        warnings.push(`${label}: a row with no scope only maps its group, so mode and amount are ignored`);
      }
      const mappedType = mappedGroups.get(value(row, 'group'));
      if (value(row, 'scope') !== '' && mappedType !== undefined && mappedType !== value(row, 'drop_type').toLowerCase()) {
        warnings.push(`${label}: group ${value(row, 'group')} is mapped to ${mappedType}, but this entry is scaled as ${value(row, 'drop_type')}`);
      }
      const minTierText = value(row, 'min_tier');
      if (minTierText !== '' && isNumberText(minTierText)) {
        const minTier = Math.round(Number.parseFloat(minTierText));
        if (minTier < vocabulary.lowestChartTier || minTier > vocabulary.highestChartTier) {
          warnings.push(`${label}: min_tier is clamped to ${vocabulary.lowestChartTier}-${vocabulary.highestChartTier}`);
        }
      }
    });
    return warnings;
  },
  summary: (row) => (value(row, 'scope') === '' ? `${value(row, 'drop_type')} group` : `${value(row, 'scope')} ${value(row, 'mode')} ${value(row, 'amount')}`),
};

export const CHART_COLONY_ITEM_SPEC: ChartTableSpec = {
  resource: 'chart-colony-items',
  fileName: 'chart_colony_items.csv',
  noun: 'colony item',
  keyColumn: 'id',
  description: `Colony items a chart run can hide. Each run makes one roll at its tier's colony item chance, raised by the Colony Item bonus and bent under a soft ceiling, and on a success one of these items (picked by weight) goes to the boss, or to a station or Domain site. The ${CHART_COLONY_ITEM_CEILING_ID} row is not an item: its value is that ceiling, which no bonus reaches. Items the game has not loaded are skipped.`,
  columns: columnsOf(CHART_COLONY_ITEM_COLUMNS, {
    id: { label: 'Item id', kind: 'text', help: `A special item id from special_items.csv, or ${CHART_COLONY_ITEM_CEILING_ID}` },
    value: { label: 'Weight / ceiling', kind: 'number' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', value: '10' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_COLONY_ITEM_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs an item id`);
      }
      issues.push(...columnFormatIssues(CHART_COLONY_ITEM_SPEC.columns, row, label, vocabulary));
      const amount = numberValue(row, 'value', 0);
      if (value(row, 'id') === CHART_COLONY_ITEM_CEILING_ID) {
        if (!(amount > 0 && amount < 1)) {
          issues.push(`${label}: the ceiling must be above 0 and below 1, so an item is never certain`);
        }
      } else if (!(amount > 0)) {
        issues.push(`${label}: an item needs a weight above 0`);
      }
    });
    return [...issues, ...duplicateKeyIssues(CHART_COLONY_ITEM_SPEC, model)];
  },
  warnings: (model) =>
    model.rows.some((row) => value(row, 'id') === CHART_COLONY_ITEM_CEILING_ID)
      ? []
      : [`No ${CHART_COLONY_ITEM_CEILING_ID} row, so the ceiling is 25%`],
  summary: (row) => (value(row, 'id') === CHART_COLONY_ITEM_CEILING_ID ? `ceiling ${value(row, 'value')}` : `weight ${value(row, 'value')}`),
};

export const CHART_EXPANSION_SPEC: ChartTableSpec = {
  resource: 'chart-expansions',
  fileName: 'chart_expansions.csv',
  noun: 'chart expansion',
  keyColumn: 'id',
  description:
    'Expansions a Star Chart run can contain, such as the Spatial Breach. A run gets one when its chart has a matching chance line (CHART_<EXPANSION>_CHANCE) and the run tier is in range. Each id needs an expansion class in the mod, and rows for an id without one are skipped. Weight, icon and intel are kept for the atlas and are not used yet.',
  columns: columnsOf(CHART_EXPANSION_COLUMNS, {
    id: { label: 'Expansion', kind: 'choice', choices: (vocabulary) => vocabulary.chartExpansions },
    weight: { label: 'Weight', kind: 'number' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    requires_mod: { label: 'Requires mod', kind: 'text', help: 'Optional mod id; the expansion only appears while that mod is enabled' },
    icon: { label: 'Icon', kind: 'text', help: 'Sprite path for the atlas marker' },
    intel: { label: 'Intel', kind: 'text', help: 'Reserved for the Outer Reach Intel item' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', weight: '1', min_tier: '1', max_tier: '15' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_EXPANSION_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs an expansion id`);
      }
      issues.push(...columnFormatIssues(CHART_EXPANSION_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_EXPANSION_SPEC, model)];
  },
  warnings: (model, vocabulary) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(CHART_EXPANSION_SPEC, row, index);
      return [...roundedIntegerWarnings(CHART_EXPANSION_SPEC.columns, row, label), ...tierSpanWarnings(row, label, vocabulary)];
    }),
  summary: tierSpanSummary,
};

function weightListIds(row: ChartTableRow, column: string): string[] {
  return value(row, column)
    .split(';')
    .map((entry) => entry.split(':')[0].trim())
    .filter((id) => id !== '');
}

function forbiddenIdIssues(row: ChartTableRow, column: string, label: string, forbidden: string[], reason: string): string[] {
  return weightListIds(row, column)
    .filter((id) => forbidden.includes(id))
    .map((id) => `${label}: ${column} lists ${id}, but ${reason}`);
}

function weightListIssues(row: ChartTableRow, column: string, label: string): string[] {
  return value(row, column)
    .split(';')
    .map((entry) => entry.trim())
    .filter((entry) => entry !== '')
    .flatMap((entry) => {
      const separator = entry.indexOf(':');
      const id = separator < 0 ? entry : entry.slice(0, separator).trim();
      const weight = separator < 0 ? '1' : entry.slice(separator + 1).trim();
      return id === '' || !isNumberText(weight) || Number.parseFloat(weight) < 0
        ? [`${label}: ${column} entry "${entry}" must be an id or id:weight with a weight of 0 or more`]
        : [];
    });
}

export const ATLAS_REGION_SPEC: ChartTableSpec = {
  resource: 'atlas-regions',
  fileName: 'atlas_regions.csv',
  noun: 'atlas region',
  keyColumn: 'id',
  description:
    'Biomes of the Outer Reaches atlas. Biomes cover the whole map: sites are scattered about five cells apart, each picks a row by weight, and every cell belongs to the nearest site, with larger biomes reaching further. The normal row is the space factions hold and is picked like any other row, so its weight sets how common it is; its sites chain together, while a site of any other row never sits beside another site of the same row, so special biomes stay small pockets. A biome sets which layouts and factions its systems use, leans phenomenon and expansion markers up or down, and sets the hyperspace clouds and scattered decorations from atlas_decorations.csv. Clouds are impassable: no system sits under one and no lane crosses one. They gather into clumps inside a biome (clouds) and into ridges with passes along some borders between different biomes (walls). A biome behind a mod or an unlock is decided when its site is first revealed; while locked, the site takes its next pick.',
  columns: columnsOf(ATLAS_REGION_COLUMNS, {
    id: { label: 'Region id', kind: 'text' },
    weight: { label: 'Weight', kind: 'number' },
    min_steps: { label: 'Min steps', kind: 'integer', help: 'A biome is never centred closer to the Gateway than this many cells' },
    max_steps: { label: 'Max steps', kind: 'integer', help: 'Optional: a biome is never centred further out than this many cells' },
    weight_per_step: { label: 'Weight per step', kind: 'number', help: 'Added to the weight for every cell beyond min_steps; negative fades a biome out further away' },
    size: { label: 'Size', kind: 'number', help: '0.5 to 2: how far a site reaches against its neighbours. Empty is 1' },
    density: { label: 'Density', kind: 'fraction', help: 'Share of the cells that may hold a system which do. 0 keeps only the every-other-cell grid. Empty is 1' },
    neighbours: { label: 'Neighbours', kind: 'text', help: 'region:multiplier; ... on the weight next to those biomes. 0 means never next to it' },
    requires_mod: { label: 'Requires mod', kind: 'text', help: 'Optional mod id; sites revealed while that mod is off take their next pick' },
    unlock: { label: 'Unlock', kind: 'text', help: 'Optional player memory flag, such as $encounteredThreat; sites revealed before it is set take their next pick' },
    layouts: { label: 'Layouts', kind: 'text', help: 'layout:weight; ... from atlas_layouts.csv. Empty uses every layout at its own weight' },
    factions: {
      label: 'Factions',
      kind: 'text',
      help: 'faction:weight; ... from chart_factions.csv: the only factions this biome holds, kept in territories. On the normal row it is the pool every territory draws from. Empty keeps the territory faction',
    },
    markers: { label: 'Markers', kind: 'text', help: 'phenomenon or expansion id:multiplier; ... Unlisted markers keep their weight' },
    backgrounds: { label: 'Backgrounds', kind: 'text', help: 'Background images for systems without a nebula, separated by ;. Empty uses vanilla\'s' },
    nebula_backgrounds: { label: 'Nebula backgrounds', kind: 'text', help: 'Background images for systems with a nebula. Empty uses vanilla\'s' },
    nebula_chance: { label: 'Nebula chance', kind: 'fraction', help: 'Chance of a system-wide nebula. Empty uses the star age\'s chance' },
    light_tint: { label: 'Light tint', kind: 'text', help: 'Colour the system light leans towards, as #RRGGBB' },
    light_tint_strength: { label: 'Tint strength', kind: 'fraction', help: '0 keeps the star\'s light, 1 is the tint colour' },
    stars: { label: 'Stars', kind: 'text', help: 'star type:multiplier; ... on vanilla\'s star weights. Black holes and neutron stars only come from phenomena' },
    planets: { label: 'Planets', kind: 'text', help: 'planet category:multiplier; ... such as cat_frozen:3 or cat_lava:0' },
    keep_ruins: { label: 'Keep ruins', kind: 'flag', help: 'Planets keep their ruins; other regions strip them' },
    clouds: { label: 'Clouds', kind: 'fraction', help: 'How readily impassable cloud clumps gather inside this biome. 0 means none' },
    walls: { label: 'Walls', kind: 'fraction', help: 'Chance that a border with a different biome rises into an impassable cloud ridge with passes, averaged with the other biome' },
    storms: { label: 'Storms', kind: 'fraction', help: 'Share of the clouds that can storm and flash with lightning' },
    cloud_colour: { label: 'Cloud colour', kind: 'text', help: 'Optional cloud tint as #RRGGBB. Empty keeps the hyperspace texture colour' },
    decorations: { label: 'Decorations', kind: 'text', help: 'decoration:weight; ... from atlas_decorations.csv, scattered between systems' },
    decoration_density: { label: 'Decoration density', kind: 'number', help: 'Decorations placed per cell on average. Empty is 1 when decorations are listed' },
    music: { label: 'Music', kind: 'text', help: 'Optional music set id from sounds.json, played in its systems' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', weight: '1', min_steps: '0' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_REGION_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a region id`);
      }
      issues.push(...columnFormatIssues(ATLAS_REGION_SPEC.columns, row, label, vocabulary));
      for (const column of ['layouts', 'factions', 'markers', 'stars', 'planets', 'neighbours', 'decorations']) {
        issues.push(...weightListIssues(row, column, label));
      }
      issues.push(...forbiddenIdIssues(row, 'stars', label, ATLAS_PHENOMENON_STARS, 'only phenomena add it'));
      issues.push(...forbiddenIdIssues(row, 'planets', label, ATLAS_PHENOMENON_CATEGORIES, 'only phenomena add it'));
      for (const column of ['light_tint', 'cloud_colour']) {
        const tint = value(row, column);
        if (tint !== '' && !/^#?[0-9a-fA-F]{6}$/.test(tint)) {
          issues.push(`${label}: ${column} "${tint}" must be a colour like #A0C8FF`);
        }
      }
      const size = value(row, 'size');
      if (size !== '' && (Number.parseFloat(size) < 0.5 || Number.parseFloat(size) > 2)) {
        issues.push(`${label}: size ${size} must be from 0.5 to 2`);
      }
      const gated = value(row, 'requires_mod') !== '' || value(row, 'unlock') !== '' || numberValue(row, 'min_steps', 0) > 0;
      if (value(row, 'id') === ATLAS_NORMAL_REGION && gated) {
        issues.push(`${label}: normal space is always open, so it takes no mod, unlock or min_steps`);
      }
    });
    if (!model.rows.some((row) => value(row, 'id') === ATLAS_NORMAL_REGION)) {
      issues.push(`Needs a ${ATLAS_NORMAL_REGION} row for normal space`);
    }
    return [...issues, ...duplicateKeyIssues(ATLAS_REGION_SPEC, model)];
  },
  warnings: (model) => model.rows.flatMap((row, index) => roundedIntegerWarnings(ATLAS_REGION_SPEC.columns, row, rowLabel(ATLAS_REGION_SPEC, row, index))),
  summary: (row) =>
    value(row, 'id') === ATLAS_NORMAL_REGION ? `open space, weight ${value(row, 'weight') || '1'}` : `weight ${value(row, 'weight') || '1'}, from ${value(row, 'min_steps') || '0'} cells`,
};

export const ATLAS_DECORATION_SPEC: ChartTableSpec = {
  resource: 'atlas-decorations',
  fileName: 'atlas_decorations.csv',
  noun: 'atlas decoration',
  keyColumn: 'id',
  description:
    'Props drawn in the space between atlas systems, picked by the decorations column of atlas_regions.csv. Each placement draws count pieces of one sprite (picked from the list) within spread cells of a spot kept clear of systems. A sheet sprite is cut into columns by rows frames. Sizes and distances are in cells; spin is degrees per second at most; drift is how far a piece sways. Pulse fades the glow in and out and ping sends out rings, like a warning beacon. Scatter places other decorations around the same spot, such as wreckage around a derelict.',
  columns: columnsOf(ATLAS_DECORATION_COLUMNS, {
    id: { label: 'Decoration id', kind: 'text' },
    sprite: { label: 'Sprites', kind: 'text', help: 'Image paths separated by ;, one picked per placement' },
    glow: { label: 'Glow', kind: 'text', help: 'Optional glow sheet drawn additively over the same frame' },
    columns: { label: 'Columns', kind: 'integer', help: 'Frames across the sheet. Empty is 1' },
    rows: { label: 'Rows', kind: 'integer', help: 'Frames down the sheet. Empty is 1' },
    size: { label: 'Size', kind: 'number', help: 'Longest side in cells' },
    size_jitter: { label: 'Size jitter', kind: 'fraction', help: 'How far each piece may be larger or smaller' },
    count: { label: 'Count', kind: 'integer', help: 'Pieces per placement' },
    spread: { label: 'Spread', kind: 'number', help: 'How far pieces scatter around the spot, in cells' },
    spin: { label: 'Spin', kind: 'number', help: 'Most degrees per second a piece turns' },
    drift: { label: 'Drift', kind: 'number', help: 'How far a piece sways, in cells' },
    colour: { label: 'Colour', kind: 'text', help: 'Optional tint as #RRGGBB' },
    glow_colour: { label: 'Glow colour', kind: 'text', help: 'Optional glow tint as #RRGGBB' },
    alpha: { label: 'Alpha', kind: 'fraction' },
    pulse: { label: 'Pulse', kind: 'number', help: 'Times per second the glow fades in and out. Empty keeps it steady' },
    ping: { label: 'Ping', kind: 'number', help: 'Seconds between rings sent out in the glow colour. Empty sends none' },
    scatter: { label: 'Scatter', kind: 'text', help: 'Other decoration ids separated by ;, placed around the same spot' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', sprite: '', size: '0.05', count: '1', alpha: '1' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    const ids = new Set(model.rows.map((row) => value(row, 'id')));
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_DECORATION_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a decoration id`);
      }
      if (value(row, 'sprite') === '') {
        issues.push(`${label}: needs at least one sprite`);
      }
      issues.push(...columnFormatIssues(ATLAS_DECORATION_SPEC.columns, row, label, vocabulary));
      for (const column of ['colour', 'glow_colour']) {
        const tint = value(row, column);
        if (tint !== '' && !/^#?[0-9a-fA-F]{6}$/.test(tint)) {
          issues.push(`${label}: ${column} "${tint}" must be a colour like #A0C8FF`);
        }
      }
      for (const scatterId of value(row, 'scatter').split(';').map((entry) => entry.trim()).filter((entry) => entry !== '')) {
        if (!ids.has(scatterId) || scatterId === value(row, 'id')) {
          issues.push(`${label}: scatter "${scatterId}" must be another decoration id`);
        }
      }
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_DECORATION_SPEC, model)];
  },
  warnings: (model) => model.rows.flatMap((row, index) => roundedIntegerWarnings(ATLAS_DECORATION_SPEC.columns, row, rowLabel(ATLAS_DECORATION_SPEC, row, index))),
  summary: (row) => `${value(row, 'count') || '1'} x ${value(row, 'size') || '0.05'} cells`,
};

export const ATLAS_LAYOUT_SPEC: ChartTableSpec = {
  resource: 'atlas-layouts',
  fileName: 'atlas_layouts.csv',
  noun: 'atlas layout',
  keyColumn: 'id',
  description:
    "Layouts (map types) of atlas systems. A system picks one by weight from its region's layout list, or from every layout here when the region lists none. A layout steers vanilla's generator: which system types it uses, how many orbits it gets and how far out they reach, how often rings and belts appear, and the signature feature built into it.",
  columns: columnsOf(ATLAS_LAYOUT_COLUMNS, {
    id: { label: 'Layout id', kind: 'text' },
    weight: { label: 'Weight', kind: 'number' },
    system_types: { label: 'System types', kind: 'text', help: `type:weight; ... from ${ATLAS_SYSTEM_TYPES.join(', ')}. Empty uses the usual chart weights` },
    min_orbits: { label: 'Min orbits', kind: 'integer', help: 'Empty leaves the orbit count to the star' },
    max_orbits: { label: 'Max orbits', kind: 'integer' },
    max_orbit_radius: { label: 'Max orbit radius', kind: 'number', help: 'How far out planets may sit. Empty is vanilla\'s 20000' },
    terrain: { label: 'Terrain', kind: 'text', help: 'category:multiplier; ... such as cat_terrain_rings:2' },
    feature: { label: 'Feature', kind: 'choice', choices: (vocabulary) => vocabulary.chartFeatures, help: 'Signature set-piece from atlas_features.csv, where the boss waits' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', weight: '1' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_LAYOUT_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a layout id`);
      }
      issues.push(...columnFormatIssues(ATLAS_LAYOUT_SPEC.columns, row, label, vocabulary));
      issues.push(...weightListIssues(row, 'system_types', label), ...weightListIssues(row, 'terrain', label));
      for (const type of weightListIds(row, 'system_types')) {
        if (!ATLAS_SYSTEM_TYPES.includes(type.toUpperCase())) {
          issues.push(`${label}: system_types lists ${type}, which is not one of ${ATLAS_SYSTEM_TYPES.join(', ')}`);
        }
      }
      issues.push(...forbiddenIdIssues(row, 'terrain', label, ATLAS_PHENOMENON_CATEGORIES, 'only phenomena add it'));
      if (value(row, 'min_orbits') !== '' && value(row, 'max_orbits') !== '' && numberValue(row, 'max_orbits', 0) < numberValue(row, 'min_orbits', 0)) {
        issues.push(`${label}: max_orbits is below min_orbits`);
      }
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_LAYOUT_SPEC, model)];
  },
  warnings: (model) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(ATLAS_LAYOUT_SPEC, row, index);
      return [
        ...roundedIntegerWarnings(ATLAS_LAYOUT_SPEC.columns, row, label),
        ...(numberValue(row, 'weight', 1) > 0 ? [] : [`${label}: a weight of 0 or less is only used by regions that list it`]),
      ];
    }),
  summary: (row) => {
    const orbits = value(row, 'min_orbits') === '' ? 'vanilla orbits' : `${value(row, 'min_orbits')}-${value(row, 'max_orbits') || value(row, 'min_orbits')} orbits`;
    return value(row, 'feature') === '' ? orbits : `${orbits}, ${value(row, 'feature')}`;
  },
};

export const CHART_PHENOMENON_SPEC: ChartTableSpec = {
  resource: 'chart-phenomena',
  fileName: 'chart_phenomena.csv',
  noun: 'chart phenomenon',
  keyColumn: 'id',
  description:
    'Gameplay-changing terrain an atlas system can roll as a marker, like an expansion: black holes, neutron stars, dense nebulae, asteroid and magnetic storms, stellar flares. Ordinary generation never makes these. A star phenomenon replaces the star or the system type; an overlay adds terrain on top of the layout. Each id needs a phenomenon class in the mod, and its kind must match it. The weight is per system, multiplied by the region leaning in atlas_regions.csv.',
  columns: columnsOf(CHART_PHENOMENON_COLUMNS, {
    id: { label: 'Phenomenon', kind: 'choice', choices: (vocabulary) => vocabulary.chartPhenomena },
    weight: { label: 'Weight', kind: 'number' },
    min_tier: { label: 'Min tier', kind: 'integer' },
    max_tier: { label: 'Max tier', kind: 'integer' },
    requires_mod: { label: 'Requires mod', kind: 'text', help: 'Optional mod id; the phenomenon only appears while that mod is enabled' },
    icon: { label: 'Icon', kind: 'text', help: 'Sprite path for the atlas marker' },
    kind: { label: 'Kind', kind: 'choice', choices: () => CHART_PHENOMENON_KINDS },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', weight: '1', min_tier: '1', max_tier: '15', kind: 'overlay' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(CHART_PHENOMENON_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a phenomenon id`);
      }
      issues.push(...columnFormatIssues(CHART_PHENOMENON_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(CHART_PHENOMENON_SPEC, model)];
  },
  warnings: (model, vocabulary) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(CHART_PHENOMENON_SPEC, row, index);
      return [...roundedIntegerWarnings(CHART_PHENOMENON_SPEC.columns, row, label), ...tierSpanWarnings(row, label, vocabulary)];
    }),
  summary: tierSpanSummary,
};

export const ATLAS_FEATURE_SPEC: ChartTableSpec = {
  resource: 'atlas-features',
  fileName: 'atlas_features.csv',
  noun: 'atlas feature',
  keyColumn: 'id',
  description:
    'Signature set-pieces a layout can build past the last planet, such as a ringed giant or a rogue planet. The boss waits at the feature and the wormhole opens on the far side. Each id needs a feature class in the mod; a feature without a row here is never built.',
  columns: columnsOf(ATLAS_FEATURE_COLUMNS, {
    id: { label: 'Feature', kind: 'choice', choices: (vocabulary) => vocabulary.chartFeatures },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_FEATURE_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a feature id`);
      }
      issues.push(...columnFormatIssues(ATLAS_FEATURE_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_FEATURE_SPEC, model)];
  },
  warnings: (model, vocabulary) => {
    const listed = new Set(model.rows.map((row) => value(row, 'id')));
    return vocabulary.chartFeatures.filter((id) => !listed.has(id)).map((id) => `No ${id} row, so layouts naming it build no feature`);
  },
};

export const ATLAS_NODE_EFFECT_SPEC: ChartTableSpec = {
  resource: 'atlas-node-effects',
  fileName: 'atlas_node_effects.csv',
  noun: 'atlas node effect',
  keyColumn: 'id',
  description:
    'Chart effect lines an atlas system adds to the run on top of the chart: a Powerful Boss marker, infested and cleansed systems, and the special sites. Write effects as EFFECT:value; EFFECT:value with chart effects from the Modifiers tab, such as CHART_BOSS_SIZE_PERCENT:50. Values may be negative.',
  columns: columnsOf(ATLAS_NODE_EFFECT_COLUMNS, {
    id: { label: 'Node', kind: 'choice', choices: () => ATLAS_NODE_EFFECT_IDS },
    effects: { label: 'Effects', kind: 'text', help: 'EFFECT:value; ... using chart effects' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', effects: '' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_NODE_EFFECT_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a node id`);
      }
      issues.push(...columnFormatIssues(ATLAS_NODE_EFFECT_SPEC.columns, row, label, vocabulary));
      for (const entry of value(row, 'effects').split(';').map((part) => part.trim()).filter((part) => part !== '')) {
        const separator = entry.indexOf(':');
        const effect = separator < 0 ? entry : entry.slice(0, separator).trim();
        const amount = separator < 0 ? '' : entry.slice(separator + 1).trim();
        if (!vocabulary.chartEffectNames.includes(effect.toUpperCase())) {
          issues.push(`${label}: ${effect} is not a chart effect`);
        } else if (!isNumberText(amount)) {
          issues.push(`${label}: ${effect} needs a number, as EFFECT:value`);
        }
      }
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_NODE_EFFECT_SPEC, model)];
  },
  warnings: () => [],
  summary: (row) => value(row, 'effects') || 'no effects',
};

export const ATLAS_MARKER_SPEC: ChartTableSpec = {
  resource: 'atlas-markers',
  fileName: 'atlas_markers.csv',
  noun: 'atlas marker',
  keyColumn: 'id',
  description:
    'How often atlas systems roll each marker: chance plus chance_per_step for every step from the Gateway, up to max_chance. Steps are cells from the Gateway. void is the chance that a cell which may be empty (half of them) is empty space. For expansion and phenomenon the chance is per candidate, multiplied by its weight.',
  columns: columnsOf(ATLAS_MARKER_COLUMNS, {
    id: { label: 'Marker', kind: 'choice', choices: () => ATLAS_MARKER_IDS },
    chance: { label: 'Chance', kind: 'fraction' },
    chance_per_step: { label: 'Per step', kind: 'fraction' },
    max_chance: { label: 'Max chance', kind: 'fraction' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', chance: '0.1', chance_per_step: '0', max_chance: '1' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_MARKER_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a marker id`);
      }
      issues.push(...columnFormatIssues(ATLAS_MARKER_SPEC.columns, row, label, vocabulary));
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_MARKER_SPEC, model)];
  },
  warnings: (model) => {
    const listed = new Set(model.rows.map((row) => value(row, 'id')));
    return ATLAS_MARKER_IDS.filter((id) => !listed.has(id)).map((id) => `No ${id} row, so it never rolls`);
  },
  summary: (row) => `${value(row, 'chance') || '0'} +${value(row, 'chance_per_step') || '0'}/step, max ${value(row, 'max_chance') || '1'}`,
};

export const ATLAS_SPECIAL_SPEC: ChartTableSpec = {
  resource: 'atlas-specials',
  fileName: 'atlas_specials.csv',
  noun: 'atlas special site',
  keyColumn: 'id',
  description:
    'Special atlas sites. A chunk site rolls its chance once per 8 by 8 chunk and lands on one of its systems at least min_steps cells out. A fixed site sits count times at steps cells from the Gateway, evenly spread around it, in every save. Infestation sites infest every system within radius cells; for sensor arrays radius is the reveal in lanes.',
  columns: columnsOf(ATLAS_SPECIAL_COLUMNS, {
    id: { label: 'Site id', kind: 'text' },
    kind: { label: 'Kind', kind: 'choice', choices: () => ATLAS_SPECIAL_KINDS },
    placement: { label: 'Placement', kind: 'choice', choices: () => ATLAS_SPECIAL_PLACEMENTS },
    chance: { label: 'Chance per chunk', kind: 'fraction' },
    min_steps: { label: 'Min steps', kind: 'number' },
    radius: { label: 'Radius', kind: 'number', help: `At most ${ATLAS_MOST_RADIUS}` },
    count: { label: 'Count', kind: 'integer', help: 'Fixed sites only' },
    steps: { label: 'Steps', kind: 'number', help: 'Fixed sites only; cells from the Gateway' },
    requires_mod: { label: 'Requires mod', kind: 'text' },
    unlock: { label: 'Unlock', kind: 'text', help: 'Optional player memory flag, such as $encounteredThreat' },
    faction: { label: 'Faction', kind: 'text', help: 'Infestations only: the faction that holds the infested systems' },
    note: NOTE_COLUMN,
  }),
  newRow: () => ({ id: '', placement: 'chunk', chance: '0.1', min_steps: '4' }),
  issues: (model, vocabulary) => {
    const issues: string[] = [];
    model.rows.forEach((row, index) => {
      const label = rowLabel(ATLAS_SPECIAL_SPEC, row, index);
      if (value(row, 'id') === '') {
        issues.push(`${label}: needs a site id`);
      }
      issues.push(...columnFormatIssues(ATLAS_SPECIAL_SPEC.columns, row, label, vocabulary));
      if (numberValue(row, 'radius', 0) > ATLAS_MOST_RADIUS) {
        issues.push(`${label}: radius above ${ATLAS_MOST_RADIUS} counts as ${ATLAS_MOST_RADIUS}`);
      }
    });
    return [...issues, ...duplicateKeyIssues(ATLAS_SPECIAL_SPEC, model)];
  },
  warnings: (model) =>
    model.rows.flatMap((row, index) => {
      const label = rowLabel(ATLAS_SPECIAL_SPEC, row, index);
      const chunk = value(row, 'placement').toLowerCase() !== 'fixed';
      return [
        ...roundedIntegerWarnings(ATLAS_SPECIAL_SPEC.columns, row, label),
        ...(chunk && !(numberValue(row, 'chance', 0) > 0) ? [`${label}: a chunk site with no chance is never placed`] : []),
      ];
    }),
  summary: (row) =>
    value(row, 'placement').toLowerCase() === 'fixed'
      ? `${value(row, 'count') || '1'} fixed at ${value(row, 'steps') || '1'} cells`
      : `${value(row, 'chance') || '0'} per chunk from ${value(row, 'min_steps') || '1'} cells`,
};

export const CHART_TABLE_SPECS: ChartTableSpec[] = [
  CHART_TIER_SPEC,
  CHART_POI_SPEC,
  CHART_FACTION_SPEC,
  CHART_BOSS_SPEC,
  CHART_BOSS_STATION_SPEC,
  CHART_DROP_SPEC,
  CHART_LOOT_SPEC,
  CHART_COLONY_ITEM_SPEC,
  CHART_BLOCKED_ABILITY_SPEC,
  CHART_EXPANSION_SPEC,
  CHART_PHENOMENON_SPEC,
  ATLAS_REGION_SPEC,
  ATLAS_DECORATION_SPEC,
  ATLAS_LAYOUT_SPEC,
  ATLAS_FEATURE_SPEC,
  ATLAS_NODE_EFFECT_SPEC,
  ATLAS_MARKER_SPEC,
  ATLAS_SPECIAL_SPEC,
];
