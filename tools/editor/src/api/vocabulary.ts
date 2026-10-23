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
  tags: { theme: string[]; region: string[]; requirement: string[]; gate?: string[] };
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
  itemUnlocks: string[];
  clearedChartTierPrefix?: string;
  chartTierRange?: [number, number];
  chartEffects?: string[];
  chartSystemTypes?: string[];
  chartPoiKinds?: string[];
  chartDerelictTypes?: string[];
  chartTierDefaults?: ChartTierDefault[];
  chartDropSources?: ChartDropSource[];
  chartLootDropTypes?: string[];
  chartLootScopes?: string[];
  chartColonyItemGroups?: string[];
  chartExpansions?: string[];
}

export interface ChartTierDefault {
  tier: number;
  fleetsMin: number;
  fleetsMax: number;
  levelMin: number;
  levelMax: number;
  bossSize: number;
}

export interface ChartDropSource {
  id: string;
  insideSystem: boolean;
}

export interface Vocabulary {
  effectNames: string[];
  effectInfo: Map<string, EffectInfo>;
  themeTags: string[];
  regionTags: string[];
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
  itemUnlocks: string[];
  clearedChartTierPrefix: string;
  lowestChartTier: number;
  highestChartTier: number;
  chartEffectNames: string[];
  chartSystemTypes: string[];
  chartPoiKinds: string[];
  chartDerelictTypes: string[];
  chartTierDefaults: ChartTierDefault[];
  chartDropSources: ChartDropSource[];
  chartLootDropTypes: string[];
  chartLootScopes: string[];
  chartColonyItemGroups: string[];
  chartExpansions: string[];
}

export function buildVocabulary(file: VocabularyFile): Vocabulary {
  return {
    effectNames: file.effects.map((effect) => effect.name).sort(compareText),
    effectInfo: new Map(file.effects.map((effect) => [effect.name, effect])),
    themeTags: file.tags.theme.slice(),
    regionTags: file.tags.region.slice(),
    requirementTags: file.tags.requirement.slice(),
    gateTags: (file.tags.gate ?? []).slice(),
    allTags: [...file.tags.theme, ...file.tags.region, ...file.tags.requirement, ...(file.tags.gate ?? [])],
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
    itemUnlocks: file.itemUnlocks.slice(),
    clearedChartTierPrefix: file.clearedChartTierPrefix ?? 'cleared_chart_tier_',
    lowestChartTier: file.chartTierRange?.[0] ?? 1,
    highestChartTier: file.chartTierRange?.[1] ?? 15,
    chartEffectNames: (file.chartEffects ?? []).slice(),
    chartSystemTypes: (file.chartSystemTypes ?? []).slice(),
    chartPoiKinds: (file.chartPoiKinds ?? []).slice(),
    chartDerelictTypes: (file.chartDerelictTypes ?? []).slice(),
    chartTierDefaults: (file.chartTierDefaults ?? []).map((tierDefault) => ({ ...tierDefault })),
    chartDropSources: (file.chartDropSources ?? []).map((source) => ({ ...source })),
    chartLootDropTypes: (file.chartLootDropTypes ?? []).slice(),
    chartLootScopes: (file.chartLootScopes ?? []).slice(),
    chartColonyItemGroups: (file.chartColonyItemGroups ?? []).slice(),
    chartExpansions: (file.chartExpansions ?? []).slice(),
  };
}

export function useVocabulary() {
  return useQuery({
    queryKey: ['vocabulary'],
    queryFn: async () => buildVocabulary(await getJson<VocabularyFile>('/api/vocabulary')),
  });
}
