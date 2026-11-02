import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import {
  definitionWarnings,
  formatPool,
  newItemDefinition,
  parsePool,
  parseStarChartDefinitions,
  serializeStarChartsCsv,
  STAR_CHART_KIND,
  validateItemDefinitions,
} from './definitions.ts';
import { parseStarChartImplicitsCsv, serializeStarChartImplicitsCsv } from './implicits.ts';
import {
  formatModifierLines,
  lineSummary,
  modifierById,
  newModifier,
  parseModifierLines,
  parseModifiersCsv,
  setTierGroupEnabled,
  togglePerTierGroup,
  validateModifiers,
  type ItemModifier,
  type ModifierLine,
} from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [{ name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false }],
  tags: { theme: [], volume: [], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['subroutine', 'hull_item', 'star_chart'],
  itemKinds: [
    { id: 'subroutine', type: 'subroutine' },
    { id: 'bridge', type: 'hull_item' },
  ],
  itemUnlocks: ['defeated_ziggurat'],
  clearedChartTierPrefix: 'cleared_chart_tier_',
  chartTierRange: [1, 15],
  chartEffects: ['CHART_EXTRA_FLEETS', 'CHART_QUANTITY_PERCENT', 'CHART_POI_BUDGET_PERCENT'],
  chartSystemTypes: ['SINGLE', 'BINARY_CLOSE'],
});

const CHART_MODIFIERS_CSV =
  'id,affix,scopes,lines,name\n' +
  'HYBRID_CHART_EXTRA_FLEETS,prefix,star_chart,CHART_EXTRA_FLEETS:low=1/mid=2/high=3; CHART_QUANTITY_PERCENT:low=5-7/mid=8-10/high=11-13,Crowded\n' +
  'CHART_POI_BUDGET_PERCENT,suffix,star_chart,CHART_POI_BUDGET_PERCENT:mid=40-60/high=90-120,Plenty\n' +
  'CHART_POI_BUDGET_PERCENT_SINGLE,implicit,star_chart,CHART_POI_BUDGET_PERCENT:5:10,\n';
const chartCatalogue = modifierById(parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers);
const GROUP_IDS = ['low', 'mid', 'high'];

function chartModifier(lines: ModifierLine[], id: string): ItemModifier {
  return { cells: [], id, slot: 'prefix', itemTypes: ['star_chart'], lines, name: 'x' };
}

