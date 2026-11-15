import { Group } from '@mantine/core';
import { CommitNumberInput } from '../../../shared/CommitInputs.tsx';

interface Position {
  x: number;
  y: number;
}

interface PositionFieldsProps {
  x: number;
  y: number;
  onEdit: (change: (target: Position) => void) => void;
}

export function PositionFields({ x, y, onEdit }: PositionFieldsProps) {
  return (
    <Group grow gap={6}>
      <CommitNumberInput size="xs" label="X" value={x} onCommit={(value) => onEdit((target) => void (target.x = value || 0))} />
      <CommitNumberInput size="xs" label="Y" value={y} onCommit={(value) => onEdit((target) => void (target.y = value || 0))} />
    </Group>
  );
}
