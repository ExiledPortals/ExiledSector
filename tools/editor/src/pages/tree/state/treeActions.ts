import { produce, type Draft } from 'immer';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { notifyInfo } from '../../../shared/dialogs.tsx';
import { connectNodes, deleteNodes, isLinkedPair, unlinkNodes } from '../model/connectors.ts';
import { deleteAnchor, detachNodeFromAnchor, syncRingArcCurves } from '../model/geometry.ts';
import { isVolumeTag } from '../model/tags.ts';
import { decorationById, decorationReach, decorationSpec, type DecorationKind } from '../model/decorations.ts';
import { ANCHOR_RECTANGLE_ASPECT, anchorById, nodeById, nodeIdStem, uniqueId, type OrbitAnchor, type TreeDocument, type TreeNode } from '../model/types.ts';
import { addVolume, removeVolume, renameVolume } from '../model/volumes.ts';
import {
  EMPTY_SELECTION,
  isReservedNodeId,
  MAX_ZOOM,
  MIN_ZOOM,
  selectionCount,
  type EditorMode,
  type Selection,
  type SelectionKind,
  type TreeStore,
} from './treeStore.ts';

export type DocumentRecipe = (document: Draft<TreeDocument>) => void;

export function uniqueNodeId(document: TreeDocument, typeId: string, isReserved: (id: string) => boolean = () => false): string {
  return uniqueId(nodeIdStem(document, typeId), (id) => Boolean(nodeById(document, id)) || isReserved(id));
}

export function clampZoom(zoom: number): number {
  return Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom));
}

function nearestRegionTag(document: TreeDocument, vocabulary: Vocabulary, x: number, y: number): string | undefined {
  let best = vocabulary.volumeTags[0];
  let bestDistance = Infinity;
  for (const node of document.nodes) {
    const regions = (node.tags ?? []).filter((tag) => isVolumeTag(vocabulary, tag));
    if (regions.length !== 1) {
      continue;
    }
    const distance = (node.x - x) * (node.x - x) + (node.y - y) * (node.y - y);
    if (distance < bestDistance) {
      bestDistance = distance;
      best = regions[0];
    }
  }
  return best;
}

