import { Avatar, Box, Button, Checkbox, Divider, Group, NavLink, ScrollArea, SegmentedControl, Select, Stack, Text, Textarea, TextInput, Title } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
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
  ANY_SYSTEM_TYPE,
  definitionWarnings,
  editablePool,
  hasModspecCacheFields,
  isModspecCacheSubroutine,
  isTrueText,
  isUniqueSubroutine,
  markDefinitionsSaved,
  itemTypeOfKind,
  ITEM_TYPE_LABELS,
  modifierQualifierFor,
  type PoolGroup,
  newItemDefinition,
  normalisedSystemType,
  parseFamilyDefinitions,
  poolShares,
  serializeFamilyDefinitions,
  setDefinitionKind,
  STAR_CHART_KIND,
  SUBROUTINE_TYPE,
  UNIQUE_LINES_GROUP,
  UNIQUE_SUBROUTINE_TYPE,
  UNLOCK_LABELS,
  uniqueItemId,
  validateItemDefinitions,
  type FamilyDefinitions,
  type ItemDefinition,
  type PoolEntry,
} from '../model/definitions.ts';
import type { CsvTable } from '../../../shared/csv.ts';
import {
  DEFINITION_FAMILY_OPTIONS,
  DEFINITION_FILE_NAMES,
  DEFINITION_RESOURCES,
  IMPLICIT_RESOURCES,
  MODIFIER_FILE_NAMES,
  MODIFIER_RESOURCES,
  type DefinitionFamily,
  type ItemFamily,
} from '../model/itemFamily.ts';
import { modifierUsage } from '../model/modifierUsage.ts';
import {
  copiedModifier,
  markModifiersSaved,
  modifierById,
  parseModifiersCsv,
  serializeModifiersCsv,
  validateModifiers,
  type ItemModifier,
  type ModifierUsage,
} from '../model/modifiers.ts';
import { implicitItemType, parseFamilyImplicits, type ImplicitsModel } from '../model/implicits.ts';
import { AnchorDefinitionsPanel } from './AnchorDefinitionsPanel.tsx';
import { DefinitionView } from './DefinitionView.tsx';
import { DoctrineArchivesPanel } from './DoctrineArchivesPanel.tsx';
import {
  DOCTRINE_ARCHIVE_FILE_NAME,
  DOCTRINE_ARCHIVE_RESOURCE,
  DOCTRINE_SUBROUTINE_KIND,
  parseDoctrineArchivesCsv,
  renameDoctrineArchive,
  serializeDoctrineArchivesCsv,
  type DoctrineArchivesModel,
} from '../model/doctrines.ts';
import { FamilyOptionSwitch } from './FamilySwitch.tsx';
import { PoolEntryRow } from './PoolEntryRow.tsx';

type DefinitionsViewMode = 'edit' | 'view';

function implicitIdsBySection(implicits: ImplicitsModel): Record<string, string[]> {
  return Object.fromEntries(
    implicits.rows.map((row) => [row.section, row.implicits.filter((reference) => reference.inlineText === undefined).map((reference) => reference.modifierId)]),
  );
}

const LIST_WIDTH = 270;

interface DefinitionsModel extends FamilyDefinitions {
  catalogue: ItemModifier[];
  catalogueTable: CsvTable;
  catalogueChanged: boolean;
  otherCatalogueIds: string[];
  implicitIdsBySection: Record<string, string[]>;
  doctrines: DoctrineArchivesModel | null;
  doctrinesChanged: boolean;
}

function otherFamily(family: ItemFamily): ItemFamily {
  return family === 'chart' ? 'ship' : 'chart';
}

function definitionFileNames(family: ItemFamily): string[] {
  const doctrineFiles = family === 'ship' ? [DOCTRINE_ARCHIVE_RESOURCE] : [];
  return [...doctrineFiles, ...DEFINITION_RESOURCES[family], ...DEFINITION_RESOURCES[otherFamily(family)], MODIFIER_RESOURCES[family], IMPLICIT_RESOURCES[family], MODIFIER_RESOURCES[otherFamily(family)]];
}

function implicitGroupOf(definition: ItemDefinition, family: ItemFamily, vocabulary: Vocabulary): string {
  return family === 'chart' ? (normalisedSystemType(definition.systemType, vocabulary) ?? '') : definition.kind;
}

