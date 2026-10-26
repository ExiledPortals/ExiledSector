import { Button, Divider, Group, MultiSelect, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import type { Draft } from 'immer';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DoctrineReplacementPools, type DoctrineTypeSource } from '../components/DoctrineArchiveView.tsx';
import { DescriptionNote } from '../components/GameText.tsx';
import { DescriptionField, IconField, IdField, ItemList, ListDetail, NameAndRarity, type ListedItem } from '../components/ItemFields.tsx';
import { ModifierPool } from '../components/ModifierPool.tsx';
import { modifierLinesOf, useLineDescriptions, type LineDescriptions } from '../describedLines.ts';
import type { ItemsPanelProps } from '../itemsDocument.ts';
import { newItemDefinition, SUBROUTINE_KIND, uniqueItemId, type ItemDefinition } from '../model/definitions.ts';
import {
  AUGMENT_BEHAVIOUR,
  DOCTRINE_POOL_TIERS,
  DOCTRINE_SUBROUTINE_KIND,
  DOCTRINE_TYPE,
  doctrineModifierReferences,
  formatDoctrineModifiers,
  joinEntries,
  listEntries,
  newDoctrineArchive,
  renameDoctrineArchive,
  replacementCandidates,
  TRANSFORM_BEHAVIOUR,
  type DoctrineArchive,
  type DoctrineModifierReference,
  type DoctrinePoolKey,
} from '../model/doctrines.ts';
import { MODIFIER_FILE_NAMES } from '../model/itemFamily.ts';
import { otherItemIds, type ItemsModel } from '../model/workspace.ts';

const POOL_LABELS: Record<DoctrinePoolKey, string> = {
  notables: 'Replacement notables',
  keystones: 'Replacement keystones',
  smallNodes: 'Replacement small nodes',
};

function isDoctrine(definition: ItemDefinition | undefined): definition is ItemDefinition {
  return definition?.kind === SUBROUTINE_KIND && definition.subroutineKind === DOCTRINE_SUBROUTINE_KIND;
}

function withCurrent(options: string[], current: readonly string[]): string[] {
  return [...current.filter((value) => !options.includes(value)), ...options];
}

export function DoctrineArchivesPanel({ document, model, vocabulary }: ItemsPanelProps) {
  const [selectedIndex, setSelectedIndex] = useState(() => model.ship.definitions.findIndex(isDoctrine));
  const doctrineModifiers = model.shipModifiers.modifiers.filter((modifier) => modifier.itemTypes.includes(DOCTRINE_TYPE));
  const descriptions = useLineDescriptions(modifierLinesOf(doctrineModifiers, () => DOCTRINE_TYPE));
  const listed: ListedItem[] = model.ship.definitions
    .map((definition, index) => ({ definition, index }))
    .filter(({ definition }) => isDoctrine(definition))
    .map(({ definition, index }) => {
      const archive = model.doctrines.archives.find((candidate) => candidate.id === definition.id);
      return {
        index,
        label: definition.name || definition.id,
        description: archive ? `${archive.behaviour || 'no behaviour'} - ${archive.radius || 'no radius'}` : 'no archive row',
        icon: definition.icon,
      };
    });
  const doctrineIds = new Set(listed.map((item) => model.ship.definitions[item.index].id));
  const orphanRows = model.doctrines.archives.filter((archive) => !doctrineIds.has(archive.id)).map((archive) => archive.id || '(blank id)');
  const selected = model.ship.definitions[selectedIndex];

  const addDoctrine = () => {
    const index = model.ship.definitions.length;
    document.update((draft) => {
      const definition = newItemDefinition(draft.ship.definitions, SUBROUTINE_KIND, draft.shipModifiers.modifiers, otherItemIds(draft, 'ship'), DOCTRINE_SUBROUTINE_KIND);
      definition.id = uniqueItemId(draft.ship.definitions, 'new_doctrine_archive', otherItemIds(draft, 'ship'));
      definition.name = 'New Doctrine Archive';
      draft.ship.definitions.push(definition);
      draft.doctrines.archives.push({ ...newDoctrineArchive(draft.doctrines.archives, [], vocabulary), id: definition.id });
    });
    setSelectedIndex(index);
  };

  return (
    <ListDetail
      list={
        <>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addDoctrine}>
            New doctrine archive
          </Button>
          {orphanRows.length > 0 && (
            <Text size="xs" c="orange">
              doctrine_archives.csv has rows without a doctrine subroutine: {orphanRows.join(', ')}
            </Text>
          )}
          <ItemList items={listed} selectedIndex={selectedIndex} onSelect={setSelectedIndex} emptyText="No doctrine archives yet." />
        </>
      }
      detail={
        isDoctrine(selected) ? (
          <DoctrineForm
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
          <Text c="dimmed">Select a doctrine archive, or add a new one.</Text>
        )
      }
    />
  );
}

