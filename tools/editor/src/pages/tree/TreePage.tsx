import { Select, Tooltip } from '@mantine/core';
import { useState } from 'react';
import { GameTooltip } from './GameTooltip.tsx';
import { HULL_SIZE_OPTIONS } from './model/types.ts';
import { shipTreeEditor } from './state/treeEditor.ts';
import { TreeEditorPage } from './TreeEditorPage.tsx';

const TOOLTIP_HULL_SIZE_KEY = 'exiledSectorEditor.tooltipHullSize';
const TOOLTIP_HULL_SIZES = [{ value: '', label: 'All sizes' }, ...HULL_SIZE_OPTIONS.map((option) => ({ value: option.value, label: option.value === 'CAPITAL_SHIP' ? 'Capital ship' : option.label }))];

function useTooltipHullSize(): [string, (hullSize: string) => void] {
  const [hullSize, setHullSize] = useState(() => {
    try {
      return localStorage.getItem(TOOLTIP_HULL_SIZE_KEY) ?? '';
    } catch {
      return '';
    }
  });
  const update = (next: string) => {
    setHullSize(next);
    try {
      localStorage.setItem(TOOLTIP_HULL_SIZE_KEY, next);
    } catch {
      return;
    }
  };
  return [hullSize, update];
}

export function TreePage() {
  const [tooltipHullSize, setTooltipHullSize] = useTooltipHullSize();
  const hullSizeSelect = (
    <Tooltip label="Hull size used by the in-game tooltip preview (hover a node, F1 for details)">
      <Select size="xs" w={130} data={TOOLTIP_HULL_SIZES} value={tooltipHullSize} onChange={(value) => setTooltipHullSize(value ?? '')} allowDeselect={false} />
    </Tooltip>
  );
  return <TreeEditorPage editor={shipTreeEditor} toolbarControls={hullSizeSelect} overlay={<GameTooltip hullSize={tooltipHullSize} />} />;
}
