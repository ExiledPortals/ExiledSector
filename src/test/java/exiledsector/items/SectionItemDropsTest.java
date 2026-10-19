package exiledsector.items;

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

import static exiledsector.items.ItemFixtures.row;
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
        ItemDefinitions.register(new JSONArray()
                .put(row("chip", "subroutine", "HULL_PERCENT:4:6"))
                .put(row("bridge_basic", "bridge", "HULL_PERCENT:4:6"))
                .put(row("emitter_basic", "shield_generator", "HULL_PERCENT:4:6"))
                .put(row("bridge_relic", "bridge", "HULL_PERCENT:4:6").put("unique", "true")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "1;0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "empty").put("chances", "").put("uniqueChance", "0.5")));
    }

    @AfterEach
    void tearDown() {
        ItemDrops.clear();
        ItemDefinitions.clear();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, false);
    }

    @Test
    void sectionItemsOpenAtPlayerLevelFifteen() {
        assertFalse(ItemUnlock.sectionsOpen(14));
        assertTrue(ItemUnlock.sectionsOpen(15));
        assertFalse(ItemUnlock.sectionsOpen((SectorAPI) null));
    }

    @Test
    void frameworkHullItemRollsMatchTheSubroutineChancesAndOnlyDrawSectionBasics() {
        Random random = new Random(5L);
        int[] itemCounts = new int[3];
        int sectionItemCount = 0;
        for (int i = 0; i < TRIALS; i++) {
            ItemDrops.SectionItemLoot loot = ItemDrops.rollSectionItemLoot("station", random, 1f);
            itemCounts[loot.items().size()]++;
            sectionItemCount += loot.upgrades().size();
            for (ItemData item : loot.items()) {
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
        List<ItemData> withoutSectionItems = ItemDrops.roll("station", first, 1f, definition -> true);
        List<ItemData> withSectionItems = ItemDrops.roll("station", second, 1f, definition -> true);
        ItemDrops.rollSectionItemLoot("station", second, 1f);

        assertEquals(withoutSectionItems, withSectionItems);
    }

    @Test
    void basicsArePickedTypeFirstSoEveryTypeWithABasicIsEquallyLikely() {
        Random random = new Random(1L);
        Map<ItemKind, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            counts.merge(ItemDrops.pickSectionItemBasic(random).kind(), 1, Integer::sum);
        }

        assertEquals(Set.of(ItemKind.BRIDGE, ItemKind.SHIELD_GENERATOR), counts.keySet());
        assertEquals(0.5, counts.get(ItemKind.BRIDGE) / (double) TRIALS, 0.02);
        assertNull(ItemDrops.pickSectionItemBasic(ItemKind.PHASE_COIL, random));
    }

    @Test
    void aTargetedSectionBonusAddsOnlyItsOwnTypeByItsPercent() {
        Random random = new Random(3L);
        ItemChances bridgeBonus = new ItemChances(1f, Map.of(ItemKind.BRIDGE, 0.4f));
        Map<ItemKind, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            for (ItemData item : ItemDrops.rollSectionItemLoot("station", random, bridgeBonus).items()) {
                counts.merge(item.definition().kind(), 1, Integer::sum);
            }
        }

        assertEquals(0.75, counts.get(ItemKind.SHIELD_GENERATOR) / (double) TRIALS, 0.03);
        assertEquals(0.75 * 1.4, counts.get(ItemKind.BRIDGE) / (double) TRIALS, 0.03);
    }

    @Test
    void aBonusForATypeThatCannotDropChangesNothing() {
        ItemChances phaseCoilBonus = new ItemChances(1f, Map.of(ItemKind.PHASE_COIL, 2f));

        assertEquals(ItemDrops.rollSectionItemLoot("station", new Random(4L), 1f).items(),
                ItemDrops.rollSectionItemLoot("station", new Random(4L), phaseCoilBonus).items());
        assertFalse(ItemDrops.hasDroppableBasic(ItemKind.PHASE_COIL));
        assertTrue(ItemDrops.hasDroppableBasic(ItemKind.SUBROUTINE));
    }

    @Test
    void aSubroutineBonusAddsSubroutinesOnTopOfTheGeneralRoll() {
        Random random = new Random(6L);
        ItemChances subroutineBonus = new ItemChances(1f, Map.of(ItemKind.SUBROUTINE, 0.5f));
        int subroutines = 0;
        for (int i = 0; i < TRIALS; i++) {
            subroutines += ItemDrops.roll("station", random, subroutineBonus, definition -> true).size();
        }

        assertEquals(1.5 * 1.5, subroutines / (double) TRIALS, 0.04);
        assertEquals(1.5f, ItemDrops.expectedCascadeCount(List.of(1f, 0.5f)), 0.0001f);
        assertEquals(0.75f, ItemDrops.expectedCascadeCount(List.of(0.5f, 0.5f, 0f)), 0.0001f);
    }

    @Test
    void sitesWithoutChancesOrRulesDropNoSectionLoot() {
        assertTrue(ItemDrops.rollSectionItemLoot("empty", new Random(1L), 1f).isEmpty());
        assertTrue(ItemDrops.rollSectionItemLoot("unknown", new Random(1L), 1f).isEmpty());
        assertTrue(ItemDrops.rollSectionItemLoot(null, new Random(1L), 1f).isEmpty());
    }

    @Test
    void flagshipsBelowLevelFifteenNeverGetASection() {
        ShipSkillData shipData = new ShipSkillData();
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            NpcItems.rollSectionItems(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 14, random);
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
            NpcItems.rollSectionItems(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 20, random);
            for (String socketTypeId : shipData.getUnlockedSocketTypeIds()) {
                String itemId = shipData.getSectionItem(socketTypeId);
                if (ItemKind.BRIDGE.id().equals(socketTypeId)) {
                    bridgeSockets++;
                    filledBridgeSockets += itemId == null ? 0 : 1;
                }
                if (itemId != null) {
                    assertEquals(ItemKind.byId(socketTypeId), NpcItems.item(itemId).definition().kind());
                }
            }
        }

        assertEquals(NpcItems.firstChance(20), filledBridgeSockets / (double) bridgeSockets, 0.03);
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
            shipData.installInSection("bridge", NpcItems.id("bridge_basic", 7L));

            NpcItems.claimForPlayer(shipData);

            ItemStore store = ItemStore.get();
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
            shipData.installInSection("bridge", NpcItems.id("bridge_basic", 7L));

            NpcItems.storeForPlayer(shipData);

            assertEquals(List.of("bridge_basic"), ItemStore.get().owned().stream().map(Item::definitionId).toList());
        }
    }
}
