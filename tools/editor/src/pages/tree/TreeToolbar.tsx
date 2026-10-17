import { Button, Group, Kbd, SegmentedControl, Select, Switch, Text, TextInput, Tooltip } from '@mantine/core';
import { IconDeviceFloppy, IconRefresh, IconSearch } from '@tabler/icons-react';
import type { Vocabulary } from '../../api/vocabulary.ts';
import { ANCHOR_SHAPE_LABELS, ANCHOR_SHAPES, HULL_SIZE_OPTIONS, type AnchorShape } from './model/types.ts';
import { setMode } from './state/treeActions.ts';
import { reloadTreeFromDisk, saveTree } from './state/treePersistence.ts';
import { useTreeStore, type EditorMode } from './state/treeStore.ts';

const MODES: { value: EditorMode; label: string; key: string }[] = [
  { value: 'select', label: 'Select / Move', key: 'A' },
  { value: 'add', label: 'Add node', key: 'S' },
  { value: 'addAnchor', label: 'Add anchor', key: 'D' },
  { value: 'addImage', label: 'Add image', key: 'F' },
  { value: 'addStar', label: 'Add star', key: 'G' },
];

const TOOLTIP_HULL_SIZES = [{ value: '', label: 'All sizes' }, ...HULL_SIZE_OPTIONS.map((option) => ({ value: option.value, label: option.value === 'CAPITAL_SHIP' ? 'Capital ship' : option.label }))];

interface TreeToolbarProps {
  vocabulary: Vocabulary | undefined;
  tooltipHullSize: string;
  onTooltipHullSize: (hullSize: string) => void;
}

export function TreeToolbar({ vocabulary, tooltipHullSize, onTooltipHullSize }: TreeToolbarProps) {
  const mode = useTreeStore((state) => state.mode);
  const pendingAnchorShape = useTreeStore((state) => state.pendingAnchorShape);
  const search = useTreeStore((state) => state.search);
  const showLabels = useTreeStore((state) => state.showLabels);
  const nodeCount = useTreeStore((state) => state.document.nodes.length);
  const typeCount = useTreeStore((state) => state.document.types.length);
  const zoom = useTreeStore((state) => state.view.zoom);
  const dirty = useTreeStore((state) => state.dirty);
  const saving = useTreeStore((state) => state.saving);
  const ready = useTreeStore((state) => state.load.status === 'ready');

  return (
    <Group px="md" py={6} gap="sm" wrap="wrap">
      <SegmentedControl
        size="xs"
        value={mode}
        onChange={(value) => setMode(value as EditorMode)}
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
          onChange={(value) => useTreeStore.setState({ pendingAnchorShape: value as AnchorShape })}
          data={ANCHOR_SHAPES.map((shape) => ({ value: shape, label: ANCHOR_SHAPE_LABELS[shape] }))}
        />
      ) : null}
      <TextInput
        size="xs"
        w={220}
        placeholder="Search node, type name or tag..."
        leftSection={<IconSearch size={14} />}
        value={search}
        onChange={(event) => useTreeStore.setState({ search: event.currentTarget.value })}
      />
      <Switch size="xs" label="Labels" checked={showLabels} onChange={(event) => useTreeStore.setState({ showLabels: event.currentTarget.checked })} />
      <Tooltip label="Hull size used by the in-game tooltip preview (hover a node, F1 for details)">
        <Select size="xs" w={130} data={TOOLTIP_HULL_SIZES} value={tooltipHullSize} onChange={(value) => onTooltipHullSize(value ?? '')} allowDeselect={false} />
      </Tooltip>
      <Text size="xs" c="dimmed" ff="monospace" ml="auto">
        {nodeCount} nodes - {typeCount} types - zoom {zoom.toFixed(2)}x
      </Text>
      <Text size="xs" c={dirty ? 'yellow' : 'dimmed'}>
        {dirty ? 'Unsaved changes' : ready ? 'Saved' : ''}
      </Text>
      <Button size="xs" variant="default" leftSection={<IconRefresh size={14} />} disabled={saving} onClick={() => void reloadTreeFromDisk()}>
        Reload from disk
      </Button>
      <Tooltip label="Disabled until the tree files and the editor vocabulary have loaded" disabled={ready && Boolean(vocabulary)}>
        <Button size="xs" leftSection={<IconDeviceFloppy size={14} />} loading={saving} disabled={!ready || !vocabulary} onClick={() => void saveTree(vocabulary)}>
          Save to project
        </Button>
      </Tooltip>
    </Group>
  );
}
