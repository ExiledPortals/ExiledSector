package exiledsector.charts.phasing;

import exiledsector.Seeds;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.crafting.material.MaterialItems;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DisplacementTheorem {

    public static final String ITEM_ID = "exiledSector_displacement_theorem";
    static final String KEY_PREFIX = "phasing.";
    static final int MIN_TIER = 15;
    static final float DROP_CHANCE = 0.3f;
    static final String SEED_PREFIX = "displacementTheorem|";

    private DisplacementTheorem() {
    }

    public static void localise() {
        MaterialItems.localise(ITEM_ID, KEY_PREFIX);
    }

    public static Map<String, Integer> bossDrop(ChartRun run, String rankIdBeforeDefeat, int officerRecordId) {
        if (run == null) {
            return Map.of();
        }
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "|" + officerRecordId);
        return drops(rankIdBeforeDefeat, run.tier(), random) ? Map.of(ITEM_ID, 1) : Map.of();
    }

    static boolean drops(String rankId, int tier, Random random) {
        return tier >= MIN_TIER && isTopRank(rankId) && random.nextFloat() < DROP_CHANCE;
    }

    static boolean isTopRank(String rankId) {
        List<OfficerRank> ranks = OfficerRanks.all();
        return rankId != null && !ranks.isEmpty() && ranks.get(ranks.size() - 1).id().equals(rankId);
    }
}
