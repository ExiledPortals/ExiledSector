import { Box, Button, Divider, Group, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import { newImplicit, parseImplicitsCsv, serializeImplicitsCsv, validateImplicits, type ImplicitRow, type ImplicitsModel } from '../model/implicits.ts';
import { modifierById, parseModifiersCsv, type HullItemModifier } from '../model/modifiers.ts';
import { ModifierSelect } from './ModifierSelect.tsx';

interface ImplicitsTabProps {
  vocabulary: Vocabulary;
}

interface ImplicitsTabModel extends ImplicitsModel {
  catalogue: HullItemModifier[];
}

function typeLabel(type: string): string {
  return type
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function ImplicitsTab({ vocabulary }: ImplicitsTabProps) {
  const document = useDataDocument<ImplicitsTabModel>({
    key: 'hullItems.implicits',
    noun: 'implicits',
    fileNames: ['hull-item-implicits', 'hull-item-modifiers'],
    parse: (files) => ({
      ...parseImplicitsCsv(files['hull-item-implicits'].content, vocabulary),
      catalogue: parseModifiersCsv(files['hull-item-modifiers'].content).modifiers,
    }),
    serialize: (model) => ({ 'hull-item-implicits': serializeImplicitsCsv(model) }),
    validate: (model) => validateImplicits(model, vocabulary, modifierById(model.catalogue)),
    savedMessage: () => 'Saved hull_item_implicits.csv',
  });

  const editRow = (type: string, recipe: (row: ImplicitRow) => void) =>
    document.update((draft) => {
      const row = draft.rows.find((candidate) => candidate.type === type);
      if (row) {
        recipe(row);
      }
    });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document} />
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Stack p="md" gap="md" maw={1000}>
              <Text size="xs" c="dimmed">
                Edits data/config/exiledSector/hull_item_implicits.csv. Every hull item of a type rolls all of that type&apos;s implicits, uniques included.
                Each implicit is a modifier from hull_item_modifiers.csv with the implicit slot (Modifiers tab), which holds its ranges. Subroutines never get
                implicits. Items already rolled keep their values when these change.
              </Text>
              {model.rows.map((row) => (
                <Paper key={row.type} withBorder p="sm">
                  <Stack gap="xs">
                    <Group justify="space-between">
                      <Title order={5}>{typeLabel(row.type)}</Title>
                      <Button
                        size="xs"
                        variant="light"
                        leftSection={<IconPlus size={14} />}
                        onClick={() => editRow(row.type, (draft) => void draft.implicits.push(newImplicit(model.catalogue)))}
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
                        <Group key={index} align="flex-start" gap="xs" wrap="nowrap">
                          <ModifierSelect
                            style={{ flex: 1 }}
                            label={implicit.inlineText === undefined ? `Implicit ${index + 1}` : `Implicit ${index + 1} (was written inline as ${implicit.inlineText})`}
                            catalogue={model.catalogue}
                            slot="implicit"
                            value={implicit.modifierId}
                            onChange={(modifierId) =>
                              editRow(row.type, (draft) => {
                                draft.implicits[index] = { modifierId };
                              })
                            }
                          />
                          <Box pt={24}>
                            <RowButtons
                              index={index}
                              count={row.implicits.length}
                              onMove={(from, to) =>
                                editRow(row.type, (draft) => {
                                  const [moved] = draft.implicits.splice(from, 1);
                                  draft.implicits.splice(to, 0, moved);
                                })
                              }
                              onRemove={() => editRow(row.type, (draft) => void draft.implicits.splice(index, 1))}
                            />
                          </Box>
                        </Group>
                      ))
                    )}
                  </Stack>
                </Paper>
              ))}
            </Stack>
          </ScrollArea>
        )}
      </DocumentState>
    </Stack>
  );
}
