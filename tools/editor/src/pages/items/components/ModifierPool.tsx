import { ActionIcon, Group, Paper, Select, Stack, Text, Title, Tooltip } from '@mantine/core';
import { IconPlus, IconX } from '@tabler/icons-react';
import { useMemo } from 'react';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { inlineLine, plainText, type LineDescriptions } from '../describedLines.ts';
import { poolCandidates } from '../itemOptions.ts';
import { poolShares, type PoolEntry } from '../model/definitions.ts';
import type { ItemModifier } from '../model/modifiers.ts';
import { GameLines, MissingModifier } from './GameText.tsx';

interface ModifierPickerProps {
  candidates: readonly ItemModifier[];
  itemType: string;
  descriptions: LineDescriptions;
  label: string;
  onPick: (modifierId: string) => void;
}

function searchLabel(modifier: ItemModifier, itemType: string, descriptions: LineDescriptions): string {
  const text = modifier.lines.map((line) => plainText(descriptions.markupOf(line, itemType))).join(' / ');
  return `${text}${modifier.name ? ` - ${modifier.name}` : ''} (${modifier.id})`;
}

export function ModifierPicker({ candidates, itemType, descriptions, label, onPick }: ModifierPickerProps) {
  const byId = useMemo(() => new Map(candidates.map((modifier) => [modifier.id, modifier])), [candidates]);
  const data = candidates.map((modifier) => ({ value: modifier.id, label: searchLabel(modifier, itemType, descriptions) }));
  return (
    <Select
      searchable
      clearable={false}
      data={data}
      value={null}
      onChange={(modifierId) => {
        if (modifierId) {
          onPick(modifierId);
        }
      }}
      placeholder={candidates.length === 0 ? 'Nothing left to add' : 'Search the in-game text, name or id'}
      disabled={candidates.length === 0}
      leftSection={<IconPlus size={14} />}
      aria-label={label}
      maxDropdownHeight={420}
      nothingFoundMessage="No modifier matches"
      comboboxProps={{ width: 'target' }}
      renderOption={({ option }) => {
        const modifier = byId.get(option.value);
        return modifier ? (
          <Stack gap={2} w="100%">
            <GameLines lines={modifier.lines} itemType={itemType} descriptions={descriptions} />
            <Text size="xs" c="dimmed">
              {modifier.name ? `${modifier.name} - ` : ''}
              {modifier.id}
            </Text>
          </Stack>
        ) : (
          option.label
        );
      }}
    />
  );
}

interface ModifierPoolProps {
  title: string;
  hint?: string;
  entries: readonly PoolEntry[];
  catalogue: readonly ItemModifier[];
  slot: string;
  itemType: string;
  descriptions: LineDescriptions;
  weighted: boolean;
  catalogueFileName: string;
  onAdd: (modifierId: string) => void;
  onRemove: (index: number) => void;
  onWeight?: (index: number, weight: string) => void;
}

export function ModifierPool({ title, hint, entries, catalogue, slot, itemType, descriptions, weighted, catalogueFileName, onAdd, onRemove, onWeight }: ModifierPoolProps) {
  const byId = new Map(catalogue.map((modifier) => [modifier.id, modifier]));
  const shares = poolShares([...entries]);
  return (
    <Stack gap="xs">
      <Group gap="xs" align="baseline">
        <Title order={5}>{title}</Title>
        {hint && (
          <Text size="xs" c="dimmed">
            {hint}
          </Text>
        )}
      </Group>
      {entries.length === 0 && (
        <Text size="sm" c="dimmed">
          None yet.
        </Text>
      )}
      {entries.map((entry, index) => {
        const modifier = entry.inlineText === undefined ? byId.get(entry.modifierId) : undefined;
        const label = modifier?.id ?? entry.inlineText ?? entry.modifierId;
        return (
          <Paper key={`${index}|${label}`} withBorder p={6}>
            <Group gap="sm" wrap="nowrap" align="center">
              <div style={{ flex: 1, minWidth: 0 }}>
                {modifier ? (
                  <GameLines lines={modifier.lines} itemType={itemType} descriptions={descriptions} />
                ) : entry.inlineText !== undefined ? (
                  <GameLines lines={[inlineLine(entry.inlineText)]} itemType={itemType} descriptions={descriptions} />
                ) : (
                  <MissingModifier text={`${entry.modifierId || '(no modifier)'} is not in ${catalogueFileName}`} />
                )}
              </div>
              <Stack gap={0} w={200} style={{ flexShrink: 0 }}>
                <Text size="sm" c="#5abeff" truncate>
                  {modifier?.name || (entry.inlineText !== undefined ? 'Written inline' : '')}
                </Text>
                <Text size="xs" c="dimmed" truncate>
                  {label}
                </Text>
              </Stack>
              {weighted && onWeight && (
                <DecimalInput
                  w={130}
                  size="xs"
                  aria-label={`${label} weight`}
                  placeholder="1"
                  value={entry.weight}
                  onChange={(text) => onWeight(index, text)}
                  rightSection={
                    <Text size="xs" c="dimmed">
                      {Math.round(shares[index] * 100)}%
                    </Text>
                  }
                  rightSectionWidth={44}
                />
              )}
              <Tooltip label="Remove">
                <ActionIcon variant="subtle" color="red" onClick={() => onRemove(index)} aria-label={`Remove ${label}`}>
                  <IconX size={16} />
                </ActionIcon>
              </Tooltip>
            </Group>
          </Paper>
        );
      })}
      <ModifierPicker
        candidates={poolCandidates(catalogue, slot, itemType, entries)}
        itemType={itemType}
        descriptions={descriptions}
        label={`Add to ${title.toLowerCase()}`}
        onPick={onAdd}
      />
    </Stack>
  );
}
