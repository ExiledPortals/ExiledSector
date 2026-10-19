import { describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../../api/vocabulary.ts';
import {
  anchorVolumes,
  exitCandidates,
  isKnownAnchorUnlock,
  markAnchorsSaved,
  missingVolumes,
  newWormholeAnchor,
  parseWormholeAnchorsCsv,
  serializeWormholeAnchorsCsv,
  treeNodeFacts,
  validateWormholeAnchors,
  type WormholeAnchor,
} from './anchors.ts';
import { shipItemTypes, validateModifiers, type ItemModifier } from './modifiers.ts';

const vocabulary = buildVocabulary({
  effects: [{ name: 'HULL_MULT', statMode: 'MULT', lowerIsBetter: false }],
  tags: { theme: [], region: ['core', 'hegemony', 'pirate', 'exotech'], requirement: [] },
  tiers: [],
  unlockConditionTypes: [],
  blueprintCategories: [],
  itemTypes: ['subroutine', 'hull_item', 'star_chart', 'wormhole_anchor'],
  itemKinds: [{ id: 'bridge', type: 'hull_item' }],
  itemUnlocks: ['rat_player_exoship'],
});

const TYPES = JSON.stringify({
  skillTypes: [
    { id: 'wormhole', tier: 'WORMHOLE' },
    { id: 'wormhole_exit', tier: 'WORMHOLE_EXIT' },
    { id: 'exoship', tier: 'WORMHOLE_EXIT' },
    { id: 'armor', tier: 'NOTABLE' },
    { id: 'small_thing' },
  ],
});
const TREE = JSON.stringify({
  nodes: [
    { id: 'hegemony_2', type: 'wormhole_exit', tags: ['hegemony'] },
    { id: 'hegemony_shortcut', type: 'wormhole', tags: ['hegemony'] },
    { id: 'hegemony_notable', type: 'armor', tags: ['hegemony'] },
    { id: 'hegemony_small', type: 'small_thing', tags: ['hegemony'] },
    { id: 'pirate_wormhole_2', type: 'wormhole_exit', tags: ['pirate'] },
    { id: 'exoship', type: 'exoship', tags: ['exotech'] },
    { id: 'core_wormhole', type: 'wormhole', tags: ['core'] },
  ],
});
const nodeFacts = treeNodeFacts(TREE, TYPES, vocabulary);

const CSV =
  'id,volume,exit_node,name,icon,color,crest_faction,rarity,description,unlock,drop_factions,drop_chance\r\n' +
  'wormhole_anchor_hegemony,hegemony,hegemony_2,Hegemony Corridor Lock,graphics/icons/nav_relay.png,#F5961E,hegemony,10,"A lock, with a comma.",,,0.06\r\n' +
  'wormhole_anchor_exotech,exotech,exoship,Hyper-Nav Beacon,graphics/icons/rat_exogrid.png,,,10,A beacon.,rat_player_exoship,rat_exotech;other_exotech,0.02\r\n';

function anchor(overrides: Partial<WormholeAnchor>): WormholeAnchor {
  return {
    cells: [],
    id: 'wormhole_anchor_pirate',
    volume: 'pirate',
    exitNode: 'pirate_wormhole_2',
    name: 'Raider Run Lock',
    icon: 'graphics/icons/nav_relay.png',
    color: '',
    crestFaction: '',
    rarity: '10',
    description: '',
    unlock: '',
    dropFactions: '',
    dropChance: '0.06',
    ...overrides,
  };
}

describe('wormhole anchors', () => {
  it('parses every column and writes the file back byte for byte', () => {
    const model = parseWormholeAnchorsCsv(CSV);

    expect(model.anchors.map((parsed) => [parsed.id, parsed.volume, parsed.exitNode, parsed.unlock, parsed.dropFactions, parsed.dropChance])).toEqual([
      ['wormhole_anchor_hegemony', 'hegemony', 'hegemony_2', '', '', '0.06'],
      ['wormhole_anchor_exotech', 'exotech', 'exoship', 'rat_player_exoship', 'rat_exotech;other_exotech', '0.02'],
    ]);
    expect(model.anchors[0].description).toBe('A lock, with a comma.');
    expect([model.anchors[0].color, model.anchors[0].crestFaction]).toEqual(['#F5961E', 'hegemony']);
    expect([model.anchors[1].color, model.anchors[1].crestFaction]).toEqual(['', '']);
    expect(serializeWormholeAnchorsCsv(model)).toBe(CSV);
  });

  it('keeps columns it does not know and writes edits into their own cells', () => {
    const model = parseWormholeAnchorsCsv(
      'id,volume,exit_node,name,icon,color,crest_faction,rarity,description,unlock,drop_factions,drop_chance,note\nold,pirate,pirate_wormhole_2,Old,,,,1,,,,0.06,keep me\n',
    );
    model.anchors[0].exitNode = 'pirate_wormhole_3';

    expect(serializeWormholeAnchorsCsv(model)).toBe(
      'id,volume,exit_node,name,icon,color,crest_faction,rarity,description,unlock,drop_factions,drop_chance,note\nold,pirate,pirate_wormhole_3,Old,,,,1,,,,0.06,keep me\n',
    );
  });

  it('offers only the wormhole exit nodes of the volume as exits, and every region but the core as a volume', () => {
    expect(anchorVolumes(vocabulary)).toEqual(['hegemony', 'pirate', 'exotech']);
    expect(exitCandidates(nodeFacts, 'hegemony')).toEqual(['hegemony_2']);
    expect(exitCandidates(nodeFacts, 'exotech')).toEqual(['exoship']);
    expect(nodeFacts.hegemony_small).toEqual({ region: 'hegemony', tier: 'SMALL' });
  });

  it('a new anchor takes the first volume without one and its first exit', () => {
    const anchors = parseWormholeAnchorsCsv(CSV).anchors;

    expect(newWormholeAnchor(anchors, vocabulary, nodeFacts)).toMatchObject({
      id: 'wormhole_anchor_pirate',
      volume: 'pirate',
      exitNode: 'pirate_wormhole_2',
      dropFactions: '',
      dropChance: '0.06',
    });
    expect(newWormholeAnchor(anchors, vocabulary, nodeFacts, ['wormhole_anchor_pirate']).id).toBe('wormhole_anchor_pirate_2');
    expect(missingVolumes(anchors, vocabulary)).toEqual(['pirate']);
  });

  it('accepts the sample anchors', () => {
    expect(validateWormholeAnchors(parseWormholeAnchorsCsv(CSV).anchors, vocabulary, nodeFacts)).toEqual([]);
  });

  it('accepts every unlock the game understands, cleared chart tiers included', () => {
    expect(isKnownAnchorUnlock('', vocabulary)).toBe(true);
    expect(isKnownAnchorUnlock('rat_player_exoship', vocabulary)).toBe(true);
    expect(isKnownAnchorUnlock('cleared_chart_tier_3', vocabulary)).toBe(true);
    expect(isKnownAnchorUnlock('cleared_chart_tier_16', vocabulary)).toBe(false);
    expect(isKnownAnchorUnlock('made_up', vocabulary)).toBe(false);
  });

  it('reports bad volumes, exits, ids, rarities and unlocks', () => {
    const issues = validateWormholeAnchors(
      [
        anchor({}),
        anchor({ id: 'wormhole_anchor_pirate', volume: 'pirate' }),
        anchor({ id: 'core_anchor', volume: 'core', exitNode: 'core_wormhole' }),
        anchor({ id: 'wrong_exit', volume: 'hegemony', exitNode: 'pirate_wormhole_2' }),
        anchor({ id: 'notable_exit', volume: 'exotech', exitNode: 'hegemony_notable' }),
        anchor({ id: 'missing_exit', volume: 'exotech', exitNode: 'nowhere' }),
        anchor({ id: 'Bad Id', volume: 'exotech', exitNode: '' }),
        anchor({ id: 'taken', volume: 'exotech', rarity: '-1', unlock: 'made_up' }),
        anchor({ id: 'shortcut_exit', volume: 'hegemony', exitNode: 'hegemony_shortcut' }),
        anchor({ id: 'bad_drops', volume: 'exotech', exitNode: 'exoship', dropChance: 'often', dropFactions: 'rat exotech;;pirates' }),
      ],
      vocabulary,
      nodeFacts,
      ['taken'],
    );

    expect(issues).toEqual([
      'wormhole_anchor_pirate: id is used more than once',
      'wormhole_anchor_pirate: pirate already has an anchor; each volume has exactly one',
      'core_anchor: volume must be one of hegemony, pirate, exotech',
      'core_anchor: exit node core_wormhole is a WORMHOLE node, not a wormhole exit',
      'wrong_exit: exit node pirate_wormhole_2 is in pirate, not hegemony',
      'notable_exit: exit node hegemony_notable is in hegemony, not exotech',
      'notable_exit: exit node hegemony_notable is a NOTABLE node, not a wormhole exit',
      'missing_exit: exotech already has an anchor; each volume has exactly one',
      'missing_exit: exit node nowhere is not in the skill tree',
      'Bad Id: id must use only lowercase letters, digits and underscores',
      'Bad Id: exotech already has an anchor; each volume has exactly one',
      'Bad Id: pick an exit node',
      'taken: id is already used by a hull item, subroutine or star chart',
      'taken: exotech already has an anchor; each volume has exactly one',
      'taken: exit node pirate_wormhole_2 is in pirate, not exotech',
      'taken: rarity must be zero or more',
      'taken: unknown unlock condition made_up',
      'shortcut_exit: hegemony already has an anchor; each volume has exactly one',
      'shortcut_exit: exit node hegemony_shortcut is a WORMHOLE node, not a wormhole exit',
      'bad_drops: exotech already has an anchor; each volume has exactly one',
      'bad_drops: drop chance must be a number of 0 or more',
      'bad_drops: drop factions must be faction ids separated by ;',
    ]);
  });

  it('reports a colour that is not #RRGGBB and a crest faction that is not one faction id', () => {
    const issues = validateWormholeAnchors(
      [
        anchor({ color: '#f5961e', crestFaction: 'pirates' }),
        anchor({ id: 'wormhole_anchor_hegemony', volume: 'hegemony', exitNode: 'hegemony_2', color: 'orange', crestFaction: 'pirates;hegemony' }),
      ],
      vocabulary,
      nodeFacts,
    );

    expect(issues).toEqual([
      'wormhole_anchor_hegemony: color must be #RRGGBB or blank',
      'wormhole_anchor_hegemony: crest faction must be a single faction id',
    ]);
  });

  it('remembers the saved id so a later rename is recognised', () => {
    const model = parseWormholeAnchorsCsv(CSV);
    model.anchors.push(anchor({}));
    const saved = structuredClone(model);

    markAnchorsSaved(model, saved);

    expect(model.anchors.map((marked) => marked.loadedId)).toEqual(['wormhole_anchor_hegemony', 'wormhole_anchor_exotech', 'wormhole_anchor_pirate']);
  });

  it('keeps anchors out of the modifier item types', () => {
    const modifier: ItemModifier = {
      cells: [],
      id: 'HULL_MULT',
      slot: 'prefix',
      itemTypes: ['wormhole_anchor'],
      lines: [{ effect: 'HULL_MULT', min: '1', max: '2' }],
      name: 'Sturdy',
    };

    expect(shipItemTypes(vocabulary)).toEqual(['hull_item']);
    expect(validateModifiers([modifier], vocabulary, new Map())).toContain(
      'HULL_MULT: wormhole anchors do not roll modifiers, so remove wormhole_anchor from its item types',
    );
  });
});
