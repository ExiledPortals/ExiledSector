import { Group, Stack, Table, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { fetchModifierLineDescriptions, stringsRevision, type DescribedModifierLines, type ModifierLineRequest } from '../../../api/describe.ts';
import { isNumberText } from '../../../shared/decimalText.ts';
import { renderMarkup, TOOLTIP_PANEL_CLASS } from '../../../shared/tooltipMarkup.ts';
import { AFFIX_GROUPS, isTrueText, poolShares, poolWeight, type ItemDefinition } from '../model/definitions.ts';
import { lineSummary, type ItemModifier, type ModifierLine } from '../model/modifiers.ts';

const RETRY_DELAY_MS = 1500;

interface DefinitionViewProps {
  definition: ItemDefinition;
  catalogue: ItemModifier[];
  implicitModifierIds: string[];
  catalogueFileName?: string;
  describeLines?: boolean;
}

interface ViewRow {
  key: string;
  weight: string;
  tiers?: string;
  share: number | null;
  modifierId: string;
  modifier: ItemModifier | undefined;
  inlineText?: string;
}

function lineRequest(line: ModifierLine): ModifierLineRequest | null {
  if (line.tierBands) {
    return null;
  }
  if (line.hullValues) {
    return line.hullValues.every(isNumberText) ? { effect: line.effect, hullValues: line.hullValues.map(Number) } : null;
  }
  return isNumberText(line.min) && isNumberText(line.max) ? { effect: line.effect, min: Number(line.min), max: Number(line.max) } : null;
}

function lineKey(line: ModifierLine): string {
  return `${line.effect}|${line.min}|${line.max}|${(line.hullValues ?? []).join('/')}`;
}

function descriptionNote(described: DescribedModifierLines | undefined): string | null {
  if (!described || described.lines) {
    return null;
  }
  if (described.pending) {
    return described.message || 'Preparing in-game descriptions...';
  }
  return described.error ?? described.message ?? null;
}

function useLineDescriptions(lines: ModifierLine[]) {
  const requests = new Map<string, ModifierLineRequest>();
  for (const line of lines) {
    const request = lineRequest(line);
    if (request) {
      requests.set(lineKey(line), request);
    }
  }
  const keys = [...requests.keys()];
  const query = useQuery({
    queryKey: ['modifierLineDescriptions', keys, stringsRevision()],
    queryFn: () => fetchModifierLineDescriptions([...requests.values()]),
    refetchInterval: (state) => (state.state.data?.pending ? RETRY_DELAY_MS : false),
    staleTime: Infinity,
    enabled: keys.length > 0,
  });
  const describedByKey = new Map<string, string | null>();
  const described = query.data?.lines;
  if (described) {
    keys.forEach((key, index) => describedByKey.set(key, described[index] ?? null));
  }
  const note = descriptionNote(query.data);
  return { describedByKey, note };
}

function LineText({ line, describedByKey }: { line: ModifierLine; describedByKey: Map<string, string | null> }) {
  const markup = describedByKey.get(lineKey(line));
  return <div>{markup ? renderMarkup(markup) : lineSummary(line)}</div>;
}

function rowsOf(entries: { modifierId: string; weight: string; tiers?: string; inlineText?: string }[], catalogue: ItemModifier[], withShares: boolean): ViewRow[] {
  const shares = withShares ? poolShares(entries.map((entry) => ({ modifierId: entry.modifierId, weight: entry.weight }))) : [];
  return entries.map((entry, index) => ({
    key: `${index}|${entry.modifierId}`,
    weight: entry.weight,
    tiers: entry.tiers,
    share: withShares ? shares[index] : null,
    modifierId: entry.modifierId,
    modifier: catalogue.find((candidate) => candidate.id === entry.modifierId),
    inlineText: entry.inlineText,
  }));
}

interface RowsTableProps {
  rows: ViewRow[];
  describedByKey: Map<string, string | null>;
  showWeights: boolean;
  catalogueFileName: string;
}

function RowsTable({ rows, describedByKey, showWeights, catalogueFileName }: RowsTableProps) {
  return (
    <Table verticalSpacing={4} horizontalSpacing="xs" withRowBorders={false}>
      <Table.Tbody>
        {rows.map((row) => (
          <Table.Tr key={row.key}>
            {showWeights && (
              <Table.Td w={110} style={{ verticalAlign: 'top', whiteSpace: 'nowrap' }}>
                <Text span size="sm" c="#ffc800">
                  {row.weight === '' ? '1' : row.weight}
                </Text>
                <Text span size="xs" c="dimmed">
                  {' '}
                  ({Math.round((row.share ?? 0) * 100)}%)
                </Text>
                {row.tiers && (
                  <Text size="xs" c="dimmed">
                    {row.tiers}
                  </Text>
                )}
              </Table.Td>
            )}
            <Table.Td style={{ verticalAlign: 'top' }}>
              {row.modifier ? (
                row.modifier.lines.map((line, lineIndex) => <LineText key={lineIndex} line={line} describedByKey={describedByKey} />)
              ) : (
                <Text span size="sm" c="#fc6300">
                  {row.inlineText ?? row.modifierId} is not in {catalogueFileName}
                </Text>
              )}
            </Table.Td>
            <Table.Td w={260} style={{ verticalAlign: 'top' }}>
              <Text span size="sm" c="#5abeff">
                {row.modifier?.name || ''}
              </Text>
              <Text size="xs" c="dimmed" truncate>
                {row.modifierId}
              </Text>
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}

export function DefinitionView({ definition, catalogue, implicitModifierIds, catalogueFileName = 'item_modifiers.csv', describeLines = true }: DefinitionViewProps) {
  const implicitRows = rowsOf(
    implicitModifierIds.map((modifierId) => ({ modifierId, weight: '' })),
    catalogue,
    false,
  );
  const groupRows = AFFIX_GROUPS.map((group) => ({ group, rows: rowsOf(definition[group.key], catalogue, true) }));
  const allLines = [...implicitRows, ...groupRows.flatMap((entry) => entry.rows)].flatMap((row) => row.modifier?.lines ?? []);
  const { describedByKey, note } = useLineDescriptions(describeLines ? allLines : []);

  return (
    <div className={TOOLTIP_PANEL_CLASS} style={{ maxWidth: 'none' }}>
      <Stack gap="sm">
        <Group justify="space-between" align="baseline">
          <Title order={4} c="white">
            {definition.name || definition.id}
          </Title>
          <Text size="xs" c="dimmed">
            {definition.kind}
            {isTrueText(definition.unique) ? ' - unique' : ''} - loot weight {definition.rarity || '0'}
          </Text>
        </Group>
        {definition.description && (
          <Text size="sm" c="#8c8c8c" fs="italic">
            {definition.description}
          </Text>
        )}
        {note && (
          <Text size="xs" c="dimmed">
            {note}
          </Text>
        )}
        {implicitRows.length > 0 && (
          <Stack gap={2}>
            <Text size="sm" fw={600} c="white">
              Implicit
            </Text>
            <RowsTable rows={implicitRows} describedByKey={describedByKey} showWeights={false} catalogueFileName={catalogueFileName} />
          </Stack>
        )}
        {groupRows.map(({ group, rows }) => (
          <Stack key={group.key} gap={2}>
            <Group gap="xs" align="baseline">
              <Text size="sm" fw={600} c="white">
                {group.label}
              </Text>
              <Text size="xs" c="dimmed">
                {rows.length} modifiers, total weight {rows.reduce((total, row) => total + (poolWeight({ modifierId: row.modifierId, weight: row.weight }) || 0), 0)}
              </Text>
            </Group>
            {rows.length === 0 ? (
              <Text size="sm" c="dimmed">
                None.
              </Text>
            ) : (
              <RowsTable rows={rows} describedByKey={describedByKey} showWeights catalogueFileName={catalogueFileName} />
            )}
          </Stack>
        ))}
      </Stack>
    </div>
  );
}
