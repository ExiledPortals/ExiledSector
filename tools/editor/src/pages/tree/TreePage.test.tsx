import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { notifications } from '@mantine/notifications';
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { openAddNode, openTypeEditor, openTypePicker } from './dialogs/treeDialogs.tsx';
import { nodeById } from './model/types.ts';
import { addNodeOfType, duplicateNode, selectOnly, setMode } from './state/treeActions.ts';
import { saveTree } from './state/treePersistence.ts';
import { EMPTY_SELECTION, isReservedNodeId, setSelection, useTreeStore } from './state/treeStore.ts';
import { TreePage } from './TreePage.tsx';

const TYPES_FILE = 'data/skilltrees/skill_types.json';
const TREE_FILE = 'data/skilltrees/ship_skill_tree.json';
const VOCABULARY_FILE = 'tools/editor_vocabulary.json';
const REPLACEMENTS_FILE = 'data/config/exiledSector/node_replacements.csv';
const RELEASED_NODES_FILE = 'src/test/resources/released_skill_nodes.json';
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile(VOCABULARY_FILE)));

let server: TestServer;

function nodeElements(container: HTMLElement): NodeListOf<SVGElement> {
  return container.querySelectorAll<SVGElement>('[data-node-id]');
}

async function renderLoadedTree() {
  const view = renderWithProviders(<TreePage />);
  await waitFor(() => expect(nodeElements(view.container).length).toBe(useTreeStore.getState().document.nodes.length), { timeout: 15000 });
  return view;
}

function commitField(label: string, value: string) {
  const input = screen.getByLabelText(label);
  fireEvent.change(input, { target: { value } });
  fireEvent.blur(input);
}

beforeAll(() => {
  server = startTestServer([TYPES_FILE, TREE_FILE, VOCABULARY_FILE, REPLACEMENTS_FILE, RELEASED_NODES_FILE]);
});

beforeEach(() => {
  localStorage.clear();
  useTreeStore.setState({ load: { status: 'idle' }, dirty: false, mode: 'select', search: '', selection: EMPTY_SELECTION });
});

afterAll(async () => {
  await server.close();
});

