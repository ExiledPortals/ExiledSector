import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseImplicitsCsv, serializeImplicitsCsv, validateImplicits } from './implicits.ts';
import { modifierById, parseModifiersCsv } from './modifiers.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');
const implicitsFile = path.join(repositoryRoot, 'data', 'config', 'exiledSector', 'hull_item_implicits.csv');

const vocabulary = buildVocabulary({
  effects: [
    { name: 'ARMOR_PERCENT', statMode: 'PERCENT', lowerIsBetter: false },
    { name: 'FLUX_CAPACITY_PERCENT', statMode: 'PERCENT', lowerIsBetter: false },
  ],
  tags: { theme: [], region: [], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['subroutine', 'hull_item'],
  itemKinds: [
    { id: 'subroutine', type: 'subroutine' },
    { id: 'armor_plating', type: 'hull_item' },
    { id: 'shield_generator', type: 'hull_item' },
  ],
  itemUnlocks: [],
});

const catalogue = modifierById(
  parseModifiersCsv(
    'id,slot,item_types,lines,name\n' +
      'ARMOR_PERCENT_IMPLICIT,implicit,hull_item,ARMOR_PERCENT:5:10,\n' +
      'HYBRID_ARMOR_FLUX_CAPACITY,implicit,hull_item,ARMOR_PERCENT:1:2; FLUX_CAPACITY_PERCENT:1:2,\n' +
      'FLUX_CAPACITY_PERCENT_IMPLICIT,implicit,subroutine,FLUX_CAPACITY_PERCENT:1:2,\n' +
      'ARMOR_PERCENT,prefix,subroutine;hull_item,ARMOR_PERCENT:1:2,Plated\n',
  ).modifiers,
);

describe('hull item implicits', () => {
  it('saving the unchanged file reproduces it byte for byte', () => {
    const text = fs.readFileSync(implicitsFile, 'utf8');
    const realVocabulary = buildVocabulary(JSON.parse(fs.readFileSync(path.join(repositoryRoot, 'tools', 'editor_vocabulary.json'), 'utf8')));
    expect(serializeImplicitsCsv(parseImplicitsCsv(text, realVocabulary))).toBe(text);
  });

  it('offers a row for every ship section but never for subroutines, and saves only rows with implicits', () => {
    const model = parseImplicitsCsv('section,implicits\narmor_plating,@ARMOR_PERCENT_IMPLICIT\n', vocabulary);

    expect(model.rows.map((row) => row.section)).toEqual(['armor_plating', 'shield_generator']);
    expect(serializeImplicitsCsv(model)).toBe('section,implicits\narmor_plating,@ARMOR_PERCENT_IMPLICIT\n');
  });

  it('rejects subroutines, inline entries, missing or non-implicit modifiers, other item types and shared effects', () => {
    const model = parseImplicitsCsv(
      'section,implicits\n' +
        'subroutine,@ARMOR_PERCENT_IMPLICIT\n' +
        'armor_plating,ARMOR_PERCENT:1:2; @NOT_THERE; @ARMOR_PERCENT; @FLUX_CAPACITY_PERCENT_IMPLICIT\n' +
        'shield_generator,@ARMOR_PERCENT_IMPLICIT; @HYBRID_ARMOR_FLUX_CAPACITY\n',
      vocabulary,
    );

    expect(validateImplicits(model, vocabulary, catalogue)).toEqual([
      'subroutine: section must be one of armor_plating, shield_generator',
      'armor_plating: "ARMOR_PERCENT:1:2" is written inline; pick an implicit modifier instead',
      'armor_plating: NOT_THERE is not in item_modifiers.csv',
      'armor_plating: ARMOR_PERCENT is a prefix, not an implicit',
      'armor_plating: FLUX_CAPACITY_PERCENT_IMPLICIT does not roll on hull items',
      'shield_generator: ARMOR_PERCENT_IMPLICIT and HYBRID_ARMOR_FLUX_CAPACITY share an effect',
    ]);
  });

  it('accepts a clean file', () => {
    const model = parseImplicitsCsv('section,implicits\nshield_generator,@HYBRID_ARMOR_FLUX_CAPACITY\n', vocabulary);

    expect(validateImplicits(model, vocabulary, catalogue)).toEqual([]);
  });
});
