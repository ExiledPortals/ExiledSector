import { Box, Button, Card, Divider, Group, ScrollArea, Select, Stack, Text, Textarea, Title } from '@mantine/core';
import { IconPlus, IconRefresh, IconTrash } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { confirmAction, notifyError, promptText } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { lineEndingOf, type LineEnding } from '../../../shared/lineEndings.ts';
import type { ItemFamily } from '../model/itemFamily.ts';
import {
  newRareNameWords,
  RARE_NAME_WORD_LISTS,
  RARE_NAME_STYLES,
  type RareNameWordList,
  sampleNames,
  serializeRareNames,
  serializeStarChartNames,
  validateNames,
  validateStarChartNames,
  wordList,
  type RareNameWords,
} from '../model/names.ts';
import { FamilySwitch } from './FamilySwitch.tsx';

type NamesTabModel =
  | { family: 'ship'; rareNames: Record<string, RareNameWords>; rareNamesLineEnding: LineEnding }
  | { family: 'chart'; words: RareNameWords; lineEnding: LineEnding };

const NAME_RESOURCES: Record<ItemFamily, string> = { ship: 'item-names', chart: 'star-chart-names' };
const NAME_FILE_NAMES: Record<ItemFamily, string> = { ship: 'item_names.json', chart: 'star_chart_names.json' };

export function NamesTab() {
  const [family, setFamily] = useState<ItemFamily>('ship');
  const document = useDataDocument<NamesTabModel>({
    key: 'items.names',
    noun: 'name',
    fileNames: [NAME_RESOURCES[family]],
    parse: (files): NamesTabModel => {
      const namesText = files[NAME_RESOURCES[family]].content;
      return family === 'chart'
        ? { family, words: JSON.parse(namesText) as RareNameWords, lineEnding: lineEndingOf(namesText) }
        : { family, rareNames: JSON.parse(namesText) as Record<string, RareNameWords>, rareNamesLineEnding: lineEndingOf(namesText) };
    },
    serialize: (model): Record<string, string> =>
      model.family === 'chart'
        ? { 'star-chart-names': serializeStarChartNames(model.words, model.lineEnding) }
        : { 'item-names': serializeRareNames(model.rareNames, model.rareNamesLineEnding) },
    validate: (model) => (model.family === 'chart' ? validateStarChartNames(model) : validateNames(model)),
    savedMessage: (model) => `Saved ${NAME_FILE_NAMES[model.family]}`,
  });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <FamilySwitch document={document} family={family} onFamily={setFamily} noun="name" />
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Box p="md" maw={1200}>
              {model.family === 'chart' ? <StarChartNames document={document} words={model.words} /> : <RareNames document={document} rareNames={model.rareNames} />}
            </Box>
          </ScrollArea>
        )}
      </DocumentState>
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

interface NameWordsCardProps {
  title: string;
  words: RareNameWords;
  onEdit: (recipe: (words: RareNameWords) => void) => void;
  onRemove?: () => void;
}

function NameWordsCard({ title, words, onEdit, onRemove }: NameWordsCardProps) {
  const [sampleSeed, setSampleSeed] = useState(0);
  const samples = useMemo(() => (sampleSeed >= 0 ? sampleNames(words) : []), [words, sampleSeed]);
  return (
    <Card withBorder padding="sm">
      <Stack gap="xs">
        <Group justify="space-between">
          <Title order={5}>{title}</Title>
          {onRemove && (
            <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={onRemove}>
              Remove
            </Button>
          )}
        </Group>
        <Select
          label="Style"
          data={RARE_NAME_STYLES}
          value={words.style ?? 'codename'}
          onChange={(value) => value && onEdit((draft) => void (draft.style = value))}
          allowDeselect={false}
        />
        <Group align="flex-start" grow>
          {RARE_NAME_WORD_LISTS.filter((list) => !list.productOnly || words.style === 'product').map((list) => (
            <WordListInput
              key={list.key}
              label={list.label}
              words={words[list.key] ?? []}
              onWords={(chosen) => onEdit((draft) => void (draft[list.key as RareNameWordList] = chosen))}
            />
          ))}
        </Group>
        <Group gap="xs" wrap="nowrap">
          <Text size="xs" c="dimmed">
            e.g. {samples.join(', ')}
          </Text>
          <Button size="compact-xs" variant="subtle" leftSection={<IconRefresh size={12} />} onClick={() => setSampleSeed((seed) => seed + 1)}>
            More samples
          </Button>
        </Group>
      </Stack>
    </Card>
  );
}

function StarChartNames({ document, words }: { document: DataDocument<NamesTabModel>; words: RareNameWords }) {
  return (
    <Stack gap="sm">
      <Text size="xs" c="dimmed">
        Edits data/config/exiledSector/star_chart_names.json. Rare star charts (3 or more modifiers) take a codename built from these words, shown above the
        chart&apos;s base name; every chart shares the one word list. One word per line. Add zh text for each word under socketable.nameWord.&lt;word&gt; in
        zh_CN.json.
      </Text>
      <NameWordsCard
        title="Star charts"
        words={words}
        onEdit={(recipe) =>
          document.update((draft) => {
            if (draft.family === 'chart') {
              recipe(draft.words);
            }
          })
        }
      />
    </Stack>
  );
}

function RareNames({ document, rareNames }: { document: DataDocument<NamesTabModel>; rareNames: Record<string, RareNameWords> }) {
  const editShipNames = (recipe: (names: Record<string, RareNameWords>) => void) =>
    document.update((draft) => {
      if (draft.family === 'ship') {
        recipe(draft.rareNames);
      }
    });

  const addHullItem = async () => {
    const hullItemId = await promptText('Add hull item', 'Item id (as in the id column of subroutines.csv or hull_items.csv)');
    if (!hullItemId) {
      return;
    }
    if (rareNames[hullItemId]) {
      notifyError('That hull item already has words.');
      return;
    }
    editShipNames((names) => {
      names[hullItemId] = newRareNameWords();
    });
  };

  const removeHullItem = async (hullItemId: string) => {
    const confirmed = await confirmAction({
      title: `Remove the rare name words for "${hullItemId}"?`,
      message: 'Its rare items would fall back to their base name.',
      confirmLabel: 'Remove',
      danger: true,
    });
    if (confirmed) {
      editShipNames((names) => {
        delete names[hullItemId];
      });
    }
  };

  const hullItemIds = Object.keys(rareNames);
  return (
    <Stack gap="sm">
      <Text size="xs" c="dimmed">
        Edits data/config/exiledSector/item_names.json. Rare items (3-4 modifiers) take a name built from their hull item&apos;s words, shown above the item&apos;s base
        name. One word per line. Each entry must match a definition id. Common items are named after their first prefix and suffix; those names live on each
        modifier in the Modifiers tab.
      </Text>
      {hullItemIds.length === 0 && <Text c="dimmed">No hull items have words yet.</Text>}
      {hullItemIds.map((hullItemId) => (
        <NameWordsCard
          key={hullItemId}
          title={hullItemId}
          words={rareNames[hullItemId]}
          onEdit={(recipe) => editShipNames((names) => recipe(names[hullItemId]))}
          onRemove={() => void removeHullItem(hullItemId)}
        />
      ))}
      <Button variant="default" w="fit-content" leftSection={<IconPlus size={14} />} onClick={() => void addHullItem()}>
        Add hull item
      </Button>
    </Stack>
  );
}
