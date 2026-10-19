import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';

export const WORMHOLE_ANCHOR_TYPE = 'wormhole_anchor';
export const WORMHOLE_ANCHOR_RESOURCE = 'wormhole-anchors';
export const WORMHOLE_ANCHOR_FILE_NAME = 'wormhole_anchors.csv';
export const CORE_REGION = 'core';
export const WORMHOLE_EXIT_TIER = 'WORMHOLE_EXIT';
export const DEFAULT_ANCHOR_ICON = 'graphics/icons/nav_relay.png';
export const DEFAULT_ANCHOR_DROP_CHANCE = '0.06';
export const DROP_FACTION_SEPARATOR = ';';
export const ANCHOR_COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/;
export const WORMHOLE_ANCHOR_COLUMNS = ['id', 'volume', 'exit_node', 'name', 'icon', 'color', 'crest_faction', 'rarity', 'description', 'unlock', 'drop_factions', 'drop_chance'] as const;

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

export function parseWormholeAnchorsCsv(text: string): WormholeAnchorsModel {
  const table = parseCsvTable(text);
  const anchors = table.rows.map((cells): WormholeAnchor => {
    const columns = trimmedCsvCells(table, cells, WORMHOLE_ANCHOR_COLUMNS);
    return {
      cells,
      id: columns.id,
      volume: columns.volume,
      exitNode: columns.exit_node,
      name: columns.name,
      icon: columns.icon,
      color: columns.color,
      crestFaction: columns.crest_faction,
      rarity: columns.rarity,
      description: columns.description,
      unlock: columns.unlock,
      dropFactions: columns.drop_factions,
      dropChance: columns.drop_chance,
      loadedId: columns.id,
    };
  });
  return { table, anchors };
}

function anchorColumnValues(anchor: WormholeAnchor): Record<(typeof WORMHOLE_ANCHOR_COLUMNS)[number], string> {
  return {
    id: anchor.id,
    volume: anchor.volume,
    exit_node: anchor.exitNode,
    name: anchor.name,
    icon: anchor.icon,
    color: anchor.color,
    crest_faction: anchor.crestFaction,
    rarity: anchor.rarity,
    description: anchor.description,
    unlock: anchor.unlock,
    drop_factions: anchor.dropFactions,
    drop_chance: anchor.dropChance,
  };
}

export function serializeWormholeAnchorsCsv(model: WormholeAnchorsModel): string {
  return serializeCsvTable(csvTableFromRows(model.table, model.anchors, anchorColumnValues));
}

export function anchorVolumes(vocabulary: Vocabulary): string[] {
  return vocabulary.regionTags.filter((region) => region !== CORE_REGION);
}

export function treeNodeFacts(treeText: string, typesText: string, vocabulary: Vocabulary): Record<string, TreeNodeFacts> {
  const types = (JSON.parse(typesText) as { skillTypes?: { id?: string; tier?: string }[] }).skillTypes ?? [];
  const tierByType = new Map(types.map((type) => [String(type.id ?? ''), String(type.tier ?? 'SMALL')]));
  const nodes = (JSON.parse(treeText) as { nodes?: { id?: string; type?: string; tags?: string[] }[] }).nodes ?? [];
  const facts: Record<string, TreeNodeFacts> = {};
  for (const node of nodes) {
    const region = (node.tags ?? []).find((tag) => vocabulary.regionTags.includes(tag)) ?? null;
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
  let id = base;
  let suffix = 2;
  while (takenIds.includes(id) || anchors.some((anchor) => anchor.id === id)) {
    id = `${base}_${suffix++}`;
  }
  return {
    cells: [],
    id,
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
  };
}

export function dropFactionIds(dropFactions: string): string[] {
  return dropFactions
    .split(DROP_FACTION_SEPARATOR)
    .map((factionId) => factionId.trim())
    .filter((factionId) => factionId !== '');
}

export function markAnchorsSaved(model: Pick<WormholeAnchorsModel, 'anchors'>, saved: Pick<WormholeAnchorsModel, 'anchors'>): void {
  for (const anchor of model.anchors) {
    const savedAnchor = saved.anchors.find((candidate) =>
      anchor.loadedId ? candidate.loadedId === anchor.loadedId : !candidate.loadedId && candidate.id === anchor.id,
    );
    if (savedAnchor) {
      anchor.loadedId = savedAnchor.id;
    }
  }
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
    if (dropFactionIds(anchor.dropFactions).some((factionId) => !/^[A-Za-z0-9_.-]+$/.test(factionId))) {
      issues.push(`${label}: drop factions must be faction ids separated by ${DROP_FACTION_SEPARATOR}`);
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
