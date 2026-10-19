import { SegmentedControl } from '@mantine/core';
import type { DataDocument } from '../../../shared/useDataDocument.ts';
import { ITEM_FAMILY_OPTIONS, type ItemFamily } from '../model/itemFamily.ts';

interface FamilyOptionSwitchProps<TModel, TFamily extends string> {
  document: DataDocument<TModel>;
  family: TFamily;
  onFamily: (family: TFamily) => void;
  noun: string;
  options: readonly { value: TFamily; label: string }[];
}

export function FamilyOptionSwitch<TModel, TFamily extends string>({ document, family, onFamily, noun, options }: FamilyOptionSwitchProps<TModel, TFamily>) {
  const switchTo = async (value: string) => {
    const chosen = options.find((option) => option.value === value);
    if (!chosen || chosen.value === family) {
      return;
    }
    if (await document.confirmDiscard(`Discard unsaved ${noun} changes and switch item type?`)) {
      onFamily(chosen.value);
    }
  };
  return (
    <SegmentedControl
      size="xs"
      value={family}
      onChange={(value) => void switchTo(value)}
      data={[...options]}
      disabled={document.saving}
      aria-label="Item type"
    />
  );
}

export function FamilySwitch<TModel>(props: Omit<FamilyOptionSwitchProps<TModel, ItemFamily>, 'options'>) {
  return <FamilyOptionSwitch {...props} options={ITEM_FAMILY_OPTIONS} />;
}
