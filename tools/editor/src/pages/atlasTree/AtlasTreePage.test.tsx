import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { openAddNode, openTypeEditor } from '../tree/dialogs/treeDialogs.tsx';
import { decorationById } from '../tree/model/decorations.ts';
import { nodeById, typeById } from '../tree/model/types.ts';
import { ATLAS_TIERS, ATLAS_TREE_KIND } from '../tree/treeKinds.ts';
import { atlasTreeEditor, shipTreeEditor } from '../tree/state/treeEditor.ts';
import { EMPTY_SELECTION } from '../tree/state/treeStore.ts';
import { AtlasTreePage } from './AtlasTreePage.tsx';

const ATLAS_TYPES_FILE = 'data/skilltrees/atlas_tree_types.json';
const ATLAS_TREE_FILE = 'data/skilltrees/atlas_tree.json';
const VOCABULARY_FILE = 'tools/editor_vocabulary.json';
const NODE_RADII_FILE = 'data/config/exiledSector/node_radii.json';
const sharedVocabulary = buildVocabulary(JSON.parse(readRepositoryFile(VOCABULARY_FILE)));
const atlasStore = atlasTreeEditor.store;
const { editDocument, selectOnly, setMode, addNodeOfType } = atlasTreeEditor.actions;

let server: TestServer;

function atlasVocabulary() {
  return ATLAS_TREE_KIND.editorVocabulary(sharedVocabulary, atlasStore.getState().document.volumes);
}

function nodeElements(container: HTMLElement): NodeListOf<SVGElement> {
  return container.querySelectorAll<SVGElement>('[data-node-id]');
}

async function renderLoadedAtlas() {
  const view = renderWithProviders(<AtlasTreePage />);
  await waitFor(() => expect(atlasStore.getState().load.status).toBe('ready'), { timeout: 15000 });
  await waitFor(() => expect(nodeElements(view.container).length).toBe(atlasStore.getState().document.nodes.length));
  return view;
}

function commitField(label: string, value: string, container: HTMLElement = document.body) {
  const input = within(container).getByLabelText(label);
  fireEvent.change(input, { target: { value } });
  fireEvent.blur(input);
}

async function openVolumes(): Promise<HTMLElement> {
  fireEvent.click(screen.getByRole('button', { name: 'Volumes' }));
  return screen.findByRole('dialog');
}

function addVolumeThroughDialog(dialog: HTMLElement, volumeId: string) {
  fireEvent.change(within(dialog).getByLabelText('New volume id'), { target: { value: volumeId } });
  fireEvent.click(within(dialog).getByRole('button', { name: 'Add volume' }));
}

beforeAll(() => {
  server = startTestServer([ATLAS_TYPES_FILE, ATLAS_TREE_FILE, VOCABULARY_FILE, NODE_RADII_FILE]);
});

beforeEach(() => {
  localStorage.clear();
  atlasStore.setState({ load: { status: 'idle' }, dirty: false, mode: 'select', search: '', selection: EMPTY_SELECTION });
});

afterAll(async () => {
  await server.close();
});

