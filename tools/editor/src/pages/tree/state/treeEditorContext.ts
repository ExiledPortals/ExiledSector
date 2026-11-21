import { createContext, useContext, useMemo } from 'react';
import { useVocabulary, type Vocabulary } from '../../../api/vocabulary.ts';
import type { TreeEditor } from './treeEditor.ts';
import type { TreeStoreState } from './treeStore.ts';

export const TreeEditorContext = createContext<TreeEditor | null>(null);

export function useTreeEditor(): TreeEditor {
  const editor = useContext(TreeEditorContext);
  if (!editor) {
    throw new Error('Tree editor parts must be rendered inside a TreeEditorContext');
  }
  return editor;
}

export function useTreeState<TSelected>(selector: (state: TreeStoreState) => TSelected): TSelected {
  return useTreeEditor().store(selector);
}

export interface TreeVocabulary {
  data: Vocabulary | undefined;
  isError: boolean;
}

export function useTreeVocabulary(): TreeVocabulary {
  const { kind } = useTreeEditor();
  const vocabulary = useVocabulary();
  const declaredVolumes = useTreeState((state) => state.document.volumes);
  const data = useMemo(() => (vocabulary.data ? kind.editorVocabulary(vocabulary.data, declaredVolumes) : undefined), [kind, vocabulary.data, declaredVolumes]);
  return { data, isError: vocabulary.isError };
}
