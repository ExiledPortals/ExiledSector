package exiledsector.items;

import exiledsector.modifiers.TierRange;

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
        Map<String, Double> shares = new LinkedHashMap<>();
        addPoolShares(AffixLayout.rollableAt(definition.prefixes(), tier), shares);
        addPoolShares(AffixLayout.rollableAt(definition.suffixes(), tier), shares);
        return shares;
    }

    private static void addPoolShares(List<PoolEntry> pool, Map<String, Double> shares) {
        double weightTotal = 0.0;
        for (PoolEntry entry : pool) {
            weightTotal += entry.weight();
        }
        for (PoolEntry entry : pool) {
            shares.put(entry.modifier().id(), weightTotal > 0.0 ? entry.weight() / weightTotal : 0.0);
        }
    }
}
