package exiledsector.skills.progression;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
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

import java.util.List;

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
        return new ShipLevelSystem.LevelCurve(XP_BASE, XP_GROWTH, growthCutoffLevel, 0f, 1, maxLevel);
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
        assertEquals(100f, curve(NO_GROWTH_CUTOFF, 50).xpToReachNextLevel(0));
        assertEquals(200f, curve(NO_GROWTH_CUTOFF, 50).xpToReachNextLevel(1));
        assertEquals(400f, curve(NO_GROWTH_CUTOFF, 50).xpToReachNextLevel(2));
    }

    @Test
    void xpToReachNextLevelLocksToTheCutoffTransitionCostFromThatLevelOnward() {
        assertEquals(400f, curve(3, 50).xpToReachNextLevel(2));
        assertEquals(400f, curve(3, 50).xpToReachNextLevel(3));
        assertEquals(400f, curve(3, 50).xpToReachNextLevel(10));
    }

    @Test
    void aCutoffOfZeroKeepsTheXpGrowingAtEveryLevel() {
        assertEquals(100f, curve(0, 50).xpToReachNextLevel(0));
        assertEquals(102400f, curve(0, 50).xpToReachNextLevel(10));
    }

    @Test
    void xpToReachNextLevelBeforeTheCutoffIsUnaffected() {
        assertEquals(100f, curve(3, 50).xpToReachNextLevel(0));
        assertEquals(200f, curve(3, 50).xpToReachNextLevel(1));
    }

    @Test
    void theGrowthRampAddsOneStepToTheGrowthAtTheStartLevelAndAnotherAtEachLevelAfter() {
        ShipLevelSystem.LevelCurve rampedCurve = new ShipLevelSystem.LevelCurve(100f, 1.1f, 0, 0.05f, 3, 50);

        assertEquals(121f, rampedCurve.xpToReachNextLevel(2), 0.01f);
        assertEquals(121f * 1.15f, rampedCurve.xpToReachNextLevel(3), 0.01f);
        assertEquals(121f * 1.15f * 1.2f, rampedCurve.xpToReachNextLevel(4), 0.01f);
    }

    @Test
    void theGrowthCutoffAlsoFreezesARampedCurve() {
        ShipLevelSystem.LevelCurve rampedCurve = new ShipLevelSystem.LevelCurve(100f, 1.1f, 5, 0.05f, 3, 50);

        assertEquals(rampedCurve.xpToReachNextLevel(4), rampedCurve.xpToReachNextLevel(12));
    }

    @Test
    void theDefaultCurveGrowsSteadilyUntilTheRampThenEachLevelOutgrowsTheOneBefore() {
        ShipLevelSystem.LevelCurve defaultCurve = new ShipLevelSystem.LevelCurve(ShipLevelConfig.DEFAULT_XP_BASE,
                1f + ShipLevelConfig.DEFAULT_XP_GROWTH_PERCENT / 100f, ShipLevelConfig.DEFAULT_XP_GROWTH_CUTOFF_LEVEL,
                ShipLevelConfig.DEFAULT_XP_GROWTH_RAMP_PERCENT / 100f, ShipLevelConfig.DEFAULT_XP_GROWTH_RAMP_START_LEVEL,
                ShipLevelConfig.DEFAULT_MAX_LEVEL);
        int rampStart = ShipLevelConfig.DEFAULT_XP_GROWTH_RAMP_START_LEVEL;

        for (int level = 1; level < rampStart; level++) {
            assertEquals(1.13f, defaultCurve.xpToReachNextLevel(level) / defaultCurve.xpToReachNextLevel(level - 1), 1e-4f);
        }
        float previousGrowth = 1.13f;
        for (int level = rampStart; level < ShipLevelConfig.DEFAULT_MAX_LEVEL; level++) {
            float growth = defaultCurve.xpToReachNextLevel(level) / defaultCurve.xpToReachNextLevel(level - 1);
            assertTrue(growth > previousGrowth, "level " + level);
            previousGrowth = growth;
        }
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
        ShipTrees.allocate(data, a, OP_COST_PER_NODE);

        ShipLevelSystem.awardXp(data, XP_BASE, curve(NO_GROWTH_CUTOFF, 50), List.of(a));

        assertEquals(1, data.getLevel());
        assertEquals(0f, data.getXp());
        assertTrue(data.isFreeNode("a"));
        assertEquals(0, ShipTrees.getSpentOp(data, OP_COST_PER_NODE));
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
        ShipTrees.allocate(data, a, OP_COST_PER_NODE);
        ShipTrees.allocate(data, b, OP_COST_PER_NODE);

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
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockSector mockSector = new MockSector()) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);

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
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockSector mockSector = new MockSector()) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);

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
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockSector mockSector = new MockSector()) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
            lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
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
    void aShipAboveTheMaxLevelSetsTheCatchUpAsIfItWereAtTheMax() {
        try (MockedStatic<LunaSettings> lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
             MockSector mockSector = new MockSector()) {
            lunaSettingsMock.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(null);
            lunaSettingsMock.when(() -> LunaSettings.getFloat(anyString(), anyString())).thenReturn(null);
            ShipSkillData overLevelled = ShipSkillDataManager.get("captured");
            for (int level = 0; level < ShipLevelConfig.DEFAULT_MAX_LEVEL + 9; level++) {
                overLevelled.incrementLevel();
            }
            ShipLevelSystem.raiseToLevel(ShipSkillDataManager.get("middling"), ShipLevelConfig.DEFAULT_MAX_LEVEL - 5, List.of());

            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
            FleetDataAPI fleetData = mock(FleetDataAPI.class);
            when(fleet.getFleetData()).thenReturn(fleetData);
            List<FleetMemberAPI> members = List.of(mockMember("captured"), mockMember("middling"));
            when(fleetData.getMembersListCopy()).thenReturn(members);

            ShipLevelSystem.awardXpToFleet(fleet, 10f);

            assertEquals(15f, ShipSkillDataManager.get("middling").getXp(), 0.001f);
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
        ShipTrees.allocate(data, paid, OP_COST_PER_NODE);
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
