package exiledsector.items.modifier;

import exiledsector.data.WeightedPick;
import exiledsector.skills.tags.TagWeights;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public final class ModifierPool {

    private ModifierPool() {
    }

    public static int draw(List<PoolEntry> pool, int count, Collection<String> presentIds, TagWeights tagWeights, Random random,
                           Consumer<PoolEntry> onDrawn) {
        List<String> takenIds = new ArrayList<>(presentIds);
        List<PoolEntry> remaining = new ArrayList<>(pool);
        remaining.removeIf(candidate -> ModifierExclusivity.clashes(candidate.modifier().id(), takenIds));
        int drawnCount = 0;
        for (int drawsLeft = count; drawsLeft > 0 && !remaining.isEmpty(); drawsLeft--) {
            PoolEntry entry = remaining.remove(WeightedPick.index(remaining, candidate -> candidate.biasedWeight(tagWeights), random.nextFloat()));
            onDrawn.accept(entry);
            takenIds.add(entry.modifier().id());
            remaining.removeIf(candidate -> ModifierExclusivity.clashes(candidate.modifier().id(), takenIds));
            drawnCount++;
        }
        return drawnCount;
    }
}
