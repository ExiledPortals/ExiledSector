import { Box, Button, Divider, Group, MultiSelect, NavLink, ScrollArea, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { parseItemDefinitions } from '../model/definitions.ts';
import {
  AUGMENT_BEHAVIOUR,
  DOCTRINE_ARCHIVE_FILE_NAME,
  DOCTRINE_ARCHIVE_RESOURCE,
  DOCTRINE_POOL_TIERS,
  DOCTRINE_SUBROUTINE_KIND,
  doctrineModifierIds,
  doctrineTypeFacts,
  joinEntries,
  listEntries,
  markDoctrinesSaved,
  newDoctrineArchive,
  parseDoctrineArchivesCsv,
  replacementCandidates,
  serializeDoctrineArchivesCsv,
  TRANSFORM_BEHAVIOUR,
  uncoveredDoctrines,
  validateDoctrineArchives,
  type DoctrineArchive,
  type DoctrineArchivesModel,
  type DoctrinePoolKey,
  type DoctrineTypeFacts,
} from '../model/doctrines.ts';
import { DEFINITION_FAMILY_OPTIONS, type DefinitionFamily } from '../model/itemFamily.ts';
import { parseModifiersCsv, type ItemModifier } from '../model/modifiers.ts';
import { FamilyOptionSwitch } from './FamilySwitch.tsx';

const LIST_WIDTH = 270;
const POOL_LABELS: Record<DoctrinePoolKey, string> = {
  notables: 'Replacement notables',
  keystones: 'Replacement keystones',
  smallNodes: 'Replacement small nodes',
};

interface DoctrineDocumentModel extends DoctrineArchivesModel {
  doctrineSubroutineIds: string[];
  doctrineNames: Record<string, string>;
  typeFacts: Record<string, DoctrineTypeFacts>;
  modifiers: ItemModifier[];
}

export function DoctrineArchivesPanel({ vocabulary, onFamily }: { vocabulary: Vocabulary; onFamily: (family: DefinitionFamily) => void }) {
  const document = useDataDocument<DoctrineDocumentModel>({
    key: 'items.doctrines',
    noun: 'doctrine archive',
    fileNames: [DOCTRINE_ARCHIVE_RESOURCE, 'subroutines', 'hull-items', 'types', 'item-modifiers'],
    parse: (files) => {
      const doctrineDefinitions = parseItemDefinitions(files.subroutines.content, files['hull-items'].content).definitions.filter(
        (definition) => definition.subroutineKind === DOCTRINE_SUBROUTINE_KIND,
      );
      return {
        ...parseDoctrineArchivesCsv(files[DOCTRINE_ARCHIVE_RESOURCE].content),
        doctrineSubroutineIds: doctrineDefinitions.map((definition) => definition.id),
        doctrineNames: Object.fromEntries(doctrineDefinitions.map((definition) => [definition.id, definition.name])),
        typeFacts: doctrineTypeFacts(files.types.content),
        modifiers: parseModifiersCsv(files['item-modifiers'].content).modifiers,
      };
    },
    serialize: (model) => ({ [DOCTRINE_ARCHIVE_RESOURCE]: serializeDoctrineArchivesCsv(model) }),
    validate: (model) => validateDoctrineArchives(model.archives, vocabulary, model.doctrineSubroutineIds, model.typeFacts, model.modifiers),
    warnings: (model) =>
      uncoveredDoctrines(model.archives, model.doctrineSubroutineIds).map((doctrineId) => `${doctrineId} has no row, so it rewrites nothing`),
    savedMessage: () => `Saved ${DOCTRINE_ARCHIVE_FILE_NAME}`,
    afterSave: (draft, saved) => markDoctrinesSaved(draft, saved),
  });
  const [selectedIndex, setSelectedIndex] = useState(0);
  const archiveCount = document.state.status === 'ready' ? document.state.model.archives.length : 0;

  const addArchive = () => {
    document.update((draft) => {
      draft.archives.push(newDoctrineArchive(draft.archives, draft.doctrineSubroutineIds, vocabulary));
    });
    setSelectedIndex(archiveCount);
  };

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilyOptionSwitch<DoctrineDocumentModel, DefinitionFamily>
            document={document}
            family="doctrine"
            onFamily={onFamily}
            noun="doctrine archive"
            options={DEFINITION_FAMILY_OPTIONS}
          />
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addArchive} disabled={document.state.status !== 'ready'}>
            New doctrine archive
          </Button>
        </DataToolbar>
        <Text size="xs" c="dimmed" mt={4}>
          Edits data/config/exiledSector/doctrine_archives.csv. Each row belongs to a {DOCTRINE_SUBROUTINE_KIND} subroutine in subroutines.csv. A
          transforming doctrine swaps notables, keystones and small nodes in its radius for replacements from its pools; an augmenting doctrine adds
          lines from its doctrine-scoped modifiers in item_modifiers.csv. The item seed picks each rewrite.
        </Text>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
            <Stack w={LIST_WIDTH} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
              <ScrollArea style={{ flex: 1 }} type="auto">
                {model.archives.length === 0 ? (
                  <Text size="sm" c="dimmed">
                    No doctrine archives yet.
                  </Text>
                ) : (
                  model.archives.map((archive, index) => (
                    <NavLink
                      key={index}
                      active={index === selectedIndex}
                      onClick={() => setSelectedIndex(index)}
                      label={model.doctrineNames[archive.id] || archive.id || 'New doctrine archive'}
                      description={`${archive.behaviour || 'no behaviour'} - ${archive.radius || 'no radius'}`}
                    />
                  ))
                )}
              </ScrollArea>
            </Stack>
            <Divider orientation="vertical" />
            <ScrollArea style={{ flex: 1 }} type="auto">
              <Box p="md">
                {model.archives[selectedIndex] ? (
                  <DoctrineForm
                    key={selectedIndex}
                    document={document}
                    archive={model.archives[selectedIndex]}
                    index={selectedIndex}
                    vocabulary={vocabulary}
                    model={model}
                    onSelect={setSelectedIndex}
                  />
                ) : (
                  <Text c="dimmed">Select a doctrine archive, or add a new one.</Text>
                )}
              </Box>
            </ScrollArea>
          </Group>
        )}
      </DocumentState>
    </Stack>
  );
}

