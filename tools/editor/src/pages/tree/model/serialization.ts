import { withLineEnding, type LineEnding } from '../../../shared/lineEndings.ts';
import { linkedEntries, pruneConnectorEntries } from './connectors.ts';
import { parseDecorations, RETIRED_DECORATION_LISTS, serializeDecoration, sortDecorationsByKind, type Decoration } from './decorations.ts';
import {
  CONNECTOR_CURVE_FIELDS,
  INTEGRATED_EFFECTS_FIELDS,
  NODE_PAIR_FIELDS,
  ORBIT_ANCHOR_FIELDS,
  refuseUnknownFields,
  SKILL_TYPE_FIELDS,
  TREE_NODE_FIELDS,
  writeFields,
  type JsonField,
} from './fieldSchema.ts';
import type { ConnectorCurve, NodePair, OrbitAnchor, SkillType, TreeDocument, TreeNode } from './types.ts';
import type { TreeFiles } from './treeFiles.ts';

interface SkillTypesFileJson {
  skillTypes?: unknown;
}

interface TreeFileJson {
  volumes?: unknown;
  nodes?: TreeNode[];
  connectorCurves?: ConnectorCurve[];
  hiddenConnectors?: NodePair[];
  orbitAnchors?: OrbitAnchor[];
  decorations?: unknown[];
}

const SKILL_TYPES_FILE_FIELDS: readonly JsonField<SkillTypesFileJson>[] = [{ key: 'skillTypes' }];

const TREE_FILE_FIELDS: readonly JsonField<TreeFileJson>[] = [
  { key: 'volumes' },
  { key: 'nodes' },
  { key: 'connectorCurves' },
  { key: 'hiddenConnectors' },
  { key: 'orbitAnchors' },
  { key: 'decorations' },
];

function parseJson<TValue>(text: string, fileName: string): TValue {
  try {
    return JSON.parse(text) as TValue;
  } catch (error) {
    throw new Error(`${fileName} is not valid JSON: ${error instanceof Error ? error.message : String(error)}`);
  }
}

function parseVolumes(files: TreeFiles, treeJson: TreeFileJson): string[] {
  if (!('volumes' in treeJson)) {
    return [];
  }
  if (!files.declaresVolumes) {
    throw new Error(`${files.layoutFileName} has a volumes list, but this tree takes its volumes from the editor vocabulary, so a save would drop it`);
  }
  const volumes = treeJson.volumes;
  if (!Array.isArray(volumes) || volumes.some((volume) => typeof volume !== 'string')) {
    throw new Error(`${files.layoutFileName} has a volumes entry that is not a list of volume ids`);
  }
  return volumes.slice() as string[];
}

export function parseSkillTypes(typesText: string, typesFileName: string): SkillType[] {
  const typesJson = parseJson<SkillTypesFileJson>(typesText, typesFileName);
  refuseUnknownFields(typesJson, SKILL_TYPES_FILE_FIELDS, typesFileName, 'the top level');
  if (!Array.isArray(typesJson.skillTypes) || typesJson.skillTypes.length === 0) {
    throw new Error(`${typesFileName} has no skill types`);
  }
  const types = typesJson.skillTypes as SkillType[];
  for (const type of types) {
    refuseUnknownFields(type, SKILL_TYPE_FIELDS, typesFileName, `skill type "${type.id}"`);
    refuseUnknownFields(type.integrated, INTEGRATED_EFFECTS_FIELDS, typesFileName, `the integrated block of skill type "${type.id}"`);
  }
  return types;
}

function refuseUnknownTreeFields(treeJson: TreeFileJson, fileName: string): void {
  refuseUnknownFields(treeJson, TREE_FILE_FIELDS, fileName, 'the top level');
  for (const node of treeJson.nodes ?? []) {
    refuseUnknownFields(node, TREE_NODE_FIELDS, fileName, `node "${node.id}"`);
  }
  for (const curve of treeJson.connectorCurves ?? []) {
    refuseUnknownFields(curve, CONNECTOR_CURVE_FIELDS, fileName, `the connector curve between "${curve.a}" and "${curve.b}"`);
  }
  for (const pair of treeJson.hiddenConnectors ?? []) {
    refuseUnknownFields(pair, NODE_PAIR_FIELDS, fileName, `the hidden connector between "${pair.a}" and "${pair.b}"`);
  }
  for (const anchor of treeJson.orbitAnchors ?? []) {
    refuseUnknownFields(anchor, ORBIT_ANCHOR_FIELDS, fileName, `orbit anchor "${anchor.id}"`);
  }
}

