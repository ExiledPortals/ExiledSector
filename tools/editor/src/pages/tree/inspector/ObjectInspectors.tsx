import { Button, Divider, Group, Paper, Select, Stack, Text, Title, UnstyledButton } from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';
import { CommitNumberInput, CommitTextInput } from '../../../shared/CommitInputs.tsx';
import { notifyError } from '../../../shared/dialogs.tsx';
import { DECORATION_KINDS, decorationById } from '../model/decorations.ts';
import { resyncNodesOnAnchor, setAnchorShape } from '../model/geometry.ts';
import { ANCHOR_RECTANGLE_ASPECT, ANCHOR_SHAPE_LABELS, ANCHOR_SHAPES, anchorById, typeById, type AnchorShape, type OrbitAnchor, type TreeDocument } from '../model/types.ts';
import { clearSelection, deleteSelection, selectOnly } from '../state/treeActions.ts';
import { editDocument, selectionCount, setSelection, useTreeStore } from '../state/treeStore.ts';
import { degreesOrZero } from './inspectorEdits.ts';

function editAnchor(anchorId: string, change: (anchor: OrbitAnchor, document: TreeDocument) => void): void {
  editDocument((document) => {
    const anchor = anchorById(document, anchorId);
    if (anchor) {
      change(anchor, document);
      resyncNodesOnAnchor(document, anchor);
    }
  });
}

export function AnchorInspector({ anchorId }: { anchorId: string }) {
  const document = useTreeStore((state) => state.document);
  const anchor = anchorById(document, anchorId);
  if (!anchor) {
    return null;
  }
  const shape = anchor.shape ?? 'circle';
  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || anchorById(document, newId)) {
      notifyError('Id must be unique and non-empty');
      return;
    }
    editDocument((draft) => {
      const target = anchorById(draft, anchorId);
      if (!target) {
        return;
      }
      target.id = newId;
      for (const node of draft.nodes) {
        if (node.orbitAnchorId === anchorId) {
          node.orbitAnchorId = newId;
        }
      }
    });
    setSelection({ anchors: [newId] });
  };

  return (
    <Stack gap="sm">
      <Title order={4}>Anchor</Title>
      <CommitTextInput size="xs" label="Anchor id" value={anchor.id} onCommit={rename} />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="X" value={anchor.x} onCommit={(value) => editAnchor(anchorId, (target) => {
          target.x = value || 0;
        })} />
        <CommitNumberInput size="xs" label="Y" value={anchor.y} onCommit={(value) => editAnchor(anchorId, (target) => {
          target.y = value || 0;
        })} />
      </Group>
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="Radius"
          value={anchor.radius}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.radius = Math.max(10, Math.trunc(value) || 10);
          })}
        />
        <Select
          size="xs"
          label="Shape"
          data={ANCHOR_SHAPES.map((value) => ({ value, label: ANCHOR_SHAPE_LABELS[value] }))}
          value={shape}
          allowDeselect={false}
          onChange={(value) => editAnchor(anchorId, (target) => setAnchorShape(target, (value ?? 'circle') as AnchorShape))}
        />
      </Group>
      {shape === 'rectangle' ? (
        <CommitNumberInput
          size="xs"
          label="Aspect ratio (width/height)"
          value={anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.aspectRatio = Math.max(0.1, value || ANCHOR_RECTANGLE_ASPECT);
          })}
        />
      ) : null}
      {shape !== 'circle' ? (
        <CommitNumberInput
          size="xs"
          label="Rotation (degrees)"
          value={anchor.rotation || 0}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.rotation = degreesOrZero(value);
          })}
        />
      ) : null}
      <Divider />
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this anchor (Q)
      </Button>
    </Stack>
  );
}

export function MultiSelectionInspector() {
  const selection = useTreeStore((state) => state.selection);
  const document = useTreeStore((state) => state.document);
  const total = selectionCount(selection);
  const rows = [
    ...selection.nodes.map((id) => {
      const node = document.nodes.find((candidate) => candidate.id === id);
      const type = node ? typeById(document, node.type) : undefined;
      return { kind: 'nodes' as const, id, label: type ? `${id} (${type.name})` : id };
    }),
    ...selection.anchors.map((id) => ({ kind: 'anchors' as const, id, label: `${id} (anchor)` })),
    ...selection.decorations.map((id) => {
      const decoration = decorationById(document, id);
      return { kind: 'decorations' as const, id, label: `${id} (${decoration ? DECORATION_KINDS[decoration.kind].label.toLowerCase() : 'decoration'})` };
    }),
  ];
  return (
    <Stack gap="sm">
      <Title order={4}>{total} objects selected</Title>
      <Stack gap={4}>
        {rows.map((row) => (
          <UnstyledButton key={`${row.kind}:${row.id}`} onClick={() => selectOnly(row.kind, row.id)}>
            <Paper withBorder px={8} py={3}>
              <Text size="xs" ff="monospace" truncate>
                {row.label}
              </Text>
            </Paper>
          </UnstyledButton>
        ))}
      </Stack>
      <Group gap={6}>
        <Button size="xs" variant="default" onClick={clearSelection}>
          Clear selection
        </Button>
      </Group>
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete {total} selected (Q)
      </Button>
    </Stack>
  );
}
