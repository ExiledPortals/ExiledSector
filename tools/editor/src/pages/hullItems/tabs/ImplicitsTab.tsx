import { Box, Button, Divider, Group, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { EffectSelect } from '../../../shared/EffectSelect.tsx';
import { RowButtons } from '../../../shared/MoveButtons.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import type { PoolEntry } from '../model/definitions.ts';
import { newImplicit, parseImplicitsCsv, serializeImplicitsCsv, validateImplicits, type ImplicitRow, type ImplicitsModel } from '../model/implicits.ts';

interface ImplicitsTabProps {
  vocabulary: Vocabulary;
}

function typeLabel(type: string): string {
  return type
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function ImplicitsTab({ vocabulary }: ImplicitsTabProps) {
  const document = useDataDocument<ImplicitsModel>({
    key: 'hullItems.implicits',
    noun: 'implicits',
    fileNames: ['hull-item-implicits'],
    parse: (files) => parseImplicitsCsv(files['hull-item-implicits'].content, vocabulary),
    serialize: (model) => ({ 'hull-item-implicits': serializeImplicitsCsv(model) }),
    validate: (model) => validateImplicits(model, vocabulary),
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
                Edits data/config/exiledSector/hull_item_implicits.csv. Every hull item of a type rolls all of that type&apos;s implicits, uniques included,
                from a min-max range. Subroutines never get implicits. Items already rolled keep their values when these change.
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
                        onClick={() => editRow(row.type, (draft) => void draft.implicits.push(newImplicit(vocabulary)))}
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
                        <ImplicitRowEditor
                          key={index}
                          implicit={implicit}
                          index={index}
                          count={row.implicits.length}
                          vocabulary={vocabulary}
                          onEdit={(recipe) => editRow(row.type, (draft) => recipe(draft.implicits[index]))}
                          onMove={(from, to) =>
                            editRow(row.type, (draft) => {
                              const [moved] = draft.implicits.splice(from, 1);
                              draft.implicits.splice(to, 0, moved);
                            })
                          }
                          onRemove={() => editRow(row.type, (draft) => void draft.implicits.splice(index, 1))}
                        />
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

interface ImplicitRowEditorProps {
  implicit: PoolEntry;
  index: number;
  count: number;
  vocabulary: Vocabulary;
  onEdit: (recipe: (implicit: PoolEntry) => void) => void;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

function ImplicitRowEditor({ implicit, index, count, vocabulary, onEdit, onMove, onRemove }: ImplicitRowEditorProps) {
  return (
    <Group align="flex-end" gap="xs" wrap="nowrap">
      <EffectSelect
        style={{ flex: 3, minWidth: 220 }}
        label={`Implicit ${index + 1}`}
        vocabulary={vocabulary}
        value={implicit.effect}
        onChange={(effect) => onEdit((draft) => void (draft.effect = effect))}
      />
      <DecimalInput style={{ flex: 1 }} label="Min" value={implicit.min} onChange={(text) => onEdit((draft) => void (draft.min = text))} />
      <DecimalInput style={{ flex: 1 }} label="Max" value={implicit.max} onChange={(text) => onEdit((draft) => void (draft.max = text))} />
      <Box mb={4}>
        <RowButtons index={index} count={count} onMove={onMove} onRemove={onRemove} />
      </Box>
    </Group>
  );
}
