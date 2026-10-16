import { Avatar, Group, Paper, ScrollArea, Stack, Text, TextInput, UnstyledButton } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { tierOf } from '../model/types.ts';
import { useTreeStore } from '../state/treeStore.ts';

interface TypePickerDialogProps {
  currentTypeId: string;
  onPick: (typeId: string) => void;
}

export function TypePickerDialog({ currentTypeId, onPick }: TypePickerDialogProps) {
  const types = useTreeStore((state) => state.document.types);
  const [filterText, setFilterText] = useState('');
  const shown = useMemo(() => {
    const needle = filterText.trim().toLowerCase();
    return types.filter((type) => !needle || `${type.id} ${type.name} ${(type.tags ?? []).join(' ')}`.toLowerCase().includes(needle));
  }, [types, filterText]);

  return (
    <Stack gap="xs">
      <TextInput
        placeholder="Filter by name, id, or tag..."
        leftSection={<IconSearch size={14} />}
        value={filterText}
        onChange={(event) => setFilterText(event.currentTarget.value)}
        data-autofocus
      />
      <ScrollArea h="60vh" offsetScrollbars>
        <Stack gap={4}>
          {shown.length === 0 ? (
            <Text size="sm" c="dimmed">
              No types match.
            </Text>
          ) : null}
          {shown.map((type) => (
            <UnstyledButton key={type.id} onClick={() => onPick(type.id)} data-type-id={type.id}>
              <Paper withBorder p={6} style={{ borderColor: type.id === currentTypeId ? 'var(--mantine-primary-color-filled)' : undefined }}>
                <Group gap="sm" wrap="nowrap">
                  <Avatar src={type.icon ? fileUrl(type.icon) : null} radius="xl" size={32} />
                  <div style={{ minWidth: 0 }}>
                    <Text size="sm" truncate>
                      {type.name}
                    </Text>
                    <Text size="xs" c="dimmed" ff="monospace">
                      {type.id} - {tierOf(type)}
                    </Text>
                  </div>
                </Group>
              </Paper>
            </UnstyledButton>
          ))}
        </Stack>
      </ScrollArea>
      <Text size="xs" c="dimmed">
        {shown.length} of {types.length} types
      </Text>
    </Stack>
  );
}
