import { modals } from '@mantine/modals';
import type { ReactNode } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import type { SkillType } from '../model/types.ts';
import type { TreeEditor } from '../state/treeEditor.ts';
import { TreeEditorContext } from '../state/treeEditorContext.ts';
import { AddNodeDialog } from './AddNodeDialog.tsx';
import { TypeEditorDialog } from './TypeEditorDialog.tsx';
import { TypePickerDialog } from './TypePickerDialog.tsx';

let dialogCounter = 0;

function nextModalId(prefix: string): string {
  dialogCounter++;
  return `${prefix}-${dialogCounter}`;
}

export function inTreeEditor(editor: TreeEditor, dialog: ReactNode): ReactNode {
  return <TreeEditorContext value={editor}>{dialog}</TreeEditorContext>;
}

export function openTypeEditor(editor: TreeEditor, vocabulary: Vocabulary, existingTypeId: string | null, onSaved?: (saved: SkillType) => void): void {
  const modalId = nextModalId('type-editor');
  modals.open({
    modalId,
    title: existingTypeId === null ? `New ${editor.kind.typeNoun}` : `Edit type "${existingTypeId}"`,
    size: 'xl',
    closeOnClickOutside: false,
    children: inTreeEditor(
      editor,
      <TypeEditorDialog
        modalId={modalId}
        existingTypeId={existingTypeId}
        vocabulary={vocabulary}
        onSaved={onSaved}
        onEditOptionType={(childId) => openTypeEditor(editor, vocabulary, childId)}
      />,
    ),
  });
}

export function openTypePicker(editor: TreeEditor, currentTypeId: string, onPick: (typeId: string) => void): void {
  const modalId = nextModalId('type-picker');
  modals.open({
    modalId,
    title: 'Change node type',
    size: 'lg',
    children: inTreeEditor(
      editor,
      <TypePickerDialog
        currentTypeId={currentTypeId}
        onPick={(typeId) => {
          modals.close(modalId);
          onPick(typeId);
        }}
      />,
    ),
  });
}

export function openAddNode(editor: TreeEditor, vocabulary: Vocabulary | undefined, x: number, y: number): void {
  if (!vocabulary) {
    return;
  }
  const { addNodeOfType } = editor.actions;
  const modalId = nextModalId('add-node');
  modals.open({
    modalId,
    title: `Add node at (${x}, ${y})`,
    children: inTreeEditor(
      editor,
      <AddNodeDialog
        vocabulary={vocabulary}
        onCancel={() => modals.close(modalId)}
        onAdd={(typeId) => {
          modals.close(modalId);
          addNodeOfType(typeId, x, y, vocabulary);
        }}
        onDefineNewType={() => {
          modals.close(modalId);
          openTypeEditor(editor, vocabulary, null, (saved) => addNodeOfType(saved.id, x, y, vocabulary));
        }}
      />,
    ),
  });
}
