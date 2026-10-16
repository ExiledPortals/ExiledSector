import { modals } from '@mantine/modals';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import type { SkillType } from '../model/types.ts';
import { addNodeOfType } from '../state/treeActions.ts';
import { AddNodeDialog } from './AddNodeDialog.tsx';
import { TypeEditorDialog } from './TypeEditorDialog.tsx';
import { TypePickerDialog } from './TypePickerDialog.tsx';

let dialogCounter = 0;

function nextModalId(prefix: string): string {
  dialogCounter++;
  return `${prefix}-${dialogCounter}`;
}

export function openTypeEditor(vocabulary: Vocabulary, existingTypeId: string | null, onSaved?: (saved: SkillType) => void): void {
  const modalId = nextModalId('type-editor');
  modals.open({
    modalId,
    title: existingTypeId === null ? 'New skill type' : `Edit type "${existingTypeId}"`,
    size: 'xl',
    closeOnClickOutside: false,
    children: (
      <TypeEditorDialog
        modalId={modalId}
        existingTypeId={existingTypeId}
        vocabulary={vocabulary}
        onSaved={onSaved}
        onEditOptionType={(childId) => openTypeEditor(vocabulary, childId)}
      />
    ),
  });
}

export function openTypePicker(currentTypeId: string, onPick: (typeId: string) => void): void {
  const modalId = nextModalId('type-picker');
  modals.open({
    modalId,
    title: 'Change node type',
    size: 'lg',
    children: (
      <TypePickerDialog
        currentTypeId={currentTypeId}
        onPick={(typeId) => {
          modals.close(modalId);
          onPick(typeId);
        }}
      />
    ),
  });
}

export function openAddNode(vocabulary: Vocabulary | undefined, x: number, y: number): void {
  if (!vocabulary) {
    return;
  }
  const modalId = nextModalId('add-node');
  modals.open({
    modalId,
    title: `Add node at (${x}, ${y})`,
    children: (
      <AddNodeDialog
        vocabulary={vocabulary}
        onCancel={() => modals.close(modalId)}
        onAdd={(typeId) => {
          modals.close(modalId);
          addNodeOfType(typeId, x, y, vocabulary);
        }}
        onDefineNewType={() => {
          modals.close(modalId);
          openTypeEditor(vocabulary, null, (saved) => addNodeOfType(saved.id, x, y, vocabulary));
        }}
      />
    ),
  });
}
