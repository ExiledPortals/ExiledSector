import { angleDegrees, normalizeDegrees, rotateAroundOrigin, type Point } from './geometry.ts';

export type DecorationKind = 'star' | 'image' | 'ringBelt' | 'threatUnit' | 'serpent';

interface DecorationBase {
  id: string;
  x: number;
  y: number;
  tags?: string[];
}

export interface StarDecoration extends DecorationBase {
  kind: 'star';
  radius: number;
  starType: string;
  color: string | null;
  overlayPath?: string;
  overlaySize?: number;
  overlayAlpha?: number;
}

export interface ImageDecoration extends DecorationBase {
  kind: 'image';
  width: number;
  height: number;
  imagePath: string | null;
  rotation: number;
  rotationSpeed: number;
  behindStars?: boolean;
  parallax?: number;
}

export interface RingBeltDecoration extends DecorationBase {
  kind: 'ringBelt';
  innerRadius: number;
  outerRadius: number;
  ringArtPath: string;
  rotation: number;
  rotationSpeed: number;
}

export interface ThreatUnitDecoration extends DecorationBase {
  kind: 'threatUnit';
  unitType: string;
  length: number;
  rotation: number;
}

export interface SerpentDecoration extends DecorationBase {
  kind: 'serpent';
  orbitRadius: number;
  orbitSeconds: number;
  clockwise: boolean;
  startAngle: number;
  wobble: number;
  wobbleWaves: number;
  wobblePhase: number;
}

export type Decoration = StarDecoration | ImageDecoration | RingBeltDecoration | ThreatUnitDecoration | SerpentDecoration;

export type DecorationOfKind<TKind extends DecorationKind> = Extract<Decoration, { kind: TKind }>;

export interface DecorationHandle {
  name: string;
  x: number;
  y: number;
  rotates: boolean;
}

export interface NewDecorationContext {
  imagePath: string | null;
  region: string | undefined;
}

type JsonRecord = Record<string, unknown>;

export interface DecorationKindSpec<TKind extends DecorationKind> {
  kind: TKind;
  label: string;
  idPrefix: string;
  create: (id: string, at: Point, context: NewDecorationContext) => DecorationOfKind<TKind>;
  parse: (json: JsonRecord) => Omit<DecorationOfKind<TKind>, 'id' | 'x' | 'y' | 'tags' | 'kind'>;
  serialize: (decoration: DecorationOfKind<TKind>) => JsonRecord;
  handles: (decoration: DecorationOfKind<TKind>) => DecorationHandle[];
  dragHandle: (decoration: DecorationOfKind<TKind>, handle: string, world: Point) => void;
}

export const DEFAULT_STAR_COLOR = '#fff2c0';
export const DEFAULT_THREAT_UNIT_LENGTH = 600;
export const MIN_DECORATION_SIZE = 20;
const ROTATE_HANDLE_GAP = 24;

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

export const RING_BELT_STYLES = [
  { value: 'asteroids', label: 'Asteroids' },
  { value: 'ice', label: 'Ice' },
  { value: 'dust', label: 'Dust' },
  { value: 'special', label: 'Special' },
];

export function ringBeltArtPath(style: string): string {
  return `graphics/planets/ring_band_${style}.png`;
}

export function ringBeltStyleOf(ringArtPath: string): string | null {
  return RING_BELT_STYLES.find((style) => ringBeltArtPath(style.value) === ringArtPath)?.value ?? null;
}

export type ThreatUnitType = 'fabricator' | 'hive';

export const THREAT_UNIT_TYPES: ThreatUnitType[] = ['fabricator', 'hive'];

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

function distance(decoration: Point, world: Point): number {
  return Math.hypot(world.x - decoration.x, world.y - decoration.y);
}

