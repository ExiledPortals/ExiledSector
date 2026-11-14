import type { Draft } from 'immer';
import { useCallback } from 'react';
import type { ItemsModel } from './model/workspace.ts';

type ItemsUpdate = (recipe: (draft: Draft<ItemsModel>) => void) => void;

export function useRowEdit<Row>(update: ItemsUpdate, rowsOf: (draft: Draft<ItemsModel>) => Row[], index: number): (recipe: (row: Row) => void) => void {
  return useCallback((recipe: (row: Row) => void) => update((draft) => recipe(rowsOf(draft)[index])), [update, rowsOf, index]);
}