export function createTreeActions(store: TreeStore) {
  function editDocument(recipe: DocumentRecipe): void {
    const state = store.getState();
    const next = produce(state.document, recipe);
    if (next !== state.document) {
      store.setState({ document: next, dirty: true, revision: state.revision + 1 });
    }
  }

  function setSelection(selection: Partial<Selection>): void {
    store.setState({ selection: { ...EMPTY_SELECTION, ...selection } });
  }

  function setMode(mode: EditorMode): void {
    store.setState({ mode });
  }

  function clearSelection(): void {
    if (selectionCount(store.getState().selection) > 0) {
      setSelection(EMPTY_SELECTION);
    }
  }

  function selectOnly(kind: SelectionKind, id: string): void {
    setSelection({ [kind]: [id] });
  }

  function toggleSelected(kind: SelectionKind, id: string): void {
    const selection = store.getState().selection;
    const current = selection[kind];
    const next = current.includes(id) ? current.filter((selectedId) => selectedId !== id) : [...current, id];
    store.setState({ selection: { ...selection, [kind]: next } });
  }

  function reservedInStore(id: string): boolean {
    return isReservedNodeId(store.getState(), id);
  }

  function addNodeOfType(typeId: string, x: number, y: number, vocabulary: Vocabulary | undefined): void {
    const document = store.getState().document;
    const region = vocabulary ? nearestRegionTag(document, vocabulary, x, y) : undefined;
    const node: TreeNode = { id: uniqueNodeId(document, typeId, reservedInStore), type: typeId, connectedTo: [], x, y, tags: region ? [region] : [] };
    editDocument((draft) => {
      draft.nodes.push(node);
    });
    setMode('select');
    selectOnly('nodes', node.id);
  }

  function addAnchorAt(x: number, y: number): void {
    const { document, pendingAnchorShape } = store.getState();
    const anchor: OrbitAnchor = { id: uniqueId('anchor', (id) => Boolean(anchorById(document, id))), x, y, radius: 150 };
    if (pendingAnchorShape !== 'circle') {
      anchor.shape = pendingAnchorShape;
      if (pendingAnchorShape === 'rectangle') {
        anchor.aspectRatio = ANCHOR_RECTANGLE_ASPECT;
      }
    }
    editDocument((draft) => {
      draft.anchors.push(anchor);
    });
    selectOnly('anchors', anchor.id);
    setMode('select');
  }

  function addDecorationAt(kind: DecorationKind, x: number, y: number, vocabulary: Vocabulary | undefined, defaultImagePath: string | null): void {
    const document = store.getState().document;
    const spec = decorationSpec(kind);
    const id = uniqueId(spec.idPrefix, (taken) => Boolean(decorationById(document, taken)));
    const region = vocabulary ? nearestRegionTag(document, vocabulary, x, y) : undefined;
    editDocument((draft) => {
      draft.decorations.push(spec.create(id, { x, y }, { imagePath: defaultImagePath, region }));
    });
    selectOnly('decorations', id);
    setMode('select');
  }

  function duplicateNode(nodeId: string): void {
    const document = store.getState().document;
    const original = nodeById(document, nodeId);
    if (!original) {
      return;
    }
    const copy: TreeNode = structuredClone(original);
    copy.id = uniqueNodeId(document, original.type, reservedInStore);
    copy.x = original.x + 40;
    copy.y = original.y - 40;
    copy.connectedTo = [original.id];
    detachNodeFromAnchor(copy);
    editDocument((draft) => {
      nodeById(draft, nodeId)?.connectedTo.push(copy.id);
      draft.nodes.push(copy);
    });
    selectOnly('nodes', copy.id);
  }

  function deleteSelection(): void {
    const { selection } = store.getState();
    if (selectionCount(selection) === 0) {
      return;
    }
    editDocument((draft) => {
      if (selection.nodes.length > 0) {
        deleteNodes(draft, selection.nodes);
      }
      for (const anchorId of selection.anchors) {
        deleteAnchor(draft, anchorId);
      }
      draft.decorations = draft.decorations.filter((decoration) => !selection.decorations.includes(decoration.id));
    });
    setSelection(EMPTY_SELECTION);
  }

  function toggleLink(firstId: string, secondId: string): void {
    const document = store.getState().document;
    const first = nodeById(document, firstId);
    const second = nodeById(document, secondId);
    if (!first || !second) {
      return;
    }
    const linked = isLinkedPair(document, firstId, secondId);
    editDocument((draft) => {
      const draftFirst = nodeById(draft, firstId);
      if (!draftFirst) {
        return;
      }
      if (linked) {
        unlinkNodes(draft, draftFirst, secondId);
        return;
      }
      connectNodes(draft, draftFirst, secondId);
      if (first.orbitAnchorId && first.orbitAnchorId === second.orbitAnchorId) {
        const sharedAnchor = anchorById(draft, first.orbitAnchorId);
        if (sharedAnchor) {
          syncRingArcCurves(draft, sharedAnchor);
        }
      }
    });
    notifyInfo(linked ? `Unlinked ${firstId} from ${secondId}` : `${firstId} <-> ${secondId} connected`);
  }

  function addDeclaredVolume(volumeId: string): void {
    editDocument((draft) => addVolume(draft, volumeId));
  }

  function renameDeclaredVolume(oldVolumeId: string, newVolumeId: string): void {
    editDocument((draft) => renameVolume(draft, oldVolumeId, newVolumeId));
  }

  function removeDeclaredVolume(volumeId: string): void {
    editDocument((draft) => removeVolume(draft, volumeId));
  }

  function zoomBy(factor: number): void {
    store.setState((state) => ({ view: { ...state.view, zoom: clampZoom(state.view.zoom * factor) } }));
  }

  function zoomAtPoint(factor: number, offsetX: number, offsetY: number): void {
    store.setState(({ view, canvasSize }) => {
      const pointerX = offsetX - canvasSize.width / 2 - view.panX;
      const pointerY = offsetY - canvasSize.height / 2 - view.panY;
      const zoom = clampZoom(view.zoom * factor);
      const scaleChange = zoom / view.zoom;
      return { view: { panX: view.panX - pointerX * (scaleChange - 1), panY: view.panY - pointerY * (scaleChange - 1), zoom } };
    });
  }

  function centerOnOrigin(): void {
    store.setState((state) => ({ view: { ...state.view, panX: 0, panY: 0 } }));
  }

  function fitToScreen(): void {
    const { document, canvasSize } = store.getState();
    if (document.nodes.length === 0 && document.decorations.length === 0) {
      return;
    }
    let maxExtent = 100;
    for (const node of document.nodes) {
      maxExtent = Math.max(maxExtent, Math.abs(node.x), Math.abs(node.y));
    }
    for (const decoration of document.decorations) {
      maxExtent = Math.max(maxExtent, Math.max(Math.abs(decoration.x), Math.abs(decoration.y)) + decorationReach(decoration));
    }
    const available = Math.min(canvasSize.width, canvasSize.height) / 2 - 60;
    store.setState({ view: { panX: 0, panY: 0, zoom: Math.max(MIN_ZOOM, Math.min(1.5, available / maxExtent)) } });
  }

  return {
    editDocument,
    setSelection,
    setMode,
    clearSelection,
    selectOnly,
    toggleSelected,
    addNodeOfType,
    addAnchorAt,
    addDecorationAt,
    duplicateNode,
    deleteSelection,
    toggleLink,
    addDeclaredVolume,
    renameDeclaredVolume,
    removeDeclaredVolume,
    zoomBy,
    zoomAtPoint,
    centerOnOrigin,
    fitToScreen,
  };
}

export type TreeActions = ReturnType<typeof createTreeActions>;
