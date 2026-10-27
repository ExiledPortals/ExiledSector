import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import {
  definitionWarnings,
  formatPool,
  markDefinitionsSaved,
  newItemDefinition,
  parseItemDefinitions,
  parsePool,
  poolShares,
  serializeHullItemsCsv,
  serializeSubroutinesCsv,
  setDefinitionKind,
  validateItemDefinitions,
  type ItemDefinition,
  type ItemDefinitionsModel,
} from './definitions.ts';
import { modifierById, parseModifiersCsv } from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [
    { name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false },
    { name: 'ARMOR_FLAT', statMode: 'FLAT', lowerIsBetter: false },
  ],
  tags: { theme: [], region: [], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['subroutine', 'hull_item'],
  itemKinds: [
    { id: 'subroutine', type: 'subroutine' },
    { id: 'bridge', type: 'hull_item' },
  ],
  subroutineKinds: ['MODSPEC_CACHE', 'DOCTRINE', 'BYPASS', 'UNIQUE'],
  itemUnlocks: ['defeated_ziggurat'],
});

const catalogue = modifierById(
  parseModifiersCsv(
    'id,slot,item_types,lines,name\n' +
      'HULL_MULT,prefix,hull_item,HULL_MULT:1:2,Reinforced\n' +
      'HULL_MULT_BRIDGE,prefix,hull_item,HULL_MULT:3:4,Reinforced\n' +
      'ARMOR_FLAT,suffix,hull_item,ARMOR_FLAT:5/10/15/20,Plating\n',
  ).modifiers,
);

function definition(overrides: Partial<ItemDefinition>): ItemDefinition {
  return {
    cells: [],
    id: 'good_id',
    kind: 'bridge',
    name: 'Good',
    icon: 'graphics/icons/a.png',
    rarity: '10',
    unique: '',
    description: '',
    prefixes: parsePool('@HULL_MULT'),
    suffixes: parsePool('@ARMOR_FLAT:3'),
    unlock: '',
    ...overrides,
  };
}

