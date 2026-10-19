package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
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
    private static final String WARDEN_ID = "warden";

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        StarChartCurrency.registerAll();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.contains(anyString())).thenReturn(false);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(10);
        when(sector.getPlayerStats()).thenReturn(playerStats);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        ChartDrops.clear();
        StarChartCurrency.unregisterAll();
        StarChartFixtures.clear();
    }

    private static JSONObject rule(String source, float chance) throws Exception {
        return new JSONObject().put("source", source).put("chance", String.valueOf(chance));
    }

    private ChartRun storeLiveRun(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "hegemony", ChartObjective.BOSS, List.of(WARDEN_ID), WARDEN_ID, 4, 6));
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
    void aFleetBeatenOverSeveralBattlesPaysOutOnceInTotal() {
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
    void onlyFleetsOfTheLiveRunRollAnything() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f)));
        CampaignFleetAPI fleet = ChartFleetFixtures.chartFleet("guard", RUN_ID, 100f);

        assertTrue(ChartFleetLoot.roll(sector, fleet, 100f, new Random(1L)).isEmpty(), "no run stored");
        storeLiveRun(5);
        assertTrue(ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("other", "otherRun", 100f), 100f, new Random(1L)).isEmpty());
        assertTrue(ChartFleetLoot.roll(sector, mock(CampaignFleetAPI.class), 100f, new Random(1L)).isEmpty());
        assertEquals(1, chartCount(ChartFleetLoot.roll(sector, fleet, 100f, new Random(1L))));
    }

    @Test
    void onlyTheWardenCarriesSpoilsAndTheHoardScalesOnlyThem() throws Exception {
        ChartDrops.register(new JSONArray().put(rule("chart_fleet", 1f)).put(rule("chart_warden", 1f)));
        ChartRun run = storeLiveRun(6);
        ChartGenContext hoard = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_WARDEN_SPOILS_PERCENT, 100f)));

        ItemDrops.SiteLoot guardLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet("guard", RUN_ID, 80f), run, hoard, 80f,
                new Random(1L));
        ItemDrops.SiteLoot plainWardenLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(WARDEN_ID, RUN_ID, 200f), run,
                ChartGenContext.none(), 200f, new Random(1L));
        ItemDrops.SiteLoot hoardWardenLoot = ChartFleetLoot.roll(sector, ChartFleetFixtures.chartFleet(WARDEN_ID, RUN_ID, 200f), run, hoard, 200f,
                new Random(1L));

        assertEquals(1, chartCount(guardLoot));
        assertEquals(2, chartCount(plainWardenLoot));
        assertEquals(3, chartCount(hoardWardenLoot));
    }
}
