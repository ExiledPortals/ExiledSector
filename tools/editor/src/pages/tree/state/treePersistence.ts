import type { Vocabulary } from '../../../api/vocabulary.ts';
import { fetchDataFiles, fetchReservedNodeIds, saveDataFiles, SERVER_HINT } from '../../../api/client.ts';
import { confirmAction, notifyError } from '../../../shared/dialogs.tsx';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { handleSaveResult } from '../../../shared/saveResult.ts';
import { saveOutcome, type SaveTicket } from '../../../shared/saveTracking.ts';
import { pruneConnectorEntries } from '../model/connectors.ts';
import { parseTreeDocument, serializeTreeFiles } from '../model/serialization.ts';
import type { TreeDocument } from '../model/types.ts';
import { validateTree } from '../model/validation.ts';
import { EMPTY_SELECTION, useTreeStore } from './treeStore.ts';

const DRAFT_KEY = 'exiledSectorEditor.treeDraft';
const DRAFT_WRITE_DELAY_MS = 400;

interface TreeDraft {
  basedOn: Record<string, string> | null;
  savedAt: number | null;
  data: TreeDocument;
}

let draftTimer: ReturnType<typeof setTimeout> | null = null;
let treeLoadGeneration = 0;

function writeDraft(): void {
  const state = useTreeStore.getState();
  if (state.load.status !== 'ready' || !state.dirty) {
    return;
  }
  try {
    const draft: TreeDraft = { basedOn: state.baseHashes, savedAt: Date.now(), data: state.document };
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
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

useTreeStore.subscribe((state, previous) => {
  if (state.revision !== previous.revision && state.dirty) {
    scheduleDraftWrite();
  }
});

function clearDraft(): void {
  cancelDraftWrite();
  try {
    localStorage.removeItem(DRAFT_KEY);
  } catch {
    return;
  }
}

function readDraft(): TreeDraft | null {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    const draft = raw ? (JSON.parse(raw) as TreeDraft) : null;
    return draft?.data?.types && draft.data.nodes ? draft : null;
  } catch {
    return null;
  }
}

function normalizeDraftData(data: TreeDocument): TreeDocument {
  const document: TreeDocument = {
    types: data.types,
    nodes: data.nodes.map((node) => ({ ...node, connectedTo: node.connectedTo ?? [] })),
    curves: data.curves ?? [],
    hiddenConnectors: data.hiddenConnectors ?? [],
    anchors: data.anchors ?? [],
    images: data.images ?? [],
    stars: data.stars ?? [],
    threatUnits: data.threatUnits ?? [],
  };
  pruneConnectorEntries(document);
  return document;
}

async function offerDraftRestore(diskDocument: TreeDocument): Promise<void> {
  const draft = readDraft();
  if (!draft) {
    return;
  }
  const { lineEndings, baseHashes } = useTreeStore.getState();
  const draftDocument = normalizeDraftData(draft.data);
  const diskTexts = serializeTreeFiles(diskDocument, lineEndings);
  const draftTexts = serializeTreeFiles(draftDocument, lineEndings);
  if (draftTexts.types === diskTexts.types && draftTexts.tree === diskTexts.tree) {
    clearDraft();
    return;
  }
  const age = draft.savedAt ? ` from ${new Date(draft.savedAt).toLocaleString()}` : '';
  const sameBase = Boolean(draft.basedOn) && draft.basedOn?.types === baseHashes.types && draft.basedOn?.tree === baseHashes.tree;
  const restore = sameBase
    ? await confirmAction({
        title: 'Unsaved skill tree changes',
        message: `This browser has unsaved skill tree changes${age}. Restore them? Cancel discards them and keeps the files on disk.`,
        confirmLabel: 'Restore draft',
      })
    : await confirmAction({
        title: 'Out-of-date skill tree draft',
        message:
          `This browser has unsaved skill tree changes${age}, but skill_types.json or ship_skill_tree.json changed on disk since. ` +
          'Restoring the draft and saving would overwrite the newer files. Cancel discards the draft and keeps the files on disk.',
        confirmLabel: 'Restore draft over newer files',
        danger: true,
      });
  if (restore) {
    useTreeStore.setState((state) => ({ document: draftDocument, dirty: true, revision: state.revision + 1 }));
  } else {
    clearDraft();
  }
}

export async function loadTree(): Promise<void> {
  treeLoadGeneration += 1;
  useTreeStore.setState({ load: { status: 'loading' } });
  try {
    const [files, reservedIds] = await Promise.all([fetchDataFiles(['types', 'tree']), fetchReservedNodeIds()]);
    const document = parseTreeDocument(files.types.content, files.tree.content);
    useTreeStore.setState({
      document,
      replacedNodeIds: new Set(reservedIds.replaced),
      releasedNodeIds: new Set(reservedIds.released),
      baseHashes: { types: files.types.hash, tree: files.tree.hash },
      lineEndings: { types: lineEndingOf(files.types.content), tree: lineEndingOf(files.tree.content) },
      dirty: false,
      selection: EMPTY_SELECTION,
      activeDrag: null,
    });
    await offerDraftRestore(document);
    useTreeStore.setState({ load: { status: 'ready' }, fitRequested: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    useTreeStore.setState({ load: { status: 'error', message: `Could not load the skill tree files (${message}) - ${SERVER_HINT}` } });
  }
}

export async function reloadTreeFromDisk(): Promise<void> {
  const confirmed = await confirmAction({
    title: 'Reload from disk',
    message: 'Discard all skill tree changes in this editor and reload skill_types.json and ship_skill_tree.json from disk?',
    confirmLabel: 'Discard and reload',
    danger: true,
  });
  if (confirmed) {
    clearDraft();
    await loadTree();
  }
}

export async function saveTree(vocabulary: Vocabulary | undefined): Promise<void> {
  const state = useTreeStore.getState();
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
  const ticket: SaveTicket = { revision: useTreeStore.getState().revision, loadGeneration: treeLoadGeneration };
  const texts = serializeTreeFiles(useTreeStore.getState().document, state.lineEndings);
  useTreeStore.setState({ saving: true });
  try {
    const result = await saveDataFiles({ types: texts.types, tree: texts.tree }, state.baseHashes);
    await handleSaveResult(result, {
      discardedChanges: 'your unsaved skill tree changes',
      onSaved: (hashes) => {
        const outcome = saveOutcome(ticket, { revision: useTreeStore.getState().revision, loadGeneration: treeLoadGeneration });
        if (outcome === 'superseded') {
          return;
        }
        useTreeStore.setState((current) => ({ baseHashes: { ...current.baseHashes, ...hashes }, dirty: outcome !== 'clean' }));
        if (outcome === 'clean') {
          clearDraft();
        } else {
          writeDraft();
        }
      },
      savedMessage: () => 'Saved skill_types.json and ship_skill_tree.json',
      reloadFromDisk: async () => {
        clearDraft();
        await loadTree();
      },
    });
  } finally {
    useTreeStore.setState({ saving: false });
  }
}
