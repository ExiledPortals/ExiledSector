import fs from 'node:fs';
import path from 'node:path';
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { notifications } from '@mantine/notifications';
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { openAddNode, openTypeEditor, openTypePicker } from './dialogs/treeDialogs.tsx';
import { DECORATION_KINDS, decorationById, decorationsOfKind } from './model/decorations.ts';
import { nodeById, tierOf, typeById } from './model/types.ts';
import { shipTreeEditor } from './state/treeEditor.ts';
import { EMPTY_DOCUMENT, EMPTY_SELECTION, isReservedNodeId } from './state/treeStore.ts';
import { TreePage } from './TreePage.tsx';

const TYPES_FILE = 'data/skilltrees/skill_types.json';
const TREE_FILE = 'data/skilltrees/ship_skill_tree.json';
const VOCABULARY_FILE = 'tools/editor_vocabulary.json';
const REPLACEMENTS_FILE = 'data/config/exiledSector/node_replacements.csv';
const RELEASED_NODES_FILE = 'exiledsector-skill-tree/src/test/resources/released_skill_nodes.json';
const NODE_RADII_FILE = 'data/config/exiledSector/node_radii.json';
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile(VOCABULARY_FILE)));
const shipTreeStore = shipTreeEditor.store;
const { addNodeOfType, duplicateNode, fitToScreen, selectOnly, setMode, setSelection } = shipTreeEditor.actions;
const { loadTree, saveTree } = shipTreeEditor.persistence;

let server: TestServer;

function nodeElements(container: HTMLElement): NodeListOf<SVGElement> {
  return container.querySelectorAll<SVGElement>('[data-node-id]');
}

async function renderLoadedTree() {
  const view = renderWithProviders(<TreePage />);
  await waitFor(() => expect(nodeElements(view.container).length).toBe(shipTreeStore.getState().document.nodes.length), { timeout: 15000 });
  return view;
}

function commitField(label: string, value: string) {
  const input = screen.getByLabelText(label);
  fireEvent.change(input, { target: { value } });
  fireEvent.blur(input);
}

beforeAll(() => {
  server = startTestServer([TYPES_FILE, TREE_FILE, VOCABULARY_FILE, REPLACEMENTS_FILE, RELEASED_NODES_FILE, NODE_RADII_FILE]);
});

beforeEach(() => {
  localStorage.clear();
  shipTreeStore.setState({ load: { status: 'idle' }, dirty: false, mode: 'select', search: '', selection: EMPTY_SELECTION });
});

afterAll(async () => {
  await server.close();
});

