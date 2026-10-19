import type { Vocabulary } from '../../../api/vocabulary.ts';

export type ItemFamily = 'ship' | 'chart';

export const ITEM_FAMILY_OPTIONS: { value: ItemFamily; label: string }[] = [
  { value: 'ship', label: 'Hull items and subroutines' },
  { value: 'chart', label: 'Star charts' },
];

export type DefinitionFamily = ItemFamily | 'anchor' | 'doctrine';

export const DEFINITION_FAMILY_OPTIONS: { value: DefinitionFamily; label: string }[] = [
  ...ITEM_FAMILY_OPTIONS,
  { value: 'anchor', label: 'Wormhole anchors' },
  { value: 'doctrine', label: 'Doctrine Archives' },
];

export const MODIFIER_FILE_NAMES: Record<ItemFamily, string> = { ship: 'item_modifiers.csv', chart: 'star_chart_modifiers.csv' };
export const MODIFIER_RESOURCES: Record<ItemFamily, string> = { ship: 'item-modifiers', chart: 'star-chart-modifiers' };
export const IMPLICIT_FILE_NAMES: Record<ItemFamily, string> = { ship: 'hull_item_implicits.csv', chart: 'star_chart_implicits.csv' };
export const IMPLICIT_RESOURCES: Record<ItemFamily, string> = { ship: 'hull-item-implicits', chart: 'star-chart-implicits' };
export const DEFINITION_RESOURCES: Record<ItemFamily, string[]> = { ship: ['subroutines', 'hull-items'], chart: ['star-charts'] };
export const DEFINITION_FILE_NAMES: Record<ItemFamily, string> = { ship: 'subroutines.csv and hull_items.csv', chart: 'star_charts.csv' };

export function isFamily(value: string | null): value is ItemFamily {
  return value === 'ship' || value === 'chart';
}

export function familyEffectNames(family: ItemFamily, vocabulary: Vocabulary): string[] {
  return family === 'chart' ? vocabulary.chartEffectNames : vocabulary.effectNames;
}

export function isFamilyEffect(family: ItemFamily, vocabulary: Vocabulary, effectName: string): boolean {
  return family === 'chart' ? vocabulary.chartEffectNames.includes(effectName) : vocabulary.effectInfo.has(effectName);
}
