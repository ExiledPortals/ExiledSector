import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { TextPage } from './TextPage.tsx';

const STRINGS_FILE = 'data/strings/exiledSector/en.json';
const TYPES_FILE = 'data/skilltrees/skill_types.json';

let server: TestServer;

beforeAll(() => {
  server = startTestServer([STRINGS_FILE, TYPES_FILE]);
});

afterAll(async () => {
  await server.close();
});

async function openString(key: string) {
  fireEvent.change(screen.getByLabelText('Search strings'), { target: { value: key } });
  fireEvent.click(await screen.findByRole('button', { name: key }));
  return screen.getByLabelText('English text');
}

describe('text page', () => {
  it('edits one English string, refuses a broken edit and saves only the changed line', async () => {
    renderWithProviders(<TextPage />);
    await screen.findByLabelText('Search strings', undefined, { timeout: 15000 });
    const original = readRepositoryFile(STRINGS_FILE);

    const textArea = await openString('stat.fluxCapacity');
    expect(textArea).toHaveProperty('value', 'flux capacity');
    fireEvent.change(textArea, { target: { value: 'flux {bogus}' } });
    expect(screen.getByText(/The game never fills \{bogus\}/)).toBeTruthy();
    expect(screen.getByText('Unsaved changes')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Save to project' }));
    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText(/stat\.fluxCapacity: The game never fills/)).toBeTruthy();
    fireEvent.keyDown(refusal, { key: 'Escape' });
    expect(server.readProjectFile(STRINGS_FILE)).toBe(original);

    fireEvent.change(screen.getByLabelText('English text'), { target: { value: 'flux <hl>reservoir</hl>' } });
    expect(screen.getByTestId('template-preview').textContent).toBe('flux reservoir');
    fireEvent.click(screen.getByRole('button', { name: 'Save to project' }));
    await waitFor(() => expect(screen.getByText('Saved')).toBeTruthy());
    expect(server.readProjectFile(STRINGS_FILE)).toBe(original.replace('"stat.fluxCapacity": "flux capacity"', '"stat.fluxCapacity": "flux <hl>reservoir</hl>"'));
  });

  it('keeps edits made while a save is in flight unsaved and writes them on the next save', async () => {
    renderWithProviders(<TextPage />);
    await screen.findByLabelText('Search strings', undefined, { timeout: 15000 });
    const original = readRepositoryFile(STRINGS_FILE);
    const serverFetch = globalThis.fetch;
    const saveGate: { release?: () => void } = {};
    const saveReleased = new Promise<void>((resolve) => {
      saveGate.release = resolve;
    });
    let saveStarted = false;
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      if (init?.method === 'POST' && String(input) === '/api/files') {
        saveStarted = true;
        await saveReleased;
      }
      return serverFetch(input, init);
    });

    try {
      fireEvent.change(await openString('stat.acceleration'), { target: { value: 'quickening' } });
      fireEvent.click(screen.getByRole('button', { name: 'Save to project' }));
      await waitFor(() => expect(saveStarted).toBe(true));
      fireEvent.change(await openString('stat.armor'), { target: { value: 'plating' } });
      saveGate.release?.();
      await screen.findByText('Saved 1 string to en.json');
      expect(server.readProjectFile(STRINGS_FILE)).toContain('"stat.acceleration": "quickening"');
      expect(screen.getByText('Unsaved changes')).toBeTruthy();
      expect(server.readProjectFile(STRINGS_FILE)).toContain('"stat.armor": "armor"');

      const saveButton = screen.getByRole('button', { name: 'Save to project' });
      await waitFor(() => expect(saveButton).toHaveProperty('disabled', false));
      fireEvent.click(saveButton);
      await screen.findByText('Saved');
    } finally {
      vi.stubGlobal('fetch', serverFetch);
    }
    expect(server.readProjectFile(STRINGS_FILE)).toBe(
      original
        .replace('"stat.fluxCapacity": "flux capacity"', '"stat.fluxCapacity": "flux <hl>reservoir</hl>"')
        .replace('"stat.acceleration": "acceleration"', '"stat.acceleration": "quickening"')
        .replace('"stat.armor": "armor"', '"stat.armor": "plating"'),
    );
  });

  it('asks for an in-game preview of the chosen node type', async () => {
    renderWithProviders(<TextPage />);
    const preview = await screen.findByTestId('tooltip-preview', undefined, { timeout: 15000 });
    await waitFor(() => expect(preview.textContent).toBe('Preparing in-game descriptions...'), { timeout: 5000 });
    expect(screen.getByRole('textbox', { name: 'Preview a node type' })).toHaveProperty('value', expect.stringMatching(/\(\w+\)$/));
  });
});
