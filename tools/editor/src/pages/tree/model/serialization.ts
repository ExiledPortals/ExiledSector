import { withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';
import { linkedEntries, pruneConnectorEntries } from './connectors.ts';
import {
  ANCHOR_RECTANGLE_ASPECT,
  defaultBeltBandWidth,
  tierOf,
  type OrbitAnchor,
  type SkillType,
  type Star,
  type StaticImage,
  type TreeDocument,
  type TreeNode,
  type UnlockCondition,
} from './types.ts';

interface TreeFileJson {
  nodes?: TreeNode[];
  connectorCurves?: TreeDocument['curves'];
  hiddenConnectors?: TreeDocument['hiddenConnectors'];
  orbitAnchors?: OrbitAnchor[];
  ringBelts?: { id: string; rotation?: number }[];
  staticImages?: Partial<StaticImage>[];
  stars?: Partial<Star>[];
}

function parseJson<TValue>(text: string, fileName: string): TValue {
  try {
    return JSON.parse(text) as TValue;
  } catch (error) {
    throw new Error(`${fileName} is not valid JSON: ${error instanceof Error ? error.message : String(error)}`);
  }
}

function anchorsFromTree(tree: TreeFileJson): OrbitAnchor[] {
  const anchors = tree.orbitAnchors ?? [];
  for (const belt of tree.ringBelts ?? []) {
    if (!belt.rotation) {
      continue;
    }
    for (const anchor of anchors) {
      if (anchor.id === belt.id && anchor.shape === 'asteroidBelt' && anchor.beltRotation === undefined) {
        anchor.beltRotation = belt.rotation;
      }
    }
  }
  return anchors;
}

function normalizeImages(images: Partial<StaticImage>[] | undefined): StaticImage[] {
  return (images ?? []).map((image) => ({
    id: image.id ?? '',
    x: image.x ?? 0,
    y: image.y ?? 0,
    width: image.width ?? 0,
    height: image.height ?? 0,
    imagePath: image.imagePath || null,
    rotation: image.rotation || 0,
    rotationSpeed: image.rotationSpeed || 0,
    tags: image.tags ? image.tags.slice() : undefined,
  }));
}

function normalizeStars(stars: Partial<Star>[] | undefined): Star[] {
  return (stars ?? []).map((star) => ({
    id: star.id ?? '',
    x: star.x ?? 0,
    y: star.y ?? 0,
    radius: star.radius ?? 0,
    starType: star.starType || 'star_yellow',
    color: star.color || null,
    tags: star.tags ? star.tags.slice() : undefined,
  }));
}

export function parseTreeDocument(typesText: string, treeText: string): TreeDocument {
  const typesJson = parseJson<{ skillTypes?: SkillType[] }>(typesText, 'skill_types.json');
  const treeJson = parseJson<TreeFileJson>(treeText, 'ship_skill_tree.json');
  if (!Array.isArray(typesJson.skillTypes) || typesJson.skillTypes.length === 0) {
    throw new Error('skill_types.json has no skill types');
  }
  if (!Array.isArray(treeJson.nodes) || treeJson.nodes.length === 0) {
    throw new Error('ship_skill_tree.json has no nodes');
  }
  const document: TreeDocument = {
    types: typesJson.skillTypes,
    nodes: treeJson.nodes.map((node) => ({ ...node, connectedTo: node.connectedTo ?? [] })),
    curves: treeJson.connectorCurves ?? [],
    hiddenConnectors: treeJson.hiddenConnectors ?? [],
    anchors: anchorsFromTree(treeJson),
    images: normalizeImages(treeJson.staticImages),
    stars: normalizeStars(treeJson.stars),
  };
  pruneConnectorEntries(document);
  return document;
}

export function serializeUnlockConditions(conditions: UnlockCondition[] | undefined): UnlockCondition[] {
  const out: UnlockCondition[] = [];
  for (const condition of conditions ?? []) {
    if (condition.type === 'characterStat' && condition.statId) {
      out.push({ type: 'characterStat', statId: condition.statId });
    } else if (condition.type === 'minShipLevel' && condition.level) {
      out.push({ type: 'minShipLevel', level: condition.level });
    } else if (condition.type === 'memoryFlag' && condition.key) {
      out.push({ type: 'memoryFlag', key: condition.key });
    } else if (condition.type === 'blueprint' && condition.id) {
      out.push({ type: 'blueprint', category: condition.category || 'hullmod', id: condition.id });
    }
  }
  return out;
}

export function serializeType(type: SkillType): Record<string, unknown> {
  const out: Record<string, unknown> = { id: type.id, name: type.name, icon: type.icon };
  if (tierOf(type) !== 'SMALL') {
    out.tier = tierOf(type);
  }
  if (type.effects?.length) {
    out.effects = type.effects.map((entry) => ({ effect: entry.effect, magnitude: entry.magnitude }));
  }
  if (type.hullSizeEffects?.length) {
    out.hullSizeEffects = type.hullSizeEffects.map((entry) => ({
      effect: entry.effect,
      frigate: entry.frigate,
      destroyer: entry.destroyer,
      cruiser: entry.cruiser,
      capitalShip: entry.capitalShip,
    }));
  }
  if (type.vanillaHullMod) {
    out.vanillaHullMod = type.vanillaHullMod;
  }
  if (type.itemCost?.itemId) {
    out.itemCost = { itemId: type.itemCost.itemId, quantity: type.itemCost.quantity || 0 };
  }
  if (type.temporaryAfterDeploymentSeconds) {
    out.temporaryAfterDeploymentSeconds = type.temporaryAfterDeploymentSeconds;
  }
  if (type.requiredHullSizes?.length) {
    out.requiredHullSizes = type.requiredHullSizes;
  }
  const unlockConditions = serializeUnlockConditions(type.unlockConditions);
  if (unlockConditions.length) {
    out.unlockConditions = unlockConditions;
  }
  if (type.exclusiveHullMods?.length) {
    out.exclusiveHullMods = type.exclusiveHullMods;
  }
  if (type.phantomHullMods?.length) {
    out.phantomHullMods = type.phantomHullMods;
  }
  if (type.exclusiveSkillTypes?.length) {
    out.exclusiveSkillTypes = type.exclusiveSkillTypes;
  }
  if (type.optionalOptions?.length) {
    out.optionalOptions = type.optionalOptions;
  }
  if (type.flavour) {
    out.flavour = type.flavour;
  }
  if (type.description) {
    out.description = type.description;
  }
  if (type.todo) {
    out.todo = type.todo;
  }
  if (type.tags?.length) {
    out.tags = type.tags;
  }
  return out;
}

function serializeTreeNode(node: TreeNode): Record<string, unknown> {
  const out: Record<string, unknown> = { id: node.id, type: node.type, connectedTo: node.connectedTo, x: node.x, y: node.y };
  if (node.orbitAnchorId) {
    out.orbitAnchorId = node.orbitAnchorId;
    out.orbitAngle = node.orbitAngle;
  }
  if (node.ringBeltPath) {
    out.ringBeltPath = node.ringBeltPath;
  }
  if (node.ringBeltColor) {
    out.ringBeltColor = node.ringBeltColor;
  }
  if (node.ringBeltWidth) {
    out.ringBeltWidth = node.ringBeltWidth;
  }
  if (node.wormholeColor) {
    out.wormholeColor = node.wormholeColor;
  }
  if (node.pairedWith) {
    out.pairedWith = node.pairedWith;
  }
  if (node.tags?.length) {
    out.tags = node.tags;
  }
  return out;
}

function serializeOrbitAnchor(anchor: OrbitAnchor): Record<string, unknown> {
  const out: Record<string, unknown> = { id: anchor.id, x: anchor.x, y: anchor.y, radius: anchor.radius };
  if (anchor.shape && anchor.shape !== 'circle') {
    out.shape = anchor.shape;
    if (anchor.shape === 'rectangle') {
      out.aspectRatio = anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT;
    }
    if (anchor.rotation) {
      out.rotation = anchor.rotation;
    }
    if (anchor.shape === 'asteroidBelt') {
      if (anchor.beltBandWidth) {
        out.beltBandWidth = anchor.beltBandWidth;
      }
      if (anchor.beltStyle) {
        out.beltStyle = anchor.beltStyle;
      }
      if (anchor.beltRotation) {
        out.beltRotation = anchor.beltRotation;
      }
      if (anchor.beltRotationSpeed) {
        out.beltRotationSpeed = anchor.beltRotationSpeed;
      }
      if (anchor.tags?.length) {
        out.tags = anchor.tags;
      }
    }
  }
  return out;
}

function serializeRingBelt(anchor: OrbitAnchor): Record<string, unknown> {
  const halfBand = (anchor.beltBandWidth || defaultBeltBandWidth(anchor)) / 2;
  const out: Record<string, unknown> = {
    id: anchor.id,
    x: anchor.x,
    y: anchor.y,
    innerRadius: Math.max(0, anchor.radius - halfBand),
    outerRadius: anchor.radius + halfBand,
    ringArtPath: `graphics/planets/ring_band_${anchor.beltStyle || 'asteroids'}.png`,
  };
  if (anchor.beltRotation) {
    out.rotation = anchor.beltRotation;
  }
  if (anchor.beltRotationSpeed) {
    out.rotationSpeed = anchor.beltRotationSpeed;
  }
  if (anchor.tags?.length) {
    out.tags = anchor.tags;
  }
  return out;
}

function serializeStaticImage(image: StaticImage): Record<string, unknown> {
  const out: Record<string, unknown> = { id: image.id, x: image.x, y: image.y, width: image.width, height: image.height, imagePath: image.imagePath || '' };
  if (image.rotation) {
    out.rotation = image.rotation;
  }
  if (image.rotationSpeed) {
    out.rotationSpeed = image.rotationSpeed;
  }
  if (image.tags?.length) {
    out.tags = image.tags;
  }
  return out;
}

function serializeStar(star: Star): Record<string, unknown> {
  const out: Record<string, unknown> = { id: star.id, x: star.x, y: star.y, radius: star.radius, starType: star.starType || 'star_yellow' };
  if (star.color) {
    out.color = star.color;
  }
  if (star.tags?.length) {
    out.tags = star.tags;
  }
  return out;
}

export function skillTypesFileObject(types: SkillType[]): { skillTypes: Record<string, unknown>[] } {
  return { skillTypes: types.map(serializeType) };
}

function treeFileObject(document: TreeDocument): Record<string, unknown> {
  const out: Record<string, unknown> = { nodes: document.nodes.map(serializeTreeNode) };
  const curves = linkedEntries(document, document.curves);
  const hiddenConnectors = linkedEntries(document, document.hiddenConnectors);
  if (curves.length) {
    out.connectorCurves = curves.map((curve) => ({ a: curve.a, b: curve.b, controlX: curve.controlX, controlY: curve.controlY }));
  }
  if (hiddenConnectors.length) {
    out.hiddenConnectors = hiddenConnectors.map((pair) => ({ a: pair.a, b: pair.b }));
  }
  if (document.anchors.length) {
    out.orbitAnchors = document.anchors.map(serializeOrbitAnchor);
  }
  const beltAnchors = document.anchors.filter((anchor) => anchor.shape === 'asteroidBelt');
  if (beltAnchors.length) {
    out.ringBelts = beltAnchors.map(serializeRingBelt);
  }
  if (document.images.length) {
    out.staticImages = document.images.map(serializeStaticImage);
  }
  if (document.stars.length) {
    out.stars = document.stars.map(serializeStar);
  }
  return out;
}

export interface TreeLineEndings {
  types: LineEnding;
  tree: LineEnding;
}

export interface TreeFileTexts {
  types: string;
  tree: string;
}

export function serializeTreeFiles(document: TreeDocument, lineEndings: TreeLineEndings): TreeFileTexts {
  return {
    types: withLineEnding(JSON.stringify(skillTypesFileObject(document.types), null, '\t'), lineEndings.types),
    tree: withLineEnding(JSON.stringify(treeFileObject(document), null, '\t'), lineEndings.tree),
  };
}
