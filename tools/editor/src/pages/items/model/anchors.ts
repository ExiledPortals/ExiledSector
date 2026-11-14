import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromColumns, LIST_SEPARATOR, parseCsvTable, readCsvRow, serializeCsvTable, splitList, type CsvColumn, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { uniqueSuffixedId } from '../../../shared/rowIds.ts';

export const WORMHOLE_ANCHOR_TYPE = 'wormhole_anchor';
export const WORMHOLE_ANCHOR_RESOURCE = 'wormhole-anchors';
export const CORE_REGION = 'core';
export const WORMHOLE_EXIT_TIER = 'WORMHOLE_EXIT';
export const DEFAULT_ANCHOR_ICON = 'graphics/icons/nav_relay.png';
export const DEFAULT_ANCHOR_DROP_CHANCE = '0.5';
export const DEFAULT_ANCHOR_PRICE = '100000';
export const ANCHOR_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;

export interface WormholeAnchor {
  cells: string[];
  id: string;
  volume: string;
  exitNode: string;
  name: string;
  icon: string;
  color: string;
  crestFaction: string;
  rarity: string;
  description: string;
  unlock: string;
  dropFactions: string;
  dropChance: string;
  guaranteedDrop: boolean;
  marketChance: string;
  price: string;
  loadedId?: string;
}

export interface WormholeAnchorsModel {
  table: CsvTable;
  anchors: WormholeAnchor[];
}

export interface TreeNodeFacts {
  region: string | null;
  tier: string;
}

const WORMHOLE_ANCHOR_COLUMNS: readonly CsvColumn<WormholeAnchor>[] = [
  { column: 'id', field: 'id' },
  { column: 'volume', field: 'volume' },
  { column: 'exit_node', field: 'exitNode' },
  { column: 'name', field: 'name' },
  { column: 'icon', field: 'icon' },
  { column: 'color', field: 'color' },
  { column: 'crest_faction', field: 'crestFaction' },
  { column: 'rarity', field: 'rarity' },
  { column: 'description', field: 'description' },
  { column: 'unlock', field: 'unlock' },
  { column: 'drop_factions', field: 'dropFactions' },
  { column: 'drop_chance', field: 'dropChance' },
  { column: 'market_chance', field: 'marketChance' },
  { column: 'price', field: 'price' },
  {
    column: 'guaranteed_drop',
    field: 'guaranteedDrop',
    read: (text) => text.toLowerCase() === 'true',
    write: (anchor) => (anchor.guaranteedDrop ? 'true' : ''),
    optional: true,
  },
];

export function parseWormholeAnchorsCsv(text: string): WormholeAnchorsModel {
  const table = parseCsvTable(text);
  const anchors = table.rows.map((cells): WormholeAnchor => {
    const anchor = { cells, ...readCsvRow(table, cells, WORMHOLE_ANCHOR_COLUMNS) } as WormholeAnchor;
    return { ...anchor, loadedId: anchor.id };
  });
  return { table, anchors };
}

export function serializeWormholeAnchorsCsv(model: WormholeAnchorsModel): string {
  return serializeCsvTable(csvTableFromColumns(model.table, model.anchors, WORMHOLE_ANCHOR_COLUMNS));
}

export function anchorVolumes(vocabulary: Vocabulary): string[] {
  return vocabulary.volumeTags.filter((region) => region !== CORE_REGION);
}

export function treeNodeFacts(treeText: string, typesText: string, vocabulary: Vocabulary): Record<string, TreeNodeFacts> {
  const types = (JSON.parse(typesText) as { skillTypes?: { id?: string; tier?: string }[] }).skillTypes ?? [];
  const tierByType = new Map(types.map((type) => [String(type.id ?? ''), String(type.tier ?? 'SMALL')]));
  const nodes = (JSON.parse(treeText) as { nodes?: { id?: string; type?: string; tags?: string[] }[] }).nodes ?? [];
  const facts: Record<string, TreeNodeFacts> = {};
  for (const node of nodes) {
    const region = (node.tags ?? []).find((tag) => vocabulary.volumeTags.includes(tag)) ?? null;
    facts[String(node.id ?? '')] = { region, tier: tierByType.get(String(node.type ?? '')) ?? 'SMALL' };
  }
  return facts;
}

export function exitCandidates(nodeFacts: Readonly<Record<string, TreeNodeFacts>>, volume: string): string[] {
  return Object.entries(nodeFacts)
    .filter(([, facts]) => facts.region === volume && facts.tier === WORMHOLE_EXIT_TIER)
    .map(([nodeId]) => nodeId)
    .sort((first, second) => first.localeCompare(second));
}

export function newWormholeAnchor(anchors: WormholeAnchor[], vocabulary: Vocabulary, nodeFacts: Readonly<Record<string, TreeNodeFacts>>, takenIds: readonly string[] = []): WormholeAnchor {
  const usedVolumes = new Set(anchors.map((anchor) => anchor.volume));
  const volume = anchorVolumes(vocabulary).find((candidate) => !usedVolumes.has(candidate)) ?? '';
  const base = volume ? `wormhole_anchor_${volume.toLowerCase()}` : 'wormhole_anchor_new';
  return {
    cells: [],
    id: uniqueSuffixedId(base, (id) => takenIds.includes(id) || anchors.some((anchor) => anchor.id === id)),
    volume,
    exitNode: exitCandidates(nodeFacts, volume)[0] ?? '',
    name: 'New Wormhole Anchor',
    icon: DEFAULT_ANCHOR_ICON,
    color: '',
    crestFaction: '',
    rarity: '10',
    description: '',
    unlock: '',
    dropFactions: '',
    dropChance: DEFAULT_ANCHOR_DROP_CHANCE,
    guaranteedDrop: false,
    marketChance: '',
    price: DEFAULT_ANCHOR_PRICE,
  };
}

