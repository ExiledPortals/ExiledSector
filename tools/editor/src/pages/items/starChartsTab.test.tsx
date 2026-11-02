import { fireEvent, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import {
  CHART_MODIFIERS_FILE,
  STAR_CHARTS_FILE,
  openItems,
  readProjectFile,
  readProjectFiles,
  save,
  setUpItemsProject,
} from '../../test/itemsPageHarness.ts';
import { findLabelled, getLabelled, queryLabelled } from '../../test/labelledQueries.ts';

setUpItemsProject();

describe('star charts', () => {
  it('lists every chart roll as its in-game text and saves edited values and weights', async () => {
    const before = readProjectFiles(CHART_MODIFIERS_FILE, STAR_CHARTS_FILE);
    const charts = await openItems('Star Charts');

    fireEvent.click(await findLabelled('Select CHART_FLEET_SIZE_PERCENT', charts));
    expect(within(charts).getAllByText('low 15 to 20, mid 50 to 70, high 150 to 200').length).toBeGreaterThan(0);
    fireEvent.change(await findLabelled('CHART_FLEET_SIZE_PERCENT low max', charts), { target: { value: '25' } });
    fireEvent.change(await findLabelled('star_chart weight', charts), { target: { value: '12' } });
    await save('Saved star_charts.csv and star_chart_modifiers.csv');

    expect(readProjectFile(CHART_MODIFIERS_FILE)).toBe(before[CHART_MODIFIERS_FILE].replace('CHART_FLEET_SIZE_PERCENT:low=15-20/', 'CHART_FLEET_SIZE_PERCENT:low=15-25/'));
    expect(readProjectFile(STAR_CHARTS_FILE)).toBe(before[STAR_CHARTS_FILE].replace('@CHART_FLEET_SIZE_PERCENT:10;', '@CHART_FLEET_SIZE_PERCENT:12;'));
  }, 90000);

  it('switches tier groups on and off as a run ending at high', async () => {
    const before = readProjectFile(CHART_MODIFIERS_FILE);
    const charts = await openItems('Star Charts');

    fireEvent.click(await findLabelled('Select CHART_FLEET_SIZE_PERCENT', charts));
    expect(within(charts).getAllByText('Tiers 1-5 (low)').length).toBe(1);
    expect(within(charts).getAllByText('Tiers 11-16 (high)').length).toBe(1);
    const highCheckbox = (await findLabelled('CHART_FLEET_SIZE_PERCENT high group', charts)) as HTMLInputElement;
    expect(highCheckbox.checked).toBe(true);
    expect(highCheckbox.disabled).toBe(true);
    const lowMax = (getLabelled('CHART_FLEET_SIZE_PERCENT low max', charts) as HTMLInputElement).value;
    const midMin = (getLabelled('CHART_FLEET_SIZE_PERCENT mid min', charts) as HTMLInputElement).value;

    fireEvent.click(await findLabelled('CHART_FLEET_SIZE_PERCENT mid group', charts));
    await waitFor(() => expect(queryLabelled('CHART_FLEET_SIZE_PERCENT low max', charts)).toBeNull());
    expect(queryLabelled('CHART_FLEET_SIZE_PERCENT mid max', charts)).toBeNull();
    expect((getLabelled('CHART_FLEET_SIZE_PERCENT low group', charts) as HTMLInputElement).checked).toBe(false);
    expect(within(charts).getAllByText(/Rolls from tier 11/).length).toBeGreaterThan(0);

    fireEvent.click(await findLabelled('CHART_FLEET_SIZE_PERCENT low group', charts));
    await waitFor(() => expect(getLabelled('CHART_FLEET_SIZE_PERCENT low max', charts)).toHaveProperty('value', lowMax));
    expect(getLabelled('CHART_FLEET_SIZE_PERCENT mid min', charts)).toHaveProperty('value', midMin);
    expect(readProjectFile(CHART_MODIFIERS_FILE)).toBe(before);
  }, 90000);
});
