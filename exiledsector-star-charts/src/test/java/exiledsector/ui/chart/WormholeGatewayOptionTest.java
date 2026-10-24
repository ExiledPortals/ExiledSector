package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI;
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.WormholeGateway;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.socket.BenchMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WormholeGatewayOptionTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<ModificationBenchCargoLauncher> launcherMock;
    private CampaignUIAPI campaignUI;
    private MarketAPI market;
    private Industry gateway;
    private final WormholeGatewayOption provider = new WormholeGatewayOption();

    @BeforeEach
    void setUp() {
        SectorAPI sector = mock(SectorAPI.class, Mockito.RETURNS_DEEP_STUBS);
        campaignUI = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        globalMock.when(Global::getSector).thenReturn(sector);
        launcherMock = Mockito.mockStatic(ModificationBenchCargoLauncher.class);
        market = mock(MarketAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(market.isPlayerOwned()).thenReturn(true);
        gateway = industry(WormholeGateway.INDUSTRY_ID, market);
    }

    @AfterEach
    void tearDown() {
        launcherMock.close();
        globalMock.close();
    }

    private static Industry industry(String id, MarketAPI market) {
        Industry industry = mock(Industry.class);
        when(industry.getId()).thenReturn(id);
        when(industry.getMarket()).thenReturn(market);
        return industry;
    }

    @Test
    void onlyThePlayersGatewayOffersStarChartsEvenWhileUnderConstruction() {
        when(gateway.isBuilding()).thenReturn(true);

        List<IndustryOptionProvider.IndustryOptionData> options = provider.getIndustryOptions(gateway);

        assertEquals(1, options.size());
        assertSame(WormholeGatewayOption.OPEN_STAR_CHARTS, options.get(0).id);
        assertSame(gateway, options.get(0).ind);
        assertSame(provider, options.get(0).provider);
        assertTrue(options.get(0).enabled);
    }

    @Test
    void aBuiltGatewayOffersNothingWhileStarChartsAreSwitchedOff() {
        StarChartSettings.setEnabledForTests(false);
        try {
            assertTrue(provider.getIndustryOptions(gateway).isEmpty());
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    @Test
    void otherStructuresAndOtherFactionsGatewaysOfferNothing() {
        assertTrue(provider.getIndustryOptions(industry("population", market)).isEmpty());
        assertTrue(provider.getIndustryOptions(industry(WormholeGateway.INDUSTRY_ID, null)).isEmpty());
        assertTrue(provider.getIndustryOptions(null).isEmpty());

        when(market.isPlayerOwned()).thenReturn(false);
        assertTrue(provider.getIndustryOptions(gateway).isEmpty());
    }

    @Test
    void choosingTheOptionOpensTheCraftingWindowOnStarCharts() {
        IndustryOptionProvider.IndustryOptionData option = provider.getIndustryOptions(gateway).get(0);

        provider.optionSelected(new IndustryOptionProvider.IndustryOptionData("other", new Object(), gateway, provider), mock(DialogCreatorUI.class));
        launcherMock.verify(() -> ModificationBenchCargoLauncher.openMode(any(), any()), never());

        provider.optionSelected(option, mock(DialogCreatorUI.class));
        launcherMock.verify(() -> ModificationBenchCargoLauncher.openMode(campaignUI, BenchMode.STAR_CHARTS), times(1));
    }

    @Test
    void theOptionTooltipDescribesTheStarChartsPage() {
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class);

        provider.createTooltip(provider.getIndustryOptions(gateway).get(0), tooltip, 300f);

        verify(tooltip).addPara(eq("%s"), anyFloat(), nullable(Color.class), nullable(Color.class), anyString());
    }
}
