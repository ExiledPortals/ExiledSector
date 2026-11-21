import { Button, Group, Kbd, SegmentedControl, Switch, Text, TextInput, Tooltip } from '@mantine/core';
import { IconDeviceFloppy, IconRefresh, IconSearch } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import type { Vocabulary } from '../../api/vocabulary.ts';
import { DECORATION_KIND_ORDER, DECORATION_KINDS, type DecorationKind } from './model/decorations.ts';
import { ANCHOR_SHAPE_LABELS, ANCHOR_SHAPES, type AnchorShape } from './model/types.ts';
import { SceneryToggle } from './SceneryToggle.tsx';
import { useTreeEditor, useTreeState } from './state/treeEditorContext.ts';
import type { EditorMode } from './state/treeStore.ts';

const MODES: { value: EditorMode; label: string; key: string }[] = [
  { value: 'select', label: 'Select / Move', key: 'A' },
  { value: 'add', label: 'Add node', key: 'S' },
  { value: 'addAnchor', label: 'Add anchor', key: 'D' },
  { value: 'addDecoration', label: 'Add decoration', key: 'F' },
];

interface TreeToolbarProps {
  vocabulary: Vocabulary | undefined;
  children?: ReactNode;
}

export function TreeToolbar({ vocabulary, children }: TreeToolbarProps) {
  const { store, actions, persistence } = useTreeEditor();
  const mode = useTreeState((state) => state.mode);
  const pendingAnchorShape = useTreeState((state) => state.pendingAnchorShape);
  const pendingDecorationKind = useTreeState((state) => state.pendingDecorationKind);
  const search = useTreeState((state) => state.search);
  const showLabels = useTreeState((state) => state.showLabels);
  const nodeCount = useTreeState((state) => state.document.nodes.length);
  const typeCount = useTreeState((state) => state.document.types.length);
  const zoom = useTreeState((state) => state.view.zoom);
  const dirty = useTreeState((state) => state.dirty);
  const saving = useTreeState((state) => state.saving);
  const ready = useTreeState((state) => state.load.status === 'ready');

  return (
    <Group px="md" py={6} gap="sm" wrap="wrap">
      <SegmentedControl
        size="xs"
        value={mode}
        onChange={(value) => actions.setMode(value as EditorMode)}
        data={MODES.map((entry) => ({
          value: entry.value,
          label: (
            <Group gap={6} wrap="nowrap">
              <span>{entry.label}</span>
              <Kbd size="xs">{entry.key}</Kbd>
            </Group>
          ),
        }))}
      />
      {mode === 'addAnchor' ? (
        <SegmentedControl
          size="xs"
          value={pendingAnchorShape}
          onChange={(value) => store.setState({ pendingAnchorShape: value as AnchorShape })}
          data={ANCHOR_SHAPES.map((shape) => ({ value: shape, label: ANCHOR_SHAPE_LABELS[shape] }))}
        />
      ) : null}
      {mode === 'addDecoration' ? (
        <SegmentedControl
          size="xs"
          value={pendingDecorationKind}
          onChange={(value) => store.setState({ pendingDecorationKind: value as DecorationKind })}
          data={DECORATION_KIND_ORDER.map((kind) => ({ value: kind, label: DECORATION_KINDS[kind].label }))}
          aria-label="Decoration kind"
        />
      ) : null}
      <TextInput
        size="xs"
        w={220}
        placeholder="Search node, type name or tag..."
        leftSection={<IconSearch size={14} />}
        value={search}
        onChange={(event) => store.setState({ search: event.currentTarget.value })}
      />
      <Switch size="xs" label="Labels" checked={showLabels} onChange={(event) => store.setState({ showLabels: event.currentTarget.checked })} />
      <SceneryToggle />
      {children}
      <Text size="xs" c="dimmed" ff="monospace" ml="auto">
        {nodeCount} nodes - {typeCount} types - zoom {zoom.toFixed(2)}x
      </Text>
      <Text size="xs" c={dirty ? 'yellow' : 'dimmed'}>
        {dirty ? 'Unsaved changes' : ready ? 'Saved' : ''}
      </Text>
      <Button size="xs" variant="default" leftSection={<IconRefresh size={14} />} disabled={saving} onClick={() => void persistence.reloadTreeFromDisk()}>
        Reload from disk
      </Button>
      <Tooltip label="Disabled until the tree files and the editor vocabulary have loaded" disabled={ready && Boolean(vocabulary)}>
        <Button size="xs" leftSection={<IconDeviceFloppy size={14} />} loading={saving} disabled={!ready || !vocabulary} onClick={() => void persistence.saveTree(vocabulary)}>
          Save to project
        </Button>
      </Tooltip>
    </Group>
  );
}
