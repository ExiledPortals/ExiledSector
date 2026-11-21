import { describe, expect, it } from 'vitest';
import { firstFreeNumberedId, markRowsSaved, uniqueSuffixedId, type SavedRow } from './rowIds.ts';

describe('row ids', () => {
  it('marks the ids that were saved, not ids renamed or added after the save started', () => {
    const saved: SavedRow[] = [{ id: 'renamed', loadedId: 'loaded' }, { id: 'added_before_save' }];
    const current: SavedRow[] = [{ id: 'renamed_again', loadedId: 'loaded' }, { id: 'added_before_save' }, { id: 'added_during_save' }];

    markRowsSaved(current, saved);

    expect(current.map((row) => row.loadedId)).toEqual(['renamed', 'added_before_save', undefined]);
  });

  it('keeps a free base id and numbers a taken one from 2', () => {
    const taken = new Set(['copy', 'copy_2']);

    expect(uniqueSuffixedId('fresh', (id) => taken.has(id))).toBe('fresh');
    expect(uniqueSuffixedId('copy', (id) => taken.has(id))).toBe('copy_3');
  });

  it('numbers from the given first number without trying the bare base', () => {
    const taken = new Set(['anchor_1']);

    expect(firstFreeNumberedId('anchor', (id) => taken.has(id), 1)).toBe('anchor_2');
    expect(firstFreeNumberedId('node', (id) => taken.has(id), 1)).toBe('node_1');
  });
});
