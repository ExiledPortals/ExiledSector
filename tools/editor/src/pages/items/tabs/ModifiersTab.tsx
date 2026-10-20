import { Badge, Box, Button, Divider, Group, MultiSelect, NavLink, ScrollArea, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconCopy, IconPlus, IconSearch, IconTrash } from '@tabler/icons-react';
import { useEffect, useMemo, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { confirmAction, notifyError } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { ModifierLineRow } from './ModifierLineRow.tsx';
import { FamilySwitch } from './FamilySwitch.tsx';
import { ITEM_TYPE_LABELS, parseFamilyDefinitions, serializeFamilyDefinitions, type FamilyDefinitions } from '../model/definitions.ts';
import {
  CORRUPTION_POOL_FILE_NAME,
  CORRUPTION_POOL_LABEL,
  CORRUPTION_POOL_RESOURCE,
  implicitItemType,
  parseCorruptionPoolCsv,
  parseFamilyImplicits,
  serializeCorruptionPoolCsv,
  serializeFamilyImplicits,
  type CorruptionPoolModel,
  type ImplicitReference,
  type ImplicitsModel,
} from '../model/implicits.ts';
import {
  DEFINITION_FILE_NAMES,
  DEFINITION_RESOURCES,
  IMPLICIT_FILE_NAMES,
  IMPLICIT_RESOURCES,
  MODIFIER_FILE_NAMES,
  MODIFIER_RESOURCES,
  type ItemFamily,
} from '../model/itemFamily.ts';
import { DOCTRINE_ARCHIVE_FILE_NAME, DOCTRINE_ARCHIVE_RESOURCE, parseDoctrineArchivesCsv, serializeDoctrineArchivesCsv, type DoctrineArchivesModel } from '../model/doctrines.ts';
import { isUsed, modifierUsage, referenceIssues, renameModifierReferences, usedByText } from '../model/modifierUsage.ts';
import {
  familyItemTypes,
  idConventionIssue,
  markModifiersSaved,
  modifierById,
  modifierSummary,
  MODIFIER_SLOT_LABELS,
  MODIFIER_SLOTS,
  newModifier,
  newModifierLine,
  parseModifiersCsv,
  serializeModifiersCsv,
  suggestedModifierId,
  uniqueModifierId,
  validateModifiers,
  type ItemModifier,
  type ModifiersModel,
  type ModifierSlot,
  type ModifierUsage,
} from '../model/modifiers.ts';

const LIST_WIDTH = 320;

interface ModifiersTabModel {
  family: ItemFamily;
  catalogue: ModifiersModel;
  items: FamilyDefinitions;
  implicits: ImplicitsModel;
  doctrines: DoctrineArchivesModel | null;
  corruptionPool: CorruptionPoolModel | null;
  otherFileIds: string[];
  referencesChanged: boolean;
}

function otherFamily(family: ItemFamily): ItemFamily {
  return family === 'chart' ? 'ship' : 'chart';
}

function modifiersFileNames(family: ItemFamily): string[] {
  const fileNames = [MODIFIER_RESOURCES[family], ...DEFINITION_RESOURCES[family], IMPLICIT_RESOURCES[family], MODIFIER_RESOURCES[otherFamily(family)]];
  return family === 'ship' ? [...fileNames, DOCTRINE_ARCHIVE_RESOURCE, CORRUPTION_POOL_RESOURCE] : fileNames;
}

function poolReferences(model: ModifiersTabModel): ImplicitReference[] {
  return model.corruptionPool?.entries.map((entry) => entry.implicit) ?? [];
}

function usageOf(model: ModifiersTabModel): Map<string, ModifierUsage> {
  return modifierUsage(model.items.definitions, model.implicits.rows, implicitItemType(model.family), model.doctrines?.archives ?? [], poolReferences(model));
}

export function ModifiersTab({ vocabulary }: { vocabulary: Vocabulary }) {
  const [family, setFamily] = useState<ItemFamily>('ship');
  const document = useDataDocument<ModifiersTabModel>({
    key: 'items.modifiers',
    noun: 'modifier',
    fileNames: modifiersFileNames(family),
    parse: (files) => ({
      family,
      catalogue: parseModifiersCsv(files[MODIFIER_RESOURCES[family]].content),
      items: parseFamilyDefinitions(family, Object.fromEntries(DEFINITION_RESOURCES[family].map((name) => [name, files[name].content]))),
      implicits: parseFamilyImplicits(family, files[IMPLICIT_RESOURCES[family]].content, vocabulary),
      doctrines: family === 'ship' ? parseDoctrineArchivesCsv(files[DOCTRINE_ARCHIVE_RESOURCE].content) : null,
      corruptionPool: family === 'ship' ? parseCorruptionPoolCsv(files[CORRUPTION_POOL_RESOURCE].content) : null,
      otherFileIds: parseModifiersCsv(files[MODIFIER_RESOURCES[otherFamily(family)]].content).modifiers.map((modifier) => modifier.id),
      referencesChanged: false,
    }),
    serialize: (model) => ({
      [MODIFIER_RESOURCES[model.family]]: serializeModifiersCsv(model.catalogue),
      ...(model.referencesChanged
        ? {
            ...serializeFamilyDefinitions(model.items),
            [IMPLICIT_RESOURCES[model.family]]: serializeFamilyImplicits(model.family, model.implicits),
            ...(model.doctrines ? { [DOCTRINE_ARCHIVE_RESOURCE]: serializeDoctrineArchivesCsv(model.doctrines) } : {}),
            ...(model.corruptionPool ? { [CORRUPTION_POOL_RESOURCE]: serializeCorruptionPoolCsv(model.corruptionPool) } : {}),
          }
        : {}),
    }),
    validate: (model) => [
      ...validateModifiers(model.catalogue.modifiers, vocabulary, usageOf(model), model.family, new Set(model.otherFileIds)),
      ...referenceIssues(model.items.definitions, model.implicits.rows, modifierById(model.catalogue.modifiers), model.doctrines?.archives ?? [], poolReferences(model)),
    ],
    savedMessage: (model) =>
      model.referencesChanged
        ? `Saved ${MODIFIER_FILE_NAMES[model.family]} and the renamed references in ${DEFINITION_FILE_NAMES[model.family]}, ${IMPLICIT_FILE_NAMES[model.family]}${model.doctrines ? `, ${DOCTRINE_ARCHIVE_FILE_NAME} and ${CORRUPTION_POOL_FILE_NAME}` : ''}`
        : `Saved ${MODIFIER_FILE_NAMES[model.family]}`,
    afterSave: (draft, saved) => {
      markModifiersSaved(draft.catalogue, saved.catalogue);
      if (saved.referencesChanged) {
        draft.referencesChanged = false;
      }
    },
  });
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [slotFilter, setSlotFilter] = useState('');
  const [searchText, setSearchText] = useState('');
  const modifierCount = document.state.status === 'ready' ? document.state.model.catalogue.modifiers.length : 0;

  const addModifier = () => {
    const slot = (slotFilter || 'prefix') as ModifierSlot;
    document.update((draft) => {
      draft.catalogue.modifiers.push(newModifier(draft.catalogue.modifiers, slot, vocabulary, draft.family, new Set(draft.otherFileIds)));
    });
    setSelectedIndex(modifierCount);
    setSearchText('');
  };

  const switchFamily = (nextFamily: ItemFamily) => {
    setSelectedIndex(0);
    setSearchText('');
    setFamily(nextFamily);
  };

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilySwitch document={document} family={family} onFamily={switchFamily} noun="modifier" />
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addModifier} disabled={document.state.status !== 'ready'}>
            New modifier
          </Button>
        </DataToolbar>
        <Text size="xs" c="dimmed" mt={4}>
          {family === 'chart'
            ? 'Edits data/config/exiledSector/star_chart_modifiers.csv, the modifiers that star chart pools and system-type implicits pick from. Their values scale by tier band (t1=min-max/t6=min-max), and their ids must not repeat an id from item_modifiers.csv.'
            : 'Edits data/config/exiledSector/item_modifiers.csv, the shared list of modifiers that item pools and implicits pick from. Each modifier lists the item types that can roll it.'}{' '}
          Names are English; add zh text under socketable.modifier.&lt;ID&gt; in zh_CN.json.
        </Text>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => {
          const usage = usageOf(model);
          const selected = model.catalogue.modifiers[selectedIndex];
          return (
            <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
              <ModifierList
                modifiers={model.catalogue.modifiers}
                usage={usage}
                slotFilter={slotFilter}
                onSlotFilter={setSlotFilter}
                searchText={searchText}
                onSearchText={setSearchText}
                selectedIndex={selectedIndex}
                onSelect={setSelectedIndex}
              />
              <Divider orientation="vertical" />
              <ScrollArea style={{ flex: 1 }} type="auto">
                <Box p="md">
                  {selected ? (
                    <ModifierForm
                      key={`${model.family}-${selectedIndex}`}
                      document={document}
                      family={model.family}
                      otherFileIds={model.otherFileIds}
                      modifier={selected}
                      index={selectedIndex}
                      usage={usage.get(selected.id)}
                      vocabulary={vocabulary}
                      onSelect={setSelectedIndex}
                    />
                  ) : (
                    <Text c="dimmed">Select a modifier, or add a new one.</Text>
                  )}
                </Box>
              </ScrollArea>
            </Group>
          );
        }}
      </DocumentState>
    </Stack>
  );
}

