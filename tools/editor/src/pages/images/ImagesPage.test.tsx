import { fireEvent, screen, waitFor } from '@testing-library/react';
import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { findLabelled, getLabelled } from '../../test/labelledQueries.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { startTestServer, type TestServer } from '../../test/testServer.ts';
import { ImagesPage } from './ImagesPage.tsx';

const ICON_FILE = 'graphics/icons/abilities/exiledSector_outer_reach_atlas.png';
const ROTATED_FILE = 'graphics/icons/abilities/exiledSector_outer_reach_atlas_turned.png';
const VANILLA_ICON_FILE = 'graphics/icons/cargo/vanilla_chip.png';
const VANILLA_COPY_FILE = 'graphics/icons/cargo/vanilla_chip_green.png';

let server: TestServer;

beforeAll(() => {
  server = startTestServer([ICON_FILE]);
  const vanillaIcon = path.join(server.projectRoot, 'vanilla-core', VANILLA_ICON_FILE);
  fs.mkdirSync(path.dirname(vanillaIcon), { recursive: true });
  const image = new PNG({ width: 2, height: 2 });
  image.data.fill(255);
  fs.writeFileSync(vanillaIcon, PNG.sync.write(image));
});

afterAll(async () => {
  await server.close();
});

function previewOf(alt: string): HTMLImageElement {
  return screen.getByAltText(alt) as HTMLImageElement;
}

describe('images page', () => {
  it('shows the selected image unmodified and previews each operation on its own', async () => {
    renderWithProviders(<ImagesPage />);
    fireEvent.click(await screen.findByTitle(ICON_FILE));

    await findLabelled('Rotation (degrees clockwise)');
    expect(previewOf(ICON_FILE).style.transform).toBe('');
    expect(previewOf(ICON_FILE).style.filter).toBe('');
    expect(previewOf(`${ICON_FILE} colour-shifted`).style.filter).toBe('hue-rotate(240deg)');
    expect(previewOf(`${ICON_FILE} colour-shifted`).style.transform).toBe('');
    expect(previewOf(`${ICON_FILE} rotated`).style.transform).toBe('rotate(90deg)');
    expect(previewOf(`${ICON_FILE} rotated`).style.filter).toBe('');
  });

  it('clips the rotation preview to the image size for angles that are not quarter turns', async () => {
    renderWithProviders(<ImagesPage />);
    fireEvent.click(await screen.findByTitle(ICON_FILE));

    const rotation = await findLabelled('Rotation (degrees clockwise)');
    expect(screen.getByTestId('rotation-clip').style.overflow).toBe('visible');
    fireEvent.change(rotation, { target: { value: '45' } });
    expect(previewOf(`${ICON_FILE} rotated`).style.transform).toBe('rotate(45deg)');
    expect(screen.getByTestId('rotation-clip').style.overflow).toBe('hidden');
  });

  it('saves a rotated copy next to the original and shows the new copy unmodified', async () => {
    renderWithProviders(<ImagesPage />);
    fireEvent.click(await screen.findByTitle(ICON_FILE));

    fireEvent.change(await findLabelled('Rotation (degrees clockwise)'), { target: { value: '180' } });
    expect(previewOf(`${ICON_FILE} rotated`).style.transform).toBe('rotate(180deg)');
    fireEvent.change(getLabelled('New file suffix'), { target: { value: 'turned' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create rotated copy' }));

    const rotatedPath = path.join(server.projectRoot, ROTATED_FILE);
    await waitFor(() => expect(fs.existsSync(rotatedPath)).toBe(true));
    const original = PNG.sync.read(fs.readFileSync(path.join(server.projectRoot, ICON_FILE)));
    const rotated = PNG.sync.read(fs.readFileSync(rotatedPath));
    expect([rotated.width, rotated.height]).toEqual([original.width, original.height]);
    const lastPixel = (original.width * original.height - 1) * 4;
    expect([...rotated.data.subarray(0, 4)]).toEqual([...original.data.subarray(lastPixel, lastPixel + 4)]);

    const createdPreview = await screen.findByAltText(ROTATED_FILE);
    expect((createdPreview as HTMLImageElement).style.transform).toBe('');
    expect(previewOf(`${ROTATED_FILE} rotated`).style.transform).toBe('rotate(90deg)');
  });

  it('copies a vanilla icon into the mod but never offers to delete it', async () => {
    renderWithProviders(<ImagesPage />);
    fireEvent.click(await screen.findByTitle(VANILLA_ICON_FILE));

    expect(await screen.findByText(/Vanilla image: it is read-only/)).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Delete this image' })).toBeNull();
    fireEvent.change(getLabelled('Hue shift (degrees)'), { target: { value: '120' } });
    fireEvent.change(getLabelled('New file suffix'), { target: { value: 'green' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create colour-shifted copy' }));

    await waitFor(() => expect(fs.existsSync(path.join(server.projectRoot, VANILLA_COPY_FILE))).toBe(true));
    expect(await screen.findByRole('button', { name: 'Delete this image' })).toBeTruthy();
    expect(fs.existsSync(path.join(server.projectRoot, 'vanilla-core', VANILLA_ICON_FILE))).toBe(true);
  });
});
