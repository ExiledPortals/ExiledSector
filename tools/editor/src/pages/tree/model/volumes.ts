import type { TreeDocument } from './types.ts';

type Tagged = { tags?: string[] };

function taggedItems(document: TreeDocument): Tagged[] {
  return [...document.nodes, ...document.decorations];
}

export function volumeIdProblem(document: TreeDocument, volumeId: string): string | null {
  if (!volumeId) {
    return 'A volume id must not be empty';
  }
  if (/\s/.test(volumeId)) {
    return 'A volume id must not contain spaces';
  }
  return document.volumes.includes(volumeId) ? `Volume "${volumeId}" already exists` : null;
}

export function volumeUseCount(document: TreeDocument, volumeId: string): number {
  return taggedItems(document).filter((item) => item.tags?.includes(volumeId)).length;
}

export function addVolume(document: TreeDocument, volumeId: string): void {
  document.volumes.push(volumeId);
}

export function renameVolume(document: TreeDocument, oldVolumeId: string, newVolumeId: string): void {
  const index = document.volumes.indexOf(oldVolumeId);
  if (index === -1) {
    return;
  }
  document.volumes[index] = newVolumeId;
  for (const item of taggedItems(document)) {
    if (item.tags) {
      item.tags = item.tags.map((tag) => (tag === oldVolumeId ? newVolumeId : tag));
    }
  }
}

export function removeVolume(document: TreeDocument, volumeId: string): void {
  document.volumes = document.volumes.filter((declared) => declared !== volumeId);
}
