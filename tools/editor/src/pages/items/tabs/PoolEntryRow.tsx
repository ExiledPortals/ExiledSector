import { Group, Text } from '@mantine/core';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import type { PoolEntry } from '../model/definitions.ts';
import type { ItemModifier } from '../model/modifiers.ts';
import { ModifierSelect } from './ModifierSelect.tsx';

interface PoolEntryRowProps {
  entry: PoolEntry;
  slot: 'prefix' | 'suffix';
  itemType: string;
  index: number;
  count: number;
  share: number;
  catalogue: ItemModifier[];
  onEdit: (recipe: (entry: PoolEntry) => void) => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

export function PoolEntryRow({ entry, slot, itemType, index, count, share, catalogue, onEdit, onMove, onRemove }: PoolEntryRowProps) {
  return (
    <Group align="flex-start" gap="xs" wrap="nowrap">
      <ModifierSelect
        style={{ flex: 4, minWidth: 260 }}
        label={entry.inlineText === undefined ? `Modifier ${index + 1}` : `Modifier ${index + 1} (was written inline as ${entry.inlineText})`}
        catalogue={catalogue}
        slot={slot}
        itemType={itemType}
        value={entry.modifierId}
        onChange={(modifierId) =>
          onEdit((draft) => {
            draft.modifierId = modifierId;
            delete draft.inlineText;
          })
        }
      />
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
      <Group pt={24}>
        <RowButtons index={index} count={count} onMove={onMove} onRemove={onRemove} />
      </Group>
    </Group>
  );
}
