import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import {
  doctrineTypeFacts,
  joinEntries,
  listEntries,
  markDoctrinesSaved,
  newDoctrineArchive,
  parseDoctrineArchivesCsv,
  replacementCandidates,
  serializeDoctrineArchivesCsv,
  uncoveredDoctrines,
  validateDoctrineArchives,
  type DoctrineArchive,
} from './doctrines.ts';
import { isUsed, modifierUsage, referenceIssues, renameModifierReferences } from './modifierUsage.ts';
import { modifierById, validateModifiers, type ItemModifier } from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [{ name: 'ARMOR_PERCENT', statMode: 'PERCENT', lowerIsBetter: false }],
  tags: { theme: [], region: ['core'], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['subroutine', 'hull_item', 'star_chart', 'wormhole_anchor'],
  itemKinds: [{ id: 'bridge', type: 'hull_item' }],
  modifierScopes: ['subroutine', 'unique_subroutine', 'doctrine', 'hull_item', 'star_chart', 'wormhole_anchor'],
  doctrineBehaviours: ['TRANSFORM', 'AUGMENT'],
  radiusSizes: ['small', 'medium', 'large'],
  itemUnlocks: [],
});

const TYPES = JSON.stringify({
  skillTypes: [
    { id: 'notable_a', tier: 'NOTABLE' },
    { id: 'notable_b', tier: 'NOTABLE' },
    { id: 'optional_notable', tier: 'NOTABLE', optionalOptions: ['notable_a'] },
    { id: 'keystone_a', tier: 'KEYSTONE' },
    { id: 'small_a' },
  ],
});

function modifier(id: string, itemTypes: string[], slot = 'prefix'): ItemModifier {
  return { cells: [], id, slot, itemTypes, lines: [{ effect: 'ARMOR_PERCENT', min: '2', max: '4' }], name: '' };
}

const MODIFIERS = [modifier('ARMOR_PERCENT_DOCTRINE', ['doctrine']), modifier('ARMOR_PERCENT_PLATE', ['hull_item'])];
const DOCTRINES = ['doc_transform', 'doc_augment', 'doc_missing'];
const CSV = [
  'id,behaviour,radius,faction,notables,keystones,small_nodes,modifiers,min_lines,max_lines',
  'doc_transform,TRANSFORM,large,tritachyon,notable_a; notable_b,keystone_a,small_a,,1,1',
  'doc_augment,AUGMENT,medium,,,,,@ARMOR_PERCENT_DOCTRINE:2,1,2',
  '',
].join('\r\n');

function archives(): DoctrineArchive[] {
  return parseDoctrineArchivesCsv(CSV).archives;
}

