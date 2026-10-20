package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.skills.progression.ShipLevelConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartTierLevelsTest {

    private static final int OPEN_WORLD_FACTION_CAP = 25;

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        ChartTiers.register(ChartContentDataTest.csvRows(ChartTiers.PATH));
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        ChartTiers.resetForTests();
    }

    private void setMaxLevel(int maxLevel) {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSectorModPlugin.MOD_ID, ShipLevelConfig.MAX_LEVEL_FIELD_ID)).thenReturn(maxLevel);
    }

    private void setMaxAllocatedNodes(int maxAllocatedNodes) {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSectorModPlugin.MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID))
                .thenReturn(maxAllocatedNodes);
    }

    private static MemoryAPI chartFleet(int tier, Integer strengthBonus, boolean warden) {
        MemoryAPI fleetMemory = mock(MemoryAPI.class);
        when(fleetMemory.get(ChartIds.TIER_MEMORY_KEY)).thenReturn(tier);
        when(fleetMemory.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY)).thenReturn(strengthBonus);
        when(fleetMemory.get(ChartIds.WARDEN_MEMORY_KEY)).thenReturn(warden ? Boolean.TRUE : null);
        return fleetMemory;
    }

    @Test
    void tierOneStartsAboveTheFactionCapAndTierFifteenReachesTheMaxLevel() {
        ChartLevelRange tierOne = ChartTiers.fleetStrengthRangeFor(chartFleet(1, null, false));
        ChartLevelRange tierFifteen = ChartTiers.fleetStrengthRangeFor(chartFleet(15, null, false));

        assertEquals(new ChartLevelRange(26, 29), tierOne);
        assertTrue(tierOne.min() > OPEN_WORLD_FACTION_CAP);
        assertEquals(new ChartLevelRange(47, ShipLevelConfig.DEFAULT_MAX_LEVEL), tierFifteen);
    }

    @Test
    void aLowerMaxLevelClampsEveryTier() {
        setMaxLevel(20);

        assertEquals(new ChartLevelRange(20, 20), ChartTiers.fleetStrengthRangeFor(chartFleet(1, null, false)));
        assertEquals(new ChartLevelRange(20, 20), ChartTiers.fleetStrengthRangeFor(chartFleet(15, null, false)));
    }

    @Test
    void aSmallerTreeCapClampsEveryTier() {
        setMaxAllocatedNodes(28);

        assertEquals(new ChartLevelRange(26, 27), ChartTiers.fleetStrengthRangeFor(chartFleet(1, null, false)));
        assertEquals(new ChartLevelRange(27, 27), ChartTiers.fleetStrengthRangeFor(chartFleet(15, null, true)));
    }

    @Test
    void theFleetStrengthBonusScalesTheLevelsUpToTheMaxLevel() {
        ChartTier seventh = ChartTiers.forTier(7);

        assertEquals(new ChartLevelRange(Math.round(seventh.levelMin() * 1.2f), Math.round(seventh.levelMax() * 1.2f)),
                ChartTiers.fleetStrengthRangeFor(chartFleet(7, 20, false)));
        assertEquals(new ChartLevelRange(ShipLevelConfig.DEFAULT_MAX_LEVEL, ShipLevelConfig.DEFAULT_MAX_LEVEL),
                ChartTiers.fleetStrengthRangeFor(chartFleet(15, 80, false)));
        assertEquals(ChartTiers.fleetStrengthRangeFor(chartFleet(7, null, false)), ChartTiers.fleetStrengthRangeFor(chartFleet(7, -30, false)));
    }

    @Test
    void wardenShipsRollOneLevelAboveTheTierEvenPastTheMaxLevel() {
        ChartTier seventh = ChartTiers.forTier(7);

        assertEquals(new ChartLevelRange(seventh.levelMin() + 1, seventh.levelMax() + 1), ChartTiers.fleetStrengthRangeFor(chartFleet(7, null, true)));
        assertEquals(new ChartLevelRange(48, ShipLevelConfig.DEFAULT_MAX_LEVEL + 1), ChartTiers.fleetStrengthRangeFor(chartFleet(15, null, true)));
        assertEquals(new ChartLevelRange(ShipLevelConfig.DEFAULT_MAX_LEVEL + 1, ShipLevelConfig.DEFAULT_MAX_LEVEL + 1),
                ChartTiers.fleetStrengthRangeFor(chartFleet(15, 80, true)));
        setMaxLevel(20);
        assertEquals(new ChartLevelRange(21, 21), ChartTiers.fleetStrengthRangeFor(chartFleet(15, null, true)));
    }

    @Test
    void fleetsWithoutAChartTierHaveNoLevelRange() {
        MemoryAPI notAChartFleet = mock(MemoryAPI.class);
        MemoryAPI garbled = mock(MemoryAPI.class);
        when(garbled.get(ChartIds.TIER_MEMORY_KEY)).thenReturn("fifteen");

        assertNull(ChartTiers.fleetStrengthRangeFor(notAChartFleet));
        assertNull(ChartTiers.fleetStrengthRangeFor(garbled));
        assertNull(ChartTiers.fleetStrengthRangeFor(null));
    }

    @Test
    void aLevelRangeRollsEveryLevelInsideItAndNothingElse() {
        ChartLevelRange range = new ChartLevelRange(26, 29);
        Random random = new Random(7);
        boolean[] seen = new boolean[4];
        for (int roll = 0; roll < 200; roll++) {
            int level = range.roll(random);
            assertTrue(level >= 26 && level <= 29, "rolled " + level);
            seen[level - 26] = true;
        }
        for (boolean levelSeen : seen) {
            assertTrue(levelSeen);
        }
        assertEquals(new ChartLevelRange(5, 5), new ChartLevelRange(5, 2));
    }
}
