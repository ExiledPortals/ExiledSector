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
  description?: string;
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

export type AnchorShape = 'circle' | 'square' | 'triangle' | 'rectangle' | 'asteroidBelt';

export interface OrbitAnchor {
  id: string;
  x: number;
  y: number;
  radius: number;
  shape?: AnchorShape;
  aspectRatio?: number;
  rotation?: number;
  beltBandWidth?: number;
  beltStyle?: string;
  beltRotation?: number;
  beltRotationSpeed?: number;
  tags?: string[];
}

export interface StaticImage {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  imagePath: string | null;
  rotation: number;
  rotationSpeed: number;
  behindStars?: boolean;
  parallax?: number;
  tags?: string[];
}

export interface Star {
  id: string;
  x: number;
  y: number;
  radius: number;
  starType: string;
  color: string | null;
  overlayPath?: string;
  overlaySize?: number;
  overlayAlpha?: number;
  tags?: string[];
}

export type ThreatUnitType = 'fabricator' | 'hive';

export interface ThreatUnit {
  id: string;
  x: number;
  y: number;
  unitType: string;
  length: number;
  rotation: number;
  tags?: string[];
}

export interface TreeDocument {
  types: SkillType[];
  nodes: TreeNode[];
  curves: ConnectorCurve[];
  hiddenConnectors: NodePair[];
  anchors: OrbitAnchor[];
  images: StaticImage[];
  stars: Star[];
  threatUnits: ThreatUnit[];
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

export const ANCHOR_SHAPES: AnchorShape[] = ['circle', 'square', 'triangle', 'rectangle', 'asteroidBelt'];

export const ANCHOR_SHAPE_LABELS: Record<AnchorShape, string> = {
  circle: 'Circle',
  square: 'Square',
  triangle: 'Triangle',
  rectangle: 'Rectangle',
  asteroidBelt: 'Asteroid Belt',
};

export const RING_BELT_STYLES = [
  { value: 'asteroids', label: 'Asteroids' },
  { value: 'ice', label: 'Ice' },
  { value: 'dust', label: 'Dust' },
  { value: 'special', label: 'Special' },
];

export const ANCHOR_RECTANGLE_ASPECT = 1.6;
export const AURORA_RING_BELT_PATH = 'graphics/planets/aurorae.png';
export const KEYSTONE_BELT_DEFAULT_WIDTH = 1.1;
export const DEFAULT_STAR_COLOR = '#fff2c0';

export const VANILLA_STAR_TYPES = [
  'star_yellow',
  'star_white',
  'star_blue_giant',
  'star_blue_supergiant',
  'star_orange',
  'star_orange_giant',
  'star_red_dwarf',
  'star_red_giant',
  'star_red_supergiant',
  'star_browndwarf',
  'star_neutron',
  'black_hole',
];

export const THREAT_UNIT_TYPES: ThreatUnitType[] = ['fabricator', 'hive'];

export const DEFAULT_THREAT_UNIT_LENGTH = 600;

export interface ThreatUnitSprite {
  label: string;
  spritePath: string;
  widthPerLength: number;
}

export const THREAT_UNIT_SPRITES: Record<ThreatUnitType, ThreatUnitSprite> = {
  fabricator: { label: 'Fabricator', spritePath: 'graphics/ships/threat/fabricator.png', widthPerLength: 380 / 370 },
  hive: { label: 'Hive', spritePath: 'graphics/ships/threat/threat_hive.png', widthPerLength: 212 / 280 },
};

export function threatUnitSprite(unitType: string): ThreatUnitSprite {
  return THREAT_UNIT_TYPES.includes(unitType as ThreatUnitType) ? THREAT_UNIT_SPRITES[unitType as ThreatUnitType] : THREAT_UNIT_SPRITES.fabricator;
}

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
  nightcrossLunarFleetDefeated: 'Nightcross: Stargazer existential threat defeated',
  nightcrossAncientPrototypeFound: 'Nightcross: an Ancient Prototype found',
  lostSectorFrozenHeartVisited: "Lost Sector: the Frozen Heart's system visited",
  taniaBloodTrailDefeated: 'Tania: The Blood Trail of Ramirez defeated',
  taniaFrostFireDefeated: 'Tania: The Frost Fire defeated',
  taniaLastStriveCompleted: 'Tania: The Last Strive completed',
  threatFought: 'Player has fought a Threat fleet',
  remnantFought: 'Player has fought a Remnant fleet',
};

export const PARAMETERISED_UNLOCK_CONDITIONS: ReadonlySet<string> = new Set(['blueprint', 'characterStat', 'minShipLevel', 'memoryFlag', 'anchorFound']);

export function isAuroraBelt(node: Pick<TreeNode, 'ringBeltPath'>): boolean {
  return !node.ringBeltPath || node.ringBeltPath === AURORA_RING_BELT_PATH;
}

export function defaultBeltBandWidth(anchor: Pick<OrbitAnchor, 'radius'>): number {
  return Math.max(60, Math.round(anchor.radius * 0.2));
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

export function imageById(document: Pick<TreeDocument, 'images'>, imageId: string): StaticImage | undefined {
  return document.images.find((image) => image.id === imageId);
}

export function starById(document: Pick<TreeDocument, 'stars'>, starId: string): Star | undefined {
  return document.stars.find((star) => star.id === starId);
}

export function threatUnitById(document: Pick<TreeDocument, 'threatUnits'>, threatUnitId: string): ThreatUnit | undefined {
  return document.threatUnits.find((threatUnit) => threatUnit.id === threatUnitId);
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