describe('doctrine archives', () => {
  it('parses every column and saves an unchanged file byte for byte', () => {
    const model = parseDoctrineArchivesCsv(CSV);

    expect(model.archives.map((archive) => archive.id)).toEqual(['doc_transform', 'doc_augment']);
    expect(listEntries(model.archives[0].notables)).toEqual(['notable_a', 'notable_b']);
    expect(model.archives[0].smallNodes).toBe('small_a');
    expect(model.archives[1].modifiers).toBe('@ARMOR_PERCENT_DOCTRINE:2');
    expect(serializeDoctrineArchivesCsv(model)).toBe(CSV);
    expect(joinEntries(['a', 'b'])).toBe('a; b');
  });

  it('accepts valid rows and warns about doctrine subroutines without one', () => {
    const facts = doctrineTypeFacts(TYPES);

    expect(validateDoctrineArchives(archives(), vocabulary, DOCTRINES, facts, MODIFIERS)).toEqual([]);
    expect(uncoveredDoctrines(archives(), DOCTRINES)).toEqual(['doc_missing']);
  });

  it('reports bad behaviours, radii, pools, modifiers and line counts', () => {
    const facts = doctrineTypeFacts(TYPES);
    const broken = archives();
    broken[0].radius = 'huge';
    broken[0].notables = 'keystone_a; missing; optional_notable';
    broken[0].modifiers = '@ARMOR_PERCENT_DOCTRINE';
    broken[1].behaviour = 'AUGMENT';
    broken[1].notables = 'notable_a';
    broken[1].modifiers = '@ARMOR_PERCENT_PLATE; ARMOR_PERCENT:1:2; @UNKNOWN';
    broken[1].minLines = '3';
    broken.push({ ...broken[1], id: 'not_a_doctrine', behaviour: 'SHUFFLE', cells: [] });

    const issues = validateDoctrineArchives(broken, vocabulary, DOCTRINES, facts, MODIFIERS);

    expect(issues).toContain('doc_transform: radius must be one of small, medium, large');
    expect(issues).toContain('doc_transform: keystone_a is listed as a NOTABLE replacement, but it is a KEYSTONE node');
    expect(issues).toContain('doc_transform: missing is not a skill type');
    expect(issues).toContain("doc_transform: optional_notable is optional or has an item cost, so it can't be a replacement");
    expect(issues).toContain('doc_transform: a transforming doctrine lists no modifiers');
    expect(issues).toContain('doc_augment: an augmenting doctrine lists no replacement nodes');
    expect(issues).toContain('doc_augment: ARMOR_PERCENT_PLATE does not list doctrine in its item types');
    expect(issues).toContain('doc_augment: ARMOR_PERCENT:1:2 is not @ID or @ID:weight');
    expect(issues).toContain('doc_augment: UNKNOWN is not in item_modifiers.csv');
    expect(issues).toContain('doc_augment: min lines must be 1 or more and max lines at least min lines');
    expect(issues).toContain('not_a_doctrine: id must be a DOCTRINE subroutine in subroutines.csv');
    expect(issues).toContain('not_a_doctrine: behaviour must be one of TRANSFORM, AUGMENT');
  });

  it('offers only plain replacements of the right tier', () => {
    const facts = doctrineTypeFacts(TYPES);

    expect(replacementCandidates(facts, 'NOTABLE')).toEqual(['notable_a', 'notable_b']);
    expect(replacementCandidates(facts, 'SMALL')).toEqual(['small_a']);
  });

  it('a new row takes the first doctrine without a row, and saving keeps the loaded id', () => {
    const current = archives();
    const added = newDoctrineArchive(current, DOCTRINES, vocabulary);

    expect(added.id).toBe('doc_missing');
    expect(added.behaviour).toBe('TRANSFORM');
    expect(added.radius).toBe('medium');
    current.push(added);
    markDoctrinesSaved({ archives: current }, { archives: current.map((archive) => ({ ...archive })) });
    expect(current[2].loadedId).toBe('doc_missing');
  });

  it('a doctrine modifier must be a prefix', () => {
    const issues = validateModifiers([modifier('ARMOR_PERCENT_DOCTRINE', ['doctrine'], 'suffix')], vocabulary, new Map());

    expect(issues).toContain('ARMOR_PERCENT_DOCTRINE: doctrine lines are prefixes, so a modifier for doctrine must be a prefix');
  });

  it('counts doctrine archives as users of their modifiers', () => {
    const usage = modifierUsage([], [], 'hull_item', archives());

    expect(usage.get('ARMOR_PERCENT_DOCTRINE')?.doctrineArchives).toEqual(['doc_augment']);
    expect(usage.get('ARMOR_PERCENT_DOCTRINE')?.itemTypes).toEqual(['doctrine']);
    expect(isUsed(usage.get('ARMOR_PERCENT_DOCTRINE'))).toBe(true);
    expect(isUsed(usage.get('ARMOR_PERCENT_PLATE'))).toBe(false);
  });

  it('renames doctrine modifier references and keeps their weights', () => {
    const current = archives();

    expect(renameModifierReferences([], [], 'ARMOR_PERCENT_DOCTRINE', 'ARMOR_PERCENT_RENAMED', current)).toBe(1);
    expect(current[1].modifiers).toBe('@ARMOR_PERCENT_RENAMED:2');
    expect(current[0].modifiers).toBe('');
  });

  it('flags a doctrine reference to a missing, non-prefix or unscoped modifier', () => {
    const broken = archives();
    broken[1].modifiers = '@GONE; @ARMOR_PERCENT_PLATE; @SUFFIX_DOCTRINE';
    const catalogue = modifierById([...MODIFIERS, modifier('SUFFIX_DOCTRINE', ['doctrine'], 'suffix')]);

    expect(referenceIssues([], [], catalogue, broken)).toEqual([
      'doc_augment in doctrine_archives.csv lists GONE, which is not a modifier',
      'doc_augment in doctrine_archives.csv lists ARMOR_PERCENT_PLATE, which does not list doctrine in its item types',
      'doc_augment in doctrine_archives.csv lists SUFFIX_DOCTRINE, but doctrine lines are prefixes and it is a suffix',
    ]);
  });
});
