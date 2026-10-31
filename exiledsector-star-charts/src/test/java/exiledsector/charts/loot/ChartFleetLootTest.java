package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import exiledsector.MockSector;
import exiledsector.charts.anomaly.storm.StormFront;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartFleetLootTest {

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
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(10);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartDrops.clear();
        StarChartCurrency.unregisterAll();
        StarChartFixtures.clear();
    }

    private static JSONObject rule(String source, float chance) throws Exception {
        return new JSONObject().put("source", source).put("chance", String.valueOf(chance));
    }

    private ChartRun storeLiveRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", ChartObjective.BOSS, List.of(BOSS_ID), BOSS_ID, 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    private static int chartCount(ItemDrops.SiteLoot loot) {
        return (int) loot.items().stream().filter(item -> item.toSpecialItem().getId().equals(ItemData.STAR_CHART_ITEM_ID)).count();
    }

    @Test
    void spawningRecordsTheFleetsStartingDeploymentPoints() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI fleetMemory = FakeChartContentEngine.memory();
        when(fleet.getMemoryWithoutUpdate()).thenReturn(fleetMemory);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        FleetMemberAPI frigate = mock(FleetMemberAPI.class);
        when(frigate.getDeploymentPointsCost()).thenReturn(10f);
        FleetMemberAPI cruiser = mock(FleetMemberAPI.class);
        when(cruiser.getDeploymentPointsCost()).thenReturn(15f);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(frigate, cruiser));
        when(fleet.getFleetData()).thenReturn(fleetData);

        ChartFleetLoot.recordStart(fleet);

        assertEquals(25f, fleetMemory.getFloat(ChartIds.START_DP_MEMORY_KEY));
        assertTrue(ChartFleetLoot.isChartFleet(fleet));
        assertFalse(ChartFleetLoot.isChartFleet(mock(CampaignFleetAPI.class)));
    }

    @Test
    void aFleetBeatenOverSeveralBattlesDropsItsLootOnceInTotal() {
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

        assertEquals(0.4f, ChartFleetLoot.claimShare(fleet, 40f), 1e-6f);
        assertEquals(0.6f, ChartFleetLoot.claimShare(fleet, 80f), 1e-6f);
        assertEquals(0f, ChartFleetLoot.claimShare(fleet, 20f));
    }

    @Test
    void aSplitFleetRollsItsLootInProportionToTheDeploymentPointsDestroyed() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 2f)));
        storeLiveRun(7);
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);
        Random random = new Random(3L);

        ItemDrops.SiteLoot firstBattle = ChartFleetLoot.roll(sector, fleet, 50f, random);
        ItemDrops.SiteLoot secondBattle = ChartFleetLoot.roll(sector, fleet, 50f, random);
        ItemDrops.SiteLoot thirdBattle = ChartFleetLoot.roll(sector, fleet, 50f, random);

        assertEquals(1, chartCount(firstBattle));
        assertEquals(7, ItemData.ofStarChart(firstBattle.items().get(0).toSpecialItem()).preview().tier());
        assertEquals(1, chartCount(secondBattle));
        assertTrue(thirdBattle.isEmpty());
    }

    @Test
    void aBattleInTheStormFrontDropsMoreItems() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 2f)));
        storeLiveRun(7);

        ItemDrops.SiteLoot calm = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("calm", RUN_ID, 100f), 100f, new Random(3L), List.of());
        ItemDrops.SiteLoot stormy = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("stormy", RUN_ID, 100f), 100f, new Random(3L), List.of(StormFront.INSTANCE));

        assertEquals(2, chartCount(calm));
        assertEquals(3, chartCount(stormy));
    }

    @Test
    void onlyFleetsOfTheLiveRunRollAnything() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f)));
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

        assertTrue(ChartFleetLoot.roll(sector, fleet, 100f, new Random(1L)).isEmpty(), "no run stored");
        storeLiveRun(5);
        assertTrue(ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("other", "otherRun", 100f), 100f, new Random(1L)).isEmpty());
        assertTrue(ChartFleetLoot.roll(sector, mock(CampaignFleetAPI.class), 100f, new Random(1L)).isEmpty());
        assertEquals(1, chartCount(ChartFleetLoot.roll(sector, fleet, 100f, new Random(1L))));
    }

    private static StarSystemAPI chartSystem(SectorEntityToken... runEntities) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag(RUN_ID)));
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        when(system.getEntitiesWithTag(ChartIds.CONTENT_TAG)).thenReturn(List.of(runEntities));
        return system;
    }

    private static SectorEntityToken siteDefendedBy(CampaignFleetAPI defenders) {
        SectorEntityToken site = mock(SectorEntityToken.class);
        when(site.hasTag(ChartIds.runTag(RUN_ID))).thenReturn(true);
        MemoryAPI siteMemory = FakeChartContentEngine.memory();
        siteMemory.set(ChartIds.VANILLA_DEFENDER_FLEET_MEMORY_KEY, defenders);
        when(site.getMemoryWithoutUpdate()).thenReturn(siteMemory);
        return site;
    }

    private static CampaignFleetAPI defenders(float deploymentPoints) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getId()).thenReturn("defenders");
        MemoryAPI fleetMemory = FakeChartContentEngine.memory();
        when(fleet.getMemoryWithoutUpdate()).thenReturn(fleetMemory);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        FleetMemberAPI drone = mock(FleetMemberAPI.class);
        when(drone.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(drone));
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleet.getFleetPoints()).thenReturn(200);
        return fleet;
    }

    @Test
    void aSitesSpawnedDefendersBecomeAChartFleetWithTheirLedgerOnTheSite() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 2f)));
        ChartLoot.register(new JSONArray().put(new JSONObject().put("id", "fleet_weapons1").put("drop_type", "weapon").put("scope", "fleet")
                .put("group", "weapons1").put("mode", "random").put("amount", "1")), group -> true);
        try {
            storeLiveRun(9);
            CampaignFleetAPI drones = defenders(40f);
            SectorEntityToken probe = siteDefendedBy(drones);
            StarSystemAPI system = chartSystem(probe);

            assertTrue(ChartFleetLoot.adoptDefender(drones, system));
            assertTrue(ChartFleetLoot.isChartFleet(drones));
            assertEquals(40f, probe.getMemoryWithoutUpdate().getFloat(ChartIds.START_DP_MEMORY_KEY));
            Mockito.verify(drones).addDropRandom(Mockito.<SalvageEntityGenDataSpec.DropData>argThat(drop -> "weapons1".equals(drop.group)));
            assertEquals(1, chartCount(ChartFleetLoot.roll(sector, drones, 20f, new Random(3L))));

            MemoryAPI wipedByVanilla = FakeChartContentEngine.memory();
            when(drones.getMemoryWithoutUpdate()).thenReturn(wipedByVanilla);
            assertTrue(ChartFleetLoot.adoptDefender(drones, system));
            Mockito.verify(drones, Mockito.times(1)).addDropRandom(Mockito.any(SalvageEntityGenDataSpec.DropData.class));
            assertEquals(1, chartCount(ChartFleetLoot.roll(sector, drones, 40f, new Random(3L))), "a rematch drops only the half not yet dropped");
            assertTrue(ChartFleetLoot.roll(sector, drones, 40f, new Random(3L)).isEmpty());
        } finally {
            ChartLoot.clear();
        }
    }

    @Test
    void fleetsThatDefendNoSiteOfTheLiveRunAreNotAdopted() {
        storeLiveRun(9);
        CampaignFleetAPI stray = defenders(40f);
        StarSystemAPI system = chartSystem(siteDefendedBy(defenders(10f)));

        assertFalse(ChartFleetLoot.adoptDefender(stray, system));
        assertFalse(ChartFleetLoot.adoptDefender(defenders(40f), mock(LocationAPI.class)));
        assertFalse(ChartFleetLoot.isChartFleet(stray));
    }

    @Test
    void onlyTheBossCarriesSpoilsAndTheHoardScalesOnlyThem() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f)).put(rule("chart_boss", 1f)));
        ChartRun run = storeLiveRun(6);
        ChartGenContext hoard = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_BOSS_SPOILS_PERCENT, 100f)));

        ItemDrops.SiteLoot guardLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f), run, hoard, 80f,
                new Random(1L));
        ItemDrops.SiteLoot plainBossLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                ChartGenContext.none(), 200f, new Random(1L));
        ItemDrops.SiteLoot hoardBossLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, hoard, 200f,
                new Random(1L));

        assertEquals(1, chartCount(guardLoot));
        assertEquals(2, chartCount(plainBossLoot));
        assertEquals(3, chartCount(hoardBossLoot));
    }
    @Test
    void aChartFleetGetsItsRunsItemChancesAndAnyOtherFleetTheGlobalOnes() throws Exception {
        ChartRun run = storeLiveRun(6);
        CampaignFleetAPI guard = ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f);

        assertEquals(ChartGenContext.none().itemChances(), LootContexts.forFleet(mock(CampaignFleetAPI.class)).itemChances());
        assertEquals(ChartGenContext.none().itemChances(), LootContexts.forFleet(null).itemChances());
        assertEquals(ChartRunEffects.contextOf(run).itemChances(), LootContexts.forFleet(guard).itemChances());
        assertTrue(ChartFleetLoot.liveRunOf(guard) != null);
    }
}
