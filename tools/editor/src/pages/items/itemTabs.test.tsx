import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';
import { ImplicitsTab } from './tabs/ImplicitsTab.tsx';
import { ModifiersTab } from './tabs/ModifiersTab.tsx';

const CONFIG_DIR = 'data/config/exiledSector';
const MODIFIERS_FILE = `${CONFIG_DIR}/item_modifiers.csv`;
const HULL_ITEMS_FILE = `${CONFIG_DIR}/hull_items.csv`;
const SUBROUTINES_FILE = `${CONFIG_DIR}/subroutines.csv`;
const IMPLICITS_FILE = `${CONFIG_DIR}/hull_item_implicits.csv`;
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));

let server: TestServer;

beforeAll(() => {
  server = startTestServer(['tools/editor_vocabulary.json', MODIFIERS_FILE, SUBROUTINES_FILE, HULL_ITEMS_FILE, IMPLICITS_FILE]);
});

afterAll(async () => {
  await server.close();
});

async function renderModifiersTab() {
  renderWithProviders(<ModifiersTab vocabulary={vocabulary} />);
  await screen.findByLabelText('Search modifiers', undefined, { timeout: 15000 });
}

async function selectModifier(modifierId: string) {
  fireEvent.change(screen.getByLabelText('Search modifiers'), { target: { value: modifierId } });
  const matches = await screen.findAllByText(modifierId, { selector: 'p' });
  fireEvent.click(matches[0]);
  await waitFor(() => expect(screen.getByLabelText(/^Id/)).toHaveProperty('value', modifierId));
}

function saveButton() {
  return screen.getByRole('button', { name: 'Save to project' });
}

