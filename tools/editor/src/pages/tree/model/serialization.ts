import { withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';
import { linkedEntries, pruneConnectorEntries } from './connectors.ts';
import { parseDecorations, RETIRED_DECORATION_LISTS, serializeDecoration, sortDecorationsByKind } from './decorations.ts';
import {
  ANCHOR_RECTANGLE_ASPECT,
  PARAMETERISED_UNLOCK_CONDITIONS,
  tierOf,
  type OrbitAnchor,
  type SkillType,
  type TreeDocument,
  type TreeNode,
  type UnlockCondition,
} from './types.ts';

interface TreeFileJson {
  nodes?: TreeNode[];
  connectorCurves?: TreeDocument['curves'];
  hiddenConnectors?: TreeDocument['hiddenConnectors'];
  orbitAnchors?: OrbitAnchor[];
  decorations?: unknown[];
}

function parseJson<TValue>(text: string, fileName: string): TValue {
  try {
    return JSON.parse(text) as TValue;
  } catch (error) {
    throw new Error(`${fileName} is not valid JSON: ${error instanceof Error ? error.message : String(error)}`);
  }
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
  const retiredLists = RETIRED_DECORATION_LISTS.filter((key) => key in treeJson);
  if (retiredLists.length > 0) {
    throw new Error(`ship_skill_tree.json still has ${retiredLists.join(', ')}; move those entries into "decorations" with a kind before editing, or a save would drop them`);
  }
  const document: TreeDocument = {
    types: typesJson.skillTypes,
    nodes: treeJson.nodes.map((node) => ({ ...node, connectedTo: node.connectedTo ?? [] })),
    curves: treeJson.connectorCurves ?? [],
    hiddenConnectors: treeJson.hiddenConnectors ?? [],
    anchors: treeJson.orbitAnchors ?? [],
    decorations: parseDecorations(treeJson.decorations),
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
    } else if (condition.type === 'anchorFound' && condition.volume) {
      out.push({ type: 'anchorFound', volume: condition.volume });
    } else if (condition.type === 'blueprint' && condition.id) {
      out.push({ type: 'blueprint', category: condition.category || 'hullmod', id: condition.id });
    } else if (condition.type && !PARAMETERISED_UNLOCK_CONDITIONS.has(condition.type)) {
      out.push({ type: condition.type });
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
  if (type.moduleShare) {
    out.moduleShare = type.moduleShare;
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
  }
  return out;
}

export interface SkillTypesFile {
  skillTypes: Record<string, unknown>[];
}

export function skillTypesFileObject(types: SkillType[]): SkillTypesFile {
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
  if (document.decorations.length) {
    out.decorations = sortDecorationsByKind(document.decorations).map(serializeDecoration);
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

export function serializeSkillTypesText(types: SkillType[], lineEnding: LineEnding): string {
  return withLineEnding(JSON.stringify(skillTypesFileObject(types), null, '\t'), lineEnding);
}

export function serializeTreeFiles(document: TreeDocument, lineEndings: TreeLineEndings): TreeFileTexts {
  return {
    types: serializeSkillTypesText(document.types, lineEndings.types),
    tree: withLineEnding(JSON.stringify(treeFileObject(document), null, '\t'), lineEndings.tree),
  };
}
