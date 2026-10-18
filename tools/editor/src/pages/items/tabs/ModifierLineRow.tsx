import { ActionIcon, Group, Switch, Text, Tooltip } from '@mantine/core';
import { IconX } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import { HULL_SIZE_LABELS, setHullValue, togglePerHullSize, type ModifierLine } from '../model/modifiers.ts';

interface ModifierLineRowProps {
  line: ModifierLine;
  index: number;
  perHullSizeAllowed: boolean;
  effectEditable: boolean;
  removable: boolean;
  vocabulary: Vocabulary;
  onEdit: (recipe: (line: ModifierLine) => void) => void;
  onRemove: () => void;
}

export function ModifierLineRow({ line, index, perHullSizeAllowed, effectEditable, removable, vocabulary, onEdit, onRemove }: ModifierLineRowProps) {
  const hullInputs = line.hullValues ? [...line.hullValues] : [];
  while (line.hullValues && hullInputs.length < HULL_SIZE_LABELS.length) {
    hullInputs.push('');
  }
  return (
    <Group align="flex-end" gap="xs" wrap="nowrap">
      {effectEditable ? (
        <EffectSelect
          style={{ flex: 3, minWidth: 220 }}
          label={`Effect ${index + 1}`}
          vocabulary={vocabulary}
          value={line.effect}
          onChange={(effect) => onEdit((draft) => void (draft.effect = effect))}
        />
      ) : (
        <Text size="sm" style={{ flex: 3, minWidth: 220 }} mb={8} truncate>
          {line.effect}
        </Text>
      )}
      <Tooltip label="Fixed values per hull size (frigate/destroyer/cruiser/capital) instead of a rolled range">
        <Switch
          label="Per hull size"
          checked={Boolean(line.hullValues)}
          disabled={!perHullSizeAllowed && !line.hullValues}
          onChange={(event) => {
            const perHullSize = event.currentTarget.checked;
            onEdit((draft) => togglePerHullSize(draft, perHullSize));
          }}
          mb={8}
        />
      </Tooltip>
      {line.hullValues ? (
        hullInputs.map((value, hullIndex) => (
          <DecimalInput
            key={hullIndex}
            style={{ flex: 1 }}
            label={HULL_SIZE_LABELS[hullIndex] ?? 'Extra (clear to remove)'}
            aria-label={`${line.effect} ${HULL_SIZE_LABELS[hullIndex] ?? 'extra'}`}
            value={value}
            onChange={(text) => onEdit((draft) => setHullValue(draft, hullIndex, text))}
          />
        ))
      ) : (
        <>
          <DecimalInput
            style={{ flex: 2 }}
            label="Min"
            aria-label={`${line.effect} min`}
            value={line.min}
            onChange={(text) => onEdit((draft) => void (draft.min = text))}
          />
          <DecimalInput
            style={{ flex: 2 }}
            label="Max"
            aria-label={`${line.effect} max`}
            value={line.max}
            onChange={(text) => onEdit((draft) => void (draft.max = text))}
          />
        </>
      )}
      {removable && (
        <ActionIcon variant="subtle" color="red" mb={6} onClick={onRemove} aria-label={`Remove line ${index + 1}`}>
          <IconX size={16} />
        </ActionIcon>
      )}
    </Group>
  );
}
