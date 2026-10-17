import { describe, expect, it } from 'vitest';
import { saveOutcome } from './saveTracking.ts';

describe('save tracking', () => {
  it('treats a save as clean, edited since, or superseded by a reload', () => {
    const ticket = { revision: 3, loadGeneration: 1 };
    expect(saveOutcome(ticket, { revision: 3, loadGeneration: 1 })).toBe('clean');
    expect(saveOutcome(ticket, { revision: 4, loadGeneration: 1 })).toBe('editedSince');
    expect(saveOutcome(ticket, { revision: 3, loadGeneration: 2 })).toBe('superseded');
  });
});
