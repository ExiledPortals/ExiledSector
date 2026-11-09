import type { TreeView } from '../state/treeStore.ts';

export interface SceneryViewport {
  canvasWidth: number;
  canvasHeight: number;
  centerX: number;
  centerY: number;
  zoom: number;
  gamePanX: number;
  gamePanY: number;
}

export function sceneryViewport(view: TreeView, canvasWidth: number, canvasHeight: number): SceneryViewport {
  return {
    canvasWidth,
    canvasHeight,
    centerX: canvasWidth / 2 + view.panX,
    centerY: canvasHeight / 2 - view.panY,
    zoom: view.zoom,
    gamePanX: view.panX,
    gamePanY: -view.panY,
  };
}

export function screenX(viewport: SceneryViewport, treeX: number): number {
  return viewport.centerX + treeX * viewport.zoom;
}

export function screenY(viewport: SceneryViewport, treeY: number): number {
  return viewport.centerY - treeY * viewport.zoom;
}

export function isOnCanvas(viewport: SceneryViewport, atScreenX: number, atScreenY: number, screenRadius: number): boolean {
  return atScreenX + screenRadius >= 0 && atScreenX - screenRadius <= viewport.canvasWidth && atScreenY + screenRadius >= 0 && atScreenY - screenRadius <= viewport.canvasHeight;
}

export function canvasPixelY(viewport: SceneryViewport, glScreenY: number): number {
  return viewport.canvasHeight - glScreenY;
}
