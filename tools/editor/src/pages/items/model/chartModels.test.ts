import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseCsvTable, serializeCsvTable } from '../../../shared/csv.ts';
import {
  ATLAS_FEATURE_SPEC,
  ATLAS_NODE_EFFECT_SPEC,
  ATLAS_LAYOUT_SPEC,
  ATLAS_MARKER_SPEC,
  ATLAS_REGION_SPEC,
  ATLAS_SPECIAL_SPEC,
  ATLAS_DECORATION_SPEC,
  CHART_BLOCKED_ABILITY_SPEC,
  CHART_COLONY_ITEM_SPEC,
  CHART_DROP_SPEC,
  CHART_EXPANSION_SPEC,
  CHART_FACTION_SPEC,
  CHART_LOOT_SPEC,
  CHART_POI_SPEC,
  CHART_TIER_SPEC,
  dropTierSummary,
  parseChartTable,
  serializeChartTable,
} from './chartTables.ts';
import { CRAFTING_ITEMS } from './crafting.ts';
import { definitionWarnings, formatPool, parsePool, parseStarChartDefinitions, serializeStarChartsCsv, validateItemDefinitions } from './definitions.ts';
import { parseStarChartImplicitsCsv, serializeStarChartImplicitsCsv, validateImplicits } from './implicits.ts';
import { formatModifierLines, modifierById, newModifier, parseModifierLines, parseModifiersCsv, togglePerTierBand, validateModifiers } from './modifiers.ts';
import { serializeStarChartNames, validateStarChartNames } from './names.ts';

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
  chartPoiKinds: ['cache', 'domain', 'station', 'derelict'],
  chartDerelictTypes: ['CIVILIAN', 'LARGE', 'MEDIUM', 'SMALL'],
  chartDropSources: [
    { id: 'run_reward', insideSystem: true },
    { id: 'battle', insideSystem: false },
  ],
  chartLootDropTypes: ['weapon', 'resource', 'blueprint', 'ai_core'],
  chartLootScopes: ['cache', 'domain', 'station', 'derelict', 'fleet', 'boss'],
  chartColonyItemGroups: ['rare_tech'],
  chartExpansions: ['spatial_breach'],
  chartFeatures: ['ringed_giant', 'rogue_planet'],
  chartPhenomena: ['black_hole', 'stellar_flares'],
});

const CHART_MODIFIERS_CSV =
  'id,slot,item_types,lines,name\n' +
  'HYBRID_CHART_EXTRA_FLEETS,prefix,star_chart,CHART_EXTRA_FLEETS:t1=1/t8=2; CHART_QUANTITY_PERCENT:t1=5-7/t8=8-10,Crowded\n' +
  'CHART_POI_BUDGET_PERCENT,suffix,star_chart,CHART_POI_BUDGET_PERCENT:t1=10-20,Plenty\n' +
  'CHART_POI_BUDGET_PERCENT_SINGLE,implicit,star_chart,CHART_POI_BUDGET_PERCENT:5:10,\n';
const chartCatalogue = modifierById(parseModifiersCsv(CHART_MODIFIERS_CSV).modifiers);

function chartTable(spec: typeof CHART_TIER_SPEC, csv: string) {
  return parseChartTable(csv, spec.columns);
}

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