describe('star chart modifiers', () => {
  it('falls back to the low, mid and high tier groups when the vocabulary has none', () => {
    expect(vocabulary.chartTierGroups).toEqual([
      { id: 'low', minTier: 1, maxTier: 5 },
      { id: 'mid', minTier: 6, maxTier: 10 },
      { id: 'high', minTier: 11, maxTier: 16 },
    ]);
  });

  it('parses tier groups, including negative ranges, fixed values and a line without low, and formats them back', () => {
    const text =
      'CHART_SENSOR_RANGE_PERCENT:low=-25--15/mid=-35--25/high=-50--40; CHART_EXTRA_FLEETS:low=1/mid=2/high=3; CHART_COLONY_ITEM_PERCENT:mid=40-60/high=90-120; CHART_POI_BUDGET_PERCENT:5:10';
    const lines = parseModifierLines(text);
    expect(lines[0].tierGroups).toEqual([
      { group: 'low', min: '-25', max: '-15' },
      { group: 'mid', min: '-35', max: '-25' },
      { group: 'high', min: '-50', max: '-40' },
    ]);
    expect(lines[1].tierGroups).toEqual([
      { group: 'low', min: '1', max: '' },
      { group: 'mid', min: '2', max: '' },
      { group: 'high', min: '3', max: '' },
    ]);
    expect(lines[2].tierGroups).toEqual([
      { group: 'mid', min: '40', max: '60' },
      { group: 'high', min: '90', max: '120' },
    ]);
    expect(lines[3].tierGroups).toBeUndefined();
    expect(formatModifierLines(lines)).toBe(text);
    expect(lineSummary(lines[2])).toBe('CHART_COLONY_ITEM_PERCENT mid 40 to 60, high 90 to 120');
  });

  it('switches a range to all three tier groups and back, keeping every group for the next switch', () => {
    const [line] = parseModifierLines('CHART_POI_BUDGET_PERCENT:5:10');
    togglePerTierGroup(line, true, GROUP_IDS);
    expect(formatModifierLines([line])).toBe('CHART_POI_BUDGET_PERCENT:low=5-10/mid=5-10/high=5-10');
    togglePerTierGroup(line, false, GROUP_IDS);
    expect(formatModifierLines([line])).toBe('CHART_POI_BUDGET_PERCENT:5:10');
    const [fixed] = parseModifierLines('CHART_POI_BUDGET_PERCENT:4:4');
    togglePerTierGroup(fixed, true, GROUP_IDS);
    expect(formatModifierLines([fixed])).toBe('CHART_POI_BUDGET_PERCENT:low=4/mid=4/high=4');
    const [grouped] = parseModifierLines('CHART_REWARD_PERCENT:mid=10-20/high=20-35');
    togglePerTierGroup(grouped, false, GROUP_IDS);
    expect(formatModifierLines([grouped])).toBe('CHART_REWARD_PERCENT:10:20');
    togglePerTierGroup(grouped, true, GROUP_IDS);
    expect(formatModifierLines([grouped])).toBe('CHART_REWARD_PERCENT:mid=10-20/high=20-35');
    const [hullSized] = parseModifierLines('CHART_EXTRA_POIS:1/2/3/4');
    togglePerTierGroup(hullSized, true, GROUP_IDS);
    expect(formatModifierLines([hullSized])).toBe('CHART_EXTRA_POIS:low=1/mid=1/high=1');
    expect(validateModifiers([chartModifier([hullSized], 'CHART_EXTRA_POIS')], vocabulary, new Map(), 'chart')).toEqual(['CHART_EXTRA_POIS: unknown star chart effect CHART_EXTRA_POIS']);
  });

  it('keeps the groups a contiguous run up to high when switching one on or off, and remembers switched-off values', () => {
    const [line] = parseModifierLines('CHART_REWARD_PERCENT:low=1-2/mid=3-4/high=5-6');
    setTierGroupEnabled(line, GROUP_IDS, 'mid', false);
    expect(formatModifierLines([line])).toBe('CHART_REWARD_PERCENT:high=5-6');
    setTierGroupEnabled(line, GROUP_IDS, 'high', false);
    expect(formatModifierLines([line])).toBe('CHART_REWARD_PERCENT:high=5-6');
    setTierGroupEnabled(line, GROUP_IDS, 'mid', true);
    expect(formatModifierLines([line])).toBe('CHART_REWARD_PERCENT:mid=3-4/high=5-6');
    setTierGroupEnabled(line, GROUP_IDS, 'low', true);
    expect(formatModifierLines([line])).toBe('CHART_REWARD_PERCENT:low=1-2/mid=3-4/high=5-6');
    expect(line.switchedOffTierGroups).toBeUndefined();

    const [fresh] = parseModifierLines('CHART_REWARD_PERCENT:high=5-6');
    setTierGroupEnabled(fresh, GROUP_IDS, 'low', true);
    expect(formatModifierLines([fresh])).toBe('CHART_REWARD_PERCENT:low=5-6/mid=5-6/high=5-6');

    const [gapped] = parseModifierLines('CHART_REWARD_PERCENT:low=1/high=3');
    setTierGroupEnabled(gapped, GROUP_IDS, 'mid', true);
    expect(formatModifierLines([gapped])).toBe('CHART_REWARD_PERCENT:low=1/mid=1/high=3');
  });

  it('keeps an unreadable group as written, flags it, and moves it to the end when a group is switched', () => {
    const lines = parseModifierLines('CHART_POI_BUDGET_PERCENT:low=5-/mid=1/high=2; CHART_EXTRA_FLEETS:5:10:');
    expect(formatModifierLines(lines)).toBe('CHART_POI_BUDGET_PERCENT:low=5-/mid=1/high=2; CHART_EXTRA_FLEETS:5:10:');
    expect(validateModifiers([chartModifier(lines, 'HYBRID_CHART_POI_BUDGET_EXTRA_FLEETS')], vocabulary, new Map(), 'chart')).toEqual([
      'HYBRID_CHART_POI_BUDGET_EXTRA_FLEETS: CHART_POI_BUDGET_PERCENT has the tier group "low=5-"; write each group as low, mid, high followed by =value or =min-max',
    ]);
    setTierGroupEnabled(lines[0], GROUP_IDS, 'low', true);
    expect(formatModifierLines([lines[0]])).toBe('CHART_POI_BUDGET_PERCENT:low=1/mid=1/high=2/low=5-');
  });

  it('accepts the shipped kind of chart modifier', () => {
    expect(validateModifiers(parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers, vocabulary, new Map(), 'chart')).toEqual([]);
  });

  it('flags tier groups out of order, repeated, with gaps, short of high, unknown or written as tiers', () => {
    const issuesOf = (text: string) => validateModifiers([chartModifier(parseModifierLines(text), 'CHART_EXTRA_FLEETS')], vocabulary, new Map(), 'chart');
    const order = 'list them in order without gaps up to high, as [low, mid, high], [mid, high] or [high]';
    const unreadable = (groupText: string) =>
      `CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier group "${groupText}"; write each group as low, mid, high followed by =value or =min-max`;
    expect(issuesOf('CHART_EXTRA_FLEETS:mid=1/low=2/high=3')).toEqual([`CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier groups [mid, low, high]; ${order}`]);
    expect(issuesOf('CHART_EXTRA_FLEETS:low=1/low=2/mid=2/high=3')).toEqual([`CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier groups [low, low, mid, high]; ${order}`]);
    expect(issuesOf('CHART_EXTRA_FLEETS:low=1/high=3')).toEqual([`CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier groups [low, high]; ${order}`]);
    expect(issuesOf('CHART_EXTRA_FLEETS:low=1/mid=2')).toEqual([`CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier groups [low, mid]; ${order}`]);
    expect(issuesOf('CHART_EXTRA_FLEETS:top=1/high=3')).toEqual([unreadable('top=1')]);
    expect(issuesOf('CHART_EXTRA_FLEETS:t1=1/t6=2')).toEqual([unreadable('t1=1'), unreadable('t6=2')]);
    expect(issuesOf('CHART_EXTRA_FLEETS:low=5-x/mid=3-1/high=2')).toEqual([
      'CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has a low value that is not a plain number',
      'CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has a mid min above its max',
    ]);
    expect(issuesOf('CHART_EXTRA_FLEETS:high=-3--1')).toEqual([]);
  });

  it('flags lines of one modifier that list different tier groups', () => {
    const lines = parseModifierLines('CHART_EXTRA_FLEETS:low=1/mid=2/high=3; CHART_QUANTITY_PERCENT:mid=5-7/high=8-10; CHART_POI_BUDGET_PERCENT:1:2');
    expect(validateModifiers([chartModifier(lines, 'HYBRID_CHART_EXTRA_FLEETS_QUANTITY_POI_BUDGET')], vocabulary, new Map(), 'chart')).toEqual([
      'HYBRID_CHART_EXTRA_FLEETS_QUANTITY_POI_BUDGET: it gives CHART_QUANTITY_PERCENT the tier groups [mid, high], but its other lines use [low, mid, high]; every line of a modifier needs the same groups',
    ]);
  });

  it('reports every rule the game would reject, plus the hull size mistakes', () => {
    const modifiers = parseModifiersCsv(
      'id,affix,scopes,lines,name\n' +
        'CHART_EXTRA_FLEETS,prefix,star_chart;hull_item,CHART_EXTRA_FLEETS:high=1/mid=2,\n' +
        'HULL_MULT,prefix,star_chart,HULL_MULT:1:2,\n' +
        'CHART_QUANTITY_PERCENT,suffix,star_chart,CHART_QUANTITY_PERCENT:1/2/3/4,\n',
    ).modifiers;
    expect(validateModifiers(modifiers, vocabulary, new Map(), 'chart', new Set(['HULL_MULT']))).toEqual([
      'CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has the tier groups [high, mid]; list them in order without gaps up to high, as [low, mid, high], [mid, high] or [high]',
      'CHART_EXTRA_FLEETS: star chart modifiers must list only star_chart as their item type',
      'HULL_MULT: item_modifiers.csv already has this id, and ids must be unique across both modifier files',
      'HULL_MULT: unknown star chart effect HULL_MULT',
      'CHART_QUANTITY_PERCENT: CHART_QUANTITY_PERCENT has hull size values, but star charts have no hull size; use tier groups or a min-max range',
    ]);
  });

  it('keeps tier groups out of ship modifiers', () => {
    const modifiers = parseModifiersCsv('id,affix,scopes,lines,name\nHULL_MULT,prefix,hull_item,HULL_MULT:low=1-2/mid=2-3/high=3-4,Reinforced\n').modifiers;
    expect(validateModifiers(modifiers, vocabulary, new Map(), 'ship', new Set(['HULL_MULT']))).toEqual([
      'HULL_MULT: star_chart_modifiers.csv already has this id, and ids must be unique across both modifier files',
      'HULL_MULT: HULL_MULT has tier groups, but only star charts have a tier',
    ]);
  });

  it('defaults new chart modifiers to the star chart type, with all three tier groups on prefixes and suffixes', () => {
    expect(newModifier([], 'implicit', vocabulary, 'chart')).toMatchObject({ itemTypes: ['star_chart'], lines: [{ effect: 'CHART_EXTRA_FLEETS', min: '1', max: '2' }] });
    expect(formatModifierLines(newModifier([], 'prefix', vocabulary, 'chart').lines)).toBe('CHART_EXTRA_FLEETS:low=1-2/mid=1-2/high=1-2');
    expect(newModifier([], 'implicit', vocabulary).itemTypes).toEqual(['hull_item']);
  });
});

