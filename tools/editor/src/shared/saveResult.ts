import { staleFileQuestion, type SaveResult } from '../api/client.ts';
import { confirmAction, notifyError, notifySuccess } from './dialogs.tsx';

export interface SaveResultHandlers {
  discardedChanges: string;
  onSaved: (hashes: Record<string, string>) => void;
  savedMessage: () => string;
  reloadFromDisk: () => Promise<void>;
}

export async function handleSaveResult(result: SaveResult, handlers: SaveResultHandlers): Promise<void> {
  if (result.ok) {
    handlers.onSaved(result.data.hashes ?? {});
    notifySuccess(handlers.savedMessage());
    return;
  }
  if (!result.conflict) {
    notifyError(`Could not save: ${result.message}`);
    return;
  }
  const reloadNow = await confirmAction({
    title: 'Files changed on disk',
    message: staleFileQuestion(result.message, handlers.discardedChanges),
    confirmLabel: 'Reload from disk',
    danger: true,
  });
  if (reloadNow) {
    await handlers.reloadFromDisk();
  }
}
