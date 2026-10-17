import { Button, Group, Text } from '@mantine/core';
import { IconDeviceFloppy, IconRefresh } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import type { DataDocument } from './useDataDocument.ts';

interface DataToolbarProps<TModel> {
  document: DataDocument<TModel>;
  children?: ReactNode;
}

export function DataToolbar<TModel>({ document, children }: DataToolbarProps<TModel>) {
  const ready = document.state.status === 'ready';
  return (
    <Group gap="xs" wrap="nowrap">
      <Text size="sm" c={document.dirty ? 'yellow' : 'dimmed'} mr="auto">
        {document.dirty ? 'Unsaved changes' : ready ? 'Saved' : ''}
      </Text>
      {children}
      <Button variant="default" size="xs" leftSection={<IconRefresh size={14} />} disabled={document.saving} onClick={() => void document.reload()}>
        Reload from disk
      </Button>
      <Button size="xs" leftSection={<IconDeviceFloppy size={14} />} loading={document.saving} disabled={!ready} onClick={() => void document.save()}>
        Save to project
      </Button>
    </Group>
  );
}
