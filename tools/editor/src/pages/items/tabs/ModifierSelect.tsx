import { Select, Text, type ComboboxItem, type SelectProps } from '@mantine/core';
import { useMemo } from 'react';
import { modifierSummary, type ItemModifier } from '../model/modifiers.ts';

interface ModifierSelectProps extends Omit<SelectProps, 'data' | 'value' | 'onChange'> {
  catalogue: ItemModifier[];
  slot: string;
  itemType: string;
  value: string;
  onChange: (modifierId: string) => void;
}

function selectionProblem(value: string, selected: ItemModifier | undefined, slot: string, itemType: string): string | undefined {
  if (!value) {
    return 'Pick a modifier';
  }
  if (!selected) {
    return 'Not in item_modifiers.csv';
  }
  if (selected.slot !== slot) {
    return `This is a ${selected.slot}`;
  }
  return selected.itemTypes.includes(itemType) ? undefined : `This only rolls on ${selected.itemTypes.join(', ')}`;
}

export function ModifierSelect({ catalogue, slot, itemType, value, onChange, ...selectProps }: ModifierSelectProps) {
  const modifiersById = useMemo(() => new Map(catalogue.map((modifier) => [modifier.id, modifier])), [catalogue]);
  const data = useMemo<ComboboxItem[]>(() => {
    const options = catalogue
      .filter((modifier) => modifier.slot === slot && modifier.itemTypes.includes(itemType))
      .map((modifier) => ({ value: modifier.id, label: modifier.id }));
    if (value && !options.some((option) => option.value === value)) {
      options.unshift({ value, label: `${value} (${modifiersById.has(value) ? 'not offered here' : 'missing'})` });
    }
    return options;
  }, [catalogue, slot, itemType, value, modifiersById]);
  const selected = modifiersById.get(value);
  const problem = selectionProblem(value, selected, slot, itemType);

  return (
    <Select
      searchable
      data={data}
      value={value || null}
      onChange={(chosen) => {
        if (chosen) {
          onChange(chosen);
        }
      }}
      maxDropdownHeight={320}
      allowDeselect={false}
      renderOption={({ option }) => {
        const modifier = modifiersById.get(option.value);
        return (
          <div>
            <Text size="sm">{option.label}</Text>
            {modifier && (
              <Text size="xs" c="dimmed">
                {modifierSummary(modifier)}
                {modifier.name ? ` - "${modifier.name}"` : ''}
              </Text>
            )}
          </div>
        );
      }}
      description={selected ? modifierSummary(selected) + (selected.name ? ` - names common items "${selected.name}"` : '') : undefined}
      error={problem}
      {...selectProps}
    />
  );
}
