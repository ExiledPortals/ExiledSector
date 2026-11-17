package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
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
import exiledsector.items.ItemDefinitions;
import exiledsector.items.crafting.material.StarChartCipher;
import exiledsector.items.starchart.StarChartFixtures;
import exiledsector.loot.Loot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

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
        StarChartCipher.registerAll();
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
        StarChartFixtures.unregisterCraftingItems();
        StarChartFixtures.clear();
    }

    @Test
    void chartFleetsAndTheBossLeaveHullItemsToBattleDrops() throws Exception {
        when(sector.getPlayerStats().getLevel()).thenReturn(15);
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "bridge_basic").put("section", "bridge").put("rarity", "10").put("prefixes", "HULL_PERCENT:4:6"))
                .put(new JSONObject().put("id", "hull_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("prefixes", "HULL_PERCENT:4:6")));
        try {
            ChartRun run = storeLiveRun(6);

            Loot guardLoot = rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f), run,
                    ChartGenContext.none(), 80f, new Random(1L));
            Loot bossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                    ChartGenContext.none(), 200f, new Random(1L));

            assertTrue(guardLoot.schematics().isEmpty());
            assertTrue(guardLoot.items().stream().noneMatch(item -> ItemDefinitions.get(item.definitionId()) != null), guardLoot.toString());
            assertTrue(guardLoot.materials().isEmpty(), "materials drop per deployment point from the battle, not from the fleet roll");
            assertTrue(bossLoot.schematics().isEmpty());
            assertTrue(bossLoot.items().stream().noneMatch(item -> ItemDefinitions.get(item.definitionId()) != null), bossLoot.toString());
        } finally {
            ItemDefinitions.clear();
        }
    }

    @Test
    void theBossGuaranteesNeitherAHullItemNorAUniqueEvenAtAFullyWokenRelayNetwork() throws Exception {
        when(sector.getPlayerStats().getLevel()).thenReturn(15);
        ItemDefinitions.register(new JSONArray()
                .put(new JSONObject().put("id", "bridge_basic").put("section", "bridge").put("rarity", "10").put("prefixes", "HULL_PERCENT:4:6"))
                .put(new JSONObject().put("id", "site_relic").put("section", "bridge").put("rarity", "10").put("unique", "true")
                        .put("source", "site:somewhere_else").put("prefixes", "HULL_PERCENT:4:6")));
        try {
            ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(6, "hegemony", BOSS_ID, 4, 6, List.of("relay_network")));
            run.recordArrival();
            ChartRuns.store(run);
            for (RelayKind kind : List.of(RelayKind.NAV_BUOY, RelayKind.COMM_RELAY, RelayKind.SENSOR_ARRAY)) {
                AnomalyState.of(run, RelayNetwork.ANOMALY_ID).put(kind.key(), RelayStates.WOKEN);
            }
            ChartGenContext lastStage = ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of("relay_network")));

            Loot bossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, lastStage, 100f, new Random(1L));

            assertTrue(bossLoot.items().stream().noneMatch(item -> ItemDefinitions.get(item.definitionId()) != null), bossLoot.toString());
        } finally {
            ItemDefinitions.clear();
        }
    }

    private ChartRun storeLiveRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", BOSS_ID, 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    private static Loot rollAlone(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context,
                                                float defeatedDeploymentPoints, Random random) {
        return ChartFleetLoot.roll(sector, fleet, run, new ChartLootContext(context, run.tier()), defeatedDeploymentPoints,
                ChartFleetLoot.Battle.of(defeatedDeploymentPoints), random);
    }
}
