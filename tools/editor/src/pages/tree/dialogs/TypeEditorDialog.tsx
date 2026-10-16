import { Button, Group, ScrollArea, Stack } from '@mantine/core';
import { modals } from '@mantine/modals';
import { produce } from 'immer';
import { useRef, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { notifyError } from '../../../shared/dialogs.tsx';
import { TypeForm, type TypeRecipe } from '../inspector/TypeForm.tsx';
import { DIALOG_TYPE_SECTIONS } from '../inspector/typeFormSections.ts';
import { typeById, type SkillType } from '../model/types.ts';
import { editDocument, useTreeStore } from '../state/treeStore.ts';

interface TypeEditorDialogProps {
  modalId: string;
  existingTypeId: string | null;
  vocabulary: Vocabulary;
  onSaved?: (saved: SkillType) => void;
  onEditOptionType: (typeId: string) => void;
}

function initialDraft(existingTypeId: string | null): SkillType {
  const existing = existingTypeId ? typeById(useTreeStore.getState().document, existingTypeId) : undefined;
  const draft: SkillType = existing ? structuredClone(existing) : { id: '', name: '', icon: '', tier: 'NOTABLE' };
  draft.effects ??= [];
  draft.hullSizeEffects ??= [];
  draft.unlockConditions ??= [];
  draft.tags ??= [];
  return draft;
}

export function TypeEditorDialog({ modalId, existingTypeId, vocabulary, onSaved, onEditOptionType }: TypeEditorDialogProps) {
  const [draft, setDraft] = useState(() => initialDraft(existingTypeId));
  const draftRef = useRef(draft);
  draftRef.current = draft;
  const allTypes = useTreeStore((state) => state.document.types);

  const edit = (recipe: TypeRecipe) => setDraft((current) => produce(current, recipe));

  const save = () => {
    const current = draftRef.current;
    if (!current.id) {
      notifyError('Id is required');
      return;
    }
    const isNew = existingTypeId === null;
    if (isNew && typeById(useTreeStore.getState().document, current.id)) {
      notifyError('A type with that id already exists');
      return;
    }
    const saved = structuredClone({ ...current, name: current.name || current.id });
    editDocument((document) => {
      if (isNew) {
        document.types.push(saved);
        return;
      }
      const index = document.types.findIndex((type) => type.id === existingTypeId);
      if (index !== -1) {
        document.types[index] = saved;
      }
    });
    modals.close(modalId);
    onSaved?.(saved);
  };

  return (
    <Stack gap="sm">
      <ScrollArea.Autosize mah="70vh" offsetScrollbars>
        <TypeForm
          type={draft}
          onEdit={edit}
          sections={DIALOG_TYPE_SECTIONS}
          vocabulary={vocabulary}
          allTypes={allTypes}
          idEditable={existingTypeId === null}
          onEditOptionType={onEditOptionType}
        />
      </ScrollArea.Autosize>
      <Group justify="flex-end">
        <Button variant="default" onClick={() => modals.close(modalId)}>
          Cancel
        </Button>
        <Button onClick={save}>Save type</Button>
      </Group>
    </Stack>
  );
}
