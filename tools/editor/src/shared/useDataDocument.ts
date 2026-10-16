import { produce, type Draft } from 'immer';
import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchDataFiles, saveDataFiles, SERVER_HINT, type DataFiles } from '../api/client.ts';
import { confirmAction, showIssues } from './dialogs.tsx';
import { handleSaveResult } from './saveResult.ts';
import { useReportUnsavedChanges } from './unsavedChanges.ts';

export interface DataDocumentOptions<TModel> {
  key: string;
  noun: string;
  fileNames: string[];
  parse: (files: DataFiles) => TModel;
  serialize: (model: TModel) => Record<string, string>;
  validate: (model: TModel) => string[];
  savedMessage: (model: TModel) => string;
  afterSave?: (draft: Draft<TModel>, saved: TModel) => void;
}

export type DataDocumentState<TModel> =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; model: TModel };

export interface DataDocument<TModel> {
  state: DataDocumentState<TModel>;
  dirty: boolean;
  saving: boolean;
  update: (recipe: (draft: Draft<TModel>) => void) => void;
  reload: () => Promise<void>;
  save: () => Promise<void>;
  confirmDiscard: (question: string) => Promise<boolean>;
}

export function useDataDocument<TModel>(options: DataDocumentOptions<TModel>): DataDocument<TModel> {
  const [state, setState] = useState<DataDocumentState<TModel>>({ status: 'loading' });
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const baseHashes = useRef<Record<string, string>>({});
  const editRevision = useRef(0);
  const loadGeneration = useRef(0);
  const optionsRef = useRef(options);
  optionsRef.current = options;
  const fileNamesKey = options.fileNames.join(',');

  useReportUnsavedChanges(options.key, dirty);

  const load = useCallback(async () => {
    const { fileNames, parse } = optionsRef.current;
    loadGeneration.current += 1;
    setState({ status: 'loading' });
    try {
      const files = await fetchDataFiles(fileNames);
      baseHashes.current = Object.fromEntries(fileNames.map((name) => [name, files[name].hash]));
      setState({ status: 'ready', model: parse(files) });
      setDirty(false);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setState({ status: 'error', message: `Could not load ${fileNames.join(', ')} (${message}) - ${SERVER_HINT}` });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load, fileNamesKey]);

  const update = useCallback((recipe: (draft: Draft<TModel>) => void) => {
    editRevision.current += 1;
    setState((current) => (current.status === 'ready' ? { status: 'ready', model: produce(current.model, recipe) } : current));
    setDirty(true);
  }, []);

  const confirmDiscard = useCallback(
    async (question: string) =>
      !dirty || confirmAction({ title: 'Unsaved changes', message: question, confirmLabel: 'Discard changes', danger: true }),
    [dirty],
  );

  const reload = useCallback(async () => {
    if (await confirmDiscard(`Discard unsaved ${optionsRef.current.noun} changes and reload from disk?`)) {
      await load();
    }
  }, [confirmDiscard, load]);

  const save = useCallback(async () => {
    if (state.status !== 'ready') {
      return;
    }
    const { validate, serialize, savedMessage, afterSave, noun } = optionsRef.current;
    const savedModel = state.model;
    const issues = validate(savedModel);
    if (issues.length > 0) {
      showIssues('Not saved', issues);
      return;
    }
    const savedRevision = editRevision.current;
    const savedLoadGeneration = loadGeneration.current;
    setSaving(true);
    try {
      const result = await saveDataFiles(serialize(savedModel), baseHashes.current);
      await handleSaveResult(result, {
        discardedChanges: `your unsaved ${noun} changes`,
        onSaved: (hashes) => {
          if (loadGeneration.current !== savedLoadGeneration) {
            return;
          }
          baseHashes.current = { ...baseHashes.current, ...hashes };
          if (afterSave) {
            setState((current) =>
              current.status === 'ready' ? { status: 'ready', model: produce(current.model, (draft) => afterSave(draft, savedModel)) } : current,
            );
          }
          if (editRevision.current === savedRevision) {
            setDirty(false);
          }
        },
        savedMessage: () => savedMessage(savedModel),
        reloadFromDisk: load,
      });
    } finally {
      setSaving(false);
    }
  }, [state, load]);

  return { state, dirty, saving, update, reload, save, confirmDiscard };
}
