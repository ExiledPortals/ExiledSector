import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  HULL_ITEMS_FILE,
  LOAD_TIMEOUT,
  MODIFIERS_FILE,
  SUBROUTINES_FILE,
  openItems,
  pickOption,
  readProjectFile,
  readProjectFiles,
  save,
  saveButton,
  setUpItemsProject,
} from '../../test/itemsPageHarness.ts';
import { findLabelled, getLabelled, queryLabelled } from '../../test/labelledQueries.ts';

setUpItemsProject();

describe('hull item types', () => {
  it('lists hull items by section and rarity and shows every modifier that can roll as its in-game text', async () => {
    const itemTypes = await openItems('Item types');

    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    expect((await within(itemTypes).findAllByText('-15 to -10', undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);
    expect(within(itemTypes).getAllByText(/crew loss percent/).length).toBeGreaterThan(0);
    expect(within(itemTypes).getAllByText('-10 to -5').length).toBeGreaterThan(0);
    expect(getLabelled('CREW_LOSS_PERCENT weight', itemTypes)).toHaveProperty('value', '1000');
    expect(queryLabelled('CREW_LOSS_PERCENT max', itemTypes)).toBeNull();

    fireEvent.click(await findLabelled('Section filter', itemTypes));
    fireEvent.click(await screen.findByRole('option', { name: /^bridge/ }));
    await waitFor(() => expect(within(itemTypes).queryByText('Launch Assembly')).toBeNull());
    expect(within(itemTypes).getByText('Back-Alley Cyber-Surgeon')).toBeTruthy();
    fireEvent.click(await findLabelled('Rarity filter', itemTypes));
    fireEvent.click(await screen.findByRole('option', { name: /^Not unique/ }));
    await waitFor(() => expect(within(itemTypes).queryByText('Back-Alley Cyber-Surgeon')).toBeNull());
  }, 90000);

  it('adds a prefix picked from the in-game texts, edits a weight, removes a suffix and saves hull_items.csv only', async () => {
    const before = readProjectFiles(HULL_ITEMS_FILE, SUBROUTINES_FILE, MODIFIERS_FILE);
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    await within(itemTypes).findAllByText('-15 to -10', undefined, LOAD_TIMEOUT);

    await pickOption(itemTypes, 'Add to prefixes', 'fighter crew loss', /fighter crew loss percent.*FIGHTER_CREW_LOSS_PERCENT$/);
    fireEvent.change(getLabelled('CREW_LOSS_PERCENT weight', itemTypes), { target: { value: '900' } });
    fireEvent.click(getLabelled('Remove SHIP_RECOVERY_CHANCE_BONUS_CREW_QUARTERS', itemTypes));
    await save('Saved hull_items.csv');

    const specialists = readProjectFile(HULL_ITEMS_FILE).split(/\r?\n/).find((line) => line.startsWith('specialist_crew,'));
    expect(specialists).toContain('@CREW_LOSS_PERCENT:900;');
    expect(specialists).toContain('@HYBRID_CREW_STEAL_RANGE_CREW_STEAL_SKELETON_CREW:250; @FIGHTER_CREW_LOSS_PERCENT,');
    expect(specialists).not.toContain('SHIP_RECOVERY_CHANCE_BONUS_CREW_QUARTERS');
    expect(readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE]);
    expect(readProjectFile(MODIFIERS_FILE)).toBe(before[MODIFIERS_FILE]);
  }, 90000);

  it('gives a unique hull item a drop source and saves it to hull_items.csv', async () => {
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Nanoforge Seed', undefined, LOAD_TIMEOUT));

    fireEvent.change(await findLabelled('Drop source', itemTypes), { target: { value: 'faction:pirates' } });
    await save('Saved hull_items.csv');

    const seed = readProjectFile(HULL_ITEMS_FILE).split(/\r?\n/).find((line) => line.startsWith('unique_nanoforge_seed,'));
    expect(seed).toMatch(/,faction:pirates,,,$/);
  }, 90000);
});

describe('hull item validation', () => {
  it('refuses to give a common item a modifier without a name for its common name', async () => {
    const before = readProjectFile(HULL_ITEMS_FILE);
    const itemTypes = await openItems('Item types');
    fireEvent.click(await within(itemTypes).findByText('Launch Assembly', undefined, LOAD_TIMEOUT));

    await pickOption(itemTypes, 'Add to prefixes', 'min crew', /min crew percent.*MIN_CREW_PERCENT$/);
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog', undefined, LOAD_TIMEOUT);
    expect(within(refusal).getByText('MIN_CREW_PERCENT: launch_assembly can roll it, so it needs a name for their common names')).toBeTruthy();
    expect(readProjectFile(HULL_ITEMS_FILE)).toBe(before);
  }, 90000);
});
