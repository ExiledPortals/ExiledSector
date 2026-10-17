import { Group, Switch, Text, Tooltip } from '@mantine/core';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { HULL_SIZE_LABELS, setHullValue, togglePerHullSize, type PoolEntry } from '../model/definitions.ts';

interface PoolEntryRowProps {
  entry: PoolEntry;
  index: number;
  count: number;
  share: number;
  vocabulary: Vocabulary;
  onEdit: (recipe: (entry: PoolEntry) => void) => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

export function PoolEntryRow({ entry, index, count, share, vocabulary, onEdit, onMove, onRemove }: PoolEntryRowProps) {
  const hullInputs = entry.hullValues ? [...entry.hullValues] : [];
  while (entry.hullValues && hullInputs.length < HULL_SIZE_LABELS.length) {
    hullInputs.push('');
  }
  return (
    <Group align="flex-end" gap="xs" wrap="nowrap">
      <EffectSelect
        style={{ flex: 3, minWidth: 220 }}
        label={`Effect ${index + 1}`}
        vocabulary={vocabulary}
        value={entry.effect}
        onChange={(effect) => onEdit((draft) => void (draft.effect = effect))}
      />
      <Tooltip label="Fixed values per hull size (frigate/destroyer/cruiser/capital) instead of a rolled range">
        <Switch
          label="Per hull size"
          checked={Boolean(entry.hullValues)}
          onChange={(event) => {
            const perHullSize = event.currentTarget.checked;
            onEdit((draft) => togglePerHullSize(draft, perHullSize));
          }}
          mb={8}
        />
      </Tooltip>
      {entry.hullValues ? (
        hullInputs.map((value, hullIndex) => (
          <DecimalInput
            key={hullIndex}
            style={{ flex: 1 }}
            label={HULL_SIZE_LABELS[hullIndex] ?? 'Extra (clear to remove)'}
            value={value}
            onChange={(text) => onEdit((draft) => setHullValue(draft, hullIndex, text))}
          />
        ))
      ) : (
        <>
          <DecimalInput style={{ flex: 2 }} label="Min" value={entry.min} onChange={(text) => onEdit((draft) => void (draft.min = text))} />
          <DecimalInput style={{ flex: 2 }} label="Max" value={entry.max} onChange={(text) => onEdit((draft) => void (draft.max = text))} />
        </>
      )}
      <DecimalInput
        style={{ flex: 2 }}
        label={
          <Group gap={4} wrap="nowrap">
            Rarity weight
            <Text span size="xs" c="dimmed">
              ({Math.round(share * 100)}% of first picks)
            </Text>
          </Group>
        }
        placeholder="1"
        value={entry.weight}
        onChange={(text) => onEdit((draft) => void (draft.weight = text))}
      />
      <RowButtons index={index} count={count} onMove={onMove} onRemove={onRemove} />
    </Group>
  );
}