interface ModifierListProps {
  modifiers: ItemModifier[];
  usage: Map<string, ModifierUsage>;
  slotFilter: string;
  onSlotFilter: (slot: string) => void;
  searchText: string;
  onSearchText: (text: string) => void;
  selectedIndex: number;
  onSelect: (index: number) => void;
}

function ModifierList({ modifiers, usage, slotFilter, onSlotFilter, searchText, onSearchText, selectedIndex, onSelect }: ModifierListProps) {
  const slotOptions = useMemo(
    () => [
      { value: '', label: `All slots (${modifiers.length})` },
      ...MODIFIER_SLOTS.map((slot) => ({ value: slot, label: `${MODIFIER_SLOT_LABELS[slot]} (${modifiers.filter((modifier) => modifier.slot === slot).length})` })),
    ],
    [modifiers],
  );
  const needle = searchText.trim().toLowerCase();
  const shown = modifiers
    .map((modifier, index) => ({ modifier, index }))
    .filter(({ modifier }) => !slotFilter || modifier.slot === slotFilter)
    .filter(({ modifier }) => !needle || `${modifier.id} ${modifier.name} ${modifierSummary(modifier)}`.toLowerCase().includes(needle));

  return (
    <Stack w={LIST_WIDTH} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
      <Select data={slotOptions} value={slotFilter} onChange={(value) => onSlotFilter(value ?? '')} allowDeselect={false} aria-label="Slot filter" />
      <TextInput
        placeholder="id, name or effect"
        aria-label="Search modifiers"
        leftSection={<IconSearch size={14} />}
        value={searchText}
        onChange={(event) => onSearchText(event.currentTarget.value)}
      />
      <ScrollArea style={{ flex: 1 }} type="auto">
        {shown.length === 0 ? (
          <Text size="sm" c="dimmed">
            No modifiers match.
          </Text>
        ) : (
          shown.map(({ modifier, index }) => (
            <NavLink
              key={index}
              active={index === selectedIndex}
              onClick={() => onSelect(index)}
              label={
                <Group gap={6} wrap="nowrap">
                  <Text size="sm" truncate>
                    {modifier.id}
                  </Text>
                  {!isUsed(usage.get(modifier.id)) && (
                    <Badge size="xs" variant="light" color="gray">
                      unused
                    </Badge>
                  )}
                </Group>
              }
              description={`${modifier.slot} on ${modifier.itemTypes.join(', ')}${modifier.name ? ` - ${modifier.name}` : ''} - ${modifierSummary(modifier)}`}
            />
          ))
        )}
      </ScrollArea>
    </Stack>
  );
}

