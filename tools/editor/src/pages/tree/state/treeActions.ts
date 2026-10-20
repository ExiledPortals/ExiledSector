import type { Vocabulary } from '../../../api/vocabulary.ts';
import { notifyInfo } from '../../../shared/dialogs.tsx';
import { connectNodes, deleteNodes, isLinkedPair, unlinkNodes } from '../model/connectors.ts';
import { deleteAnchor, detachNodeFromAnchor, syncRingArcCurves } from '../model/geometry.ts';
import { isRegionTag } from '../model/tags.ts';
import {
  ANCHOR_RECTANGLE_ASPECT,
  anchorById,
  DEFAULT_THREAT_UNIT_LENGTH,
  imageById,
  nodeById,
  starById,
  threatUnitById,
  uniqueId,
  type OrbitAnchor,
  type TreeDocument,
  type TreeNode,
} from '../model/types.ts';
import {
  editDocument,
  EMPTY_SELECTION,
  isReservedNodeId,
  MAX_ZOOM,
  MIN_ZOOM,
  selectionCount,
  setSelection,
  useTreeStore,
  type EditorMode,
  type SelectionKind,
} from './treeStore.ts';

export function setMode(mode: EditorMode): void {
  useTreeStore.setState({ mode });
}

export function clearSelection(): void {
  if (selectionCount(useTreeStore.getState().selection) > 0) {
    setSelection(EMPTY_SELECTION);
  }
}

export function selectOnly(kind: SelectionKind, id: string): void {
  setSelection({ [kind]: [id] });
}

export function toggleSelected(kind: SelectionKind, id: string): void {
  const selection = useTreeStore.getState().selection;
  const current = selection[kind];
  const next = current.includes(id) ? current.filter((selectedId) => selectedId !== id) : [...current, id];
  useTreeStore.setState({ selection: { ...selection, [kind]: next } });
}

export function uniqueNodeId(document: TreeDocument, typeId: string, isReserved: (id: string) => boolean = () => false): string {
  return uniqueId(typeId, (id) => Boolean(nodeById(document, id)) || isReserved(id));
}

function reservedInStore(id: string): boolean {
  return isReservedNodeId(useTreeStore.getState(), id);
}

function nearestRegionTag(document: TreeDocument, vocabulary: Vocabulary, x: number, y: number): string | undefined {
  let best = vocabulary.regionTags[0];
  let bestDistance = Infinity;
  for (const node of document.nodes) {
    const regions = (node.tags ?? []).filter((tag) => isRegionTag(vocabulary, tag));
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

export function addNodeOfType(typeId: string, x: number, y: number, vocabulary: Vocabulary | undefined): void {
  const document = useTreeStore.getState().document;
  const region = vocabulary ? nearestRegionTag(document, vocabulary, x, y) : undefined;
  const node: TreeNode = { id: uniqueNodeId(document, typeId, reservedInStore), type: typeId, connectedTo: [], x, y, tags: region ? [region] : [] };
  editDocument((draft) => {
    draft.nodes.push(node);
  });
  setMode('select');
  selectOnly('nodes', node.id);
}

export function addAnchorAt(x: number, y: number): void {
  const { document, pendingAnchorShape } = useTreeStore.getState();
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

export function addImageAt(x: number, y: number, defaultImagePath: string | null): void {
  const document = useTreeStore.getState().document;
  const id = uniqueId('image', (taken) => Boolean(imageById(document, taken)));
  editDocument((draft) => {
    draft.images.push({ id, x, y, width: 800, height: 800, imagePath: defaultImagePath, rotation: 0, rotationSpeed: 0 });
  });
  selectOnly('images', id);
  setMode('select');
}

export function addStarAt(x: number, y: number): void {
  const document = useTreeStore.getState().document;
  const id = uniqueId('star', (taken) => Boolean(starById(document, taken)));
  editDocument((draft) => {
    draft.stars.push({ id, x, y, radius: 850, starType: 'star_yellow', color: null });
  });
  selectOnly('stars', id);
  setMode('select');
}

export function addThreatUnitAt(x: number, y: number, vocabulary?: Vocabulary): void {
  const document = useTreeStore.getState().document;
  const id = uniqueId('threat_unit', (taken) => Boolean(threatUnitById(document, taken)));
  const region = vocabulary ? nearestRegionTag(document, vocabulary, x, y) : undefined;
  editDocument((draft) => {
    draft.threatUnits.push({ id, x, y, unitType: 'fabricator', length: DEFAULT_THREAT_UNIT_LENGTH, rotation: 0, tags: region ? [region] : undefined });
  });
  selectOnly('threatUnits', id);
  setMode('select');
}

export function duplicateNode(nodeId: string): void {
  const document = useTreeStore.getState().document;
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

export function deleteSelection(): void {
  const { selection } = useTreeStore.getState();
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
    draft.images = draft.images.filter((image) => !selection.images.includes(image.id));
    draft.stars = draft.stars.filter((star) => !selection.stars.includes(star.id));
    draft.threatUnits = draft.threatUnits.filter((threatUnit) => !selection.threatUnits.includes(threatUnit.id));
  });
  setSelection(EMPTY_SELECTION);
}

export function toggleLink(firstId: string, secondId: string): void {
  const document = useTreeStore.getState().document;
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

export function clampZoom(zoom: number): number {
  return Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom));
}

export function zoomBy(factor: number): void {
  useTreeStore.setState((state) => ({ view: { ...state.view, zoom: clampZoom(state.view.zoom * factor) } }));
}

export function zoomAtPoint(factor: number, offsetX: number, offsetY: number): void {
  useTreeStore.setState(({ view, canvasSize }) => {
    const pointerX = offsetX - canvasSize.width / 2 - view.panX;
    const pointerY = offsetY - canvasSize.height / 2 - view.panY;
    const zoom = clampZoom(view.zoom * factor);
    const scaleChange = zoom / view.zoom;
    return { view: { panX: view.panX - pointerX * (scaleChange - 1), panY: view.panY - pointerY * (scaleChange - 1), zoom } };
  });
}

export function centerOnOrigin(): void {
  useTreeStore.setState((state) => ({ view: { ...state.view, panX: 0, panY: 0 } }));
}

export function fitToScreen(): void {
  const { document, canvasSize } = useTreeStore.getState();
  if (document.nodes.length === 0) {
    return;
  }
  let maxExtent = 100;
  for (const node of document.nodes) {
    maxExtent = Math.max(maxExtent, Math.abs(node.x), Math.abs(node.y));
  }
  const available = Math.min(canvasSize.width, canvasSize.height) / 2 - 60;
  useTreeStore.setState({ view: { panX: 0, panY: 0, zoom: Math.max(MIN_ZOOM, Math.min(1.5, available / maxExtent)) } });
}
