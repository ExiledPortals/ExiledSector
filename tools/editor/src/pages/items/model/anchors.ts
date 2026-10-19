import type { Vocabulary } from '../../../api/vocabulary.ts';
import { csvTableFromRows, parseCsvTable, serializeCsvTable, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';

export const WORMHOLE_ANCHOR_TYPE = 'wormhole_anchor';
export const WORMHOLE_ANCHOR_RESOURCE = 'wormhole-anchors';
export const WORMHOLE_ANCHOR_FILE_NAME = 'wormhole_anchors.csv';
export const CORE_REGION = 'core';
export const WORMHOLE_TIER = 'WORMHOLE';
export const DEFAULT_ANCHOR_ICON = 'graphics/icons/nav_relay.png';
export const WORMHOLE_ANCHOR_COLUMNS = ['id', 'volume', 'exit_node', 'name', 'icon', 'rarity', 'description', 'unlock'] as const;

export interface WormholeAnchor {
  cells: string[];
  id: string;
  volume: string;
  exitNode: string;
  name: string;
  icon: string;
  rarity: string;
  description: string;
  unlock: string;
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
      rarity: columns.rarity,
      description: columns.description,
      unlock: columns.unlock,
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
    rarity: anchor.rarity,
    description: anchor.description,
    unlock: anchor.unlock,
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
    .filter(([, facts]) => facts.region === volume && facts.tier === WORMHOLE_TIER)
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
    rarity: '10',
    description: '',
    unlock: '',
  };
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
  if (exit.tier !== WORMHOLE_TIER) {
    issues.push(`${label}: exit node ${anchor.exitNode} is a ${exit.tier} node, not a wormhole`);
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