interface DoctrineFormProps extends ItemsPanelProps {
  definition: ItemDefinition;
  index: number;
  descriptions: LineDescriptions;
  onSelect: (index: number) => void;
}

function draftArchive(draft: Draft<ItemsModel>, doctrineId: string): DoctrineArchive | undefined {
  return draft.doctrines.archives.find((archive) => archive.id === doctrineId);
}

function DoctrineForm({ document, model, vocabulary, definition, index, descriptions, onSelect }: DoctrineFormProps) {
  const updateDocument = document.update;
  const edit = useCallback((recipe: (draft: ItemDefinition) => void) => updateDocument((draft) => recipe(draft.ship.definitions[index])), [updateDocument, index]);
  const archive = model.doctrines.archives.find((candidate) => candidate.id === definition.id);
  const editArchive = (recipe: (draft: DoctrineArchive) => void) =>
    document.update((draft) => {
      const target = draftArchive(draft, definition.id);
      if (target) {
        recipe(target);
      }
    });

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete "${definition.id}"?`,
      message: 'Removes it from subroutines.csv and its row from doctrine_archives.csv. Archives players already own will show as unrecognised.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => {
        draft.ship.definitions.splice(index, 1);
        draft.doctrines.archives = draft.doctrines.archives.filter((candidate) => candidate.id !== definition.id);
      });
      onSelect(Math.max(0, index - 1));
    }
  };

  return (
    <Stack gap="sm" maw={1100}>
      <IconField icon={definition.icon} targetFolder="graphics/icons/socketables/" onChange={(icon) => edit((draft) => void (draft.icon = icon))} />
      <IdField
        id={definition.id}
        loadedId={definition.loadedId}
        noun="doctrine archive"
        renameNote={`Its row in doctrine_archives.csv is renamed and saved with it. Its socketable.${definition.loadedId ?? definition.id}.* keys in zh_CN.json keep the old id and must be renamed by hand.`}
        onRename={(newId) =>
          document.update((draft) => {
            const oldId = draft.ship.definitions[index].id;
            draft.ship.definitions[index].id = newId;
            renameDoctrineArchive(draft.doctrines.archives, oldId, newId);
          })
        }
      />
      <NameAndRarity
        name={definition.name}
        rarity={definition.rarity}
        onName={(name) => edit((draft) => void (draft.name = name))}
        onRarity={(rarity) => edit((draft) => void (draft.rarity = rarity))}
      />
      <DescriptionField description={definition.description} onChange={(description) => edit((draft) => void (draft.description = description))} />
      <Divider />
      <Title order={5}>Doctrine archive</Title>
      {archive ? (
        <ArchiveFields model={model} vocabulary={vocabulary} archive={archive} descriptions={descriptions} editArchive={editArchive} document={document} />
      ) : (
        <Stack gap="xs" align="flex-start">
          <Text size="sm" c="orange">
            No row in doctrine_archives.csv, so it rewrites nothing.
          </Text>
          <Button
            size="xs"
            variant="default"
            leftSection={<IconPlus size={14} />}
            onClick={() => document.update((draft) => void draft.doctrines.archives.push({ ...newDoctrineArchive(draft.doctrines.archives, [], vocabulary), id: definition.id }))}
          >
            Add its doctrine archive row
          </Button>
        </Stack>
      )}
      <Divider />
      <Group>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this doctrine archive
        </Button>
      </Group>
    </Stack>
  );
}

interface ArchiveFieldsProps extends Pick<ItemsPanelProps, 'document'> {
  model: ItemsModel;
  vocabulary: Vocabulary;
  archive: DoctrineArchive;
  descriptions: LineDescriptions;
  editArchive: (recipe: (draft: DoctrineArchive) => void) => void;
}

function typeSource({ document, model }: Pick<ItemsPanelProps, 'document' | 'model'>): DoctrineTypeSource {
  return {
    typesById: new Map(model.skillTypes.map((type) => [type.id, type])),
    typesJson: model.typesText,
    typesKey: model.typesKey,
    onIconPicked: (typeId, iconPath) =>
      document.update((draft) => {
        const type = draft.skillTypes.find((candidate) => candidate.id === typeId);
        if (type) {
          type.icon = iconPath;
        }
      }),
  };
}

function ArchiveFields({ document, model, vocabulary, archive, descriptions, editArchive }: ArchiveFieldsProps) {
  const references = doctrineModifierReferences(archive);
  const setReferences = (recipe: (draft: DoctrineModifierReference[]) => void) =>
    editArchive((draft) => {
      const draftReferences = doctrineModifierReferences(draft);
      recipe(draftReferences);
      draft.modifiers = formatDoctrineModifiers(draftReferences);
    });
  return (
    <>
      <Group grow align="flex-start">
        <Select
          label="Behaviour"
          description="Transform swaps nodes for replacements; augment adds lines to every node"
          data={withCurrent(vocabulary.doctrineBehaviours, archive.behaviour === '' ? [] : [archive.behaviour])}
          value={archive.behaviour === '' ? null : archive.behaviour}
          onChange={(value) => editArchive((draft) => void (draft.behaviour = value ?? ''))}
          allowDeselect={false}
        />
        <Select
          label="Radius"
          description="Sizes are set in node_radii.json"
          data={withCurrent(vocabulary.radiusSizes, archive.radius === '' ? [] : [archive.radius])}
          value={archive.radius === '' ? null : archive.radius}
          onChange={(value) => editArchive((draft) => void (draft.radius = value ?? ''))}
          allowDeselect={false}
        />
        <TextInput
          label="Faction"
          description="Vanilla faction id whose colour marks the radius"
          value={archive.faction}
          onChange={(event) => {
            const faction = event.currentTarget.value;
            editArchive((draft) => void (draft.faction = faction));
          }}
        />
      </Group>
      {archive.behaviour !== AUGMENT_BEHAVIOUR && (
        <>
          {(Object.keys(DOCTRINE_POOL_TIERS) as DoctrinePoolKey[]).map((poolKey) => {
            const entries = listEntries(archive[poolKey]);
            return (
              <MultiSelect
                key={poolKey}
                label={POOL_LABELS[poolKey]}
                description={`${DOCTRINE_POOL_TIERS[poolKey]} skill types, tried in an order the item seed picks`}
                data={withCurrent(replacementCandidates(model.typeFacts, DOCTRINE_POOL_TIERS[poolKey]), entries)}
                value={entries}
                onChange={(values) => editArchive((draft) => void (draft[poolKey] = joinEntries(values)))}
                searchable
              />
            );
          })}
          <DoctrineReplacementPools archive={archive} source={typeSource({ document, model })} />
        </>
      )}
      {archive.behaviour !== TRANSFORM_BEHAVIOUR && (
        <>
          <Group grow align="flex-start">
            <TextInput
              label="Min lines per node"
              value={archive.minLines}
              onChange={(event) => {
                const text = event.currentTarget.value;
                editArchive((draft) => void (draft.minLines = text));
              }}
            />
            <TextInput
              label="Max lines per node"
              value={archive.maxLines}
              onChange={(event) => {
                const text = event.currentTarget.value;
                editArchive((draft) => void (draft.maxLines = text));
              }}
            />
          </Group>
          <DescriptionNote descriptions={descriptions} />
          <ModifierPool
            title="Lines added to each node"
            hint="doctrine-scoped prefixes; values are edited in Hull Items > Modifiers"
            entries={references.map((reference) => ({ modifierId: reference.modifierId, weight: reference.weight ?? '' }))}
            catalogue={model.shipModifiers.modifiers}
            slot="prefix"
            itemType={DOCTRINE_TYPE}
            descriptions={descriptions}
            weighted
            catalogueFileName={MODIFIER_FILE_NAMES.ship}
            onAdd={(modifierId) => setReferences((draft) => void draft.push({ modifierId }))}
            onRemove={(entryIndex) => setReferences((draft) => void draft.splice(entryIndex, 1))}
            onWeight={(entryIndex, weight) => setReferences((draft) => void (draft[entryIndex].weight = weight))}
          />
        </>
      )}
    </>
  );
}
