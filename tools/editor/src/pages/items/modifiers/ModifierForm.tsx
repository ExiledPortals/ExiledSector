import { ActionIcon, Button, Divider, Group, MultiSelect, Paper, Select, Stack, Text, TextInput, Title, Tooltip } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash, IconX } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import type { Draft } from 'immer';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction, notifyError } from '../../../shared/dialogs.tsx';
import { GameLines } from '../components/GameText.tsx';
import { ModifierLineRow } from '../components/ModifierLineRow.tsx';
import { displayItemType, type LineDescriptions } from '../describedLines.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import { AFFIX_GROUPS, ITEM_TYPE_LABELS, poolShares, type ItemDefinition } from '../model/definitions.ts';
import type { ItemFamily } from '../model/itemFamily.ts';
import { isUsed, renameModifierReferences, usedByText } from '../model/modifierUsage.ts';
import {
  familyItemTypes,
  idConventionIssue,
  MODIFIER_SLOT_LABELS,
  MODIFIER_SLOTS,
  newModifierLine,
  suggestedModifierId,
  uniqueModifierId,
  type ItemModifier,
  type ModifierUsage,
} from '../model/modifiers.ts';
import { modifiersOf, type ItemsModel } from '../model/workspace.ts';

interface ModifierFormProps extends ItemsPanelProps {
  family: ItemFamily;
  modifier: ItemModifier;
  index: number;
  usage: ModifierUsage | undefined;
  descriptions: LineDescriptions;
  onSelect: (index: number) => void;
}

function draftCatalogue(draft: Draft<ItemsModel>, family: ItemFamily): ItemModifier[] {
  return family === 'chart' ? draft.chartModifiers.modifiers : draft.shipModifiers.modifiers;
}

function referenceFiles(family: ItemFamily): string {
  return family === 'chart'
    ? 'star_charts.csv and star_chart_implicits.csv'
    : 'subroutines.csv, hull_items.csv, hull_item_implicits.csv, doctrine_archives.csv and the corruption pools';
}

