import { ATLAS_TREE_KIND, SHIP_TREE_KIND, type TreeKind } from '../treeKinds.ts';
import { createTreeActions, type TreeActions } from './treeActions.ts';
import { createTreePersistence, type TreePersistence } from './treePersistence.ts';
import { createTreeStore, type TreeStore } from './treeStore.ts';

export interface TreeEditor {
  kind: TreeKind;
  store: TreeStore;
  actions: TreeActions;
  persistence: TreePersistence;
}

export function createTreeEditor(kind: TreeKind): TreeEditor {
  const store = createTreeStore();
  return { kind, store, actions: createTreeActions(store), persistence: createTreePersistence(kind, store) };
}

export const shipTreeEditor = createTreeEditor(SHIP_TREE_KIND);
export const atlasTreeEditor = createTreeEditor(ATLAS_TREE_KIND);
