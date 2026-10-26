package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledlib.MockSector;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartFleetHullItemsTest {

    private static final String RUN_ID = "fleetLootRun";
    private static final String BOSS_ID = "boss";

    private MockSector mockSector;
    private SectorAPI sector;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        mockSector = new MockSector();
        sector = mockSector.sector;
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.contains(anyString())).thenReturn(false);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPlayerStats()).thenReturn(mock(MutableCharacterStatsAPI.class));
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        StarChartCurrency.unregisterAll();
        StarChartFixtures.clear();
    }

    @Test
    void chartFleetsLeaveHullItemsToBattleDropsWhileTheBossStillCarriesThem() throws Exception {
        when(sector.getPlayerStats().getLevel()).thenReturn(15);
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "bridge_basic").put("section", "bridge").put("rarity", "10").put("prefixes", "HULL_PERCENT:4:6"))
                .put(new JSONObject().put("id", "hull_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("prefixes", "HULL_PERCENT:4:6")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", ChartIds.FLEET_LOOT_SITE).put("chances", "1;1;1").put("uniqueChance", "1"))
                .put(new JSONObject().put("site", ChartIds.BOSS_LOOT_SITE).put("chances", "1;1;1").put("uniqueChance", "1")));
        try {
            ChartRun run = storeLiveRun(6);

            ItemDrops.SiteLoot guardLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f), run,
                    ChartGenContext.none(), 80f, new Random(1L));
            ItemDrops.SiteLoot bossLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                    ChartGenContext.none(), 200f, new Random(1L));

            assertTrue(guardLoot.upgrades().isEmpty());
            assertTrue(guardLoot.items().stream().noneMatch(item -> ItemDefinitions.get(item.definitionId()) != null), guardLoot.toString());
            assertFalse(guardLoot.materials().isEmpty());
            assertFalse(bossLoot.upgrades().isEmpty());
            assertTrue(bossLoot.items().stream().anyMatch(item -> "bridge_basic".equals(item.definitionId())));
        } finally {
            ItemDrops.clear();
            ItemDefinitions.clear();
        }
    }

    private ChartRun storeLiveRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", ChartObjective.BOSS, List.of(BOSS_ID), BOSS_ID, 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }
}
