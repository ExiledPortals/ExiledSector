import { produce, type Draft } from 'immer';
import { create } from 'zustand';
import { registerUnloadCheck } from '../../../shared/unsavedChanges.ts';
import type { DecorationKind } from '../model/decorations.ts';
import type { TreeLineEndings } from '../model/serialization.ts';
import type { AnchorShape, TreeDocument } from '../model/types.ts';

export type EditorMode = 'select' | 'add' | 'addAnchor' | 'addDecoration';

export type SelectionKind = 'nodes' | 'anchors' | 'decorations';

export type Selection = Record<SelectionKind, string[]>;

export type HandleKind = 'anchorResize' | 'anchorRotate';

export type ActiveDrag =
  | { kind: 'move'; anchorIds: string[]; decorationIds: string[] }
  | { kind: 'decorationHandle'; id: string; handle: string }
  | { kind: 'curve'; key: string }
  | { kind: HandleKind; id: string }
  | { kind: 'marquee'; startX: number; startY: number; currentX: number; currentY: number };

export interface TreeView {
  panX: number;
  panY: number;
  zoom: number;
}

export type TreeLoadState = { status: 'idle' } | { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready' };

export interface TreeStoreState {
  load: TreeLoadState;
  document: TreeDocument;
  baseHashes: Record<string, string>;
  lineEndings: TreeLineEndings;
  dirty: boolean;
  revision: number;
  saving: boolean;
  view: TreeView;
  canvasSize: { width: number; height: number };
  mode: EditorMode;
  pendingAnchorShape: AnchorShape;
  pendingDecorationKind: DecorationKind;
  selection: Selection;
  showLabels: boolean;
  search: string;
  activeDrag: ActiveDrag | null;
  fitRequested: boolean;
  replacedNodeIds: ReadonlySet<string>;
  releasedNodeIds: ReadonlySet<string>;
}

export const EMPTY_SELECTION: Selection = { nodes: [], anchors: [], decorations: [] };

export const EMPTY_DOCUMENT: TreeDocument = { types: [], nodes: [], curves: [], hiddenConnectors: [], anchors: [], decorations: [] };

export const MIN_ZOOM = 0.08;
export const MAX_ZOOM = 3;

export const useTreeStore = create<TreeStoreState>(() => ({
  load: { status: 'idle' },
  document: EMPTY_DOCUMENT,
  baseHashes: {},
  lineEndings: { types: '\n', tree: '\n' },
  dirty: false,
  revision: 0,
  saving: false,
  view: { panX: 0, panY: 0, zoom: 0.55 },
  canvasSize: { width: 800, height: 600 },
  mode: 'select',
  pendingAnchorShape: 'circle',
  pendingDecorationKind: 'star',
  selection: EMPTY_SELECTION,
  showLabels: true,
  search: '',
  activeDrag: null,
  fitRequested: false,
  replacedNodeIds: new Set<string>(),
  releasedNodeIds: new Set<string>(),
}));

export function isReservedNodeId(state: Pick<TreeStoreState, 'replacedNodeIds' | 'releasedNodeIds'>, nodeId: string): boolean {
  return state.replacedNodeIds.has(nodeId) || state.releasedNodeIds.has(nodeId);
}

registerUnloadCheck(() => useTreeStore.getState().dirty);

export function editDocument(recipe: (document: Draft<TreeDocument>) => void): void {
  const state = useTreeStore.getState();
  const next = produce(state.document, recipe);
  if (next !== state.document) {
    useTreeStore.setState({ document: next, dirty: true, revision: state.revision + 1 });
  }
}

export function setSelection(selection: Partial<Selection>): void {
  useTreeStore.setState({ selection: { ...EMPTY_SELECTION, ...selection } });
}

export function selectionCount(selection: Selection): number {
  return selection.nodes.length + selection.anchors.length + selection.decorations.length;
}
