import type { Decoration } from './decorations.ts';

export interface EffectEntry {
  effect: string;
  magnitude: number;
}

export interface HullSizeEffectEntry {
  effect: string;
  frigate: number;
  destroyer: number;
  cruiser: number;
  capitalShip: number;
}

export interface UnlockCondition {
  type: string;
  category?: string;
  id?: string;
  statId?: string;
  level?: number;
  key?: string;
  volume?: string;
}

export interface ItemCost {
  itemId: string;
  quantity: number;
}

export interface SkillType {
  id: string;
  name: string;
  icon: string;
  tier?: string;
  effects?: EffectEntry[];
  hullSizeEffects?: HullSizeEffectEntry[];
  vanillaHullMod?: string;
  itemCost?: ItemCost;
  temporaryAfterDeploymentSeconds?: number;
  requiredHullSizes?: string[];
  unlockConditions?: UnlockCondition[];
  exclusiveHullMods?: string[];
  phantomHullMods?: string[];
  exclusiveSkillTypes?: string[];
  optionalOptions?: string[];
  flavour?: string;
  flavourAutomated?: string;
  effectText?: string[];
  todo?: string;
  tags?: string[];
  moduleShare?: string;
}

export interface TreeNode {
  id: string;
  type: string;
  connectedTo: string[];
  x: number;
  y: number;
  orbitAnchorId?: string;
  orbitAngle?: number;
  ringBeltPath?: string;
  ringBeltColor?: string;
  ringBeltWidth?: number;
  wormholeColor?: string;
  pairedWith?: string;
  tags?: string[];
}

export interface ConnectorCurve {
  a: string;
  b: string;
  controlX: number;
  controlY: number;
}

export interface NodePair {
  a: string;
  b: string;
}

export type AnchorShape = 'circle' | 'square' | 'triangle' | 'rectangle';

export interface OrbitAnchor {
  id: string;
  x: number;
  y: number;
  radius: number;
  shape?: AnchorShape;
  aspectRatio?: number;
  rotation?: number;
}

export interface TreeDocument {
  types: SkillType[];
  nodes: TreeNode[];
  curves: ConnectorCurve[];
  hiddenConnectors: NodePair[];
  anchors: OrbitAnchor[];
  decorations: Decoration[];
}

export type Tier = 'SMALL' | 'NOTABLE' | 'KEYSTONE' | 'WORMHOLE' | 'WORMHOLE_EXIT' | 'ROOT' | 'SOCKET';

export const TIER_RADIUS: Record<Tier, number> = {
  SMALL: 32,
  NOTABLE: 38.4,
  KEYSTONE: 44.8,
  WORMHOLE: 41.6,
  WORMHOLE_EXIT: 41.6,
  ROOT: 80,
  SOCKET: 38.4,
};

export const TIER_LABELS: Record<string, string> = {
  SMALL: 'Small',
  NOTABLE: 'Notable',
  KEYSTONE: 'Keystone',
  WORMHOLE: 'Wormhole',
  WORMHOLE_EXIT: 'Wormhole Exit',
  SOCKET: 'Modular Hull Socket',
  ROOT: 'Root',
};

export const TIER_COLOR_VARIABLES: Record<Tier, string> = {
  SMALL: 'var(--tree-small)',
  NOTABLE: 'var(--tree-notable)',
  KEYSTONE: 'var(--tree-keystone)',
  WORMHOLE: 'var(--tree-wormhole)',
  WORMHOLE_EXIT: 'var(--tree-wormhole)',
  ROOT: 'var(--tree-root)',
  SOCKET: 'var(--tree-socket)',
};

export function tierOf(type: Pick<SkillType, 'tier'> | null | undefined): Tier {
  const tier = type?.tier;
  return tier && tier in TIER_RADIUS ? (tier as Tier) : 'SMALL';
}

export function radiusForTier(tier: Tier): number {
  return TIER_RADIUS[tier];
}

export const ANCHOR_SHAPES: AnchorShape[] = ['circle', 'square', 'triangle', 'rectangle'];

export const ANCHOR_SHAPE_LABELS: Record<AnchorShape, string> = {
  circle: 'Circle',
  square: 'Square',
  triangle: 'Triangle',
  rectangle: 'Rectangle',
};

export const ANCHOR_RECTANGLE_ASPECT = 1.6;
export const AURORA_RING_BELT_PATH = 'graphics/planets/aurorae.png';
export const KEYSTONE_BELT_DEFAULT_WIDTH = 1.1;

export const HULL_SIZE_OPTIONS = [
  { value: 'FRIGATE', label: 'Frigate' },
  { value: 'DESTROYER', label: 'Destroyer' },
  { value: 'CRUISER', label: 'Cruiser' },
  { value: 'CAPITAL_SHIP', label: 'Capital' },
];

export const MODULE_SHARE_OPTIONS = [
  { value: 'auto', label: 'Auto (all or nothing by effect)' },
  { value: 'all', label: 'All shareable effects' },
  { value: 'none', label: 'None' },
];

export const UNLOCK_CONDITION_LABELS: Record<string, string> = {
  blueprint: 'Blueprint known',
  characterStat: 'Character stat flag',
  minShipLevel: 'Min ship level',
  memoryFlag: 'Game state (memory flag)',
  ratPlayerExoship: 'RAT: player owns an Exoship',
  ratSingularityDefeated: 'RAT: Singularity defeated',
  anchorFound: "A volume's wormhole anchor found",
  nightcrossExistentialThreatDefeated: 'Nightcross: Stargazer existential threat defeated',
  nightcrossAncientPrototypeFound: 'Nightcross: an Ancient Prototype found',
  lostSectorFrozenHeartVisited: "Lost Sector: the Frozen Heart's system visited",
  taniaBloodTrailDefeated: 'Tania: The Blood Trail of Ramirez defeated',
  taniaFrostFireDefeated: 'Tania: The Frost Fire defeated',
  taniaLastStriveCompleted: 'Tania: The Last Strive completed',
  threatFought: 'Player has fought a Threat fleet',
  remnantFought: 'Player has fought a Remnant fleet',
  dwellerFought: 'Player has fought the Shrouded Dwellers',
};

export const PARAMETERISED_UNLOCK_CONDITIONS: ReadonlySet<string> = new Set(['blueprint', 'characterStat', 'minShipLevel', 'memoryFlag', 'anchorFound']);

export function isAuroraBelt(node: Pick<TreeNode, 'ringBeltPath'>): boolean {
  return !node.ringBeltPath || node.ringBeltPath === AURORA_RING_BELT_PATH;
}

export function typeById(document: Pick<TreeDocument, 'types'>, typeId: string): SkillType | undefined {
  return document.types.find((type) => type.id === typeId);
}

export function nodeById(document: Pick<TreeDocument, 'nodes'>, nodeId: string): TreeNode | undefined {
  return document.nodes.find((node) => node.id === nodeId);
}

export function anchorById(document: Pick<TreeDocument, 'anchors'>, anchorId: string): OrbitAnchor | undefined {
  return document.anchors.find((anchor) => anchor.id === anchorId);
}

export function uniqueId(prefix: string, taken: (id: string) => boolean): string {
  let counter = 1;
  let id = `${prefix}_${counter}`;
  while (taken(id)) {
    counter++;
    id = `${prefix}_${counter}`;
  }
  return id;
}
