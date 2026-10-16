import { ActionIcon, Box, ScrollArea, Text, Tooltip } from '@mantine/core';
import { IconLayoutSidebarRightCollapse, IconLayoutSidebarRightExpand } from '@tabler/icons-react';
import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { selectionCount, useTreeStore } from '../state/treeStore.ts';
import classes from './InspectorPanel.module.css';
import { NodeInspector } from './NodeInspector.tsx';
import { AnchorInspector, ImageInspector, MultiSelectionInspector, StarInspector } from './ObjectInspectors.tsx';

const PANEL_STORAGE_KEY = 'exiledSectorEditor.treeInspector';
const MIN_PANEL_WIDTH = 260;
const MAX_PANEL_WIDTH = 720;
const DEFAULT_PANEL_WIDTH = 380;

interface PanelLayout {
  panelWidth: number;
  collapsed: boolean;
}

function readPanelLayout(): PanelLayout {
  try {
    const stored = JSON.parse(localStorage.getItem(PANEL_STORAGE_KEY) ?? 'null') as Partial<PanelLayout> | null;
    return { panelWidth: typeof stored?.panelWidth === 'number' ? stored.panelWidth : DEFAULT_PANEL_WIDTH, collapsed: Boolean(stored?.collapsed) };
  } catch {
    return { panelWidth: DEFAULT_PANEL_WIDTH, collapsed: false };
  }
}

function SelectionInspector({ vocabulary }: { vocabulary: Vocabulary }) {
  const selection = useTreeStore((state) => state.selection);
  const total = selectionCount(selection);
  if (total === 0) {
    return (
      <Text size="sm" c="dimmed">
        Select a node, or the centre dot of an anchor, image or star, to see and edit its details. Switch to an Add mode and click the canvas to place
        something new. Shift-click or shift-drag to select several objects; drag any selected one to move them together. Ctrl-click another node to
        connect or disconnect it.
      </Text>
    );
  }
  if (total > 1) {
    return <MultiSelectionInspector />;
  }
  if (selection.anchors.length === 1) {
    return <AnchorInspector key={selection.anchors[0]} anchorId={selection.anchors[0]} vocabulary={vocabulary} />;
  }
  if (selection.images.length === 1) {
    return <ImageInspector key={selection.images[0]} imageId={selection.images[0]} vocabulary={vocabulary} />;
  }
  if (selection.stars.length === 1) {
    return <StarInspector key={selection.stars[0]} starId={selection.stars[0]} vocabulary={vocabulary} />;
  }
  return <NodeInspector key={selection.nodes[0]} nodeId={selection.nodes[0]} vocabulary={vocabulary} />;
}

export function InspectorPanel({ vocabulary }: { vocabulary: Vocabulary | undefined }) {
  const [layout, setLayout] = useState(readPanelLayout);
  const resizeStart = useRef<{ pointerX: number; panelWidth: number } | null>(null);

  useEffect(() => {
    try {
      localStorage.setItem(PANEL_STORAGE_KEY, JSON.stringify(layout));
    } catch {
      return;
    }
  }, [layout]);

  const startResize = (event: ReactPointerEvent<HTMLDivElement>) => {
    resizeStart.current = { pointerX: event.clientX, panelWidth: layout.panelWidth };
    event.currentTarget.setPointerCapture(event.pointerId);
  };
  const resize = (event: ReactPointerEvent<HTMLDivElement>) => {
    const start = resizeStart.current;
    if (start) {
      const panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, start.panelWidth + start.pointerX - event.clientX));
      setLayout((current) => ({ ...current, panelWidth }));
    }
  };

  const toggle = (
    <Tooltip label={layout.collapsed ? 'Show inspector' : 'Hide inspector'} position="left">
      <ActionIcon variant="subtle" color="gray" onClick={() => setLayout((current) => ({ ...current, collapsed: !current.collapsed }))} aria-label="Toggle inspector">
        {layout.collapsed ? <IconLayoutSidebarRightExpand size={18} /> : <IconLayoutSidebarRightCollapse size={18} />}
      </ActionIcon>
    </Tooltip>
  );

  if (layout.collapsed) {
    return (
      <Box className={classes.collapsedPanel} p={6}>
        {toggle}
      </Box>
    );
  }
  return (
    <Box className={classes.panel} style={{ width: layout.panelWidth }}>
      <div
        className={classes.resizeHandle}
        onPointerDown={startResize}
        onPointerMove={resize}
        onPointerUp={() => {
          resizeStart.current = null;
        }}
      />
      <Box className={classes.panelHeader} px="sm" py={4}>
        <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
          Inspector
        </Text>
        {toggle}
      </Box>
      <ScrollArea className={classes.panelBody} offsetScrollbars>
        <Box p="sm">{vocabulary ? <SelectionInspector vocabulary={vocabulary} /> : <Text size="sm">Loading the editor vocabulary...</Text>}</Box>
      </ScrollArea>
    </Box>
  );
}
