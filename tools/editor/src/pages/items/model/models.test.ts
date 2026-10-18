import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { fractionToPercentText, percentToFractionText } from '../../../shared/decimalText.ts';
import {
  formatPool,
  markDefinitionsSaved,
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
import { dropSummary, setChance, validateDrops, type DropRow } from './drops.ts';
import { modifierById, parseModifiersCsv } from './modifiers.ts';
import { validateNames, wordList } from './names.ts';

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
  itemUnlocks: ['defeated_ziggurat'],
});

const catalogue = modifierById(
  parseModifiersCsv(
    'id,slot,item_types,lines,name\n' +
      'HULL_MULT,prefix,subroutine;hull_item,HULL_MULT:1:2,Reinforced\n' +
      'HULL_MULT_BRIDGE,prefix,hull_item,HULL_MULT:3:4,Reinforced\n' +
      'ARMOR_FLAT,suffix,subroutine;hull_item,ARMOR_FLAT:5/10/15/20,Plating\n',
  ).modifiers,
);

function definition(overrides: Partial<ItemDefinition>): ItemDefinition {
  return {
    cells: [],
    id: 'good_id',
    kind: 'subroutine',
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

  it('rejects a modifier scoped to another item type', () => {
    const subroutine = definition({ id: 'chip', prefixes: parsePool('@HULL_MULT_BRIDGE'), suffixes: [] });
    const hullItem = definition({ id: 'plate', kind: 'bridge', prefixes: parsePool('@HULL_MULT_BRIDGE'), suffixes: [] });

    expect(validateItemDefinitions([subroutine, hullItem], vocabulary, catalogue)).toEqual([
      'chip: HULL_MULT_BRIDGE only rolls on hull_item, not subroutine',
    ]);
  });

  it('reads subroutines and hull items from their own files and saves each back to its file', () => {
    const subroutineHeader = 'id,name,icon,rarity,unique,description,prefixes,suffixes,unlock,notes\n';
    const hullItemHeader = 'id,section,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n';
    const subroutinesText = `${subroutineHeader}chip,Chip,,10,,,@HULL_MULT,,,keep\n`;
    const hullItemsText = `${hullItemHeader}plate,bridge,Plate,,10,,,@HULL_MULT_BRIDGE,,\n`;
    const model = parseItemDefinitions(subroutinesText, hullItemsText);

    expect(model.definitions.map((entry) => [entry.id, entry.kind])).toEqual([
      ['chip', 'subroutine'],
      ['plate', 'bridge'],
    ]);
    expect(serializeSubroutinesCsv(model)).toBe(subroutinesText);
    expect(serializeHullItemsCsv(model)).toBe(hullItemsText);

    setDefinitionKind(model.definitions[0], 'bridge');
    expect(serializeSubroutinesCsv(model)).toBe(subroutineHeader);
    expect(serializeHullItemsCsv(model)).toBe(`${hullItemHeader}chip,bridge,Chip,,10,,,@HULL_MULT,,\nplate,bridge,Plate,,10,,,@HULL_MULT_BRIDGE,,\n`);
  });
});

describe('drops', () => {
  function row(chances: string[]): DropRow {
    return { cells: [], passthrough: false, site: 'derelict', chances, unique: '' };
  }

  it('summarises the chance of at least N items as a product of the chained rolls', () => {
    expect(dropSummary(row(['0.5', '0.5', '0.1']))).toBe('1+: 50%, 2+: 25%, 3+: 2.5%');
  });

  it('pads earlier slots and trims trailing blanks when a slot changes', () => {
    const drop = row(['0.5']);
    setChance(drop, 2, '0.1');
    expect(drop.chances).toEqual(['0.5', '', '0.1']);
    setChance(drop, 2, '');
    expect(drop.chances).toEqual(['0.5']);
  });

  it('rejects gaps, out-of-range chances and duplicate sites, but ignores comment rows', () => {
    const issues = validateDrops({
      table: { header: [], rows: [], lineEnding: '\n' },
      rows: [row(['', '1.5']), row(['0.2']), { cells: [], passthrough: true, site: '#note', chances: ['9'], unique: '' }],
    });
    expect(issues).toEqual([
      'derelict: item 1 is blank but a later item is set',
      'derelict: item 2 must be between 0 and 100',
      'derelict: listed more than once',
    ]);
  });

  it('converts between stored fractions and shown percentages without float noise', () => {
    expect(fractionToPercentText('0.075')).toBe('7.5');
    expect(percentToFractionText('7.5')).toBe('0.075');
    expect(percentToFractionText('0.3')).toBe('0.003');
    expect(fractionToPercentText('')).toBe('');
  });
});

describe('names', () => {
  it('needs both word lists for every rare name entry', () => {
    expect(validateNames({ rareNames: { a: { first: ['X'] }, b: { first: ['Y'], second: ['Z'] } }, rareNamesLineEnding: '\n' })).toEqual([
      'a: needs at least one first word and one second word',
    ]);
  });

  it('splits word lists on new lines and commas', () => {
    expect(wordList('Alpha\n Beta, Gamma\n\n')).toEqual(['Alpha', 'Beta', 'Gamma']);
  });
});