describe('tree page', () => {
  it('loads both tree files and draws every node, edge handle and anchor', async () => {
    const { container } = await renderLoadedTree();
    const document = useTreeStore.getState().document;
    expect(document.nodes.length).toBeGreaterThan(100);
    expect(container.querySelectorAll('[data-curve-a]').length).toBeGreaterThan(0);
    expect(container.querySelectorAll('[data-anchor-id]').length).toBe(document.anchors.length);
    expect(useTreeStore.getState().dirty).toBe(false);
  });

  it('never gives a new or duplicated node an id that was retired or released', async () => {
    await renderLoadedTree();
    const state = useTreeStore.getState();
    expect(state.replacedNodeIds.has('small_flux_optional_8')).toBe(true);
    expect(state.releasedNodeIds.size).toBeGreaterThan(100);

    act(() => addNodeOfType('small_flux_optional', 0, 0, vocabulary));
    const addedId = useTreeStore.getState().selection.nodes[0];
    act(() => duplicateNode(addedId));
    const duplicatedId = useTreeStore.getState().selection.nodes[0];

    for (const newId of [addedId, duplicatedId]) {
      expect(newId.startsWith('small_flux_optional_')).toBe(true);
      expect(isReservedNodeId(useTreeStore.getState(), newId)).toBe(false);
    }
  });

  it('selects a node by clicking it and edits it through the inspector', async () => {
    const { container } = await renderLoadedTree();
    const target = nodeElements(container)[0];
    const nodeId = target.dataset.nodeId ?? '';
    fireEvent.pointerDown(target, { button: 0, clientX: 0, clientY: 0 });
    window.dispatchEvent(new MouseEvent('pointerup'));
    expect(useTreeStore.getState().selection.nodes).toEqual([nodeId]);
    expect(await screen.findByText('Connected to')).toBeTruthy();

    commitField('X', '1234');
    expect(nodeById(useTreeStore.getState().document, nodeId)?.x).toBe(1234);
    expect(useTreeStore.getState().dirty).toBe(true);

    const shown = vi.spyOn(notifications, 'show');
    commitField('Node id', 'renamed_test_node');
    expect(useTreeStore.getState().selection.nodes).toEqual(['renamed_test_node']);
    expect(nodeById(useTreeStore.getState().document, nodeId)).toBeUndefined();
    expect(shown).not.toHaveBeenCalled();
    shown.mockRestore();
  });

  it('drags a selected node and pans the empty canvas', async () => {
    const { container } = await renderLoadedTree();
    const target = [...nodeElements(container)].find((element) => {
      const node = nodeById(useTreeStore.getState().document, element.dataset.nodeId ?? '');
      return node && !node.orbitAnchorId;
    });
    const nodeId = target?.dataset.nodeId ?? '';
    const before = nodeById(useTreeStore.getState().document, nodeId);
    fireEvent.pointerDown(target as Element, { button: 0, clientX: 0, clientY: 0 });
    act(() => {
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 40, clientY: 25 }));
      window.dispatchEvent(new MouseEvent('pointerup', { clientX: 40, clientY: 25 }));
    });
    const after = nodeById(useTreeStore.getState().document, nodeId);
    expect(after?.x === before?.x && after?.y === before?.y).toBe(false);

    const svg = nodeElements(container)[0].closest('svg') as SVGSVGElement;
    const panBefore = useTreeStore.getState().view.panX;
    fireEvent.pointerDown(svg, { button: 0, clientX: 10, clientY: 10 });
    act(() => {
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 70, clientY: 10 }));
      window.dispatchEvent(new MouseEvent('pointerup', { clientX: 70, clientY: 10 }));
    });
    expect(useTreeStore.getState().view.panX).toBe(panBefore + 60);
    expect(useTreeStore.getState().selection.nodes).toEqual([]);
  });

  it('shows the in-game tooltip next to a hovered node without needing the mouse to move', async () => {
    const { container } = await renderLoadedTree();
    fireEvent.mouseOver(nodeElements(container)[0], { clientX: 100, clientY: 50 });
    const note = await screen.findByText('Preparing in-game descriptions...');
    const box = note.parentElement as HTMLElement;
    expect([box.style.left, box.style.top]).toEqual(['116px', '66px']);
  });

  it('shows the anchor, image, star and multi-selection inspectors', async () => {
    await renderLoadedTree();
    const document = useTreeStore.getState().document;
    act(() => selectOnly('anchors', document.anchors[0].id));
    expect(await screen.findByLabelText('Anchor id')).toBeTruthy();
    commitField('Radius', '321');
    expect(useTreeStore.getState().document.anchors[0].radius).toBe(321);

    act(() => selectOnly('images', document.images[0].id));
    expect(await screen.findByText('Static image')).toBeTruthy();
    fireEvent.click(screen.getByText('Reverse spin'));

    act(() => selectOnly('stars', document.stars[0].id));
    expect(await screen.findByText('Star type')).toBeTruthy();

    act(() => setSelection({ nodes: [document.nodes[0].id, document.nodes[1].id], stars: [document.stars[0].id] }));
    expect(await screen.findByText('3 objects selected')).toBeTruthy();
    fireEvent.click(screen.getByText('Clear selection'));
    expect(useTreeStore.getState().selection.nodes).toEqual([]);
  });

  it('switches modes and deletes the selection from the keyboard', async () => {
    await renderLoadedTree();
    fireEvent.keyDown(window, { key: 's' });
    expect(useTreeStore.getState().mode).toBe('add');
    fireEvent.keyDown(window, { key: 'a' });
    expect(useTreeStore.getState().mode).toBe('select');

    const victim = useTreeStore.getState().document.nodes[5].id;
    act(() => selectOnly('nodes', victim));
    fireEvent.keyDown(window, { key: 'd', ctrlKey: true });
    const copyId = useTreeStore.getState().selection.nodes[0];
    expect(copyId).not.toBe(victim);
    expect(nodeById(useTreeStore.getState().document, copyId)?.connectedTo).toEqual([victim]);
    fireEvent.keyDown(window, { key: 'q' });
    expect(nodeById(useTreeStore.getState().document, copyId)).toBeUndefined();
    fireEvent.keyDown(window, { key: 'Escape' });
  });

  it('opens the type editor, type picker and add-node dialogs', async () => {
    await renderLoadedTree();
    const document = useTreeStore.getState().document;
    const typeId = document.types[0].id;

    act(() => openTypeEditor(vocabulary, typeId));
    const editor = await screen.findByRole('dialog');
    expect(within(editor).getByText('Unlock conditions')).toBeTruthy();
    fireEvent.click(within(editor).getByText('Add effect'));
    fireEvent.click(within(editor).getByText('Save type'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(useTreeStore.getState().document.types[0].effects?.length).toBe((document.types[0].effects?.length ?? 0) + 1);

    const nodeId = document.nodes[2].id;
    act(() => openTypePicker(document.nodes[2].type, (picked) => useTreeStore.setState({ search: picked })));
    const picker = await screen.findByRole('dialog');
    fireEvent.click(within(picker).getAllByRole('button')[1]);
    await waitFor(() => expect(useTreeStore.getState().search).not.toBe(''));
    expect(nodeById(useTreeStore.getState().document, nodeId)).toBeDefined();

    const nodeCount = useTreeStore.getState().document.nodes.length;
    act(() => openAddNode(vocabulary, 5, 6));
    const adder = await screen.findByRole('dialog');
    fireEvent.click(within(adder).getByText('Add node'));
    await waitFor(() => expect(useTreeStore.getState().document.nodes.length).toBe(nodeCount + 1));
    act(() => setMode('select'));
  });

  it('offers to restore an unsaved browser draft when the tree loads', async () => {
    const first = await renderLoadedTree();
    const { baseHashes, document } = useTreeStore.getState();
    const draftDocument = { ...document, nodes: document.nodes.map((node, index) => (index === 0 ? { ...node, x: 999999 } : node)) };
    localStorage.setItem('exiledSectorEditor.treeDraft', JSON.stringify({ basedOn: baseHashes, savedAt: 1, data: draftDocument }));
    first.unmount();
    useTreeStore.setState({ load: { status: 'idle' } });
    renderWithProviders(<TreePage />);
    fireEvent.click(await screen.findByText('Restore draft', {}, { timeout: 15000 }));
    await waitFor(() => expect(useTreeStore.getState().load.status).toBe('ready'));
    expect(useTreeStore.getState().document.nodes[0].x).toBe(999999);
    expect(useTreeStore.getState().dirty).toBe(true);
    localStorage.clear();
  });

  it('saves byte-identical files and then writes edits through the server', async () => {
    await renderLoadedTree();
    expect(useTreeStore.getState().dirty).toBe(false);
    await act(async () => saveTree(vocabulary));
    expect(server.readProjectFile(TYPES_FILE)).toBe(readRepositoryFile(TYPES_FILE));
    expect(server.readProjectFile(TREE_FILE)).toBe(readRepositoryFile(TREE_FILE));

    const document = useTreeStore.getState().document;
    act(() => selectOnly('nodes', document.nodes[0].id));
    commitField('X', '4321');
    await act(async () => saveTree(vocabulary));
    expect(useTreeStore.getState().dirty).toBe(false);
    expect(JSON.parse(server.readProjectFile(TREE_FILE)).nodes[0].x).toBe(4321);
  });
});
