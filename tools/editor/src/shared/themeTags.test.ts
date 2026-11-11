import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../api/vocabulary.ts';
import { parseModifiersCsv, TAGLESS, validateModifiers } from '../pages/items/model/modifiers.ts';
import { readRepositoryFile } from '../test/testServer.ts';
import { ancestorsListedBeside, ancestorsOf, themeTagIssues, themeTagsFor } from './themeTags.ts';

const vocabulary = buildVocabulary({
  effects: [
    { name: 'SHIELD_ARC_PERCENT', statMode: 'PERCENT', lowerIsBetter: false },
    { name: 'CARGO_PERCENT', statMode: 'PERCENT', lowerIsBetter: false },
  ],
  tags: { theme: ['defence', 'shield', 'front_shield', 'logistics'], volume: [], requirement: [] },
  themeTags: [
    { id: 'defence', parent: null, scopes: ['ship', 'star_chart'] },
    { id: 'shield', parent: 'defence', scopes: ['ship', 'star_chart'] },
    { id: 'front_shield', parent: 'shield', scopes: ['ship', 'star_chart'] },
    { id: 'logistics', parent: null, scopes: ['ship'] },
    { id: 'loot', parent: null, scopes: ['star_chart'] },
  ],
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['hull_item', 'star_chart'],
  itemKinds: [],
  itemUnlocks: [],
  chartEffects: ['CHART_ITEM_PERCENT'],
});

describe('theme tags', () => {
  it('lists the tags each scope may carry and walks a tag up to its root', () => {
    expect(themeTagsFor(vocabulary, 'ship')).toEqual(['defence', 'shield', 'front_shield', 'logistics']);
    expect(themeTagsFor(vocabulary, 'star_chart')).toEqual(['defence', 'shield', 'front_shield', 'loot']);
    expect(ancestorsOf(vocabulary, 'front_shield')).toEqual(['shield', 'defence']);
    expect(ancestorsOf(vocabulary, 'unknown')).toEqual([]);
    expect(ancestorsListedBeside(vocabulary, 'front_shield', ['defence', 'front_shield'])).toEqual(['defence']);
  });

  it('flags unknown tags, tags of the other scope and a tag written beside its own ancestor', () => {
    expect(themeTagIssues(vocabulary, 'ship', ['front_shield', 'logistics'])).toEqual([]);
    expect(themeTagIssues(vocabulary, 'ship', ['plating', 'loot', 'shield', 'front_shield'])).toEqual([
      'tag "plating" is not a theme tag',
      'tag "loot" is not a ship theme tag',
      'tag "front_shield" sits beside its ancestor "shield", which it already counts as',
    ]);
    expect(themeTagIssues(vocabulary, 'star_chart', ['logistics'])).toEqual(['tag "logistics" is not a star chart theme tag']);
  });

  it('keeps the ship-only theme list when the vocabulary file predates tag details', () => {
    const older = buildVocabulary({
      effects: [],
      tags: { theme: ['flux'], volume: [], requirement: [] },
      tiers: [],
      unlockConditionTypes: [],
      blueprintCategories: [],
      itemTypes: [],
      itemKinds: [],
      itemUnlocks: [],
    });

    expect(themeTagsFor(older, 'ship')).toEqual(['flux']);
    expect(themeTagsFor(older, 'star_chart')).toEqual([]);
  });

  it('checks modifier tags against the scope of their file and leaves tagless modifiers alone', () => {
    const shipModifiers = parseModifiersCsv(
      'id,affix,scopes,lines,name,tags\n' +
        'SHIELD_ARC_PERCENT,prefix,hull_item,SHIELD_ARC_PERCENT:1:2,Wide,shield;front_shield\n' +
        `CARGO_PERCENT,prefix,hull_item,CARGO_PERCENT:1:2,Roomy,${TAGLESS}\n`,
    ).modifiers;
    const chartModifiers = parseModifiersCsv(
      'id,affix,scopes,lines,name,tags\nCHART_ITEM_PERCENT,suffix,star_chart,CHART_ITEM_PERCENT:low=1/mid=2/high=3,Plunder,loot;logistics\n',
    ).modifiers;

    expect(validateModifiers(shipModifiers, vocabulary, new Map())).toEqual([
      'SHIELD_ARC_PERCENT: tag "front_shield" sits beside its ancestor "shield", which it already counts as',
    ]);
    expect(validateModifiers(chartModifiers, vocabulary, new Map(), 'chart')).toEqual([
      'CHART_ITEM_PERCENT: tag "logistics" is not a star chart theme tag',
    ]);
  });

  it('finds no tag issue on the shipped modifiers', () => {
    const shippedVocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));
    const files = [
      { file: 'data/config/exiledSector/item_modifiers.csv', scope: 'ship' as const },
      { file: 'data/config/exiledSector/star_chart_modifiers.csv', scope: 'star_chart' as const },
    ];
    for (const { file, scope } of files) {
      const issues = parseModifiersCsv(readRepositoryFile(file)).modifiers.flatMap((modifier) => {
        const tags = modifier.tags ?? [];
        return tags.includes(TAGLESS) ? [] : themeTagIssues(shippedVocabulary, scope, tags).map((issue) => `${modifier.id}: ${issue}`);
      });
      expect(issues, file).toEqual([]);
    }
  });
});
