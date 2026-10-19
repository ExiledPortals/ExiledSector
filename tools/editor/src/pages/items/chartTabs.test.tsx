import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { StrictMode } from 'react';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { findLabelled } from '../../test/labelledQueries.ts';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { CHART_DROP_SPEC, CHART_POI_SPEC, CHART_TIER_SPEC } from './model/chartTables.ts';
import { ChartTableTab } from './tabs/ChartTableTab.tsx';
import { CraftingTab } from './tabs/CraftingTab.tsx';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';
import { ImplicitsTab } from './tabs/ImplicitsTab.tsx';
import { ModifiersTab } from './tabs/ModifiersTab.tsx';
import { NamesTab } from './tabs/NamesTab.tsx';

const CONFIG_DIR = 'data/config/exiledSector';
const config = (name: string) => `${CONFIG_DIR}/${name}`;
const STAR_CHARTS_FILE = config('star_charts.csv');
const CHART_MODIFIERS_FILE = config('star_chart_modifiers.csv');
const CHART_IMPLICITS_FILE = config('star_chart_implicits.csv');
const CHART_NAMES_FILE = config('star_chart_names.json');
const CRAFTING_FILE = config('item_crafting.csv');
const SPECIAL_ITEMS_FILE = 'data/campaign/special_items.csv';
const TIERS_FILE = config('star_chart_tiers.csv');
const DROPS_FILE = config('chart_drops.csv');
const POIS_FILE = config('chart_poi.csv');
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));

let server: TestServer;

beforeAll(() => {
  server = startTestServer([
    'tools/editor_vocabulary.json',
    STAR_CHARTS_FILE,
    CHART_MODIFIERS_FILE,
    CHART_IMPLICITS_FILE,
    CHART_NAMES_FILE,
    CRAFTING_FILE,
    SPECIAL_ITEMS_FILE,
    TIERS_FILE,
    DROPS_FILE,
    POIS_FILE,
    ...['item_modifiers.csv', 'subroutines.csv', 'hull_items.csv', 'hull_item_implicits.csv', 'item_names.json'].map(config),
  ]);
});

afterAll(async () => {
  await server.close();
});

