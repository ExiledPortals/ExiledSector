import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
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

  it('loads the implicits with their modifiers resolved', async () => {
    renderWithProviders(<ImplicitsTab vocabulary={vocabulary} />);

    expect((await screen.findAllByDisplayValue('HYBRID_FLUX_CAPACITY_FLUX_DISSIPATION', undefined, { timeout: 15000 })).length).toBeGreaterThan(0);
    expect(screen.queryByText('Not in item_modifiers.csv')).toBeNull();
  });

  it('renames a modifier and its references, then saves the changed files', async () => {
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
      readRepositoryFile(MODIFIERS_FILE).replace(
        'FUEL_USE_MULT_ENGINE_ROOM,suffix,hull_item,FUEL_USE_MULT:-15:-10,Mileage',
        'FUEL_USE_MULT_DRIVE,suffix,hull_item,FUEL_USE_MULT:-15:-10,Thrift',
      ),
    );
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(readRepositoryFile(HULL_ITEMS_FILE).replace('@FUEL_USE_MULT_ENGINE_ROOM', '@FUEL_USE_MULT_DRIVE'));
    expect(server.readProjectFile(IMPLICITS_FILE)).toBe(readRepositoryFile(IMPLICITS_FILE));
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(readRepositoryFile(SUBROUTINES_FILE));
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
