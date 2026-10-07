package exiledsector.socketables;

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

final class WeightedPick {

    private WeightedPick() {
    }

    static <T> int index(List<T> candidates, ToDoubleFunction<T> weightOf, float roll) {
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

    static <T> T pick(List<T> candidates, ToDoubleFunction<T> weightOf, Random random) {
        return candidates.isEmpty() ? null : candidates.get(index(candidates, weightOf, random.nextFloat()));
    }
}
