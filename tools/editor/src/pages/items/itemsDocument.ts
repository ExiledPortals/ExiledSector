import { useEffect } from 'react';
import type { Vocabulary } from '../../api/vocabulary.ts';
import { useDataDocument, type DataDocument } from '../../shared/useDataDocument.ts';
import { reloadTreeIfUnchanged } from '../tree/state/treePersistence.ts';
import {
  changedItemFiles,
  ITEMS_RESOURCES,
  itemsModelWarnings,
  markItemsSaved,
  parseItemsModel,
  savedItemsMessage,
  validateItemsModel,
  type ItemsModel,
} from './model/workspace.ts';

export type ItemsDocument = DataDocument<ItemsModel>;

export interface ItemsPanelProps {
  document: ItemsDocument;
  model: ItemsModel;
  vocabulary: Vocabulary;
}

export function useItemsDocument(vocabulary: Vocabulary): ItemsDocument {
  const document = useDataDocument<ItemsModel>({
    key: 'items',
    noun: 'item',
    fileNames: ITEMS_RESOURCES,
    parse: (files) => parseItemsModel(files, vocabulary),
    serialize: changedItemFiles,
    validate: (model) => {
      const issues = validateItemsModel(model, vocabulary);
      return Object.keys(changedItemFiles(model)).length === 0 && issues.length === 0 ? ['Nothing has changed since the last save.'] : issues;
    },
    warnings: (model) => itemsModelWarnings(model, vocabulary),
    savedMessage: (model) => savedItemsMessage(Object.keys(changedItemFiles(model))),
    afterSave: markItemsSaved,
  });
  const typesSaveCount = document.state.status === 'ready' ? document.state.model.typesSaveCount : 0;
  useEffect(() => {
    if (typesSaveCount > 0) {
      void reloadTreeIfUnchanged();
    }
  }, [typesSaveCount]);
  return document;
}
