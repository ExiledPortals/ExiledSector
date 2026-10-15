package exiledsector.socketables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SocketableRollOdds {

    public static final List<Integer> EFFECT_COUNTS = List.of(2, 3, AffixLayout.MAX_EFFECTS);

    private SocketableRollOdds() {
    }

    public static double effectCountChance(int effectCount) {
        return switch (effectCount) {
            case 2 -> AffixLayout.TWO_EFFECTS_CHANCE;
            case 3 -> AffixLayout.THREE_EFFECTS_CHANCE;
            case AffixLayout.MAX_EFFECTS -> 1.0 - AffixLayout.TWO_EFFECTS_CHANCE - AffixLayout.THREE_EFFECTS_CHANCE;
            default -> 0.0;
        };
    }

    public static Map<String, Double> foundChances(SocketableDefinition definition) {
        Map<String, Double> chances = zeroChances(definition);
        if (definition.unique()) {
            chances.replaceAll((effectName, chance) -> 1.0);
            return chances;
        }
        for (int effectCount : EFFECT_COUNTS) {
            double countChance = effectCountChance(effectCount);
            addSideChances(definition, chances, effectCount, AffixLayout.prefixCount(effectCount, true), countChance / 2.0);
            addSideChances(definition, chances, effectCount, AffixLayout.prefixCount(effectCount, false), countChance / 2.0);
        }
        return chances;
    }

    public static Map<String, Double> synthesisedChances(SocketableDefinition definition) {
        Map<String, Double> chances = zeroChances(definition);
        int effectCount = AffixLayout.COMMON_MAX_EFFECTS;
        addSideChances(definition, chances, effectCount, AffixLayout.prefixCount(effectCount, true), 0.5);
        addSideChances(definition, chances, effectCount, AffixLayout.prefixCount(effectCount, false), 0.5);
        return chances;
    }

    private static Map<String, Double> zeroChances(SocketableDefinition definition) {
        Map<String, Double> chances = new LinkedHashMap<>();
        for (PoolEntry entry : definition.pool()) {
            chances.put(entry.effectName(), 0.0);
        }
        return chances;
    }

    private static void addSideChances(SocketableDefinition definition, Map<String, Double> chances, int effectCount, int prefixCount,
                                       double layoutChance) {
        addPoolChances(definition.prefixes(), prefixCount, layoutChance, chances);
        addPoolChances(definition.suffixes(), effectCount - prefixCount, layoutChance, chances);
    }

    private static void addPoolChances(List<PoolEntry> pool, int drawCount, double layoutChance, Map<String, Double> chances) {
        int draws = Math.min(drawCount, pool.size());
        if (draws <= 0) {
            return;
        }
        List<Double> weights = new ArrayList<>(pool.size());
        for (PoolEntry entry : pool) {
            weights.add((double) entry.weight());
        }
        for (int index = 0; index < pool.size(); index++) {
            double inclusion = inclusionChance(weights, index, draws);
            chances.merge(pool.get(index).effectName(), layoutChance * inclusion, Double::sum);
        }
    }

    static double inclusionChance(List<Double> weights, int targetIndex, int draws) {
        if (draws <= 0 || targetIndex < 0) {
            return 0.0;
        }
        double weightTotal = 0.0;
        for (double weight : weights) {
            weightTotal += weight;
        }
        if (weightTotal <= 0.0) {
            return 0.0;
        }
        double chance = weights.get(targetIndex) / weightTotal;
        if (draws == 1) {
            return chance;
        }
        for (int pickedIndex = 0; pickedIndex < weights.size(); pickedIndex++) {
            if (pickedIndex == targetIndex) {
                continue;
            }
            List<Double> remainingWeights = new ArrayList<>(weights);
            remainingWeights.remove(pickedIndex);
            int remainingTarget = pickedIndex < targetIndex ? targetIndex - 1 : targetIndex;
            chance += weights.get(pickedIndex) / weightTotal * inclusionChance(remainingWeights, remainingTarget, draws - 1);
        }
        return chance;
    }
}