describe('tree page', () => {
  it('loads both tree files and draws every node, edge handle and anchor', async () => {
    const { container } = await renderLoadedTree();
    const document = shipTreeStore.getState().document;
    expect(document.nodes.length).toBeGreaterThan(100);
    expect(container.querySelectorAll('[data-curve-a]').length).toBeGreaterThan(0);
    expect(container.querySelectorAll('[data-anchor-id]').length).toBe(document.anchors.length);
    expect(shipTreeStore.getState().dirty).toBe(false);
  });

  it('never gives a new or duplicated node an id that was retired or released', async () => {
    await renderLoadedTree();
    const state = shipTreeStore.getState();
    expect(state.replacedNodeIds.has('modular_hull_socket_2')).toBe(true);
    expect(state.releasedNodeIds.size).toBeGreaterThan(100);

    act(() => addNodeOfType('affinity', 0, 0, vocabulary));
    const addedId = shipTreeStore.getState().selection.nodes[0];
    act(() => duplicateNode(addedId));
    const duplicatedId = shipTreeStore.getState().selection.nodes[0];

    for (const newId of [addedId, duplicatedId]) {
      expect(newId.startsWith('affinity_')).toBe(true);
      expect(isReservedNodeId(shipTreeStore.getState(), newId)).toBe(false);
    }
  });

  it('rings a hovered socket with each subroutine radius and counts the nodes inside', async () => {
    const { container } = await renderLoadedTree();
    const document = shipTreeStore.getState().document;
    const tierOfElement = (element: SVGElement) => {
      const node = nodeById(document, element.dataset.nodeId ?? '');
      return node ? tierOf(typeById(document, node.type)) : null;
    };
    const socketElement = [...nodeElements(container)].find((element) => tierOfElement(element) === 'SOCKET') as SVGElement;
    const smallElement = [...nodeElements(container)].find((element) => tierOfElement(element) === 'SMALL') as SVGElement;
    const radii = JSON.parse(readRepositoryFile(NODE_RADII_FILE)) as Record<string, number>;
    const radiusLayer = () => container.querySelector('[data-testid="socket-radius-layer"]');

    fireEvent.pointerOver(socketElement);
    await waitFor(() => expect(radiusLayer()).toBeTruthy());
    const rings = [...(radiusLayer() as Element).querySelectorAll('circle')].map((ring) => Number(ring.getAttribute('r')));
    expect(rings).toEqual(Object.values(radii));
    expect(radiusLayer()?.textContent).toContain(`small ${radii.small}:`);

    fireEvent.pointerOver(smallElement);
    expect(radiusLayer()).toBeNull();
  });

  it('selects a node by clicking it and edits it through the inspector', async () => {
    const { container } = await renderLoadedTree();
    const target = nodeElements(container)[0];
    const nodeId = target.dataset.nodeId ?? '';
    fireEvent.pointerDown(target, { button: 0, clientX: 0, clientY: 0 });
    window.dispatchEvent(new MouseEvent('pointerup'));
    expect(shipTreeStore.getState().selection.nodes).toEqual([nodeId]);
    expect(await screen.findByText('Connected to')).toBeTruthy();

    commitField('X', '1234');
    expect(nodeById(shipTreeStore.getState().document, nodeId)?.x).toBe(1234);
    expect(shipTreeStore.getState().dirty).toBe(true);

    const typeId = nodeById(shipTreeStore.getState().document, nodeId)?.type ?? '';
    const automatedFlavour = screen.getByLabelText(/^Automated flavour text/);
    fireEvent.change(automatedFlavour, { target: { value: ' The core logs the plating. ' } });
    fireEvent.blur(automatedFlavour);
    expect(typeById(shipTreeStore.getState().document, typeId)?.flavourAutomated).toBe('The core logs the plating.');

    const shown = vi.spyOn(notifications, 'show');
    commitField('Node id', 'renamed_test_node');
    expect(shipTreeStore.getState().selection.nodes).toEqual(['renamed_test_node']);
    expect(nodeById(shipTreeStore.getState().document, nodeId)).toBeUndefined();
    expect(shown).not.toHaveBeenCalled();
    shown.mockRestore();
  });

  it('drags a selected node and pans the empty canvas', async () => {
    const { container } = await renderLoadedTree();
    const target = [...nodeElements(container)].find((element) => {
      const node = nodeById(shipTreeStore.getState().document, element.dataset.nodeId ?? '');
      return node && !node.orbitAnchorId;
    });
    const nodeId = target?.dataset.nodeId ?? '';
    const before = nodeById(shipTreeStore.getState().document, nodeId);
    fireEvent.pointerDown(target as Element, { button: 0, clientX: 0, clientY: 0 });
    act(() => {
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 40, clientY: 25 }));
      window.dispatchEvent(new MouseEvent('pointerup', { clientX: 40, clientY: 25 }));
    });
    const after = nodeById(shipTreeStore.getState().document, nodeId);
    expect(after?.x === before?.x && after?.y === before?.y).toBe(false);

    const svg = nodeElements(container)[0].closest('svg') as SVGSVGElement;
    const panBefore = shipTreeStore.getState().view.panX;
    fireEvent.pointerDown(svg, { button: 0, clientX: 10, clientY: 10 });
    act(() => {
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 70, clientY: 10 }));
      window.dispatchEvent(new MouseEvent('pointerup', { clientX: 70, clientY: 10 }));
    });
    expect(shipTreeStore.getState().view.panX).toBe(panBefore + 60);
    expect(shipTreeStore.getState().selection.nodes).toEqual([]);
  });

  it('shows the in-game tooltip next to a hovered node without needing the mouse to move', async () => {
    const { container } = await renderLoadedTree();
    fireEvent.mouseOver(nodeElements(container)[0], { clientX: 100, clientY: 50 });
    const note = await screen.findByText('The editor has no copy of the tooltip code yet; press Refresh tooltip code.');
    const box = note.parentElement as HTMLElement;
    expect([box.style.left, box.style.top]).toEqual(['116px', '66px']);
  });

  it('describes every type in one request and serves later hovers from that copy', async () => {
    const serverFetch = globalThis.fetch;
    const describeBodies: string[] = [];
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input) !== '/api/describe') {
        return serverFetch(input, init);
      }
      describeBodies.push(String(init?.body));
      const descriptions = Object.fromEntries(
        shipTreeStore.getState().document.types.map((type) => [
          type.id,
          Object.fromEntries(
            ['FRIGATE', 'DESTROYER', 'CRUISER', 'CAPITAL_SHIP'].map((hullSize) => [
              hullSize,
              { title: `Copied ${type.id}`, flavour: null, effects: [`${hullSize} line`], details: [], expandHint: '' },
            ]),
          ),
        ]),
      );
      return new Response(JSON.stringify({ descriptions }), { headers: { 'content-type': 'application/json' } });
    });
    try {
      const { container } = await renderLoadedTree();
      const [firstNode, secondNode] = [...nodeElements(container)];
      const firstType = nodeById(shipTreeStore.getState().document, firstNode.dataset.nodeId ?? '')?.type;
      const secondType = nodeById(shipTreeStore.getState().document, secondNode.dataset.nodeId ?? '')?.type;
      fireEvent.mouseOver(firstNode, { clientX: 100, clientY: 50 });
      expect(await screen.findByText(`Copied ${firstType}`)).toBeTruthy();
      expect(screen.getByText('CAPITAL_SHIP line')).toBeTruthy();
      fireEvent.mouseOver(container, { clientX: 0, clientY: 0 });
      fireEvent.mouseOver(secondNode, { clientX: 100, clientY: 50 });
      expect(await screen.findByText(`Copied ${secondType}`)).toBeTruthy();
      expect(describeBodies).toHaveLength(1);
      expect(JSON.parse(describeBodies[0])).toMatchObject({ hullSizes: ['FRIGATE', 'DESTROYER', 'CRUISER', 'CAPITAL_SHIP'] });
    } finally {
      vi.stubGlobal('fetch', serverFetch);
    }
  });

  it('shows the anchor, decoration and multi-selection inspectors', async () => {
    await renderLoadedTree();
    const document = shipTreeStore.getState().document;
    act(() => selectOnly('anchors', document.anchors[0].id));
    expect(await screen.findByLabelText('Anchor id')).toBeTruthy();
    commitField('Radius', '321');
    expect(shipTreeStore.getState().document.anchors[0].radius).toBe(321);

    const image = decorationsOfKind(document.decorations, 'image')[0];
    act(() => selectOnly('decorations', image.id));
    expect(await screen.findByLabelText('Image id')).toBeTruthy();
    commitField('Spin (deg/sec)', '2');
    fireEvent.click(screen.getByText('Reverse spin'));
    expect(decorationById(shipTreeStore.getState().document, image.id)).toMatchObject({ rotationSpeed: -2 });

    const star = decorationsOfKind(document.decorations, 'star')[0];
    act(() => selectOnly('decorations', star.id));
    expect(await screen.findByText('Star type')).toBeTruthy();

    const belt = decorationsOfKind(document.decorations, 'ringBelt')[0];
    act(() => selectOnly('decorations', belt.id));
    commitField('Outer radius', String(belt.outerRadius + 100));
    expect(decorationById(shipTreeStore.getState().document, belt.id)).toMatchObject({ outerRadius: belt.outerRadius + 100 });

    const serpent = decorationsOfKind(document.decorations, 'serpent')[0];
    act(() => selectOnly('decorations', serpent.id));
    commitField('Seconds per orbit', '120');
    expect(decorationById(shipTreeStore.getState().document, serpent.id)).toMatchObject({ orbitSeconds: 120 });

    const hypershunt = decorationsOfKind(document.decorations, 'hypershunt')[0];
    act(() => selectOnly('decorations', hypershunt.id));
    commitField('Size', '320');
    expect(decorationById(shipTreeStore.getState().document, hypershunt.id)).toMatchObject({ size: 320 });
    fireEvent.click(screen.getByLabelText('Clockwise'));
    expect(decorationById(shipTreeStore.getState().document, hypershunt.id)).toMatchObject({ clockwise: !hypershunt.clockwise });

    act(() => setSelection({ nodes: [document.nodes[0].id, document.nodes[1].id], decorations: [star.id] }));
    expect(await screen.findByText('3 objects selected')).toBeTruthy();
    fireEvent.click(screen.getByText('Clear selection'));
    expect(shipTreeStore.getState().selection.nodes).toEqual([]);
  });

  it('draws every decoration from the tree file with a selectable centre handle', async () => {
    const { container } = await renderLoadedTree();
    const decorations = shipTreeStore.getState().document.decorations;
    expect(new Set(decorations.map((decoration) => decoration.kind))).toEqual(new Set(['star', 'image', 'ringBelt', 'threatUnit', 'serpent', 'shroudedMaw', 'hypershunt']));
    expect(container.querySelectorAll('[data-decoration-id]').length).toBe(decorations.length);
  });

  it('outlines a selected hypershunt and gives it separate radius, start and size handles', async () => {
    const { container } = await renderLoadedTree();
    const hypershunt = decorationsOfKind(shipTreeStore.getState().document.decorations, 'hypershunt')[0];
    const outline = () => container.querySelector('image[href$="coronal_shunt2.png"]')?.parentElement?.querySelector('ellipse');
    const unselectedClass = outline()?.getAttribute('class');
    expect(unselectedClass).toBeTruthy();

    act(() => selectOnly('decorations', hypershunt.id));
    expect(outline()?.getAttribute('class')).not.toBe(unselectedClass);
    const handles = [...container.querySelectorAll<SVGCircleElement>(`[data-decoration-handle-of="${hypershunt.id}"]`)];
    expect(handles.map((handle) => handle.dataset.decorationHandle)).toEqual(['radius', 'start', 'size']);
    const positions = new Set(handles.map((handle) => `${Math.round(Number(handle.getAttribute('cx')))},${Math.round(Number(handle.getAttribute('cy')))}`));
    expect(positions.size).toBe(3);
  });

  it('shrinks handles with the tree when zoomed out but keeps their screen size when zoomed in', async () => {
    const { container } = await renderLoadedTree();
    const decoration = shipTreeStore.getState().document.decorations[0];
    act(() => selectOnly('decorations', decoration.id));
    const screenRadii = (zoom: number) => {
      act(() => shipTreeStore.setState({ view: { panX: 0, panY: 0, zoom } }));
      const centre = container.querySelector(`[data-decoration-id="${decoration.id}"]`) as SVGCircleElement;
      const handle = container.querySelector(`[data-decoration-handle-of="${decoration.id}"]`) as SVGCircleElement;
      const anchor = container.querySelector('[data-anchor-id]') as SVGCircleElement;
      const curve = container.querySelector('[data-curve-a]') as SVGCircleElement;
      return [centre, handle, anchor, curve].map((element) => Number(element.getAttribute('r')) * zoom);
    };
    const atDefaultZoom = screenRadii(1);
    expect(screenRadii(4).map((radius) => radius.toFixed(6))).toEqual(atDefaultZoom.map((radius) => radius.toFixed(6)));
    expect(screenRadii(0.25).map((radius) => radius.toFixed(6))).toEqual(atDefaultZoom.map((radius) => (radius * 0.25).toFixed(6)));
    expect(atDefaultZoom[0]).toBeGreaterThanOrEqual(5);
  });

  it('fits the view around decorations that lie beyond the nodes', () => {
    const farStar = DECORATION_KINDS.star.create('far_star', { x: 0, y: 0 }, { imagePath: null, region: undefined });
    act(() =>
      shipTreeStore.setState({
        canvasSize: { width: 1060, height: 1060 },
        document: { ...EMPTY_DOCUMENT, decorations: [{ ...farStar, x: -9000, radius: 1000 }] },
      }),
    );
    act(() => fitToScreen());
    expect(shipTreeStore.getState().view.zoom).toBeCloseTo(470 / 10000);
  });

  it('adds a threat unit in the decoration mode, edits it and moves it together with a selected node', async () => {
    const { container } = await renderLoadedTree();
    fireEvent.keyDown(window, { key: 'f' });
    expect(shipTreeStore.getState().mode).toBe('addDecoration');
    fireEvent.click(screen.getByText('Threat unit'));
    expect(shipTreeStore.getState().pendingDecorationKind).toBe('threatUnit');
    act(() => shipTreeStore.setState({ view: { panX: 0, panY: 0, zoom: 1 } }));
    const svg = nodeElements(container)[0].closest('svg') as SVGSVGElement;
    fireEvent.pointerDown(svg, { button: 0, clientX: 77123, clientY: 66456 });
    window.dispatchEvent(new MouseEvent('pointerup'));

    const threatUnitId = shipTreeStore.getState().selection.decorations[0];
    expect(threatUnitId).toMatch(/^threat_unit_\d+$/);
    expect(shipTreeStore.getState().mode).toBe('select');
    const added = decorationById(shipTreeStore.getState().document, threatUnitId);
    expect(added).toMatchObject({ x: 77123, y: 66456, unitType: 'fabricator', length: 600, rotation: 0 });
    expect(await screen.findByLabelText('Threat unit id')).toBeTruthy();
    commitField('Length', '480');
    expect(decorationById(shipTreeStore.getState().document, threatUnitId)).toMatchObject({ length: 480 });
    expect(container.querySelector(`image[href$="fabricator.png"]`)).toBeTruthy();

    act(() => setSelection({}));
    fireEvent.pointerDown(svg, { button: 0, shiftKey: true, clientX: 77100, clientY: 66400 });
    act(() => window.dispatchEvent(new MouseEvent('pointerup', { clientX: 77150, clientY: 66500 })));
    expect(shipTreeStore.getState().selection.decorations).toEqual([threatUnitId]);

    const freeNode = shipTreeStore.getState().document.nodes.find((node) => !node.orbitAnchorId);
    const nodeBefore = { ...freeNode! };
    act(() => setSelection({ nodes: [nodeBefore.id], decorations: [threatUnitId] }));
    const handle = container.querySelector(`[data-decoration-id="${threatUnitId}"]`) as SVGElement;
    fireEvent.pointerDown(handle, { button: 0, clientX: 0, clientY: 0 });
    act(() => {
      window.dispatchEvent(new MouseEvent('pointermove', { clientX: 40, clientY: 25 }));
      window.dispatchEvent(new MouseEvent('pointerup', { clientX: 40, clientY: 25 }));
    });
    const moved = shipTreeStore.getState().document;
    expect(decorationById(moved, threatUnitId)).toMatchObject({ x: 77163, y: 66481 });
    expect(nodeById(moved, nodeBefore.id)).toMatchObject({ x: nodeBefore.x + 40, y: nodeBefore.y + 25 });

    act(() => selectOnly('decorations', threatUnitId));
    fireEvent.keyDown(window, { key: 'q' });
    expect(decorationById(shipTreeStore.getState().document, threatUnitId)).toBeUndefined();
    expect(container.querySelector(`[data-decoration-id="${threatUnitId}"]`)).toBeNull();
  });

  it('switches modes and deletes the selection from the keyboard', async () => {
    await renderLoadedTree();
    fireEvent.keyDown(window, { key: 's' });
    expect(shipTreeStore.getState().mode).toBe('add');
    fireEvent.keyDown(window, { key: 'a' });
    expect(shipTreeStore.getState().mode).toBe('select');

    const victim = shipTreeStore.getState().document.nodes[5].id;
    act(() => selectOnly('nodes', victim));
    fireEvent.keyDown(window, { key: 'd', ctrlKey: true });
    const copyId = shipTreeStore.getState().selection.nodes[0];
    expect(copyId).not.toBe(victim);
    expect(nodeById(shipTreeStore.getState().document, copyId)?.connectedTo).toEqual([victim]);
    fireEvent.keyDown(window, { key: 'q' });
    expect(nodeById(shipTreeStore.getState().document, copyId)).toBeUndefined();
    fireEvent.keyDown(window, { key: 'Escape' });
  });

  it('opens the type editor, type picker and add-node dialogs', async () => {
    await renderLoadedTree();
    const document = shipTreeStore.getState().document;
    const typeId = document.types[0].id;

    act(() => openTypeEditor(shipTreeEditor, vocabulary, typeId));
    const editor = await screen.findByRole('dialog');
    expect(within(editor).getByText('Unlock conditions')).toBeTruthy();
    fireEvent.click(within(editor).getByText('Add effect'));
    fireEvent.click(within(editor).getByText('Save type'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(shipTreeStore.getState().document.types[0].effects?.length).toBe((document.types[0].effects?.length ?? 0) + 1);

    const nodeId = document.nodes[2].id;
    act(() => openTypePicker(shipTreeEditor, document.nodes[2].type, (picked) => shipTreeStore.setState({ search: picked })));
    const picker = await screen.findByRole('dialog');
    fireEvent.click(within(picker).getAllByRole('button')[1]);
    await waitFor(() => expect(shipTreeStore.getState().search).not.toBe(''));
    expect(nodeById(shipTreeStore.getState().document, nodeId)).toBeDefined();

    const nodeCount = shipTreeStore.getState().document.nodes.length;
    act(() => openAddNode(shipTreeEditor, vocabulary, 5, 6));
    const adder = await screen.findByRole('dialog');
    fireEvent.click(within(adder).getByText('Add node'));
    await waitFor(() => expect(shipTreeStore.getState().document.nodes.length).toBe(nodeCount + 1));
    act(() => setMode('select'));
  });

  it('offers to restore an unsaved browser draft when the tree loads', async () => {
    const first = await renderLoadedTree();
    const { baseHashes, document } = shipTreeStore.getState();
    const draftDocument = { ...document, nodes: document.nodes.map((node, index) => (index === 0 ? { ...node, x: 999999 } : node)) };
    localStorage.setItem('exiledSectorEditor.treeDraft', JSON.stringify({ basedOn: baseHashes, savedAt: 1, data: draftDocument }));
    first.unmount();
    shipTreeStore.setState({ load: { status: 'idle' } });
    renderWithProviders(<TreePage />);
    fireEvent.click(await screen.findByText('Restore draft', {}, { timeout: 15000 }));
    await waitFor(() => expect(shipTreeStore.getState().load.status).toBe('ready'));
    expect(shipTreeStore.getState().document.nodes[0].x).toBe(999999);
    expect(shipTreeStore.getState().dirty).toBe(true);
    localStorage.clear();
  });

  it('saves byte-identical files and then writes edits through the server', async () => {
    await renderLoadedTree();
    expect(shipTreeStore.getState().dirty).toBe(false);
    await act(async () => saveTree(vocabulary));
    expect(server.readProjectFile(TYPES_FILE)).toBe(readRepositoryFile(TYPES_FILE));
    expect(server.readProjectFile(TREE_FILE)).toBe(readRepositoryFile(TREE_FILE));

    const document = shipTreeStore.getState().document;
    act(() => selectOnly('nodes', document.nodes[0].id));
    commitField('X', '4321');
    await act(async () => saveTree(vocabulary));
    expect(shipTreeStore.getState().dirty).toBe(false);
    expect(JSON.parse(server.readProjectFile(TREE_FILE)).nodes[0].x).toBe(4321);
  });

  it('offers to reload when the tree files changed on disk since they loaded', async () => {
    await renderLoadedTree();
    const treePath = path.join(server.projectRoot, TREE_FILE);
    const diskText = server.readProjectFile(TREE_FILE);
    try {
      fs.writeFileSync(treePath, `${diskText}\n`);
      const document = shipTreeStore.getState().document;
      act(() => selectOnly('nodes', document.nodes[0].id));
      commitField('X', '1234');
      let saving: Promise<void> = Promise.resolve();
      act(() => {
        saving = saveTree(vocabulary);
      });
      const conflict = await screen.findByRole('dialog');
      expect(within(conflict).getByText('Files changed on disk')).toBeTruthy();
      fireEvent.click(within(conflict).getByRole('button', { name: 'Reload from disk' }));
      await act(async () => saving);
      await waitFor(() => expect(shipTreeStore.getState().load.status).toBe('ready'));
      expect(shipTreeStore.getState().dirty).toBe(false);
      expect(shipTreeStore.getState().document.nodes[0].x).toBe(JSON.parse(diskText).nodes[0].x);
      expect(server.readProjectFile(TREE_FILE)).toBe(`${diskText}\n`);
    } finally {
      fs.writeFileSync(treePath, diskText);
    }
  });

  it('keeps the reloaded hashes when a reload from disk finishes while a save is still in flight', async () => {
    await renderLoadedTree();
    const treePath = path.join(server.projectRoot, TREE_FILE);
    const typesPath = path.join(server.projectRoot, TYPES_FILE);
    const diskTree = server.readProjectFile(TREE_FILE);
    const diskTypes = server.readProjectFile(TYPES_FILE);
    const serverFetch = globalThis.fetch;
    let releaseSave: () => void = () => undefined;
    const saveReleased = new Promise<void>((resolve) => {
      releaseSave = resolve;
    });
    let saveRequested = false;
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input) === '/api/files' && init?.method === 'POST') {
        saveRequested = true;
        await saveReleased;
      }
      return serverFetch(input, init);
    });
    try {
      const document = shipTreeStore.getState().document;
      act(() => selectOnly('nodes', document.nodes[0].id));
      commitField('X', '2468');
      let saving: Promise<void> = Promise.resolve();
      act(() => {
        saving = saveTree(vocabulary);
      });
      await waitFor(() => expect(saveRequested).toBe(true));
      expect((screen.getByRole('button', { name: 'Reload from disk' }) as HTMLButtonElement).disabled).toBe(true);
      localStorage.clear();
      await act(async () => loadTree());
      const reloadedHashes = shipTreeStore.getState().baseHashes;
      releaseSave();
      await act(async () => saving);
      expect(JSON.parse(server.readProjectFile(TREE_FILE)).nodes[0].x).toBe(2468);
      expect(shipTreeStore.getState().baseHashes).toEqual(reloadedHashes);
    } finally {
      vi.stubGlobal('fetch', serverFetch);
      fs.writeFileSync(treePath, diskTree);
      fs.writeFileSync(typesPath, diskTypes);
    }
  });

  it('shows the load error with a retry that loads the tree once the server answers', async () => {
    const serverFetch = globalThis.fetch;
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) =>
      String(input).startsWith('/api/files?') ? new Response(JSON.stringify({ message: 'disk unplugged' }), { status: 500 }) : serverFetch(input, init),
    );
    try {
      renderWithProviders(<TreePage />);
      expect(await screen.findByText(/Could not load the skill tree files \(disk unplugged\)/, undefined, { timeout: 15000 })).toBeTruthy();
    } finally {
      vi.stubGlobal('fetch', serverFetch);
    }
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    await waitFor(() => expect(shipTreeStore.getState().load.status).toBe('ready'), { timeout: 15000 });
    expect(screen.queryByText('Could not load')).toBeNull();
  });
});
