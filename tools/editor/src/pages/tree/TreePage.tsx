import { Alert, Button, Center, Divider, Group, Loader, Stack } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useVocabulary } from '../../api/vocabulary.ts';
import { SERVER_HINT } from '../../api/client.ts';
import { TreeCanvas } from './canvas/TreeCanvas.tsx';
import { openAddNode } from './dialogs/treeDialogs.tsx';
import { GameTooltip } from './GameTooltip.tsx';
import { InspectorPanel } from './inspector/InspectorPanel.tsx';
import { clearSelection, deleteSelection, duplicateNode, setMode } from './state/treeActions.ts';
import { loadTree } from './state/treePersistence.ts';
import { useTreeStore, type EditorMode } from './state/treeStore.ts';
import { TreeToolbar } from './TreeToolbar.tsx';

const TOOLTIP_HULL_SIZE_KEY = 'exiledSectorEditor.tooltipHullSize';
const MODE_KEYS: Record<string, EditorMode> = { a: 'select', s: 'add', d: 'addAnchor', f: 'addImage', g: 'addStar' };

function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable || Boolean(target.closest('[role="dialog"]'));
}

function useTreeShortcuts(): void {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (isTypingTarget(event.target)) {
        return;
      }
      const key = event.key.toLowerCase();
      const commandKey = event.ctrlKey || event.metaKey;
      if (commandKey && key === 'd') {
        event.preventDefault();
        const selectedNodes = useTreeStore.getState().selection.nodes;
        if (selectedNodes.length === 1) {
          duplicateNode(selectedNodes[0]);
        }
        return;
      }
      if (!commandKey && MODE_KEYS[key]) {
        setMode(MODE_KEYS[key]);
      } else if ((!commandKey && key === 'q') || event.key === 'Delete' || event.key === 'Backspace') {
        deleteSelection();
      } else if (event.key === 'Escape') {
        clearSelection();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);
}

function useTooltipHullSize(): [string, (hullSize: string) => void] {
  const [hullSize, setHullSize] = useState(() => {
    try {
      return localStorage.getItem(TOOLTIP_HULL_SIZE_KEY) ?? '';
    } catch {
      return '';
    }
  });
  const update = (next: string) => {
    setHullSize(next);
    try {
      localStorage.setItem(TOOLTIP_HULL_SIZE_KEY, next);
    } catch {
      return;
    }
  };
  return [hullSize, update];
}

export function TreePage() {
  const load = useTreeStore((state) => state.load);
  const vocabulary = useVocabulary();
  const [tooltipHullSize, setTooltipHullSize] = useTooltipHullSize();
  useTreeShortcuts();

  useEffect(() => {
    if (useTreeStore.getState().load.status === 'idle') {
      void loadTree();
    }
  }, []);

  let body;
  if (load.status === 'error') {
    body = (
      <Stack p="md">
        <Alert color="red" icon={<IconAlertTriangle />} title="Could not load">
          {load.message}
        </Alert>
        <Button variant="default" w="fit-content" onClick={() => void loadTree()}>
          Retry
        </Button>
      </Stack>
    );
  } else if (load.status === 'ready') {
    body = (
      <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
        <TreeCanvas onRequestAddNode={(x, y) => openAddNode(vocabulary.data, x, y)} />
        <InspectorPanel vocabulary={vocabulary.data} />
      </Group>
    );
  } else {
    body = (
      <Center p="xl">
        <Loader />
      </Center>
    );
  }

  return (
    <Stack h="100%" gap={0}>
      <TreeToolbar vocabulary={vocabulary.data} tooltipHullSize={tooltipHullSize} onTooltipHullSize={setTooltipHullSize} />
      {vocabulary.isError ? (
        <Alert color="red" radius={0} py={6} icon={<IconAlertTriangle size={16} />}>
          Could not load the editor vocabulary, so saving is disabled - {SERVER_HINT}.
        </Alert>
      ) : null}
      <Divider />
      {body}
      <GameTooltip hullSize={tooltipHullSize} />
    </Stack>
  );
}
