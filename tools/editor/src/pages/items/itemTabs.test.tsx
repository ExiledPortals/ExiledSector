import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { findLabelled } from '../../test/labelledQueries.ts';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';
import { ImplicitsTab } from './tabs/ImplicitsTab.tsx';
import { ModifiersTab } from './tabs/ModifiersTab.tsx';

const CONFIG_DIR = 'data/config/exiledSector';
const MODIFIERS_FILE = `${CONFIG_DIR}/item_modifiers.csv`;
const HULL_ITEMS_FILE = `${CONFIG_DIR}/hull_items.csv`;
const SUBROUTINES_FILE = `${CONFIG_DIR}/subroutines.csv`;
const IMPLICITS_FILE = `${CONFIG_DIR}/hull_item_implicits.csv`;
const CHART_FILES = ['star_charts.csv', 'star_chart_modifiers.csv', 'star_chart_implicits.csv'].map((name) => `${CONFIG_DIR}/${name}`);
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));

let server: TestServer;

beforeAll(() => {
  server = startTestServer(['tools/editor_vocabulary.json', MODIFIERS_FILE, SUBROUTINES_FILE, HULL_ITEMS_FILE, IMPLICITS_FILE, ...CHART_FILES]);
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
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

async function openSpecialistCrew() {
  renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
  fireEvent.click(await screen.findByText('Specialist Crew', undefined, { timeout: 30000 }));
  await screen.findAllByDisplayValue('Specialist Crew', undefined, { timeout: 30000 });
}

describe('hull item tabs', () => {
  it('loads the definitions with their modifier references resolved', async () => {
    await openSpecialistCrew();

    expect((await screen.findAllByDisplayValue('CREW_LOSS_PERCENT', undefined, { timeout: 30000 })).length).toBeGreaterThan(0);
    expect(screen.queryByText('Not in item_modifiers.csv')).toBeNull();
  }, 60000);

  it('edits a modifier value from a definition and saves it to item_modifiers.csv', async () => {
    await openSpecialistCrew();
    const maxInput = await findLabelled('CREW_LOSS_PERCENT max');
    const subroutinesBefore = server.readProjectFile(SUBROUTINES_FILE);
    const hullItemsBefore = server.readProjectFile(HULL_ITEMS_FILE);
    const modifiersBefore = server.readProjectFile(MODIFIERS_FILE);

    expect(screen.getAllByText(/CREW_LOSS_PERCENT is shared: editing its values also changes unique_olinadu_security/).length).toBeGreaterThan(0);
    fireEvent.change(maxInput, { target: { value: '-9' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved subroutines.csv, hull_items.csv and item_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(
      modifiersBefore.replace('CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-10,Sheltered', 'CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-9,Sheltered'),
    );
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(subroutinesBefore);
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(hullItemsBefore);
  }, 90000);

  it('copies a shared modifier for one item so its values can be tuned alone', async () => {
    await openSpecialistCrew();
    await findLabelled('CREW_LOSS_PERCENT max');

    fireEvent.click(screen.getAllByText('Copy for this item')[0].closest('button') as HTMLButtonElement);
    await waitFor(() => expect(screen.getAllByDisplayValue('CREW_LOSS_PERCENT_CREW_QUARTERS_SPECIALISTS').length).toBeGreaterThan(0));
    fireEvent.click(saveButton());
    await screen.findByText('Saved subroutines.csv, hull_items.csv and item_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(MODIFIERS_FILE)).toContain('CREW_LOSS_PERCENT_CREW_QUARTERS_SPECIALISTS,prefix,hull_item,CREW_LOSS_PERCENT:-15:');
    const hullItemLines = server.readProjectFile(HULL_ITEMS_FILE).split('\n');
    expect(hullItemLines.find((line) => line.startsWith('crew_quarters_specialists,'))).toContain('@CREW_LOSS_PERCENT_CREW_QUARTERS_SPECIALISTS:1000');
    expect(hullItemLines.find((line) => line.startsWith('unique_olinadu_security,'))).toContain('@CREW_LOSS_PERCENT,');
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
        const lines = modifierLines.map((line) => (line.effect === 'SUPPLIES_PER_MONTH_MULT' ? '<good>fewer</good> supplies per month' : null));
        return new Response(JSON.stringify({ lines }), { status: 200, headers: { 'content-type': 'application/json' } });
      }
      return serverFetch(input, init);
    });
    try {
      await openSpecialistCrew();

      fireEvent.click(screen.getByText('View'));

      const highlighted = await screen.findAllByText('fewer');
      expect(highlighted[0].parentElement?.textContent).toBe('fewer supplies per month');
      expect(screen.getAllByText('Thrifty').length).toBeGreaterThan(0);
      expect(screen.getAllByText('750').length).toBeGreaterThan(0);
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
    await selectModifier('SUPPLIES_PER_MONTH_MULT_CREW_QUARTERS');
    const before = server.readProjectFile(MODIFIERS_FILE);

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: '' } });
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText(/SUPPLIES_PER_MONTH_MULT_CREW_QUARTERS: .* can roll it, so it needs a name/)).toBeTruthy();
    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(before);
  });

  it('adds a subroutine with a kind and no prefixes or suffixes and saves it to subroutines.csv', async () => {
    const subroutinesBefore = server.readProjectFile(SUBROUTINES_FILE);
    const hullItemsBefore = server.readProjectFile(HULL_ITEMS_FILE);
    renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
    await screen.findAllByDisplayValue('Coatl Bastion Gunnery Team', undefined, { timeout: 30000 });

    fireEvent.click(screen.getByText('New item').closest('button') as HTMLButtonElement);
    expect(await findLabelled('Subroutine kind')).toHaveProperty('value', 'VOLUME');
    expect(screen.queryByText('Add prefix')).toBeNull();
    fireEvent.click(saveButton());
    await screen.findByText('Saved subroutines.csv and hull_items.csv', undefined, { timeout: 30000 });

    const lineEnding = subroutinesBefore.includes('\r\n') ? '\r\n' : '\n';
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(`${subroutinesBefore}new_subroutine,New Subroutine,,10,,VOLUME,,${lineEnding}`);
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(hullItemsBefore);
  }, 90000);
});
