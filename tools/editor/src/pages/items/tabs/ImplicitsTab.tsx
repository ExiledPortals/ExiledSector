import { Box, Button, Divider, Group, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core';
import { IconCopy, IconPlus } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import type { CsvTable } from '../../../shared/csv.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import { HULL_ITEM_TYPE } from '../model/definitions.ts';
import {
  newImplicit,
  parseImplicitsCsv,
  serializeImplicitsCsv,
  validateImplicits,
  type ImplicitReference,
  type ImplicitRow,
  type ImplicitsModel,
} from '../model/implicits.ts';
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
import { ModifierLineRow } from './ModifierLineRow.tsx';
import { ModifierSelect } from './ModifierSelect.tsx';

interface ImplicitsTabProps {
  vocabulary: Vocabulary;
}

interface ImplicitsTabModel extends ImplicitsModel {
  catalogue: ItemModifier[];
  catalogueTable: CsvTable;
  catalogueChanged: boolean;
}

function sectionLabel(section: string): string {
  return section
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function ImplicitsTab({ vocabulary }: ImplicitsTabProps) {
  const document = useDataDocument<ImplicitsTabModel>({
    key: 'items.implicits',
    noun: 'implicits',
    fileNames: ['hull-item-implicits', 'item-modifiers'],
    parse: (files) => {
      const modifiers = parseModifiersCsv(files['item-modifiers'].content);
      return {
        ...parseImplicitsCsv(files['hull-item-implicits'].content, vocabulary),
        catalogue: modifiers.modifiers,
        catalogueTable: modifiers.table,
        catalogueChanged: false,
      };
    },
    serialize: (model) => ({
      'hull-item-implicits': serializeImplicitsCsv(model),
      ...(model.catalogueChanged ? { 'item-modifiers': serializeModifiersCsv({ table: model.catalogueTable, modifiers: model.catalogue }) } : {}),
    }),
    validate: (model) => [
      ...validateImplicits(model, vocabulary, modifierById(model.catalogue)),
      ...(model.catalogueChanged ? validateModifiers(model.catalogue, vocabulary, modifierUsage([], model.rows)) : []),
    ],
    savedMessage: (model) => (model.catalogueChanged ? 'Saved hull_item_implicits.csv and item_modifiers.csv' : 'Saved hull_item_implicits.csv'),
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
      const copy = copiedModifier(draft.catalogue, source, section, HULL_ITEM_TYPE);
      draft.catalogue.push(copy);
      reference.modifierId = copy.id;
      draft.catalogueChanged = true;
    });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document} />
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => {
          const usage = modifierUsage([], model.rows);
          return (
            <ScrollArea style={{ flex: 1 }} type="auto">
              <Stack p="md" gap="md" maw={1000}>
                <Text size="xs" c="dimmed">
                  Edits data/config/exiledSector/hull_item_implicits.csv. Every hull item of a section rolls all of that section&apos;s implicits, uniques
                  included. Each implicit is a hull item modifier from item_modifiers.csv with the implicit slot; its values can be edited here or in the
                  Modifiers tab, and a modifier shared by several sections changes all of them unless you copy it for one section. Subroutines never get
                  implicits. Items already rolled keep their values when these change.
                </Text>
                {model.rows.map((row) => (
                  <Paper key={row.section} withBorder p="sm">
                    <Stack gap="xs">
                      <Group justify="space-between">
                        <Title order={5}>{sectionLabel(row.section)}</Title>
                        <Button
                          size="xs"
                          variant="light"
                          leftSection={<IconPlus size={14} />}
                          onClick={() => editRow(row.section, (draft) => void draft.implicits.push(newImplicit(model.catalogue)))}
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
                            implicit={implicit}
                            index={index}
                            count={row.implicits.length}
                            catalogue={model.catalogue}
                            otherSectionsUsingModifier={(usage.get(implicit.modifierId)?.implicitSections ?? []).filter((section) => section !== row.section)}
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
              </Stack>
            </ScrollArea>
          );
        }}
      </DocumentState>
    </Stack>
  );
}

interface ImplicitEntryRowProps {
  implicit: ImplicitReference;
  index: number;
  count: number;
  catalogue: ItemModifier[];
  otherSectionsUsingModifier: string[];
  vocabulary: Vocabulary;
  onSelect: (modifierId: string) => void;
  onEditModifier: (recipe: (modifier: ItemModifier) => void) => void;
  onCopyModifier: () => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

function ImplicitEntryRow({
  implicit,
  index,
  count,
  catalogue,
  otherSectionsUsingModifier,
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
            itemType={HULL_ITEM_TYPE}
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
              effectEditable={false}
              removable={false}
              vocabulary={vocabulary}
              onEdit={(recipe) => editLine(lineIndex, recipe)}
              onRemove={() => undefined}
            />
          ))}
        {modifier && otherSectionsUsingModifier.length > 0 && (
          <Group gap="xs" justify="space-between" wrap="nowrap">
            <Text size="xs" c="orange">
              {modifier.id} is shared: editing its values also changes the {otherSectionsUsingModifier.map(sectionLabel).join(', ')} implicits.
            </Text>
            <Button size="compact-xs" variant="light" leftSection={<IconCopy size={12} />} onClick={onCopyModifier}>
              Copy for this section
            </Button>
          </Group>
        )}
      </Stack>
    </Paper>
  );
}