describe('hull item tabs', () => {
  it('loads the definitions with their modifier references resolved', async () => {
    renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);

    expect((await screen.findAllByDisplayValue('FLUX_CAPACITY_MULT', undefined, { timeout: 30000 })).length).toBeGreaterThan(0);
    expect(screen.queryByText('Not in item_modifiers.csv')).toBeNull();
  }, 60000);

  it('edits a modifier value from a definition and saves it to item_modifiers.csv', async () => {
    renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
    const maxInput = await screen.findByLabelText('FLUX_CAPACITY_MULT max', undefined, { timeout: 30000 });
    const subroutinesBefore = server.readProjectFile(SUBROUTINES_FILE);
    const modifiersBefore = server.readProjectFile(MODIFIERS_FILE);

    expect(screen.getAllByText(/FLUX_CAPACITY_MULT is shared: editing its values also changes/).length).toBeGreaterThan(0);
    fireEvent.change(maxInput, { target: { value: '7' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved subroutines.csv, hull_items.csv and item_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(
      modifiersBefore.replace('FLUX_CAPACITY_MULT,prefix,subroutine,FLUX_CAPACITY_MULT:4:6,Capacitive', 'FLUX_CAPACITY_MULT,prefix,subroutine,FLUX_CAPACITY_MULT:4:7,Capacitive'),
    );
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(subroutinesBefore);
  }, 90000);

  it('copies a shared modifier for one item so its values can be tuned alone', async () => {
    renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
    await screen.findByLabelText('FLUX_CAPACITY_MULT max', undefined, { timeout: 30000 });

    fireEvent.click(screen.getAllByRole('button', { name: 'Copy for this item' })[0]);
    await waitFor(() => expect(screen.getAllByDisplayValue('FLUX_CAPACITY_MULT_DOMAIN_SUBROUTINE_MILITARY').length).toBeGreaterThan(0));
    fireEvent.click(saveButton());
    await screen.findByText('Saved subroutines.csv, hull_items.csv and item_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toContain('FLUX_CAPACITY_MULT_DOMAIN_SUBROUTINE_MILITARY,prefix,subroutine,FLUX_CAPACITY_MULT:4:');
    const military = server.readProjectFile(SUBROUTINES_FILE).split('\n').find((line) => line.startsWith('domain_subroutine_military,'));
    expect(military).toContain('@FLUX_CAPACITY_MULT_DOMAIN_SUBROUTINE_MILITARY:1500');
    const industrial = server.readProjectFile(SUBROUTINES_FILE).split('\n').find((line) => line.startsWith('domain_subroutine_industrial,'));
    expect(industrial).toContain('@FLUX_CAPACITY_MULT:1500');
  }, 90000);

  it('edits an implicit value from the Implicits tab and saves it to item_modifiers.csv', async () => {
    renderWithProviders(<ImplicitsTab vocabulary={vocabulary} />);
    const maxInput = await screen.findByLabelText('ARMOR_PERCENT max', undefined, { timeout: 30000 });
    const implicitsBefore = server.readProjectFile(IMPLICITS_FILE);
    const modifiersBefore = server.readProjectFile(MODIFIERS_FILE);

    expect(screen.queryByRole('button', { name: 'Copy for this section' })).toBeNull();
    fireEvent.change(maxInput, { target: { value: '12' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved hull_item_implicits.csv and item_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(
      modifiersBefore.replace('ARMOR_PERCENT_IMPLICIT,implicit,hull_item,ARMOR_PERCENT:5:10,', 'ARMOR_PERCENT_IMPLICIT,implicit,hull_item,ARMOR_PERCENT:5:12,'),
    );
    expect(server.readProjectFile(IMPLICITS_FILE)).toBe(implicitsBefore);
  }, 90000);

  it('view mode lists each modifier as its in-game description with its weight and share', async () => {
    const serverFetch = globalThis.fetch;
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input) === '/api/describe') {
        const { modifierLines } = JSON.parse(String(init?.body)) as { modifierLines: { effect: string }[] };
        const lines = modifierLines.map((line) => (line.effect === 'FLUX_CAPACITY_MULT' ? '+<good>4%-6%</good> flux capacity' : null));
        return new Response(JSON.stringify({ lines }), { status: 200, headers: { 'content-type': 'application/json' } });
      }
      return serverFetch(input, init);
    });
    try {
      renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
      await screen.findAllByDisplayValue('Military Domain Subroutine', undefined, { timeout: 30000 });

      fireEvent.click(screen.getByText('View'));

      const highlighted = await screen.findByText('4%-6%');
      expect(highlighted.parentElement?.textContent).toBe('+4%-6% flux capacity');
      expect(screen.getAllByText('Capacitive').length).toBeGreaterThan(0);
      expect(screen.getAllByText('1500').length).toBeGreaterThan(0);
      expect(screen.queryAllByLabelText('Modifier 1')).toEqual([]);
    } finally {
      vi.stubGlobal('fetch', serverFetch);
    }
  }, 90000);

  it('loads the implicits with their modifiers resolved', async () => {
    renderWithProviders(<ImplicitsTab vocabulary={vocabulary} />);

    expect((await screen.findAllByDisplayValue('HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION', undefined, { timeout: 15000 })).length).toBeGreaterThan(0);
    expect(screen.queryByText('Not in item_modifiers.csv')).toBeNull();
  });

  it('renames a modifier and its references, then saves the changed files', async () => {
    const filesBefore = Object.fromEntries([MODIFIERS_FILE, HULL_ITEMS_FILE, IMPLICITS_FILE, SUBROUTINES_FILE].map((file) => [file, server.readProjectFile(file)]));
    await renderModifiersTab();
    await selectModifier('FUEL_USE_MULT_ENGINE_ROOM');

    const idInput = screen.getByLabelText(/^Id/);
    fireEvent.change(idInput, { target: { value: 'FUEL_USE_MULT_DRIVE' } });
    fireEvent.blur(idInput);
    fireEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(screen.getByLabelText(/^Id/)).toHaveProperty('value', 'FUEL_USE_MULT_DRIVE'));
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Thrift' } });

    fireEvent.click(saveButton());
    await screen.findByText(/Saved item_modifiers.csv and the renamed references/, undefined, { timeout: 15000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(
      filesBefore[MODIFIERS_FILE].replace(
        'FUEL_USE_MULT_ENGINE_ROOM,suffix,hull_item,FUEL_USE_MULT:-15:-10,Mileage',
        'FUEL_USE_MULT_DRIVE,suffix,hull_item,FUEL_USE_MULT:-15:-10,Thrift',
      ),
    );
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(filesBefore[HULL_ITEMS_FILE].replace('@FUEL_USE_MULT_ENGINE_ROOM', '@FUEL_USE_MULT_DRIVE'));
    expect(server.readProjectFile(IMPLICITS_FILE)).toBe(filesBefore[IMPLICITS_FILE]);
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(filesBefore[SUBROUTINES_FILE]);
  });

  it('refuses to save a modifier that a common hull item rolls without a name', async () => {
    await renderModifiersTab();
    await selectModifier('HULL_MULT');
    const before = server.readProjectFile(MODIFIERS_FILE);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: '' } });
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText(/HULL_MULT: .* can roll it, so it needs a name/)).toBeTruthy();
    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(before);
  });
});
