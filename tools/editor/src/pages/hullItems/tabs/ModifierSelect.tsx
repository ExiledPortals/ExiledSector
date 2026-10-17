import { Select, Text, type ComboboxItem, type SelectProps } from '@mantine/core';
import { useMemo } from 'react';
import { modifierSummary, type HullItemModifier } from '../model/modifiers.ts';

interface ModifierSelectProps extends Omit<SelectProps, 'data' | 'value' | 'onChange'> {
  catalogue: HullItemModifier[];
  slot: string;
  value: string;
  onChange: (modifierId: string) => void;
}

export function ModifierSelect({ catalogue, slot, value, onChange, ...selectProps }: ModifierSelectProps) {
  const modifiersById = useMemo(() => new Map(catalogue.map((modifier) => [modifier.id, modifier])), [catalogue]);
  const data = useMemo<ComboboxItem[]>(() => {
    const options = catalogue.filter((modifier) => modifier.slot === slot).map((modifier) => ({ value: modifier.id, label: modifier.id }));
    if (value && !options.some((option) => option.value === value)) {
      options.unshift({ value, label: `${value} (${modifiersById.has(value) ? `a ${modifiersById.get(value)?.slot}` : 'missing'})` });
    }
    return options;
  }, [catalogue, slot, value, modifiersById]);
  const selected = modifiersById.get(value);
  const problem = !value ? 'Pick a modifier' : !selected ? 'Not in hull_item_modifiers.csv' : selected.slot !== slot ? `This is a ${selected.slot}` : undefined;

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
