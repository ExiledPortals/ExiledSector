import { Box, Button, Divider, Group, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core';
import { IconCopy, IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import type { CsvTable } from '../../../shared/csv.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import {
  CORRUPTION_POOLS,
  HULL_ITEM_CORRUPTION_POOL,
  implicitItemType,
  newImplicit,
  newImplicitOf,
  parseCorruptionPoolCsv,
  parseFamilyImplicits,
  serializeCorruptionPoolCsv,
  serializeFamilyImplicits,
  validateCorruptionPool,
  validateImplicits,
  type CorruptionPool,
  type CorruptionPoolModel,
  type ImplicitReference,
  type ImplicitRow,
  type ImplicitsModel,
} from '../model/implicits.ts';
import { IMPLICIT_FILE_NAMES, IMPLICIT_RESOURCES, MODIFIER_FILE_NAMES, MODIFIER_RESOURCES, type ItemFamily } from '../model/itemFamily.ts';
import { modifierUsage } from '../model/modifierUsage.ts';
import {
  copiedModifier,
  markModifiersSaved,
  modifierById,
  parseModifiersCsv,
  serializeModifiersCsv,
  validateModifiers,
  type ItemModifier,
  type ModifierLine,
} from '../model/modifiers.ts';
import { FamilySwitch } from './FamilySwitch.tsx';
import { ModifierLineRow } from './ModifierLineRow.tsx';
import { ModifierSelect } from './ModifierSelect.tsx';

interface ImplicitsTabProps {
  vocabulary: Vocabulary;
}

interface ImplicitsTabModel extends ImplicitsModel {
  family: ItemFamily;
  catalogue: ItemModifier[];
  catalogueTable: CsvTable;
  catalogueChanged: boolean;
  corruptionPools: CorruptionPoolModel[];
  otherFileIds: string[];
}

function groupLabel(group: string, family: ItemFamily): string {
  if (family === 'chart') {
    return group === 'any' ? 'Any system type' : group;
  }
  return group
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function otherFamily(family: ItemFamily): ItemFamily {
  return family === 'chart' ? 'ship' : 'chart';
}

function poolDescription(pool: CorruptionPool): string {
  return pool === HULL_ITEM_CORRUPTION_POOL
    ? "A Corrupted Nanoswarm that replaces a hull item's implicit picks from these and every section's implicits, each equally likely."
    : 'A Corrupted Nanoswarm can add one of these to a subroutine, each equally likely.';
}

function implicitsFileNames(family: ItemFamily): string[] {
  const fileNames = [IMPLICIT_RESOURCES[family], MODIFIER_RESOURCES[family], MODIFIER_RESOURCES[otherFamily(family)]];
  return family === 'ship' ? [...fileNames, ...CORRUPTION_POOLS.map((pool) => pool.resource)] : fileNames;
}

function usageOf(model: ImplicitsTabModel) {
  return modifierUsage([], model.rows, implicitItemType(model.family), [], model.corruptionPools);
}

function savedFileNames(model: ImplicitsTabModel): string {
  const names = [
    IMPLICIT_FILE_NAMES[model.family],
    ...model.corruptionPools.map((pool) => pool.pool.fileName),
    ...(model.catalogueChanged ? [MODIFIER_FILE_NAMES[model.family]] : []),
  ];
  return names.length > 1 ? `${names.slice(0, -1).join(', ')} and ${names[names.length - 1]}` : names[0];
}

export function ImplicitsTab({ vocabulary }: ImplicitsTabProps) {
  const [family, setFamily] = useState<ItemFamily>('ship');
  const document = useDataDocument<ImplicitsTabModel>({
    key: 'items.implicits',
    noun: 'implicits',
    fileNames: implicitsFileNames(family),
    parse: (files) => {
      const modifiers = parseModifiersCsv(files[MODIFIER_RESOURCES[family]].content);
      return {
        ...parseFamilyImplicits(family, files[IMPLICIT_RESOURCES[family]].content, vocabulary),
        family,
        catalogue: modifiers.modifiers,
        catalogueTable: modifiers.table,
        catalogueChanged: false,
        corruptionPools: family === 'ship' ? CORRUPTION_POOLS.map((pool) => parseCorruptionPoolCsv(files[pool.resource].content, pool)) : [],
        otherFileIds: parseModifiersCsv(files[MODIFIER_RESOURCES[otherFamily(family)]].content).modifiers.map((modifier) => modifier.id),
      };
    },
    serialize: (model) => ({
      [IMPLICIT_RESOURCES[model.family]]: serializeFamilyImplicits(model.family, model),
      ...Object.fromEntries(model.corruptionPools.map((pool) => [pool.pool.resource, serializeCorruptionPoolCsv(pool)])),
      ...(model.catalogueChanged
        ? { [MODIFIER_RESOURCES[model.family]]: serializeModifiersCsv({ table: model.catalogueTable, modifiers: model.catalogue }) }
        : {}),
    }),
    validate: (model) => [
      ...validateImplicits(model, vocabulary, modifierById(model.catalogue), model.family),
      ...model.corruptionPools.flatMap((pool) => validateCorruptionPool(pool, modifierById(model.catalogue))),
      ...(model.catalogueChanged ? validateModifiers(model.catalogue, vocabulary, usageOf(model), model.family, new Set(model.otherFileIds)) : []),
    ],
    savedMessage: (model) => `Saved ${savedFileNames(model)}`,
    afterSave: (draft, saved) => {
      markModifiersSaved({ table: draft.catalogueTable, modifiers: draft.catalogue }, { table: saved.catalogueTable, modifiers: saved.catalogue });
      if (saved.catalogueChanged) {
        draft.catalogueChanged = false;
      }
    },
  });

  const editRow = (section: string, recipe: (row: ImplicitRow) => void) =>
    document.update((draft) => {
      const row = draft.rows.find((candidate) => candidate.section === section);
      if (row) {
        recipe(row);
      }
    });

  const editModifier = (modifierId: string, recipe: (modifier: ItemModifier) => void) =>
    document.update((draft) => {
      const modifier = draft.catalogue.find((candidate) => candidate.id === modifierId);
      if (modifier) {
        recipe(modifier);
        draft.catalogueChanged = true;
      }
    });

  const copyModifier = (section: string, index: number) =>
    document.update((draft) => {
      const reference = draft.rows.find((candidate) => candidate.section === section)?.implicits[index];
      const source = reference ? draft.catalogue.find((candidate) => candidate.id === reference.modifierId) : undefined;
      if (!reference || !source) {
        return;
      }
      const copy = copiedModifier(draft.catalogue, source, section, implicitItemType(draft.family), new Set(draft.otherFileIds));
      draft.catalogue.push(copy);
      reference.modifierId = copy.id;
      draft.catalogueChanged = true;
    });

  const editPool = (poolIndex: number, recipe: (pool: CorruptionPoolModel) => void) =>
    document.update((draft) => {
      const pool = draft.corruptionPools[poolIndex];
      if (pool) {
        recipe(pool);
      }
    });

  const copyPoolModifier = (poolIndex: number, index: number) =>
    document.update((draft) => {
      const pool = draft.corruptionPools[poolIndex];
      const entry = pool?.entries[index];
      const source = entry ? draft.catalogue.find((candidate) => candidate.id === entry.implicit.modifierId) : undefined;
      if (!entry || !source) {
        return;
      }
      const copy = copiedModifier(draft.catalogue, source, pool.pool.copyQualifier, pool.pool.itemType, new Set(draft.otherFileIds));
      draft.catalogue.push(copy);
      entry.implicit = { modifierId: copy.id };
      draft.catalogueChanged = true;
    });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilySwitch document={document} family={family} onFamily={setFamily} noun="implicit" />
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => {
          const usage = usageOf(model);
          return (
            <ScrollArea style={{ flex: 1 }} type="auto">
              <Stack p="md" gap="md" maw={1000}>
                <Text size="xs" c="dimmed">
                  {model.family === 'chart'
                    ? 'Edits data/config/exiledSector/star_chart_implicits.csv. Every star chart first rolls one inhabiting faction by weight from chart_factions.csv, edited in the Chart factions tab. Every star chart of a system type also rolls all of that type\'s implicits, uniques included; charts whose system type is "any" use the Any row. Each implicit is a star chart modifier from star_chart_modifiers.csv with the implicit slot; its values can be edited here or in the Modifiers tab, and a modifier shared by several system types changes all of them unless you copy it for one. Charts already rolled keep their values when these change.'
                    : "Edits data/config/exiledSector/hull_item_implicits.csv. Every hull item of a section rolls all of that section's implicits, uniques included. Each implicit is a hull item modifier from item_modifiers.csv with the implicit slot; its values can be edited here or in the Modifiers tab, and a modifier shared by several sections changes all of them unless you copy it for one section. A Corrupted Nanoswarm picks a subroutine's or a hull item's new implicit from the corruption pools below. Items already rolled keep their values when these change."}
                </Text>
                {model.rows.map((row) => (
                  <Paper key={row.section} withBorder p="sm">
                    <Stack gap="xs">
                      <Group justify="space-between">
                        <Title order={5}>{groupLabel(row.section, model.family)}</Title>
                        <Button
                          size="xs"
                          variant="light"
                          leftSection={<IconPlus size={14} />}
                          onClick={() => editRow(row.section, (draft) => void draft.implicits.push(newImplicit(model.catalogue, model.family)))}
                        >
                          Add implicit
                        </Button>
                      </Group>
                      {row.implicits.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          No implicits.
                        </Text>
                      ) : (
                        row.implicits.map((implicit, index) => (
                          <ImplicitEntryRow
                            key={index}
                            family={model.family}
                            itemType={implicitItemType(model.family)}
                            copyLabel={model.family === 'chart' ? 'Copy for this system type' : 'Copy for this section'}
                            implicit={implicit}
                            index={index}
                            count={row.implicits.length}
                            catalogue={model.catalogue}
                            otherGroupsUsingModifier={[
                              ...(usage.get(implicit.modifierId)?.implicitSections ?? [])
                                .filter((section) => section !== row.section)
                                .map((section) => groupLabel(section, model.family)),
                              ...(usage.get(implicit.modifierId)?.corruptionPools ?? []).map((label) => label.toLowerCase()),
                            ]}
                            vocabulary={vocabulary}
                            onSelect={(modifierId) =>
                              editRow(row.section, (draft) => {
                                draft.implicits[index] = { modifierId };
                              })
                            }
                            onEditModifier={(recipe) => editModifier(implicit.modifierId, recipe)}
                            onCopyModifier={() => copyModifier(row.section, index)}
                            onMove={(from, to) =>
                              editRow(row.section, (draft) => {
                                const [moved] = draft.implicits.splice(from, 1);
                                draft.implicits.splice(to, 0, moved);
                              })
                            }
                            onRemove={() => editRow(row.section, (draft) => void draft.implicits.splice(index, 1))}
                          />
                        ))
                      )}
                    </Stack>
                  </Paper>
                ))}
                {model.corruptionPools.map((corruptionPool, poolIndex) => (
                  <Paper key={corruptionPool.pool.resource} withBorder p="sm">
                    <Stack gap="xs">
                      <Group justify="space-between">
                        <Title order={5}>{corruptionPool.pool.label}</Title>
                        <Button
                          size="xs"
                          variant="light"
                          leftSection={<IconPlus size={14} />}
                          onClick={() =>
                            editPool(poolIndex, (pool) => void pool.entries.push({ cells: [], implicit: newImplicitOf(model.catalogue, pool.pool.itemType) }))
                          }
                        >
                          Add implicit
                        </Button>
                      </Group>
                      <Text size="xs" c="dimmed">
                        Edits data/config/exiledSector/{corruptionPool.pool.fileName}. {poolDescription(corruptionPool.pool)} Each is an implicit modifier from
                        item_modifiers.csv whose item types include {corruptionPool.pool.itemType}.
                      </Text>
                      {corruptionPool.entries.length === 0 ? (
                        <Text size="sm" c="dimmed">
                          No implicits.
                        </Text>
                      ) : (
                        corruptionPool.entries.map((entry, index, entries) => (
                          <ImplicitEntryRow
                            key={index}
                            family={model.family}
                            itemType={corruptionPool.pool.itemType}
                            copyLabel={`Copy for the ${corruptionPool.pool.label.toLowerCase()}`}
                            implicit={entry.implicit}
                            index={index}
                            count={entries.length}
                            catalogue={model.catalogue}
                            otherGroupsUsingModifier={[
                              ...(usage.get(entry.implicit.modifierId)?.implicitSections ?? []).map((section) => groupLabel(section, model.family)),
                              ...(usage.get(entry.implicit.modifierId)?.corruptionPools ?? [])
                                .filter((label) => label !== corruptionPool.pool.label)
                                .map((label) => label.toLowerCase()),
                            ]}
                            vocabulary={vocabulary}
                            onSelect={(modifierId) =>
                              editPool(poolIndex, (pool) => {
                                pool.entries[index].implicit = { modifierId };
                              })
                            }
                            onEditModifier={(recipe) => editModifier(entry.implicit.modifierId, recipe)}
                            onCopyModifier={() => copyPoolModifier(poolIndex, index)}
                            onMove={(from, to) =>
                              editPool(poolIndex, (pool) => {
                                const [moved] = pool.entries.splice(from, 1);
                                pool.entries.splice(to, 0, moved);
                              })
                            }
                            onRemove={() => editPool(poolIndex, (pool) => void pool.entries.splice(index, 1))}
                          />
                        ))
                      )}
                    </Stack>
                  </Paper>
                ))}
              </Stack>
            </ScrollArea>
          );
        }}
      </DocumentState>
    </Stack>
  );
}

