package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.ExiledSector;
import exiledsector.domain.ExiledSectorDomain;
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

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaMock;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);
    private final List<MarketAPI> markets = new ArrayList<>();
    private final MemoryAPI sectorMemory = mock(MemoryAPI.class);

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        for (String industryId : WormholeGateway.INDUSTRY_IDS) {
            IndustrySpecAPI spec = mock(IndustrySpecAPI.class);
            when(spec.getId()).thenReturn(industryId);
            when(spec.getUpgrade()).thenReturn(WormholeGateway.PERFECTED_INDUSTRY_ID.equals(industryId) ? null : "next");
            when(settings.getIndustrySpec(industryId)).thenReturn(spec);
        }
        when(settings.getModManager()).thenReturn(modManager);
        mockSector = new MockSector();
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarketsCopy()).thenAnswer(invocation -> new ArrayList<>(markets));
        when(mockSector.sector.getEconomy()).thenReturn(economy);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(true);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        lunaMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaMock.close();
        mockSector.close();
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
        return gatewayOn(market, WormholeGateway.INDUSTRY_ID);
    }

    private static WormholeGateway gatewayOn(MarketAPI market, String industryId) {
        WormholeGateway gateway = new WormholeGateway();
        gateway.init(industryId, market);
        return gateway;
    }

    private static MockedStatic<VaultsOfKnowledgeCompat> vaults(boolean advanced, boolean perfected) {
        MockedStatic<VaultsOfKnowledgeCompat> compat = Mockito.mockStatic(VaultsOfKnowledgeCompat.class, Mockito.CALLS_REAL_METHODS);
        compat.when(VaultsOfKnowledgeCompat::isModEnabled).thenReturn(true);
        compat.when(() -> VaultsOfKnowledgeCompat.isResearchedForPlayer(VaultsOfKnowledgeCompat.GATEWAY_ADVANCED_RESEARCH_ID)).thenReturn(advanced);
        compat.when(() -> VaultsOfKnowledgeCompat.isResearchedForPlayer(VaultsOfKnowledgeCompat.GATEWAY_PERFECTED_RESEARCH_ID)).thenReturn(perfected);
        return compat;
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
        when(first.hasIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(true);

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
        when(colony.hasIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(true);
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
        when(theirs.hasIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(true);

        assertTrue(gatewayOn(colony("Home")).isAvailableToBuild());
    }

    @Test
    void withoutVaultsOfKnowledgeItNeedsTheBlueprintUnlock() {
        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(false);
        WormholeGateway gateway = gatewayOn(colony("Home"));

        assertFalse(gateway.isAvailableToBuild());
        assertEquals("Requires the Wormhole Tech Blueprint.", gateway.getUnavailableReason());

        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(true);
        assertTrue(gateway.isAvailableToBuild());
    }

    @Test
    void theJanusDeviceNoLongerUnlocksIt() {
        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(false);
        when(sectorMemory.getBoolean("$playerCanUseGates")).thenReturn(true);

        assertFalse(WormholeGateway.isUnlockedFor(colony("Home")));
    }

    @Test
    void theUnlockBypassOpensItWithoutTheBlueprint() {
        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(false);
        ExiledSectorDomain bypass = mock(ExiledSectorDomain.class);
        when(bypass.ignoresMemoryFlagUnlocks()).thenReturn(true);
        try (MockedStatic<ExiledSector> exiledSector = Mockito.mockStatic(ExiledSector.class, Mockito.CALLS_REAL_METHODS)) {
            exiledSector.when(ExiledSector::domains).thenReturn(List.of(bypass));

            assertTrue(WormholeGateway.isUnlockedFor(colony("Home")));
        }
    }

    @Test
    void unlockingSetsThePersistentSectorFlag() {
        WormholeGateway.unlock(mockSector.sector);

        Mockito.verify(sectorMemory).set(WormholeGateway.UNLOCKED_FLAG, true);
    }

    @Test
    void anUpgradeNeedsItsResearchAndTheGatewayBeforeIt() {
        MarketAPI colony = colony("Home");
        when(colony.hasIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(true);
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(false, false)) {
            WormholeGateway advanced = gatewayOn(colony, WormholeGateway.ADVANCED_INDUSTRY_ID);
            assertFalse(advanced.isAvailableToBuild());
            assertEquals("Requires the Gateway Array research.", advanced.getUnavailableReason());
        }
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(true, true)) {
            assertTrue(gatewayOn(colony, WormholeGateway.ADVANCED_INDUSTRY_ID).isAvailableToBuild());
            assertFalse(gatewayOn(colony, WormholeGateway.PERFECTED_INDUSTRY_ID).isAvailableToBuild());
            when(colony.hasIndustry(WormholeGateway.ADVANCED_INDUSTRY_ID)).thenReturn(true);
            assertTrue(gatewayOn(colony, WormholeGateway.PERFECTED_INDUSTRY_ID).isAvailableToBuild());
        }
    }

    @Test
    void theGatewayOnlyUpgradesWithVaultsOfKnowledge() {
        MarketAPI colony = colony("Home");

        assertFalse(gatewayOn(colony).canUpgrade());
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(true, true)) {
            assertTrue(gatewayOn(colony).canUpgrade());
            assertTrue(gatewayOn(colony, WormholeGateway.ADVANCED_INDUSTRY_ID).canUpgrade());
            assertFalse(gatewayOn(colony, WormholeGateway.PERFECTED_INDUSTRY_ID).canUpgrade());
        }
    }

    @Test
    void anUpgradedGatewayStillCountsAsTheFactionsGateway() {
        MarketAPI first = colony("Home");
        when(first.hasIndustry(WormholeGateway.PERFECTED_INDUSTRY_ID)).thenReturn(true);
        when(first.isPlayerOwned()).thenReturn(true);
        Industry perfected = mock(Industry.class);
        when(perfected.getId()).thenReturn(WormholeGateway.PERFECTED_INDUSTRY_ID);
        when(first.getIndustry(WormholeGateway.PERFECTED_INDUSTRY_ID)).thenReturn(perfected);

        assertFalse(gatewayOn(colony("Outpost")).isAvailableToBuild());
        assertEquals(WormholeGateway.PERFECTED_INDUSTRY_ID, WormholeGateway.playerGatewayId(mockSector.sector));
    }

    @Test
    void withVaultsOfKnowledgeTheResearchReplacesTheBlueprintUnlock() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(false);

        assertTrue(WormholeGateway.isUnlockedFor(colony("Home")));
    }

    @Test
    void withoutAColonyRequirementTheGatewayIsHiddenAndCannotBeBuilt() {
        StarChartSettings.setNoColonyForTests(true);
        try {
            WormholeGateway gateway = gatewayOn(colony("Home"));

            assertFalse(gateway.isAvailableToBuild());
            assertFalse(gateway.showWhenUnavailable());
        } finally {
            StarChartSettings.setNoColonyForTests(StarChartSettings.DEFAULT_NO_COLONY);
        }
    }

    @Test
    void withoutAColonyRequirementVaultsOfKnowledgeNeedsOnlyTheBlueprint() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);
        StarChartSettings.setNoColonyForTests(true);
        try {
            assertFalse(WormholeGateway.researchGated());
            when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(false);
            assertFalse(WormholeGateway.isUnlockedFor(colony("Home")));
            when(sectorMemory.getBoolean(WormholeGateway.UNLOCKED_FLAG)).thenReturn(true);
            assertTrue(WormholeGateway.isUnlockedFor(colony("Home")));
        } finally {
            StarChartSettings.setNoColonyForTests(StarChartSettings.DEFAULT_NO_COLONY);
        }
    }

    @Test
    void withVaultsOfKnowledgeTheResearchIsCheckedAndAFailedLookupNeverLocksTheBuildingAway() {
        when(modManager.isModEnabled(VaultsOfKnowledgeCompat.MOD_ID)).thenReturn(true);

        assertTrue(VaultsOfKnowledgeCompat.isGatewayResearched(colony("Home")));
    }

    @Test
    void worksWithANullMarketForTooltipsAndPluginInstances() {
        WormholeGateway gateway = new WormholeGateway();

        assertDoesNotThrow(() -> gateway.init(WormholeGateway.INDUSTRY_ID, null));
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
        when(market.getIndustry(WormholeGateway.INDUSTRY_ID)).thenReturn(gateway);
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
