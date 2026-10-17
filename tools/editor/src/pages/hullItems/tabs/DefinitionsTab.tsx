import { Avatar, Box, Button, Checkbox, Divider, Group, NavLink, ScrollArea, Select, Stack, Text, Textarea, TextInput, Title } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { useEffect, useMemo, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { confirmAction } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { moveItem } from '../../../shared/arrays.ts';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import {
  AFFIX_GROUPS,
  isTrueText,
  markDefinitionsSaved,
  newHullItem,
  parseHullItemsCsv,
  poolShares,
  serializeHullItemsCsv,
  UNLOCK_LABELS,
  uniqueHullItemId,
  validateHullItems,
  type HullItemDefinition,
  type HullItemsModel,
} from '../model/definitions.ts';
import { modifierById, parseModifiersCsv, type HullItemModifier } from '../model/modifiers.ts';
import { PoolEntryRow } from './PoolEntryRow.tsx';

const LIST_WIDTH = 270;

interface DefinitionsModel extends HullItemsModel {
  catalogue: HullItemModifier[];
}

export function DefinitionsTab({ vocabulary }: { vocabulary: Vocabulary }) {
  const document = useDataDocument<DefinitionsModel>({
    key: 'socketables.definitions',
    noun: 'hull item',
    fileNames: ['socketables', 'hull-item-modifiers'],
    parse: (files) => ({ ...parseHullItemsCsv(files.socketables.content), catalogue: parseModifiersCsv(files['hull-item-modifiers'].content).modifiers }),
    serialize: (model) => ({ socketables: serializeHullItemsCsv(model) }),
    validate: (model) => validateHullItems(model.definitions, vocabulary, modifierById(model.catalogue)),
    savedMessage: () => 'Saved socketables.csv',
    afterSave: markDefinitionsSaved,
  });
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [kindFilter, setKindFilter] = useState('');
  const definitions = document.state.status === 'ready' ? document.state.model.definitions : [];

  const addHullItem = () => {
    const kind = kindFilter || vocabulary.hullItemKinds[0] || 'subroutine';
    document.update((draft) => {
      draft.definitions.push(newHullItem(draft.definitions, kind, draft.catalogue));
    });
    setSelectedIndex(definitions.length);
  };

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addHullItem} disabled={document.state.status !== 'ready'}>
            New hull item
          </Button>
        </DataToolbar>
        <Text size="xs" c="dimmed" mt={4}>
          Edits data/config/exiledSector/socketables.csv. Names and descriptions are English; add zh text under socketable.&lt;id&gt;.name / .description in
          zh_CN.json.
        </Text>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
            <DefinitionList
              model={model}
              vocabulary={vocabulary}
              kindFilter={kindFilter}
              onKindFilter={setKindFilter}
              selectedIndex={selectedIndex}
              onSelect={setSelectedIndex}
            />
            <Divider orientation="vertical" />
            <ScrollArea style={{ flex: 1 }} type="auto">
              <Box p="md">
                {model.definitions[selectedIndex] ? (
                  <DefinitionForm
                    key={selectedIndex}
                    document={document}
                    definition={model.definitions[selectedIndex]}
                    index={selectedIndex}
                    vocabulary={vocabulary}
                    catalogue={model.catalogue}
                    onSelect={setSelectedIndex}
                  />
                ) : (
                  <Text c="dimmed">Select a hull item, or add a new one.</Text>
                )}
              </Box>
            </ScrollArea>
          </Group>
        )}
      </DocumentState>
    </Stack>
  );
}

interface DefinitionListProps {
  model: DefinitionsModel;
  vocabulary: Vocabulary;
  kindFilter: string;
  onKindFilter: (kind: string) => void;
  selectedIndex: number;
  onSelect: (index: number) => void;
}

