import { Button, Group, Paper, Stack, Text } from '@mantine/core';
import { IconCopy } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import type { PoolEntry } from '../model/definitions.ts';
import type { ItemModifier, ModifierLine } from '../model/modifiers.ts';
import { ModifierLineRow } from './ModifierLineRow.tsx';
import { ModifierSelect } from './ModifierSelect.tsx';

interface PoolEntryRowProps {
  entry: PoolEntry;
  slot: 'prefix' | 'suffix';
  itemType: string;
  index: number;
  count: number;
  share: number;
  catalogue: ItemModifier[];
  otherItemsUsingModifier: string[];
  vocabulary: Vocabulary;
  onEdit: (recipe: (entry: PoolEntry) => void) => void;
  onEditModifier: (recipe: (modifier: ItemModifier) => void) => void;
  onCopyModifier: () => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

export function PoolEntryRow({
  entry,
  slot,
  itemType,
  index,
  count,
  share,
  catalogue,
  otherItemsUsingModifier,
  vocabulary,
  onEdit,
  onEditModifier,
  onCopyModifier,
  onMove,
  onRemove,
}: PoolEntryRowProps) {
  const modifier = entry.inlineText === undefined ? catalogue.find((candidate) => candidate.id === entry.modifierId) : undefined;
  const editLine = (lineIndex: number, recipe: (line: ModifierLine) => void) => onEditModifier((draft) => recipe(draft.lines[lineIndex]));
  return (
    <Paper withBorder p="xs">
      <Stack gap="xs">
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
        {modifier &&
          modifier.lines.map((line, lineIndex) => (
            <ModifierLineRow
              key={lineIndex}
              line={line}
              index={lineIndex}
              perHullSizeAllowed
              effectEditable={false}
              removable={false}
              vocabulary={vocabulary}
              onEdit={(recipe) => editLine(lineIndex, recipe)}
              onRemove={() => undefined}
            />
          ))}
        {modifier && otherItemsUsingModifier.length > 0 && (
          <Group gap="xs" justify="space-between" wrap="nowrap">
            <Text size="xs" c="orange">
              {modifier.id} is shared: editing its values also changes {otherItemsUsingModifier.join(', ')}.
            </Text>
            <Button size="compact-xs" variant="light" leftSection={<IconCopy size={12} />} onClick={onCopyModifier}>
              Copy for this item
            </Button>
          </Group>
        )}
      </Stack>
    </Paper>
  );
}
