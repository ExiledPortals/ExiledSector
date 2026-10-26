import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ModifierLineRequest } from '../../api/describe.ts';
import { findLabelled } from '../../test/labelledQueries.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { startTestServer, type TestServer } from '../../test/testServer.ts';
import { forgetDescriptionsForTests } from './describedLines.ts';
import { ItemsPage } from './ItemsPage.tsx';

const config = (name: string) => `data/config/exiledSector/${name}`;
const HULL_ITEMS_FILE = config('hull_items.csv');
const SUBROUTINES_FILE = config('subroutines.csv');
const MODIFIERS_FILE = config('item_modifiers.csv');
const CHART_MODIFIERS_FILE = config('star_chart_modifiers.csv');
const STAR_CHARTS_FILE = config('star_charts.csv');
const DOCTRINES_FILE = config('doctrine_archives.csv');
const ANCHORS_FILE = config('wormhole_anchors.csv');
const IMPLICITS_FILE = config('hull_item_implicits.csv');
const LOAD_TIMEOUT = { timeout: 30000 };

let server: TestServer;
let serverFetch: typeof fetch;
let describeCalls = 0;
let describeFails = false;

function describedText(line: ModifierLineRequest): string {
  const values = line.tierBands
    ? line.tierBands.map((band) => `t${band.tier} ${band.min} to ${band.max}`).join(', ')
    : line.hullValues
      ? line.hullValues.join('/')
      : `${line.min} to ${line.max}`;
  return `<good>${values}</good> ${line.effect.toLowerCase().replaceAll('_', ' ')}`;
}

beforeAll(() => {
  server = startTestServer([
    'tools/editor_vocabulary.json',
    'data/skilltrees/skill_types.json',
    'data/skilltrees/ship_skill_tree.json',
    ...[
      'subroutines.csv',
      'hull_items.csv',
      'star_charts.csv',
      'item_modifiers.csv',
      'star_chart_modifiers.csv',
      'hull_item_implicits.csv',
      'star_chart_implicits.csv',
      'subroutine_corruption_implicits.csv',
      'hull_item_corruption_implicits.csv',
      'doctrine_archives.csv',
      'wormhole_anchors.csv',
    ].map(config),
  ]);
  serverFetch = globalThis.fetch;
});

beforeEach(() => {
  forgetDescriptionsForTests();
  describeCalls = 0;
  describeFails = false;
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    if (String(input) === '/api/describe') {
      describeCalls++;
      const { modifierLines } = JSON.parse(String(init?.body)) as { modifierLines?: ModifierLineRequest[] };
      if (describeFails) {
        return new Response(JSON.stringify({ error: 'The description helper stopped responding.' }), { status: 200, headers: { 'content-type': 'application/json' } });
      }
      const lines = (modifierLines ?? []).map((line) => (line.effect === 'NOT_DESCRIBABLE' ? null : describedText(line)));
      return new Response(JSON.stringify({ lines }), { status: 200, headers: { 'content-type': 'application/json' } });
    }
    return serverFetch(input, init);
  });
});

afterEach(() => {
  vi.stubGlobal('fetch', serverFetch);
});

afterAll(async () => {
  await server.close();
});

