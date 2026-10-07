package exiledsector.skills.progression;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipLevelSystemTest {

    private static final float XP_BASE = 100f;
    private static final float XP_GROWTH = 2f;
    private static final int NO_GROWTH_CUTOFF = 50;
    private static final int OP_COST_PER_NODE = 3;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearTypes();
    }

    private static ShipLevelSystem.LevelCurve curve(int growthCutoffLevel, int maxLevel) {
        return new ShipLevelSystem.LevelCurve(XP_BASE, XP_GROWTH, growthCutoffLevel, maxLevel);
    }

    private static SkillNode node(String id) {
        SkillType type = new SkillType.Builder(id, id, "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    @Test
    void difficultyOnlyEverAddsXpByTheBonusAboveOneTimesTheStrengthUpToTheCap() {
        assertEquals(1f, ShipLevelSystem.difficultyMultiplier(0.5f, 1f, 6f));
        assertEquals(2.5f, ShipLevelSystem.difficultyMultiplier(2.5f, 1f, 6f));
        assertEquals(2f, ShipLevelSystem.difficultyMultiplier(3f, 0.5f, 6f));
        assertEquals(2f, ShipLevelSystem.difficultyMultiplier(4f, 1f, 2f));
        assertEquals(1f, ShipLevelSystem.difficultyMultiplier(4f, 0f, 6f));
    }

    @Test
    void xpToReachNextLevelGrowsExponentiallyWithLevel() {
        assertEquals(100f, ShipLevelSystem.xpToReachNextLevel(0, XP_BASE, XP_GROWTH, NO_GROWTH_CUTOFF));
        assertEquals(200f, ShipLevelSystem.xpToReachNextLevel(1, XP_BASE, XP_GROWTH, NO_GROWTH_CUTOFF));
        assertEquals(400f, ShipLevelSystem.xpToReachNextLevel(2, XP_BASE, XP_GROWTH, NO_GROWTH_CUTOFF));
    }

    @Test
    void xpToReachNextLevelLocksToTheCutoffTransitionCostFromThatLevelOnward() {
        assertEquals(400f, ShipLevelSystem.xpToReachNextLevel(2, XP_BASE, XP_GROWTH, 3));
        assertEquals(400f, ShipLevelSystem.xpToReachNextLevel(3, XP_BASE, XP_GROWTH, 3));
        assertEquals(400f, ShipLevelSystem.xpToReachNextLevel(10, XP_BASE, XP_GROWTH, 3));
    }

    @Test
    void xpToReachNextLevelBeforeTheCutoffIsUnaffected() {
        assertEquals(100f, ShipLevelSystem.xpToReachNextLevel(0, XP_BASE, XP_GROWTH, 3));
        assertEquals(200f, ShipLevelSystem.xpToReachNextLevel(1, XP_BASE, XP_GROWTH, 3));
    }

    @Test
    void aNonFiniteDifficultyInputFallsBackToNoBonus() {
        assertEquals(1f, ShipLevelSystem.difficultyMultiplier(Float.NaN, 1f, 6f));
        assertEquals(1f, ShipLevelSystem.difficultyMultiplier(0.5f, Float.POSITIVE_INFINITY, 6f));
        assertEquals(1f, ShipLevelSystem.difficultyMultiplier(3f, 1f, Float.NaN));
    }

    @Test
    void aNonFiniteXpAwardIsIgnoredSoItCannotCorruptTheShipsLevel() {
        ShipSkillData data = new ShipSkillData();
        data.addXp(10f);

        ShipLevelSystem.awardXp(data, Float.NaN, curve(NO_GROWTH_CUTOFF, 50), List.of());
        ShipLevelSystem.awardXp(data, Float.POSITIVE_INFINITY, curve(NO_GROWTH_CUTOFF, 50), List.of());

        assertEquals(0, data.getLevel());
        assertEquals(10f, data.getXp());
    }

    @Test
    void awardXpBelowTheThresholdOnlyAccumulatesXpWithoutLevelingUp() {
        ShipSkillData data = new ShipSkillData();

        ShipLevelSystem.awardXp(data, 50f, curve(NO_GROWTH_CUTOFF, 50), List.of());

        assertEquals(0, data.getLevel());
        assertEquals(50f, data.getXp());
    }

    @Test
    void awardXpAtTheThresholdLevelsUpAndConvertsTheMostRecentAllocationToFree() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a");
        data.allocate(a, OP_COST_PER_NODE);

        ShipLevelSystem.awardXp(data, XP_BASE, curve(NO_GROWTH_CUTOFF, 50), List.of(a));

        assertEquals(1, data.getLevel());
        assertEquals(0f, data.getXp());
        assertTrue(data.isFreeNode("a"));
        assertEquals(0, data.getSpentOp(OP_COST_PER_NODE));
    }

    @Test
    void awardXpBanksACreditWhenNothingIsEligibleToConvert() {
        ShipSkillData data = new ShipSkillData();

        ShipLevelSystem.awardXp(data, XP_BASE, curve(NO_GROWTH_CUTOFF, 50), List.of());

        assertEquals(1, data.getLevel());
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void awardXpCanTriggerMultipleLevelUpsFromASingleAward() {
        ShipSkillData data = new ShipSkillData();
        SkillNode a = node("a");
        SkillNode b = node("b");
        data.allocate(a, OP_COST_PER_NODE);
        data.allocate(b, OP_COST_PER_NODE);

        ShipLevelSystem.awardXp(data, 300f, curve(NO_GROWTH_CUTOFF, 50), List.of(a, b));

        assertEquals(2, data.getLevel());
        assertEquals(0f, data.getXp());
        assertTrue(data.isFreeNode("a"));
        assertTrue(data.isFreeNode("b"));
    }

    @Test
    void awardXpUsesTheFlatCutoffCostPastTheCutoffLevel() {
        ShipSkillData data = new ShipSkillData();

        ShipLevelSystem.awardXp(data, 100f + 200f + 400f, curve(3, 50), List.of());
        assertEquals(3, data.getLevel());
        assertEquals(0f, data.getXp());

        ShipLevelSystem.awardXp(data, 400f, curve(3, 50), List.of());
        assertEquals(4, data.getLevel());
        assertEquals(0f, data.getXp());
    }

    @Test
    void awardXpDoesNotLevelPastTheConfiguredMaxLevel() {
        ShipSkillData data = new ShipSkillData();

        ShipLevelSystem.awardXp(data, 100000f, curve(NO_GROWTH_CUTOFF, 2), List.of());

        assertEquals(2, data.getLevel());
    }

    @Test
    void awardXpDoesNothingWhenAlreadyAtMaxLevel() {
        ShipSkillData data = new ShipSkillData();
        ShipLevelSystem.awardXp(data, XP_BASE, curve(NO_GROWTH_CUTOFF, 1), List.of());
        assertEquals(1, data.getLevel());
        assertEquals(0f, data.getXp());

        ShipLevelSystem.awardXp(data, 50f, curve(NO_GROWTH_CUTOFF, 1), List.of());

        assertEquals(1, data.getLevel());
        assertEquals(0f, data.getXp());
    }

    @Test
    void awardXpToFleetAwardsXpToEveryMember() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
            globalMock.when(Global::getSector).thenReturn(sector);

            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
            FleetDataAPI fleetData = mock(FleetDataAPI.class);
            when(fleet.getFleetData()).thenReturn(fleetData);
            FleetMemberAPI memberA = mockMember("ship-a");
            FleetMemberAPI memberB = mockMember("ship-b");
            when(fleetData.getMembersListCopy()).thenReturn(List.of(memberA, memberB));

            ShipLevelSystem.awardXpToFleet(fleet, 40f);

            assertEquals(40f, ShipSkillDataManager.get("ship-a").getXp());
            assertEquals(40f, ShipSkillDataManager.get("ship-b").getXp());
        }
    }

    @Test
    void awardXpToMemberAwardsXpToThatShipOnly() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
            globalMock.when(Global::getSector).thenReturn(sector);

            ShipLevelSystem.awardXpToMember(mockMember("ship-a"), 40f);

            assertEquals(40f, ShipSkillDataManager.get("ship-a").getXp());
            assertEquals(0f, ShipSkillDataManager.get("ship-b").getXp());
        }
    }

    @Test
    void awardXpToFleetDoesNothingWhenFleetIsNull() {
        assertDoesNotThrow(() -> ShipLevelSystem.awardXpToFleet(null, 40f));
    }

    @Test
    void shipsBelowTheFleetsHighestLevelShipEarnCatchUpXp() {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);

        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
            lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
            globalMock.when(Global::getSector).thenReturn(sector);
            ShipLevelSystem.raiseToLevel(ShipSkillDataManager.get("veteran"), 20, List.of());
            ShipLevelSystem.raiseToLevel(ShipSkillDataManager.get("middling"), 15, List.of());

            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
            FleetDataAPI fleetData = mock(FleetDataAPI.class);
            when(fleet.getFleetData()).thenReturn(fleetData);
            List<FleetMemberAPI> members = List.of(mockMember("veteran"), mockMember("middling"), mockMember("fresh"));
            when(fleetData.getMembersListCopy()).thenReturn(members);

            float highest = ShipLevelSystem.awardXpToFleet(fleet, 10f);

            assertEquals(10f, ShipSkillDataManager.get("veteran").getXp(), 0.001f);
            assertEquals(15f, ShipSkillDataManager.get("middling").getXp(), 0.001f);
            assertEquals(30f, ShipSkillDataManager.get("fresh").getXp(), 0.001f);
            assertEquals(3f, highest, 0.001f);
        }
    }

    @Test
    void catchUpGrowsPerLevelBehindUpToItsCap() {
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(0, 0.1f, 4f));
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(-5, 0.1f, 4f));
        assertEquals(2f, ShipLevelSystem.catchUpMultiplier(10, 0.1f, 4f), 0.0001f);
        assertEquals(4f, ShipLevelSystem.catchUpMultiplier(45, 0.1f, 4f));
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(10, 0f, 4f));
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(10, -1f, 4f));
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(10, Float.NaN, 4f));
        assertEquals(1f, ShipLevelSystem.catchUpMultiplier(10, 0.1f, 0.5f));
    }

    @Test
    void theLevelFloorIsAPercentageOfThePlayersLevelCappedAtTheMaxLevel() {
        assertEquals(15, ShipLevelSystem.levelFloor(15, 100, 50));
        assertEquals(7, ShipLevelSystem.levelFloor(15, 50, 50));
        assertEquals(0, ShipLevelSystem.levelFloor(15, 0, 50));
        assertEquals(15, ShipLevelSystem.levelFloor(15, 250, 50));
        assertEquals(0, ShipLevelSystem.levelFloor(15, -10, 50));
        assertEquals(10, ShipLevelSystem.levelFloor(15, 100, 10));
    }

    @Test
    void raisingToALevelGrantsEachLevelUpWithoutTouchingXp() {
        SkillNode paid = node("paid");
        ShipSkillData data = new ShipSkillData();
        data.allocate(paid, OP_COST_PER_NODE);
        data.addXp(12f);

        assertTrue(ShipLevelSystem.raiseToLevel(data, 3, List.of(paid)));

        assertEquals(3, data.getLevel());
        assertTrue(data.isFreeNode("paid"));
        assertEquals(2, data.getBankedFreeAllocations());
        assertEquals(12f, data.getXp());
        assertFalse(ShipLevelSystem.raiseToLevel(data, 3, List.of(paid)));
        assertFalse(ShipLevelSystem.raiseToLevel(data, 1, List.of(paid)));
        assertEquals(3, data.getLevel());
    }

    private static FleetMemberAPI mockMember(String id) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(hullSpec.getHullSize()).thenReturn(HullSize.FRIGATE);
        return member;
    }
}
