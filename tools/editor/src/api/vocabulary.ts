import { useQuery } from '@tanstack/react-query';
import { compareText } from '../shared/arrays.ts';
import { getJson } from './client.ts';

export interface EffectInfo {
  name: string;
  statMode: string | null;
  lowerIsBetter: boolean;
}

export interface ChartTierGroup {
  id: string;
  minTier: number;
  maxTier: number;
}

const DEFAULT_CHART_TIER_GROUPS: ChartTierGroup[] = [
  { id: 'low', minTier: 1, maxTier: 5 },
  { id: 'mid', minTier: 6, maxTier: 10 },
  { id: 'high', minTier: 11, maxTier: 16 },
];

interface VocabularyFile {
  effects: EffectInfo[];
  tags: { theme: string[]; volume: string[]; requirement: string[]; gate?: string[] };
  tiers: string[];
  unlockConditionTypes: string[];
  parameterlessUnlockConditionTypes?: string[];
  blueprintCategories: string[];
  itemTypes: string[];
  itemKinds: { id: string; type: string }[];
  subroutineKinds?: string[];
  modifierScopes?: string[];
  doctrineBehaviours?: string[];
  radiusSizes?: string[];
  uniqueSourceKinds?: string[];
  hullItemMechanics?: string[];
  hullItemCrew?: string[];
  itemVolumes?: string[];
  itemUnlocks: string[];
  clearedChartTierPrefix?: string;
  chartTierRange?: [number, number];
  chartTierGroups?: ChartTierGroup[];
  chartEffects?: string[];
  chartSystemTypes?: string[];
}

export interface Vocabulary {
  effectNames: string[];
  effectInfo: Map<string, EffectInfo>;
  themeTags: string[];
  volumeTags: string[];
  requirementTags: string[];
  gateTags: string[];
  allTags: string[];
  tiers: string[];
  unlockConditionTypes: string[];
  parameterlessUnlockConditionTypes: string[];
  blueprintCategories: string[];
  itemTypes: string[];
  itemKinds: string[];
  itemKindTypes: Map<string, string>;
  subroutineKinds: string[];
  modifierScopes: string[];
  doctrineBehaviours: string[];
  radiusSizes: string[];
  uniqueSourceKinds: string[];
  hullItemMechanics: string[];
  hullItemCrew: string[];
  itemVolumes: string[];
  itemUnlocks: string[];
  clearedChartTierPrefix: string;
  lowestChartTier: number;
  highestChartTier: number;
  chartTierGroups: ChartTierGroup[];
  chartEffectNames: string[];
  chartSystemTypes: string[];
}

export function buildVocabulary(file: VocabularyFile): Vocabulary {
  return {
    effectNames: file.effects.map((effect) => effect.name).sort(compareText),
    effectInfo: new Map(file.effects.map((effect) => [effect.name, effect])),
    themeTags: file.tags.theme.slice(),
    volumeTags: file.tags.volume.slice(),
    requirementTags: file.tags.requirement.slice(),
    gateTags: (file.tags.gate ?? []).slice(),
    allTags: [...file.tags.theme, ...file.tags.volume, ...file.tags.requirement, ...(file.tags.gate ?? [])],
    tiers: file.tiers.slice(),
    unlockConditionTypes: file.unlockConditionTypes.slice(),
    parameterlessUnlockConditionTypes: (file.parameterlessUnlockConditionTypes ?? []).slice(),
    blueprintCategories: file.blueprintCategories.slice(),
    itemTypes: file.itemTypes.slice(),
    itemKinds: file.itemKinds.map((kind) => kind.id),
    itemKindTypes: new Map(file.itemKinds.map((kind) => [kind.id, kind.type])),
    subroutineKinds: (file.subroutineKinds ?? []).slice(),
    modifierScopes: (file.modifierScopes ?? file.itemTypes).slice(),
    doctrineBehaviours: (file.doctrineBehaviours ?? ['TRANSFORM', 'AUGMENT']).slice(),
    radiusSizes: (file.radiusSizes ?? ['small', 'medium', 'large']).slice(),
    uniqueSourceKinds: (file.uniqueSourceKinds ?? ['faction', 'site']).slice(),
    hullItemMechanics: (file.hullItemMechanics ?? ['one_of']).slice(),
    hullItemCrew: (file.hullItemCrew ?? ['crewed', 'automated']).slice(),
    itemVolumes: (file.itemVolumes ?? []).slice(),
    itemUnlocks: file.itemUnlocks.slice(),
    clearedChartTierPrefix: file.clearedChartTierPrefix ?? 'cleared_chart_tier_',
    lowestChartTier: file.chartTierRange?.[0] ?? 1,
    highestChartTier: file.chartTierRange?.[1] ?? 15,
    chartTierGroups: (file.chartTierGroups ?? DEFAULT_CHART_TIER_GROUPS).map((group) => ({ ...group })),
    chartEffectNames: (file.chartEffects ?? []).slice(),
    chartSystemTypes: (file.chartSystemTypes ?? []).slice(),
  };
}

export function useVocabulary() {
  return useQuery({
    queryKey: ['vocabulary'],
    queryFn: async () => buildVocabulary(await getJson<VocabularyFile>('/api/vocabulary')),
  });
}
