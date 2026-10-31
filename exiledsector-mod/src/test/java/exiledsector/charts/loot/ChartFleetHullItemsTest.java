package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.anomaly.relay.RelayKind;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.anomaly.relay.RelayStates;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        StarChartFixtures.unregisterCurrencies();
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

    @Test
    void theLastRelayStageBossDropsOneUniqueWhateverItsSourceOnceEveryRelayIsAwake() throws Exception {
        when(sector.getPlayerStats().getLevel()).thenReturn(15);
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "site_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("source", "site:somewhere_else").put("prefixes", "HULL_PERCENT:4:6")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", ChartIds.FLEET_LOOT_SITE).put("chances", "0").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", ChartIds.BOSS_LOOT_SITE).put("chances", "0").put("uniqueChance", "0")));
        try {
            ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(6, "hegemony", ChartObjective.BOSS, List.of(BOSS_ID), BOSS_ID, 4, 6, List.of("relay_network")));
            run.recordArrival();
            ChartRuns.store(run);
            ChartGenContext lastStage = ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of("relay_network"), false)
                    .withExtraEffects(List.of(), true));

            ItemDrops.SiteLoot guardLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f), run, lastStage, 80f,
                    new Random(1L));
            ItemDrops.SiteLoot plainBossLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                    ChartGenContext.none(), 200f, new Random(1L));
            ItemDrops.SiteLoot relaysAsleep = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, lastStage, 100f,
                    new Random(1L));
            AnomalyState.of(run, RelayNetwork.ANOMALY_ID).put(RelayKind.NAV_BUOY.key(), RelayStates.WOKEN);
            AnomalyState.of(run, RelayNetwork.ANOMALY_ID).put(RelayKind.COMM_RELAY.key(), RelayStates.WOKEN);
            ItemDrops.SiteLoot oneRelayAsleep = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, lastStage, 100f,
                    new Random(1L));
            AnomalyState.of(run, RelayNetwork.ANOMALY_ID).put(RelayKind.SENSOR_ARRAY.key(), RelayStates.WOKEN);
            ItemDrops.SiteLoot firstBattle = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, lastStage, 100f,
                    new Random(1L));
            ItemDrops.SiteLoot secondBattle = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, lastStage, 100f,
                    new Random(1L));

            assertTrue(guardLoot.items().stream().noneMatch(item -> "site_relic".equals(item.definitionId())));
            assertTrue(plainBossLoot.items().stream().noneMatch(item -> "site_relic".equals(item.definitionId())));
            assertTrue(relaysAsleep.items().stream().noneMatch(item -> "site_relic".equals(item.definitionId())));
            assertTrue(oneRelayAsleep.items().stream().noneMatch(item -> "site_relic".equals(item.definitionId())));
            assertEquals(1, firstBattle.items().stream().filter(item -> "site_relic".equals(item.definitionId())).count());
            assertTrue(secondBattle.items().stream().noneMatch(item -> "site_relic".equals(item.definitionId())));
            assertTrue(run.isBossUniqueGranted());
        } finally {
            ItemDrops.clear();
            ItemDefinitions.clear();
        }
    }

    @Test
    void aRunWithoutARelayNetworkNeverHoldsBackItsGuaranteedUnique() throws Exception {
        when(sector.getPlayerStats().getLevel()).thenReturn(15);
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "site_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("source", "site:somewhere_else").put("prefixes", "HULL_PERCENT:4:6")));
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", ChartIds.FLEET_LOOT_SITE).put("chances", "0").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", ChartIds.BOSS_LOOT_SITE).put("chances", "0").put("uniqueChance", "0")));
        try {
            ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(6, "hegemony", ChartObjective.BOSS, List.of(BOSS_ID), BOSS_ID, 4, 6, List.of()));
            run.recordArrival();
            ChartRuns.store(run);
            ChartGenContext guaranteed = ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of(), false)
                    .withExtraEffects(List.of(), true));

            ItemDrops.SiteLoot bossLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, guaranteed, 100f,
                    new Random(1L));

            assertEquals(1, bossLoot.items().stream().filter(item -> "site_relic".equals(item.definitionId())).count());
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
