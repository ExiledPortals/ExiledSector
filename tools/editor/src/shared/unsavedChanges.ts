import { useEffect } from 'react';
import { create } from 'zustand';

interface UnsavedChangesState {
  dirtyByKey: Record<string, boolean>;
  setDirty: (key: string, dirty: boolean) => void;
}

export const useUnsavedChangesStore = create<UnsavedChangesState>((set) => ({
  dirtyByKey: {},
  setDirty: (key, dirty) =>
    set((state) => (Boolean(state.dirtyByKey[key]) === dirty ? state : { dirtyByKey: { ...state.dirtyByKey, [key]: dirty } })),
}));

export function hasUnsavedChanges(): boolean {
  return Object.values(useUnsavedChangesStore.getState().dirtyByKey).some(Boolean);
}

export function useReportUnsavedChanges(key: string, dirty: boolean): void {
  const setDirty = useUnsavedChangesStore((state) => state.setDirty);
  useEffect(() => {
    setDirty(key, dirty);
  }, [key, dirty, setDirty]);
  useEffect(() => () => setDirty(key, false), [key, setDirty]);
}

const unloadChecks = new Set<() => boolean>();

export function registerUnloadCheck(check: () => boolean): () => void {
  unloadChecks.add(check);
  return () => unloadChecks.delete(check);
}

export function hasUnsavedWork(): boolean {
  return hasUnsavedChanges() || [...unloadChecks].some((check) => check());
}

let unloadConfirmed = false;

export function confirmUnload(): void {
  unloadConfirmed = true;
}

export function shouldWarnBeforeUnload(): boolean {
  return !unloadConfirmed && hasUnsavedWork();
}
