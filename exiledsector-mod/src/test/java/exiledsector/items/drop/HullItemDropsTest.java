package exiledsector.items.drop;

import com.fs.starfarer.api.campaign.SectorAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemFixtures;
import exiledsector.items.ItemKind;
import exiledsector.items.hull.HullUpgradesSetting;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static exiledsector.items.HullItemRows.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.subroutineRow("chip"))
                .put(row("bridge_basic", "bridge", "HULL_PERCENT:4:6"))
                .put(row("emitter_basic", "shield_generator", "HULL_PERCENT:4:6"))
                .put(row("bridge_relic", "bridge", "HULL_PERCENT:4:6").put("unique", "true")));
        SalvageSiteDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "1;0.5").put("hullItemUniqueChance", "0"))
                .put(new JSONObject().put("site", "empty").put("chances", "").put("hullItemUniqueChance", "0.5")));
    }

    @AfterEach
    void tearDown() {
        SalvageSiteDrops.clear();
        ItemDefinitions.clear();
    }

    @Test
    void hullItemsOpenAtPlayerLevelFifteen() {
        assertFalse(HullUpgradesSetting.hullItemDropsOpen(14));
        assertTrue(HullUpgradesSetting.hullItemDropsOpen(15));
        assertFalse(HullUpgradesSetting.hullItemDropsOpen((SectorAPI) null));
    }

    @Test
    void hullItemRollsKeepTheirSalvageShareOfTheSiteCascadeAndOnlyDrawHullItemBasics() {
        Random random = new Random(5L);
        int[] itemCounts = new int[3];
        int hullItemCount = 0;
        for (int i = 0; i < TRIALS; i++) {
            Loot loot = HullItemDrops.rollHullItemLoot("station", random, ItemChances.BASE);
            itemCounts[loot.items().size()]++;
            hullItemCount += loot.schematics().size();
            for (ItemData item : loot.items()) {
                assertTrue(Set.of("bridge_basic", "emitter_basic").contains(item.definitionId()), item.definitionId());
            }
        }

        assertEquals(0.375, itemCounts[0] / (double) TRIALS, 0.02);
        assertEquals(0.5, itemCounts[1] / (double) TRIALS, 0.02);
        assertEquals(0.125, itemCounts[2] / (double) TRIALS, 0.02);
        assertEquals(TRIALS, hullItemCount);
    }

    @Test
    void frameworkRollsLeaveTheSiteRollUntouched() {
        Random first = new Random(9L);
        Random second = new Random(9L);
        List<ItemData> withoutHullItems = SalvageSiteDrops.roll("station", first, ItemChances.BASE, definition -> true);
        List<ItemData> withHullItems = SalvageSiteDrops.roll("station", second, ItemChances.BASE, definition -> true);
        HullItemDrops.rollHullItemLoot("station", second, ItemChances.BASE);

        assertEquals(withoutHullItems, withHullItems);
    }

    @Test
    void basicsArePickedTypeFirstSoEveryTypeWithABasicIsEquallyLikely() {
        Random random = new Random(1L);
        Map<ItemKind, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            counts.merge(DroppableDefinitions.pickHullItemBasic(random).kind(), 1, Integer::sum);
        }

        assertEquals(Set.of(ItemKind.BRIDGE, ItemKind.SHIELD_GENERATOR), counts.keySet());
        assertEquals(0.5, counts.get(ItemKind.BRIDGE) / (double) TRIALS, 0.02);
        assertNull(DroppableDefinitions.pickBasicOfKind(ItemKind.PHASE_COIL, random));
    }

    @Test
    void aSectionsQuantityAddsOnlyItsOwnTypeByItsPercent() {
        Random random = new Random(3L);
        ItemChances bridgeBonus = ItemChances.increasedBy(DropType.BRIDGE, 40f);
        Map<ItemKind, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            for (ItemData item : HullItemDrops.rollHullItemLoot("station", random, bridgeBonus).items()) {
                counts.merge(item.definition().kind(), 1, Integer::sum);
            }
        }

        assertEquals(0.375, counts.get(ItemKind.SHIELD_GENERATOR) / (double) TRIALS, 0.03);
        assertEquals(0.375 * 1.4, counts.get(ItemKind.BRIDGE) / (double) TRIALS, 0.03);
    }

    @Test
    void aBonusForATypeThatCannotDropChangesNothing() {
        ItemChances phaseCoilBonus = ItemChances.increasedBy(DropType.PHASE_COIL, 200f);

        assertEquals(HullItemDrops.rollHullItemLoot("station", new Random(4L), ItemChances.BASE).items(),
                HullItemDrops.rollHullItemLoot("station", new Random(4L), phaseCoilBonus).items());
        assertFalse(DroppableDefinitions.hasDroppableBasic(ItemKind.PHASE_COIL));
    }

    @Test
    void theExpectedCascadeCountSumsTheChanceOfReachingEachStep() {
        assertEquals(1.5f, SalvageSiteDrops.expectedCascadeCount(List.of(1f, 0.5f)), 0.0001f);
        assertEquals(0.75f, SalvageSiteDrops.expectedCascadeCount(List.of(0.5f, 0.5f, 0f)), 0.0001f);
    }

    @Test
    void sitesWithoutChancesOrRulesDropNoHullItemLoot() {
        assertTrue(HullItemDrops.rollHullItemLoot("empty", new Random(1L), ItemChances.BASE).isEmpty());
        assertTrue(HullItemDrops.rollHullItemLoot("unknown", new Random(1L), ItemChances.BASE).isEmpty());
        assertTrue(HullItemDrops.rollHullItemLoot(null, new Random(1L), ItemChances.BASE).isEmpty());
    }

}
