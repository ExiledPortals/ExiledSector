import { Button, Group, Select, Stack, Text } from '@mantine/core';
import { useMemo, useState } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { notifyError } from '../../../shared/dialogs.tsx';
import { TIER_LABELS, tierOf } from '../model/types.ts';
import { useTreeStore } from '../state/treeStore.ts';

interface AddNodeDialogProps {
  vocabulary: Vocabulary;
  onAdd: (typeId: string) => void;
  onDefineNewType: () => void;
  onCancel: () => void;
}

const ALL_SIZES = 'ALL';

export function AddNodeDialog({ vocabulary, onAdd, onDefineNewType, onCancel }: AddNodeDialogProps) {
  const types = useTreeStore((state) => state.document.types);
  const [sizeFilter, setSizeFilter] = useState(ALL_SIZES);
  const [typeId, setTypeId] = useState<string | null>(null);
  const typeOptions = useMemo(
    () => types.filter((type) => sizeFilter === ALL_SIZES || tierOf(type) === sizeFilter).map((type) => ({ value: type.id, label: `${type.name} (${type.id})` })),
    [types, sizeFilter],
  );
  const chosen = typeId && typeOptions.some((option) => option.value === typeId) ? typeId : (typeOptions[0]?.value ?? null);

  return (
    <Stack gap="sm">
      <Select
        label="Size"
        data={[{ value: ALL_SIZES, label: 'All sizes' }, ...vocabulary.tiers.map((tier) => ({ value: tier, label: TIER_LABELS[tier] ?? tier }))]}
        value={sizeFilter}
        onChange={(value) => setSizeFilter(value ?? ALL_SIZES)}
        allowDeselect={false}
      />
      <Select
        label="Type"
        data={typeOptions}
        value={chosen}
        onChange={setTypeId}
        searchable
        allowDeselect={false}
        nothingFoundMessage="No types of this size"
        renderOption={({ option }) => (
          <Text size="sm" data-type-id={option.value}>
            {option.label}
          </Text>
        )}
        data-autofocus
      />
      <Group justify="space-between">
        <Button variant="default" onClick={onDefineNewType}>
          Define new type instead
        </Button>
        <Group gap="xs">
          <Button variant="default" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            onClick={() => {
              if (chosen) {
                onAdd(chosen);
              } else {
                notifyError('Pick a type first');
              }
            }}
          >
            Add node
          </Button>
        </Group>
      </Group>
    </Stack>
  );
}