interface DoctrineFormProps {
  document: DataDocument<DoctrineDocumentModel>;
  archive: DoctrineArchive;
  index: number;
  vocabulary: Vocabulary;
  model: DoctrineDocumentModel;
  onSelect: (index: number) => void;
}

function withCurrent(options: string[], current: readonly string[]): string[] {
  return [...current.filter((value) => !options.includes(value)), ...options];
}

function DoctrineForm({ document, archive, index, vocabulary, model, onSelect }: DoctrineFormProps) {
  const updateDocument = document.update;
  const edit = useCallback(
    (recipe: (draft: DoctrineArchive) => void) =>
      updateDocument((draft) => {
        recipe(draft.archives[index]);
      }),
    [updateDocument, index],
  );

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Delete the row for "${archive.id}"?`,
      message: 'The doctrine subroutine stays in subroutines.csv, but it rewrites nothing until it has a row again.',
      confirmLabel: 'Delete',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => {
        draft.archives.splice(index, 1);
      });
      onSelect(Math.max(0, index - 1));
    }
  };

  const doctrineIds = withCurrent(model.doctrineSubroutineIds, archive.id === '' ? [] : [archive.id]);
  const modifierEntries = listEntries(archive.modifiers);
  const modifierOptions = withCurrent(
    doctrineModifierIds(model.modifiers).map((modifierId) => `@${modifierId}`),
    modifierEntries,
  );

  return (
    <Stack gap="sm" maw={1100}>
      <Select
        label="Doctrine subroutine"
        description="The DOCTRINE row in subroutines.csv this archive belongs to"
        data={doctrineIds.map((doctrineId) => ({ value: doctrineId, label: model.doctrineNames[doctrineId] ? `${model.doctrineNames[doctrineId]} (${doctrineId})` : doctrineId }))}
        value={archive.id === '' ? null : archive.id}
        onChange={(value) => edit((draft) => void (draft.id = value ?? ''))}
        allowDeselect={false}
        searchable
      />
      <Group grow align="flex-start">
        <Select
          label="Behaviour"
          description="Transform swaps nodes for replacements; augment adds lines to every node"
          data={withCurrent(vocabulary.doctrineBehaviours, archive.behaviour === '' ? [] : [archive.behaviour])}
          value={archive.behaviour === '' ? null : archive.behaviour}
          onChange={(value) => edit((draft) => void (draft.behaviour = value ?? ''))}
          allowDeselect={false}
        />
        <Select
          label="Radius"
          description="Sizes are set in node_radii.json"
          data={withCurrent(vocabulary.radiusSizes, archive.radius === '' ? [] : [archive.radius])}
          value={archive.radius === '' ? null : archive.radius}
          onChange={(value) => edit((draft) => void (draft.radius = value ?? ''))}
          allowDeselect={false}
        />
      </Group>
      {archive.behaviour !== AUGMENT_BEHAVIOUR &&
        (Object.keys(DOCTRINE_POOL_TIERS) as DoctrinePoolKey[]).map((poolKey) => {
          const entries = listEntries(archive[poolKey]);
          return (
            <MultiSelect
              key={poolKey}
              label={POOL_LABELS[poolKey]}
              description={`${DOCTRINE_POOL_TIERS[poolKey]} skill types, tried in an order the item seed picks`}
              data={withCurrent(replacementCandidates(model.typeFacts, DOCTRINE_POOL_TIERS[poolKey]), entries)}
              value={entries}
              onChange={(values) => edit((draft) => void (draft[poolKey] = joinEntries(values)))}
              searchable
            />
          );
        })}
      {archive.behaviour !== TRANSFORM_BEHAVIOUR && (
        <>
          <MultiSelect
            label="Modifiers"
            description="Doctrine-scoped prefixes from item_modifiers.csv; write @ID:weight in the file for a weight"
            data={modifierOptions}
            value={modifierEntries}
            onChange={(values) => edit((draft) => void (draft.modifiers = joinEntries(values)))}
            searchable
          />
          <Group grow align="flex-start">
            <TextInput
              label="Min lines per node"
              value={archive.minLines}
              onChange={(event) => {
                const text = event.currentTarget.value;
                edit((draft) => void (draft.minLines = text));
              }}
            />
            <TextInput
              label="Max lines per node"
              value={archive.maxLines}
              onChange={(event) => {
                const text = event.currentTarget.value;
                edit((draft) => void (draft.maxLines = text));
              }}
            />
          </Group>
        </>
      )}
      <Divider />
      <Group>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this row
        </Button>
      </Group>
    </Stack>
  );
}