function DefinitionList({ model, vocabulary, kindFilter, onKindFilter, selectedIndex, onSelect }: DefinitionListProps) {
  const kindOptions = useMemo(() => {
    const kinds = [...vocabulary.hullItemKinds];
    for (const definition of model.definitions) {
      if (!kinds.includes(definition.kind)) {
        kinds.push(definition.kind);
      }
    }
    const countOf = (kind: string) => model.definitions.filter((definition) => definition.kind === kind).length;
    return [
      { value: '', label: `All socket types (${model.definitions.length})` },
      ...kinds.map((kind) => ({ value: kind, label: `${kind} (${countOf(kind)})` })),
    ];
  }, [model.definitions, vocabulary.hullItemKinds]);

  const shown = model.definitions.map((definition, index) => ({ definition, index })).filter(({ definition }) => !kindFilter || definition.kind === kindFilter);

  return (
    <Stack w={LIST_WIDTH} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
      <Select data={kindOptions} value={kindFilter} onChange={(value) => onKindFilter(value ?? '')} allowDeselect={false} aria-label="Socket type filter" />
      <ScrollArea style={{ flex: 1 }} type="auto">
        {shown.length === 0 ? (
          <Text size="sm" c="dimmed">
            {kindFilter && model.definitions.length ? 'No hull items of this type.' : 'No hull items yet.'}
          </Text>
        ) : (
          shown.map(({ definition, index }) => (
            <NavLink
              key={index}
              active={index === selectedIndex}
              onClick={() => onSelect(index)}
              label={definition.name || definition.id}
              description={`${definition.kind} - ${definition.prefixes.length} prefixes, ${definition.suffixes.length} suffixes`}
              leftSection={<Avatar src={fileUrl(definition.icon)} radius="sm" size={32} />}
            />
          ))
        )}
      </ScrollArea>
    </Stack>
  );
}

interface DefinitionFormProps {
  document: DataDocument<DefinitionsModel>;
  definition: HullItemDefinition;
  index: number;
  vocabulary: Vocabulary;
  catalogue: HullItemModifier[];
  onSelect: (index: number) => void;
}

