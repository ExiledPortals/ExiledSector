import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import { parseImplicitsCsv, serializeImplicitsCsv, validateImplicits } from './implicits.ts';

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
  socketableKinds: ['subroutine', 'armor_plating', 'shield_generator'],
  socketableUnlocks: [],
});

describe('hull item implicits', () => {
  it('saving the unchanged file reproduces it byte for byte', () => {
    const text = fs.readFileSync(implicitsFile, 'utf8');
    const realVocabulary = buildVocabulary(
      JSON.parse(fs.readFileSync(path.join(repositoryRoot, 'tools', 'editor_vocabulary.json'), 'utf8')),
    );
    expect(serializeImplicitsCsv(parseImplicitsCsv(text, realVocabulary))).toBe(text);
  });

  it('offers a row for every hull socket type but never for subroutines, and saves only rows with implicits', () => {
    const model = parseImplicitsCsv('type,implicits\narmor_plating,ARMOR_PERCENT:5:10\n', vocabulary);

    expect(model.rows.map((row) => row.type)).toEqual(['armor_plating', 'shield_generator']);
    expect(serializeImplicitsCsv(model)).toBe('type,implicits\narmor_plating,ARMOR_PERCENT:5:10\n');
  });

  it('rejects subroutine implicits, unknown effects, per-hull-size values and weights', () => {
    const model = parseImplicitsCsv(
      'type,implicits\nsubroutine,ARMOR_PERCENT:1:2\narmor_plating,NOT_AN_EFFECT:1:2; FLUX_CAPACITY_PERCENT:1/2/3/4\nshield_generator,ARMOR_PERCENT:1:2:5\n',
      vocabulary,
    );
    const issues = validateImplicits(model, vocabulary);

    expect(issues).toContain('subroutine: subroutines never roll implicits');
    expect(issues).toContain('armor_plating: NOT_AN_EFFECT is not a skill effect');
    expect(issues).toContain('armor_plating: FLUX_CAPACITY_PERCENT must roll a min-max range, not per-hull-size values');
    expect(issues).toContain('shield_generator: ARMOR_PERCENT has a weight, but implicits always roll');
  });

  it('accepts a clean file', () => {
    const model = parseImplicitsCsv('type,implicits\nshield_generator,FLUX_CAPACITY_PERCENT:5:10; ARMOR_PERCENT:1:2\n', vocabulary);

    expect(validateImplicits(model, vocabulary)).toEqual([]);
  });
});
