import type { Vocabulary } from '../../../api/vocabulary.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { isVolumeTag } from './tags.ts';
import { tierOf, type TreeDocument } from './types.ts';

function validateWormholes(document: TreeDocument, issues: string[]): void {
  const typeMap = new Map(document.types.map((type) => [type.id, type]));
  const nodeMap = new Map(document.nodes.map((node) => [node.id, node]));
  for (const node of document.nodes) {
    const tier = tierOf(typeMap.get(node.type));
    if (tier === 'WORMHOLE_EXIT' && node.pairedWith) {
      issues.push(`Wormhole exit "${node.id}" is paired with "${node.pairedWith}"; exits are opened by a wormhole anchor and take no pair`);
      continue;
    }
    if (tier !== 'WORMHOLE') {
      continue;
    }
    if (!node.pairedWith) {
      issues.push(`Wormhole node "${node.id}" has no paired wormhole set`);
      continue;
    }
    if (node.pairedWith === node.id) {
      issues.push(`Wormhole node "${node.id}" is paired with itself`);
      continue;
    }
    const paired = nodeMap.get(node.pairedWith);
    if (!paired) {
      issues.push(`Wormhole node "${node.id}" is paired with "${node.pairedWith}", which does not exist`);
      continue;
    }
    if (tierOf(typeMap.get(paired.type)) !== 'WORMHOLE') {
      issues.push(`Wormhole node "${node.id}" is paired with "${node.pairedWith}", which is not a wormhole-tier node`);
      continue;
    }
    if (paired.pairedWith !== node.id) {
      issues.push(`Wormhole node "${node.id}" is paired with "${node.pairedWith}", but that node is paired with "${paired.pairedWith || 'nothing'}" instead`);
    }
  }
}

function validateTags(document: TreeDocument, vocabulary: Vocabulary, issues: string[]): void {
  for (const node of document.nodes) {
    const nodeTags = node.tags ?? [];
    const volumeTags = nodeTags.filter((tag) => isVolumeTag(vocabulary, tag));
    if (volumeTags.length === 0) {
      issues.push(`Node "${node.id}" has no volume tag`);
    } else if (volumeTags.length > 1) {
      issues.push(`Node "${node.id}" has ${volumeTags.length} volume tags (${volumeTags.join(', ')}), needs exactly one`);
    }
    for (const tag of nodeTags) {
      if (!vocabulary.allTags.includes(tag)) {
        issues.push(`Node "${node.id}" has unknown tag "${tag}"`);
      }
    }
  }
  for (const type of document.types) {
    for (const tag of type.tags ?? []) {
      if (!vocabulary.allTags.includes(tag)) {
        issues.push(`Type "${type.id}" has unknown tag "${tag}"`);
      } else if (isVolumeTag(vocabulary, tag)) {
        issues.push(`Type "${type.id}" carries volume tag "${tag}", volume tags belong on nodes`);
      }
    }
  }
}

export function validateTree(document: TreeDocument, vocabulary: Vocabulary | null, replacedNodeIds: ReadonlySet<string> = new Set()): string[] {
  const issues: string[] = [];
  const idCounts = new Map<string, number>();
  for (const node of document.nodes) {
    idCounts.set(node.id, (idCounts.get(node.id) ?? 0) + 1);
    if (replacedNodeIds.has(node.id)) {
      issues.push(`Node id "${node.id}" was retired in node_replacements.csv; give the node a new id`);
    }
  }
  for (const [id, count] of idCounts) {
    if (count > 1) {
      issues.push(`Duplicate node id: ${id}`);
    }
  }
  const nodeIds = new Set(idCounts.keys());
  const typeIds = new Set(document.types.map((type) => type.id));
  for (const node of document.nodes) {
    if (!typeIds.has(node.type)) {
      issues.push(`Node "${node.id}" uses unknown type "${node.type}"`);
    }
    if (isUnshippedImagePath(node.ringBeltPath)) {
      issues.push(`Node "${node.id}" ring belt art ${node.ringBeltPath} is in a folder that is not deployed`);
    }
    for (const connectedId of node.connectedTo) {
      if (!nodeIds.has(connectedId)) {
        issues.push(`Node "${node.id}" connection "${connectedId}" does not exist`);
      }
    }
  }
  validateWormholes(document, issues);
  for (const type of document.types) {
    if (isUnshippedImagePath(type.icon)) {
      issues.push(`Type "${type.id}" icon ${type.icon} is in a folder that is not deployed`);
    }
    if (!vocabulary) {
      continue;
    }
    const integrated = type.integrated ?? {};
    const effectNames = new Set(
      [...(type.effects ?? []), ...(type.hullSizeEffects ?? []), ...(integrated.effects ?? []), ...(integrated.hullSizeEffects ?? [])].map(
        (entry) => entry.effect,
      ),
    );
    for (const effect of effectNames) {
      if (!vocabulary.effectInfo.has(effect)) {
        issues.push(`Type "${type.id}" uses unknown effect "${effect}"`);
      }
    }
  }
  if (vocabulary) {
    validateTags(document, vocabulary, issues);
  }
  return issues;
}

export function brokenNodeIds(document: TreeDocument): Set<string> {
  const nodeIds = new Set(document.nodes.map((node) => node.id));
  const typeIds = new Set(document.types.map((type) => type.id));
  const broken = new Set<string>();
  for (const node of document.nodes) {
    if (!typeIds.has(node.type) || node.connectedTo.some((connectedId) => !nodeIds.has(connectedId))) {
      broken.add(node.id);
    }
  }
  return broken;
}
