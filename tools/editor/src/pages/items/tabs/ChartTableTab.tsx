import { ActionIcon, Alert, Box, Button, Divider, List, ScrollArea, Select, Stack, Table, Text, TextInput, Tooltip } from '@mantine/core';
import { IconAlertTriangle, IconPlus, IconX } from '@tabler/icons-react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { DataToolbar } from '../../../shared/DataToolbar.tsx';
import { DecimalInput } from '../../../shared/DecimalInput.tsx';
import { DocumentState } from '../../../shared/DocumentState.tsx';
import { PercentInput } from '../../../shared/PercentInput.tsx';
import { useDataDocument, type DataDocument } from '../../../shared/useDataDocument.ts';
import { parseChartTable, serializeChartTable, type ChartColumn, type ChartTableModel, type ChartTableRow, type ChartTableSpec } from '../model/chartTables.ts';

interface ChartTableTabProps {
  spec: ChartTableSpec;
  vocabulary: Vocabulary;
}

const FLAG_OPTIONS = ['yes', 'no'];
const TEXT_COLUMN_WIDTH = 200;
const NOTE_COLUMN_WIDTH = 320;
const NUMBER_COLUMN_WIDTH = 82;

function optionsWithCurrent(options: string[], current: string): string[] {
  return current === '' || options.includes(current) ? options : [current, ...options];
}

interface CellInputProps {
  column: ChartColumn;
  row: ChartTableRow;
  rowName: string;
  vocabulary: Vocabulary;
  onChange: (text: string) => void;
}

function CellInput({ column, row, rowName, vocabulary, onChange }: CellInputProps) {
  const current = row.values[column.column] ?? '';
  const label = `${rowName} ${column.column}`;
  switch (column.kind) {
    case 'fraction':
      return <PercentInput label={label} fraction={current} onFraction={onChange} />;
    case 'integer':
    case 'number':
      return <DecimalInput w={NUMBER_COLUMN_WIDTH} size="xs" aria-label={label} value={current} styles={{ input: { textAlign: 'right' } }} onChange={onChange} />;
    case 'flag':
    case 'choice': {
      const options = column.kind === 'flag' ? FLAG_OPTIONS : (column.choices?.(vocabulary) ?? []);
      return (
        <Select
          size="xs"
          w={column.kind === 'flag' ? 80 : 170}
          aria-label={label}
          data={optionsWithCurrent(options, current)}
          value={current === '' ? null : current}
          placeholder="-"
          clearable={column.kind === 'choice'}
          onChange={(value) => onChange(value ?? '')}
        />
      );
    }
    default:
      return (
        <TextInput
          size="xs"
          w={column.column === 'note' ? NOTE_COLUMN_WIDTH : TEXT_COLUMN_WIDTH}
          aria-label={label}
          value={current}
          onChange={(event) => onChange(column.column === 'note' ? event.currentTarget.value : event.currentTarget.value.trim())}
        />
      );
  }
}

function ProblemList({ issues, warnings }: { issues: string[]; warnings: string[] }) {
  if (issues.length === 0 && warnings.length === 0) {
    return null;
  }
  return (
    <Stack gap="xs">
      {issues.length > 0 && (
        <Alert color="red" icon={<IconAlertTriangle />} title={`${issues.length} ${issues.length === 1 ? 'problem blocks' : 'problems block'} saving`}>
          <List size="sm" spacing={2}>
            {issues.map((issue) => (
              <List.Item key={issue}>{issue}</List.Item>
            ))}
          </List>
        </Alert>
      )}
      {warnings.length > 0 && (
        <Alert color="yellow" icon={<IconAlertTriangle />} title={`${warnings.length} warning${warnings.length === 1 ? '' : 's'}`}>
          <List size="sm" spacing={2}>
            {warnings.map((warning) => (
              <List.Item key={warning}>{warning}</List.Item>
            ))}
          </List>
        </Alert>
      )}
    </Stack>
  );
}

function ChartTableView({ spec, vocabulary, model, document }: ChartTableTabProps & { model: ChartTableModel; document: DataDocument<ChartTableModel> }) {
  const editCell = (rowIndex: number, column: string, text: string) =>
    document.update((draft) => {
      draft.rows[rowIndex].values[column] = text;
    });
  return (
    <ScrollArea style={{ flex: 1 }} type="auto">
      <Stack p="md" gap="sm">
        <Text size="xs" c="dimmed">
          Edits data/config/exiledSector/{spec.fileName}. {spec.description}
        </Text>
        <ProblemList issues={spec.issues(model, vocabulary)} warnings={spec.warnings(model, vocabulary)} />
        <Table striped withTableBorder highlightOnHover w="fit-content">
          <Table.Thead>
            <Table.Tr>
              {spec.columns.map((column) => (
                <Table.Th key={column.column}>
                  {column.help ? (
                    <Tooltip label={column.help} multiline maw={320}>
                      <span>{column.label}</span>
                    </Tooltip>
                  ) : (
                    column.label
                  )}
                </Table.Th>
              ))}
              {spec.summary && <Table.Th>Summary</Table.Th>}
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {model.rows.map((row, rowIndex) => {
              const rowName = row.values[spec.keyColumn]?.trim() || `row ${rowIndex + 1}`;
              return (
                <Table.Tr key={rowIndex}>
                  {spec.columns.map((column) => (
                    <Table.Td key={column.column}>
                      <CellInput column={column} row={row} rowName={rowName} vocabulary={vocabulary} onChange={(text) => editCell(rowIndex, column.column, text)} />
                    </Table.Td>
                  ))}
                  {spec.summary && (
                    <Table.Td>
                      <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                        {spec.summary(row, vocabulary)}
                      </Text>
                    </Table.Td>
                  )}
                  <Table.Td>
                    <ActionIcon
                      variant="subtle"
                      color="red"
                      aria-label={`Remove ${rowName}`}
                      onClick={() => document.update((draft) => void draft.rows.splice(rowIndex, 1))}
                    >
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
  );
}

export function ChartTableTab({ spec, vocabulary }: ChartTableTabProps) {
  const document = useDataDocument<ChartTableModel>({
    key: `items.${spec.resource}`,
    noun: spec.noun,
    fileNames: [spec.resource],
    parse: (files) => parseChartTable(files[spec.resource].content, spec.columns),
    serialize: (model) => ({ [spec.resource]: serializeChartTable(model, spec.columns) }),
    validate: (model) => spec.issues(model, vocabulary),
    warnings: (model) => spec.warnings(model, vocabulary),
    savedMessage: () => `Saved ${spec.fileName}`,
  });

  const addRow = () =>
    document.update((draft) => {
      draft.rows.push({ cells: [], values: spec.newRow(draft as ChartTableModel, vocabulary) });
    });

  return (
    <Stack h="100%" gap={0}>
      <Box px="md" py="xs">
        <DataToolbar document={document}>
          <Button size="xs" variant="default" leftSection={<IconPlus size={14} />} onClick={addRow} disabled={document.state.status !== 'ready'}>
            Add {spec.noun}
          </Button>
        </DataToolbar>
      </Box>
      <Divider />
      <DocumentState state={document.state} onRetry={() => void document.reload()}>
        {(model) => <ChartTableView spec={spec} vocabulary={vocabulary} model={model} document={document} />}
      </DocumentState>
    </Stack>
  );
}