describe('star chart definitions', () => {
  const header = 'id,system_type,name,icon,rarity,min_tier,description,prefixes,suffixes\n';

  it('keeps pool tier ranges with and without a weight', () => {
    const pool = parsePool('@HYBRID_CHART_EXTRA_FLEETS:6:t3+; @CHART_POI_BUDGET_PERCENT:t2-6; @CHART_POI_BUDGET_PERCENT:4');
    expect(pool).toEqual([
      { modifierId: 'HYBRID_CHART_EXTRA_FLEETS', weight: '6', tiers: 't3+' },
      { modifierId: 'CHART_POI_BUDGET_PERCENT', weight: '', tiers: 't2-6' },
      { modifierId: 'CHART_POI_BUDGET_PERCENT', weight: '4' },
    ]);
    expect(formatPool(pool)).toBe('@HYBRID_CHART_EXTRA_FLEETS:6:t3+; @CHART_POI_BUDGET_PERCENT:t2-6; @CHART_POI_BUDGET_PERCENT:4');
  });

  it('round-trips quoted descriptions and unknown columns', () => {
    const text = `${header.trim()},extra\r\nchart_a,SINGLE,A,,1,,"No comma here",@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT,keep\r\n`;
    expect(serializeStarChartsCsv(parseStarChartDefinitions(text))).toBe(text);
  });

  it('loads a chart file that still has the old unique and unlock columns and saves it without them', () => {
    const model = parseStarChartDefinitions(
      'id,system_type,name,icon,rarity,unique,min_tier,description,prefixes,suffixes,unlock\r\n' +
        'chart_b,binary_close,B,,1,true,4,,@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT,cleared_chart_tier_3\r\n',
    );
    expect(model.definitions[0]).toMatchObject({ id: 'chart_b', systemType: 'binary_close', minTier: '4' });
    expect(model.definitions[0].unique).toBeUndefined();
    expect(model.definitions[0].unlock).toBeUndefined();
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart')).toEqual([]);
    expect(definitionWarnings(model.definitions, 'chart')).toEqual([]);
    expect(serializeStarChartsCsv(model)).toBe(`${header.trim()}\r\nchart_b,binary_close,B,,1,4,,@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT\r\n`);
  });

  it('gives a new chart no unique or unlock field', () => {
    const chart = newItemDefinition([], STAR_CHART_KIND, parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers);
    expect(chart).toMatchObject({ id: 'new_star_chart', systemType: 'SINGLE', prefixes: [{ modifierId: 'HYBRID_CHART_EXTRA_FLEETS' }] });
    expect(chart).not.toHaveProperty('unique');
    expect(chart).not.toHaveProperty('unlock');
  });

  it('accepts the shipped kind of chart', () => {
    const model = parseStarChartDefinitions(
      header +
        'chart_a,SINGLE,A,,30,,,@HYBRID_CHART_EXTRA_FLEETS:6:t3+,@CHART_POI_BUDGET_PERCENT\n' +
        'chart_b,binary_close,B,,1,4,,@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT\n' +
        'chart_c,,C,,1,,,@HYBRID_CHART_EXTRA_FLEETS,\n',
    );
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart')).toEqual([]);
  });

  it('reports what the chart loader refuses', () => {
    const model = parseStarChartDefinitions(
      header +
        'chart_a,TRINARY,A,,1,16,,@HYBRID_CHART_EXTRA_FLEETS:6:t0+; @CHART_POI_BUDGET_PERCENT_SINGLE,@CHART_POI_BUDGET_PERCENT:x\n' +
        'taken_id,SINGLE,B,,1,,,@HYBRID_CHART_EXTRA_FLEETS:t4-2,\n',
    );
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart', ['taken_id'])).toEqual([
      'chart_a: system type must be any or one of SINGLE, BINARY_CLOSE',
      'chart_a: minimum tier must be blank or a whole number from 1 to 15',
      'chart_a: HYBRID_CHART_EXTRA_FLEETS has the tier range "t0+", but tiers run from 1 to 15',
      'chart_a: CHART_POI_BUDGET_PERCENT_SINGLE is a implicit, so it cannot be one of its prefixes',
      'chart_a: CHART_POI_BUDGET_PERCENT has the weight "x", which is not a number',
      'chart_a: CHART_POI_BUDGET_PERCENT is listed twice, as CHART_POI_BUDGET_PERCENT_SINGLE and CHART_POI_BUDGET_PERCENT',
      'taken_id: id is already used by a hull item or subroutine',
      'taken_id: HYBRID_CHART_EXTRA_FLEETS has the tier range "t4-2", whose highest tier must be from t4 to t15',
    ]);
  });

  it('flags tier ranges on ship items, which have no tier', () => {
    const shipCatalogue = modifierById(parseModifiersCsv('id,affix,scopes,lines,name\nHULL_MULT,prefix,hull_item,HULL_MULT:1:2,Reinforced\n').modifiers);
    const definitions = [
      {
        cells: [],
        id: 'chip',
        kind: 'bridge',
        name: 'Chip',
        icon: '',
        rarity: '1',
        unique: '',
        description: '',
        unlock: '',
        prefixes: parsePool('@HULL_MULT:t2+'),
        suffixes: [],
      },
    ];
    expect(validateItemDefinitions(definitions, vocabulary, shipCatalogue, 'ship', ['chip'])).toEqual([
      'chip: id is already used by a star chart',
      'chip: HULL_MULT has a tier range, but only star charts have a tier',
    ]);
  });
});

