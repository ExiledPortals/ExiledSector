import { ActionIcon, Alert, Box, Button, Card, Divider, Group, ScrollArea, SegmentedControl, Select, Stack, Text, Textarea, TextInput, Title, Tooltip } from '@mantine/core';
import { IconPlus, IconSearch, IconTrash, IconX } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { confirmAction, notifyError, promptText } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { parseSocketablesCsv } from '../model/definitions.ts';
import {
  missingEffects,
  parseAffixesCsv,
  RARE_NAME_STYLES,
  sampleNames,
  serializeAffixesCsv,
  serializeRareNames,
  usedEffectsOf,
  validateNames,
  wordList,
  type NamesModel,
  type RareNameWords,
} from '../model/names.ts';

type NamesSection = 'affixes' | 'rare';

export function NamesTab({ vocabulary }: { vocabulary: Vocabulary }) {
  const document = useDataDocument<NamesModel>({
    key: 'socketables.names',
    noun: 'name',
    fileNames: ['socketable-affixes', 'socketable-names', 'socketables'],
    parse: (files) => {
      const affixes = parseAffixesCsv(files['socketable-affixes'].content);
      const namesText = files['socketable-names'].content;
      return {
        affixTable: affixes.table,
        affixes: affixes.rows,
        rareNames: JSON.parse(namesText) as Record<string, RareNameWords>,
        rareNamesLineEnding: lineEndingOf(namesText),
        usedEffects: usedEffectsOf(parseSocketablesCsv(files.socketables.content).definitions),
      };
    },
    serialize: (model) => ({
      'socketable-affixes': serializeAffixesCsv(model.affixTable, model.affixes),
      'socketable-names': serializeRareNames(model.rareNames, model.rareNamesLineEnding),
    }),
    validate: validateNames,
    savedMessage: () => 'Saved socketable_affixes.csv and socketable_names.json',
  });
  const [section, setSection] = useState<NamesSection>('affixes');

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <SegmentedControl
            size="xs"
            value={section}
            onChange={(value) => setSection(value as NamesSection)}
            data={[
              { value: 'affixes', label: 'Prefix and suffix names' },
              { value: 'rare', label: 'Rare names' },
            ]}
          />
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Box p="md" maw={1200}>
              {section === 'affixes' ? <AffixNames document={document} model={model} vocabulary={vocabulary} /> : <RareNames document={document} model={model} />}
            </Box>
          </ScrollArea>
        )}
      </DocumentState>
    </Stack>
  );
}

interface SectionProps {
  document: DataDocument<NamesModel>;
  model: NamesModel;
}

function AffixNames({ document, model, vocabulary }: SectionProps & { vocabulary: Vocabulary }) {
  const [filterText, setFilterText] = useState('');
  const [effectToAdd, setEffectToAdd] = useState<string | null>(null);
  const missing = missingEffects(model);
  const needle = filterText.trim().toLowerCase();

  const addOptions = useMemo(
    () =>
      vocabulary.effectNames
        .filter((name) => !model.affixes.some((row) => row.effect === name))
        .map((name) => ({ value: name, label: name + (model.usedEffects[name] ? ' (used, needs names)' : '') })),
    [vocabulary.effectNames, model.affixes, model.usedEffects],
  );

  const removeRow = async (index: number) => {
    const row = model.affixes[index];
    if (!model.usedEffects[row.effect]) {
      const confirmed = await confirmAction({
        title: `Remove the names for ${row.effect}?`,
        message: 'No current pool uses it, but items players already own may still carry it, and their frozen names would lose these words.',
        confirmLabel: 'Remove',
        danger: true,
      });
      if (!confirmed) {
        return;
      }
    }
    document.update((draft) => void draft.affixes.splice(index, 1));
  };

  return (
    <Stack gap="sm">
      <Text size="xs" c="dimmed">
        Edits data/config/exiledSector/socketable_affixes.csv. A common item is named "&lt;prefix&gt; &lt;noun&gt; of &lt;suffix&gt;" from its first prefix and
        first suffix effect. Every effect a non-unique socketable can roll needs a row; uniques keep their own names. Chinese words go under
        socketable.affix.&lt;EFFECT&gt;.prefix / .suffix in zh_CN.json.
      </Text>
      {missing.length > 0 && (
        <Alert color="red" title="Used by socketables but missing names">
          {missing.join(', ')}
        </Alert>
      )}
      <Group align="flex-end">
        <TextInput
          style={{ flex: 1 }}
          label="Filter"
          placeholder="effect or word"
          leftSection={<IconSearch size={14} />}
          value={filterText}
          onChange={(event) => setFilterText(event.currentTarget.value)}
        />
        <Select style={{ flex: 1 }} label="Add an effect" searchable data={addOptions} value={effectToAdd} onChange={setEffectToAdd} />
        <Button
          leftSection={<IconPlus size={14} />}
          disabled={!effectToAdd}
          onClick={() => {
            if (!effectToAdd) {
              return;
            }
            const effect = effectToAdd;
            document.update((draft) => void draft.affixes.push({ cells: [], effect, prefix: '', suffix: '' }));
            setFilterText(effect);
            setEffectToAdd(null);
          }}
        >
          Add
        </Button>
      </Group>
      <Divider />
      {model.affixes.map((row, index) => {
        const haystack = `${row.effect} ${row.prefix} ${row.suffix}`.toLowerCase();
        if (needle && !haystack.includes(needle)) {
          return null;
        }
        const users = model.usedEffects[row.effect];
        return (
          <Group key={index} align="flex-end" wrap="nowrap">
            <Stack gap={2} style={{ flex: 3, minWidth: 0 }}>
              <Tooltip label={users ? `Used by ${users.join(', ')}` : 'Not in any non-unique pool - items already released may still carry it'}>
                <Text size="sm" fw={500} truncate>
                  {row.effect}{' '}
                  {!users && (
                    <Text span size="xs" c="dimmed">
                      (not in any non-unique pool)
                    </Text>
                  )}
                </Text>
              </Tooltip>
              <Text size="xs" c="dimmed">
                {row.prefix || '...'} subroutine of {row.suffix || '...'}
              </Text>
            </Stack>
            <TextInput
              style={{ flex: 2 }}
              label="Prefix"
              value={row.prefix}
              onChange={(event) => {
                const prefix = event.currentTarget.value;
                document.update((draft) => void (draft.affixes[index].prefix = prefix));
              }}
            />
            <TextInput
              style={{ flex: 2 }}
              label='Suffix (after "of")'
              value={row.suffix}
              onChange={(event) => {
                const suffix = event.currentTarget.value;
                document.update((draft) => void (draft.affixes[index].suffix = suffix));
              }}
            />
            <ActionIcon variant="subtle" color="red" mb={6} onClick={() => void removeRow(index)} aria-label={`Remove ${row.effect}`}>
              <IconX size={16} />
            </ActionIcon>
          </Group>
        );
      })}
    </Stack>
  );
}

