import { Alert, Divider, Group, Stack } from '@mantine/core';
import { IconAlertTriangle } from '@tabler/icons-react';
import { useEffect, type ReactNode } from 'react';
import { SERVER_HINT } from '../../api/client.ts';
import { DocumentLoadError, DocumentLoading } from '../../shared/DocumentState.tsx';
import { TreeCanvas } from './canvas/TreeCanvas.tsx';
import { openAddNode } from './dialogs/treeDialogs.tsx';
import { InspectorPanel } from './inspector/InspectorPanel.tsx';
import type { TreeEditor } from './state/treeEditor.ts';
import { TreeEditorContext, useTreeEditor, useTreeState, useTreeVocabulary } from './state/treeEditorContext.ts';
import type { EditorMode } from './state/treeStore.ts';
import { TreeToolbar } from './TreeToolbar.tsx';

const MODE_KEYS: Record<string, EditorMode> = { a: 'select', s: 'add', d: 'addAnchor', f: 'addDecoration' };

function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable || Boolean(target.closest('[role="dialog"]'));
}

function useTreeShortcuts(): void {
  const { store, actions } = useTreeEditor();
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (isTypingTarget(event.target)) {
        return;
      }
      const key = event.key.toLowerCase();
      const commandKey = event.ctrlKey || event.metaKey;
      if (commandKey && key === 'd') {
        event.preventDefault();
        const selectedNodes = store.getState().selection.nodes;
        if (selectedNodes.length === 1) {
          actions.duplicateNode(selectedNodes[0]);
        }
        return;
      }
      if (!commandKey && MODE_KEYS[key]) {
        actions.setMode(MODE_KEYS[key]);
      } else if ((!commandKey && key === 'q') || event.key === 'Delete' || event.key === 'Backspace') {
        actions.deleteSelection();
      } else if (event.key === 'Escape') {
        actions.clearSelection();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [store, actions]);
}

interface TreeEditorPageProps {
  editor: TreeEditor;
  toolbarControls?: ReactNode;
  overlay?: ReactNode;
}

function TreeEditorBody({ toolbarControls, overlay }: Omit<TreeEditorPageProps, 'editor'>) {
  const editor = useTreeEditor();
  const { store, persistence } = editor;
  const load = useTreeState((state) => state.load);
  const vocabulary = useTreeVocabulary();
  useTreeShortcuts();

  useEffect(() => {
    if (store.getState().load.status === 'idle') {
      void persistence.loadTree();
    }
  }, [store, persistence]);

  let body;
  if (load.status === 'error') {
    body = <DocumentLoadError message={load.message} onRetry={() => void persistence.loadTree()} />;
  } else if (load.status === 'ready') {
    body = (
      <Group align="stretch" gap={0} wrap="nowrap" style={{ flex: 1, minHeight: 0 }}>
        <TreeCanvas onRequestAddNode={(x, y) => openAddNode(editor, vocabulary.data, x, y)} />
        <InspectorPanel vocabulary={vocabulary.data} />
      </Group>
    );
  } else {
    body = <DocumentLoading />;
  }

  return (
    <Stack h="100%" gap={0}>
      <TreeToolbar vocabulary={vocabulary.data}>{toolbarControls}</TreeToolbar>
      {vocabulary.isError ? (
        <Alert color="red" radius={0} py={6} icon={<IconAlertTriangle size={16} />}>
          Could not load the editor vocabulary, so saving is disabled - {SERVER_HINT}.
        </Alert>
      ) : null}
      <Divider />
      {body}
      {overlay}
    </Stack>
  );
}

export function TreeEditorPage({ editor, toolbarControls, overlay }: TreeEditorPageProps) {
  return (
    <TreeEditorContext value={editor}>
      <TreeEditorBody toolbarControls={toolbarControls} overlay={overlay} />
    </TreeEditorContext>
  );
}
