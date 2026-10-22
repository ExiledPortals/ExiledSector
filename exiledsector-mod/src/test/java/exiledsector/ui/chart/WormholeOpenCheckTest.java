package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.WormholeGateway;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import exiledsector.ui.chart.WormholeOpenCheck.Blocker;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeOpenCheckTest {

    private final ItemStore store = ItemStore.forStarCharts();
    private final CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
    private final LocationAPI hyperspace = mock(LocationAPI.class);
    private Item chart;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 4L, 3));
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
    }

    @Test
    void aStoredChartOpensWhenNothingStandsInTheWay() {
        Blocker blocker = WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet);

        assertEquals(Blocker.NONE, blocker);
        assertTrue(blocker.allowsOpening());
    }

    @Test
    void openingNeedsAChartThatIsReallyInTheChartStore() {
        Item loose = ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 5L, 3).preview();

        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(null, store, true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(loose, store, true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, new ItemStore(), true, false, playerFleet));
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, null, true, false, playerFleet));
        store.remove(chart);
        assertEquals(Blocker.NO_CHART, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
    }

    private static SectorEntityToken colony(boolean playerOwned, Boolean gatewayFunctional) {
        SectorEntityToken colony = mock(SectorEntityToken.class);
        MarketAPI market = mock(MarketAPI.class);
        when(colony.getMarket()).thenReturn(market);
        when(market.isPlayerOwned()).thenReturn(playerOwned);
        if (gatewayFunctional != null) {
            Industry gateway = mock(Industry.class);
            when(gateway.isFunctional()).thenReturn(gatewayFunctional);
            when(market.getIndustry(WormholeGateway.ID)).thenReturn(gateway);
        }
        return colony;
    }

    @Test
    void withoutTheDevSwitchTheFleetMustBeDockedAtAPlayerColonyWithAWorkingGateway() {
        assertEquals(Blocker.NO_GATEWAY, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet));
        assertEquals(Blocker.NO_GATEWAY, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, mock(SectorEntityToken.class)));
        assertEquals(Blocker.NO_GATEWAY, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, colony(true, null)));
        assertEquals(Blocker.NO_GATEWAY, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, colony(false, true)));
        assertEquals(Blocker.GATEWAY_INACTIVE, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, colony(true, false)));
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, colony(true, true)));
        assertFalse(Blocker.NO_GATEWAY.allowsOpening());
        assertFalse(Blocker.GATEWAY_INACTIVE.allowsOpening());
    }

    @Test
    void theDevSwitchOpensAnywhereWithoutAGateway() {
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, colony(true, false)));
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
    }

    @Test
    void aWorkingGatewayNeverOverridesTheOtherBlockers() {
        SectorEntityToken gatewayColony = colony(true, true);

        assertEquals(Blocker.RUN_OPEN, WormholeOpenCheck.evaluate(chart, store, false, true, playerFleet, gatewayColony));
        when(hyperspace.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        assertEquals(Blocker.IN_SYSTEM, WormholeOpenCheck.evaluate(chart, store, false, false, playerFleet, gatewayColony));
    }

    @Test
    void switchingStarChartsOffBlocksOpeningBeforeAnythingElse() {
        StarChartSettings.setEnabledForTests(false);
        try {
            assertEquals(Blocker.DISABLED, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, colony(true, true)));
            assertEquals(Blocker.DISABLED, WormholeOpenCheck.evaluate(null, store, true, true, playerFleet));
            assertFalse(Blocker.DISABLED.allowsOpening());
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    @Test
    void anOpenRunBlocksOpening() {
        assertEquals(Blocker.RUN_OPEN, WormholeOpenCheck.evaluate(chart, store, true, true, playerFleet));
    }

    @Test
    void theFleetMustBeOutsideASystemAndNotMidJump() {
        assertEquals(Blocker.NO_FLEET, WormholeOpenCheck.evaluate(chart, store, true, false, null));
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);
        assertEquals(Blocker.IN_TRANSITION, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
        when(playerFleet.isInHyperspaceTransition()).thenReturn(false);
        when(hyperspace.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
        assertEquals(Blocker.IN_SYSTEM, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
        when(playerFleet.getContainingLocation()).thenReturn(null);
        assertEquals(Blocker.NO_FLEET, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet));
    }

    @Test
    void aFleetEncounterBlocksOpeningButADockedMarketDoesNot() {
        CampaignFleetAPI otherFleet = mock(CampaignFleetAPI.class);
        SectorEntityToken station = mock(SectorEntityToken.class);

        assertEquals(Blocker.IN_FLEET_ENCOUNTER, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, otherFleet));
        assertFalse(Blocker.IN_FLEET_ENCOUNTER.allowsOpening());
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, station));
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, playerFleet));
        assertEquals(Blocker.NONE, WormholeOpenCheck.evaluate(chart, store, true, false, playerFleet, null));
    }

    @Test
    void theLiveCheckReadsTheCurrentDialogTarget() {
        SectorAPI sector = mock(SectorAPI.class);
        CampaignUIAPI campaignUi = mock(CampaignUIAPI.class);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        CampaignFleetAPI otherFleet = mock(CampaignFleetAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(dialog);
        when(dialog.getInteractionTarget()).thenReturn(otherFleet);

        assertEquals(otherFleet, WormholeOpenCheck.dialogTarget(sector));
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(null);
        assertNull(WormholeOpenCheck.dialogTarget(sector));
        assertNull(WormholeOpenCheck.dialogTarget(null));
    }

    @Test
    void theLiveCheckReadsTheSettingTheRunAndThePlayerFleet() {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class);
             MockedStatic<LunaSettings> lunaMock = Mockito.mockStatic(LunaSettings.class)) {
            globalMock.when(Global::getSector).thenReturn(sector);
            lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(null);
            assertEquals(Blocker.NO_GATEWAY, WormholeOpenCheck.current(sector, store, chart), "the dev switch defaults to off");

            ChartRuns.store(new ChartRun("busy", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
            assertEquals(Blocker.RUN_OPEN, WormholeOpenCheck.current(sector, store, chart));
            ChartRuns.delete("busy");

            lunaMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(true);
            assertEquals(Blocker.NONE, WormholeOpenCheck.current(sector, store, chart));
        }
    }
}
