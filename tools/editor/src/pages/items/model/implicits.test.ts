import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import {
  HULL_ITEM_CORRUPTION_POOL,
  parseCorruptionPoolCsv,
  parseImplicitsCsv,
  serializeCorruptionPoolCsv,
  serializeImplicitsCsv,
} from './implicits.ts';
import { isUsed, modifierUsage, referenceIssues, renameModifierReferences, usedByText } from './modifierUsage.ts';
import { modifierById, parseModifiersCsv, validateModifiers } from './modifiers.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');
const implicitsFile = path.join(repositoryRoot, 'data', 'config', 'exiledSector', 'hull_item_implicits.csv');
const corruptionPoolFile = path.join(repositoryRoot, 'data', 'config', 'exiledSector', 'subroutine_corruption_implicits.csv');
const hullItemCorruptionPoolFile = path.join(repositoryRoot, 'data', 'config', 'exiledSector', 'hull_item_corruption_implicits.csv');

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
});

describe('subroutine corruption pool', () => {
  it('saving the unchanged file reproduces it byte for byte', () => {
    const text = fs.readFileSync(corruptionPoolFile, 'utf8');
    expect(serializeCorruptionPoolCsv(parseCorruptionPoolCsv(text))).toBe(text);
  });

  it('keeps a row listing several references as written instead of dropping any', () => {
    const text = 'implicit\n@FLUX_CAPACITY_PERCENT_IMPLICIT; @ARMOR_PERCENT_IMPLICIT\n';
    const model = parseCorruptionPoolCsv(text);

    expect(serializeCorruptionPoolCsv(model)).toBe(text);
  });

  it('counts pool entries as uses, renames them and reports dangling references', () => {
    const pool = parseCorruptionPoolCsv('implicit\n@FLUX_CAPACITY_PERCENT_IMPLICIT\n');
    const usage = modifierUsage([], [], 'hull_item', [], [pool]);

    expect(isUsed(usage.get('FLUX_CAPACITY_PERCENT_IMPLICIT'))).toBe(true);
    expect(usedByText(usage.get('FLUX_CAPACITY_PERCENT_IMPLICIT'))).toBe('Subroutine corruption pool');
    expect(usage.get('FLUX_CAPACITY_PERCENT_IMPLICIT')?.itemTypes).toEqual(['subroutine']);
    const scopedToHullItems = parseModifiersCsv('id,slot,item_types,lines,name\nFLUX_CAPACITY_PERCENT_IMPLICIT,implicit,hull_item,FLUX_CAPACITY_PERCENT:1:2,\n');
    expect(validateModifiers(scopedToHullItems.modifiers, vocabulary, usage)).toEqual([
      'FLUX_CAPACITY_PERCENT_IMPLICIT: a subroutine rolls it, so item types must include subroutine',
    ]);

    expect(renameModifierReferences([], [], 'FLUX_CAPACITY_PERCENT_IMPLICIT', 'FLUX_CAPACITY_PERCENT_SUBROUTINE_IMPLICIT', [], [pool])).toBe(1);
    expect(pool.entries.map((entry) => entry.implicit)).toEqual([{ modifierId: 'FLUX_CAPACITY_PERCENT_SUBROUTINE_IMPLICIT' }]);
    expect(referenceIssues([], [], catalogue, [], [pool])).toEqual([
      'The subroutine corruption pool lists FLUX_CAPACITY_PERCENT_SUBROUTINE_IMPLICIT, which is not a modifier',
    ]);
  });
});

describe('hull item corruption pool', () => {
  it('saving the unchanged file reproduces it byte for byte', () => {
    const text = fs.readFileSync(hullItemCorruptionPoolFile, 'utf8');
    expect(serializeCorruptionPoolCsv(parseCorruptionPoolCsv(text, HULL_ITEM_CORRUPTION_POOL))).toBe(text);
  });

  it('counts each pool by name and as a use by its item type', () => {
    const subroutinePool = parseCorruptionPoolCsv('implicit\n@ARMOR_PERCENT_IMPLICIT\n');
    const hullItemPool = parseCorruptionPoolCsv('implicit\n@ARMOR_PERCENT_IMPLICIT\n', HULL_ITEM_CORRUPTION_POOL);
    const usage = modifierUsage([], [], 'hull_item', [], [subroutinePool, hullItemPool]);

    expect(usedByText(usage.get('ARMOR_PERCENT_IMPLICIT'))).toBe('Subroutine corruption pool, Hull item corruption pool');
    expect(usage.get('ARMOR_PERCENT_IMPLICIT')?.itemTypes).toEqual(['subroutine', 'hull_item']);
    expect(renameModifierReferences([], [], 'ARMOR_PERCENT_IMPLICIT', 'ARMOR_PERCENT_CORRUPTION_IMPLICIT', [], [subroutinePool, hullItemPool])).toBe(2);
    expect(referenceIssues([], [], catalogue, [], [hullItemPool])).toEqual([
      'The hull item corruption pool lists ARMOR_PERCENT_CORRUPTION_IMPLICIT, which is not a modifier',
    ]);
  });
});
