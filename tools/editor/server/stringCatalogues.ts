export const STRING_CATALOGUES = [
  { part: 'lib', file: 'exiled-lib/mod/data/strings/exiledLib/en.json' },
  { part: 'core', file: 'data/strings/exiledSector/core/en.json' },
  { part: 'skill-tree', file: 'data/strings/exiledSector/skillTree/en.json' },
  { part: 'hull-upgrades', file: 'data/strings/exiledSector/hullUpgrades/en.json' },
  { part: 'star-charts', file: 'data/strings/exiledSector/starCharts/en.json' },
] as const;

const RESOURCE_PREFIX = 'strings-en:';

export const STRING_CATALOGUE_RESOURCES: string[] = STRING_CATALOGUES.map((catalogue) => RESOURCE_PREFIX + catalogue.part);

export function stringCatalogueFile(resourceName: string): string | null {
  if (!resourceName.startsWith(RESOURCE_PREFIX)) {
    return null;
  }
  const part = resourceName.slice(RESOURCE_PREFIX.length);
  return STRING_CATALOGUES.find((catalogue) => catalogue.part === part)?.file ?? null;
}
