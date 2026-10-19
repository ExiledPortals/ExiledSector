import { Button, Group, Paper, Stack, Text, TextInput } from '@mantine/core';
import { IconCopy } from '@tabler/icons-react';
import { memo } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import type { PoolEntry, PoolGroup } from '../model/definitions.ts';
import { MODIFIER_FILE_NAMES, type ItemFamily } from '../model/itemFamily.ts';
import type { ItemModifier, ModifierLine } from '../model/modifiers.ts';
import { ModifierLineRow } from './ModifierLineRow.tsx';
import { ModifierSelect } from './ModifierSelect.tsx';

interface PoolEntryRowProps {
  groupKey: PoolGroup;
  entry: PoolEntry;
  slot: 'prefix' | 'suffix';
  itemType: string;
  index: number;
  count: number;
  share: number;
  catalogue: ItemModifier[];
  otherItemsUsingModifier: string;
  vocabulary: Vocabulary;
  family?: ItemFamily;
  weighted?: boolean;
  onEdit: (groupKey: PoolGroup, entryIndex: number, recipe: (entry: PoolEntry) => void) => void;
  onEditModifier: (modifierId: string, recipe: (modifier: ItemModifier) => void) => void;
  onCopyModifier: (groupKey: PoolGroup, entryIndex: number) => void;
  onMove: (groupKey: PoolGroup, from: number, to: number) => void;
  onRemove: (groupKey: PoolGroup, entryIndex: number) => void;
}

export const PoolEntryRow = memo(function PoolEntryRow({
  groupKey,
  entry,
  slot,
  itemType,
  index,
  count,
  share,
  catalogue,
  otherItemsUsingModifier,
  vocabulary,
  family = 'ship',
  weighted = true,
  onEdit,
  onEditModifier,
  onCopyModifier,
  onMove,
  onRemove,
}: PoolEntryRowProps) {
  const editEntry = (recipe: (draft: PoolEntry) => void) => onEdit(groupKey, index, recipe);
  const modifier = entry.inlineText === undefined ? catalogue.find((candidate) => candidate.id === entry.modifierId) : undefined;
  const editLine = (lineIndex: number, recipe: (line: ModifierLine) => void) => onEditModifier(entry.modifierId, (draft) => recipe(draft.lines[lineIndex]));
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
            catalogueFileName={MODIFIER_FILE_NAMES[family]}
            onChange={(modifierId) =>
              editEntry((draft) => {
                draft.modifierId = modifierId;
                delete draft.inlineText;
              })
            }
          />
          {weighted && (
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
              onChange={(text) =>
                editEntry((draft) => {
                  draft.weight = text;
                  delete draft.emptyWeightField;
                })
              }
            />
          )}
          {family === 'chart' && (
            <TextInput
              style={{ flex: 1 }}
              label="Tiers"
              placeholder="all"
              description="t3+, t2-6 or t5"
              value={entry.tiers ?? ''}
              onChange={(event) => {
                const tiers = event.currentTarget.value.trim();
                editEntry((draft) => {
                  if (tiers) {
                    draft.tiers = tiers;
                  } else {
                    delete draft.tiers;
                  }
                });
              }}
            />
          )}
          <Group pt={24}>
            <RowButtons index={index} count={count} onMove={(from, to) => onMove(groupKey, from, to)} onRemove={() => onRemove(groupKey, index)} />
          </Group>
        </Group>
        {modifier &&
          modifier.lines.map((line, lineIndex) => (
            <ModifierLineRow
              key={lineIndex}
              line={line}
              index={lineIndex}
              perHullSizeAllowed={family !== 'chart'}
              family={family}
              effectEditable={false}
              removable={false}
              vocabulary={vocabulary}
              onEdit={(recipe) => editLine(lineIndex, recipe)}
              onRemove={() => undefined}
            />
          ))}
        {modifier && otherItemsUsingModifier !== '' && (
          <Group gap="xs" justify="space-between" wrap="nowrap">
            <Text size="xs" c="orange">
              {modifier.id} is shared: editing its values also changes {otherItemsUsingModifier}.
            </Text>
            <Button size="compact-xs" variant="light" leftSection={<IconCopy size={12} />} onClick={() => onCopyModifier(groupKey, index)}>
              Copy for this item
            </Button>
          </Group>
        )}
      </Stack>
    </Paper>
  );
});
