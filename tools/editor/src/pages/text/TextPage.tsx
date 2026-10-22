import { Box, Center, Divider, Group, ScrollArea, Stack, Text } from '@mantine/core';
import { useMemo, useState } from 'react';
import { stringsSaved } from '../../api/describe.ts';
import { DataToolbar } from '../../shared/DataToolbar.tsx';
import { DocumentState } from '../../shared/DocumentState.tsx';
import { useDataDocument } from '../../shared/useDataDocument.ts';
import { STRING_CATALOGUE_RESOURCES } from '../../../server/stringCatalogues.ts';
import { currentStrings, editedEntries, markStringsSaved, parseStringFiles, serializeStringFiles, validateStrings, type StringsModel } from './model/strings.ts';
import { StringEditor } from './StringEditor.tsx';
import { StringList } from './StringList.tsx';
import { TooltipPreview } from './TooltipPreview.tsx';

function TextWorkspace({ model, onEdit }: { model: StringsModel; onEdit: (key: string, value: string) => void }) {
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const strings = useMemo(() => currentStrings(model), [model]);
  const editedKeys = useMemo(() => new Set(editedEntries(model).map((entry) => entry.key)), [model]);
  const selected = model.entries.find((entry) => entry.key === selectedKey) ?? null;

  return (
    <Group h="100%" gap={0} align="stretch" wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
      <StringList model={model} selectedKey={selectedKey} onSelect={setSelectedKey} />
      <ScrollArea style={{ flex: 1 }} type="auto">
        <Stack p="md" gap="lg" maw={900}>
          {selected ? (
            <StringEditor entry={selected} model={model} onChange={(value) => onEdit(selected.key, value)} />
          ) : (
            <Center p="lg">
              <Text size="sm" c="dimmed">
                Pick a string from the list, or preview a node below and click one of the strings its tooltip uses.
              </Text>
            </Center>
          )}
          <Divider />
          <TooltipPreview strings={strings} selectedKey={selectedKey} editedKeys={editedKeys} onSelectKey={setSelectedKey} />
        </Stack>
      </ScrollArea>
    </Group>
  );
}

export function TextPage() {
  const document = useDataDocument<StringsModel>({
    key: 'text',
    noun: 'text',
    fileNames: STRING_CATALOGUE_RESOURCES,
    parse: (files) => parseStringFiles(Object.fromEntries(STRING_CATALOGUE_RESOURCES.map((name) => [name, files[name].content]))),
    serialize: serializeStringFiles,
    validate: validateStrings,
    savedMessage: (model) => {
      stringsSaved();
      const count = editedEntries(model).length;
      return `Saved ${count} string${count === 1 ? '' : 's'} to the English catalogues`;
    },
    afterSave: markStringsSaved,
  });

  const edit = (key: string, value: string) =>
    document.update((draft) => {
      const entry = draft.entries.find((candidate) => candidate.key === key);
      if (entry) {
        entry.value = value;
      }
    });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <Text size="xs" c="dimmed">
            English wording in the en.json catalogues under data/strings
          </Text>
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => <TextWorkspace model={model} onEdit={edit} />}
      </DocumentState>
    </Stack>
  );
}