export function ModifierForm({ document, model, vocabulary, family, modifier, index, usage, descriptions, onSelect }: ModifierFormProps) {
  const [idText, setIdText] = useState(modifier.id);
  useEffect(() => setIdText(modifier.id), [modifier.id]);
  const isImplicit = modifier.slot === 'implicit';
  const isChart = family === 'chart';
  const otherIds = new Set(modifiersOf(model, isChart ? 'ship' : 'chart').map((other) => other.id));

  const edit = (recipe: (draft: ItemModifier) => void) => document.update((draft) => recipe(draftCatalogue(draft, family)[index]));

  const renameTo = (newId: string) =>
    document.update((draft) => {
      const draftModifier = draftCatalogue(draft, family)[index];
      const oldId = draftModifier.id;
      draftModifier.id = newId;
      if (isChart) {
        renameModifierReferences(draft.charts.definitions, draft.chartImplicits.rows, oldId, newId);
      } else {
        renameModifierReferences(draft.ship.definitions, draft.hullImplicits.rows, oldId, newId, draft.doctrines.archives, draft.corruptionPools);
      }
    });

  const commitId = async (newId: string) => {
    if (newId === modifier.id) {
      return;
    }
    if (modifier.loadedId && modifier.id === modifier.loadedId) {
      const rename = await confirmAction({
        title: `Rename "${modifier.loadedId}" to "${newId}"?`,
        message:
          'Saved games store this id on every item that rolled it. Items whose id matches its effect name still find it, but other ids are only matched by ' +
          'their effect within the same slot, and common items whose frozen name used it take the word of their current first prefix or suffix. ' +
          `References in ${referenceFiles(family)} are renamed and saved with it. ` +
          `Its socketable.modifier.${modifier.loadedId} key in zh_CN.json keeps the old id and must be renamed by hand.`,
        confirmLabel: 'Rename',
        danger: true,
      });
      if (!rename) {
        setIdText(modifier.id);
        return;
      }
    }
    renameTo(newId);
  };

  const duplicate = () => {
    document.update((draft) => {
      const catalogue = draftCatalogue(draft, family);
      const copy = structuredClone(modifier);
      copy.id = uniqueModifierId(catalogue, `${modifier.id}_COPY`, otherIds);
      copy.cells = [];
      delete copy.loadedId;
      catalogue.splice(index + 1, 0, copy);
    });
    onSelect(index + 1);
  };

  const remove = async () => {
    if (isUsed(usage)) {
      notifyError(`${modifier.id} is used by ${usedByText(usage)}. Remove those references first.`);
      return;
    }
    const confirmed = await confirmAction({
      title: `Delete "${modifier.id}"?`,
      message: 'Nothing references it now, but items players already own may carry it, and their frozen names lose its word.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => void draftCatalogue(draft, family).splice(index, 1));
      onSelect(Math.max(0, index - 1));
    }
  };

  const conventionIssue = idConventionIssue(modifier);
  const suggestedId = suggestedModifierId(modifier.lines);

  return (
    <Stack gap="sm" maw={1100}>
      <Title order={5}>In game</Title>
      <GameLines lines={modifier.lines} itemType={displayItemType(modifier, family)} descriptions={descriptions} style={{ maxWidth: 520 }} />
      <Divider />
      <Group grow align="flex-start">
        <TextInput
          label={modifier.loadedId ? 'Id (saved games store it)' : 'Id'}
          value={idText}
          onChange={(event) => setIdText(event.currentTarget.value)}
          onBlur={() => void commitId(idText.trim())}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.currentTarget.blur();
            }
          }}
          error={conventionIssue ? `This modifier ${conventionIssue}` : undefined}
          description="One effect: the effect name, plus _QUALIFIER for another range or slot. Several effects: HYBRID_ plus each effect."
        />
        <Select
          label="Slot"
          data={MODIFIER_SLOTS.map((slot) => ({ value: slot, label: MODIFIER_SLOT_LABELS[slot] }))}
          value={modifier.slot}
          onChange={(value) => value && edit((draft) => void (draft.slot = value))}
          allowDeselect={false}
        />
      </Group>
      {conventionIssue && (
        <Button size="xs" variant="light" w="fit-content" onClick={() => void commitId(suggestedId)}>
          Use {suggestedId}
        </Button>
      )}
      {!isChart && (
        <MultiSelect
          label="Item types"
          description="Which item types can roll it."
          data={familyItemTypes(family, vocabulary).map((itemType) => ({ value: itemType, label: ITEM_TYPE_LABELS[itemType] ?? itemType }))}
          value={modifier.itemTypes}
          onChange={(itemTypes) => edit((draft) => void (draft.itemTypes = itemTypes))}
          error={modifier.itemTypes.length === 0 ? 'Pick at least one item type' : undefined}
        />
      )}
      <TextInput
        label="Name"
        description={isImplicit ? 'Implicits never show a name.' : 'Common items are named from their first prefix and suffix, as "<prefix> <noun> of <suffix>".'}
        disabled={isImplicit && !modifier.name}
        value={modifier.name}
        onChange={(event) => {
          const text = event.currentTarget.value;
          edit((draft) => void (draft.name = text));
        }}
      />
      <Divider />
      <Title order={5}>{modifier.lines.length > 1 ? 'Values - a hybrid rolls every line and counts as one modifier' : 'Values'}</Title>
      {modifier.lines.map((line, lineIndex) => (
        <ModifierLineRow
          key={lineIndex}
          line={line}
          index={lineIndex}
          perHullSizeAllowed={!isImplicit && !isChart}
          family={family}
          effectEditable
          removable={modifier.lines.length > 1}
          vocabulary={vocabulary}
          onEdit={(recipe) => edit((draft) => recipe(draft.lines[lineIndex]))}
          onRemove={() => edit((draft) => void draft.lines.splice(lineIndex, 1))}
        />
      ))}
      <Button
        size="xs"
        variant="default"
        w="fit-content"
        leftSection={<IconPlus size={14} />}
        onClick={() => edit((draft) => void draft.lines.push(newModifierLine(family, vocabulary, draft.slot)))}
      >
        Add line (makes a hybrid)
      </Button>
      {isChart && !isImplicit && <ChartRolls document={document} model={model} modifier={modifier} />}
      <Divider />
      <Title order={5}>Used by</Title>
      {isUsed(usage) ? (
        <Stack gap={2}>
          {usage && usage.items.length > 0 && <Text size="sm">Items: {usage.items.join(', ')}</Text>}
          {usage && usage.implicitSections.length > 0 && <Text size="sm">Implicit of: {usage.implicitSections.join(', ')}</Text>}
          {usage?.doctrineArchives && usage.doctrineArchives.length > 0 && <Text size="sm">Doctrine archives: {usage.doctrineArchives.join(', ')}</Text>}
          {usage?.corruptionPools?.map((label) => (
            <Text key={label} size="sm">
              {label}
            </Text>
          ))}
        </Stack>
      ) : (
        <Text size="sm" c="dimmed">
          Nothing. Keep it if released items may have rolled it, so their frozen names keep its word.
        </Text>
      )}
      <Divider />
      <Group>
        <Button variant="default" leftSection={<IconCopy size={14} />} onClick={duplicate}>
          Duplicate
        </Button>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this modifier
        </Button>
      </Group>
    </Stack>
  );
}

