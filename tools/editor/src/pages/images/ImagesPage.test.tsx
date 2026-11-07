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

let server: TestServer;

beforeAll(() => {
  server = startTestServer([ICON_FILE]);
});

afterAll(async () => {
  await server.close();
});

describe('images page', () => {
  it('previews a rotation and saves a rotated copy next to the original', async () => {
    renderWithProviders(<ImagesPage />);
    fireEvent.click(await screen.findByTitle(ICON_FILE));

    fireEvent.change(await findLabelled('Rotation (degrees clockwise)'), { target: { value: '180' } });
    expect(screen.getByAltText(ICON_FILE).style.transform).toBe('rotate(180deg)');
    fireEvent.change(getLabelled('New file suffix'), { target: { value: 'turned' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create rotated copy' }));

    const rotatedPath = path.join(server.projectRoot, ROTATED_FILE);
    await waitFor(() => expect(fs.existsSync(rotatedPath)).toBe(true));
    const original = PNG.sync.read(fs.readFileSync(path.join(server.projectRoot, ICON_FILE)));
    const rotated = PNG.sync.read(fs.readFileSync(rotatedPath));
    expect([rotated.width, rotated.height]).toEqual([original.width, original.height]);
    const lastPixel = (original.width * original.height - 1) * 4;
    expect([...rotated.data.subarray(0, 4)]).toEqual([...original.data.subarray(lastPixel, lastPixel + 4)]);
  });
});
