import fs from 'node:fs';
import path from 'node:path';
import { readDataFile } from './dataFiles.ts';
import type { ProjectPaths } from './paths.ts';

export interface ReservedNodeIds {
  replaced: string[];
  released: string[];
}

function replacedNodeIds(paths: ProjectPaths): string[] {
  const replacementsPath = path.join(paths.projectRoot, 'data', 'config', 'exiledSector', 'node_replacements.csv');
  if (!fs.existsSync(replacementsPath)) {
    return [];
  }
  return readDataFile(replacementsPath)
    .content.split(/\r?\n/)
    .slice(1)
    .map((line) => line.split(',')[0]?.trim() ?? '')
    .filter((oldId) => oldId.length > 0);
}

function releasedNodeIds(paths: ProjectPaths): string[] {
  const releasedPath = path.join(paths.projectRoot, 'src', 'test', 'resources', 'released_skill_nodes.json');
  if (!fs.existsSync(releasedPath)) {
    return [];
  }
  try {
    const released = JSON.parse(readDataFile(releasedPath).content) as { nodes?: unknown };
    return Array.isArray(released.nodes) ? released.nodes.filter((id): id is string => typeof id === 'string') : [];
  } catch {
    return [];
  }
}

export function reservedNodeIds(paths: ProjectPaths): ReservedNodeIds {
  return { replaced: replacedNodeIds(paths), released: releasedNodeIds(paths) };
}
