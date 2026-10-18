import { Select, Text, type ComboboxItem, type SelectProps } from '@mantine/core';
import { useMemo } from 'react';
import type { Vocabulary } from '../api/vocabulary.ts';

interface EffectSelectProps extends Omit<SelectProps, 'data' | 'value' | 'onChange'> {
  vocabulary: Vocabulary;
  value: string;
  onChange: (effectName: string) => void;
  effectNames?: string[];
  effectKindLabel?: string;
}

function effectDescription(vocabulary: Vocabulary, effectName: string): string {
  const info = vocabulary.effectInfo.get(effectName);
  if (!info) {
    return 'not a known effect';
  }
  return (info.statMode ?? 'special') + (info.lowerIsBetter ? ', lower is better' : '');
}

export function EffectSelect({ vocabulary, value, onChange, effectNames, effectKindLabel, ...selectProps }: EffectSelectProps) {
  const offered = effectNames ?? vocabulary.effectNames;
  const isKnown = (name: string) => (effectNames ? effectNames.includes(name) : vocabulary.effectInfo.has(name));
  const data = useMemo<ComboboxItem[]>(() => {
    const known = (name: string) => (effectNames ? effectNames.includes(name) : vocabulary.effectInfo.has(name));
    const names = value && !known(value) ? [value, ...offered] : offered;
    return names.map((name) => ({ value: name, label: known(name) ? name : `${name} (unknown)` }));
  }, [vocabulary, value, offered, effectNames]);

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
            {effectNames ? (isKnown(option.value) ? (effectKindLabel ?? 'effect') : 'not a known effect') : effectDescription(vocabulary, option.value)}
          </Text>
        </div>
      )}
      error={value && !isKnown(value) ? 'Unknown effect' : undefined}
      {...selectProps}
    />
  );
}
