package exiledsector.charts.officer.reward;

import exiledsector.charts.ChartTables;
import exiledsector.i18n.RealCatalogue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardGroupTest {

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
    }

    @Test
    void theGroupsKeepTheirShownOrder() {
        assertEquals(List.of(RewardGroup.LOW_CRAFTING, RewardGroup.MID_CRAFTING, RewardGroup.HIGH_CRAFTING, RewardGroup.INHIBITOR,
                RewardGroup.ENGRAM, RewardGroup.FEEDSTOCK, RewardGroup.STAR_CHART, RewardGroup.HULL_ITEM, RewardGroup.BIOME_HULL_ITEM,
                RewardGroup.UNIQUE), Arrays.asList(RewardGroup.values()));
    }

    @Test
    void everyKindHasAGroup() {
        for (RewardKind kind : RewardKind.values()) {
            RewardGroup group = RewardGroup.of(CarriedItem.of(kind, "not_in_the_table"));

            assertNotNull(group, kind.name());
            assertEquals(kind, group.kind());
        }
        assertEquals(RewardGroup.FEEDSTOCK, RewardGroup.of(CarriedItem.of(RewardKind.FEEDSTOCK, "feedstock")));
        assertEquals(RewardGroup.STAR_CHART, RewardGroup.of(CarriedItem.of(RewardKind.STAR_CHART, "chart")));
    }

    @Test
    void everyShippedRewardRowFallsIntoTheGroupOfItsCategoryAndTier() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        List<String> classified = new ArrayList<>();
        for (RewardKind kind : List.of(RewardKind.CRAFTING_ITEM, RewardKind.INHIBITOR, RewardKind.FEEDSTOCK)) {
            for (String itemId : OfficerRewardTable.itemsIn(kind)) {
                RewardGroup group = RewardGroup.of(CarriedItem.of(kind, itemId));
                assertTrue(group.holds(kind, OfficerRewardTable.tierOf(itemId)), itemId + " in " + group);
                classified.add(itemId);
            }
        }

        assertEquals(25, classified.size());
        assertEquals(RewardGroup.LOW_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_augmentation_nanoswarm"));
        assertEquals(RewardGroup.LOW_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_erasure_cipher"));
        assertEquals(RewardGroup.MID_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_fundamental_nanoswarm"));
        assertEquals(RewardGroup.MID_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_cartographer_cipher"));
        assertEquals(RewardGroup.HIGH_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_replicator_nanoseed"));
        assertEquals(RewardGroup.HIGH_CRAFTING, groupOf(RewardKind.CRAFTING_ITEM, "exiledSector_beam_inhibitor"));
        assertEquals(RewardGroup.INHIBITOR, groupOf(RewardKind.INHIBITOR, "exiledSector_prefix_inhibitor"));
        assertEquals(RewardGroup.INHIBITOR, groupOf(RewardKind.INHIBITOR, "exiledSector_suffix_inhibitor"));
        assertEquals(RewardGroup.FEEDSTOCK, groupOf(RewardKind.FEEDSTOCK, "exiledSector_nanite_feedstock"));
        assertEquals(RewardGroup.FEEDSTOCK, groupOf(RewardKind.FEEDSTOCK, "exiledSector_phase_feedstock"));
    }

    private static RewardGroup groupOf(RewardKind kind, String itemId) {
        return RewardGroup.of(CarriedItem.of(kind, itemId));
    }

    @Test
    void entriesCountRewardsPickedUpSeparatelyNotTheHiddenCountsInside() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        CarriedItem seed = CarriedItem.of(RewardKind.CRAFTING_ITEM, "exiledSector_replicator_nanoseed");
        CarriedItem swarm = new CarriedItem(RewardKind.CRAFTING_ITEM, "exiledSector_augmentation_nanoswarm", null, 5);
        CarriedItem feedstock = new CarriedItem(RewardKind.FEEDSTOCK, "exiledSector_nanite_feedstock", null, 60);
        CarriedItem bridge = CarriedItem.of(RewardKind.HULL_ITEM, "bridge");

        Map<RewardGroup, Integer> counts = RewardGroup.entryCounts(List.of(bridge, seed, swarm, feedstock, seed, bridge.withCount(2)));

        assertEquals(List.of(RewardGroup.LOW_CRAFTING, RewardGroup.HIGH_CRAFTING, RewardGroup.FEEDSTOCK, RewardGroup.HULL_ITEM),
                List.copyOf(counts.keySet()));
        assertEquals(1, counts.get(RewardGroup.LOW_CRAFTING));
        assertEquals(2, counts.get(RewardGroup.HIGH_CRAFTING));
        assertEquals(1, counts.get(RewardGroup.FEEDSTOCK));
        assertEquals(3, counts.get(RewardGroup.HULL_ITEM));
        assertTrue(RewardGroup.entryCounts(List.of()).isEmpty());
    }

    @Test
    void everyGroupHasAnEnglishAndChineseLabel() {
        for (String language : List.of("en", "zh_CN")) {
            Map<String, String> catalogue = RealCatalogue.entries(language);
            for (RewardGroup group : RewardGroup.values()) {
                assertTrue(catalogue.containsKey(group.labelKey()), language + " " + group.labelKey());
            }
            assertTrue(catalogue.containsKey("chart.officer.reward.groupCount"), language);
        }
    }
}
