import { ActionIcon, Button, Group, Stack, Switch, Text, Tooltip } from '@mantine/core';
import { IconPlus, IconX } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import type { ItemFamily } from '../model/itemFamily.ts';
import { addTierBand, HULL_SIZE_LABELS, setHullValue, togglePerHullSize, togglePerTierBand, type ModifierLine } from '../model/modifiers.ts';

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

function TierBandInputs({ line, vocabulary, onEdit }: Pick<ModifierLineRowProps, 'line' | 'vocabulary' | 'onEdit'>) {
  const bands = line.tierBands ?? [];
  const setBand = (bandIndex: number, field: 'tier' | 'min' | 'max', text: string) =>
    onEdit((draft) => {
      const band = draft.tierBands?.[bandIndex];
      if (band) {
        band[field] = text;
      }
    });
  return (
    <Stack gap={4} pl="md">
      {bands.map((band, bandIndex) => (
        <Group key={bandIndex} gap="xs" align="flex-end" wrap="nowrap">
          {band.unparsedText !== undefined ? (
            <Text size="sm" c="red">
              Unreadable band &quot;{band.unparsedText}&quot; - remove it and add a new one
            </Text>
          ) : (
            <>
              <DecimalInput
                w={90}
                label={bandIndex === 0 ? 'From tier' : undefined}
                aria-label={`${line.effect} band ${bandIndex + 1} tier`}
                value={band.tier}
                onChange={(text) => setBand(bandIndex, 'tier', text)}
              />
              <DecimalInput
                w={110}
                label={bandIndex === 0 ? 'Min' : undefined}
                aria-label={`${line.effect} band ${bandIndex + 1} min`}
                value={band.min}
                onChange={(text) => setBand(bandIndex, 'min', text)}
              />
              <DecimalInput
                w={110}
                label={bandIndex === 0 ? 'Max (blank = fixed)' : undefined}
                aria-label={`${line.effect} band ${bandIndex + 1} max`}
                value={band.max}
                onChange={(text) => setBand(bandIndex, 'max', text)}
              />
            </>
          )}
          {bandIndex > 0 && (
            <ActionIcon variant="subtle" color="red" mb={6} onClick={() => onEdit((draft) => void draft.tierBands?.splice(bandIndex, 1))} aria-label={`Remove band ${bandIndex + 1}`}>
              <IconX size={16} />
            </ActionIcon>
          )}
        </Group>
      ))}
      <Button
        size="compact-xs"
        variant="subtle"
        w="fit-content"
        leftSection={<IconPlus size={12} />}
        onClick={() => onEdit((draft) => addTierBand(draft, vocabulary.highestChartTier))}
      >
        Add tier band
      </Button>
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
        {(isChart || line.tierBands) && (
          <Tooltip label="Separate values from each tier band upwards (t1, then the next band's tier, and so on) instead of one range for every tier">
            <Switch
              label="Per tier band"
              checked={Boolean(line.tierBands)}
              onChange={(event) => {
                const perTierBand = event.currentTarget.checked;
                onEdit((draft) => togglePerTierBand(draft, perTierBand));
              }}
              mb={8}
            />
          </Tooltip>
        )}
        {(!isChart || line.hullValues) && (
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
        )}
        {line.tierBands ? (
          <Text size="xs" c="dimmed" style={{ flex: 4 }} mb={8}>
            {line.tierBands.length} tier band{line.tierBands.length === 1 ? '' : 's'}
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
      {line.tierBands && <TierBandInputs line={line} vocabulary={vocabulary} onEdit={onEdit} />}
    </Stack>
  );
}
