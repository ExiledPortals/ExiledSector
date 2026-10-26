import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import { definitionWarnings, formatPool, parsePool, parseStarChartDefinitions, serializeStarChartsCsv, validateItemDefinitions } from './definitions.ts';
import { parseStarChartImplicitsCsv, serializeStarChartImplicitsCsv } from './implicits.ts';
import { formatModifierLines, modifierById, newModifier, parseModifierLines, parseModifiersCsv, togglePerTierBand, validateModifiers } from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [{ name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false }],
  tags: { theme: [], region: [], requirement: [] },
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
  'id,slot,item_types,lines,name\n' +
  'HYBRID_CHART_EXTRA_FLEETS,prefix,star_chart,CHART_EXTRA_FLEETS:t1=1/t8=2; CHART_QUANTITY_PERCENT:t1=5-7/t8=8-10,Crowded\n' +
  'CHART_POI_BUDGET_PERCENT,suffix,star_chart,CHART_POI_BUDGET_PERCENT:t1=10-20,Plenty\n' +
  'CHART_POI_BUDGET_PERCENT_SINGLE,implicit,star_chart,CHART_POI_BUDGET_PERCENT:5:10,\n';
const chartCatalogue = modifierById(parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers);

describe('star chart modifiers', () => {
  it('parses tier bands, including negative ranges and fixed values, and formats them back', () => {
    const text = 'CHART_SENSOR_RANGE_PERCENT:t1=-25--15/t6=-35--25; CHART_EXTRA_FLEETS:t1=1/t8=2; CHART_POI_BUDGET_PERCENT:5:10';
    const lines = parseModifierLines(text);
    expect(lines[0].tierBands).toEqual([
      { tier: '1', min: '-25', max: '-15' },
      { tier: '6', min: '-35', max: '-25' },
    ]);
    expect(lines[1].tierBands).toEqual([
      { tier: '1', min: '1', max: '' },
      { tier: '8', min: '2', max: '' },
    ]);
    expect(formatModifierLines(lines)).toBe(text);
  });

  it('switches a range to tier bands and back, keeping every band for the next switch', () => {
    const [line] = parseModifierLines('CHART_POI_BUDGET_PERCENT:5:10');
    togglePerTierBand(line, true);
    expect(line.tierBands).toEqual([{ tier: '1', min: '5', max: '10' }]);
    togglePerTierBand(line, false);
    expect(formatModifierLines([line])).toBe('CHART_POI_BUDGET_PERCENT:5:10');
    const [banded] = parseModifierLines('CHART_REWARD_PERCENT:t1=10-20/t6=20-35');
    togglePerTierBand(banded, false);
    togglePerTierBand(banded, true);
    expect(formatModifierLines([banded])).toBe('CHART_REWARD_PERCENT:t1=10-20/t6=20-35');
    const [hullSized] = parseModifierLines('CHART_EXTRA_POIS:1/2/3/4');
    togglePerTierBand(hullSized, true);
    expect(formatModifierLines([hullSized])).toBe('CHART_EXTRA_POIS:t1=1');
    expect(validateModifiers([{ cells: [], id: 'CHART_EXTRA_POIS', slot: 'suffix', itemTypes: ['star_chart'], lines: [hullSized], name: 'x' }], vocabulary, new Map(), 'chart')).toEqual([
      'CHART_EXTRA_POIS: unknown star chart effect CHART_EXTRA_POIS',
    ]);
  });

  it('keeps a band with a trailing dash as written and flags it, and accepts trailing colons like the game', () => {
    const lines = parseModifierLines('CHART_POI_BUDGET_PERCENT:t1=5-; CHART_EXTRA_FLEETS:5:10:');
    expect(formatModifierLines(lines)).toBe('CHART_POI_BUDGET_PERCENT:t1=5-; CHART_EXTRA_FLEETS:5:10:');
    const modifier = { cells: [], id: 'HYBRID_CHART_POI_BUDGET_EXTRA_FLEETS', slot: 'prefix', itemTypes: ['star_chart'], lines, name: 'x' };
    expect(validateModifiers([modifier], vocabulary, new Map(), 'chart')).toEqual([
      'HYBRID_CHART_POI_BUDGET_EXTRA_FLEETS: CHART_POI_BUDGET_PERCENT has the tier band "t1=5-"; write each band as tN=value or tN=min-max',
    ]);
  });

  it('accepts the shipped kind of chart modifier', () => {
    expect(validateModifiers(parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers, vocabulary, new Map(), 'chart')).toEqual([]);
  });

  it('reports every rule the game would reject, plus the tier and hull size mistakes', () => {
    const modifiers = parseModifiersCsv(
      'id,slot,item_types,lines,name\n' +
        'CHART_EXTRA_FLEETS,prefix,star_chart;hull_item,CHART_EXTRA_FLEETS:t2=1/t1=2,\n' +
        'HULL_MULT,prefix,star_chart,HULL_MULT:1:2,\n' +
        'CHART_QUANTITY_PERCENT,suffix,star_chart,CHART_QUANTITY_PERCENT:1/2/3/4,\n' +
        'CHART_POI_BUDGET_PERCENT,suffix,star_chart,CHART_POI_BUDGET_PERCENT:t1=5-x/t16=9,\n',
    ).modifiers;
    expect(validateModifiers(modifiers, vocabulary, new Map(), 'chart', new Set(['HULL_MULT']))).toEqual([
      'CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS must start its first tier band at t1',
      'CHART_EXTRA_FLEETS: CHART_EXTRA_FLEETS has tier bands that do not rise',
      'CHART_EXTRA_FLEETS: star chart modifiers must list only star_chart as their item type',
      'HULL_MULT: item_modifiers.csv already has this id, and ids must be unique across both modifier files',
      'HULL_MULT: unknown star chart effect HULL_MULT',
      'CHART_QUANTITY_PERCENT: CHART_QUANTITY_PERCENT has hull size values, but star charts have no hull size; use tier bands or a min-max range',
      'CHART_POI_BUDGET_PERCENT: CHART_POI_BUDGET_PERCENT has a t1 value that is not a plain number',
      'CHART_POI_BUDGET_PERCENT: CHART_POI_BUDGET_PERCENT has a band at t16, above the highest tier 15',
    ]);
  });

  it('keeps tier bands out of ship modifiers', () => {
    const modifiers = parseModifiersCsv('id,slot,item_types,lines,name\nHULL_MULT,prefix,hull_item,HULL_MULT:t1=1-2,Reinforced\n').modifiers;
    expect(validateModifiers(modifiers, vocabulary, new Map(), 'ship', new Set(['HULL_MULT']))).toEqual([
      'HULL_MULT: star_chart_modifiers.csv already has this id, and ids must be unique across both modifier files',
      'HULL_MULT: HULL_MULT has tier bands, but only star charts have a tier',
    ]);
  });

  it('defaults new chart modifiers to the star chart type, with tier bands on prefixes and suffixes', () => {
    expect(newModifier([], 'implicit', vocabulary, 'chart')).toMatchObject({ itemTypes: ['star_chart'], lines: [{ effect: 'CHART_EXTRA_FLEETS', min: '1', max: '2' }] });
    expect(newModifier([], 'prefix', vocabulary, 'chart').lines[0].tierBands).toEqual([{ tier: '1', min: '1', max: '2' }]);
    expect(newModifier([], 'implicit', vocabulary).itemTypes).toEqual(['hull_item']);
  });
});