describe('review findings', () => {
  const withDefaults = { ...vocabulary, chartTierDefaults: [{ tier: 10, fleetsMin: 5, fleetsMax: 7, levelMin: 40, levelMax: 43, bossSize: 0 }] };

  it('compares blank tier cells against the built-in values', () => {
    const model = chartTable(CHART_TIER_SPEC, 'tier,fleets_min,fleets_max,boss,boss_size,level_min,level_max\n10,,3,yes,,,35\n');
    expect(CHART_TIER_SPEC.warnings(model, withDefaults).slice(0, 3)).toEqual([
      'tier 10: fleets_max is below fleets_min (built-in 5), so it is raised to fleets_min',
      'tier 10: level_min (40) is above level_max (35), counting built-in values for blanks, so the game swaps them',
      'tier 10: boss is yes but boss_size is blank and the built-in value is 0, so there is no boss',
    ]);
  });

  it('warns about the values the faction, point of interest and drop loaders change', () => {
    expect(CHART_FACTION_SPEC.warnings(chartTable(CHART_FACTION_SPEC, 'faction,weight\npirates,0\n'), vocabulary)).toEqual([
      'pirates: a weight of 0 or less means it is never picked',
    ]);
    expect(CHART_POI_SPEC.warnings(chartTable(CHART_POI_SPEC, 'id,kind,entity,cost\nfree,cache,supply_cache,0\n'), vocabulary)).toEqual([
      'free: a cost or weight of 0 or less means it is never placed',
    ]);
    const drops = chartTable(CHART_DROP_SPEC, 'source,chance\nrun_reward,6\nbattle,0\n');
    expect(CHART_DROP_SPEC.warnings(drops, vocabulary)).toEqual([]);
  });

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
  it('keeps the system_type column, adds empty rows for missing types without saving them, and normalises like the game', () => {
    const text = 'system_type,implicits\r\nSINGLE,@CHART_POI_BUDGET_PERCENT_SINGLE\r\n';
    const model = parseStarChartImplicitsCsv(text, vocabulary);
    expect(model.rows.map((row) => row.section)).toEqual(['SINGLE', 'BINARY_CLOSE', 'any']);
    expect(serializeStarChartImplicitsCsv(model)).toBe(text);
    expect(validateImplicits(model, vocabulary, chartCatalogue, 'chart')).toEqual([]);
  });

  it('reports bad system types, repeats, weights and non-chart modifiers', () => {
    const model = parseStarChartImplicitsCsv(
      'system_type,implicits\n' +
        'single,@CHART_POI_BUDGET_PERCENT_SINGLE; @CHART_POI_BUDGET_PERCENT_SINGLE\n' +
        'SINGLE,@CHART_POI_BUDGET_PERCENT_SINGLE:5\n' +
        'NOWHERE,@CHART_POI_BUDGET_PERCENT\n',
      vocabulary,
    );
    expect(validateImplicits(model, vocabulary, chartCatalogue, 'chart')).toEqual([
      'single: CHART_POI_BUDGET_PERCENT_SINGLE is listed more than once',
      'SINGLE: listed more than once',
      'SINGLE: @CHART_POI_BUDGET_PERCENT_SINGLE:5 has a weight, but implicits always roll',
      'NOWHERE: system type must be any or one of SINGLE, BINARY_CLOSE',
      'NOWHERE: CHART_POI_BUDGET_PERCENT is a suffix, not an implicit',
    ]);
  });
});

describe('star chart names', () => {
  it('writes one word set at the top level', () => {
    const text = '{\r\n\t"style": "codename",\r\n\t"first": ["Lantern", "Umbra"],\r\n\t"second": ["Reach"]\r\n}\r\n';
    expect(serializeStarChartNames(JSON.parse(text), '\r\n')).toBe(text);
    expect(validateStarChartNames({ words: JSON.parse(text), lineEnding: '\r\n' })).toEqual([]);
    expect(validateStarChartNames({ words: { style: 'poem', first: [] }, lineEnding: '\n' })).toEqual([
      'Star charts: need at least one first word and one second word',
      'Star charts: style must be one of codename, product',
    ]);
  });
});