function saveButton() {
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

async function save(message: string | RegExp) {
  fireEvent.click(saveButton());
  await waitFor(() => {
    const refusal = screen.queryByRole('dialog');
    if (refusal) {
      throw new Error(`Not saved: ${refusal.textContent}`);
    }
    screen.getByText(message);
  }, LOAD_TIMEOUT);
}

function panel(tabName: string): HTMLElement {
  return screen.getByRole('tabpanel', { name: tabName });
}

async function openItems(...tabs: string[]): Promise<HTMLElement> {
  renderWithProviders(<ItemsPage />);
  await screen.findByRole('tab', { name: 'Hull Items' }, LOAD_TIMEOUT);
  for (const tab of tabs) {
    fireEvent.click(screen.getByRole('tab', { name: tab }));
  }
  return panel(tabs[tabs.length - 1] ?? 'Item types');
}

async function pickOption(container: HTMLElement, pickerLabel: string, search: string, optionName: RegExp) {
  const picker = await findLabelled(pickerLabel, container);
  fireEvent.click(picker);
  fireEvent.change(picker, { target: { value: search } });
  fireEvent.click(await screen.findByRole('option', { name: optionName }, LOAD_TIMEOUT));
}

describe('hull item types', () => {
  it('lists hull items by socket type and rarity and shows every modifier that can roll as its in-game text', async () => {
    const itemTypes = await openItems('Item types');

    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    expect((await within(itemTypes).findAllByText('-15 to -10', undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);
    expect(within(itemTypes).getAllByText(/crew loss percent/).length).toBeGreaterThan(0);
    expect(within(itemTypes).getAllByText('-10 to -5').length).toBeGreaterThan(0);
    expect(within(itemTypes).getByLabelText('CREW_LOSS_PERCENT weight')).toHaveProperty('value', '1000');
    expect(within(itemTypes).queryByLabelText('CREW_LOSS_PERCENT max')).toBeNull();

    fireEvent.click(await findLabelled('Socket type filter', itemTypes));
    fireEvent.click(await screen.findByRole('option', { name: /^bridge/ }));
    await waitFor(() => expect(within(itemTypes).queryByText('Deck Crew')).toBeNull());
    expect(within(itemTypes).getByText('Back-Alley Cyber-Surgeon')).toBeTruthy();
    fireEvent.click(await findLabelled('Rarity filter', itemTypes));
    fireEvent.click(await screen.findByRole('option', { name: /^Not unique/ }));
    await waitFor(() => expect(within(itemTypes).queryByText('Back-Alley Cyber-Surgeon')).toBeNull());
  }, 90000);

  it('adds a prefix picked from the in-game texts, edits a weight, removes a suffix and saves hull_items.csv only', async () => {
    const before = Object.fromEntries([HULL_ITEMS_FILE, SUBROUTINES_FILE, MODIFIERS_FILE].map((file) => [file, server.readProjectFile(file)]));
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    await within(itemTypes).findAllByText('-15 to -10', undefined, LOAD_TIMEOUT);

    await pickOption(itemTypes, 'Add to prefixes', 'fighter crew loss', /fighter crew loss percent.*FIGHTER_CREW_LOSS_PERCENT$/);
    fireEvent.change(within(itemTypes).getByLabelText('CREW_LOSS_PERCENT weight'), { target: { value: '900' } });
    fireEvent.click(within(itemTypes).getByLabelText('Remove SHIP_RECOVERY_CHANCE_BONUS_CREW_QUARTERS'));
    await save('Saved hull_items.csv');

    const specialists = server.readProjectFile(HULL_ITEMS_FILE).split(/\r?\n/).find((line) => line.startsWith('crew_quarters_specialists,'));
    expect(specialists).toContain('@CREW_LOSS_PERCENT:900;');
    expect(specialists).toContain('@HYBRID_CREW_STEAL_RANGE_CREW_STEAL_SKELETON_CREW:250; @FIGHTER_CREW_LOSS_PERCENT,');
    expect(specialists).not.toContain('SHIP_RECOVERY_CHANCE_BONUS_CREW_QUARTERS');
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE]);
    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(before[MODIFIERS_FILE]);
  }, 90000);

  it('gives a unique hull item a drop source and saves it to hull_items.csv', async () => {
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Nanoforge Seed', undefined, LOAD_TIMEOUT));

    fireEvent.change(await findLabelled('Drop source', itemTypes), { target: { value: 'faction:pirates' } });
    await save('Saved hull_items.csv');

    const seed = server.readProjectFile(HULL_ITEMS_FILE).split(/\r?\n/).find((line) => line.startsWith('unique_nanoforge_seed,'));
    expect(seed).toMatch(/,faction:pirates,,$/);
  }, 90000);
});

describe('hull item validation', () => {
  it('refuses to give a common item a modifier without a name for its common name', async () => {
    const before = server.readProjectFile(HULL_ITEMS_FILE);
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Deck Crew', undefined, LOAD_TIMEOUT));

    await pickOption(itemTypes, 'Add to prefixes', 'min crew', /min crew percent.*MIN_CREW_PERCENT$/);
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog', undefined, LOAD_TIMEOUT);
    expect(within(refusal).getByText('MIN_CREW_PERCENT: flight_deck_crew can roll it, so it needs a name for their common names')).toBeTruthy();
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(before);
  }, 90000);
});

describe('hull item modifiers', () => {
  it('finds a modifier by its in-game text, saves an edited value to item_modifiers.csv and shows it on the item straight away', async () => {
    const hullItemsBefore = server.readProjectFile(HULL_ITEMS_FILE);
    const modifiersBefore = server.readProjectFile(MODIFIERS_FILE);
    const modifiers = await openItems('Modifiers');

    fireEvent.change(await findLabelled('Search modifiers', modifiers), { target: { value: 'crew loss percent' } });
    fireEvent.click(await within(modifiers).findByLabelText('Select CREW_LOSS_PERCENT', undefined, LOAD_TIMEOUT));
    fireEvent.change(await findLabelled('CREW_LOSS_PERCENT max', modifiers), { target: { value: '-9' } });
    await waitFor(() => expect(within(modifiers).getAllByText('-15 to -9').length).toBeGreaterThan(0), LOAD_TIMEOUT);

    fireEvent.click(screen.getByRole('tab', { name: 'Item types' }));
    const itemTypes = panel('Item types');
    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    expect((await within(itemTypes).findAllByText('-15 to -9', undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);

    await save('Saved item_modifiers.csv');
    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(
      modifiersBefore.replace('CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-10,Sheltered', 'CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-9,Sheltered'),
    );
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(hullItemsBefore);
  }, 90000);

  it('renames a modifier and its references and saves every changed file', async () => {
    const before = Object.fromEntries([MODIFIERS_FILE, HULL_ITEMS_FILE, IMPLICITS_FILE, SUBROUTINES_FILE].map((file) => [file, server.readProjectFile(file)]));
    const modifiers = await openItems('Modifiers');
    fireEvent.change(await findLabelled('Search modifiers', modifiers), { target: { value: 'FUEL_USE_MULT_ENGINE_ROOM' } });
    fireEvent.click(await within(modifiers).findByLabelText('Select FUEL_USE_MULT_ENGINE_ROOM', undefined, LOAD_TIMEOUT));

    const idInput = await findLabelled('Id (saved games store it)', modifiers);
    fireEvent.change(idInput, { target: { value: 'FUEL_USE_MULT_DRIVE' } });
    fireEvent.blur(idInput);
    fireEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(idInput).toHaveProperty('value', 'FUEL_USE_MULT_DRIVE'));
    await save('Saved hull_items.csv and item_modifiers.csv');

    expect(server.readProjectFile(MODIFIERS_FILE)).toBe(before[MODIFIERS_FILE].replace('FUEL_USE_MULT_ENGINE_ROOM,suffix', 'FUEL_USE_MULT_DRIVE,suffix'));
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(before[HULL_ITEMS_FILE].replace('@FUEL_USE_MULT_ENGINE_ROOM', '@FUEL_USE_MULT_DRIVE'));
    expect(server.readProjectFile(IMPLICITS_FILE)).toBe(before[IMPLICITS_FILE]);
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE]);
  }, 90000);

  it('keeps retrying when the description helper fails and shows the in-game text once it answers', async () => {
    describeFails = true;
    const modifiers = await openItems('Modifiers');

    expect((await within(modifiers).findAllByText(/stopped responding.*Retrying/, undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);
    expect(within(modifiers).getAllByText('ARMOR_PERCENT 5 to 10').length).toBeGreaterThan(0);
    const failedCalls = describeCalls;
    describeFails = false;
    expect(await within(modifiers).findAllByText('5 to 10', undefined, { timeout: 20000 })).not.toHaveLength(0);
    expect(describeCalls).toBeGreaterThan(failedCalls);
    expect(within(modifiers).queryByText(/Retrying/)).toBeNull();
  }, 90000);
});

describe('star charts', () => {
  it('lists every chart roll as its in-game text and saves edited values and weights', async () => {
    const before = Object.fromEntries([CHART_MODIFIERS_FILE, STAR_CHARTS_FILE].map((file) => [file, server.readProjectFile(file)]));
    const charts = await openItems('Star Charts');

    fireEvent.click(await within(charts).findByLabelText('Select HYBRID_CHART_FLEET_SIZE', undefined, LOAD_TIMEOUT));
    expect(within(charts).getAllByText('t1 15 to 20, t6 50 to 70, t11 150 to 200').length).toBeGreaterThan(0);
    fireEvent.change(await findLabelled('CHART_FLEET_SIZE_PERCENT band 1 max', charts), { target: { value: '25' } });
    fireEvent.change(await findLabelled('star_chart weight', charts), { target: { value: '12' } });
    await save('Saved star_charts.csv and star_chart_modifiers.csv');

    expect(server.readProjectFile(CHART_MODIFIERS_FILE)).toBe(before[CHART_MODIFIERS_FILE].replace('CHART_FLEET_SIZE_PERCENT:t1=15-20/', 'CHART_FLEET_SIZE_PERCENT:t1=15-25/'));
    expect(server.readProjectFile(STAR_CHARTS_FILE)).toBe(before[STAR_CHARTS_FILE].replace('@HYBRID_CHART_FLEET_SIZE:10;', '@HYBRID_CHART_FLEET_SIZE:12;'));
  }, 90000);
});

describe('skill tree socketables', () => {
  it('shows a unique subroutine its lines as in-game text and saves an added line to subroutines.csv only', async () => {
    const before = Object.fromEntries([SUBROUTINES_FILE, HULL_ITEMS_FILE].map((file) => [file, server.readProjectFile(file)]));
    const subroutines = await openItems('Skill Tree Socketables', 'Subroutines');

    fireEvent.click(await within(subroutines).findByText('Halcyon Fire-Control Lattice', undefined, LOAD_TIMEOUT));
    expect((await within(subroutines).findAllByText('10 to 15', undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);
    expect(within(subroutines).queryByText('Hegemony Doctrine Archive')).toBeNull();
    await pickOption(subroutines, 'Add to lines', 'top speed', /TOP_SPEED_PERCENT_CRACKED_GOVERNOR$/);
    await save('Saved subroutines.csv');

    const halcyon = server.readProjectFile(SUBROUTINES_FILE).split(/\r?\n/).find((line) => line.startsWith('unique_halcyon_fire_lattice,'));
    expect(halcyon).toContain('@SENSOR_PROFILE_PERCENT_HALCYON_LATTICE; @TOP_SPEED_PERCENT_CRACKED_GOVERNOR,');
    expect(server.readProjectFile(HULL_ITEMS_FILE)).toBe(before[HULL_ITEMS_FILE]);
  }, 90000);

  it('gives a unique subroutine a radius and saves it to subroutines.csv', async () => {
    const subroutines = await openItems('Skill Tree Socketables', 'Subroutines');
    fireEvent.click(await within(subroutines).findByText('Cracked Drive Governor', undefined, LOAD_TIMEOUT));

    fireEvent.click(await findLabelled('Radius', subroutines));
    fireEvent.click(await screen.findByRole('option', { name: 'medium' }, LOAD_TIMEOUT));
    await save('Saved subroutines.csv');

    const lines = server.readProjectFile(SUBROUTINES_FILE).split(/\r?\n/);
    expect(lines[0]).toMatch(/,radius,area$/);
    expect(lines.find((line) => line.startsWith('unique_cracked_drive_governor,'))).toMatch(/,medium,$/);
  }, 90000);

  it('edits a doctrine archive row and its subroutine together and refuses line counts out of order', async () => {
    const before = Object.fromEntries([DOCTRINES_FILE, SUBROUTINES_FILE].map((file) => [file, server.readProjectFile(file)]));
    const doctrines = await openItems('Skill Tree Socketables', 'Doctrine Archives');

    expect((await within(doctrines).findAllByText('AUGMENT - large', undefined, LOAD_TIMEOUT)).length).toBe(4);
    expect(within(doctrines).getAllByText('TRANSFORM - large').length).toBe(3);
    fireEvent.click(within(doctrines).getByText('Hegemony Doctrine Archive'));
    fireEvent.change(await findLabelled('Max lines per node', doctrines), { target: { value: '3' } });
    fireEvent.change(await findLabelled('Name', doctrines), { target: { value: 'Hegemony Battle Doctrine' } });
    await save('Saved subroutines.csv and doctrine_archives.csv');

    expect(server.readProjectFile(DOCTRINES_FILE)).toBe(before[DOCTRINES_FILE].replace('@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,2', '@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,3'));
    expect(server.readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE].replace('Hegemony Doctrine Archive,', 'Hegemony Battle Doctrine,'));

    fireEvent.change(await findLabelled('Min lines per node', doctrines), { target: { value: '5' } });
    fireEvent.click(saveButton());
    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('doctrine_archive_hegemony: min lines must be 1 or more and max lines at least min lines')).toBeTruthy();
  }, 90000);

  it('saves an edited wormhole anchor to wormhole_anchors.csv only and refuses a repeated id', async () => {
    const before = server.readProjectFile(ANCHORS_FILE);
    const anchors = await openItems('Skill Tree Socketables', 'Wormhole Anchors');

    expect(await within(anchors).findByText('exotech - exoship', undefined, LOAD_TIMEOUT)).toBeTruthy();
    fireEvent.click(within(anchors).getByText('Hyper-Nav Beacon'));
    expect(await findLabelled('Exit node', anchors)).toHaveProperty('value', 'exoship');
    fireEvent.change(await findLabelled('Name', anchors), { target: { value: 'Portable Hyper-Nav Beacon' } });
    await save('Saved wormhole_anchors.csv');
    expect(server.readProjectFile(ANCHORS_FILE)).toBe(before.replace(',Hyper-Nav Beacon,', ',Portable Hyper-Nav Beacon,'));

    fireEvent.click(within(anchors).getByText('Hegemony Wormhole Anchor'));
    const idInput = await findLabelled('Id (saved games store it - renaming orphans owned anchors)', anchors);
    fireEvent.change(idInput, { target: { value: 'wormhole_anchor_tritachyon' } });
    fireEvent.blur(idInput);
    fireEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(idInput).toHaveProperty('value', 'wormhole_anchor_tritachyon'));
    fireEvent.click(saveButton());
    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('wormhole_anchor_tritachyon: id is used more than once')).toBeTruthy();
  }, 90000);
});