describe('atlas tree page', () => {
  it('loads the atlas files apart from the ship tree and draws the root', async () => {
    const { container } = await renderLoadedAtlas();
    const document = atlasStore.getState().document;
    expect(document.volumes).toEqual([]);
    expect(nodeById(document, 'atlas_root')).toBeDefined();
    expect(container.querySelector('[data-node-id="atlas_root"]')).toBeTruthy();
    expect(shipTreeEditor.store.getState().load.status).toBe('idle');
    expect(screen.queryByText('All sizes')).toBeNull();
    expect(screen.getByText('1 issue - click to view')).toBeTruthy();
  });

  it('adds, renames and removes volumes, renaming the tags that use them', async () => {
    await renderLoadedAtlas();
    const dialog = await openVolumes();
    addVolumeThroughDialog(dialog, 'crafting');
    addVolumeThroughDialog(dialog, 'charts');
    addVolumeThroughDialog(dialog, 'charts');
    expect(atlasStore.getState().document.volumes).toEqual(['crafting', 'charts']);

    act(() =>
      editDocument((draft) => {
        draft.nodes[0].tags = ['crafting'];
      }),
    );
    expect(await within(dialog).findByText('1 use')).toBeTruthy();
    expect((within(dialog).getByLabelText('Remove volume crafting') as HTMLButtonElement).disabled).toBe(true);
    await waitFor(() => expect(screen.queryByText(/issue - click to view/)).toBeNull());

    commitField('Volume crafting', 'crafting_items', dialog);
    const renamed = atlasStore.getState().document;
    expect(renamed.volumes).toEqual(['crafting_items', 'charts']);
    expect(nodeById(renamed, 'atlas_root')?.tags).toEqual(['crafting_items']);

    fireEvent.click(within(dialog).getByLabelText('Remove volume charts'));
    expect(atlasStore.getState().document.volumes).toEqual(['crafting_items']);
  });

  it('offers chart effects, atlas tiers and no ship-only sections in the type editor', async () => {
    await renderLoadedAtlas();
    act(() => openTypeEditor(atlasTreeEditor, atlasVocabulary(), 'atlas_root'));
    const editor = await screen.findByRole('dialog');
    for (const shipOnly of ['Unlock conditions', 'Hull-size effects', 'Item cost', 'Optional slot options', 'Required hull sizes (none = any)']) {
      expect(within(editor).queryByText(shipOnly)).toBeNull();
    }
    expect(within(editor).queryByLabelText(/^Can be integrated/)).toBeNull();
    expect(within(editor).queryByLabelText(/^Automated flavour text/)).toBeNull();
    expect(within(editor).queryByLabelText('vanillaHullMod id')).toBeNull();
    expect(within(editor).getByLabelText(/^Flavour text/)).toBeTruthy();

    fireEvent.click(within(editor).getByText('Add effect'));
    fireEvent.click(within(editor).getByText('Save type'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    const effects = typeById(atlasStore.getState().document, 'atlas_root')?.effects ?? [];
    expect(effects).toHaveLength(1);
    expect(sharedVocabulary.chartEffectNames).toContain(effects[0].effect);
    expect(atlasVocabulary().tiers).toEqual(ATLAS_TIERS);
  });

  it('shows the atlas sections in the node inspector and keeps the node on its declared volume', async () => {
    const { container } = await renderLoadedAtlas();
    act(() => editDocument((draft) => void draft.volumes.push('crafting')));
    fireEvent.pointerDown(container.querySelector('[data-node-id="atlas_root"]') as Element, { button: 0, clientX: 0, clientY: 0 });
    window.dispatchEvent(new MouseEvent('pointerup'));
    expect(await screen.findByText('Connected to')).toBeTruthy();
    expect(screen.getByLabelText(/^Flavour text/)).toBeTruthy();
    expect(screen.queryByLabelText(/^Automated flavour text/)).toBeNull();
    expect(screen.queryByText(/^Item cost/)).toBeNull();
    expect(screen.queryByText(/^Type tags/)).toBeNull();
    expect(screen.getByPlaceholderText('Volume: none')).toBeTruthy();

    commitField('X', '250');
    expect(nodeById(atlasStore.getState().document, 'atlas_root')?.x).toBe(250);
  });

  it('places nodes, anchors and decorations on the nearest volume', async () => {
    const { container } = await renderLoadedAtlas();
    act(() =>
      editDocument((draft) => {
        draft.volumes.push('crafting');
        draft.nodes[0].tags = ['crafting'];
      }),
    );
    act(() => addNodeOfType('atlas_root', 300, 0, atlasVocabulary()));
    const addedId = atlasStore.getState().selection.nodes[0];
    expect(nodeById(atlasStore.getState().document, addedId)?.tags).toEqual(['crafting']);

    act(() => atlasStore.setState({ view: { panX: 0, panY: 0, zoom: 1 } }));
    const svg = nodeElements(container)[0].closest('svg') as SVGSVGElement;
    fireEvent.keyDown(window, { key: 'd' });
    fireEvent.pointerDown(svg, { button: 0, clientX: 500, clientY: 500 });
    window.dispatchEvent(new MouseEvent('pointerup'));
    expect(atlasStore.getState().document.anchors).toHaveLength(1);

    fireEvent.keyDown(window, { key: 'f' });
    fireEvent.pointerDown(svg, { button: 0, clientX: 800, clientY: 300 });
    window.dispatchEvent(new MouseEvent('pointerup'));
    const starId = atlasStore.getState().selection.decorations[0];
    expect(decorationById(atlasStore.getState().document, starId)).toMatchObject({ kind: 'star', x: 800, y: 300, tags: ['crafting'] });
    expect(await screen.findByLabelText('Star id')).toBeTruthy();

    const nodeCount = atlasStore.getState().document.nodes.length;
    act(() => openAddNode(atlasTreeEditor, atlasVocabulary(), 5, 6));
    const adder = await screen.findByRole('dialog');
    fireEvent.click(within(adder).getByText('Add node'));
    await waitFor(() => expect(atlasStore.getState().document.nodes.length).toBe(nodeCount + 1));
    act(() => setMode('select'));
  });

  it('keeps its unsaved changes under its own draft key', async () => {
    await renderLoadedAtlas();
    act(() => selectOnly('nodes', 'atlas_root'));
    commitField('Y', '77');
    await waitFor(() => expect(localStorage.getItem(ATLAS_TREE_KIND.draftStorageKey)).not.toBeNull());
    expect(localStorage.getItem('exiledSectorEditor.treeDraft')).toBeNull();
  });

  it('saves both atlas files through the server with the declared volumes', async () => {
    await renderLoadedAtlas();
    act(() =>
      editDocument((draft) => {
        draft.volumes.push('star_charts');
        draft.nodes[0].tags = ['star_charts'];
      }),
    );
    await act(async () => atlasTreeEditor.persistence.saveTree(atlasVocabulary()));
    expect(atlasStore.getState().dirty).toBe(false);
    const savedTree = JSON.parse(server.readProjectFile(ATLAS_TREE_FILE)) as { volumes: string[]; nodes: { id: string; tags?: string[] }[] };
    expect(savedTree.volumes).toEqual(['star_charts']);
    expect(savedTree.nodes[0]).toMatchObject({ id: 'atlas_root', tags: ['star_charts'] });
    expect(JSON.parse(server.readProjectFile(ATLAS_TYPES_FILE))).toEqual(JSON.parse(readRepositoryFile(ATLAS_TYPES_FILE)));
  });
});
