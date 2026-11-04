package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.i18n.Translation;
import exiledsector.ui.atlas.AtlasScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExiledSectorAtlasOptionTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<AtlasScreen> atlasMock;
    private CampaignUIAPI campaignUI;
    private MarketAPI market;
    private InteractionDialogAPI dialog;
    private OptionPanelAPI options;
    private final ExiledSectorAtlasOption command = new ExiledSectorAtlasOption();

    @BeforeEach
    void setUp() {
        SectorAPI sector = mock(SectorAPI.class);
        campaignUI = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        atlasMock = Mockito.mockStatic(AtlasScreen.class);
        market = mock(MarketAPI.class);
        when(market.isPlayerOwned()).thenReturn(true);
        when(market.getIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(mock(Industry.class));
        SectorEntityToken colony = mock(SectorEntityToken.class);
        when(colony.getMarket()).thenReturn(market);
        dialog = mock(InteractionDialogAPI.class);
        options = mock(OptionPanelAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(colony);
        when(dialog.getOptionPanel()).thenReturn(options);
    }

    @AfterEach
    void tearDown() {
        atlasMock.close();
        globalMock.close();
    }

    @Test
    void aColonyWithTheGatewayOffersTheAtlasRightAfterTheDockyardRepair() {
        assertTrue(command.execute("rule", dialog, List.of(), Map.of()));

        verify(options).addOption(Translation.gameText("chart.gateway.option.colonyMenu"), ExiledSectorAtlasOption.OPTION_ID,
                Translation.gameText("chart.gateway.option.tooltip"));
        assertTrue(command.doesCommandAddOptions());
        assertEquals(16, command.getOptionOrder(List.of(), Map.of()));
    }

    @Test
    void theOptionIsAddedOnceWhenTheRuleScriptRunsAgain() {
        when(options.hasOption(ExiledSectorAtlasOption.OPTION_ID)).thenReturn(true);

        command.execute("rule", dialog, List.of(), Map.of());

        verify(options, never()).addOption(anyString(), any(), anyString());
    }

    @Test
    void coloniesWithoutAGatewayAndOtherFactionsColoniesOfferNothing() {
        when(market.getIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(null);
        assertFalse(command.execute("rule", dialog, List.of(), Map.of()));

        when(market.getIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(mock(Industry.class));
        when(market.isPlayerOwned()).thenReturn(false);
        when(market.getFactionId()).thenReturn("hegemony");
        assertFalse(command.execute("rule", dialog, List.of(), Map.of()));

        verify(options, never()).addOption(anyString(), any(), anyString());
    }

    @Test
    void nothingIsOfferedWhileStarChartsAreSwitchedOff() {
        StarChartSettings.setEnabledForTests(false);
        try {
            assertFalse(command.execute("rule", dialog, List.of(), Map.of()));
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
        verify(options, never()).addOption(anyString(), any(), anyString());
    }

    @Test
    void choosingTheOptionOpensTheAtlas() {
        atlasMock.when(() -> AtlasScreen.open(eq(campaignUI), any())).thenReturn(true);

        assertTrue(command.execute("rule", dialog, List.of(new Misc.Token(ExiledSectorAtlasOption.OPEN, Misc.TokenType.LITERAL)), Map.of()));

        atlasMock.verify(() -> AtlasScreen.open(eq(campaignUI), any()), times(1));
        verify(options, never()).addOption(anyString(), any(), anyString());
    }

    @Test
    void closingTheAtlasRestoresTheColonyMenu() {
        ArgumentCaptor<Runnable> afterClose = ArgumentCaptor.forClass(Runnable.class);
        atlasMock.when(() -> AtlasScreen.open(eq(campaignUI), afterClose.capture())).thenReturn(true);
        Map<String, MemoryAPI> memoryMap = Map.of();
        command.execute("rule", dialog, List.of(new Misc.Token(ExiledSectorAtlasOption.OPEN, Misc.TokenType.LITERAL)), memoryMap);

        try (MockedStatic<FireAll> fireAll = Mockito.mockStatic(FireAll.class)) {
            afterClose.getValue().run();

            fireAll.verify(() -> FireAll.fire(null, dialog, memoryMap, ExiledSectorAtlasOption.POPULATE_OPTIONS), times(1));
        }
    }
}
