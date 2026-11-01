package exiledsector.items;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

public final class WeightedPick {

    private WeightedPick() {
    }

    public static <T> int index(List<T> candidates, ToDoubleFunction<T> weightOf, float roll) {
        float weightTotal = 0f;
        for (T candidate : candidates) {
            weightTotal += (float) weightOf.applyAsDouble(candidate);
        }
        float remainingWeight = roll * weightTotal;
        for (int i = 0; i < candidates.size(); i++) {
            remainingWeight -= (float) weightOf.applyAsDouble(candidates.get(i));
            if (remainingWeight < 0f) {
                return i;
            }
        }
        return candidates.size() - 1;
    }

    public static <T> T pick(List<T> candidates, ToDoubleFunction<T> weightOf, Random random) {
        return candidates.isEmpty() ? null : candidates.get(index(candidates, weightOf, random.nextFloat()));
    }

    public static <K> K pick(Map<K, Float> weights, Random random) {
        List<Map.Entry<K, Float>> positive = weights.entrySet().stream().filter(entry -> entry.getValue() > 0f).toList();
        Map.Entry<K, Float> picked = pick(positive, Map.Entry::getValue, random);
        return picked == null ? null : picked.getKey();
    }
}
