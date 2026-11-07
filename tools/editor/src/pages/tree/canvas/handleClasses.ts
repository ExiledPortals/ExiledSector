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

const HANDLE_RADIUS = 5;
const ACTIVE_HANDLE_RADIUS = 7;
const CENTRE_HANDLE_RADIUS = 6;
const MOVING_CENTRE_HANDLE_RADIUS = 8;
export const SHAPE_CYCLE_HANDLE_SIZE = 10;

export function handleRadius(active: boolean, handleScale: number): number {
  return (active ? ACTIVE_HANDLE_RADIUS : HANDLE_RADIUS) * handleScale;
}

export function centreHandleRadius(moving: boolean, handleScale: number): number {
  return (moving ? MOVING_CENTRE_HANDLE_RADIUS : CENTRE_HANDLE_RADIUS) * handleScale;
}
