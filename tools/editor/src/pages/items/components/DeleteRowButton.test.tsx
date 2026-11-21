import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../../test/renderWithProviders.tsx';
import { DeleteRowButton } from './ItemFields.tsx';

describe('delete row button', () => {
  it('asks first, then deletes the row and selects the one before it', async () => {
    const onDelete = vi.fn();
    const onSelect = vi.fn();
    renderWithProviders(<DeleteRowButton noun="anchor" rowId="wormhole_anchor_pirate" message="Nothing will lead here." index={2} onDelete={onDelete} onSelect={onSelect} />);

    fireEvent.click(screen.getByRole('button', { name: 'Delete this anchor' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('Delete "wormhole_anchor_pirate"?')).toBeTruthy();
    expect(within(dialog).getByText('Nothing will lead here.')).toBeTruthy();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(onDelete).toHaveBeenCalledTimes(1));
    expect(onSelect).toHaveBeenCalledWith(1);
  });

  it('keeps the row when the deletion is cancelled', async () => {
    const onDelete = vi.fn();
    const onSelect = vi.fn();
    renderWithProviders(<DeleteRowButton noun="item" rowId="hull_item" message="Gone." index={0} onDelete={onDelete} onSelect={onSelect} />);

    fireEvent.click(screen.getByRole('button', { name: 'Delete this item' }));
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onDelete).not.toHaveBeenCalled();
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('shows the refusal instead of asking when the row cannot be deleted', async () => {
    const onDelete = vi.fn();
    renderWithProviders(<DeleteRowButton noun="modifier" rowId="HULL_MULT" message="Gone." index={0} refusal="HULL_MULT is used by 2 items." onDelete={onDelete} onSelect={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: 'Delete this modifier' }));

    expect(await screen.findByText('HULL_MULT is used by 2 items.')).toBeTruthy();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onDelete).not.toHaveBeenCalled();
  });
});
