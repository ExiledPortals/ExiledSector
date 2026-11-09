export const SCENERY_STORAGE_KEY = 'exiledSectorEditor.treeScenery';

export function readShowScenery(): boolean {
  try {
    return globalThis.localStorage?.getItem(SCENERY_STORAGE_KEY) !== 'off';
  } catch {
    return true;
  }
}

export function rememberShowScenery(showScenery: boolean): void {
  try {
    globalThis.localStorage?.setItem(SCENERY_STORAGE_KEY, showScenery ? 'on' : 'off');
  } catch {
    return;
  }
}