export function isKnownAnchorUnlock(unlock: string, vocabulary: Vocabulary): boolean {
  if (unlock === '' || vocabulary.itemUnlocks.includes(unlock)) {
    return true;
  }
  if (!unlock.startsWith(vocabulary.clearedChartTierPrefix)) {
    return false;
  }
  const tierText = unlock.slice(vocabulary.clearedChartTierPrefix.length);
  const tier = /^\d+$/.test(tierText) ? Number.parseInt(tierText, 10) : Number.NaN;
  return tier >= vocabulary.lowestChartTier && tier <= vocabulary.highestChartTier;
}

function exitIssues(label: string, anchor: WormholeAnchor, nodeFacts: Readonly<Record<string, TreeNodeFacts>>): string[] {
  if (!anchor.exitNode) {
    return [`${label}: pick an exit node`];
  }
  const exit = Object.hasOwn(nodeFacts, anchor.exitNode) ? nodeFacts[anchor.exitNode] : undefined;
  if (!exit) {
    return [`${label}: exit node ${anchor.exitNode} is not in the skill tree`];
  }
  const issues: string[] = [];
  if (exit.region !== anchor.volume) {
    issues.push(`${label}: exit node ${anchor.exitNode} is in ${exit.region ?? 'no volume'}, not ${anchor.volume || 'the anchor volume'}`);
  }
  if (exit.tier !== WORMHOLE_EXIT_TIER) {
    issues.push(`${label}: exit node ${anchor.exitNode} is a ${exit.tier} node, not a wormhole exit`);
  }
  return issues;
}

export function validateWormholeAnchors(
  anchors: WormholeAnchor[],
  vocabulary: Vocabulary,
  nodeFacts: Readonly<Record<string, TreeNodeFacts>>,
  otherItemIds: readonly string[] = [],
): string[] {
  const issues: string[] = [];
  const seenIds = new Set<string>();
  const seenVolumes = new Set<string>();
  const volumes = anchorVolumes(vocabulary);
  anchors.forEach((anchor, index) => {
    const label = anchor.id || `row ${index + 1}`;
    if (!/^[a-z0-9_]+$/.test(anchor.id)) {
      issues.push(`${label}: id must use only lowercase letters, digits and underscores`);
    }
    if (seenIds.has(anchor.id)) {
      issues.push(`${label}: id is used more than once`);
    }
    seenIds.add(anchor.id);
    if (otherItemIds.includes(anchor.id)) {
      issues.push(`${label}: id is already used by a hull item, subroutine or star chart`);
    }
    if (!volumes.includes(anchor.volume)) {
      issues.push(`${label}: volume must be one of ${volumes.join(', ')}`);
    } else if (seenVolumes.has(anchor.volume)) {
      issues.push(`${label}: ${anchor.volume} already has an anchor; each volume has exactly one`);
    }
    seenVolumes.add(anchor.volume);
    issues.push(...exitIssues(label, anchor, nodeFacts));
    if (!(Number.parseFloat(anchor.rarity) >= 0)) {
      issues.push(`${label}: rarity must be zero or more`);
    }
    if (anchor.dropChance.trim() !== '' && !(Number.parseFloat(anchor.dropChance) >= 0)) {
      issues.push(`${label}: drop chance must be a number of 0 or more`);
    }
    if (anchor.marketChance.trim() !== '' && !(Number.parseFloat(anchor.marketChance) >= 0)) {
      issues.push(`${label}: market chance must be a number of 0 or more`);
    }
    if (anchor.price.trim() !== '' && !(Number.parseFloat(anchor.price) >= 0)) {
      issues.push(`${label}: price must be a number of 0 or more`);
    }
    if (splitList(anchor.dropFactions).some((factionId) => !/^[A-Za-z0-9_.-]+$/.test(factionId))) {
      issues.push(`${label}: drop factions must be faction ids separated by ${LIST_SEPARATOR}`);
    }
    if (anchor.color !== '' && !ANCHOR_COLOR_PATTERN.test(anchor.color)) {
      issues.push(`${label}: color must be #RRGGBB or blank`);
    }
    if (anchor.crestFaction !== '' && !/^[A-Za-z0-9_.-]+$/.test(anchor.crestFaction)) {
      issues.push(`${label}: crest faction must be a single faction id`);
    }
    if (!isKnownAnchorUnlock(anchor.unlock, vocabulary)) {
      issues.push(`${label}: unknown unlock condition ${anchor.unlock}`);
    }
    if (isUnshippedImagePath(anchor.icon)) {
      issues.push(`${label}: icon ${anchor.icon} is in a folder that is not deployed`);
    }
  });
  return issues;
}

export function missingVolumes(anchors: WormholeAnchor[], vocabulary: Vocabulary): string[] {
  const covered = new Set(anchors.map((anchor) => anchor.volume));
  return anchorVolumes(vocabulary).filter((volume) => !covered.has(volume));
}
