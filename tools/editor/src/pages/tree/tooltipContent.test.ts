import { describe, expect, it } from 'vitest';
import { mergeDescriptions } from './tooltipContent.ts';

const base = { title: 'Armor', flavour: null, expandHint: 'Press F1 for more info.' };

describe('tooltip content', () => {
  it('keeps a single hull size as one unlabelled group', () => {
    const merged = mergeDescriptions([{ label: 'Frigate', described: { ...base, effects: ['Increases armor by <good>15</good>.'], details: [] } }]);
    expect(merged.effects).toEqual([{ label: null, lines: ['Increases armor by <good>15</good>.'] }]);
    expect(merged.details).toEqual([]);
  });

  it('shows lines shared by every hull size once and labels the ones that differ', () => {
    const merged = mergeDescriptions(
      ['Frigate', 'Destroyer', 'Cruiser', 'Capital'].map((label, index) => ({
        label,
        described: { ...base, effects: ['<good>2%</good> more hull.', `Increases armor by <good>${(index + 1) * 15}</good>.`], details: ['Shared detail.'] },
      })),
    );
    expect(merged.title).toBe('Armor');
    expect(merged.effects).toEqual([
      { label: null, lines: ['<good>2%</good> more hull.'] },
      { label: 'Frigate', lines: ['Increases armor by <good>15</good>.'] },
      { label: 'Destroyer', lines: ['Increases armor by <good>30</good>.'] },
      { label: 'Cruiser', lines: ['Increases armor by <good>45</good>.'] },
      { label: 'Capital', lines: ['Increases armor by <good>60</good>.'] },
    ]);
    expect(merged.details).toEqual([{ label: null, lines: ['Shared detail.'] }]);
  });
});