export function DefinitionsTab({ vocabulary }: { vocabulary: Vocabulary }) {
  const [family, setFamily] = useState<DefinitionFamily>('ship');
  if (family === 'anchor') {
    return <AnchorDefinitionsPanel vocabulary={vocabulary} onFamily={setFamily} />;
  }
  if (family === 'doctrine') {
    return <DoctrineArchivesPanel vocabulary={vocabulary} onFamily={setFamily} />;
  }
  return <ItemDefinitionsPanel vocabulary={vocabulary} family={family} onFamily={setFamily} />;
}

interface ItemDefinitionsPanelProps {
  vocabulary: Vocabulary;
  family: ItemFamily;
  onFamily: (family: DefinitionFamily) => void;
}

function ItemDefinitionsPanel({ vocabulary, family, onFamily }: ItemDefinitionsPanelProps) {
  const document = useDataDocument<DefinitionsModel>({
    key: 'items.definitions',
    noun: 'item definition',
    fileNames: definitionFileNames(family),
    parse: (files) => {
      const modifiers = parseModifiersCsv(files[MODIFIER_RESOURCES[family]].content);
      const definitionTexts = Object.fromEntries(
        [...DEFINITION_RESOURCES.ship, ...DEFINITION_RESOURCES.chart].map((name) => [name, files[name].content]),
      );
      return {
        ...parseFamilyDefinitions(family, definitionTexts),
        catalogue: modifiers.modifiers,
        catalogueTable: modifiers.table,
        catalogueChanged: false,
        otherCatalogueIds: parseModifiersCsv(files[MODIFIER_RESOURCES[otherFamily(family)]].content).modifiers.map((modifier) => modifier.id),
        implicitIdsBySection: implicitIdsBySection(parseFamilyImplicits(family, files[IMPLICIT_RESOURCES[family]].content, vocabulary)),
        doctrines: family === 'ship' ? parseDoctrineArchivesCsv(files[DOCTRINE_ARCHIVE_RESOURCE].content) : null,
        doctrinesChanged: false,
      };
    },
    serialize: (model) => ({
      ...serializeFamilyDefinitions(model),
      ...(model.catalogueChanged
        ? { [MODIFIER_RESOURCES[model.family]]: serializeModifiersCsv({ table: model.catalogueTable, modifiers: model.catalogue }) }
        : {}),
      ...(model.doctrinesChanged && model.doctrines ? { [DOCTRINE_ARCHIVE_RESOURCE]: serializeDoctrineArchivesCsv(model.doctrines) } : {}),
    }),
    validate: (model) => [
      ...validateItemDefinitions(model.definitions, vocabulary, modifierById(model.catalogue), model.family, model.otherFamilyIds),
      ...(model.catalogueChanged
        ? validateModifiers(model.catalogue, vocabulary, modifierUsage(model.definitions, []), model.family, new Set(model.otherCatalogueIds))
        : []),
    ],
    warnings: (model) => definitionWarnings(model.definitions, model.family),
    savedMessage: (model) => {
      const savedFiles = DEFINITION_FILE_NAMES[model.family].split(' and ');
      if (model.catalogueChanged) {
        savedFiles.push(MODIFIER_FILE_NAMES[model.family]);
      }
      if (model.doctrinesChanged) {
        savedFiles.push(DOCTRINE_ARCHIVE_FILE_NAME);
      }
      return `Saved ${savedFiles.length === 1 ? savedFiles[0] : `${savedFiles.slice(0, -1).join(', ')} and ${savedFiles[savedFiles.length - 1]}`}`;
    },
    afterSave: (draft, saved) => {
      markDefinitionsSaved(draft, saved);
      markModifiersSaved({ table: draft.catalogueTable, modifiers: draft.catalogue }, { table: saved.catalogueTable, modifiers: saved.catalogue });
      if (saved.catalogueChanged) {
        draft.catalogueChanged = false;
      }
      if (saved.doctrinesChanged) {
        draft.doctrinesChanged = false;
      }
    },
  });
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [kindFilter, setKindFilter] = useState('');
  const [viewMode, setViewMode] = useState<DefinitionsViewMode>('edit');
  const definitions = document.state.status === 'ready' ? document.state.model.definitions : [];

  const addItem = () => {
    document.update((draft) => {
      const kind = draft.family === 'chart' ? STAR_CHART_KIND : kindFilter || vocabulary.itemKinds[0] || 'subroutine';
      draft.definitions.push(newItemDefinition(draft.definitions, kind, draft.catalogue, draft.otherFamilyIds, vocabulary.subroutineKinds[0] ?? ''));
    });
    setSelectedIndex(definitions.length);
  };

  const switchFamily = (nextFamily: DefinitionFamily) => {
    setSelectedIndex(0);
    setKindFilter('');
    onFamily(nextFamily);
  };

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilyOptionSwitch<DefinitionsModel, DefinitionFamily>
            document={document}
            family={family}
            onFamily={switchFamily}
            noun="item definition"
            options={DEFINITION_FAMILY_OPTIONS}
          />
          <SegmentedControl
            size="xs"
            value={viewMode}
            onChange={(value) => setViewMode(value as DefinitionsViewMode)}
            data={[
              { value: 'edit', label: 'Edit' },
              { value: 'view', label: 'View' },
            ]}
            aria-label="Definitions mode"
          />
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addItem} disabled={document.state.status !== 'ready'}>
            New item
          </Button>
        </DataToolbar>
        <Text size="xs" c="dimmed" mt={4}>
          {family === 'chart'
            ? 'Edits data/config/exiledSector/star_charts.csv. Each chart has a system type, which picks its implicits, and uniques can need a minimum tier. Names and descriptions are English; add zh text under socketable.<id>.name / .description in zh_CN.json.'
            : "Edits data/config/exiledSector/subroutines.csv and hull_items.csv; a definition is saved to the file of its kind's item type. Names and descriptions are English; add zh text under socketable.<id>.name / .description in zh_CN.json."}
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
                {model.definitions[selectedIndex] && viewMode === 'view' ? (
                  <DefinitionView
                    definition={model.definitions[selectedIndex]}
                    catalogue={model.catalogue}
                    catalogueFileName={MODIFIER_FILE_NAMES[model.family]}
                    itemType={implicitItemType(model.family)}
                    implicitModifierIds={model.implicitIdsBySection[implicitGroupOf(model.definitions[selectedIndex], model.family, vocabulary)] ?? []}
                  />
                ) : model.definitions[selectedIndex] ? (
                  <DefinitionForm
                    key={`${model.family}-${selectedIndex}`}
                    document={document}
                    family={model.family}
                    definition={model.definitions[selectedIndex]}
                    index={selectedIndex}
                    vocabulary={vocabulary}
                    catalogue={model.catalogue}
                    otherCatalogueIds={model.otherCatalogueIds}
                    otherFamilyIds={model.otherFamilyIds}
                    usage={modifierUsage(model.definitions, [])}
                    onSelect={setSelectedIndex}
                  />
                ) : (
                  <Text c="dimmed">Select an item, or add a new one.</Text>
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

function groupOfListedDefinition(definition: ItemDefinition, family: ItemFamily): string {
  return family === 'chart' ? definition.systemType || ANY_SYSTEM_TYPE : definition.kind;
}

function listedDefinitionSummary(definition: ItemDefinition, family: ItemFamily): string {
  const group = groupOfListedDefinition(definition, family);
  if (itemTypeOfKind(definition.kind) === SUBROUTINE_TYPE) {
    return `${group} - ${definition.subroutineKind || 'no kind'}`;
  }
  return `${group} - ${definition.prefixes.length} prefixes, ${definition.suffixes.length} suffixes`;
}

function DefinitionList({ model, vocabulary, kindFilter, onKindFilter, selectedIndex, onSelect }: DefinitionListProps) {
  const isChart = model.family === 'chart';
  const kindOptions = useMemo(() => {
    const kinds = isChart ? [...vocabulary.chartSystemTypes, ANY_SYSTEM_TYPE] : [...vocabulary.itemKinds];
    for (const definition of model.definitions) {
      const group = groupOfListedDefinition(definition, model.family);
      if (!kinds.includes(group)) {
        kinds.push(group);
      }
    }
    const countOf = (kind: string) => model.definitions.filter((definition) => groupOfListedDefinition(definition, model.family) === kind).length;
    return [
      { value: '', label: `${isChart ? 'All system types' : 'All socket types'} (${model.definitions.length})` },
      ...kinds.map((kind) => ({ value: kind, label: `${kind} (${countOf(kind)})` })),
    ];
  }, [model.definitions, model.family, isChart, vocabulary.itemKinds, vocabulary.chartSystemTypes]);

  const shown = model.definitions
    .map((definition, index) => ({ definition, index }))
    .filter(({ definition }) => !kindFilter || groupOfListedDefinition(definition, model.family) === kindFilter);

  return (
    <Stack w={LIST_WIDTH} gap="xs" p="xs" style={{ flexShrink: 0, minHeight: 0 }}>
      <Select
        data={kindOptions}
        value={kindFilter}
        onChange={(value) => onKindFilter(value ?? '')}
        allowDeselect={false}
        aria-label={isChart ? 'System type filter' : 'Socket type filter'}
      />
      <ScrollArea style={{ flex: 1 }} type="auto">
        {shown.length === 0 ? (
          <Text size="sm" c="dimmed">
            {kindFilter && model.definitions.length ? 'No items of this type.' : 'No items yet.'}
          </Text>
        ) : (
          shown.map(({ definition, index }) => (
            <NavLink
              key={index}
              active={index === selectedIndex}
              onClick={() => onSelect(index)}
              label={definition.name || definition.id}
              description={listedDefinitionSummary(definition, model.family)}
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
  family: ItemFamily;
  definition: ItemDefinition;
  index: number;
  vocabulary: Vocabulary;
  catalogue: ItemModifier[];
  otherCatalogueIds: string[];
  otherFamilyIds: string[];
  usage: Map<string, ModifierUsage>;
  onSelect: (index: number) => void;
}

function unlockOptions(definition: ItemDefinition, family: ItemFamily, vocabulary: Vocabulary): { value: string; label: string }[] {
  const options = [{ value: '', label: 'None' }, ...vocabulary.itemUnlocks.map((unlockId) => ({ value: unlockId, label: UNLOCK_LABELS[unlockId] ?? unlockId }))];
  if (family === 'chart') {
    for (let tier = vocabulary.lowestChartTier; tier <= vocabulary.highestChartTier; tier++) {
      options.push({ value: `${vocabulary.clearedChartTierPrefix}${tier}`, label: `Cleared a tier ${tier} chart objective` });
    }
  }
  if (!options.some((option) => option.value === definition.unlock)) {
    options.push({ value: definition.unlock, label: `${definition.unlock} (unknown)` });
  }
  return options;
}

function StarChartFields({ definition, vocabulary, edit }: { definition: ItemDefinition; vocabulary: Vocabulary; edit: (recipe: (draft: ItemDefinition) => void) => void }) {
  const systemTypes = [...vocabulary.chartSystemTypes, ANY_SYSTEM_TYPE];
  const current = definition.systemType ?? '';
  const options = current === '' || systemTypes.includes(current) ? systemTypes : [current, ...systemTypes];
  return (
    <Group grow align="flex-start">
      <Select
        label="System type"
        description="The system's star layout, and which implicits the chart rolls"
        data={options}
        value={current === '' ? ANY_SYSTEM_TYPE : current}
        onChange={(value) => edit((draft) => void (draft.systemType = value ?? ''))}
        allowDeselect={false}
      />
      <DecimalInput
        label="Minimum tier"
        description={`Blank for any tier; a chart never drops below it (${vocabulary.lowestChartTier} to ${vocabulary.highestChartTier})`}
        value={definition.minTier ?? ''}
        onChange={(text) => edit((draft) => void (draft.minTier = text))}
      />
    </Group>
  );
}

function ModspecCacheFields({ definition, edit }: { definition: ItemDefinition; edit: (recipe: (draft: ItemDefinition) => void) => void }) {
  const nodeCountChances = definition.nodeCountChances ?? '';
  return (
    <>
      <Divider />
      <Group grow align="flex-start">
        <TextInput
          label="Excluded nodes"
          description="Notable node ids that never roll, separated by ;"
          value={definition.excludedNodes ?? ''}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.excludedNodes = text));
          }}
        />
        <TextInput
          label="Node count chances"
          description="Relative chances of rolling 1, 2, 3... nodes, separated by ;"
          placeholder="0.9;0.09;0.01"
          value={nodeCountChances}
          onChange={(event) => {
            const text = event.currentTarget.value;
            edit((draft) => void (draft.nodeCountChances = text));
          }}
          error={nodeCountChances.trim() === '' ? 'Enter at least one chance' : undefined}
        />
      </Group>
      <Group grow align="flex-start">
        <DecimalInput
          label="Fleet drop chance"
          description="Chance per 100 deployment points destroyed, from fleets of any faction"
          placeholder="never"
          value={definition.dropChance ?? ''}
          onChange={(text) => edit((draft) => void (draft.dropChance = text))}
        />
        <DecimalInput
          label="Salvage chance"
          description="Chance per salvaged site, scaled by the site's first item chance"
          placeholder="never"
          value={definition.salvageChance ?? ''}
          onChange={(text) => edit((draft) => void (draft.salvageChance = text))}
        />
      </Group>
    </>
  );
}

function DefinitionForm({ document, family, definition, index, vocabulary, catalogue, otherCatalogueIds, otherFamilyIds, usage, onSelect }: DefinitionFormProps) {
  const [idText, setIdText] = useState(definition.id);
  useEffect(() => setIdText(definition.id), [definition.id]);
  const isChart = family === 'chart';
  const updateDocument = document.update;

  const edit = useCallback(
    (recipe: (draft: ItemDefinition) => void) =>
      updateDocument((draft) => {
        recipe(draft.definitions[index]);
      }),
    [updateDocument, index],
  );

  const editModifier = useCallback(
    (modifierId: string, recipe: (draft: ItemModifier) => void) =>
      updateDocument((draft) => {
        const modifier = draft.catalogue.find((candidate) => candidate.id === modifierId);
        if (modifier) {
          recipe(modifier);
          draft.catalogueChanged = true;
        }
      }),
    [updateDocument],
  );

  const copyModifier = useCallback(
    (groupKey: PoolGroup, entryIndex: number) =>
      updateDocument((draft) => {
        const draftDefinition = draft.definitions[index];
        const entry = editablePool(draftDefinition, groupKey)[entryIndex];
        const source = draft.catalogue.find((candidate) => candidate.id === entry.modifierId);
        if (!source) {
          return;
        }
        const copyItemType = groupKey === UNIQUE_LINES_GROUP.key ? UNIQUE_SUBROUTINE_TYPE : itemTypeOfKind(draftDefinition.kind);
        const copy = copiedModifier(draft.catalogue, source, modifierQualifierFor(draftDefinition), copyItemType, new Set(otherCatalogueIds));
        draft.catalogue.push(copy);
        entry.modifierId = copy.id;
        draft.catalogueChanged = true;
      }),
    [updateDocument, index, otherCatalogueIds],
  );

  const editEntry = useCallback(
    (groupKey: PoolGroup, entryIndex: number, recipe: (entry: PoolEntry) => void) => edit((draft) => recipe(editablePool(draft, groupKey)[entryIndex])),
    [edit],
  );
  const moveEntry = useCallback((groupKey: PoolGroup, from: number, to: number) => edit((draft) => moveItem(editablePool(draft, groupKey), from, to)), [edit]);
  const removeEntry = useCallback(
    (groupKey: PoolGroup, entryIndex: number) => edit((draft) => void editablePool(draft, groupKey).splice(entryIndex, 1)),
    [edit],
  );

  const commitId = async () => {
    const newId = idText.trim();
    if (newId === definition.id) {
      return;
    }
    const isDoctrine = definition.subroutineKind === DOCTRINE_SUBROUTINE_KIND;
    if (definition.loadedId && definition.id === definition.loadedId) {
      const rename = await confirmAction({
        title: `Rename "${definition.loadedId}" to "${newId}"?`,
        message:
          'Saved games store this id on every item players own. Once it has shipped, add a row to item_id_aliases.csv mapping the old id to the new one, or those items turn into unknown items. ' +
          (isDoctrine ? `Its row in ${DOCTRINE_ARCHIVE_FILE_NAME} is renamed and saved with it. ` : '') +
          `${isChart ? 'Its' : 'Its rare-name words in item_names.json and its'} socketable.${definition.loadedId}.* keys in zh_CN.json keep the old id and must be renamed by hand.`,
        confirmLabel: 'Rename',
        danger: true,
      });
      if (!rename) {
        setIdText(definition.id);
        return;
      }
    }
    const oldId = definition.id;
    updateDocument((draft) => {
      draft.definitions[index].id = newId;
      if (isDoctrine && draft.doctrines && renameDoctrineArchive(draft.doctrines.archives, oldId, newId) > 0) {
        draft.doctrinesChanged = true;
      }
    });
  };

  const kinds = vocabulary.itemKinds.includes(definition.kind) ? vocabulary.itemKinds : [definition.kind, ...vocabulary.itemKinds];
  const unique = isTrueText(definition.unique);
  const itemType = itemTypeOfKind(definition.kind);
  const isSubroutine = itemType === SUBROUTINE_TYPE;
  const showsAffixPools = !isSubroutine || definition.prefixes.length > 0 || definition.suffixes.length > 0;
  const uniqueLines = definition.lines ?? [];
  const showsUniqueFields = isSubroutine && (isUniqueSubroutine(definition) || uniqueLines.length > 0 || Boolean(definition.source));
  const subroutineKind = definition.subroutineKind ?? '';
  const subroutineKindOptions =
    subroutineKind === '' || vocabulary.subroutineKinds.includes(subroutineKind) ? vocabulary.subroutineKinds : [subroutineKind, ...vocabulary.subroutineKinds];

  const duplicate = () => {
    document.update((draft) => {
      const copy = structuredClone(definition);
      copy.id = uniqueItemId(draft.definitions, `${definition.id}_copy`, otherFamilyIds);
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
        <TextInput style={{ flex: 1 }} label="Icon path" value={definition.icon} onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.icon = text)); }} />
        <ImagePickButton targetFolder={isChart ? 'graphics/icons/' : 'graphics/icons/socketables/'} onPicked={(path) => edit((draft) => void (draft.icon = path))} />
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
        {isChart ? (
          <TextInput label="Item type" value={`${ITEM_TYPE_LABELS[itemType] ?? itemType} - saved to star_charts.csv`} readOnly />
        ) : (
          <Select
            label="Kind"
            description={`${ITEM_TYPE_LABELS[itemType] ?? itemType} - saved to ${itemType === SUBROUTINE_TYPE ? 'subroutines.csv' : 'hull_items.csv'}`}
            data={kinds}
            value={definition.kind}
            onChange={(value) => value && edit((draft) => setDefinitionKind(draft, value))}
            allowDeselect={false}
          />
        )}
      </Group>
      {isChart && <StarChartFields definition={definition} vocabulary={vocabulary} edit={edit} />}
      {isSubroutine && (
        <Select
          label="Subroutine kind"
          description="Sets what the subroutine does on the skill tree; subroutines roll no prefixes or suffixes"
          data={subroutineKindOptions}
          value={subroutineKind === '' ? null : subroutineKind}
          onChange={(value) => edit((draft) => void (draft.subroutineKind = value ?? ''))}
          allowDeselect={false}
          error={vocabulary.subroutineKinds.includes(subroutineKind) ? undefined : 'Pick a subroutine kind'}
        />
      )}
      <TextInput label="Name" value={definition.name} onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.name = text)); }} />
      <DecimalInput w={220} label="Rarity (loot weight)" value={definition.rarity} onChange={(text) => edit((draft) => void (draft.rarity = text))} />
      {isSubroutine && (
        <DecimalInput
          w={320}
          label="Socket limit per ship"
          description="Most copies of this subroutine one ship can socket; blank or 0 for no limit"
          placeholder="no limit"
          value={definition.socketLimit ?? ''}
          onChange={(text) => edit((draft) => void (draft.socketLimit = text))}
        />
      )}
      {isSubroutine && (isModspecCacheSubroutine(definition) || hasModspecCacheFields(definition)) && <ModspecCacheFields definition={definition} edit={edit} />}
      <Checkbox
        label={isSubroutine ? 'Unique - shown with the name above in orange' : 'Unique - shown with the name above in orange, and rolls every prefix and suffix'}
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
          label={isChart ? 'Drop condition (latched once met)' : 'Drop condition (uniques never drop before player level 15)'}
          data={unlockOptions(definition, family, vocabulary)}
          value={definition.unlock}
          onChange={(value) => edit((draft) => void (draft.unlock = value ?? ''))}
          allowDeselect={false}
          searchable
        />
      )}
      <Textarea
        label="Tooltip description (optional)"
        autosize
        minRows={3}
        value={definition.description}
        onChange={(event) => { const text = event.currentTarget.value; edit((draft) => void (draft.description = text)); }}
      />
      {showsUniqueFields && (
        <>
          <Divider />
          <TextInput
            label="Drop source"
            description="faction:FACTION_ID - flagships of that faction's fleets can carry it; site:SALVAGE_SITE_ID - that salvage site's unique roll. Separate several with ;"
            value={definition.source ?? ''}
            onChange={(event) => {
              const text = event.currentTarget.value;
              edit((draft) => void (draft.source = text));
            }}
          />
          <Text size="xs" c="dimmed">
            Every line always rolls, once. A min-max range is rolled when the subroutine drops and frozen on it. Each line picks a prefix from{' '}
            {MODIFIER_FILE_NAMES[family]} whose item types include {UNIQUE_SUBROUTINE_TYPE}.
          </Text>
          <Title order={5}>{UNIQUE_LINES_GROUP.label}</Title>
          {uniqueLines.length === 0 && (
            <Text size="sm" c="dimmed">
              None yet.
            </Text>
          )}
          {uniqueLines.map((entry, entryIndex) => (
            <PoolEntryRow
              key={entryIndex}
              groupKey={UNIQUE_LINES_GROUP.key}
              entry={entry}
              slot={UNIQUE_LINES_GROUP.singular}
              itemType={UNIQUE_SUBROUTINE_TYPE}
              family={family}
              index={entryIndex}
              count={uniqueLines.length}
              share={0}
              weighted={false}
              catalogue={catalogue}
              otherItemsUsingModifier={(usage.get(entry.modifierId)?.items ?? []).filter((itemId) => itemId !== definition.id).join(', ')}
              vocabulary={vocabulary}
              onEdit={editEntry}
              onEditModifier={editModifier}
              onCopyModifier={copyModifier}
              onMove={moveEntry}
              onRemove={removeEntry}
            />
          ))}
          <Button
            size="xs"
            variant="default"
            w="fit-content"
            leftSection={<IconPlus size={14} />}
            onClick={() => edit((draft) => void editablePool(draft, UNIQUE_LINES_GROUP.key).push({ modifierId: '', weight: '' }))}
          >
            Add line
          </Button>
        </>
      )}
      {showsAffixPools && (
        <>
          <Divider />
          <Text size="xs" c="dimmed">
            {isChart
              ? 'Charts roll 2 to 6 modifiers, at most 3 a side; 3 or more makes a rare chart. A tier range (t3+, t2-6) keeps an entry out of charts below or above those tiers.'
              : 'Items roll 2 effects (common: 1 prefix + 1 suffix) 55% of the time, 3 (rare: 2+1 or 1+2) 35% and 4 (rare: 2+2) 10%, never repeating a modifier.'}{' '}
            A higher rarity weight makes a modifier come up more often within its group; a blank weight counts as 1. Each entry picks a modifier from {MODIFIER_FILE_NAMES[family]}. Its
            values can be edited here or in the Modifiers tab; a shared modifier changes every item that uses it, so use &quot;Copy for this item&quot; to tune one item alone.
          </Text>
          {AFFIX_GROUPS.map((group) => {
            const pool = definition[group.key];
            const shares = poolShares(pool);
            return (
              <Stack key={group.key} gap="xs">
                <Title order={5}>
                  {group.label} - {isChart ? 'a common chart rolls 1, a rare 1-3' : 'a common item rolls 1, a rare 1-2'}
                </Title>
                {pool.length === 0 && (
                  <Text size="sm" c="dimmed">
                    None yet.
                  </Text>
                )}
                {pool.map((entry, entryIndex) => (
                  <PoolEntryRow
                    key={entryIndex}
                    groupKey={group.key}
                    entry={entry}
                    slot={group.singular}
                    itemType={itemType}
                    family={family}
                    index={entryIndex}
                    count={pool.length}
                    share={shares[entryIndex]}
                    catalogue={catalogue}
                    otherItemsUsingModifier={(usage.get(entry.modifierId)?.items ?? []).filter((itemId) => itemId !== definition.id).join(', ')}
                    vocabulary={vocabulary}
                    onEdit={editEntry}
                    onEditModifier={editModifier}
                    onCopyModifier={copyModifier}
                    onMove={moveEntry}
                    onRemove={removeEntry}
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
        </>
      )}
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
