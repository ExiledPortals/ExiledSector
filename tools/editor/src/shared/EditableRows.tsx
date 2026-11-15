import { Button, Input, Stack, Text } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { Fragment, type ReactNode } from 'react';
import { moveItem } from './arrays.ts';
import { RowButtons } from './MoveButtons.tsx';

export interface EditableRowControls<Row> {
  editRow: (recipe: (row: Row) => void) => void;
  rowButtons: ReactNode;
}

interface EditableRowsProps<Row> {
  label: string;
  rows: readonly Row[];
  emptyText: string;
  addLabel: string;
  newRow: () => Row;
  onEditRows: (recipe: (rows: Row[]) => void) => void;
  renderRow: (row: Row, index: number, controls: EditableRowControls<Row>) => ReactNode;
}

export function EditableRows<Row>({ label, rows, emptyText, addLabel, newRow, onEditRows, renderRow }: EditableRowsProps<Row>) {
  return (
    <Input.Wrapper label={label}>
      <Stack gap={6} mt={4}>
        {rows.length === 0 ? (
          <Text size="xs" c="dimmed">
            {emptyText}
          </Text>
        ) : null}
        {rows.map((row, index) => (
          <Fragment key={index}>
            {renderRow(row, index, {
              editRow: (recipe) => onEditRows((draftRows) => recipe(draftRows[index])),
              rowButtons: (
                <RowButtons
                  index={index}
                  count={rows.length}
                  onMove={(from, to) => onEditRows((draftRows) => moveItem(draftRows, from, to))}
                  onRemove={() => onEditRows((draftRows) => void draftRows.splice(index, 1))}
                />
              ),
            })}
          </Fragment>
        ))}
        <Button size="xs" variant="default" w="fit-content" leftSection={<IconPlus size={14} />} onClick={() => onEditRows((draftRows) => void draftRows.push(newRow()))}>
          {addLabel}
        </Button>
      </Stack>
    </Input.Wrapper>
  );
}