function chartPoolKey(modifier: ItemModifier) {
  return AFFIX_GROUPS.find((group) => group.singular === modifier.slot)?.key;
}

function ChartRolls({ document, model, modifier }: Pick<ItemsPanelProps, 'document' | 'model'> & { modifier: ItemModifier }) {
  const poolKey = chartPoolKey(modifier);
  if (!poolKey) {
    return null;
  }
  const editChart = (chartIndex: number, recipe: (draft: ItemDefinition) => void) => document.update((draft) => recipe(draft.charts.definitions[chartIndex]));
  return (
    <>
      <Divider />
      <Title order={5}>Rolls on</Title>
      <Text size="xs" c="dimmed">
        A higher weight makes it come up more often among the chart&apos;s {poolKey}; a blank weight counts as 1. A tier range (t3+, t2-6 or t5) keeps it out of charts outside those tiers.
      </Text>
      {model.charts.definitions.map((chart, chartIndex) => {
        const pool = chart[poolKey];
        const entryIndex = pool.findIndex((entry) => entry.inlineText === undefined && entry.modifierId === modifier.id);
        const entry = pool[entryIndex];
        return (
          <Paper key={chartIndex} withBorder p="xs">
            <Group gap="sm" align="flex-end" wrap="nowrap">
              <Text size="sm" fw={600} style={{ flex: 1 }} mb={6}>
                {chart.name || chart.id}
              </Text>
              {entry ? (
                <>
                  <DecimalInput
                    w={170}
                    label={`Weight (${Math.round(poolShares(pool)[entryIndex] * 100)}% of ${poolKey})`}
                    aria-label={`${chart.id} weight`}
                    placeholder="1"
                    value={entry.weight}
                    onChange={(text) =>
                      editChart(chartIndex, (draft) => {
                        const draftEntry = draft[poolKey][entryIndex];
                        draftEntry.weight = text;
                        delete draftEntry.emptyWeightField;
                      })
                    }
                  />
                  <TextInput
                    w={120}
                    label="Tiers"
                    aria-label={`${chart.id} tiers`}
                    placeholder="all"
                    value={entry.tiers ?? ''}
                    onChange={(event) => {
                      const tiers = event.currentTarget.value.trim();
                      editChart(chartIndex, (draft) => {
                        const draftEntry = draft[poolKey][entryIndex];
                        if (tiers) {
                          draftEntry.tiers = tiers;
                        } else {
                          delete draftEntry.tiers;
                        }
                      });
                    }}
                  />
                  <Tooltip label={`Remove from ${chart.name || chart.id}`}>
                    <ActionIcon variant="subtle" color="red" mb={6} onClick={() => editChart(chartIndex, (draft) => void draft[poolKey].splice(entryIndex, 1))} aria-label={`Remove from ${chart.id}`}>
                      <IconX size={16} />
                    </ActionIcon>
                  </Tooltip>
                </>
              ) : (
                <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={() => editChart(chartIndex, (draft) => void draft[poolKey].push({ modifierId: modifier.id, weight: '' }))}>
                  Add to {chart.name || chart.id}
                </Button>
              )}
            </Group>
          </Paper>
        );
      })}
    </>
  );
}
