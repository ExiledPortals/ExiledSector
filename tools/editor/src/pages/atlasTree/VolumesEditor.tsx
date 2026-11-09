import { ActionIcon, Button, Group, Stack, Text, TextInput, Tooltip } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { useState } from 'react';
import { CommitTextInput } from '../../shared/CommitInputs.tsx';
import { notifyError } from '../../shared/dialogs.tsx';
import { volumeIdProblem, volumeUseCount } from '../tree/model/volumes.ts';
import { useTreeEditor, useTreeState } from '../tree/state/treeEditorContext.ts';

function usesText(count: number): string {
  return count === 1 ? '1 use' : `${count} uses`;
}

function VolumeRow({ volumeId }: { volumeId: string }) {
  const { actions } = useTreeEditor();
  const document = useTreeState((state) => state.document);
  const useCount = volumeUseCount(document, volumeId);
  const rename = (text: string) => {
    const newVolumeId = text.trim();
    if (newVolumeId === volumeId) {
      return;
    }
    const problem = volumeIdProblem(document, newVolumeId);
    if (problem) {
      notifyError(problem);
      return;
    }
    actions.renameDeclaredVolume(volumeId, newVolumeId);
  };
  return (
    <Group gap={6} wrap="nowrap">
      <CommitTextInput size="xs" style={{ flex: 1 }} aria-label={`Volume ${volumeId}`} value={volumeId} onCommit={rename} />
      <Text size="xs" c="dimmed" w={60}>
        {usesText(useCount)}
      </Text>
      <Tooltip label={useCount > 0 ? 'Retag its nodes and decorations before removing it' : 'Remove this volume'}>
        <ActionIcon
          variant="subtle"
          color="red"
          aria-label={`Remove volume ${volumeId}`}
          disabled={useCount > 0}
          onClick={() => actions.removeDeclaredVolume(volumeId)}
        >
          <IconTrash size={14} />
        </ActionIcon>
      </Tooltip>
    </Group>
  );
}

export function VolumesEditor() {
  const { actions } = useTreeEditor();
  const document = useTreeState((state) => state.document);
  const [newVolumeId, setNewVolumeId] = useState('');
  const add = () => {
    const volumeId = newVolumeId.trim();
    const problem = volumeIdProblem(document, volumeId);
    if (problem) {
      notifyError(problem);
      return;
    }
    actions.addDeclaredVolume(volumeId);
    setNewVolumeId('');
  };
  return (
    <Stack gap="sm">
      <Text size="sm" c="dimmed">
        A node or decoration belongs to a volume by carrying its id as a tag. Renaming a volume renames that tag everywhere.
      </Text>
      {document.volumes.length === 0 ? (
        <Text size="sm" c="dimmed">
          No volumes yet.
        </Text>
      ) : null}
      {document.volumes.map((volumeId) => (
        <VolumeRow key={volumeId} volumeId={volumeId} />
      ))}
      <Group gap={6} align="flex-end" wrap="nowrap">
        <TextInput
          size="xs"
          style={{ flex: 1 }}
          label="New volume id"
          value={newVolumeId}
          onChange={(event) => setNewVolumeId(event.currentTarget.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              add();
            }
          }}
          data-autofocus
        />
        <Button size="xs" leftSection={<IconPlus size={14} />} onClick={add}>
          Add volume
        </Button>
      </Group>
    </Stack>
  );
}
