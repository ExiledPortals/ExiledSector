import { SegmentedControl } from '@mantine/core';
import type { DataDocument } from '../../../shared/useDataDocument.ts';
import { isFamily, ITEM_FAMILY_OPTIONS, type ItemFamily } from '../model/itemFamily.ts';

interface FamilySwitchProps<TModel> {
  document: DataDocument<TModel>;
  family: ItemFamily;
  onFamily: (family: ItemFamily) => void;
  noun: string;
}

export function FamilySwitch<TModel>({ document, family, onFamily, noun }: FamilySwitchProps<TModel>) {
  const switchTo = async (value: string) => {
    if (!isFamily(value) || value === family) {
      return;
    }
    if (await document.confirmDiscard(`Discard unsaved ${noun} changes and switch item type?`)) {
      onFamily(value);
    }
  };
  return (
    <SegmentedControl
      size="xs"
      value={family}
      onChange={(value) => void switchTo(value)}
      data={ITEM_FAMILY_OPTIONS}
      disabled={document.saving}
      aria-label="Item type"
    />
  );
}