export interface TreeDocumentContent {
  types: SkillType[];
  volumes?: string[];
  nodes: (Omit<TreeNode, 'connectedTo'> & Partial<Pick<TreeNode, 'connectedTo'>>)[];
  curves?: ConnectorCurve[];
  hiddenConnectors?: NodePair[];
  anchors?: OrbitAnchor[];
  decorations: Decoration[];
}

export function normalizeTreeDocument(content: TreeDocumentContent): TreeDocument {
  const document: TreeDocument = {
    types: content.types,
    volumes: content.volumes ?? [],
    nodes: content.nodes.map((node) => ({ ...node, connectedTo: node.connectedTo ?? [] })),
    curves: content.curves ?? [],
    hiddenConnectors: content.hiddenConnectors ?? [],
    anchors: content.anchors ?? [],
    decorations: content.decorations,
  };
  pruneConnectorEntries(document);
  return document;
}

export function parseTreeDocument(files: TreeFiles, typesText: string, treeText: string): TreeDocument {
  const types = parseSkillTypes(typesText, files.typesFileName);
  const treeJson = parseJson<TreeFileJson>(treeText, files.layoutFileName);
  if (!Array.isArray(treeJson.nodes) || treeJson.nodes.length === 0) {
    throw new Error(`${files.layoutFileName} has no nodes`);
  }
  const retiredLists = RETIRED_DECORATION_LISTS.filter((key) => key in treeJson);
  if (retiredLists.length > 0) {
    throw new Error(
      `${files.layoutFileName} still has ${retiredLists.join(', ')}; move those entries into "decorations" with a kind before editing, or a save would drop them`,
    );
  }
  refuseUnknownTreeFields(treeJson, files.layoutFileName);
  return normalizeTreeDocument({
    types,
    volumes: parseVolumes(files, treeJson),
    nodes: treeJson.nodes,
    curves: treeJson.connectorCurves,
    hiddenConnectors: treeJson.hiddenConnectors,
    anchors: treeJson.orbitAnchors,
    decorations: parseDecorations(treeJson.decorations, files.layoutFileName),
  });
}

export function serializeType(type: SkillType): Record<string, unknown> {
  return writeFields(type, SKILL_TYPE_FIELDS);
}

export interface SkillTypesFile {
  skillTypes: Record<string, unknown>[];
}

export function skillTypesFileObject(types: SkillType[]): SkillTypesFile {
  return { skillTypes: types.map(serializeType) };
}

function treeFileObject(files: TreeFiles, document: TreeDocument): Record<string, unknown> {
  const out: Record<string, unknown> = files.declaresVolumes ? { volumes: document.volumes.slice() } : {};
  out.nodes = document.nodes.map((node) => writeFields(node, TREE_NODE_FIELDS));
  const curves = linkedEntries(document, document.curves);
  const hiddenConnectors = linkedEntries(document, document.hiddenConnectors);
  if (curves.length) {
    out.connectorCurves = curves.map((curve) => writeFields(curve, CONNECTOR_CURVE_FIELDS));
  }
  if (hiddenConnectors.length) {
    out.hiddenConnectors = hiddenConnectors.map((pair) => writeFields(pair, NODE_PAIR_FIELDS));
  }
  if (document.anchors.length) {
    out.orbitAnchors = document.anchors.map((anchor) => writeFields(anchor, ORBIT_ANCHOR_FIELDS));
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

export function serializeTreeFiles(files: TreeFiles, document: TreeDocument, lineEndings: TreeLineEndings): TreeFileTexts {
  return {
    types: serializeSkillTypesText(document.types, lineEndings.types),
    tree: withLineEnding(JSON.stringify(treeFileObject(files, document), null, '\t'), lineEndings.tree),
  };
}
