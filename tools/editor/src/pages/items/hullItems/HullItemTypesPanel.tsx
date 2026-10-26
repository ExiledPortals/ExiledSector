import { Button, Divider, Group, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconCopy, IconPlus, IconSearch, IconTrash } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DescriptionField, IconField, IdField, ItemList, ListDetail, NameAndRarity, UniqueFields, type ListedItem } from '../components/ItemFields.tsx';
import { DescriptionNote, GameLines, MissingModifier } from '../components/GameText.tsx';
import { ModifierPool } from '../components/ModifierPool.tsx';
import { modifierLinesOf, useLineDescriptions, type LineDescriptions } from '../describedLines.ts';
import { hullItemKinds } from '../itemOptions.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import {
  AFFIX_GROUPS,
  HULL_ITEM_TYPE,
  isTrueText,
  itemTypeOfKind,
  newItemDefinition,
  setDefinitionKind,
  uniqueItemId,
  type ItemDefinition,
} from '../model/definitions.ts';
import { MODIFIER_FILE_NAMES } from '../model/itemFamily.ts';
import type { ItemModifier } from '../model/modifiers.ts';
import { otherItemIds, type ItemsModel } from '../model/workspace.ts';

const UNIQUE_FILTER = 'unique';
const COMMON_FILTER = 'common';
const WEIGHT_FILTER_PREFIX = 'weight:';

function matchesRarity(definition: ItemDefinition, rarityFilter: string): boolean {
  if (rarityFilter === UNIQUE_FILTER) {
    return isTrueText(definition.unique);
  }
  if (rarityFilter === COMMON_FILTER) {
    return !isTrueText(definition.unique);
  }
  return !rarityFilter.startsWith(WEIGHT_FILTER_PREFIX) || definition.rarity === rarityFilter.slice(WEIGHT_FILTER_PREFIX.length);
}

function rarityOptions(hullItems: ItemDefinition[]): { value: string; label: string }[] {
  const weights = [...new Set(hullItems.map((definition) => definition.rarity))].sort((first, second) => Number(first) - Number(second));
  const countOf = (filter: string) => hullItems.filter((definition) => matchesRarity(definition, filter)).length;
  return [
    { value: '', label: `All rarities (${hullItems.length})` },
    { value: UNIQUE_FILTER, label: `Unique (${countOf(UNIQUE_FILTER)})` },
    { value: COMMON_FILTER, label: `Not unique (${countOf(COMMON_FILTER)})` },
    ...weights.map((weight) => ({ value: WEIGHT_FILTER_PREFIX + weight, label: `Loot weight ${weight || 'blank'} (${countOf(WEIGHT_FILTER_PREFIX + weight)})` })),
  ];
}

