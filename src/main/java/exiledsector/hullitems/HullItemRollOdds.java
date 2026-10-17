package exiledsector.hullitems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HullItemRollOdds {

    private HullItemRollOdds() {
    }

    public static Map<String, Double> weightShares(HullItemDefinition definition) {
        Map<String, Double> shares = new LinkedHashMap<>();
        addPoolShares(definition.prefixes(), shares);
        addPoolShares(definition.suffixes(), shares);
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
