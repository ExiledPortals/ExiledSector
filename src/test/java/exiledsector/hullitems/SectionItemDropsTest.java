package exiledsector.hullitems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static exiledsector.hullitems.HullItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectionItemDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        HullItemDefinitions.register(new JSONArray()
                .put(row("chip", "subroutine", "HULL_PERCENT:4:6"))
                .put(row("bridge_basic", "bridge", "HULL_PERCENT:4:6"))
                .put(row("emitter_basic", "shield_generator", "HULL_PERCENT:4:6"))
                .put(row("bridge_relic", "bridge", "HULL_PERCENT:4:6").put("unique", "true")));
        HullItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "1;0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "empty").put("chances", "").put("uniqueChance", "0.5")));
    }

    @AfterEach
    void tearDown() {
        HullItemDrops.clear();
        HullItemDefinitions.clear();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, false);
    }

    @Test
    void sectionItemsOpenAtPlayerLevelFifteen() {
        assertFalse(HullItemUnlock.sectionsOpen(14));
        assertTrue(HullItemUnlock.sectionsOpen(15));
        assertFalse(HullItemUnlock.sectionsOpen((SectorAPI) null));
    }

    @Test
    void frameworkHullItemRollsMatchTheSubroutineChancesAndOnlyDrawSectionBasics() {
        Random random = new Random(5L);
        int[] itemCounts = new int[3];
        int sectionItemCount = 0;
        for (int i = 0; i < TRIALS; i++) {
            HullItemDrops.SectionItemLoot loot = HullItemDrops.rollSectionItemLoot("station", random, 1f);
            itemCounts[loot.items().size()]++;
            sectionItemCount += loot.upgrades().size();
            for (HullItemData item : loot.items()) {
                assertTrue(Set.of("bridge_basic", "emitter_basic").contains(item.definitionId()), item.definitionId());
            }
        }

        assertEquals(0, itemCounts[0]);
        assertEquals(0.5, itemCounts[1] / (double) TRIALS, 0.02);
        assertEquals(0.5, itemCounts[2] / (double) TRIALS, 0.02);
        assertEquals(TRIALS, sectionItemCount);
    }

    @Test
    void frameworkRollsLeaveTheSubroutineRollUntouched() {
        Random first = new Random(9L);
        Random second = new Random(9L);
        List<HullItemData> withoutSectionItems = HullItemDrops.roll("station", first, 1f, definition -> true);
        List<HullItemData> withSectionItems = HullItemDrops.roll("station", second, 1f, definition -> true);
        HullItemDrops.rollSectionItemLoot("station", second, 1f);

        assertEquals(withoutSectionItems, withSectionItems);
    }

    @Test
    void basicsArePickedTypeFirstSoEveryTypeWithABasicIsEquallyLikely() {
        Random random = new Random(1L);
        Map<HullItemType, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            counts.merge(HullItemDrops.pickSectionItemBasic(random).kind(), 1, Integer::sum);
        }

        assertEquals(Set.of(HullItemType.BRIDGE, HullItemType.SHIELD_GENERATOR), counts.keySet());
        assertEquals(0.5, counts.get(HullItemType.BRIDGE) / (double) TRIALS, 0.02);
        assertNull(HullItemDrops.pickSectionItemBasic(HullItemType.PHASE_COIL, random));
    }

    @Test
    void sitesWithoutChancesOrRulesDropNoSectionLoot() {
        assertTrue(HullItemDrops.rollSectionItemLoot("empty", new Random(1L), 1f).isEmpty());
        assertTrue(HullItemDrops.rollSectionItemLoot("unknown", new Random(1L), 1f).isEmpty());
        assertTrue(HullItemDrops.rollSectionItemLoot(null, new Random(1L), 1f).isEmpty());
    }

    @Test
    void flagshipsBelowLevelFifteenNeverGetASection() {
        ShipSkillData shipData = new ShipSkillData();
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            NpcHullItems.rollSectionItems(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 14, random);
        }

        assertEquals(List.of(), shipData.getUnlockedSocketTypeIds());
    }

    @Test
    void eachFlagshipSocketRollsTheNpcHullItemChanceForABasicOfItsType() {
        Random random = new Random(4L);
        int bridgeSockets = 0;
        int filledBridgeSockets = 0;
        for (int i = 0; i < TRIALS; i++) {
            ShipSkillData shipData = new ShipSkillData();
            NpcHullItems.rollSectionItems(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 20, random);
            for (String socketTypeId : shipData.getUnlockedSocketTypeIds()) {
                String hullItemId = shipData.getSectionItem(socketTypeId);
                if (HullItemType.BRIDGE.id().equals(socketTypeId)) {
                    bridgeSockets++;
                    filledBridgeSockets += hullItemId == null ? 0 : 1;
                }
                if (hullItemId != null) {
                    assertEquals(HullItemType.byId(socketTypeId), NpcHullItems.item(hullItemId).definition().kind());
                }
            }
        }

        assertEquals(NpcHullItems.firstChance(20), filledBridgeSockets / (double) bridgeSockets, 0.03);
    }

    @Test
    void aCapturedFlagshipKeepsItsUnlockedSocketsAndItsItemsBecomePlayerOwned() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            ShipSkillData shipData = new ShipSkillData();
            shipData.grantUnlockedSocketType("reactor");
            shipData.grantUnlockedSocketType("bridge");
            shipData.installInSection("bridge", NpcHullItems.id("bridge_basic", 7L));

            NpcHullItems.claimForPlayer(shipData);

            HullItemStore store = HullItemStore.get();
            assertEquals(List.of("reactor", "bridge"), shipData.getUnlockedSocketTypeIds());
            assertEquals(2, shipData.getHullUpgradePoints());
            assertEquals(Map.of("bridge", store.owned().get(0).id()), shipData.getSectionItems());
        }
    }

    @Test
    void aFlagshipThatCannotBeClaimedHandsItsItemsToStorage() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            ShipSkillData shipData = new ShipSkillData();
            shipData.grantUnlockedSocketType("bridge");
            shipData.installInSection("bridge", NpcHullItems.id("bridge_basic", 7L));

            NpcHullItems.storeForPlayer(shipData);

            assertEquals(List.of("bridge_basic"), HullItemStore.get().owned().stream().map(HullItem::definitionId).toList());
        }
    }
}