interface WordListInputProps {
  label: string;
  words: string[];
  onWords: (words: string[]) => void;
}

function WordListInput({ label, words, onWords }: WordListInputProps) {
  const [text, setText] = useState(() => words.join('\n'));
  return (
    <Textarea
      style={{ flex: 1 }}
      label={
        <Group gap={4}>
          {label}
          <Text span size="xs" c="dimmed">
            ({words.length})
          </Text>
        </Group>
      }
      autosize
      minRows={6}
      maxRows={14}
      value={text}
      onChange={(event) => {
        const nextText = event.currentTarget.value;
        setText(nextText);
        onWords(wordList(nextText));
      }}
    />
  );
}

function RareNameCard({ socketableId, words, document }: { socketableId: string; words: RareNameWords; document: DataDocument<NamesModel> }) {
  const samples = useMemo(() => sampleNames(words), [words]);
  const setWords = (key: 'first' | 'second' | 'models', list: string[]) =>
    document.update((draft) => {
      draft.rareNames[socketableId][key] = list;
    });

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Remove the rare name words for "${socketableId}"?`,
      message: 'Its rare items would fall back to their base name.',
      confirmLabel: 'Remove',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => {
        delete draft.rareNames[socketableId];
      });
    }
  };

  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Title order={5}>{socketableId}</Title>
          <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
            Remove
          </Button>
        </Group>
        <Select
          label="Style"
          data={RARE_NAME_STYLES}
          value={words.style ?? 'codename'}
          onChange={(value) => value && document.update((draft) => void (draft.rareNames[socketableId].style = value))}
          allowDeselect={false}
        />
        <Group align="flex-start" grow>
          <WordListInput label="First words" words={words.first ?? []} onWords={(list) => setWords('first', list)} />
          <WordListInput label="Second words" words={words.second ?? []} onWords={(list) => setWords('second', list)} />
          {words.style === 'product' && <WordListInput label="Models" words={words.models ?? []} onWords={(list) => setWords('models', list)} />}
        </Group>
        <Text size="xs" c="dimmed">
          e.g. {samples.join(', ')}
        </Text>
      </Stack>
    </Card>
  );
}

function RareNames({ document, model }: SectionProps) {
  const addSocketable = async () => {
    const socketableId = await promptText('Add socketable', 'Socketable id (as in the id column of socketables.csv)');
    if (!socketableId) {
      return;
    }
    if (model.rareNames[socketableId]) {
      notifyError('That socketable already has words.');
      return;
    }
    document.update((draft) => {
      draft.rareNames[socketableId] = { style: 'codename', first: [], second: [] };
    });
  };

  const socketableIds = Object.keys(model.rareNames);
  return (
    <Stack gap="sm">
      <Text size="xs" c="dimmed">
        Edits data/config/exiledSector/socketable_names.json. Rare items (3-4 effects) take a name built from their socketable's words, shown above the item's base
        name. One word per line. Each entry must match an id in socketables.csv.
      </Text>
      {socketableIds.length === 0 && <Text c="dimmed">No socketables have words yet.</Text>}
      {socketableIds.map((socketableId) => (
        <RareNameCard key={socketableId} socketableId={socketableId} words={model.rareNames[socketableId]} document={document} />
      ))}
      <Button variant="default" w="fit-content" leftSection={<IconPlus size={14} />} onClick={() => void addSocketable()}>
        Add socketable
      </Button>
    </Stack>
  );
}
