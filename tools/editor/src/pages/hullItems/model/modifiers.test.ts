import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseHullItemsCsv } from './definitions.ts';
import { parseImplicitsCsv } from './implicits.ts';
import { modifierUsage, referenceIssues, renameModifierReferences } from './modifierUsage.ts';
import {
  formatModifierLines,
  modifierById,
  parseModifierLines,
  parseModifiersCsv,
  serializeModifiersCsv,
  suggestedModifierId,
  togglePerHullSize,
  validateModifiers,
} from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [
    { name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false },
    { name: 'ARMOR_FLAT', statMode: 'FLAT', lowerIsBetter: false },
    { name: 'ARMOR_PERCENT', statMode: 'PERCENT', lowerIsBetter: false },
  ],
  tags: { theme: [], region: [], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  socketableKinds: ['subroutine', 'armor_plating'],
  socketableUnlocks: [],
});

const HULL_ITEMS_CSV =
  'id,kind,name,icon,rarity,unique,description,prefixes,suffixes,unlock\n' +
  'chip,subroutine,Chip,,10,,,@HULL_MULT:5,@ARMOR_FLAT,\n' +
  'relic,subroutine,Relic,,1,true,,@UNNAMED_PERCENT,,\n';
const IMPLICITS_CSV = 'type,implicits\narmor_plating,@HYBRID_ARMOR_HULL\n';

describe('hull item modifiers', () => {
  it('parses single, per-hull-size and hybrid lines and formats them back', () => {
    const lines = parseModifierLines('ARMOR_PERCENT:5:10; ARMOR_FLAT:10/15/20/30');
    expect(lines).toEqual([
      { effect: 'ARMOR_PERCENT', min: '5', max: '10' },
      { effect: 'ARMOR_FLAT', min: '', max: '', hullValues: ['10', '15', '20', '30'] },
    ]);
    expect(formatModifierLines(lines)).toBe('ARMOR_PERCENT:5:10; ARMOR_FLAT:10/15/20/30');
    expect(formatModifierLines(parseModifierLines('HULL_MULT:1:2:50'))).toBe('HULL_MULT:1:2:50');
  });

  it('round-trips a file and keeps unknown columns', () => {
    const text = 'id,slot,lines,name,notes\nHULL_MULT,prefix,HULL_MULT:4:6,Reinforced,keep me\n';
    expect(serializeModifiersCsv(parseModifiersCsv(text))).toBe(text);
  });

  it('suggests ids that follow the naming convention', () => {
    expect(suggestedModifierId(parseModifierLines('HULL_MULT:1:2'))).toBe('HULL_MULT');
    expect(suggestedModifierId(parseModifierLines('HULL_MULT:1:2'), 'reactor')).toBe('HULL_MULT_REACTOR');
    expect(suggestedModifierId(parseModifierLines('ARMOR_PERCENT:1:2; HULL_MULT:1:2'))).toBe('HYBRID_ARMOR_HULL');
  });

  it('switches a line to per-hull-size values and back to the original range', () => {
    const [line] = parseModifierLines('HULL_MULT:4:6');
    togglePerHullSize(line, true);
    expect(line.hullValues).toEqual(['4', '4', '4', '4']);
    togglePerHullSize(line, false);
    expect(line).toEqual({ effect: 'HULL_MULT', min: '4', max: '6' });
  });

  it('reports every rule the game would reject', () => {
    const modifiers = parseModifiersCsv(
      'id,slot,lines,name\n' +
        'HULL_MULT,prefix,HULL_MULT:4:6,Reinforced\n' +
        'HULL_MULT,prefix,HULL_MULT:1:2,Again\n' +
        'bad id,sideways,,\n' +
        'STURDY,suffix,HULL_MULT:6:4:5,Sturdy\n' +
        'ARMOR_AND_HULL,implicit,ARMOR_FLAT:1/2/3/4; ARMOR_FLAT:1:2,\n' +
        'ARMOR_FLAT,suffix,ARMOR_FLAT:1/2/3,\n' +
        'UNNAMED_PERCENT,prefix,UNNAMED_PERCENT:1:2,\n',
    ).modifiers;
    const usage = modifierUsage(parseHullItemsCsv(HULL_ITEMS_CSV).definitions, []);

    expect(validateModifiers(modifiers, vocabulary, usage)).toEqual([
      'HULL_MULT: id is used more than once',
      'bad id: id must use only letters, digits and underscores',
      'bad id: slot must be one of implicit, prefix, suffix',
      'bad id: it has no lines',
      'STURDY: HULL_MULT has a weight; weights belong on the pool entry',
      'STURDY: HULL_MULT has a min above its max',
      'STURDY: has one effect, so its id must be HULL_MULT or HULL_MULT_<QUALIFIER>',
      'ARMOR_AND_HULL: ARMOR_FLAT is on an implicit, so it must roll a min-max range',
      'ARMOR_AND_HULL: ARMOR_FLAT is listed more than once',
      'ARMOR_AND_HULL: has several effects, so its id must start with HYBRID_',
      'ARMOR_FLAT: ARMOR_FLAT needs exactly four hull size values',
      'ARMOR_FLAT: chip can roll it, so it needs a name for their common names',
      'UNNAMED_PERCENT: unknown effect UNNAMED_PERCENT',
    ]);
  });

  it('finds every use and renames references in pools and implicits', () => {
    const definitions = parseHullItemsCsv(HULL_ITEMS_CSV).definitions;
    const implicitRows = parseImplicitsCsv(IMPLICITS_CSV, vocabulary).rows;
    const usage = modifierUsage(definitions, implicitRows);

    expect(usage.get('HULL_MULT')).toEqual({ hullItems: ['chip'], nonUniqueHullItems: ['chip'], implicitTypes: [] });
    expect(usage.get('UNNAMED_PERCENT')).toEqual({ hullItems: ['relic'], nonUniqueHullItems: [], implicitTypes: [] });
    expect(usage.get('HYBRID_ARMOR_HULL')).toEqual({ hullItems: [], nonUniqueHullItems: [], implicitTypes: ['armor_plating'] });

    expect(renameModifierReferences(definitions, implicitRows, 'HYBRID_ARMOR_HULL', 'HYBRID_ARMOR_HULL_PLATING')).toBe(1);
    expect(renameModifierReferences(definitions, implicitRows, 'HULL_MULT', 'HULL_MULT_CHIP')).toBe(1);
    expect(definitions[0].prefixes).toEqual([{ modifierId: 'HULL_MULT_CHIP', weight: '5' }]);
    expect(implicitRows[0].implicits).toEqual([{ modifierId: 'HYBRID_ARMOR_HULL_PLATING' }]);
  });

  it('flags references that a modifier edit breaks', () => {
    const definitions = parseHullItemsCsv(HULL_ITEMS_CSV).definitions;
    const implicitRows = parseImplicitsCsv(IMPLICITS_CSV, vocabulary).rows;
    const catalogue = modifierById(
      parseModifiersCsv('id,slot,lines,name\nHULL_MULT,suffix,HULL_MULT:4:6,Reinforced\nARMOR_FLAT,suffix,ARMOR_FLAT:1:2,Plating\n').modifiers,
    );

    expect(referenceIssues(definitions, implicitRows, catalogue)).toEqual([
      'chip lists HULL_MULT as a prefix, but it is a suffix',
      'relic lists UNNAMED_PERCENT, which is not a modifier',
      'armor_plating implicits list HYBRID_ARMOR_HULL, which is not a modifier',
    ]);
  });
});
