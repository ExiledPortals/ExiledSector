package exiledsector.items;

import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
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
        addPoolShares(AffixLayout.rollableAt(definition.type(), definition.prefixes(), tier), shares);
        addPoolShares(AffixLayout.rollableAt(definition.type(), definition.suffixes(), tier), shares);
        return shares;
    }

    public static Map<String, Double> doctrineLineChances(DoctrineArchive archive) {
        List<PoolEntry> pool = archive.modifierPool();
        Map<String, Double> chances = new LinkedHashMap<>();
        int lineCountOptions = archive.maxLines() - archive.minLines() + 1;
        for (int targetIndex = 0; targetIndex < pool.size(); targetIndex++) {
            double chanceTotal = 0.0;
            for (int lineCount = archive.minLines(); lineCount <= archive.maxLines(); lineCount++) {
                chanceTotal += drawnChance(pool, pool.get(targetIndex), Math.min(lineCount, pool.size()));
            }
            chances.put(pool.get(targetIndex).modifier().id(), chanceTotal / lineCountOptions);
        }
        return chances;
    }

    private static double drawnChance(List<PoolEntry> remainingPool, PoolEntry target, int drawsLeft) {
        double weightTotal = 0.0;
        for (PoolEntry entry : remainingPool) {
            weightTotal += entry.weight();
        }
        if (drawsLeft <= 0 || weightTotal <= 0.0) {
            return 0.0;
        }
        double chance = 0.0;
        for (int drawnIndex = 0; drawnIndex < remainingPool.size(); drawnIndex++) {
            PoolEntry drawn = remainingPool.get(drawnIndex);
            double drawnShare = drawn.weight() / weightTotal;
            if (drawn == target) {
                chance += drawnShare;
            } else if (drawnShare > 0.0) {
                List<PoolEntry> poolAfterDraw = new ArrayList<>(remainingPool);
                poolAfterDraw.remove(drawnIndex);
                chance += drawnShare * drawnChance(poolAfterDraw, target, drawsLeft - 1);
            }
        }
        return chance;
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
