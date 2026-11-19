package exiledsector.charts.effect;

import exiledsector.ModCsv;
import exiledsector.ProjectFiles;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ChartPrefixRewardsTest {

    private static final String MODIFIERS_PATH = "data/config/exiledSector/star_chart_modifiers.csv";
    private static final int LOW_TIER = 3;
    private static final int MID_TIER = 8;
    private static final int HIGH_TIER = 13;

    @AfterEach
    void tearDown() {
        ChartPrefixRewards.resetForTests();
    }

    @Test
    void aPrefixGrantsEachListedRewardAtItsTierGroupValue() throws Exception {
        ChartPrefixRewards.register(new JSONArray().put(new JSONObject().put("id", "TEST_PREFIX")
                .put("item_rarity", "low=2/mid=7/high=11").put("hull_items", "low=6/mid=17/high=29")));

        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 2f), new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 6f)),
                ChartPrefixRewards.rewards("TEST_PREFIX", LOW_TIER));
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 11f), new AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 29f)),
                ChartPrefixRewards.rewards("TEST_PREFIX", HIGH_TIER));
    }

    @Test
    void aRewardWithoutAValueForTheChartsTierGroupGrantsNothing() throws Exception {
        ChartPrefixRewards.register(new JSONArray().put(new JSONObject().put("id", "TEST_PREFIX").put("star_charts", "mid=18/high=30")));

        assertEquals(List.of(), ChartPrefixRewards.rewards("TEST_PREFIX", LOW_TIER));
        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_STAR_CHART_PERCENT, 18f)), ChartPrefixRewards.rewards("TEST_PREFIX", MID_TIER));
    }

    @Test
    void unlistedPrefixesAndUnreadableCellsGrantNothing() throws Exception {
        ChartPrefixRewards.register(new JSONArray().put(new JSONObject().put("id", "TEST_PREFIX").put("catalysts", "high=lots")
                .put("fleet_size", "high=15")));

        assertEquals(List.of(new AppliedEffect(ChartModifier.CHART_FLEET_SIZE_PERCENT, 15f)), ChartPrefixRewards.rewards("TEST_PREFIX", HIGH_TIER));
        assertEquals(List.of(), ChartPrefixRewards.rewards("UNLISTED_PREFIX", HIGH_TIER));
        assertEquals(List.of(), ChartPrefixRewards.rewards(null, HIGH_TIER));
    }

    @Test
    void everyShippedStarChartPrefixHasARewardAtEveryTierGroupItRollsIn() throws Exception {
        ChartPrefixRewards.register(ProjectFiles.csvRows(ChartPrefixRewards.PATH));
        Set<String> prefixIds = new LinkedHashSet<>();
        JSONArray modifiers = ProjectFiles.csvRows(MODIFIERS_PATH);
        List<String> unrewarded = new ArrayList<>();
        ModCsv.forEach(modifiers, (index, row) -> {
            String modifierId = ModCsv.text(row, "id");
            if (!"prefix".equals(ModCsv.text(row, "affix")) || !ModCsv.text(row, "scopes").contains("star_chart")) {
                return;
            }
            prefixIds.add(modifierId);
            String groups = ModCsv.text(row, "lines");
            for (String group : List.of("low", "mid", "high")) {
                int tier = "low".equals(group) ? LOW_TIER : "mid".equals(group) ? MID_TIER : HIGH_TIER;
                if (groups.contains(group + "=") && ChartPrefixRewards.rewards(modifierId, tier).isEmpty()) {
                    unrewarded.add(modifierId + " at " + group);
                }
            }
        });

        assertFalse(prefixIds.isEmpty());
        assertEquals(List.of(), unrewarded, "add a reward for these in " + ChartPrefixRewards.PATH);
        List<String> strays = new ArrayList<>();
        ModCsv.forEach(ProjectFiles.csvRows(ChartPrefixRewards.PATH), (index, row) -> {
            if (!prefixIds.contains(ModCsv.text(row, "id"))) {
                strays.add(ModCsv.text(row, "id"));
            }
        });
        assertEquals(List.of(), strays, "these rows in " + ChartPrefixRewards.PATH + " name no Star Chart prefix");
    }
}