describe('star chart warnings', () => {
  it('never warns about drop conditions on charts, and flags an empty weight', () => {
    const model = parseStarChartDefinitions(
      'id,system_type,name,icon,rarity,min_tier,description,prefixes,suffixes\n' + 'chart_b,SINGLE,B,,1,,,@HYBRID_CHART_EXTRA_FLEETS:,@CHART_POI_BUDGET_PERCENT\n',
    );
    expect(definitionWarnings([{ ...model.definitions[0], unlock: 'defeated_ziggurat' }], 'chart')).toEqual([]);
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart')).toEqual([
      'chart_b: HYBRID_CHART_EXTRA_FLEETS has an empty weight after its colon; write a weight or remove the colon',
    ]);
    expect(formatPool(model.definitions[0].prefixes)).toBe('@HYBRID_CHART_EXTRA_FLEETS:');
  });
});

describe('star chart implicits', () => {
  it('keeps the system_type column, adds empty rows for missing types without saving them', () => {
    const text = 'system_type,implicits\r\nSINGLE,@CHART_POI_BUDGET_PERCENT_SINGLE\r\n';
    const model = parseStarChartImplicitsCsv(text, vocabulary);
    expect(model.rows.map((row) => row.section)).toEqual(['SINGLE', 'BINARY_CLOSE', 'any']);
    expect(serializeStarChartImplicitsCsv(model)).toBe(text);
  });
});

describe('csv', () => {
  it('keeps quotes the file had even where they are not needed', () => {
    const text = 'faction,weight,note\r\npirates,10,"Plain note"\r\nderelict,3,"Has, comma"\r\nremnant,4,Plain note\r\n';
    expect(serializeCsvTable(parseCsvTable(text))).toBe('faction,weight,note\r\npirates,10,"Plain note"\r\nderelict,3,"Has, comma"\r\nremnant,4,"Plain note"\r\n');
  });
});
