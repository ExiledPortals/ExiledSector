import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { fractionToPercentText, percentToFractionText } from '../../../shared/decimalText.ts';
import { formatPool, parsePool, poolShares, togglePerHullSize, validateSocketables, type SocketableDefinition } from './definitions.ts';
import { dropSummary, setChance, validateDrops, type DropRow } from './drops.ts';
import { missingEffects, validateNames, wordList } from './names.ts';

const vocabulary = buildVocabulary({
  effects: [
    { name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false },
    { name: 'ARMOR_FLAT', statMode: 'FLAT', lowerIsBetter: false },
  ],
  tags: { theme: [], region: [], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  socketableKinds: ['subroutine', 'bridge'],
  socketableUnlocks: ['defeated_ziggurat'],
});

function definition(overrides: Partial<SocketableDefinition>): SocketableDefinition {
  return {
    cells: [],
    id: 'good_id',
    kind: 'subroutine',
    name: 'Good',
    icon: 'graphics/icons/a.png',
    rarity: '10',
    unique: '',
    description: '',
    prefixes: parsePool('HULL_MULT:1:2'),
    suffixes: parsePool('ARMOR_FLAT:5/10/15/20:3'),
    unlock: '',
    ...overrides,
  };
}

describe('socketable pools', () => {
  it('parses ranged and per-hull-size entries with optional weights and formats them back', () => {
    const pool = parsePool(' HULL_MULT:4:6:1500 ;ARMOR_FLAT:5/10/15/20; ');
    expect(pool).toEqual([
      { effect: 'HULL_MULT', min: '4', max: '6', weight: '1500' },
      { effect: 'ARMOR_FLAT', min: '', max: '', hullValues: ['5', '10', '15', '20'], weight: '' },
    ]);
    expect(formatPool(pool)).toBe('HULL_MULT:4:6:1500; ARMOR_FLAT:5/10/15/20');
  });

  it('shares weight within a group, counting a blank weight as 1', () => {
    expect(poolShares(parsePool('HULL_MULT:1:2:3; ARMOR_FLAT:1:2'))).toEqual([0.75, 0.25]);
  });

  it('switches to per-hull-size values and back to the original range', () => {
    const [entry] = parsePool('HULL_MULT:4:6');
    togglePerHullSize(entry, true);
    expect(entry.hullValues).toEqual(['4', '4', '4', '4']);
    togglePerHullSize(entry, false);
    expect(entry).toEqual({ effect: 'HULL_MULT', min: '4', max: '6', weight: '' });
  });

  it('reports every rule the game would reject', () => {
    expect(validateSocketables([definition({})], vocabulary)).toEqual([]);
    const issues = validateSocketables(
      [
        definition({
          id: 'Bad Id',
          kind: 'nope',
          icon: 'graphics/unused/x.png',
          rarity: '-1',
          unlock: 'mystery',
          prefixes: parsePool('HULL_MULT:6:4; UNKNOWN:1:2:0'),
          suffixes: parsePool('HULL_MULT:5/10/15'),
        }),
      ],
      vocabulary,
    );
    expect(issues).toEqual([
      'Bad Id: id must use only lowercase letters, digits and underscores',
      'Bad Id: kind must be one of subroutine, bridge',
      'Bad Id: icon graphics/unused/x.png is in a folder that is not deployed',
      'Bad Id: rarity must be zero or more',
      'Bad Id: unknown drop condition mystery',
      'Bad Id: HULL_MULT has a min above its max',
      'Bad Id: unknown effect UNKNOWN',
      'Bad Id: UNKNOWN needs a weight above zero',
      'Bad Id: HULL_MULT is listed more than once across its prefixes and suffixes',
      'Bad Id: HULL_MULT needs exactly four hull size values',
    ]);
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
  it('lists effects used by non-unique socketables that have no names yet', () => {
    const model = {
      affixes: [{ cells: [], effect: 'HULL_MULT', prefix: 'Hard', suffix: '' }],
      usedEffects: { HULL_MULT: ['a'], ARMOR_FLAT: ['a', 'b'] },
    };
    expect(missingEffects(model)).toEqual(['ARMOR_FLAT']);
    expect(
      validateNames({ ...model, affixTable: { header: [], rows: [], lineEnding: '\n' }, rareNames: { a: { first: ['X'] } }, rareNamesLineEnding: '\n' }),
    ).toEqual([
      'HULL_MULT: used by socketables, so it needs both a prefix and a suffix',
      'ARMOR_FLAT: used by a, b but has no names',
      'a: needs at least one first word and one second word',
    ]);
  });

  it('splits word lists on new lines and commas', () => {
    expect(wordList('Alpha\n Beta, Gamma\n\n')).toEqual(['Alpha', 'Beta', 'Gamma']);
  });
});
