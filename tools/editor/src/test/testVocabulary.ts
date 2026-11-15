import { buildVocabulary, type Vocabulary, type VocabularyFile } from '../api/vocabulary.ts';
import { readRepositoryFile } from './testServer.ts';

const REPOSITORY_VOCABULARY_FILE = 'tools/editor_vocabulary.json';

export type TestVocabularyOverrides = Partial<Omit<VocabularyFile, 'tags'>> & { tags?: Partial<VocabularyFile['tags']> };

export function testVocabulary(overrides: TestVocabularyOverrides = {}): Vocabulary {
  const { tags, ...fields } = overrides;
  return buildVocabulary({
    effects: [],
    tiers: [],
    unlockConditionTypes: [],
    blueprintCategories: [],
    itemTypes: [],
    itemKinds: [],
    itemUnlocks: [],
    ...fields,
    tags: { theme: [], volume: [], requirement: [], ...tags },
  });
}

export function repositoryVocabulary(): Vocabulary {
  return buildVocabulary(JSON.parse(readRepositoryFile(REPOSITORY_VOCABULARY_FILE)) as VocabularyFile);
}