describe('chart tables', () => {
  it('keeps quotes the file had even where they are not needed', () => {
    const text = 'faction,weight,note\r\npirates,10,"Plain note"\r\nderelict,3,"Has, comma"\r\nremnant,4,Plain note\r\n';
    expect(serializeCsvTable(parseCsvTable(text))).toBe('faction,weight,note\r\npirates,10,"Plain note"\r\nderelict,3,"Has, comma"\r\nremnant,4,"Plain note"\r\n');
    const model = chartTable(CHART_FACTION_SPEC, 'faction,weight,note\r\npirates,10,"Plain note"\r\n');
    model.rows[0].values.note = 'Edited note';
    expect(serializeChartTable(model, CHART_FACTION_SPEC.columns)).toBe('faction,weight,note\r\npirates,10,Edited note\r\n');
  });

  it('mirrors the drop loader: unknown sources refuse, ciphers outside chart systems warn', () => {
    const model = chartTable(
      CHART_DROP_SPEC,
      'source,chance,extra_chance,tier_up_chance,currency_chance,min_strength,strength_per_tier,max_tier,note\n' +
        'battle,0.01,0,1.5,0.2,150,120,6,\n' +
        'nowhere,x,0,0,0,,,15,\n' +
        'BATTLE,0.02,0,0,0,,,20,\n',
    );
    expect(CHART_DROP_SPEC.issues(model, vocabulary)).toEqual([
      'nowhere: source must be one of run_reward, battle',
      'nowhere: chance "x" is not a number',
      'BATTLE: listed more than once, so only its last row is used',
    ]);
    expect(CHART_DROP_SPEC.warnings(model, vocabulary)).toEqual([
      'battle: tier_up_chance above 100% counts as 100%',
      'battle: battle is outside a chart system, where chart currencies never drop, so the game reads currency_chance as 0',
      'BATTLE: max_tier is clamped to 1-15',
      'No row for run_reward, so it never drops charts',
    ]);
    expect(dropTierSummary(model.rows[0], vocabulary)).toBe('1% from 150, +1 tier per 120 more, up to t6');
  });

  it('mirrors the colony item loader: the ceiling row is a fraction, items need a weight', () => {
    const text = 'id,value,note\r\nceiling,1.5,\r\nsynchrotron,10,\r\nmantle_bore,0,\r\n';
    const model = chartTable(CHART_COLONY_ITEM_SPEC, text);
    expect(serializeChartTable(model, CHART_COLONY_ITEM_SPEC.columns)).toBe(text);
    expect(CHART_COLONY_ITEM_SPEC.issues(model, vocabulary)).toEqual([
      'ceiling: the ceiling must be above 0 and below 1, so an item is never certain',
      'mantle_bore: an item needs a weight above 0',
    ]);
    expect(CHART_COLONY_ITEM_SPEC.warnings(chartTable(CHART_COLONY_ITEM_SPEC, 'id,value\nsynchrotron,10\n'), vocabulary)).toEqual([
      'No ceiling row, so the ceiling is 25%',
    ]);
  });

  it('round-trips the chart expansion table and flags unknown expansions and swapped tiers', () => {
    const text =
      'id,weight,min_tier,max_tier,requires_mod,icon,intel,note\r\n' +
      'spatial_breach,1,1,15,,graphics/icons/icon_portal.png,,"A growing tear, with caches"\r\n' +
      'unbuilt,1,9,4,,,,\r\n' +
      ',1,,,,,,\r\n';
    const model = chartTable(CHART_EXPANSION_SPEC, text);
    expect(serializeChartTable(model, CHART_EXPANSION_SPEC.columns)).toBe(text);
    const issues = CHART_EXPANSION_SPEC.issues(model, vocabulary);
    expect(issues).toContain('row 3: needs an expansion id');
    expect(issues.some((issue) => issue.startsWith('unbuilt:'))).toBe(true);
    expect(CHART_EXPANSION_SPEC.warnings(model, vocabulary)).toContain('unbuilt: min_tier is above max_tier, so the game swaps them');
    expect(CHART_EXPANSION_SPEC.summary?.(model.rows[0], vocabulary)).toBe('tiers 1-15');
  });

  it('mirrors the atlas region loader: weight lists and an always-open normal row', () => {
    const text =
      'id,weight,min_steps,requires_mod,unlock,layouts,factions,markers,note\r\n' +
      'normal,15,0,,$flag,,,,\r\n' +
      'graveyard,10,0,,,compact:2; sprawling,pirates:3,black_hole:0,\r\n' +
      'rift,3,15,assortment_of_things,,compact:x,,,\r\n' +
      'expanse,3,20,,$encounteredThreat,,,,\r\n';
    const model = chartTable(ATLAS_REGION_SPEC, text);
    expect(serializeChartTable(model, ATLAS_REGION_SPEC.columns)).toBe(text);
    expect(ATLAS_REGION_SPEC.issues(model, vocabulary)).toEqual([
      'normal: normal space is always open, so it takes no mod, unlock or min_steps',
      'rift: layouts entry "compact:x" must be an id or id:weight with a weight of 0 or more',
    ]);
    expect(ATLAS_REGION_SPEC.summary?.(model.rows[0], vocabulary)).toBe('open space, weight 15');
    expect(ATLAS_REGION_SPEC.issues(chartTable(ATLAS_REGION_SPEC, 'id,weight\nfurnace,1\n'), vocabulary)).toEqual(['Needs a normal row for normal space']);
  });

  it('checks biome placement and scenery columns', () => {
    const model = chartTable(
      ATLAS_REGION_SPEC,
      'id,weight,size,neighbours,cloud_colour,decorations\n' + 'normal,6,1,,,\n' + 'frozen,8,3,furnace:0,blue,ice:2; probe:x\n',
    );
    expect(ATLAS_REGION_SPEC.issues(model, vocabulary)).toEqual([
      'frozen: decorations entry "probe:x" must be an id or id:weight with a weight of 0 or more',
      'frozen: cloud_colour "blue" must be a colour like #A0C8FF',
      'frozen: size 3 must be from 0.5 to 2',
    ]);
  });

  it('mirrors the atlas decoration loader', () => {
    const text =
      'id,sprite,columns,rows,size,count,colour,scatter,note\n' +
      'debris,graphics/debris/debris_sheet0.png,4,4,0.012,12,#FFA564,,\n' +
      'probe,graphics/stations/survey_probe.png,1,1,0.05,1,,debris; probe; ghost,\n' +
      'empty,,,,,,red,,\n';
    const model = chartTable(ATLAS_DECORATION_SPEC, text);
    expect(serializeChartTable(model, ATLAS_DECORATION_SPEC.columns)).toBe(text);
    expect(ATLAS_DECORATION_SPEC.issues(model, vocabulary)).toEqual([
      'probe: scatter "probe" must be another decoration id',
      'probe: scatter "ghost" must be another decoration id',
      'empty: needs at least one sprite',
      'empty: colour "red" must be a colour like #A0C8FF',
    ]);
    expect(ATLAS_DECORATION_SPEC.summary?.(model.rows[0], vocabulary)).toBe('12 x 0.012 cells');
  });

  it('mirrors the atlas look and layout loaders: phenomena stay out, colours and system types are checked', () => {
    const regions = chartTable(
      ATLAS_REGION_SPEC,
      'id,weight,stars,planets,light_tint,keep_ruins\n' +
        'normal,10,,,,\n' +
        'collapsed,4,star_white:2; black_hole:4,cat_barren:3; cat_magfield:2,#9090C0,yes\n' +
        'odd,1,,,purple,maybe\n',
    );
    expect(ATLAS_REGION_SPEC.issues(regions, vocabulary)).toEqual([
      'collapsed: stars lists black_hole, but only phenomena add it',
      'collapsed: planets lists cat_magfield, but only phenomena add it',
      'odd: keep_ruins "maybe" must be yes or no',
      'odd: light_tint "purple" must be a colour like #A0C8FF',
    ]);
    const layouts = chartTable(
      ATLAS_LAYOUT_SPEC,
      'id,weight,system_types,min_orbits,max_orbits,max_orbit_radius,terrain,feature,note\n' +
        'ringed,1,SINGLE:4; BINARY_FAR:1,4,7,15000,cat_terrain_rings:2,ringed_giant,\n' +
        'broken,1,NEBULA:1,6,3,,cat_magfield:1,,\n',
    );
    expect(ATLAS_LAYOUT_SPEC.issues(layouts, vocabulary)).toEqual([
      'broken: system_types lists NEBULA, which is not one of SINGLE, BINARY_CLOSE, BINARY_FAR, TRINARY_2CLOSE, TRINARY_1CLOSE_1FAR, TRINARY_2FAR',
      'broken: terrain lists cat_magfield, but only phenomena add it',
      'broken: max_orbits is below min_orbits',
    ]);
    expect(ATLAS_LAYOUT_SPEC.summary?.(layouts.rows[0], vocabulary)).toBe('4-7 orbits, ringed_giant');
    expect(ATLAS_LAYOUT_SPEC.issues(chartTable(ATLAS_LAYOUT_SPEC, 'id,feature\ncrystal,crystal_spire\n'), vocabulary)).toEqual([
      'crystal: feature must be one of ringed_giant, rogue_planet',
    ]);
    const features = chartTable(ATLAS_FEATURE_SPEC, 'id,note\nringed_giant,\nspire,\n');
    expect(ATLAS_FEATURE_SPEC.issues(features, vocabulary)).toEqual(['spire: id must be one of ringed_giant, rogue_planet']);
    expect(ATLAS_FEATURE_SPEC.warnings(features, vocabulary)).toEqual(['No rogue_planet row, so layouts naming it build no feature']);
  });

  it('mirrors the atlas node effect loader: chart effects with numbers, negatives allowed', () => {
    const text = 'id,effects,note\r\nsensor_array,CHART_QUANTITY_PERCENT:-30,\r\nnexus,CHART_MADE_UP:5; CHART_EXTRA_FLEETS,\r\n';
    const model = chartTable(ATLAS_NODE_EFFECT_SPEC, text);
    expect(serializeChartTable(model, ATLAS_NODE_EFFECT_SPEC.columns)).toBe(text);
    expect(ATLAS_NODE_EFFECT_SPEC.issues(model, vocabulary)).toEqual([
      'nexus: CHART_MADE_UP is not a chart effect',
      'nexus: CHART_EXTRA_FLEETS needs a number, as EFFECT:value',
    ]);
  });

  it('mirrors the atlas marker and special site loaders', () => {
    const markers = chartTable(ATLAS_MARKER_SPEC, 'id,chance,chance_per_step,max_chance,note\nvoid,0.35,0,0.35,\nstorm,0.1,,,\n');
    expect(ATLAS_MARKER_SPEC.issues(markers, vocabulary)).toEqual(['storm: id must be one of void, powerful_boss, expansion, phenomenon']);
    expect(ATLAS_MARKER_SPEC.warnings(markers, vocabulary)).toEqual([
      'No powerful_boss row, so it never rolls',
      'No expansion row, so it never rolls',
      'No phenomenon row, so it never rolls',
    ]);
    const specials = chartTable(
      ATLAS_SPECIAL_SPEC,
      'id,kind,placement,chance,min_steps,radius,count,steps,requires_mod,unlock,note\n' +
        'bastion,bastion,fixed,,,,4,25,,,\n' +
        'nexus,infestation,chunk,0.1,8,12,,,,,\n' +
        'idle,sensor_array,chunk,0,4,6,,,,,\n',
    );
    expect(ATLAS_SPECIAL_SPEC.issues(specials, vocabulary)).toEqual(['nexus: radius above 7 counts as 7']);
    expect(ATLAS_SPECIAL_SPEC.warnings(specials, vocabulary)).toEqual(['idle: a chunk site with no chance is never placed']);
    expect(ATLAS_SPECIAL_SPEC.summary?.(specials.rows[0], vocabulary)).toBe('4 fixed at 25 cells');
  });

  it('mirrors the chart loot loader: one drop type per group, base entries need a scope, mode and amount', () => {
    const text =
      'id,drop_type,scope,group,mode,amount,min_tier,note\r\n' +
      'weapons1,weapon,,weapons1,,,,\r\n' +
      'goods,weapon,,goods,,,,\r\n' +
      'goods_again,resource,,goods,,,,\r\n' +
      'fleet_weapons1,weapon,fleet,weapons1,random,1,1,\r\n' +
      'bad_scope,weapon,vault,weapons1,random,1,1,\r\n' +
      'no_mode,ai_core,boss,weapons1,,2,20,\r\n' +
      'coins,currency,,basic,,,,\r\n' +
      'colony,resource,,rare_tech,,,,\r\n';
    const model = chartTable(CHART_LOOT_SPEC, text);
    expect(serializeChartTable(model, CHART_LOOT_SPEC.columns)).toBe(text);
    expect(CHART_LOOT_SPEC.issues(model, vocabulary)).toEqual([
      'goods_again: group goods already belongs to weapon',
      'bad_scope: scope must be one of cache, domain, station, derelict, fleet, boss',
      'no_mode: a base entry needs a mode, random or value',
      'coins: drop_type must be one of weapon, resource, blueprint, ai_core',
      "colony: group rare_tech holds colony items, which only come from the run's single capped roll",
    ]);
    expect(CHART_LOOT_SPEC.warnings(model, vocabulary)).toEqual([
      'no_mode: group weapons1 is mapped to weapon, but this entry is scaled as ai_core',
      'no_mode: min_tier is clamped to 1-15',
    ]);
  });

  it('mirrors the tier loader', () => {
    const model = chartTable(
      CHART_TIER_SPEC,
      'tier,fleet_size,fleets_min,fleets_max,boss,boss_size,level_min,level_max\n' +
        '1,60,2,1,yes,0,30,28\n' +
        '16,x,1.5,3,maybe,,,\n',
    );
    expect(CHART_TIER_SPEC.issues(model, vocabulary)).toEqual([
      'tier 16: tier must be a whole number from 1 to 15',
      'tier 16: fleet_size "x" is not a number',
      'tier 16: boss "maybe" must be yes or no',
    ]);
    expect(CHART_TIER_SPEC.warnings(model, vocabulary)).toEqual([
      'tier 1: fleets_max is below fleets_min, so it is raised to fleets_min',
      'tier 1: level_min (30) is above level_max (28), counting built-in values for blanks, so the game swaps them',
      'tier 1: boss is yes but boss_size is 0, so there is no boss',
      'tier 16: fleets_min 1.5 is rounded to a whole number',
      'No row for tier 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15; the game uses built-in values for them',
    ]);
  });

  it('warns when a colony item chance looks like a percentage or is negative', () => {
    const model = chartTable(CHART_TIER_SPEC, 'tier,colony_item_chance\n1,4\n2,-0.01\n');
    expect(CHART_TIER_SPEC.warnings(model, vocabulary)).toEqual(
      expect.arrayContaining(['tier 1: colony_item_chance above 100% counts as 100%; it is a fraction, so 0.04 is 4%']),
    );
    expect(CHART_TIER_SPEC.warnings(model, vocabulary).filter((warning) => warning.startsWith('tier 2: colony_item_chance'))).toHaveLength(1);
  });

  it('keeps the tier level columns on a round trip and warns where the game clamps them', () => {
    expect(CHART_TIER_SPEC.columns.map((column) => column.column)).toEqual(expect.arrayContaining(['level_min', 'level_max']));
    expect(CHART_TIER_SPEC.columns.map((column) => column.column)).not.toContain('fleet_strength_min');
    const text = 'tier,level_min,level_max\r\n1,26,29\r\n15,-2,60\r\n';
    const model = chartTable(CHART_TIER_SPEC, text);
    expect(serializeChartTable(model, CHART_TIER_SPEC.columns)).toBe(text);
    expect(CHART_TIER_SPEC.warnings(model, vocabulary)).toEqual(
      expect.arrayContaining([
        'tier 15: level_min below 0 counts as 0',
        'tier 15: level_max is above the default max level (50), so it is clamped to the max level setting',
      ]),
    );
    expect(CHART_TIER_SPEC.warnings(model, vocabulary).filter((warning) => warning.startsWith('tier 1:'))).toEqual([]);
  });

  it('gives each site category its own tier budget column and checks them like the tier loader', () => {
    expect(CHART_TIER_SPEC.columns.map((column) => column.column)).toEqual(expect.arrayContaining(['cache_budget', 'domain_budget', 'station_budget', 'derelict_budget']));
    expect(CHART_TIER_SPEC.columns.map((column) => column.column)).not.toContain('loot_budget');
    const text = 'tier,cache_budget,domain_budget,station_budget,derelict_budget\r\n1,3.5,0,-2,x\r\n';
    const model = chartTable(CHART_TIER_SPEC, text);
    expect(serializeChartTable(model, CHART_TIER_SPEC.columns)).toBe(text);
    expect(CHART_TIER_SPEC.issues(model, vocabulary)).toEqual(['tier 1: derelict_budget "x" is not a number']);
    expect(CHART_TIER_SPEC.warnings(model, vocabulary)).toContain('tier 1: station_budget below 0 counts as 0');
  });

  it('mirrors the point of interest, faction and blocked ability loaders', () => {
    const pois = chartTable(
      CHART_POI_SPEC,
      'id,kind,entity,derelict_type,cost,weight,min_tier,max_tier,guarded\n' + 'wreck,derelict,wreck,HUGE,2,1,1,15,no\n' + 'odd,shrine,,,0,1,9,3,sometimes\n',
    );
    expect(CHART_POI_SPEC.issues(pois, vocabulary)).toEqual([
      'wreck: a derelict needs a derelict type, one of CIVILIAN, LARGE, MEDIUM, SMALL',
      'odd: kind must be one of cache, domain, station, derelict',
      'odd: needs an entity type',
      'odd: guarded "sometimes" must be yes or no',
    ]);
    expect(CHART_POI_SPEC.warnings(pois, vocabulary)).toEqual([
      'odd: min_tier is above max_tier, so the game swaps them',
      'odd: a cost or weight of 0 or less means it is never placed',
    ]);
    const factions = chartTable(CHART_FACTION_SPEC, 'faction,weight\npirates,0\npirates,1\n');
    expect(CHART_FACTION_SPEC.issues(factions, vocabulary)).toEqual(['pirates: listed more than once, so only its last row is used']);
    expect(CHART_FACTION_SPEC.warnings(factions, vocabulary)).toEqual([
      'pirates: a weight of 0 or less means it is never picked',
    ]);
    const abilities = chartTable(CHART_BLOCKED_ABILITY_SPEC, 'ability_id,note\nfracture_jump,\nfracture_jump,\n');
    expect(CHART_BLOCKED_ABILITY_SPEC.issues(abilities, vocabulary)).toEqual([]);
    expect(CHART_BLOCKED_ABILITY_SPEC.warnings(abilities, vocabulary)).toEqual(['fracture_jump: listed more than once, which changes nothing']);
  });
});

describe('chart currencies in the Crafting tab', () => {
  it('lists the three ciphers with a parts cost', () => {
    const ciphers = CRAFTING_ITEMS.filter((item) => item.starChart);
    expect(ciphers.map((item) => [item.id, item.cost])).toEqual([
      ['exiledSector_reforge_cipher', true],
      ['exiledSector_annul_cipher', true],
      ['exiledSector_cartographer_cipher', true],
    ]);
  });
});
