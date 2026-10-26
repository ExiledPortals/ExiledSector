import type { Vocabulary } from '../../api/vocabulary.ts';
import { HULL_ITEM_TYPE, UNLOCK_LABELS } from './model/definitions.ts';
import type { ItemModifier } from './model/modifiers.ts';

export function hullItemKinds(vocabulary: Vocabulary): string[] {
  return vocabulary.itemKinds.filter((kind) => vocabulary.itemKindTypes.get(kind) === HULL_ITEM_TYPE);
}

export function poolCandidates(catalogue: readonly ItemModifier[], slot: string, itemType: string, entries: readonly { modifierId: string }[]): ItemModifier[] {
  const listed = new Set(entries.map((entry) => entry.modifierId));
  return catalogue.filter((modifier) => modifier.slot === slot && modifier.itemTypes.includes(itemType) && !listed.has(modifier.id));
}

export function unlockOptions(current: string, vocabulary: Vocabulary, withChartTiers: boolean): { value: string; label: string }[] {
  const options = [{ value: '', label: 'None' }, ...vocabulary.itemUnlocks.map((unlockId) => ({ value: unlockId, label: UNLOCK_LABELS[unlockId] ?? unlockId }))];
  if (withChartTiers) {
    for (let tier = vocabulary.lowestChartTier; tier <= vocabulary.highestChartTier; tier++) {
      options.push({ value: `${vocabulary.clearedChartTierPrefix}${tier}`, label: `Cleared a tier ${tier} chart objective` });
    }
  }
  if (!options.some((option) => option.value === current)) {
    options.push({ value: current, label: `${current} (unknown)` });
  }
  return options;
}
