import type { Vocabulary } from '../../../api/vocabulary.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';
import { isRegionTag } from './tags.ts';
import { tierOf, type TreeDocument } from './types.ts';

function validateWormholes(document: TreeDocument, issues: string[]): void {
  const typeMap = new Map(document.types.map((type) => [type.id, type]));
  const nodeMap = new Map(document.nodes.map((node) => [node.id, node]));
  for (const node of document.nodes) {
    if (tierOf(typeMap.get(node.type)) !== 'WORMHOLE') {
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
    const regionTags = nodeTags.filter((tag) => isRegionTag(vocabulary, tag));
    if (regionTags.length === 0) {
      issues.push(`Node "${node.id}" has no region tag`);
    } else if (regionTags.length > 1) {
      issues.push(`Node "${node.id}" has ${regionTags.length} region tags (${regionTags.join(', ')}), needs exactly one`);
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
      } else if (isRegionTag(vocabulary, tag)) {
        issues.push(`Type "${type.id}" carries region tag "${tag}", region tags belong on nodes`);
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
    for (const entry of [...(type.effects ?? []), ...(type.hullSizeEffects ?? [])]) {
      if (!vocabulary.effectInfo.has(entry.effect)) {
        issues.push(`Type "${type.id}" uses unknown effect "${entry.effect}"`);
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