function rotationTowards(decoration: Point, world: Point): number {
  return Math.round(normalizeDegrees(angleDegrees(decoration.x, decoration.y, world.x, world.y) + 90));
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function textOr(value: unknown, fallback: string): string {
  return typeof value === 'string' && value !== '' ? value : fallback;
}

function optionalNumber(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

function putIf(out: JsonRecord, key: string, value: unknown, keep: boolean): void {
  if (keep) {
    out[key] = value;
  }
}

function rotatedHandles(center: Point, halfLength: number, rotation: number, resize: Point): DecorationHandle[] {
  const rotate = rotateAroundOrigin(0, -halfLength - ROTATE_HANDLE_GAP, rotation);
  return [
    { name: 'resize', x: center.x + resize.x, y: center.y + resize.y, rotates: false },
    { name: 'rotate', x: center.x + rotate.x, y: center.y + rotate.y, rotates: true },
  ];
}

const STAR: DecorationKindSpec<'star'> = {
  kind: 'star',
  label: 'Star',
  idPrefix: 'star',
  create: (id, at, context) => ({ kind: 'star', id, x: at.x, y: at.y, radius: 850, starType: 'star_yellow', color: null, tags: context.region ? [context.region] : undefined }),
  parse: (json) => ({
    radius: numberOr(json.radius, 0),
    starType: textOr(json.starType, 'star_yellow'),
    color: typeof json.color === 'string' && json.color !== '' ? json.color : null,
    overlayPath: typeof json.overlayPath === 'string' && json.overlayPath !== '' ? json.overlayPath : undefined,
    overlaySize: optionalNumber(json.overlaySize),
    overlayAlpha: optionalNumber(json.overlayAlpha),
  }),
  serialize: (star) => {
    const out: JsonRecord = { radius: star.radius, starType: star.starType || 'star_yellow' };
    putIf(out, 'color', star.color, Boolean(star.color));
    if (star.overlayPath) {
      out.overlayPath = star.overlayPath;
      putIf(out, 'overlaySize', star.overlaySize, star.overlaySize !== undefined);
      putIf(out, 'overlayAlpha', star.overlayAlpha, star.overlayAlpha !== undefined);
    }
    return out;
  },
  handles: (star) => [{ name: 'radius', x: star.x + star.radius, y: star.y, rotates: false }],
  dragHandle: (star, _handle, world) => {
    star.radius = Math.max(MIN_DECORATION_SIZE, Math.round(distance(star, world)));
  },
};

const IMAGE: DecorationKindSpec<'image'> = {
  kind: 'image',
  label: 'Image',
  idPrefix: 'image',
  create: (id, at, context) => ({
    kind: 'image',
    id,
    x: at.x,
    y: at.y,
    width: 800,
    height: 800,
    imagePath: context.imagePath,
    rotation: 0,
    rotationSpeed: 0,
    tags: context.region ? [context.region] : undefined,
  }),
  parse: (json) => {
    const parallax = optionalNumber(json.parallax);
    return {
      width: numberOr(json.width, 0),
      height: numberOr(json.height, 0),
      imagePath: typeof json.imagePath === 'string' && json.imagePath !== '' ? json.imagePath : null,
      rotation: numberOr(json.rotation, 0),
      rotationSpeed: numberOr(json.rotationSpeed, 0),
      behindStars: json.behindStars === true || undefined,
      parallax: parallax === undefined || parallax === 1 ? undefined : parallax,
    };
  },
  serialize: (image) => {
    const out: JsonRecord = { width: image.width, height: image.height, imagePath: image.imagePath || '' };
    putIf(out, 'rotation', image.rotation, Boolean(image.rotation));
    putIf(out, 'rotationSpeed', image.rotationSpeed, Boolean(image.rotationSpeed));
    putIf(out, 'behindStars', true, Boolean(image.behindStars));
    putIf(out, 'parallax', image.parallax, image.parallax !== undefined && image.parallax !== 1);
    return out;
  },
  handles: (image) => rotatedHandles(image, image.height / 2, image.rotation || 0, rotateAroundOrigin(image.width / 2, image.height / 2, image.rotation || 0)),
  dragHandle: (image, handle, world) => {
    if (handle === 'rotate') {
      image.rotation = rotationTowards(image, world);
      return;
    }
    const local = rotateAroundOrigin(world.x - image.x, world.y - image.y, -(image.rotation || 0));
    image.width = Math.max(MIN_DECORATION_SIZE, Math.round(Math.abs(local.x) * 2));
    image.height = Math.max(MIN_DECORATION_SIZE, Math.round(Math.abs(local.y) * 2));
  },
};

const RING_BELT: DecorationKindSpec<'ringBelt'> = {
  kind: 'ringBelt',
  label: 'Ring belt',
  idPrefix: 'ring_belt',
  create: (id, at, context) => ({
    kind: 'ringBelt',
    id,
    x: at.x,
    y: at.y,
    innerRadius: 1400,
    outerRadius: 1700,
    ringArtPath: ringBeltArtPath('asteroids'),
    rotation: 0,
    rotationSpeed: 0,
    tags: context.region ? [context.region] : undefined,
  }),
  parse: (json) => ({
    innerRadius: numberOr(json.innerRadius, 0),
    outerRadius: numberOr(json.outerRadius, 0),
    ringArtPath: textOr(json.ringArtPath, ringBeltArtPath('asteroids')),
    rotation: numberOr(json.rotation, 0),
    rotationSpeed: numberOr(json.rotationSpeed, 0),
  }),
  serialize: (belt) => {
    const out: JsonRecord = { innerRadius: belt.innerRadius, outerRadius: belt.outerRadius, ringArtPath: belt.ringArtPath };
    putIf(out, 'rotation', belt.rotation, Boolean(belt.rotation));
    putIf(out, 'rotationSpeed', belt.rotationSpeed, Boolean(belt.rotationSpeed));
    return out;
  },
  handles: (belt) => [
    { name: 'inner', x: belt.x + belt.innerRadius, y: belt.y, rotates: false },
    { name: 'outer', x: belt.x + belt.outerRadius, y: belt.y, rotates: false },
  ],
  dragHandle: (belt, handle, world) => {
    const radius = Math.round(distance(belt, world));
    if (handle === 'inner') {
      belt.innerRadius = Math.max(0, Math.min(radius, belt.outerRadius - MIN_DECORATION_SIZE));
    } else {
      belt.outerRadius = Math.max(belt.innerRadius + MIN_DECORATION_SIZE, radius);
    }
  },
};

const THREAT_UNIT: DecorationKindSpec<'threatUnit'> = {
  kind: 'threatUnit',
  label: 'Threat unit',
  idPrefix: 'threat_unit',
  create: (id, at, context) => ({
    kind: 'threatUnit',
    id,
    x: at.x,
    y: at.y,
    unitType: 'fabricator',
    length: DEFAULT_THREAT_UNIT_LENGTH,
    rotation: 0,
    tags: context.region ? [context.region] : undefined,
  }),
  parse: (json) => ({
    unitType: textOr(json.unitType, 'fabricator'),
    length: numberOr(json.length, DEFAULT_THREAT_UNIT_LENGTH),
    rotation: numberOr(json.rotation, 0),
  }),
  serialize: (unit) => {
    const out: JsonRecord = { unitType: unit.unitType || 'fabricator', length: unit.length };
    putIf(out, 'rotation', unit.rotation, Boolean(unit.rotation));
    return out;
  },
  handles: (unit) => rotatedHandles(unit, unit.length / 2, unit.rotation || 0, rotateAroundOrigin(0, unit.length / 2, unit.rotation || 0)),
  dragHandle: (unit, handle, world) => {
    if (handle === 'rotate') {
      unit.rotation = rotationTowards(unit, world);
    } else {
      unit.length = Math.max(MIN_DECORATION_SIZE, Math.round(distance(unit, world) * 2));
    }
  },
};

export function serpentPathPoint(serpent: SerpentDecoration, angleInDegrees: number): Point {
  const angle = (angleInDegrees * Math.PI) / 180;
  const radius = serpent.orbitRadius * (1 + serpent.wobble * Math.sin(serpent.wobbleWaves * angle + (serpent.wobblePhase * Math.PI) / 180));
  return { x: serpent.x + radius * Math.cos(angle), y: serpent.y + radius * Math.sin(angle) };
}

const SERPENT: DecorationKindSpec<'serpent'> = {
  kind: 'serpent',
  label: 'Serpent',
  idPrefix: 'serpent',
  create: (id, at, context) => ({
    kind: 'serpent',
    id,
    x: at.x,
    y: at.y,
    orbitRadius: 1500,
    orbitSeconds: 150,
    clockwise: true,
    startAngle: 0,
    wobble: 0.12,
    wobbleWaves: 9,
    wobblePhase: 0,
    tags: context.region ? [context.region] : undefined,
  }),
  parse: (json) => ({
    orbitRadius: numberOr(json.orbitRadius, 0),
    orbitSeconds: numberOr(json.orbitSeconds, 0),
    clockwise: json.clockwise !== false,
    startAngle: numberOr(json.startAngle, 0),
    wobble: numberOr(json.wobble, 0),
    wobbleWaves: numberOr(json.wobbleWaves, 0),
    wobblePhase: numberOr(json.wobblePhase, 0),
  }),
  serialize: (serpent) => {
    const out: JsonRecord = { orbitRadius: serpent.orbitRadius, orbitSeconds: serpent.orbitSeconds, clockwise: serpent.clockwise };
    putIf(out, 'startAngle', serpent.startAngle, Boolean(serpent.startAngle));
    putIf(out, 'wobble', serpent.wobble, Boolean(serpent.wobble));
    putIf(out, 'wobbleWaves', serpent.wobbleWaves, Boolean(serpent.wobbleWaves));
    putIf(out, 'wobblePhase', serpent.wobblePhase, Boolean(serpent.wobblePhase));
    return out;
  },
  handles: (serpent) => {
    const head = serpentPathPoint(serpent, serpent.startAngle);
    return [
      { name: 'radius', x: serpent.x + serpent.orbitRadius, y: serpent.y, rotates: false },
      { name: 'start', x: head.x, y: head.y, rotates: true },
    ];
  },
  dragHandle: (serpent, handle, world) => {
    if (handle === 'start') {
      serpent.startAngle = Math.round(normalizeDegrees(angleDegrees(serpent.x, serpent.y, world.x, world.y)));
    } else {
      serpent.orbitRadius = Math.max(MIN_DECORATION_SIZE, Math.round(distance(serpent, world)));
    }
  },
};

export const DECORATION_KINDS: { [TKind in DecorationKind]: DecorationKindSpec<TKind> } = {
  star: STAR,
  image: IMAGE,
  ringBelt: RING_BELT,
  threatUnit: THREAT_UNIT,
  serpent: SERPENT,
};

export const DECORATION_KIND_ORDER: DecorationKind[] = ['star', 'image', 'ringBelt', 'threatUnit', 'serpent'];

export function decorationSpec<TKind extends DecorationKind>(kind: TKind): DecorationKindSpec<TKind> {
  return DECORATION_KINDS[kind];
}

function isDecorationKind(value: unknown): value is DecorationKind {
  return typeof value === 'string' && value in DECORATION_KINDS;
}

export const RETIRED_DECORATION_LISTS = ['stars', 'staticImages', 'ringBelts', 'threatUnits'];

export function parseDecorations(entries: unknown): Decoration[] {
  if (!Array.isArray(entries)) {
    return [];
  }
  return entries.flatMap((entry): Decoration[] => {
    const json = (entry ?? {}) as JsonRecord;
    if (!isDecorationKind(json.kind)) {
      throw new Error(`ship_skill_tree.json has decoration "${String(json.id)}" of unknown kind "${String(json.kind)}"; the editor would drop it on save`);
    }
    const base = {
      id: typeof json.id === 'string' ? json.id : '',
      x: numberOr(json.x, 0),
      y: numberOr(json.y, 0),
      tags: Array.isArray(json.tags) ? (json.tags as string[]).slice() : undefined,
    };
    return [{ kind: json.kind, ...base, ...DECORATION_KINDS[json.kind].parse(json) } as Decoration];
  });
}

export function serializeDecoration(decoration: Decoration): JsonRecord {
  const spec = DECORATION_KINDS[decoration.kind] as DecorationKindSpec<DecorationKind>;
  const out: JsonRecord = { kind: decoration.kind, id: decoration.id, x: decoration.x, y: decoration.y, ...spec.serialize(decoration as never) };
  putIf(out, 'tags', decoration.tags, Boolean(decoration.tags?.length));
  return out;
}

export function decorationHandles(decoration: Decoration): DecorationHandle[] {
  return (DECORATION_KINDS[decoration.kind] as DecorationKindSpec<DecorationKind>).handles(decoration as never);
}

export function dragDecorationHandle(decoration: Decoration, handle: string, world: Point): void {
  (DECORATION_KINDS[decoration.kind] as DecorationKindSpec<DecorationKind>).dragHandle(decoration as never, handle, world);
}

export function decorationById(document: { decorations: Decoration[] }, decorationId: string): Decoration | undefined {
  return document.decorations.find((decoration) => decoration.id === decorationId);
}

export function decorationsOfKind<TKind extends DecorationKind>(decorations: readonly Decoration[], kind: TKind): DecorationOfKind<TKind>[] {
  return decorations.filter((decoration): decoration is DecorationOfKind<TKind> => decoration.kind === kind);
}

export function sortDecorationsByKind(decorations: readonly Decoration[]): Decoration[] {
  return DECORATION_KIND_ORDER.flatMap((kind) => decorations.filter((decoration) => decoration.kind === kind));
}