describe('star chart definitions', () => {
  const header = 'id,system_type,name,icon,rarity,unique,min_tier,description,prefixes,suffixes,unlock\n';

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
    const text = `${header.trim()},extra\r\nchart_a,SINGLE,A,,1,,,"No comma here",@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT,,keep\r\n`;
    expect(serializeStarChartsCsv(parseStarChartDefinitions(text))).toBe(text);
  });

  it('accepts the shipped kind of chart', () => {
    const model = parseStarChartDefinitions(
      header +
        'chart_a,SINGLE,A,,30,,,,@HYBRID_CHART_EXTRA_FLEETS:6:t3+,@CHART_POI_BUDGET_PERCENT,\n' +
        'unique_b,binary_close,B,,1,true,4,,@HYBRID_CHART_EXTRA_FLEETS,@CHART_POI_BUDGET_PERCENT,cleared_chart_tier_3\n' +
        'chart_c,,C,,1,,,,@HYBRID_CHART_EXTRA_FLEETS,,defeated_ziggurat\n',
    );
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart')).toEqual([]);
  });

  it('reports what the chart loader refuses', () => {
    const model = parseStarChartDefinitions(
      header +
        'chart_a,TRINARY,A,,1,true,16,,@HYBRID_CHART_EXTRA_FLEETS:6:t0+; @CHART_POI_BUDGET_PERCENT_SINGLE,@CHART_POI_BUDGET_PERCENT:x,cleared_chart_tier_16\n' +
        'taken_id,SINGLE,B,,1,,,,@HYBRID_CHART_EXTRA_FLEETS:t4-2,,\n',
    );
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart', ['taken_id'])).toEqual([
      'chart_a: system type must be any or one of SINGLE, BINARY_CLOSE',
      'chart_a: minimum tier must be blank or a whole number from 1 to 15',
      'chart_a: unknown drop condition cleared_chart_tier_16; use an unlock id or cleared_chart_tier_N with N from 1 to 15',
      'chart_a: HYBRID_CHART_EXTRA_FLEETS has the tier range "t0+", but tiers run from 1 to 15',
      'chart_a: CHART_POI_BUDGET_PERCENT_SINGLE is a implicit, so it cannot be one of its prefixes',
      'chart_a: CHART_POI_BUDGET_PERCENT has the weight "x", which is not a number',
      'chart_a: CHART_POI_BUDGET_PERCENT is listed twice, as CHART_POI_BUDGET_PERCENT_SINGLE and CHART_POI_BUDGET_PERCENT',
      'taken_id: id is already used by a hull item or subroutine',
      'taken_id: HYBRID_CHART_EXTRA_FLEETS has the tier range "t4-2", whose highest tier must be from t4 to t15',
    ]);
  });

  it('flags tier ranges on ship items, which have no tier', () => {
    const shipCatalogue = modifierById(parseModifiersCsv('id,slot,item_types,lines,name\nHULL_MULT,prefix,hull_item,HULL_MULT:1:2,Reinforced\n').modifiers);
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
  it('warns that unique charts ignore pool tier ranges and that non-uniques ignore drop conditions, and flags an empty weight', () => {
    const model = parseStarChartDefinitions(
      'id,system_type,name,icon,rarity,unique,min_tier,description,prefixes,suffixes,unlock\n' +
        'unique_a,SINGLE,A,,1,true,,,@HYBRID_CHART_EXTRA_FLEETS:t9+,@CHART_POI_BUDGET_PERCENT,\n' +
        'chart_b,SINGLE,B,,1,,,,@HYBRID_CHART_EXTRA_FLEETS:,@CHART_POI_BUDGET_PERCENT,defeated_ziggurat\n',
    );
    expect(definitionWarnings(model.definitions, 'chart')).toEqual([
      'unique_a: a unique chart rolls every entry at every tier, so the tier ranges on HYBRID_CHART_EXTRA_FLEETS are ignored',
      'chart_b: the drop condition defeated_ziggurat only applies to uniques, so it is ignored',
    ]);
    expect(validateItemDefinitions(model.definitions, vocabulary, chartCatalogue, 'chart')).toEqual([
      'chart_b: HYBRID_CHART_EXTRA_FLEETS has an empty weight after its colon; write a weight or remove the colon',
    ]);
    expect(formatPool(model.definitions[1].prefixes)).toBe('@HYBRID_CHART_EXTRA_FLEETS:');
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