interface ImplicitEntryRowProps {
  family: ItemFamily;
  itemType: string;
  copyLabel: string;
  implicit: ImplicitReference;
  index: number;
  count: number;
  catalogue: ItemModifier[];
  otherGroupsUsingModifier: string[];
  vocabulary: Vocabulary;
  onSelect: (modifierId: string) => void;
  onEditModifier: (recipe: (modifier: ItemModifier) => void) => void;
  onCopyModifier: () => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

function ImplicitEntryRow({
  family,
  itemType,
  copyLabel,
  implicit,
  index,
  count,
  catalogue,
  otherGroupsUsingModifier,
  vocabulary,
  onSelect,
  onEditModifier,
  onCopyModifier,
  onMove,
  onRemove,
}: ImplicitEntryRowProps) {
  const modifier = implicit.inlineText === undefined ? catalogue.find((candidate) => candidate.id === implicit.modifierId) : undefined;
  const editLine = (lineIndex: number, recipe: (line: ModifierLine) => void) => onEditModifier((draft) => recipe(draft.lines[lineIndex]));
  return (
    <Paper withBorder p="xs">
      <Stack gap="xs">
        <Group align="flex-start" gap="xs" wrap="nowrap">
          <ModifierSelect
            style={{ flex: 1 }}
            label={implicit.inlineText === undefined ? `Implicit ${index + 1}` : `Implicit ${index + 1} (was written inline as ${implicit.inlineText})`}
            catalogue={catalogue}
            slot="implicit"
            itemType={itemType}
            catalogueFileName={MODIFIER_FILE_NAMES[family]}
            value={implicit.modifierId}
            onChange={onSelect}
          />
          <Box pt={24}>
            <RowButtons index={index} count={count} onMove={onMove} onRemove={onRemove} />
          </Box>
        </Group>
        {modifier &&
          modifier.lines.map((line, lineIndex) => (
            <ModifierLineRow
              key={lineIndex}
              line={line}
              index={lineIndex}
              perHullSizeAllowed={false}
              family={family}
              effectEditable={false}
              removable={false}
              vocabulary={vocabulary}
              onEdit={(recipe) => editLine(lineIndex, recipe)}
              onRemove={() => undefined}
            />
          ))}
        {modifier && otherGroupsUsingModifier.length > 0 && (
          <Group gap="xs" justify="space-between" wrap="nowrap">
            <Text size="xs" c="orange">
              {modifier.id} is shared: editing its values also changes the {otherGroupsUsingModifier.join(', ')} implicits.
            </Text>
            <Button size="compact-xs" variant="light" leftSection={<IconCopy size={12} />} onClick={onCopyModifier}>
              {copyLabel}
            </Button>
          </Group>
        )}
      </Stack>
    </Paper>
  );
}
