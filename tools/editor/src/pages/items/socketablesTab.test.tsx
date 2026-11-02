import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  ANCHORS_FILE,
  DOCTRINES_FILE,
  HULL_ITEMS_FILE,
  LOAD_TIMEOUT,
  SUBROUTINES_FILE,
  openItems,
  readProjectFile,
  readProjectFiles,
  save,
  saveButton,
  setUpItemsProject,
} from '../../test/itemsPageHarness.ts';
import { findLabelled } from '../../test/labelledQueries.ts';

setUpItemsProject();

describe('skill tree socketables', () => {
  it('saves a unique subroutine its edited drop source to subroutines.csv only', async () => {
    const before = readProjectFiles(SUBROUTINES_FILE, HULL_ITEMS_FILE);
    const subroutines = await openItems('Skill Tree Socketables', 'Subroutines');

    fireEvent.click(await within(subroutines).findByText('P-Space Lattice', undefined, LOAD_TIMEOUT));
    expect(within(subroutines).queryByText('Hegemony Doctrine Archive')).toBeNull();
    fireEvent.change(await findLabelled('Drop source', subroutines), { target: { value: 'faction:omega; site:station_research' } });
    await save('Saved subroutines.csv');

    const lattice = readProjectFile(SUBROUTINES_FILE).split(/\r?\n/).find((line) => line.startsWith('unique_p_space_lattice,'));
    expect(lattice).toContain(',GATES_RADIUS_WHILE_PHASED:3:3,faction:omega; site:station_research,');
    expect(readProjectFile(HULL_ITEMS_FILE)).toBe(before[HULL_ITEMS_FILE]);
  }, 90000);

  it('gives a unique subroutine a radius and saves it to subroutines.csv', async () => {
    const subroutines = await openItems('Skill Tree Socketables', 'Subroutines');
    fireEvent.click(await within(subroutines).findByText('P-Space Lattice', undefined, LOAD_TIMEOUT));

    fireEvent.click(await findLabelled('Radius', subroutines));
    fireEvent.click(await screen.findByRole('option', { name: 'large' }, LOAD_TIMEOUT));
    await save('Saved subroutines.csv');

    const lines = readProjectFile(SUBROUTINES_FILE).split(/\r?\n/);
    expect(lines[0]).toMatch(/,radius,volume$/);
    expect(lines.find((line) => line.startsWith('unique_p_space_lattice,'))).toMatch(/,large,$/);
  }, 90000);

  it('edits a doctrine archive row and its subroutine together and refuses line counts out of order', async () => {
    const before = readProjectFiles(DOCTRINES_FILE, SUBROUTINES_FILE);
    const doctrines = await openItems('Skill Tree Socketables', 'Doctrine Archives');

    expect((await within(doctrines).findAllByText('AUGMENT - large', undefined, LOAD_TIMEOUT)).length).toBe(4);
    expect(within(doctrines).getAllByText('TRANSFORM - large').length).toBe(3);
    fireEvent.click(within(doctrines).getByText('Hegemony Doctrine Archive'));
    fireEvent.change(await findLabelled('Max lines per node', doctrines), { target: { value: '3' } });
    fireEvent.change(await findLabelled('Name', doctrines), { target: { value: 'Hegemony Battle Doctrine' } });
    await save('Saved subroutines.csv and doctrine_archives.csv');

    expect(readProjectFile(DOCTRINES_FILE)).toBe(before[DOCTRINES_FILE].replace('@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,2', '@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,3'));
    expect(readProjectFile(SUBROUTINES_FILE)).toBe(before[SUBROUTINES_FILE].replace('Hegemony Doctrine Archive,', 'Hegemony Battle Doctrine,'));

    fireEvent.change(await findLabelled('Min lines per node', doctrines), { target: { value: '5' } });
    fireEvent.click(saveButton());
    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('doctrine_archive_hegemony: min lines must be 1 or more and max lines at least min lines')).toBeTruthy();
  }, 90000);

  it('saves an edited wormhole anchor to wormhole_anchors.csv only and refuses a repeated id', async () => {
    const before = readProjectFile(ANCHORS_FILE);
    const anchors = await openItems('Skill Tree Socketables', 'Wormhole Anchors');

    expect(await within(anchors).findByText('exotech - exoship', undefined, LOAD_TIMEOUT)).toBeTruthy();
    fireEvent.click(within(anchors).getByText('Hyper-Nav Beacon'));
    expect(await findLabelled('Exit node', anchors)).toHaveProperty('value', 'exoship');
    fireEvent.change(await findLabelled('Name', anchors), { target: { value: 'Portable Hyper-Nav Beacon' } });
    await save('Saved wormhole_anchors.csv');
    expect(readProjectFile(ANCHORS_FILE)).toBe(before.replace(',Hyper-Nav Beacon,', ',Portable Hyper-Nav Beacon,'));

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
