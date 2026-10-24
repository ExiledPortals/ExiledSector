package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.ExiledSector;
import exiledsector.skills.npc.NpcNodeRange;
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
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        ChartTiers.resetForTests();
    }

    private void setMaxLevel(int maxLevel) {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, ShipLevelConfig.MAX_LEVEL_FIELD_ID)).thenReturn(maxLevel);
    }

    private void setMaxAllocatedNodes(int maxAllocatedNodes) {
        lunaSettingsMock.when(() -> LunaSettings.getInt(ExiledSector.MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID))
                .thenReturn(maxAllocatedNodes);
    }

    private static NpcNodeRange range(MemoryAPI fleetMemory) {
        return NpcNodeRange.of(ChartTiers.fleetStrengthRangeFor(fleetMemory), ShipLevelConfig.maxLevel(), ShipLevelConfig.maxAllocatedNodesBesidesRoot());
    }

    private static MemoryAPI chartFleet(int tier, Integer strengthBonus, boolean boss) {
        MemoryAPI fleetMemory = mock(MemoryAPI.class);
        when(fleetMemory.get(ChartIds.TIER_MEMORY_KEY)).thenReturn(tier);
        when(fleetMemory.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY)).thenReturn(strengthBonus);
        when(fleetMemory.get(ChartIds.BOSS_MEMORY_KEY)).thenReturn(boss ? Boolean.TRUE : null);
        return fleetMemory;
    }

    @Test
    void tierOneStartsAboveTheFactionCapAndTierFifteenReachesTheMaxLevel() {
        NpcNodeRange tierOne = range(chartFleet(1, null, false));
        NpcNodeRange tierFifteen = range(chartFleet(15, null, false));

        assertEquals(new NpcNodeRange(26, 29), tierOne);
        assertTrue(tierOne.min() > OPEN_WORLD_FACTION_CAP);
        assertEquals(new NpcNodeRange(47, ShipLevelConfig.DEFAULT_MAX_LEVEL), tierFifteen);
    }

    @Test
    void aLowerMaxLevelClampsEveryTier() {
        setMaxLevel(20);

        assertEquals(new NpcNodeRange(20, 20), range(chartFleet(1, null, false)));
        assertEquals(new NpcNodeRange(20, 20), range(chartFleet(15, null, false)));
    }

    @Test
    void aSmallerTreeCapClampsEveryTier() {
        setMaxAllocatedNodes(28);

        assertEquals(new NpcNodeRange(26, 27), range(chartFleet(1, null, false)));
        assertEquals(new NpcNodeRange(27, 27), range(chartFleet(15, null, true)));
    }

    @Test
    void theFleetStrengthBonusScalesTheLevelsUpToTheMaxLevel() {
        ChartTier seventh = ChartTiers.forTier(7);

        assertEquals(new NpcNodeRange(Math.round(seventh.levelMin() * 1.2f), Math.round(seventh.levelMax() * 1.2f)),
                range(chartFleet(7, 20, false)));
        assertEquals(new NpcNodeRange(ShipLevelConfig.DEFAULT_MAX_LEVEL, ShipLevelConfig.DEFAULT_MAX_LEVEL),
                range(chartFleet(15, 80, false)));
        assertEquals(range(chartFleet(7, null, false)), range(chartFleet(7, -30, false)));
    }

    @Test
    void bossShipsRollOneLevelAboveTheTierEvenPastTheMaxLevel() {
        ChartTier seventh = ChartTiers.forTier(7);

        assertEquals(new NpcNodeRange(seventh.levelMin() + 1, seventh.levelMax() + 1), range(chartFleet(7, null, true)));
        assertEquals(new NpcNodeRange(48, ShipLevelConfig.DEFAULT_MAX_LEVEL + 1), range(chartFleet(15, null, true)));
        assertEquals(new NpcNodeRange(ShipLevelConfig.DEFAULT_MAX_LEVEL + 1, ShipLevelConfig.DEFAULT_MAX_LEVEL + 1),
                range(chartFleet(15, 80, true)));
        setMaxLevel(20);
        assertEquals(new NpcNodeRange(21, 21), range(chartFleet(15, null, true)));
    }

    @Test
    void fleetsWithoutAChartTierHaveNoLevelRange() {
        MemoryAPI notAChartFleet = mock(MemoryAPI.class);
        MemoryAPI garbled = mock(MemoryAPI.class);
        when(garbled.get(ChartIds.TIER_MEMORY_KEY)).thenReturn("fifteen");

        assertNull(range(notAChartFleet));
        assertNull(range(garbled));
        assertNull(range(null));
    }

    @Test
    void aLevelRangeRollsEveryLevelInsideItAndNothingElse() {
        NpcNodeRange range = new NpcNodeRange(26, 29);
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
        assertEquals(new NpcNodeRange(5, 5), new NpcNodeRange(5, 2));
    }
}
