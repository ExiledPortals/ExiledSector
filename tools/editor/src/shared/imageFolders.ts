export const SHIPPED_IMAGE_ROOT = 'graphics/';
export const SOURCE_ART_ROOT = 'art/';
export const UNUSED_ART_FOLDER = 'art/unused/';
export const CIRCULAR_ART_FOLDER = 'art/unused/circular/';
export const ATLAS_POOL_FOLDER = 'art/atlas_unused/';
export const ATLAS_CIRCULAR_POOL_FOLDER = 'art/atlas_unused_circular/';
export const ATLAS_ICON_FOLDER = 'graphics/exiledSector/tree/atlas/';
export const SHIP_TREE_ICON_FOLDER = 'graphics/exiledSector/tree/base/';
export const DOCTRINE_TYPE_ICON_FOLDER = 'graphics/exiledSector/tree/doctrine/';
export const ITEM_ICON_FOLDER = 'graphics/exiledSector/items/';
export const ATLAS_TREE_DATA_PREFIX = 'data/skilltrees/atlas_tree';

const MOD_PREFIX_PATTERN = /^(rat|na|nskr|tania|aotd|mhm)_/;

export interface ImageDestination {
  folder: string;
  name?: string;
  atlas?: boolean;
}

function fileNameOf(path: string): string {
  return path.split('/').pop() ?? path;
}

function extensionOf(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  return dot === -1 ? '' : fileName.substring(dot);
}

export function isSourceArtPath(path: string | null | undefined): boolean {
  return (path ?? '').startsWith(SOURCE_ART_ROOT);
}

export function isInFolder(path: string, folder: string): boolean {
  return path.startsWith(folder) && !path.substring(folder.length).includes('/');
}

export function modPrefixOf(path: string): string {
  return fileNameOf(path).match(MOD_PREFIX_PATTERN)?.[0] ?? '';
}

export function destinationPath(pickedPath: string, destination: ImageDestination): string {
  const fileName = fileNameOf(pickedPath);
  if (!destination.name) {
    return destination.folder + fileName;
  }
  const prefix = modPrefixOf(pickedPath);
  const name = prefix && !destination.name.startsWith(prefix) ? prefix + destination.name : destination.name;
  return destination.folder + name + extensionOf(fileName);
}

export function displacementFolder(destination: ImageDestination): string {
  return destination.atlas ? ATLAS_CIRCULAR_POOL_FOLDER : UNUSED_ART_FOLDER;
}

export function circularCopyFolder(sourceFolder: string): string {
  return sourceFolder + '/' === ATLAS_POOL_FOLDER ? ATLAS_CIRCULAR_POOL_FOLDER : CIRCULAR_ART_FOLDER;
}

export function processedCopyFolder(sourceFolder: string): string {
  return isSourceArtPath(sourceFolder + '/') ? sourceFolder + '/' : UNUSED_ART_FOLDER;
}

export function shipTreeIconDestination(typeId: string): ImageDestination {
  return { folder: SHIP_TREE_ICON_FOLDER, name: typeId };
}

export function doctrineTypeIconDestination(typeId: string): ImageDestination {
  return { folder: DOCTRINE_TYPE_ICON_FOLDER, name: typeId };
}

export function atlasIconDestination(typeId: string): ImageDestination {
  return { folder: ATLAS_ICON_FOLDER, name: typeId, atlas: true };
}

export function itemIconDestination(itemFolder: string, itemId: string): ImageDestination {
  return { folder: `${ITEM_ICON_FOLDER}${itemFolder}/`, name: itemId };
}