function saveButton() {
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

async function switchToStarCharts() {
  fireEvent.click(await findLabelled('Star charts'));
}

describe('star chart editing', () => {
  it('edits a star chart definition and saves only star_charts.csv', async () => {
    renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
    await screen.findAllByDisplayValue('Military Domain Subroutine', undefined, { timeout: 30000 });
    await switchToStarCharts();

    expect((await screen.findAllByDisplayValue('t3+', undefined, { timeout: 30000 })).length).toBeGreaterThan(0);
    expect(screen.queryByText(/Not in star_chart_modifiers.csv/)).toBeNull();
    fireEvent.click(screen.getByText("The Warden's Vigil"));
    const minTier = await findLabelled('Minimum tier');
    expect(minTier).toHaveProperty('value', '4');
    const before = server.readProjectFile(STAR_CHARTS_FILE);

    fireEvent.change(minTier, { target: { value: '5' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved star_charts.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(STAR_CHARTS_FILE)).toBe(before.replace("The Warden's Vigil,graphics/icons/attuned_sensors.png,1,true,4,", "The Warden's Vigil,graphics/icons/attuned_sensors.png,1,true,5,"));
  }, 90000);

  it('edits a star chart description under StrictMode', async () => {
    renderWithProviders(
      <StrictMode>
        <DefinitionsTab vocabulary={vocabulary} />
      </StrictMode>,
    );
    await screen.findAllByDisplayValue('Military Domain Subroutine', undefined, { timeout: 30000 });
    await switchToStarCharts();
    fireEvent.click(await screen.findByText("The Warden's Vigil", undefined, { timeout: 30000 }));
    const description = (await findLabelled('Tooltip description (optional)')) as HTMLTextAreaElement;
    const edited = `${description.value} Edited.`;

    fireEvent.change(description, { target: { value: edited } });

    expect(description.value).toBe(edited);
  }, 90000);

  it('edits a tier band of a star chart modifier', async () => {
    renderWithProviders(<ModifiersTab vocabulary={vocabulary} />);
    await screen.findByLabelText('Search modifiers', undefined, { timeout: 30000 });
    await switchToStarCharts();
    await waitFor(() => expect(screen.getAllByText('HYBRID_CHART_FLEET_SIZE', { selector: 'p' }).length).toBeGreaterThan(0), { timeout: 30000 });

    fireEvent.change(screen.getByLabelText('Search modifiers'), { target: { value: 'CHART_REWARD_PERCENT' } });
    fireEvent.click((await screen.findAllByText('CHART_REWARD_PERCENT', { selector: 'p' }))[0]);
    const bandMax = await screen.findByLabelText('CHART_REWARD_PERCENT band 1 max');
    const before = server.readProjectFile(CHART_MODIFIERS_FILE);

    fireEvent.change(bandMax, { target: { value: '22' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved star_chart_modifiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(CHART_MODIFIERS_FILE)).toBe(before.replace('CHART_REWARD_PERCENT:t1=10-20/', 'CHART_REWARD_PERCENT:t1=10-22/'));
  }, 90000);

  it('shows the star chart system types without implicits and points to the faction table', async () => {
    renderWithProviders(<ImplicitsTab vocabulary={vocabulary} />);
    await screen.findByLabelText('ARMOR_PERCENT max', undefined, { timeout: 30000 });
    await switchToStarCharts();

    await screen.findByText(/first rolls one inhabiting faction by weight from chart_factions\.csv/, undefined, { timeout: 30000 });
    expect(screen.getAllByText('No implicits.').length).toBe(vocabulary.chartSystemTypes.length + 1);
    expect(server.readProjectFile(CHART_IMPLICITS_FILE).trim()).toBe('system_type,implicits');
  }, 90000);

  it('shows and edits the star chart codename words with sample names', async () => {
    renderWithProviders(<NamesTab />);
    await screen.findByText('Add hull item', undefined, { timeout: 30000 });
    await switchToStarCharts();

    const lanternBox = () => screen.queryAllByRole('textbox').find((box) => (box as HTMLTextAreaElement).value.startsWith('Lantern'));
    await waitFor(() => expect(lanternBox()).toBeTruthy(), { timeout: 30000 });
    const firstWords = lanternBox();
    expect(screen.getByText(/^e\.g\. \S+ \S+/)).toBeTruthy();
    const before = server.readProjectFile(CHART_NAMES_FILE);

    fireEvent.change(firstWords as HTMLElement, { target: { value: `${(firstWords as HTMLTextAreaElement).value}\nBeacon` } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved star_chart_names.json', undefined, { timeout: 30000 });

    expect(server.readProjectFile(CHART_NAMES_FILE)).toBe(before.replace('"Corona", "Ember"]', '"Corona", "Ember", "Beacon"]'));
  }, 90000);

  it('lists the ciphers in the Crafting tab with their cost and icon', async () => {
    renderWithProviders(<CraftingTab />);
    const costInputs = await screen.findAllByLabelText('Parts to synthesise', undefined, { timeout: 30000 });
    expect(screen.getByText('Star chart ciphers')).toBeTruthy();
    expect(screen.getByLabelText("Cartographer's Cipher - image")).toHaveProperty('value', 'graphics/icons/cargo/transposition_kernel.png');
    const cartographerCost = costInputs.find((input) => (input as HTMLInputElement).value === '120');
    expect(cartographerCost).toBeTruthy();
    const craftingBefore = server.readProjectFile(CRAFTING_FILE);
    const specialItemsBefore = server.readProjectFile(SPECIAL_ITEMS_FILE);

    fireEvent.change(cartographerCost as HTMLElement, { target: { value: '150' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved item_crafting.csv and special_items.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(CRAFTING_FILE)).toBe(craftingBefore.replace('exiledSector_cartographer_cipher,120', 'exiledSector_cartographer_cipher,150'));
    expect(server.readProjectFile(SPECIAL_ITEMS_FILE)).toBe(specialItemsBefore);
  }, 90000);

  it('edits a tier and keeps the quoted notes', async () => {
    renderWithProviders(<ChartTableTab spec={CHART_TIER_SPEC} vocabulary={vocabulary} />);
    const fleetSize = await findLabelled('1 fleet_size');
    const before = server.readProjectFile(TIERS_FILE);

    fireEvent.change(fleetSize, { target: { value: '110' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved star_chart_tiers.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(TIERS_FILE)).toBe(before.replace('1,100,2,3,0,0,30,50,80,6,no,0,1,0,0,"A couple', '1,110,2,3,0,0,30,50,80,6,no,0,1,0,0,"A couple'));
  }, 90000);

  it('warns before saving a cipher chance on a drop source outside chart systems', async () => {
    renderWithProviders(<ChartTableTab spec={CHART_DROP_SPEC} vocabulary={vocabulary} />);
    const battleCipher = await findLabelled('battle currency_chance');
    const before = server.readProjectFile(DROPS_FILE);

    fireEvent.change(battleCipher, { target: { value: '5' } });
    expect(await screen.findAllByText(/battle is outside a chart system, where chart currencies never drop/)).toHaveLength(1);
    fireEvent.click(saveButton());
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/battle is outside a chart system/)).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save anyway' }));
    await screen.findByText('Saved chart_drops.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(DROPS_FILE)).toBe(before.replace('battle,0.01,0,0,0,150', 'battle,0.01,0,0,0.05,150'));
  }, 90000);

  it('refuses to save a point of interest without an entity type', async () => {
    renderWithProviders(<ChartTableTab spec={CHART_POI_SPEC} vocabulary={vocabulary} />);
    const entity = await findLabelled('derelict_small entity');
    const before = server.readProjectFile(POIS_FILE);

    fireEvent.change(entity, { target: { value: '' } });
    expect(await screen.findByText('1 problem blocks saving')).toBeTruthy();
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('derelict_small: needs an entity type')).toBeTruthy();
    expect(server.readProjectFile(POIS_FILE)).toBe(before);
  }, 90000);
});