export function HullItemTypesPanel({ document, model, vocabulary }: ItemsPanelProps) {
  const [selectedIndex, setSelectedIndex] = useState(() => model.ship.definitions.findIndex((definition) => itemTypeOfKind(definition.kind) === HULL_ITEM_TYPE));
  const [socketFilter, setSocketFilter] = useState('');
  const [rarityFilter, setRarityFilter] = useState('');
  const [searchText, setSearchText] = useState('');
  const hullModifiers = model.shipModifiers.modifiers.filter((modifier) => modifier.itemTypes.includes(HULL_ITEM_TYPE));
  const descriptions = useLineDescriptions(modifierLinesOf(hullModifiers, () => HULL_ITEM_TYPE));
  const kinds = hullItemKinds(vocabulary);
  const hullItems = model.ship.definitions.filter((definition) => itemTypeOfKind(definition.kind) === HULL_ITEM_TYPE);
  const needle = searchText.trim().toLowerCase();
  const listed: ListedItem[] = model.ship.definitions
    .map((definition, index) => ({ definition, index }))
    .filter(({ definition }) => itemTypeOfKind(definition.kind) === HULL_ITEM_TYPE)
    .filter(({ definition }) => !socketFilter || definition.kind === socketFilter)
    .filter(({ definition }) => matchesRarity(definition, rarityFilter))
    .filter(({ definition }) => !needle || `${definition.name} ${definition.id}`.toLowerCase().includes(needle))
    .map(({ definition, index }) => ({
      index,
      label: definition.name || definition.id,
      description: `${definition.kind}${isTrueText(definition.unique) ? ' - unique' : ''} - ${definition.prefixes.length} prefixes, ${definition.suffixes.length} suffixes`,
      icon: definition.icon,
    }));
  const selected = model.ship.definitions[selectedIndex];

  const addItem = () => {
    const index = model.ship.definitions.length;
    document.update((draft) => {
      draft.ship.definitions.push(newItemDefinition(draft.ship.definitions, socketFilter || kinds[0] || 'bridge', draft.shipModifiers.modifiers, otherItemIds(draft, 'ship')));
    });
    setSearchText('');
    setRarityFilter('');
    setSelectedIndex(index);
  };

  const socketOptions = [
    { value: '', label: `All socket types (${hullItems.length})` },
    ...kinds.map((kind) => ({ value: kind, label: `${kind} (${hullItems.filter((definition) => definition.kind === kind).length})` })),
  ];

  return (
    <ListDetail
      list={
        <>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addItem}>
            New hull item
          </Button>
          <Select data={socketOptions} value={socketFilter} onChange={(value) => setSocketFilter(value ?? '')} allowDeselect={false} aria-label="Socket type filter" />
          <Select data={rarityOptions(hullItems)} value={rarityFilter} onChange={(value) => setRarityFilter(value ?? '')} allowDeselect={false} aria-label="Rarity filter" />
          <TextInput
            placeholder="Name or id"
            aria-label="Search hull items"
            leftSection={<IconSearch size={14} />}
            value={searchText}
            onChange={(event) => setSearchText(event.currentTarget.value)}
          />
          <ItemList items={listed} selectedIndex={selectedIndex} onSelect={setSelectedIndex} emptyText={hullItems.length ? 'No hull items match.' : 'No hull items yet.'} />
        </>
      }
      detail={
        selected && itemTypeOfKind(selected.kind) === HULL_ITEM_TYPE ? (
          <HullItemForm
            key={selectedIndex}
            document={document}
            model={model}
            vocabulary={vocabulary}
            definition={selected}
            index={selectedIndex}
            descriptions={descriptions}
            onSelect={setSelectedIndex}
          />
        ) : (
          <Text c="dimmed">Select a hull item, or add a new one.</Text>
        )
      }
    />
  );
}

interface HullItemFormProps extends ItemsPanelProps {
  definition: ItemDefinition;
  index: number;
  descriptions: LineDescriptions;
  onSelect: (index: number) => void;
}

function ImplicitLines({ model, section, descriptions }: { model: ItemsModel; section: string; descriptions: LineDescriptions }) {
  const references = model.hullImplicits.rows.find((row) => row.section === section)?.implicits ?? [];
  const byId = new Map(model.shipModifiers.modifiers.map((modifier) => [modifier.id, modifier]));
  return (
    <Stack gap="xs">
      <Group gap="xs" align="baseline">
        <Title order={5}>Implicit</Title>
        <Text size="xs" c="dimmed">
          every {section} item rolls it; set in hull_item_implicits.csv
        </Text>
      </Group>
      {references.length === 0 ? (
        <Text size="sm" c="dimmed">
          None.
        </Text>
      ) : (
        references.map((reference, index) => {
          const modifier: ItemModifier | undefined = byId.get(reference.modifierId);
          return modifier ? (
            <GameLines key={index} lines={modifier.lines} itemType={HULL_ITEM_TYPE} descriptions={descriptions} />
          ) : (
            <MissingModifier key={index} text={`${reference.inlineText ?? reference.modifierId} is not in ${MODIFIER_FILE_NAMES.ship}`} />
          );
        })
      )}
    </Stack>
  );
}

