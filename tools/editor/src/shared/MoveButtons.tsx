import { ActionIcon, Group, Tooltip } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconX } from '@tabler/icons-react';

interface RowButtonsProps {
  index: number;
  count: number;
  onMove: (from: number, to: number) => void;
  onRemove: () => void;
}

export function RowButtons({ index, count, onMove, onRemove }: RowButtonsProps) {
  return (
    <Group gap={2} wrap="nowrap">
      <Tooltip label="Move up">
        <ActionIcon variant="subtle" color="gray" disabled={index === 0} onClick={() => onMove(index, index - 1)} aria-label="Move up">
          <IconArrowUp size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Move down">
        <ActionIcon variant="subtle" color="gray" disabled={index === count - 1} onClick={() => onMove(index, index + 1)} aria-label="Move down">
          <IconArrowDown size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Remove">
        <ActionIcon variant="subtle" color="red" onClick={onRemove} aria-label="Remove">
          <IconX size={16} />
        </ActionIcon>
      </Tooltip>
    </Group>
  );
}
