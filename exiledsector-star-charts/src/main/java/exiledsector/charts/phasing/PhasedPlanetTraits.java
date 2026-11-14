package exiledsector.charts.phasing;

import exiledsector.items.WeightedPick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public final class PhasedPlanetTraits {

    public static final String PHASE_DISPLACED_CONDITION_ID = "exiledSector_phase_displaced";
    static final int FIRST_MIDDLE_TIER = 6;
    static final int FIRST_HIGH_TIER = 11;

    private static List<PhasedPlanetTrait> traits = List.of();

    private PhasedPlanetTraits() {
    }

    public static List<PhasedPlanetTrait> all() {
        return traits;
    }

    static void use(List<PhasedPlanetTrait> loaded) {
        traits = loaded == null ? List.of() : List.copyOf(loaded);
    }

    static int fewestFor(int tier) {
        if (tier >= FIRST_HIGH_TIER) {
            return 2;
        }
        return tier >= FIRST_MIDDLE_TIER ? 1 : 0;
    }

    static int countFor(int tier, Random random) {
        return fewestFor(tier) + random.nextInt(2);
    }

    public static List<String> roll(int tier, String regionId, Collection<String> anomalyIds, Random random) {
        List<PhasedPlanetTrait> picked = new ArrayList<>();
        int wanted = countFor(tier, random);
        while (picked.size() < wanted) {
            List<PhasedPlanetTrait> candidates = traits.stream()
                    .filter(trait -> !picked.contains(trait) && picked.stream().noneMatch(trait::conflictsWith))
                    .filter(trait -> trait.weightFor(regionId, anomalyIds) > 0f)
                    .toList();
            if (candidates.isEmpty()) {
                break;
            }
            picked.add(WeightedPick.pick(candidates, trait -> trait.weightFor(regionId, anomalyIds), random));
        }
        return picked.stream().map(PhasedPlanetTrait::conditionId).toList();
    }
}