describe('item definitions', () => {
  it('parses modifier references with optional weights, keeps inline entries as written and formats them back', () => {
    const pool = parsePool(' @HULL_MULT:1500 ;@ARMOR_FLAT; HULL_MULT:4:6 ');
    expect(pool).toEqual([
      { modifierId: 'HULL_MULT', weight: '1500' },
      { modifierId: 'ARMOR_FLAT', weight: '' },
      { modifierId: '', weight: '', inlineText: 'HULL_MULT:4:6' },
    ]);
    expect(formatPool(pool)).toBe('@HULL_MULT:1500; @ARMOR_FLAT; HULL_MULT:4:6');
  });

  it('shares weight within a group, counting a blank weight as 1', () => {
    expect(poolShares(parsePool('@HULL_MULT:3; @ARMOR_FLAT'))).toEqual([0.75, 0.25]);
  });

  it('marks the ids that were saved, not ids renamed or added after the save started', () => {
    const saved: Pick<ItemDefinitionsModel, 'definitions'> = {
      definitions: [definition({ id: 'renamed', loadedId: 'loaded' }), definition({ id: 'added_before_save' })],
    };
    const current: Pick<ItemDefinitionsModel, 'definitions'> = {
      definitions: [
        definition({ id: 'renamed_again', loadedId: 'loaded' }),
        definition({ id: 'added_before_save' }),
        definition({ id: 'added_during_save' }),
      ],
    };
    markDefinitionsSaved(current, saved);
    expect(current.definitions.map((entry) => entry.loadedId)).toEqual(['renamed', 'added_before_save', undefined]);
  });

  it('reports every rule the game would reject', () => {
    expect(validateItemDefinitions([definition({})], vocabulary, catalogue)).toEqual([]);
    const issues = validateItemDefinitions(
      [
        definition({
          id: 'Bad Id',
          kind: 'nope',
          icon: 'graphics/unused/x.png',
          rarity: '-1',
          unlock: 'mystery',
          prefixes: parsePool('@HULL_MULT; @HULL_MULT_BRIDGE; @NOT_THERE:0; ARMOR_FLAT:1:2'),
          suffixes: parsePool('@HULL_MULT'),
        }),
      ],
      vocabulary,
      catalogue,
    );
    expect(issues).toEqual([
      'Bad Id: id must use only lowercase letters, digits and underscores',
      'Bad Id: kind must be one of subroutine, bridge',
      'Bad Id: icon graphics/unused/x.png is in a folder that is not deployed',
      'Bad Id: rarity must be zero or more',
      'Bad Id: unknown drop condition mystery',
      'Bad Id: HULL_MULT is listed twice, as HULL_MULT and HULL_MULT_BRIDGE',
      'Bad Id: NOT_THERE needs a weight above zero',
      'Bad Id: NOT_THERE is not in item_modifiers.csv',
      'Bad Id: "ARMOR_FLAT:1:2" is written inline; pick a modifier instead',
      'Bad Id: HULL_MULT is a prefix, so it cannot be one of its suffixes',
      'Bad Id: HULL_MULT is listed more than once',
      'Bad Id: HULL_MULT is listed twice, as HULL_MULT_BRIDGE and HULL_MULT',
    ]);
  });

  it('needs a known kind on every subroutine and keeps prefixes and suffixes off it', () => {
    const bypass = definition({ id: 'bypass', kind: 'subroutine', subroutineKind: 'BYPASS', prefixes: [], suffixes: [] });
    const pooled = definition({ id: 'pooled', kind: 'subroutine', subroutineKind: 'EXPANSION', prefixes: parsePool('@HULL_MULT'), suffixes: [] });
    const kindless = definition({ id: 'kindless', kind: 'subroutine', prefixes: [], suffixes: [] });

    expect(validateItemDefinitions([bypass, pooled, kindless], vocabulary, catalogue)).toEqual([
      'pooled: subroutine kind must be one of MODSPEC_CACHE, DOCTRINE, BYPASS, UNIQUE',
      'pooled: subroutines do not roll prefixes or suffixes; remove them',
      'pooled: HULL_MULT only rolls on hull_item, not subroutine',
      'kindless: subroutine kind must be one of MODSPEC_CACHE, DOCTRINE, BYPASS, UNIQUE',
    ]);
  });

  it('starts a new subroutine with a kind and no prefixes or suffixes', () => {
    const subroutine = newItemDefinition([], 'subroutine', [...catalogue.values()], [], 'DOCTRINE');

    expect(subroutine).toMatchObject({ id: 'new_subroutine', kind: 'subroutine', subroutineKind: 'DOCTRINE', prefixes: [], suffixes: [] });
    expect(validateItemDefinitions([subroutine], vocabulary, catalogue)).toEqual([]);
    expect(newItemDefinition([], 'bridge', [...catalogue.values()]).prefixes).toEqual([{ modifierId: 'HULL_MULT', weight: '' }]);
  });

  it('reads subroutines and hull items from their own files and saves each back to its file', () => {
    const subroutineHeader = 'id,name,icon,rarity,unique,kind,description,unlock,notes\n';
    const hullItemHeader = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n';
    const subroutinesText = `${subroutineHeader}chip,Chip,,10,,DOCTRINE,,,keep\n`;
    const hullItemsText = `${hullItemHeader}plate,bridge,Plate,,10,,,@HULL_MULT_BRIDGE,,\n`;
    const model = parseItemDefinitions(subroutinesText, hullItemsText);

    expect(model.definitions.map((entry) => [entry.id, entry.kind, entry.subroutineKind])).toEqual([
      ['chip', 'subroutine', 'DOCTRINE'],
      ['plate', 'bridge', undefined],
    ]);
    expect(serializeSubroutinesCsv(model)).toBe(subroutinesText);
    expect(serializeHullItemsCsv(model)).toBe(hullItemsText);

    model.definitions[0].subroutineKind = 'BYPASS';
    expect(serializeSubroutinesCsv(model)).toBe(`${subroutineHeader}chip,Chip,,10,,BYPASS,,,keep\n`);

    setDefinitionKind(model.definitions[0], 'bridge');
    expect(serializeSubroutinesCsv(model)).toBe(subroutineHeader);
    expect(serializeHullItemsCsv(model)).toBe(`${hullItemHeader}chip,bridge,Chip,,10,,,,,\nplate,bridge,Plate,,10,,,@HULL_MULT_BRIDGE,,\n`);
  });

  it('keeps leftover pool columns of an older subroutines.csv so removing their entries is saved', () => {
    const olderText = 'id,name,icon,rarity,unique,description,prefixes,suffixes,unlock\nchip,Chip,,10,,,@HULL_MULT,,\n';
    const model = parseItemDefinitions(olderText, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(serializeSubroutinesCsv(model)).toBe('id,name,icon,rarity,unique,description,prefixes,suffixes,unlock,kind\nchip,Chip,,10,,,@HULL_MULT,,,\n');
    model.definitions[0].prefixes = [];
    model.definitions[0].subroutineKind = 'UNIQUE';
    expect(serializeSubroutinesCsv(model)).toBe('id,name,icon,rarity,unique,description,prefixes,suffixes,unlock,kind\nchip,Chip,,10,,,,,,UNIQUE\n');
  });
});

describe('hull item sources', () => {
  const header = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock,source\n';

  it('reads the source column and writes it back unchanged', () => {
    const text = `${header}relic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,,faction:pirates; site:wreck\n`;
    const model = parseItemDefinitions('id,name,icon,rarity,unique,kind,description,unlock\n', text);

    expect(model.definitions[0].source).toBe('faction:pirates; site:wreck');
    expect(serializeHullItemsCsv(model)).toBe(text);

    model.definitions[0].source = 'faction:tritachyon';
    expect(serializeHullItemsCsv(model)).toBe(`${header}relic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,,faction:tritachyon\n`);
  });

  it('adds the source column to an older file only once a hull item uses it', () => {
    const olderText = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\nrelic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,\n';
    const model = parseItemDefinitions('id,name,icon,rarity,unique,kind,description,unlock\n', olderText);

    expect(serializeHullItemsCsv(model)).toBe(olderText);
    model.definitions[0].source = 'faction:pirates';
    expect(serializeHullItemsCsv(model)).toBe(`${header}relic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,,faction:pirates\n`);
  });

  it('checks the source text and warns when a common hull item names one', () => {
    const issues = validateItemDefinitions(
      [
        definition({ id: 'fleet_relic', unique: 'true', source: 'faction:pirates; site:wreck' }),
        definition({ id: 'misplaced', unique: 'true', source: 'colony:jangala; faction:pirates; faction:pirates' }),
      ],
      vocabulary,
      catalogue,
    );

    expect(issues).toEqual(['misplaced: source "colony:jangala" is not faction:ID or site:ID', 'misplaced: source "faction:pirates" is listed more than once']);
    expect(definitionWarnings([definition({ id: 'common', source: 'faction:pirates' }), definition({ id: 'relic', unique: 'true', source: 'faction:pirates' })], 'ship')).toEqual([
      'common: the source faction:pirates only applies to unique hull items, so it is ignored',
    ]);
  });

  it('starts a new hull item with a blank source', () => {
    expect(newItemDefinition([], 'bridge', []).source).toBe('');
  });
});

describe('hull item mechanics', () => {
  it('adds the mechanic column only once a hull item uses it and writes it back', () => {
    const subroutinesText = 'id,name,icon,rarity,unique,kind,description,unlock\n';
    const olderText = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\nrelic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,\n';
    const model = parseItemDefinitions(subroutinesText, olderText);

    expect(serializeHullItemsCsv(model)).toBe(olderText);
    model.definitions[0].mechanic = 'one_of';
    const withMechanic = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock,mechanic\nrelic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,,one_of\n';
    expect(serializeHullItemsCsv(model)).toBe(withMechanic);
    expect(parseItemDefinitions(subroutinesText, withMechanic).definitions[0].mechanic).toBe('one_of');
  });

  it('rejects an unknown mechanic and warns when a common hull item names one', () => {
    const issues = validateItemDefinitions([definition({ id: 'odd', unique: 'true', mechanic: 'tripler' })], vocabulary, catalogue);

    expect(issues).toEqual(['odd: mechanic must be blank or one of one_of']);
    expect(definitionWarnings([definition({ id: 'common', mechanic: 'one_of' })], 'ship')).toEqual([
      'common: the mechanic one_of is meant for unique hull items',
    ]);
  });
});

describe('item areas', () => {
  it('writes the area column of hull items and subroutines only once one is set', () => {
    const subroutinesText = 'id,name,icon,rarity,unique,kind,description,unlock\nchip,Chip,,10,,MODSPEC_CACHE,,\n';
    const hullItemsText = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\nrelic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,\n';
    const model = parseItemDefinitions(subroutinesText, hullItemsText);

    expect(serializeHullItemsCsv(model)).toBe(hullItemsText);
    expect(serializeSubroutinesCsv(model)).toBe(subroutinesText);
    model.definitions.find((definition) => definition.id === 'relic')!.area = 'tania';
    model.definitions.find((definition) => definition.id === 'chip')!.area = 'nightcross';
    expect(serializeHullItemsCsv(model)).toBe('id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock,area\nrelic,bridge,Relic,,10,true,,@HULL_MULT_BRIDGE,,,tania\n');
    expect(serializeSubroutinesCsv(model)).toBe('id,name,icon,rarity,unique,kind,description,unlock,area\nchip,Chip,,10,,MODSPEC_CACHE,,,nightcross\n');
  });

  it('rejects an area the vocabulary does not list', () => {
    const issues = validateItemDefinitions([definition({ id: 'lost', area: 'atlantis' })], { ...vocabulary, itemAreas: ['tania'] }, catalogue);

    expect(issues).toEqual(['lost: area must be blank or one of tania']);
  });
});

describe('unique subroutines', () => {
  const uniqueCatalogue = modifierById(
    parseModifiersCsv(
      'id,slot,item_types,lines,name\n' +
        'HULL_MULT_RELIC,prefix,unique_subroutine,HULL_MULT:5:8,\n' +
        'ARMOR_FLAT_RELIC,suffix,unique_subroutine,ARMOR_FLAT:5:10,\n' +
        'HULL_MULT,prefix,hull_item,HULL_MULT:1:2,Reinforced\n',
    ).modifiers,
  );
  const header = 'id,name,icon,rarity,unique,kind,description,lines,source,unlock\n';

  function relic(overrides: Partial<ItemDefinition>): ItemDefinition {
    return definition({
      id: 'relic',
      kind: 'subroutine',
      unique: 'true',
      subroutineKind: 'UNIQUE',
      prefixes: [],
      suffixes: [],
      lines: parsePool('@HULL_MULT_RELIC; ARMOR_FLAT:3:3'),
      source: 'faction:pirates',
      ...overrides,
    });
  }

  it('reads the lines and source columns and writes them back unchanged', () => {
    const text = `${header}relic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC; ARMOR_FLAT:3:3,faction:pirates; site:wreck,\n`;
    const model = parseItemDefinitions(text, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(model.definitions[0].lines).toEqual([{ modifierId: 'HULL_MULT_RELIC', weight: '' }, { modifierId: '', weight: '', inlineText: 'ARMOR_FLAT:3:3' }]);
    expect(model.definitions[0].source).toBe('faction:pirates; site:wreck');
    expect(serializeSubroutinesCsv(model)).toBe(text);

    model.definitions[0].source = 'site:wreck';
    expect(serializeSubroutinesCsv(model)).toBe(`${header}relic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC; ARMOR_FLAT:3:3,site:wreck,\n`);
  });

  it('adds the lines and source columns to an older file only once a subroutine uses them', () => {
    const olderText = 'id,name,icon,rarity,unique,kind,description,unlock\nchip,Chip,,10,,DOCTRINE,,\n';
    const model = parseItemDefinitions(olderText, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(serializeSubroutinesCsv(model)).toBe(olderText);
    model.definitions[0].subroutineKind = 'UNIQUE';
    model.definitions[0].lines = parsePool('@HULL_MULT_RELIC');
    expect(serializeSubroutinesCsv(model)).toBe('id,name,icon,rarity,unique,kind,description,unlock,lines\nchip,Chip,,10,,UNIQUE,,,@HULL_MULT_RELIC\n');
    model.definitions[0].source = 'site:wreck';
    expect(serializeSubroutinesCsv(model)).toBe('id,name,icon,rarity,unique,kind,description,unlock,lines,source\nchip,Chip,,10,,UNIQUE,,,@HULL_MULT_RELIC,site:wreck\n');
  });

  it('reads and writes the socket limit, adding its column to an older file only once it is set', () => {
    const withLimit = 'id,name,icon,rarity,unique,kind,description,lines,source,socket_limit,unlock\nrelic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC,site:wreck,1,\n';
    const limited = parseItemDefinitions(withLimit, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');
    const olderText = 'id,name,icon,rarity,unique,kind,description,unlock\nchip,Chip,,10,,DOCTRINE,,\n';
    const older = parseItemDefinitions(olderText, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(limited.definitions[0].socketLimit).toBe('1');
    expect(serializeSubroutinesCsv(limited)).toBe(withLimit);
    expect(serializeSubroutinesCsv(older)).toBe(olderText);
    older.definitions[0].socketLimit = '2';
    expect(serializeSubroutinesCsv(older)).toBe('id,name,icon,rarity,unique,kind,description,unlock,socket_limit\nchip,Chip,,10,,DOCTRINE,,,2\n');
  });

  it('reads and writes the radius, adding its column to an older file only once it is set', () => {
    const withRadius = 'id,name,icon,rarity,unique,kind,description,lines,source,radius,unlock\nrelic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC,site:wreck,small,\n';
    const ringed = parseItemDefinitions(withRadius, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');
    const olderText = 'id,name,icon,rarity,unique,kind,description,lines,source,unlock\nrelic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC,site:wreck,\n';
    const older = parseItemDefinitions(olderText, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(ringed.definitions[0].radius).toBe('small');
    expect(serializeSubroutinesCsv(ringed)).toBe(withRadius);
    expect(serializeSubroutinesCsv(older)).toBe(olderText);
    older.definitions[0].radius = 'large';
    expect(serializeSubroutinesCsv(older)).toBe(`${olderText.split('\n')[0]},radius\nrelic,Relic,,10,true,UNIQUE,,@HULL_MULT_RELIC,site:wreck,,large\n`);
  });

  it('accepts a blank or known radius on a unique subroutine and keeps it off other kinds', () => {
    expect(validateItemDefinitions([relic({ radius: '' }), relic({ id: 'ringed', radius: 'medium' })], vocabulary, uniqueCatalogue)).toEqual([]);
    expect(
      validateItemDefinitions(
        [relic({ id: 'huge', radius: 'huge' }), relic({ id: 'bypass', subroutineKind: 'BYPASS', lines: [], source: '', radius: 'small' })],
        vocabulary,
        uniqueCatalogue,
      ),
    ).toEqual(['huge: radius must be blank or one of small, medium, large', 'bypass: only UNIQUE subroutines read a radius from subroutines.csv; remove it']);
  });

  it('starts a new subroutine with no radius', () => {
    expect(newItemDefinition([], 'subroutine', [], [], 'UNIQUE').radius).toBe('');
  });

  it('accepts a blank or whole socket limit and refuses anything else', () => {
    expect(validateItemDefinitions([relic({ socketLimit: '' }), relic({ id: 'zero', socketLimit: '0' }), relic({ id: 'one', socketLimit: '1' })], vocabulary, uniqueCatalogue)).toEqual([]);
    expect(validateItemDefinitions([relic({ id: 'negative', socketLimit: '-1' }), relic({ id: 'fractional', socketLimit: '1.5' })], vocabulary, uniqueCatalogue)).toEqual([
      'negative: socket limit must be blank or a whole number of 0 or more',
      'fractional: socket limit must be blank or a whole number of 0 or more',
    ]);
  });

  it('accepts a unique subroutine with scoped or inline lines and a source', () => {
    expect(validateItemDefinitions([relic({})], vocabulary, uniqueCatalogue)).toEqual([]);
  });

  it('checks the flag, lines and source of a unique subroutine and keeps lines off other kinds', () => {
    const issues = validateItemDefinitions(
      [
        relic({ id: 'flagless', unique: '' }),
        relic({ id: 'empty', lines: [], source: '' }),
        relic({ id: 'misplaced', source: 'colony:jangala; faction:pirates; faction:pirates' }),
        relic({ id: 'weighted', lines: parsePool('@HULL_MULT_RELIC:4; HULL_MULT:1:2:3') }),
        relic({ id: 'wrong_scope', lines: parsePool('@HULL_MULT; @ARMOR_FLAT_RELIC; @MISSING') }),
        relic({ id: 'bypass', subroutineKind: 'BYPASS', lines: parsePool('@HULL_MULT_RELIC'), source: '' }),
      ],
      vocabulary,
      uniqueCatalogue,
    );

    expect(issues).toEqual([
      'flagless: a UNIQUE subroutine must be marked unique',
      'empty: a UNIQUE subroutine needs at least one line',
      'empty: a UNIQUE subroutine needs a source, such as faction:pirates or site:station_research',
      'misplaced: source "colony:jangala" is not faction:ID or site:ID',
      'misplaced: source "faction:pirates" is listed more than once',
      'weighted: line HULL_MULT_RELIC has a weight or tier range, but every unique subroutine line always rolls',
      'weighted: line "HULL_MULT:1:2:3" has a weight or tier bands, but every unique subroutine line always rolls',
      'weighted: HULL_MULT is listed twice, as HULL_MULT_RELIC and HULL_MULT',
      'wrong_scope: HULL_MULT only rolls on hull_item, not unique_subroutine',
      'wrong_scope: ARMOR_FLAT_RELIC is a suffix, but unique subroutine lines are prefixes',
      'wrong_scope: line MISSING is not in item_modifiers.csv',
      'bypass: only UNIQUE subroutines have lines; remove them',
    ]);
  });
});

describe('modspec cache subroutines', () => {
  function cache(overrides: Partial<ItemDefinition>): ItemDefinition {
    return definition({ kind: 'subroutine', subroutineKind: 'MODSPEC_CACHE', prefixes: [], suffixes: [], nodeCountChances: '0.9;0.09;0.01', salvageChance: '0.02', ...overrides });
  }

  it('reads and writes the cache columns, adding them to an older file only once they are set', () => {
    const withCache =
      'id,name,icon,rarity,unique,kind,description,lines,source,socket_limit,unlock,excluded_nodes,node_count_chances,drop_chance,salvage_chance,notes\n' +
      'cache,Cache,,10,true,MODSPEC_CACHE,,,,,,hull_breacher_1; torpedo_specialisation_1,0.9;0.09;0.01,0.02,,keep\n';
    const model = parseItemDefinitions(withCache, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');
    const olderText = 'id,name,icon,rarity,unique,kind,description,unlock\ncache,Cache,,10,true,MODSPEC_CACHE,,\n';
    const older = parseItemDefinitions(olderText, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(model.definitions[0]).toMatchObject({
      excludedNodes: 'hull_breacher_1; torpedo_specialisation_1',
      nodeCountChances: '0.9;0.09;0.01',
      dropChance: '0.02',
      salvageChance: '',
    });
    expect(serializeSubroutinesCsv(model)).toBe(withCache);
    model.definitions[0].nodeCountChances = '1';
    expect(serializeSubroutinesCsv(model)).toBe(withCache.replace('0.9;0.09;0.01', '1'));
    expect(serializeSubroutinesCsv(older)).toBe(olderText);
    older.definitions[0].nodeCountChances = '0.5;0.5';
    expect(serializeSubroutinesCsv(older)).toBe('id,name,icon,rarity,unique,kind,description,unlock,node_count_chances\ncache,Cache,,10,true,MODSPEC_CACHE,,,0.5;0.5\n');
  });

  it('keeps an older volume column as an unknown column', () => {
    const withVolume =
      'id,name,icon,rarity,unique,kind,description,lines,source,socket_limit,unlock,volume,excluded_nodes,drop_chance,salvage_chance\n' +
      'chip,Chip,,10,,BYPASS,,,,,,hegemony,,,\n';
    const model = parseItemDefinitions(withVolume, 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n');

    expect(serializeSubroutinesCsv(model)).toBe(withVolume);
  });

  it('starts a new cache subroutine with every cache field blank', () => {
    expect(newItemDefinition([], 'subroutine', [], [], 'MODSPEC_CACHE')).toMatchObject({
      subroutineKind: 'MODSPEC_CACHE',
      excludedNodes: '',
      nodeCountChances: '',
      dropChance: '',
      salvageChance: '',
    });
  });

  it('checks the exclusions, node count chances and chances, allows several caches, and keeps the fields off other kinds', () => {
    const issues = validateItemDefinitions(
      [
        cache({ id: 'good', excludedNodes: 'hull_breacher_1; exoship_core', dropChance: '0.02' }),
        cache({ id: 'second_good', nodeCountChances: '0;1' }),
        cache({ id: 'messy', excludedNodes: 'a b; hull_breacher_1; hull_breacher_1', dropChance: '-1', salvageChance: 'often' }),
        cache({ id: 'no_counts', nodeCountChances: '' }),
        cache({ id: 'bad_counts', nodeCountChances: '0.9;;-1;lots' }),
        cache({ id: 'zero_counts', nodeCountChances: '0;0' }),
        definition({ id: 'doctrine', kind: 'subroutine', subroutineKind: 'DOCTRINE', prefixes: [], suffixes: [], nodeCountChances: '1' }),
      ],
      vocabulary,
      new Map(),
    );

    expect(issues).toEqual([
      'messy: excluded node "a b" is not a node id',
      'messy: excluded node hull_breacher_1 is listed more than once',
      'messy: drop chance must be blank or a number of 0 or more',
      'messy: salvage chance must be blank or a number of 0 or more',
      'no_counts: a MODSPEC_CACHE subroutine needs node count chances, such as 0.9;0.09;0.01',
      'bad_counts: node count chance "" must be a number of 0 or more',
      'bad_counts: node count chance "-1" must be a number of 0 or more',
      'bad_counts: node count chance "lots" must be a number of 0 or more',
      'zero_counts: at least one node count chance must be above 0',
      'doctrine: only MODSPEC_CACHE subroutines have excluded nodes, node count chances or drop chances; clear them',
    ]);
  });
});
