import type { Vocabulary } from '../../../api/vocabulary.ts';
import { fetchDataFiles, fetchReservedNodeIds, saveDataFiles, SERVER_HINT, type ReservedNodeIds } from '../../../api/client.ts';
import { confirmAction, notifyError } from '../../../shared/dialogs.tsx';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { handleSaveResult } from '../../../shared/saveResult.ts';
import { saveOutcome, type SaveTicket } from '../../../shared/saveTracking.ts';
import { pruneConnectorEntries } from '../model/connectors.ts';
import { parseTreeDocument, serializeTreeFiles } from '../model/serialization.ts';
import type { TreeDocument } from '../model/types.ts';
import { validateTree } from '../model/validation.ts';
import type { TreeKind } from '../treeKinds.ts';
import { EMPTY_SELECTION, type TreeStore } from './treeStore.ts';

const DRAFT_WRITE_DELAY_MS = 400;
const NO_RESERVED_NODE_IDS: ReservedNodeIds = { replaced: [], released: [] };

interface TreeDraft {
  basedOn: Record<string, string> | null;
  savedAt: number | null;
  data: TreeDocument;
}

function normalizeDraftData(data: TreeDocument): TreeDocument {
  const document: TreeDocument = {
    types: data.types,
    volumes: data.volumes ?? [],
    nodes: data.nodes.map((node) => ({ ...node, connectedTo: node.connectedTo ?? [] })),
    curves: data.curves ?? [],
    hiddenConnectors: data.hiddenConnectors ?? [],
    anchors: data.anchors ?? [],
    decorations: data.decorations,
  };
  pruneConnectorEntries(document);
  return document;
}

