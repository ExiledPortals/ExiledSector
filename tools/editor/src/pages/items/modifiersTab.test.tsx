import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  HULL_ITEMS_FILE,
  IMPLICITS_FILE,
  LOAD_TIMEOUT,
  MODIFIERS_FILE,
  SUBROUTINES_FILE,
  describeHelper,
  openItems,
  panel,
  readProjectFile,
  readProjectFiles,
  save,
  setUpItemsProject,
  tab,
} from '../../test/itemsPageHarness.ts';
import { findLabelled } from '../../test/labelledQueries.ts';

setUpItemsProject();

describe('hull item modifiers', () => {
  it('finds a modifier by its in-game text, saves an edited value to item_modifiers.csv and shows it on the item straight away', async () => {
    const hullItemsBefore = readProjectFile(HULL_ITEMS_FILE);
    const modifiersBefore = readProjectFile(MODIFIERS_FILE);
    const modifiers = await openItems('Modifiers');

    fireEvent.change(await findLabelled('Search modifiers', modifiers), { target: { value: 'crew loss percent' } });
    fireEvent.click(await findLabelled('Select CREW_LOSS_PERCENT', modifiers));
    fireEvent.change(await findLabelled('CREW_LOSS_PERCENT max', modifiers), { target: { value: '-9' } });
    await waitFor(() => expect(within(modifiers).getAllByText('-15 to -9').length).toBeGreaterThan(0), LOAD_TIMEOUT);

    fireEvent.click(tab('Item types'));
    const itemTypes = panel('Item types');
    fireEvent.click(await within(itemTypes).findByText('Specialist Crew', undefined, LOAD_TIMEOUT));
    expect((await within(itemTypes).findAllByText('-15 to -9', undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);

    await save('Saved item_modifiers.csv');
    expect(readProjectFile(MODIFIERS_FILE)).toBe(
      modifiersBefore.replace('CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-10,Sheltered', 'CREW_LOSS_PERCENT,prefix,hull_item,CREW_LOSS_PERCENT:-15:-9,Sheltered'),
    );
    expect(readProjectFile(HULL_ITEMS_FILE)).toBe(hullItemsBefore);
  }, 90000);

  it('renames a modifier and its references and saves every changed file', async () => {
    const before = readProjectFiles(MODIFIERS_FILE, HULL_ITEMS_FILE, IMPLICITS_FILE, SUBROUTINES_FILE);
    const modifiers = await openItems('Modifiers');
    fireEvent.change(await findLabelled('Search modifiers', modifiers), { target: { value: 'FUEL_USE_MULT_ENGINE_ROOM' } });
    fireEvent.click(await findLabelled('Select FUEL_USE_MULT_ENGINE_ROOM', modifiers));

    const idInput = await findLabelled('Id (saved games store it)', modifiers);
    fireEvent.change(idInput, { target: { value: 'FUEL_USE_MULT_DRIVE' } });
    fireEvent.blur(idInput);
    fireEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(idInput).toHaveProperty('value', 'FUEL_USE_MULT_DRIVE'));
    await save('Saved hull_items.csv and item_modifiers.csv');

    expect(readProjectFile(MODIFIERS_FILE)).toBe(before[MODIFIERS_FILE].replace('FUEL_USE_MULT_ENGINE_ROOM,suffix', 'FUEL_USE_MULT_DRIVE,suffix'));
    expect(readProjectFile(HULL_ITEMS_FILE)).toBe(before[HULL_ITEMS_FILE].replace('@FUEL_USE_MULT_ENGINE_ROOM', '@FUEL_USE_MULT_DRIVE'));
    expect(readProjectFile(IMPLICITS_FILE)).toBe(before[IMPLICITS_FILE]);
    expect(readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE]);
  }, 90000);

  it('keeps retrying when the description helper fails and shows the in-game text once it answers', async () => {
    describeHelper.fails = true;
    const modifiers = await openItems('Modifiers');

    expect((await within(modifiers).findAllByText(/stopped responding.*Retrying/, undefined, LOAD_TIMEOUT)).length).toBeGreaterThan(0);
    expect(within(modifiers).getAllByText('ARMOR_PERCENT 5 to 10').length).toBeGreaterThan(0);
    const failedCalls = describeHelper.calls;
    describeHelper.fails = false;
    expect(await within(modifiers).findAllByText('5 to 10', undefined, { timeout: 20000 })).not.toHaveLength(0);
    expect(describeHelper.calls).toBeGreaterThan(failedCalls);
    expect(within(modifiers).queryByText(/Retrying/)).toBeNull();
  }, 90000);
});
