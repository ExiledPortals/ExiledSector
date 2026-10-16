import { Select, Text, type ComboboxItem, type SelectProps } from '@mantine/core';
import { useMemo } from 'react';
import type { Vocabulary } from '../api/vocabulary.ts';

interface EffectSelectProps extends Omit<SelectProps, 'data' | 'value' | 'onChange'> {
  vocabulary: Vocabulary;
  value: string;
  onChange: (effectName: string) => void;
}

function effectDescription(vocabulary: Vocabulary, effectName: string): string {
  const info = vocabulary.effectInfo.get(effectName);
  if (!info) {
    return 'not a known effect';
  }
  return (info.statMode ?? 'special') + (info.lowerIsBetter ? ', lower is better' : '');
}

export function EffectSelect({ vocabulary, value, onChange, ...selectProps }: EffectSelectProps) {
  const data = useMemo<ComboboxItem[]>(() => {
    const names = value && !vocabulary.effectInfo.has(value) ? [value, ...vocabulary.effectNames] : vocabulary.effectNames;
    return names.map((name) => ({ value: name, label: vocabulary.effectInfo.has(name) ? name : `${name} (unknown)` }));
  }, [vocabulary, value]);

  return (
    <Select
      searchable
      data={data}
      value={value}
      onChange={(selected) => {
        if (selected) {
          onChange(selected);
        }
      }}
      maxDropdownHeight={320}
      allowDeselect={false}
      renderOption={({ option }) => (
        <div>
          <Text size="sm">{option.label}</Text>
          <Text size="xs" c="dimmed">
            {effectDescription(vocabulary, option.value)}
          </Text>
        </div>
      )}
      error={value && !vocabulary.effectInfo.has(value) ? 'Unknown effect' : undefined}
      {...selectProps}
    />
  );
}
