import { Group, ScrollArea, Select, Stack, Text, TextInput, UnstyledButton } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { memo, useMemo, useState } from 'react';
import { keyGroup, stringIssues, type StringEntry, type StringsModel } from './model/strings.ts';
import classes from './TextPage.module.css';

const ALL_GROUPS = 'all';
const EDITED_ONLY = 'edited';

interface StringRowProps {
  entry: StringEntry;
  selected: boolean;
  broken: boolean;
  onSelect: (key: string) => void;
}

const StringRow = memo(function StringRow({ entry, selected, broken, onSelect }: StringRowProps) {
  const edited = entry.value !== entry.savedValue;
  return (
    <UnstyledButton className={`${classes.row} ${selected ? classes.rowSelected : ''}`} onClick={() => onSelect(entry.key)} aria-label={entry.key}>
      <div className={classes.rowKey}>
        {broken ? <span className={classes.marker} style={{ background: 'var(--mantine-color-red-6)' }} /> : null}
        {edited && !broken ? <span className={classes.marker} style={{ background: 'var(--mantine-color-yellow-5)' }} /> : null}
        {entry.key}
      </div>
      <Text size="sm" lineClamp={2}>
        {entry.value}
      </Text>
    </UnstyledButton>
  );
});

interface StringListProps {
  model: StringsModel;
  selectedKey: string | null;
  onSelect: (key: string) => void;
}

export function StringList({ model, selectedKey, onSelect }: StringListProps) {
  const [search, setSearch] = useState('');
  const [group, setGroup] = useState<string>(ALL_GROUPS);

  const groupOptions = useMemo(() => {
    const counts = new Map<string, number>();
    for (const entry of model.entries) {
      counts.set(keyGroup(entry.key), (counts.get(keyGroup(entry.key)) ?? 0) + 1);
    }
    const editedCount = model.entries.filter((entry) => entry.value !== entry.savedValue).length;
    return [
      { value: ALL_GROUPS, label: `All strings (${model.entries.length})` },
      { value: EDITED_ONLY, label: `Edited (${editedCount})` },
      ...[...counts.entries()].sort(([first], [second]) => first.localeCompare(second)).map(([name, count]) => ({ value: name, label: `${name}.* (${count})` })),
    ];
  }, [model.entries]);

  const visible = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return model.entries.filter((entry) => {
      if (group === EDITED_ONLY ? entry.value === entry.savedValue : group !== ALL_GROUPS && keyGroup(entry.key) !== group) {
        return false;
      }
      return !needle || entry.key.toLowerCase().includes(needle) || entry.value.toLowerCase().includes(needle);
    });
  }, [model.entries, search, group]);

  return (
    <div className={classes.listColumn}>
      <Stack gap="xs" p="sm">
        <TextInput
          placeholder="Search keys and text"
          leftSection={<IconSearch size={14} />}
          value={search}
          onChange={(event) => setSearch(event.currentTarget.value)}
          aria-label="Search strings"
        />
        <Group gap="xs" wrap="nowrap">
          <Select style={{ flex: 1 }} data={groupOptions} value={group} onChange={(value) => setGroup(value ?? ALL_GROUPS)} allowDeselect={false} aria-label="String group" />
          <Text size="xs" c="dimmed">
            {visible.length} shown
          </Text>
        </Group>
      </Stack>
      <ScrollArea style={{ flex: 1 }} type="auto">
        {visible.map((entry) => (
          <StringRow
            key={entry.key}
            entry={entry}
            selected={entry.key === selectedKey}
            broken={entry.value !== entry.savedValue && stringIssues(entry, model).errors.length > 0}
            onSelect={onSelect}
          />
        ))}
      </ScrollArea>
    </div>
  );
}
