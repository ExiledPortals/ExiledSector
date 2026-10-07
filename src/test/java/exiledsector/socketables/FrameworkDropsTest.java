package exiledsector.socketables;

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

import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrameworkDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(row("chip", "subroutine", "HULL_PERCENT:4:6"))
                .put(row("bridge_basic", "bridge", "HULL_PERCENT:4:6"))
                .put(row("emitter_basic", "shield_generator", "HULL_PERCENT:4:6"))
                .put(row("bridge_relic", "bridge", "HULL_PERCENT:4:6").put("unique", "true")));
        SocketableDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "1;0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "empty").put("chances", "").put("uniqueChance", "0.5")));
    }

    @AfterEach
    void tearDown() {
        SocketableDrops.clear();
        SocketableDefinitions.clear();
        SocketableStore.clearNpcFrameworkPreviews();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, false);
    }

    @Test
    void frameworksOpenAtPlayerLevelFifteen() {
        assertFalse(SocketableUnlock.frameworksOpen(14));
        assertTrue(SocketableUnlock.frameworksOpen(15));
        assertFalse(SocketableUnlock.frameworksOpen((SectorAPI) null));
    }

    @Test
    void frameworkSocketableRollsMatchTheSubroutineChancesAndOnlyDrawFrameworkBasics() {
        Random random = new Random(5L);
        int[] itemCounts = new int[3];
        int frameworkCount = 0;
        for (int i = 0; i < TRIALS; i++) {
            SocketableDrops.FrameworkLoot loot = SocketableDrops.rollFrameworkLoot("station", random, 1f);
            itemCounts[loot.items().size()]++;
            frameworkCount += loot.frameworks().size();
            for (SocketableItemData item : loot.items()) {
                assertTrue(Set.of("bridge_basic", "emitter_basic").contains(item.definitionId()), item.definitionId());
            }
        }

        assertEquals(0, itemCounts[0]);
        assertEquals(0.5, itemCounts[1] / (double) TRIALS, 0.02);
        assertEquals(0.5, itemCounts[2] / (double) TRIALS, 0.02);
        assertEquals(TRIALS, frameworkCount);
    }

    @Test
    void frameworkRollsLeaveTheSubroutineRollUntouched() {
        Random first = new Random(9L);
        Random second = new Random(9L);
        List<SocketableItemData> withoutFrameworks = SocketableDrops.roll("station", first, 1f, definition -> true);
        List<SocketableItemData> withFrameworks = SocketableDrops.roll("station", second, 1f, definition -> true);
        SocketableDrops.rollFrameworkLoot("station", second, 1f);

        assertEquals(withoutFrameworks, withFrameworks);
    }

    @Test
    void basicsArePickedTypeFirstSoEveryTypeWithABasicIsEquallyLikely() {
        Random random = new Random(1L);
        Map<SocketType, Integer> counts = new HashMap<>();
        for (int i = 0; i < TRIALS; i++) {
            counts.merge(SocketableDrops.pickFrameworkBasic(random).kind(), 1, Integer::sum);
        }

        assertEquals(Set.of(SocketType.BRIDGE, SocketType.SHIELD_GENERATOR), counts.keySet());
        assertEquals(0.5, counts.get(SocketType.BRIDGE) / (double) TRIALS, 0.02);
        assertNull(SocketableDrops.pickFrameworkBasic(SocketType.PHASE_COIL, random));
    }

    @Test
    void sitesWithoutChancesOrRulesDropNoFrameworkLoot() {
        assertTrue(SocketableDrops.rollFrameworkLoot("empty", new Random(1L), 1f).isEmpty());
        assertTrue(SocketableDrops.rollFrameworkLoot("unknown", new Random(1L), 1f).isEmpty());
        assertTrue(SocketableDrops.rollFrameworkLoot(null, new Random(1L), 1f).isEmpty());
    }

    @Test
    void flagshipsBelowLevelFifteenNeverGetAFramework() {
        ShipSkillData shipData = new ShipSkillData();
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            NpcSocketables.rollFramework(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 14, random);
        }

        assertNull(shipData.getInstalledFrameworkId());
    }

    @Test
    void halfOfFlagshipsGetAFrameworkThatOnlyRollsTypesTheirFitAllows() {
        Random random = new Random(3L);
        int installed = 0;
        Set<SocketType> seenTypes = EnumSet.noneOf(SocketType.class);
        for (int i = 0; i < TRIALS; i++) {
            ShipSkillData shipData = new ShipSkillData();
            NpcSocketables.rollFramework(shipData, HullSize.CRUISER, () -> fit(ShieldType.NONE), 20, random);
            HullFrameworkData framework = NpcSocketables.frameworkCarriedBy(shipData);
            if (framework != null) {
                installed++;
                assertEquals(HullSize.CRUISER, framework.hullSize());
                seenTypes.addAll(framework.socketTypes());
            }
        }

        assertEquals(0.5, installed / (double) TRIALS, 0.02);
        assertFalse(seenTypes.contains(SocketType.SHIELD_GENERATOR));
        assertFalse(seenTypes.contains(SocketType.PHASE_COIL));
        assertFalse(seenTypes.contains(SocketType.FLIGHT_DECK));
        assertTrue(seenTypes.contains(SocketType.BRIDGE));
    }

    @Test
    void eachFlagshipSocketRollsTheNpcSocketableChanceForABasicOfItsType() {
        Random random = new Random(4L);
        int bridgeSockets = 0;
        int filledBridgeSockets = 0;
        for (int i = 0; i < TRIALS; i++) {
            ShipSkillData shipData = new ShipSkillData();
            NpcSocketables.rollFramework(shipData, HullSize.CRUISER, () -> fit(ShieldType.FRONT), 20, random);
            HullFrameworkData framework = NpcSocketables.frameworkCarriedBy(shipData);
            for (int slotIndex = 0; framework != null && slotIndex < framework.socketTypes().size(); slotIndex++) {
                String socketableId = shipData.getFrameworkSocketedItem(slotIndex);
                if (framework.socketTypes().get(slotIndex) == SocketType.BRIDGE) {
                    bridgeSockets++;
                    filledBridgeSockets += socketableId == null ? 0 : 1;
                }
                if (socketableId != null) {
                    assertEquals(framework.socketTypes().get(slotIndex), NpcSocketables.item(socketableId).definition().kind());
                }
            }
        }

        assertEquals(NpcSocketables.firstChance(20), filledBridgeSockets / (double) bridgeSockets, 0.03);
    }

    @Test
    void aCapturedFlagshipKeepsItsFrameworkAndItemsAsPlayerOwnedOnes() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            ShipSkillData shipData = new ShipSkillData();
            HullFrameworkData framework = new HullFrameworkData(HullSize.CRUISER, SocketableRarity.COMMON,
                    List.of(SocketType.REACTOR, SocketType.BRIDGE), 6L);
            shipData.installFramework(framework.npcId());
            shipData.socketFrameworkItem(1, NpcSocketables.id("bridge_basic", 7L));

            NpcSocketables.claimForPlayer(shipData);

            SocketableStore store = SocketableStore.get();
            assertEquals(1, store.frameworks().size());
            assertEquals(store.frameworks().get(0).id(), shipData.getInstalledFrameworkId());
            assertEquals(framework, store.frameworks().get(0).data());
            assertEquals(Map.of(1, store.owned().get(0).id()), shipData.getFrameworkSocketedItems());
        }
    }

    @Test
    void aFlagshipThatCannotBeClaimedHandsItsFrameworkAndItemsToStorage() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            ShipSkillData shipData = new ShipSkillData();
            shipData.installFramework(new HullFrameworkData(HullSize.CRUISER, SocketableRarity.COMMON,
                    List.of(SocketType.REACTOR, SocketType.BRIDGE), 6L).npcId());
            shipData.socketFrameworkItem(1, NpcSocketables.id("bridge_basic", 7L));

            NpcSocketables.storeForPlayer(shipData);

            assertEquals(1, SocketableStore.get().frameworks().size());
            assertEquals(List.of("bridge_basic"), SocketableStore.get().owned().stream().map(Socketable::definitionId).toList());
        }
    }
}