export function createTreePersistence(kind: TreeKind, store: TreeStore) {
  const { files, label, draftStorageKey } = kind;
  const { typesResource, layoutResource, typesFileName, layoutFileName } = files;
  const bothFileNames = `${typesFileName} and ${layoutFileName}`;
  let draftTimer: ReturnType<typeof setTimeout> | null = null;
  let treeLoadGeneration = 0;

  function writeDraft(): void {
    const state = store.getState();
    if (state.load.status !== 'ready' || !state.dirty) {
      return;
    }
    try {
      const draft: TreeDraft = { basedOn: state.baseHashes, savedAt: Date.now(), data: state.document };
      localStorage.setItem(draftStorageKey, JSON.stringify(draft));
    } catch {
      return;
    }
  }

  function cancelDraftWrite(): void {
    if (draftTimer !== null) {
      clearTimeout(draftTimer);
      draftTimer = null;
    }
  }

  function scheduleDraftWrite(): void {
    cancelDraftWrite();
    draftTimer = setTimeout(() => {
      draftTimer = null;
      writeDraft();
    }, DRAFT_WRITE_DELAY_MS);
  }

  store.subscribe((state, previous) => {
    if (state.revision !== previous.revision && state.dirty) {
      scheduleDraftWrite();
    }
  });

  function clearDraft(): void {
    cancelDraftWrite();
    try {
      localStorage.removeItem(draftStorageKey);
    } catch {
      return;
    }
  }

  function readDraft(): TreeDraft | null {
    try {
      const raw = localStorage.getItem(draftStorageKey);
      const draft = raw ? (JSON.parse(raw) as TreeDraft) : null;
      return draft?.data?.types && draft.data.nodes && Array.isArray(draft.data.decorations) ? draft : null;
    } catch {
      return null;
    }
  }

  async function offerDraftRestore(diskDocument: TreeDocument): Promise<void> {
    const draft = readDraft();
    if (!draft) {
      return;
    }
    const { lineEndings, baseHashes } = store.getState();
    const draftDocument = normalizeDraftData(draft.data);
    const diskTexts = serializeTreeFiles(files, diskDocument, lineEndings);
    const draftTexts = serializeTreeFiles(files, draftDocument, lineEndings);
    if (draftTexts.types === diskTexts.types && draftTexts.tree === diskTexts.tree) {
      clearDraft();
      return;
    }
    const age = draft.savedAt ? ` from ${new Date(draft.savedAt).toLocaleString()}` : '';
    const sameBase = Boolean(draft.basedOn) && draft.basedOn?.[typesResource] === baseHashes[typesResource] && draft.basedOn?.[layoutResource] === baseHashes[layoutResource];
    const restore = sameBase
      ? await confirmAction({
          title: `Unsaved ${label} changes`,
          message: `This browser has unsaved ${label} changes${age}. Restore them? Cancel discards them and keeps the files on disk.`,
          confirmLabel: 'Restore draft',
        })
      : await confirmAction({
          title: `Out-of-date ${label} draft`,
          message:
            `This browser has unsaved ${label} changes${age}, but ${typesFileName} or ${layoutFileName} changed on disk since. ` +
            'Restoring the draft and saving would overwrite the newer files. Cancel discards the draft and keeps the files on disk.',
          confirmLabel: 'Restore draft over newer files',
          danger: true,
        });
    if (restore) {
      store.setState((state) => ({ document: draftDocument, dirty: true, revision: state.revision + 1 }));
    } else {
      clearDraft();
    }
  }

  async function loadTree(): Promise<void> {
    treeLoadGeneration += 1;
    store.setState({ load: { status: 'loading' } });
    try {
      const [loaded, reservedIds] = await Promise.all([
        fetchDataFiles([typesResource, layoutResource]),
        kind.reservesRetiredNodeIds ? fetchReservedNodeIds() : Promise.resolve(NO_RESERVED_NODE_IDS),
      ]);
      const typesFile = loaded[typesResource];
      const layoutFile = loaded[layoutResource];
      const document = parseTreeDocument(files, typesFile.content, layoutFile.content);
      store.setState({
        document,
        replacedNodeIds: new Set(reservedIds.replaced),
        releasedNodeIds: new Set(reservedIds.released),
        baseHashes: { [typesResource]: typesFile.hash, [layoutResource]: layoutFile.hash },
        lineEndings: { types: lineEndingOf(typesFile.content), tree: lineEndingOf(layoutFile.content) },
        dirty: false,
        selection: EMPTY_SELECTION,
        activeDrag: null,
      });
      await offerDraftRestore(document);
      store.setState({ load: { status: 'ready' }, fitRequested: true });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      store.setState({ load: { status: 'error', message: `Could not load the ${label} files (${message}) - ${SERVER_HINT}` } });
    }
  }

  async function reloadTreeIfUnchanged(): Promise<void> {
    const state = store.getState();
    if (state.load.status === 'ready' && !state.dirty && !state.saving) {
      await loadTree();
    }
  }

  async function reloadTreeFromDisk(): Promise<void> {
    const confirmed = await confirmAction({
      title: 'Reload from disk',
      message: `Discard all ${label} changes in this editor and reload ${bothFileNames} from disk?`,
      confirmLabel: 'Discard and reload',
      danger: true,
    });
    if (confirmed) {
      clearDraft();
      await loadTree();
    }
  }

  async function saveTree(vocabulary: Vocabulary | undefined): Promise<void> {
    const state = store.getState();
    if (state.load.status !== 'ready' || !vocabulary) {
      notifyError('Not saved - the tree files or the editor vocabulary did not load. Fix the error, then reload from disk.');
      return;
    }
    const issues = validateTree(state.document, vocabulary, state.replacedNodeIds);
    if (issues.length > 0) {
      const shown = issues.slice(0, 8).join('\n') + (issues.length > 8 ? '\n...' : '');
      const saveAnyway = await confirmAction({
        title: 'Save with issues?',
        message: `${issues.length} issue${issues.length > 1 ? 's' : ''} will fail the build or break the tree in game:\n${shown}`,
        confirmLabel: 'Save anyway',
        danger: true,
      });
      if (!saveAnyway) {
        return;
      }
    }
    const ticket: SaveTicket = { revision: store.getState().revision, loadGeneration: treeLoadGeneration };
    const texts = serializeTreeFiles(files, store.getState().document, state.lineEndings);
    store.setState({ saving: true });
    try {
      const result = await saveDataFiles({ [typesResource]: texts.types, [layoutResource]: texts.tree }, state.baseHashes);
      await handleSaveResult(result, {
        discardedChanges: `your unsaved ${label} changes`,
        onSaved: (hashes) => {
          const outcome = saveOutcome(ticket, { revision: store.getState().revision, loadGeneration: treeLoadGeneration });
          if (outcome === 'superseded') {
            return;
          }
          store.setState((current) => ({ baseHashes: { ...current.baseHashes, ...hashes }, dirty: outcome !== 'clean' }));
          if (outcome === 'clean') {
            clearDraft();
          } else {
            writeDraft();
          }
        },
        savedMessage: () => `Saved ${bothFileNames}`,
        reloadFromDisk: async () => {
          clearDraft();
          await loadTree();
        },
      });
    } finally {
      store.setState({ saving: false });
    }
  }

  return { loadTree, reloadTreeIfUnchanged, reloadTreeFromDisk, saveTree };
}

export type TreePersistence = ReturnType<typeof createTreePersistence>;
