import { ActionIcon, Checkbox, Group, Stack, Text, Tooltip } from '@mantine/core';
import { IconX } from '@tabler/icons-react';
import type { ChartTierGroup, Vocabulary } from '../../../api/vocabulary.ts';
import { CommitSwitch } from '../../../shared/CommitInputs.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import type { ItemFamily } from '../model/itemFamily.ts';
import {
  formatTierGroup,
  HULL_SIZE_LABELS,
  isReadableTierGroup,
  setHullValue,
  setTierGroupEnabled,
  tierGroupIds,
  tierGroupValue,
  togglePerHullSize,
  togglePerTierGroup,
  type ModifierLine,
} from '../model/modifiers.ts';

interface ModifierLineRowProps {
  line: ModifierLine;
  index: number;
  perHullSizeAllowed: boolean;
  effectEditable: boolean;
  removable: boolean;
  vocabulary: Vocabulary;
  family?: ItemFamily;
  onEdit: (recipe: (line: ModifierLine) => void) => void;
  onRemove: () => void;
}

function tierGroupLabel(tierGroup: ChartTierGroup): string {
  return `Tiers ${tierGroup.minTier}-${tierGroup.maxTier} (${tierGroup.id})`;
}

function tierGroupTooltip(vocabulary: Vocabulary): string {
  const ranges = vocabulary.chartTierGroups.map((tierGroup) => `${tierGroup.minTier}-${tierGroup.maxTier}`);
  const rangesText = ranges.length > 1 ? `${ranges.slice(0, -1).join(', ')} and ${ranges[ranges.length - 1]}` : (ranges[0] ?? '');
  return `Separate values for each tier group: tiers ${rangesText}, instead of one range for every tier; leave lower groups off for a modifier that only rolls at higher tiers`;
}

function lowestTierText(line: ModifierLine, vocabulary: Vocabulary): string {
  const lowest = vocabulary.chartTierGroups.find((tierGroup) => tierGroupValue(line, tierGroup.id));
  return lowest ? `Rolls from tier ${lowest.minTier}` : 'No readable tier groups';
}

function TierGroupInputs({ line, vocabulary, onEdit }: Pick<ModifierLineRowProps, 'line' | 'vocabulary' | 'onEdit'>) {
  const groupIds = tierGroupIds(vocabulary);
  const highestGroup = groupIds[groupIds.length - 1];
  const firstShownGroup = groupIds.find((group) => tierGroupValue(line, group));
  const unreadable = (line.tierGroups ?? []).flatMap((groupValue, groupIndex) => (isReadableTierGroup(groupValue, groupIds) ? [] : [{ groupValue, groupIndex }]));
  const setValue = (group: string, field: 'min' | 'max', text: string) =>
    onEdit((draft) => {
      const groupValue = tierGroupValue(draft, group);
      if (groupValue) {
        groupValue[field] = text;
      }
    });
  return (
    <Stack gap={4} pl="md">
      {vocabulary.chartTierGroups.map((tierGroup) => {
        const groupValue = tierGroupValue(line, tierGroup.id);
        const labelled = tierGroup.id === firstShownGroup;
        return (
          <Group key={tierGroup.id} gap="xs" align="flex-end" wrap="nowrap">
            <Checkbox
              w={170}
              mb={8}
              label={tierGroupLabel(tierGroup)}
              aria-label={`${line.effect} ${tierGroup.id} group`}
              checked={Boolean(groupValue)}
              disabled={tierGroup.id === highestGroup && Boolean(groupValue)}
              onChange={(event) => {
                const enabled = event.currentTarget.checked;
                onEdit((draft) => setTierGroupEnabled(draft, groupIds, tierGroup.id, enabled));
              }}
            />
            {groupValue && (
              <>
                <DecimalInput
                  w={110}
                  label={labelled ? 'Min' : undefined}
                  aria-label={`${line.effect} ${tierGroup.id} min`}
                  value={groupValue.min}
                  onChange={(text) => setValue(tierGroup.id, 'min', text)}
                />
                <DecimalInput
                  w={110}
                  label={labelled ? 'Max (blank = fixed)' : undefined}
                  aria-label={`${line.effect} ${tierGroup.id} max`}
                  value={groupValue.max}
                  onChange={(text) => setValue(tierGroup.id, 'max', text)}
                />
              </>
            )}
          </Group>
        );
      })}
      {unreadable.map(({ groupValue, groupIndex }) => (
        <Group key={`unreadable-${groupIndex}`} gap="xs" align="center" wrap="nowrap">
          <Text size="sm" c="red">
            Unreadable tier group &quot;{formatTierGroup(groupValue)}&quot; - remove it and switch on the group you meant
          </Text>
          <ActionIcon
            variant="subtle"
            color="red"
            onClick={() => onEdit((draft) => void draft.tierGroups?.splice(groupIndex, 1))}
            aria-label={`Remove tier group ${groupIndex + 1}`}
          >
            <IconX size={16} />
          </ActionIcon>
        </Group>
      ))}
    </Stack>
  );
}

export function ModifierLineRow({ line, index, perHullSizeAllowed, effectEditable, removable, vocabulary, family = 'ship', onEdit, onRemove }: ModifierLineRowProps) {
  const hullInputs = line.hullValues ? [...line.hullValues] : [];
  while (line.hullValues && hullInputs.length < HULL_SIZE_LABELS.length) {
    hullInputs.push('');
  }
  const isChart = family === 'chart';
  return (
    <Stack gap={4}>
      <Group align="flex-end" gap="xs" wrap="nowrap">
        {effectEditable ? (
          <EffectSelect
            style={{ flex: 3, minWidth: 220 }}
            label={`Effect ${index + 1}`}
            vocabulary={vocabulary}
            effectNames={isChart ? vocabulary.chartEffectNames : undefined}
            effectKindLabel="star chart effect"
            value={line.effect}
            onChange={(effect) => onEdit((draft) => void (draft.effect = effect))}
          />
        ) : (
          <Text size="sm" style={{ flex: 3, minWidth: 220 }} mb={8} truncate>
            {line.effect}
          </Text>
        )}
        {(isChart || line.tierGroups) && (
          <Tooltip label={tierGroupTooltip(vocabulary)}>
            <CommitSwitch
              label="Per tier group"
              checked={Boolean(line.tierGroups)}
              onCommit={(perTierGroup) => onEdit((draft) => togglePerTierGroup(draft, perTierGroup, tierGroupIds(vocabulary)))}
              mb={8}
            />
          </Tooltip>
        )}
        {(!isChart || line.hullValues) && (
          <Tooltip label="Fixed values per hull size (frigate/destroyer/cruiser/capital) instead of a rolled range">
            <CommitSwitch
              label="Per hull size"
              checked={Boolean(line.hullValues)}
              disabled={!perHullSizeAllowed && !line.hullValues}
              onCommit={(perHullSize) => onEdit((draft) => togglePerHullSize(draft, perHullSize))}
              mb={8}
            />
          </Tooltip>
        )}
        {line.tierGroups ? (
          <Text size="xs" c="dimmed" style={{ flex: 4 }} mb={8}>
            {lowestTierText(line, vocabulary)}
          </Text>
        ) : line.hullValues ? (
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
      {line.tierGroups && <TierGroupInputs line={line} vocabulary={vocabulary} onEdit={onEdit} />}
    </Stack>
  );
}
