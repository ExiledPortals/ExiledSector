import { Box, Button, Card, Divider, Group, ScrollArea, Select, Stack, Text, Textarea, Title } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { confirmAction, notifyError, promptText } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import {
  newRareNameWords,
  RARE_NAME_WORD_LISTS,
  RARE_NAME_STYLES,
  type RareNameWordList,
  sampleNames,
  serializeRareNames,
  validateNames,
  wordList,
  type NamesModel,
  type RareNameWords,
} from '../model/names.ts';

export function NamesTab() {
  const document = useDataDocument<NamesModel>({
    key: 'items.names',
    noun: 'name',
    fileNames: ['item-names'],
    parse: (files) => {
      const namesText = files['item-names'].content;
      return {
        rareNames: JSON.parse(namesText) as Record<string, RareNameWords>,
        rareNamesLineEnding: lineEndingOf(namesText),
      };
    },
    serialize: (model) => ({
      'item-names': serializeRareNames(model.rareNames, model.rareNamesLineEnding),
    }),
    validate: validateNames,
    savedMessage: () => 'Saved item_names.json',
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
            <Box p="md" maw={1200}>
              <RareNames document={document} model={model} />
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

function RareNameCard({ hullItemId, words, document }: { hullItemId: string; words: RareNameWords; document: DataDocument<NamesModel> }) {
  const samples = useMemo(() => sampleNames(words), [words]);
  const setWords = (key: RareNameWordList, list: string[]) =>
    document.update((draft) => {
      draft.rareNames[hullItemId][key] = list;
    });

  const remove = async () => {
    const confirmed = await confirmAction({
      title: `Remove the rare name words for "${hullItemId}"?`,
      message: 'Its rare items would fall back to their base name.',
      confirmLabel: 'Remove',
      danger: true,
    });
    if (confirmed) {
      document.update((draft) => {
        delete draft.rareNames[hullItemId];
      });
    }
  };

  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Title order={5}>{hullItemId}</Title>
          <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={() => void remove()}>
            Remove
          </Button>
        </Group>
        <Select
          label="Style"
          data={RARE_NAME_STYLES}
          value={words.style ?? 'codename'}
          onChange={(value) => value && document.update((draft) => void (draft.rareNames[hullItemId].style = value))}
          allowDeselect={false}
        />
        <Group align="flex-start" grow>
          {RARE_NAME_WORD_LISTS.filter((list) => !list.productOnly || words.style === 'product').map((list) => (
            <WordListInput key={list.key} label={list.label} words={words[list.key] ?? []} onWords={(chosen) => setWords(list.key, chosen)} />
          ))}
        </Group>
        <Text size="xs" c="dimmed">
          e.g. {samples.join(', ')}
        </Text>
      </Stack>
    </Card>
  );
}

function RareNames({ document, model }: SectionProps) {
  const addHullItem = async () => {
    const hullItemId = await promptText('Add hull item', 'Item id (as in the id column of subroutines.csv or hull_items.csv)');
    if (!hullItemId) {
      return;
    }
    if (model.rareNames[hullItemId]) {
      notifyError('That hull item already has words.');
      return;
    }
    document.update((draft) => {
      draft.rareNames[hullItemId] = newRareNameWords();
    });
  };

  const hullItemIds = Object.keys(model.rareNames);
  return (
    <Stack gap="sm">
      <Text size="xs" c="dimmed">
        Edits data/config/exiledSector/item_names.json. Rare items (3-4 modifiers) take a name built from their hull item's words, shown above the item's base
        name. One word per line. Each entry must match a definition id. Common items are named after their first prefix and suffix; those names
        live on each modifier in the Modifiers tab.
      </Text>
      {hullItemIds.length === 0 && <Text c="dimmed">No hull items have words yet.</Text>}
      {hullItemIds.map((hullItemId) => (
        <RareNameCard key={hullItemId} hullItemId={hullItemId} words={model.rareNames[hullItemId]} document={document} />
      ))}
      <Button variant="default" w="fit-content" leftSection={<IconPlus size={14} />} onClick={() => void addHullItem()}>
        Add hull item
      </Button>
    </Stack>
  );
}