interface ModifierFormProps {
  document: DataDocument<ModifiersTabModel>;
  family: ItemFamily;
  otherFileIds: string[];
  modifier: ItemModifier;
  index: number;
  usage: ModifierUsage | undefined;
  vocabulary: Vocabulary;
  onSelect: (index: number) => void;
}

function ModifierForm({ document, family, otherFileIds, modifier, index, usage, vocabulary, onSelect }: ModifierFormProps) {
  const [idText, setIdText] = useState(modifier.id);
  useEffect(() => setIdText(modifier.id), [modifier.id]);

  const edit = (recipe: (draft: ItemModifier) => void) =>
    document.update((draft) => {
      recipe(draft.catalogue.modifiers[index]);
    });

  const renameTo = (newId: string) =>
    document.update((draft) => {
      const draftModifier = draft.catalogue.modifiers[index];
      const oldId = draftModifier.id;
      draftModifier.id = newId;
      if (renameModifierReferences(draft.items.definitions, draft.implicits.rows, oldId, newId, draft.doctrines?.archives ?? [], poolReferences(draft)) > 0) {
        draft.referencesChanged = true;
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
          `References in ${DEFINITION_FILE_NAMES[family]}, ${IMPLICIT_FILE_NAMES[family]}${family === 'ship' ? `, ${DOCTRINE_ARCHIVE_FILE_NAME} and ${CORRUPTION_POOL_FILE_NAME}` : ''} are renamed and saved with it. ` +
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

  const conventionIssue = idConventionIssue(modifier);
  const suggestedId = suggestedModifierId(modifier.lines);
  const isImplicit = modifier.slot === 'implicit';
  const isChart = family === 'chart';

  const duplicate = () => {
    document.update((draft) => {
      const copy = structuredClone(modifier);
      copy.id = uniqueModifierId(draft.catalogue.modifiers, `${modifier.id}_COPY`, new Set(otherFileIds));
      copy.cells = [];
      delete copy.loadedId;
      draft.catalogue.modifiers.splice(index + 1, 0, copy);
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
      document.update((draft) => {
        draft.catalogue.modifiers.splice(index, 1);
      });
      onSelect(Math.max(0, index - 1));
    }
  };

  return (
    <Stack gap="sm" maw={1100}>
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
      {isChart ? (
        <Text size="sm">
          Item type: {ITEM_TYPE_LABELS.star_chart} - every modifier in star_chart_modifiers.csv rolls on star charts only.
        </Text>
      ) : (
        <MultiSelect
          label="Item types"
          description="Which item types can roll it. Ship modifiers roll on hull items; subroutines roll none, and star charts have their own modifier file."
          data={familyItemTypes(family, vocabulary).map((itemType) => ({ value: itemType, label: ITEM_TYPE_LABELS[itemType] ?? itemType }))}
          value={modifier.itemTypes}
          onChange={(itemTypes) => edit((draft) => void (draft.itemTypes = itemTypes))}
          error={modifier.itemTypes.length === 0 ? 'Pick at least one item type' : undefined}
        />
      )}
      {conventionIssue && (
        <Button size="xs" variant="light" w="fit-content" onClick={() => void commitId(suggestedId)}>
          Use {suggestedId}
        </Button>
      )}
      <TextInput
        label="Name"
        description={
          isImplicit
            ? 'Implicits never show a name.'
            : `Common items are named from their first prefix and suffix, as "<prefix> <noun> of <suffix>". Uniques keep their own name.`
        }
        disabled={isImplicit && !modifier.name}
        value={modifier.name}
        onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.name = text)); }}
      />
      <Divider />
      <Title order={5}>{modifier.lines.length > 1 ? 'Lines - a hybrid rolls every line and counts as one modifier' : 'Line'}</Title>
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
      <Divider />
      <Title order={5}>Used by</Title>
      {isUsed(usage) ? (
        <Stack gap={2}>
          {usage && usage.items.length > 0 && <Text size="sm">Items: {usage.items.join(', ')}</Text>}
          {usage && usage.implicitSections.length > 0 && <Text size="sm">Implicit of: {usage.implicitSections.join(', ')}</Text>}
          {usage?.doctrineArchives && usage.doctrineArchives.length > 0 && <Text size="sm">Doctrine archives: {usage.doctrineArchives.join(', ')}</Text>}
          {usage?.inCorruptionPool && <Text size="sm">{CORRUPTION_POOL_LABEL}</Text>}
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
