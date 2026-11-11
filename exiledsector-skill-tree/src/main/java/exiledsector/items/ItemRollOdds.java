package exiledsector.items;

import exiledsector.modifiers.TierRange;
import exiledsector.skills.tags.TagWeights;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemRollOdds {

    private ItemRollOdds() {
    }

    public static Map<String, Double> weightShares(ItemDefinition definition) {
        return weightShares(definition, TierRange.NO_TIER);
    }

    public static Map<String, Double> weightShares(ItemDefinition definition, int tier) {
        return weightShares(definition, tier, TagWeights.NONE);
    }

    public static Map<String, Double> weightShares(ItemDefinition definition, int tier, TagWeights tagWeights) {
        Map<String, Double> shares = new LinkedHashMap<>();
        addPoolShares(AffixLayout.rollableAt(definition.type(), definition.prefixes(), tier), tagWeights, shares);
        addPoolShares(AffixLayout.rollableAt(definition.type(), definition.suffixes(), tier), tagWeights, shares);
        return shares;
    }

    private static void addPoolShares(List<PoolEntry> pool, TagWeights tagWeights, Map<String, Double> shares) {
        double weightTotal = 0.0;
        for (PoolEntry entry : pool) {
            weightTotal += entry.biasedWeight(tagWeights);
        }
        for (PoolEntry entry : pool) {
            shares.put(entry.modifier().id(), weightTotal > 0.0 ? entry.biasedWeight(tagWeights) / weightTotal : 0.0);
        }
    }
}
