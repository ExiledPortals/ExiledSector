import { ActionIcon, Box, Button, Divider, Group, ScrollArea, Select, Stack, Table, Text, TextInput, Tooltip } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { IconPlus, IconSearch, IconX } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { getJson } from '../../../api/client.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { fractionToPercentText, percentToFractionText } from '../../../shared/decimalText.ts';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { notifyError, promptText } from '../../../shared/dialogs.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { useDataDocument } from '../../../shared/useDataDocument.ts';
import { DROP_SLOTS, dropSummary, parseSalvageCsv, serializeSalvageCsv, setChance, validateDrops, type DropsModel } from '../model/drops.ts';

const DEFAULT_FILE = 'socketable_salvage.csv';
const SLOT_INDEXES = Array.from({ length: DROP_SLOTS }, (_, slot) => slot);

interface PercentCellProps {
  fraction: string;
  onFraction: (fraction: string) => void;
  label: string;
}

function PercentCell({ fraction, onFraction, label }: PercentCellProps) {
  const [text, setText] = useState(() => fractionToPercentText(fraction));
  useEffect(() => {
    setText((current) => (percentToFractionText(current) === fraction ? current : fractionToPercentText(fraction)));
  }, [fraction]);
  return (
    <DecimalInput
      w={78}
      size="xs"
      aria-label={label}
      value={text}
      styles={{ input: { textAlign: 'right' } }}
      onChange={(nextText) => {
        setText(nextText);
        onFraction(percentToFractionText(nextText));
      }}
    />
  );
}

export function DropsTab() {
  const [file, setFile] = useState(DEFAULT_FILE);
  const [filterText, setFilterText] = useState('');
  const salvageFiles = useQuery({ queryKey: ['salvage-files'], queryFn: () => getJson<string[]>('/api/salvage-files') });
  const resourceName = `salvage:${file}`;
  const document = useDataDocument<DropsModel>({
    key: 'socketables.drops',
    noun: 'drop',
    fileNames: [resourceName],
    parse: (files) => parseSalvageCsv(files[resourceName].content),
    serialize: (model) => ({ [resourceName]: serializeSalvageCsv(model) }),
    validate: validateDrops,
    savedMessage: () => `Saved ${file}`,
  });

  const switchFile = async (nextFile: string | null) => {
    if (!nextFile || nextFile === file) {
      return;
    }
    if (await document.confirmDiscard(`Discard unsaved drop changes in ${file}?`)) {
      setFilterText('');
      setFile(nextFile);
    }
  };

  const addSite = async (model: DropsModel) => {
    const site = await promptText('Add site', 'Site id (custom entity type or salvage spec id)');
    if (!site) {
      return;
    }
    if (model.rows.some((row) => row.site === site)) {
      notifyError('That site is already listed.');
      return;
    }
    document.update((draft) => void draft.rows.push({ cells: [], passthrough: false, site, chances: ['0.1'], unique: '0' }));
    setFilterText(site);
  };

  const needle = filterText.trim().toLowerCase();

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <Select
            size="xs"
            w={300}
            aria-label="Drop file"
            data={salvageFiles.data ?? [DEFAULT_FILE]}
            value={file}
            onChange={(value) => void switchFile(value)}
            allowDeselect={false}
            searchable
          />
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => (
          <ScrollArea style={{ flex: 1 }} type="auto">
            <Stack p="md" gap="sm">
              <Text size="xs" c="dimmed">
                Files under compat/salvage/ only load when that mod is enabled, and only drop while the LunaLib setting "Drops From Other Mods' Sites" is on.
              </Text>
              <Text size="xs" c="dimmed">
                Edits data/config/exiledSector/{file}. Each site rolls item 1; item 2 only rolls if item 1 dropped, and so on. Leave a slot blank to stop there.
                Unique is a separate roll. "techmining_first_find" is the first Tech Mining harvest on a ruins colony; "techmining_monthly" is every later month and
                decays 5% a month. A site id is the custom entity type (or salvage spec override) of the thing salvaged.
              </Text>
              <Group align="flex-end">
                <TextInput
                  w={320}
                  label="Filter"
                  placeholder="site id"
                  leftSection={<IconSearch size={14} />}
                  value={filterText}
                  onChange={(event) => setFilterText(event.currentTarget.value)}
                />
                <Button leftSection={<IconPlus size={14} />} onClick={() => void addSite(model)}>
                  Add site
                </Button>
              </Group>
              <Table striped withTableBorder highlightOnHover w="fit-content">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Site</Table.Th>
                    {SLOT_INDEXES.map((slot) => (
                      <Table.Th key={slot}>
                        <Tooltip label={`Chance of item ${slot + 1}, rolled only if the previous one dropped`}>
                          <span>Item {slot + 1} %</span>
                        </Tooltip>
                      </Table.Th>
                    ))}
                    <Table.Th>
                      <Tooltip label="Separate roll for a unique socketable">
                        <span>Unique %</span>
                      </Tooltip>
                    </Table.Th>
                    <Table.Th>At least</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {model.rows.map((row, index) => {
                    if (row.passthrough || (needle && !row.site.toLowerCase().includes(needle))) {
                      return null;
                    }
                    return (
                      <Table.Tr key={index}>
                        <Table.Td>
                          <TextInput
                            size="xs"
                            w={240}
                            aria-label="Site id"
                            value={row.site}
                            onChange={(event) => {
                              const site = event.currentTarget.value.trim();
                              document.update((draft) => void (draft.rows[index].site = site));
                            }}
                          />
                        </Table.Td>
                        {SLOT_INDEXES.map((slot) => (
                          <Table.Td key={slot}>
                            <PercentCell
                              label={`${row.site} item ${slot + 1} percent`}
                              fraction={row.chances[slot] ?? ''}
                              onFraction={(fraction) => document.update((draft) => setChance(draft.rows[index], slot, fraction))}
                            />
                          </Table.Td>
                        ))}
                        <Table.Td>
                          <PercentCell
                            label={`${row.site} unique percent`}
                            fraction={row.unique}
                            onFraction={(fraction) => document.update((draft) => void (draft.rows[index].unique = fraction))}
                          />
                        </Table.Td>
                        <Table.Td>
                          <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                            {dropSummary(row)}
                          </Text>
                        </Table.Td>
                        <Table.Td>
                          <ActionIcon variant="subtle" color="red" onClick={() => document.update((draft) => void draft.rows.splice(index, 1))} aria-label={`Remove ${row.site}`}>
                            <IconX size={16} />
                          </ActionIcon>
                        </Table.Td>
                      </Table.Tr>
                    );
                  })}
                </Table.Tbody>
              </Table>
            </Stack>
          </ScrollArea>
        )}
      </DocumentState>
    </Stack>
  );
}