function DefinitionForm({ document, definition, index, vocabulary, catalogue, onSelect }: DefinitionFormProps) {
  const [idText, setIdText] = useState(definition.id);
  useEffect(() => setIdText(definition.id), [definition.id]);

  const edit = (recipe: (draft: HullItemDefinition) => void) =>
    document.update((draft) => {
      recipe(draft.definitions[index]);
    });

  const commitId = async () => {
    const newId = idText.trim();
    if (newId === definition.id) {
      return;
    }
    if (definition.loadedId && definition.id === definition.loadedId) {
      const rename = await confirmAction({
        title: `Rename "${definition.loadedId}" to "${newId}"?`,
        message:
          'Saved games store this id on every item players own, so once it has shipped those items turn into unknown hull items. ' +
          `Its rare-name words in socketable_names.json and its socketable.${definition.loadedId}.* keys in zh_CN.json keep the old id and must be renamed by hand.`,
        confirmLabel: 'Rename',
        danger: true,
      });
      if (!rename) {
        setIdText(definition.id);
        return;
      }
    }
    edit((draft) => void (draft.id = newId));
  };

  const kinds = vocabulary.hullItemKinds.includes(definition.kind) ? vocabulary.hullItemKinds : [definition.kind, ...vocabulary.hullItemKinds];
  const unlockIds = ['', ...vocabulary.hullItemUnlocks];
  if (!unlockIds.includes(definition.unlock)) {
    unlockIds.push(definition.unlock);
  }
  const unique = isTrueText(definition.unique);

  const duplicate = () => {
    document.update((draft) => {
      const copy = structuredClone(definition);
      copy.id = uniqueHullItemId(draft.definitions, `${definition.id}_copy`);
      delete copy.loadedId;
      draft.definitions.splice(index + 1, 0, copy);
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
      document.update((draft) => {
        draft.definitions.splice(index, 1);
      });
      onSelect(Math.max(0, index - 1));
    }
  };

  return (
    <Stack gap="sm" maw={1100}>
      <Group align="flex-end" wrap="nowrap">
        <Avatar src={fileUrl(definition.icon)} radius="sm" size={64} />
        <TextInput style={{ flex: 1 }} label="Icon path" value={definition.icon} onChange={(event) => edit((draft) => void (draft.icon = event.currentTarget.value))} />
        <ImagePickButton targetFolder="graphics/icons/socketables/" onPicked={(path) => edit((draft) => void (draft.icon = path))} />
      </Group>
      <Group grow align="flex-start">
        <TextInput
          label={definition.loadedId ? 'Id (saved games store it - renaming orphans owned items)' : 'Id'}
          value={idText}
          onChange={(event) => setIdText(event.currentTarget.value)}
          onBlur={() => void commitId()}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.currentTarget.blur();
            }
          }}
        />
        <Select label="Kind" data={kinds} value={definition.kind} onChange={(value) => value && edit((draft) => void (draft.kind = value))} allowDeselect={false} />
      </Group>
      <TextInput label="Name" value={definition.name} onChange={(event) => edit((draft) => void (draft.name = event.currentTarget.value))} />
      <DecimalInput w={220} label="Rarity (loot weight)" value={definition.rarity} onChange={(text) => edit((draft) => void (draft.rarity = text))} />
      <Checkbox
        label="Unique - shown with the name above in orange, and rolls every prefix and suffix"
        checked={unique}
        onChange={(event) => {
          const checked = event.currentTarget.checked;
          edit((draft) => {
            draft.unique = checked ? 'true' : '';
            if (!checked) {
              draft.unlock = '';
            }
          });
        }}
      />
      {unique && (
        <Select
          label="Drop condition (uniques never drop before player level 15)"
          data={unlockIds.map((unlockId) => ({ value: unlockId, label: unlockId ? (UNLOCK_LABELS[unlockId] ?? unlockId) : 'None' }))}
          value={definition.unlock}
          onChange={(value) => edit((draft) => void (draft.unlock = value ?? ''))}
          allowDeselect={false}
        />
      )}
      <Textarea
        label="Tooltip description (optional)"
        autosize
        minRows={3}
        value={definition.description}
        onChange={(event) => edit((draft) => void (draft.description = event.currentTarget.value))}
      />
      <Divider />
      <Text size="xs" c="dimmed">
        Items roll 2 effects (common: 1 prefix + 1 suffix) 55% of the time, 3 (rare: 2+1 or 1+2) 35% and 4 (rare: 2+2) 10%, never repeating a modifier. A
        higher rarity weight makes a modifier come up more often within its group; a blank weight counts as 1. Each entry picks a modifier from
        hull_item_modifiers.csv (Modifiers tab), which holds its rolled ranges and its name.
      </Text>
      {AFFIX_GROUPS.map((group) => {
        const pool = definition[group.key];
        const shares = poolShares(pool);
        return (
          <Stack key={group.key} gap="xs">
            <Title order={5}>{group.label} - a common item rolls 1, a rare 1-2</Title>
            {pool.length === 0 && (
              <Text size="sm" c="dimmed">
                None yet.
              </Text>
            )}
            {pool.map((entry, entryIndex) => (
              <PoolEntryRow
                key={entryIndex}
                entry={entry}
                slot={group.singular}
                index={entryIndex}
                count={pool.length}
                share={shares[entryIndex]}
                catalogue={catalogue}
                onEdit={(recipe) => edit((draft) => recipe(draft[group.key][entryIndex]))}
                onMove={(from, to) => edit((draft) => moveItem(draft[group.key], from, to))}
                onRemove={() => edit((draft) => void draft[group.key].splice(entryIndex, 1))}
              />
            ))}
            <Button
              size="xs"
              variant="default"
              w="fit-content"
              leftSection={<IconPlus size={14} />}
              onClick={() => edit((draft) => void draft[group.key].push({ modifierId: '', weight: '' }))}
            >
              Add {group.singular}
            </Button>
          </Stack>
        );
      })}
      <Divider />
      <Group>
        <Button variant="default" leftSection={<IconCopy size={14} />} onClick={duplicate}>
          Duplicate
        </Button>
        <Button color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
          Delete this hull item
        </Button>
      </Group>
    </Stack>
  );
}
