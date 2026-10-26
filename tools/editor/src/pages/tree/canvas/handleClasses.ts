import type { ActiveDrag } from '../state/treeStore.ts';
import classes from './TreeCanvas.module.css';

export function handleClass(base: string, active: boolean): string {
  return `${classes.handle} ${base}${active ? ` ${classes.handleActive}` : ''}`;
}

export function centerClass(selected: boolean): string {
  return `${classes.centerHandle}${selected ? ` ${classes.centerHandleSelected}` : ''}`;
}

export function isMoving(activeDrag: ActiveDrag | null, list: 'anchorIds' | 'decorationIds', id: string): boolean {
  return activeDrag?.kind === 'move' && activeDrag[list].includes(id);
}

export function isHandleActive(activeDrag: ActiveDrag | null, kind: ActiveDrag['kind'], id: string): boolean {
  return activeDrag !== null && activeDrag.kind === kind && 'id' in activeDrag && activeDrag.id === id;
}