function HullItemForm({ document, model, vocabulary, definition, index, descriptions, onSelect }: HullItemFormProps) {
  const updateDocument = document.update;
  const edit = useCallback((recipe: (draft: ItemDefinition) => void) => updateDocument((draft) => recipe(draft.ship.definitions[index])), [updateDocument, index]);
  const unique = isTrueText(definition.unique);
  const kinds = hullItemKinds(vocabulary);

  const duplicate = () => {
    document.update((draft) => {
      const copy = structuredClone(definition);
      copy.id = uniqueItemId(draft.ship.definitions, `${definition.id}_copy`, otherItemIds(draft, 'ship'));
      delete copy.loadedId;
      draft.ship.definitions.splice(index + 1, 0, copy);
    });
    onSelect(index + 1);
  };

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete "${definition.id}"?`,
      message: 'Items players already own keep their rolls but will show as unrecognised.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => void draft.ship.definitions.splice(index, 1));
      onSelect(Math.max(0, index - 1));
    }
  };

  return (
    <Stack gap="sm" maw={1100}>
      <IconField icon={definition.icon} targetFolder="graphics/icons/socketables/" onChange={(icon) => edit((draft) => void (draft.icon = icon))} />
      <Group grow align="flex-start">
        <IdField
          id={definition.id}
          loadedId={definition.loadedId}
          noun="item"
          renameNote={`Its rare-name words in item_names.json and its socketable.${definition.loadedId ?? definition.id}.* keys in zh_CN.json keep the old id and must be renamed by hand.`}
          onRename={(newId) => edit((draft) => void (draft.id = newId))}
        />
        <Select
          label="Kind (socket type)"
          data={kinds.includes(definition.kind) ? kinds : [definition.kind, ...kinds]}
          value={definition.kind}
          onChange={(value) => value && edit((draft) => setDefinitionKind(draft, value))}
          allowDeselect={false}
        />
      </Group>
      <NameAndRarity
        name={definition.name}
        rarity={definition.rarity}
        onName={(name) => edit((draft) => void (draft.name = name))}
        onRarity={(rarity) => edit((draft) => void (draft.rarity = rarity))}
      />
      <UniqueFields
        unique={unique}
        unlock={definition.unlock}
        vocabulary={vocabulary}
        uniqueLabel="Unique - shown with the name above in orange, and rolls every prefix and suffix"
        unlockLabel="Drop condition (uniques never drop before player level 15)"
        onUnique={(checked) =>
          edit((draft) => {
            draft.unique = checked ? 'true' : '';
            if (!checked) {
              draft.unlock = '';
            }
          })
        }
        onUnlock={(unlock) => edit((draft) => void (draft.unlock = unlock))}
      />
      <DescriptionField description={definition.description} onChange={(description) => edit((draft) => void (draft.description = description))} />
      <Divider />
      <DescriptionNote descriptions={descriptions} />
      <ImplicitLines model={model} section={definition.kind} descriptions={descriptions} />
      <Text size="xs" c="dimmed">
        {unique
          ? 'A unique rolls every prefix and suffix listed here.'
          : 'Items roll 2 effects (common: 1 prefix + 1 suffix) 55% of the time, 3 (rare) 35% and 4 (rare) 10%, never repeating a modifier. A higher weight makes a modifier come up more often within its group; a blank weight counts as 1.'}{' '}
        Values are edited in the Modifiers tab.
      </Text>
      {AFFIX_GROUPS.map((group) => (
        <ModifierPool
          key={group.key}
          title={group.label}
          hint={unique ? undefined : 'a common item rolls 1, a rare 1-2'}
          entries={definition[group.key]}
          catalogue={model.shipModifiers.modifiers}
          slot={group.singular}
          itemType={HULL_ITEM_TYPE}
          descriptions={descriptions}
          weighted={!unique}
          catalogueFileName={MODIFIER_FILE_NAMES.ship}
          onAdd={(modifierId) => edit((draft) => void draft[group.key].push({ modifierId, weight: '' }))}
          onRemove={(entryIndex) => edit((draft) => void draft[group.key].splice(entryIndex, 1))}
          onWeight={(entryIndex, weight) =>
            edit((draft) => {
              const entry = draft[group.key][entryIndex];
              entry.weight = weight;
              delete entry.emptyWeightField;
            })
          }
        />
      ))}
      <Divider />
      <Group>
        <Button variant="default" leftSection={<IconCopy size={14} />} onClick={duplicate}>
          Duplicate
        </Button>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this item
        </Button>
      </Group>
    </Stack>
  );
}
