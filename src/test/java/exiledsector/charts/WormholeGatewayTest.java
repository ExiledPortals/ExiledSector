package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeGatewayTest {

    private static final String PLAYER_FACTION = "player";

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaMock;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);
    private final List<MarketAPI> markets = new ArrayList<>();
    private final MemoryAPI sectorMemory = mock(MemoryAPI.class);

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getIndustrySpec(WormholeGateway.ID)).thenReturn(mock(IndustrySpecAPI.class));
        when(settings.getModManager()).thenReturn(modManager);
        SectorAPI sector = mock(SectorAPI.class);
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarketsCopy()).thenAnswer(invocation -> new ArrayList<>(markets));
        when(sector.getEconomy()).thenReturn(economy);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sectorMemory.getBoolean(WormholeGateway.JANUS_FLAG)).thenReturn(true);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
        lunaMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaMock.close();
        globalMock.close();
    }

    private MarketAPI colony(String name) {
        MarketAPI market = mock(MarketAPI.class);
        when(market.getName()).thenReturn(name);
        when(market.getFactionId()).thenReturn(PLAYER_FACTION);
        when(market.hasIndustry(Industries.POPULATION)).thenReturn(true);
        markets.add(market);
        return market;
    }

    private static WormholeGateway gatewayOn(MarketAPI market) {
        WormholeGateway gateway = new WormholeGateway();
        gateway.init(WormholeGateway.ID, market);
        return gateway;
    }

    @Test
    void aColonyCanBuildItWhenTheFactionHasNoOtherGateway() {
        MarketAPI colony = colony("Home");

        assertTrue(gatewayOn(colony).isAvailableToBuild());
        assertTrue(gatewayOn(colony).showWhenUnavailable());
    }

    @Test
    void switchingStarChartsOffHidesItFromTheBuildMenu() {
        MarketAPI colony = colony("Home");
        StarChartSettings.setEnabledForTests(false);
        try {
            assertFalse(gatewayOn(colony).isAvailableToBuild());
            assertFalse(gatewayOn(colony).showWhenUnavailable());
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    @Test
    void onlyOneGatewayIsAllowedPerFaction() {
        MarketAPI first = colony("Home");
        MarketAPI second = colony("Outpost");
        when(first.hasIndustry(WormholeGateway.ID)).thenReturn(true);

        WormholeGateway secondGateway = gatewayOn(second);

        assertFalse(secondGateway.isAvailableToBuild());
        assertSame(first, WormholeGateway.otherGatewayOf(second));
        assertTrue(secondGateway.getUnavailableReason().contains("Home"), secondGateway.getUnavailableReason());
        assertNull(WormholeGateway.otherGatewayOf(first));
    }

    @Test
    void theBuildMenuTooltipsCloneOfTheColonyIsNotAnotherGateway() {
        MarketAPI colony = colony("Home");
        when(colony.getId()).thenReturn("home");
        when(colony.hasIndustry(WormholeGateway.ID)).thenReturn(true);
        MarketAPI tooltipClone = mock(MarketAPI.class);
        when(tooltipClone.getId()).thenReturn("home");
        when(tooltipClone.getFactionId()).thenReturn(PLAYER_FACTION);
        when(tooltipClone.hasIndustry(Industries.POPULATION)).thenReturn(true);

        assertNull(WormholeGateway.otherGatewayOf(tooltipClone));
        assertTrue(gatewayOn(tooltipClone).isAvailableToBuild());
    }

    @Test
    void anotherFactionsGatewayDoesNotCount() {
        MarketAPI theirs = colony("Theirs");
        when(theirs.getFactionId()).thenReturn("hegemony");
        when(theirs.hasIndustry(WormholeGateway.ID)).thenReturn(true);

        assertTrue(gatewayOn(colony("Home")).isAvailableToBuild());
    }

    @Test
    void withoutVaultsOfKnowledgeItNeedsTheJanusDeviceLikeTheTreeWormholes() {
        when(sectorMemory.getBoolean(WormholeGateway.JANUS_FLAG)).thenReturn(false);
        WormholeGateway gateway = gatewayOn(colony("Home"));

        assertFalse(gateway.isAvailableToBuild());
        assertEquals("Requires the Janus Device.", gateway.getUnavailableReason());

        when(sectorMemory.getBoolean(WormholeGateway.JANUS_FLAG)).thenReturn(true);
        assertTrue(gateway.isAvailableToBuild());
    }

    @Test
    void withVaultsOfKnowledgeTheResearchReplacesTheJanusDevice() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        when(sectorMemory.getBoolean(WormholeGateway.JANUS_FLAG)).thenReturn(false);

        assertTrue(WormholeGateway.isUnlockedFor(colony("Home")));
    }

    @Test
    void withVaultsOfKnowledgeTheResearchIsCheckedAndAFailedLookupNeverLocksTheBuildingAway() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);

        assertTrue(VaultsOfKnowledgeCompat.isGatewayResearched(colony("Home")));
    }

    @Test
    void worksWithANullMarketForTooltipsAndPluginInstances() {
        WormholeGateway gateway = new WormholeGateway();

        assertDoesNotThrow(() -> gateway.init(WormholeGateway.ID, null));
        assertFalse(gateway.isAvailableToBuild());
        assertFalse(gateway.showWhenUnavailable());
        assertNotNull(gateway.getUnavailableReason());
        assertNull(WormholeGateway.otherGatewayOf(null));
    }

    @Test
    void takesNoAiCoresAndCannotBeImproved() {
        WormholeGateway gateway = gatewayOn(colony("Home"));

        assertFalse(gateway.canInstallAICores());
        assertFalse(gateway.canImprove());
    }

    @Test
    void reportsWhetherTheDockedColonysGatewayWorks() {
        assertEquals(WormholeGateway.Status.NONE, WormholeGateway.statusAt(null));
    }

    private static SectorEntityToken dockedAtGateway(MarketAPI market, boolean functional) {
        Industry gateway = mock(Industry.class);
        when(gateway.isFunctional()).thenReturn(functional);
        when(market.getIndustry(WormholeGateway.ID)).thenReturn(gateway);
        SectorEntityToken dockedAt = mock(SectorEntityToken.class);
        when(dockedAt.getMarket()).thenReturn(market);
        return dockedAt;
    }

    @Test
    void anAutonomousColonyOfThePlayerFactionStillOpensWormholes() {
        MarketAPI autonomous = colony("Autonomous");
        when(autonomous.isPlayerOwned()).thenReturn(false);

        assertEquals(WormholeGateway.Status.WORKING, WormholeGateway.statusAt(dockedAtGateway(autonomous, true)));
        assertEquals(WormholeGateway.Status.INACTIVE, WormholeGateway.statusAt(dockedAtGateway(autonomous, false)));
    }

    @Test
    void aPlayerOwnedColonyUnderACommissionFactionOpensWormholes() {
        MarketAPI commissioned = colony("Commissioned");
        when(commissioned.getFactionId()).thenReturn("hegemony");
        when(commissioned.isPlayerOwned()).thenReturn(true);

        assertEquals(WormholeGateway.Status.WORKING, WormholeGateway.statusAt(dockedAtGateway(commissioned, true)));
    }

    @Test
    void anotherFactionsColonyNeverOpensWormholes() {
        MarketAPI theirs = colony("Theirs");
        when(theirs.getFactionId()).thenReturn("hegemony");

        assertEquals(WormholeGateway.Status.NONE, WormholeGateway.statusAt(dockedAtGateway(theirs, true)));
    }
}
