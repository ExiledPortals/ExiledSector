import type { Vocabulary } from '../../../api/vocabulary.ts';

export type ItemFamily = 'ship' | 'chart';

export const MODIFIER_FILE_NAMES: Record<ItemFamily, string> = { ship: 'item_modifiers.csv', chart: 'star_chart_modifiers.csv' };

export function familyEffectNames(family: ItemFamily, vocabulary: Vocabulary): string[] {
  return family === 'chart' ? vocabulary.chartEffectNames : vocabulary.effectNames;
}

export function isFamilyEffect(family: ItemFamily, vocabulary: Vocabulary, effectName: string): boolean {
  return family === 'chart' ? vocabulary.chartEffectNames.includes(effectName) : vocabulary.effectInfo.has(effectName);
}
