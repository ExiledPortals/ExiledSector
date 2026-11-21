import { describe, expect, it } from 'vitest';
import { atlasIconDestination, circularCopyFolder, destinationPath, isInFolder, itemIconDestination, modPrefixOf, processedCopyFolder } from './imageFolders.ts';

describe('image folders', () => {
  it('names a picked image after its user and keeps the mod prefix that marks other mods art', () => {
    expect(destinationPath('art/atlas_unused_circular/rat_exo_tech_grid_splice.png', atlasIconDestination('travel_catalysts'))).toBe(
      'graphics/exiledSector/tree/atlas/rat_travel_catalysts.png',
    );
    expect(destinationPath('art/unused/plain.png', itemIconDestination('hull_items', 'na_bolter'))).toBe('graphics/exiledSector/items/hull_items/na_bolter.png');
    expect(destinationPath('art/unused/na_bolter.png', itemIconDestination('hull_items', 'na_bolter'))).toBe('graphics/exiledSector/items/hull_items/na_bolter.png');
    expect(destinationPath('art/unused/plain.png', { folder: 'graphics/exiledSector/tree/base/' })).toBe('graphics/exiledSector/tree/base/plain.png');
  });

  it('only treats the known mod prefixes as mod prefixes', () => {
    expect(['rat_a.png', 'na_a.png', 'nskr_a.png', 'tania_a.png', 'aotd_a.png', 'art/x/rat_b.png'].map(modPrefixOf)).toEqual(['rat_', 'na_', 'nskr_', 'tania_', 'aotd_', 'rat_']);
    expect(['ratchet.png', 'exiledSector_a.png', 'nav_a.png'].map(modPrefixOf)).toEqual(['', '', '']);
  });

  it('sends processed copies of shipped images to art/unused and keeps copies of source art in place', () => {
    expect(processedCopyFolder('graphics/exiledSector/tree/base')).toBe('art/unused/');
    expect(processedCopyFolder('graphics/icons/cargo')).toBe('art/unused/');
    expect(processedCopyFolder('art/atlas_unused')).toBe('art/atlas_unused/');
    expect(circularCopyFolder('art/atlas_unused')).toBe('art/atlas_unused_circular/');
    expect(circularCopyFolder('graphics/exiledSector/items/crafting')).toBe('art/unused/circular/');
  });

  it('only counts direct children as being in a folder', () => {
    expect(isInFolder('art/atlas_unused/a.png', 'art/atlas_unused/')).toBe(true);
    expect(isInFolder('art/atlas_unused/deeper/a.png', 'art/atlas_unused/')).toBe(false);
    expect(isInFolder('art/atlas_unused_circular/a.png', 'art/atlas_unused/')).toBe(false);
  });
});
