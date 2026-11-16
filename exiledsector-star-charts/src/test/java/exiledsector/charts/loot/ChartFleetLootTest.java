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
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.debris.AnomalousDebris;
import exiledsector.charts.anomaly.stormfront.StormFront;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.DenseNebula;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.Nanoswarm;
import exiledsector.items.StarChartCipher;
import exiledsector.items.StarChartFixtures;
import exiledsector.loot.BattleLootRates;
import org.json.JSONArray;
import org.json.JSONObject;
import exiledsector.loot.FleetLootRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartFleetLootTest {

    private static final String RUN_ID = "fleetLootRun";
    private static final String BOSS_ID = "boss";
    private static final String AUGMENTATION = Nanoswarm.AUGMENTATION.itemId();
    private static final String CORRUPTION = Nanoswarm.CORRUPTION.itemId();
    private static final String REFORGE_CIPHER = StarChartCipher.REFORGE.itemId();

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
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(10);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ChartDrops.clear();
        StarChartFixtures.unregisterCraftingItems();
        StarChartFixtures.clear();
    }

    private static JSONObject rule(String source, float chartsPerHundredFleetPoints, int tier) throws Exception {
        float chance = chartsPerHundredFleetPoints * ChartFleetLoot.expectedFleetPoints(tier) / 100f;
        return new JSONObject().put("source", source).put("chance", String.valueOf(chance));
    }

    private ChartRun storeLiveRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", BOSS_ID, 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    private static ItemDrops.SiteLoot rollAlone(SectorAPI sector, CampaignFleetAPI fleet, float defeatedDeploymentPoints, Random random) {
        return ChartFleetLoot.roll(sector, fleet, defeatedDeploymentPoints, ChartFleetLoot.Battle.of(defeatedDeploymentPoints), random);
    }

    private static ItemDrops.SiteLoot rollAlone(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context,
                                                float defeatedDeploymentPoints, Random random) {
        return ChartFleetLoot.roll(sector, fleet, run, context, defeatedDeploymentPoints, ChartFleetLoot.Battle.of(defeatedDeploymentPoints), 1f,
                random);
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
        when(fleet.getFleetPoints()).thenReturn(30);

        ChartFleetLoot.recordStart(fleet);

        assertEquals(25f, fleetMemory.getFloat(ChartIds.START_DP_MEMORY_KEY));
        assertEquals(30f, fleetMemory.getFloat(ChartIds.START_FP_MEMORY_KEY));
        assertTrue(ChartFleetLoot.hasLootLedger(fleet));
        assertFalse(ChartFleetLoot.hasLootLedger(mock(CampaignFleetAPI.class)));
    }

    @Test
    void aFleetBeatenOverSeveralBattlesDropsItsLootOnceInTotal() {
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

        assertEquals(0.4f, ChartFleetLoot.claimShare(fleet, 40f), 1e-6f);
        assertEquals(0.6f, ChartFleetLoot.claimShare(fleet, 80f), 1e-6f);
        assertEquals(0f, ChartFleetLoot.claimShare(fleet, 20f));
    }

    @Test
    void aFleetWhoseLootIsSpentPaysNoExiledSectorBattleLoot() {
        storeLiveRun(7);
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("convoy", RUN_ID, 100f);

        ChartFleetLoot.spendLoot(fleet);

        assertTrue(ChartFleetLoot.lootSpent(fleet));
        assertSame(FleetLootRules.SPENT, StarChartLootSource.INSTANCE.fleetLootRules(fleet));
    }

    @Test
    void aSplitFleetRollsItsLootInProportionToTheDeploymentPointsDestroyed() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 2f, 7)));
        storeLiveRun(7);
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);
        Random random = new Random(3L);

        ItemDrops.SiteLoot firstBattle = rollAlone(sector, fleet, 50f, random);
        ItemDrops.SiteLoot secondBattle = rollAlone(sector, fleet, 50f, random);
        ItemDrops.SiteLoot thirdBattle = rollAlone(sector, fleet, 50f, random);

        assertEquals(1, chartCount(firstBattle));
        assertEquals(7, ItemData.ofStarChart(firstBattle.items().get(0).toSpecialItem()).preview().tier());
        assertEquals(1, chartCount(secondBattle));
        assertTrue(thirdBattle.isEmpty());
    }

    @Test
    void aBattleInTheStormFrontDropsMoreItems() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 2f, 7)));
        storeLiveRun(7);

        ItemDrops.SiteLoot calm = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("calm", RUN_ID, 100f), 100f,
                new ChartFleetLoot.Battle(100f, 1f, List.of()), new Random(3L));
        ItemDrops.SiteLoot stormy = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("stormy", RUN_ID, 100f), 100f,
                new ChartFleetLoot.Battle(100f, 1f, List.of(StormFront.INSTANCE)), new Random(3L));

        assertEquals(2, chartCount(calm));
        assertEquals(3, chartCount(stormy));
    }

    @Test
    void onlyFleetsOfTheLiveRunRollAnything() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 5)));
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

        assertTrue(rollAlone(sector, fleet, 100f, new Random(1L)).isEmpty(), "no run stored");
        storeLiveRun(5);
        assertTrue(rollAlone(sector, ChartFleetFixtures.chartFleet("other", "otherRun", 100f), 100f, new Random(1L)).isEmpty());
        assertTrue(rollAlone(sector, mock(CampaignFleetAPI.class), 100f, new Random(1L)).isEmpty());
        assertEquals(1, chartCount(rollAlone(sector, fleet, 100f, new Random(1L))));
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
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 9)));
        ChartLoot.register(new JSONArray().put(new JSONObject().put("id", "fleet_weapons1").put("drop_type", "weapon").put("scope", "fleet")
                .put("group", "weapons1").put("mode", "random").put("amount", "1")), group -> true);
        try {
            storeLiveRun(9);
            CampaignFleetAPI drones = defenders(40f);
            SectorEntityToken probe = siteDefendedBy(drones);
            StarSystemAPI system = chartSystem(probe);

            assertTrue(ChartFleetLoot.adoptDefender(drones, system));
            assertTrue(ChartFleetLoot.hasLootLedger(drones));
            assertEquals(40f, probe.getMemoryWithoutUpdate().getFloat(ChartIds.START_DP_MEMORY_KEY));
            assertEquals(200f, probe.getMemoryWithoutUpdate().getFloat(ChartIds.START_FP_MEMORY_KEY));
            Mockito.verify(drones).addDropRandom(Mockito.<SalvageEntityGenDataSpec.DropData>argThat(drop -> "weapons1".equals(drop.group)));
            assertEquals(1, chartCount(rollAlone(sector, drones, 20f, new Random(3L))));

            MemoryAPI wipedByVanilla = FakeChartContentEngine.memory();
            when(drones.getMemoryWithoutUpdate()).thenReturn(wipedByVanilla);
            assertTrue(ChartFleetLoot.adoptDefender(drones, system));
            Mockito.verify(drones, Mockito.times(1)).addDropRandom(Mockito.any(SalvageEntityGenDataSpec.DropData.class));
            assertEquals(1, chartCount(rollAlone(sector, drones, 40f, new Random(3L))), "a rematch drops only the half not yet dropped");
            assertTrue(rollAlone(sector, drones, 40f, new Random(3L)).isEmpty());
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
        assertFalse(ChartFleetLoot.hasLootLedger(stray));
    }

    @Test
    void theBossRollsItsOwnShareAndTheHoardScalesOnlyIt() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 6)).put(rule("chart_boss", 1f, 6)));
        ChartRun run = storeLiveRun(6);
        ChartGenContext hoard = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_SPOILS_PERCENT, 100f)));

        ItemDrops.SiteLoot guardLoot = rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f), run, hoard, 100f,
                new Random(1L));
        ItemDrops.SiteLoot plainBossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                ChartGenContext.none(), 200f, new Random(1L));
        ItemDrops.SiteLoot hoardBossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, hoard, 200f,
                new Random(1L));

        assertEquals(1, chartCount(guardLoot));
        assertEquals(2, chartCount(plainBossLoot));
        assertEquals(4, chartCount(hoardBossLoot));
    }

    @Test
    void theBossStarChartLineScalesOnlyTheBosssStarCharts() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 6)).put(rule("chart_boss", 1f, 6)));
        ChartRun run = storeLiveRun(6);
        ChartGenContext recovery = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_BOSS_STAR_CHART_PERCENT, 100f)));

        ItemDrops.SiteLoot guardLoot = rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f), run, recovery, 100f,
                new Random(1L));
        ItemDrops.SiteLoot bossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run, recovery, 200f,
                new Random(1L));

        assertEquals(1, chartCount(guardLoot));
        assertEquals(4, chartCount(bossLoot));
    }

    @Test
    void settlingDustScalesOnlyTheBosssStarCharts() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f, 6)).put(rule("chart_boss", 1f, 6)));
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(6, "hegemony", BOSS_ID, 4, 6, List.of(DenseNebula.ANOMALY_ID)));
        run.recordArrival();
        ChartRuns.store(run);
        for (int battle = 0; battle < DenseNebula.MAX_DUST; battle++) {
            DenseNebula.INSTANCE.battleWon(new AnomalyRun(run, null));
        }

        ItemDrops.SiteLoot guardLoot = rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f), run,
                ChartGenContext.none(), 100f, new Random(1L));
        ItemDrops.SiteLoot bossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                ChartGenContext.none(), 200f, new Random(1L));

        assertEquals(1, chartCount(guardLoot));
        assertEquals(4, chartCount(bossLoot));
    }

    @Test
    void aWholeTiersWorthOfFleetPointsDropsTheRowsChanceAndBiggerFleetsDropMore() throws Exception {
        ChartDrops.register(new JSONArray().put(new JSONObject().put("source", "chart_fleet").put("chance", "1.5")));
        ChartRun run = storeLiveRun(10);
        float wholeChart = ChartFleetLoot.expectedFleetPoints(10);
        int charts = 0;
        int doubledCharts = 0;
        Random random = new Random(7L);
        for (int trial = 0; trial < 2000; trial++) {
            charts += chartCount(rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, wholeChart), run,
                    ChartGenContext.none(), wholeChart, random));
            doubledCharts += chartCount(rollAlone(sector, ChartFleetFixtures.chartFleet("big", RUN_ID, 2f * wholeChart), run,
                    ChartGenContext.none(), 2f * wholeChart, random));
        }

        assertEquals(1.5, charts / 2000.0, 0.05);
        assertEquals(3.0, doubledCharts / 2000.0, 0.08);
    }

    private static void registerCraftingRate(String per100DeploymentPoints) throws Exception {
        BattleLootRates.register(new JSONArray().put(new JSONObject().put("drop", CraftingDrops.RATE_DROP).put("per_100_dp", per100DeploymentPoints)
                .put("curve", "diminishing").put("difficulty_scaling", "1")));
    }

    private static void registerCraftingDrops(JSONObject... rows) throws Exception {
        JSONArray table = new JSONArray();
        for (JSONObject row : rows) {
            table.put(row);
        }
        CraftingDrops.register(table);
    }

    private static JSONObject craftingDrop(String itemId, String weight, String chartOnly) throws Exception {
        return new JSONObject().put("item", itemId).put("weight", weight).put("chart_only", chartOnly);
    }

    private static int total(Map<String, Integer> drops) {
        return drops.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void aChartFleetRollsCraftingItemsOnlyFromTheDeploymentPointsItStillOwesSoARebuiltFleetCannotBeFarmed() throws Exception {
        registerCraftingRate("2");
        registerCraftingDrops(craftingDrop(AUGMENTATION, "1", "FALSE"));
        try {
            ChartRun run = storeLiveRun(6);
            CampaignFleetAPI guard = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

            ItemDrops.SiteLoot halfKilled = rollAlone(sector, guard, run, ChartGenContext.none(), 50f, new Random(1L));
            ItemDrops.SiteLoot rebuiltAndKilled = rollAlone(sector, guard, run, ChartGenContext.none(), 100f, new Random(1L));
            ItemDrops.SiteLoot farmedAgain = rollAlone(sector, guard, run, ChartGenContext.none(), 100f, new Random(1L));

            assertEquals(Map.of(AUGMENTATION, 1), halfKilled.materials());
            assertEquals(Map.of(AUGMENTATION, 1), rebuiltAndKilled.materials());
            assertTrue(farmedAgain.isEmpty());
        } finally {
            BattleLootRates.clear();
            CraftingDrops.clear();
        }
    }

    @Test
    void aChartFleetDropsTheRatePerDeploymentPointWhateverTheBattleAndCanDropChartOnlyItems() throws Exception {
        registerCraftingRate("1");
        registerCraftingDrops(craftingDrop(AUGMENTATION, "1", "FALSE"), craftingDrop(REFORGE_CIPHER, "1", "TRUE"));
        try {
            ChartRun run = storeLiveRun(8);
            ChartFleetLoot.Battle hardBattle = new ChartFleetLoot.Battle(200000f, 3f, List.of());

            Map<String, Integer> drops = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 100000f), run,
                    ChartGenContext.none(), 100000f, hardBattle, 1f, new Random(1L)).materials();

            assertEquals(1000, total(drops));
            assertTrue(drops.getOrDefault(REFORGE_CIPHER, 0) > 400, drops.toString());
        } finally {
            BattleLootRates.clear();
            CraftingDrops.clear();
        }
    }

    @Test
    void theChartTiersBandShiftTimesItemRarityMovesChartFleetDropsOutOfTheCommonBand() throws Exception {
        registerCraftingRate("1");
        registerCraftingDrops(craftingDrop(AUGMENTATION, "1", "FALSE"), craftingDrop(CORRUPTION, "1", "FALSE"));
        ChartTiers.register(new JSONArray().put(new JSONObject().put("tier", "8").put("band_shift", "1.5")));
        try {
            ChartRun run = storeLiveRun(8);
            ChartGenContext treasure = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 100f)));

            Map<String, Integer> drops = rollAlone(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 100000f), run, treasure,
                    100000f, new Random(1L)).materials();

            assertEquals(3f, treasure.craftingBandShift(8), 1e-6f);
            assertEquals(1000, total(drops));
            assertEquals(0.75f, drops.get(CORRUPTION) / 1000f, 0.05f);
        } finally {
            BattleLootRates.clear();
            CraftingDrops.clear();
            ChartTiers.resetForTests();
        }
    }

    @Test
    void anAnomalysFleetsDropTheirDeploymentPointShareTimesItsFleetLootButTheBossKeepsItsFull() throws Exception {
        ChartAnomalies.register(new JSONArray().put(new JSONObject().put("id", AnomalousDebris.ANOMALY_ID).put("fleet_loot", "0.5")));
        registerCraftingRate("10");
        registerCraftingDrops(craftingDrop(AUGMENTATION, "1", "FALSE"));
        try {
            ChartRun run = storeLiveRun(8);
            CampaignFleetAPI debrisFleet = ChartFleetFixtures.chartFleet("debris", RUN_ID, 100f);
            ChartFleetLoot.tagAnomaly(debrisFleet, AnomalousDebris.ANOMALY_ID);
            CampaignFleetAPI anomalyBoss = ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 100f);
            ChartFleetLoot.tagAnomaly(anomalyBoss, AnomalousDebris.ANOMALY_ID);
            CampaignFleetAPI untagged = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);
            ChartFleetLoot.tagAnomaly(untagged, "not_an_anomaly");

            assertEquals(0.5f, ChartFleetLoot.anomalyFleetLoot(debrisFleet, run));
            assertEquals(1f, ChartFleetLoot.anomalyFleetLoot(anomalyBoss, run));
            assertEquals(1f, ChartFleetLoot.anomalyFleetLoot(untagged, run));
            assertEquals(Map.of(AUGMENTATION, 5), rollAlone(sector, debrisFleet, run, ChartGenContext.none(), 100f,
                    new Random(1L)).materials());
        } finally {
            BattleLootRates.clear();
            CraftingDrops.clear();
            ChartAnomalies.resetForTests();
        }
    }

    @Test
    void theBossAddsNoCraftingItemsOfItsOwnBeyondItsDeploymentPoints() throws Exception {
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", ChartIds.BOSS_LOOT_SITE).put("chances", "0").put("uniqueChance", "0")));
        try {
            ChartRun run = storeLiveRun(6);

            ItemDrops.SiteLoot bossLoot = rollAlone(sector, ChartFleetFixtures.chartFleet(BOSS_ID, RUN_ID, 200f), run,
                    ChartGenContext.none(), 200f, new Random(1L));

            assertTrue(bossLoot.materials().isEmpty(), bossLoot.toString());
            assertTrue(run.isBossLootGuaranteed());
        } finally {
            ItemDrops.clear();
        }
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
