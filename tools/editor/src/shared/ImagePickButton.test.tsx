import { fireEvent, screen, waitFor } from '@testing-library/react';
import fs from 'node:fs';
import path from 'node:path';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders.tsx';
import { startTestServer, type TestServer } from '../test/testServer.ts';
import { atlasIconDestination, shipTreeIconDestination } from './imageFolders.ts';
import { ImagePickButton } from './ImagePickButton.tsx';

const SOURCE_ICON = 'graphics/exiledSector/campaign/abilities/outer_reach_atlas.png';
const CIRCULAR_POOL_ICON = 'art/atlas_unused_circular/rat_grid_splice.png';
const UNCIRCLED_POOL_ICON = 'art/atlas_unused/accelerated_shields.png';
const UNUSED_ART_ICON = 'art/unused/spare_icon.png';

let server: TestServer;

function placeCopy(relativePath: string): void {
  const target = path.join(server.projectRoot, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(path.join(server.projectRoot, SOURCE_ICON), target);
}

function projectFileExists(relativePath: string): boolean {
  return fs.existsSync(path.join(server.projectRoot, relativePath));
}

beforeAll(() => {
  server = startTestServer([SOURCE_ICON]);
  placeCopy(CIRCULAR_POOL_ICON);
  placeCopy(UNCIRCLED_POOL_ICON);
  placeCopy(UNUSED_ART_ICON);
});

afterAll(async () => {
  await server.close();
});

describe('image pick button', () => {
  it('opens Atlas Tree pickers on the circular pool with the steps explained', async () => {
    renderWithProviders(<ImagePickButton destination={atlasIconDestination('travel_catalysts')} onPicked={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Select image' }));

    expect(await screen.findByText('Picking an Atlas Tree icon')).toBeTruthy();
    expect(await screen.findByTitle(CIRCULAR_POOL_ICON)).toBeTruthy();
    expect(screen.queryByTitle(UNCIRCLED_POOL_ICON)).toBeNull();
    expect(screen.queryByTitle(SOURCE_ICON)).toBeNull();
  });

  it('moves a circular pool icon into the Atlas Tree folder under the type id, keeping its mod prefix', async () => {
    const onPicked = vi.fn();
    renderWithProviders(<ImagePickButton destination={atlasIconDestination('travel_catalysts')} onPicked={onPicked} />);
    fireEvent.click(screen.getByRole('button', { name: 'Select image' }));
    fireEvent.click(await screen.findByTitle(CIRCULAR_POOL_ICON));

    const placed = 'graphics/exiledSector/tree/atlas/rat_travel_catalysts.png';
    await waitFor(() => expect(onPicked).toHaveBeenCalledWith(placed));
    expect(projectFileExists(placed)).toBe(true);
    expect(projectFileExists(CIRCULAR_POOL_ICON)).toBe(false);
  });

  it('refuses an Atlas pool icon that has not been made circular yet', async () => {
    const onPicked = vi.fn();
    renderWithProviders(<ImagePickButton destination={atlasIconDestination('travel_boss_star_charts')} onPicked={onPicked} />);
    fireEvent.click(screen.getByRole('button', { name: 'Select image' }));
    fireEvent.change(await screen.findByPlaceholderText('Filter by path...'), { target: { value: 'accelerated' } });
    fireEvent.click(screen.getByRole('textbox', { name: 'Folder' }));
    fireEvent.click(await screen.findByRole('option', { name: /^All folders/ }));
    fireEvent.click(await screen.findByTitle(UNCIRCLED_POOL_ICON));

    expect(await screen.findByText(/make art\/atlas_unused\/accelerated_shields.png circular on the Images page first/)).toBeTruthy();
    expect(onPicked).not.toHaveBeenCalled();
    expect(projectFileExists(UNCIRCLED_POOL_ICON)).toBe(true);
  });

  it('moves unshipped art into the ship tree folder under the type id and leaves shipped images where they are', async () => {
    const onPicked = vi.fn();
    renderWithProviders(<ImagePickButton destination={shipTreeIconDestination('heavy_armor')} onPicked={onPicked} />);
    fireEvent.click(screen.getByRole('button', { name: 'Select image' }));
    fireEvent.click(await screen.findByTitle(UNUSED_ART_ICON));

    await waitFor(() => expect(onPicked).toHaveBeenCalledWith('graphics/exiledSector/tree/base/heavy_armor.png'));
    expect(projectFileExists(UNUSED_ART_ICON)).toBe(false);

    fireEvent.click(screen.getByRole('button', { name: 'Select image' }));
    fireEvent.click(await screen.findByTitle(SOURCE_ICON));
    await waitFor(() => expect(onPicked).toHaveBeenLastCalledWith(SOURCE_ICON));
    expect(projectFileExists(SOURCE_ICON)).toBe(true);
  });
});
