import { useQuery } from '@tanstack/react-query';
import { compareText } from '../shared/arrays.ts';
import { getJson } from './client.ts';

export interface EffectInfo {
  name: string;
  statMode: string | null;
  lowerIsBetter: boolean;
}

interface VocabularyFile {
  effects: EffectInfo[];
  tags: { theme: string[]; region: string[]; requirement: string[] };
  tiers: string[];
  unlockConditionTypes: string[];
  blueprintCategories: string[];
  socketableKinds: string[];
  socketableUnlocks: string[];
}

export interface Vocabulary {
  effectNames: string[];
  effectInfo: Map<string, EffectInfo>;
  themeTags: string[];
  regionTags: string[];
  requirementTags: string[];
  allTags: string[];
  tiers: string[];
  unlockConditionTypes: string[];
  blueprintCategories: string[];
  socketableKinds: string[];
  socketableUnlocks: string[];
}

export function buildVocabulary(file: VocabularyFile): Vocabulary {
  return {
    effectNames: file.effects.map((effect) => effect.name).sort(compareText),
    effectInfo: new Map(file.effects.map((effect) => [effect.name, effect])),
    themeTags: file.tags.theme.slice(),
    regionTags: file.tags.region.slice(),
    requirementTags: file.tags.requirement.slice(),
    allTags: [...file.tags.theme, ...file.tags.region, ...file.tags.requirement],
    tiers: file.tiers.slice(),
    unlockConditionTypes: file.unlockConditionTypes.slice(),
    blueprintCategories: file.blueprintCategories.slice(),
    socketableKinds: file.socketableKinds.slice(),
    socketableUnlocks: file.socketableUnlocks.slice(),
  };
}

export function useVocabulary() {
  return useQuery({
    queryKey: ['vocabulary'],
    queryFn: async () => buildVocabulary(await getJson<VocabularyFile>('/api/vocabulary')),
  });
}
