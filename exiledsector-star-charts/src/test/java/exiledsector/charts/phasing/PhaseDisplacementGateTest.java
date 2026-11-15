package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseDisplacementGateTest {

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaMock;
    private MockedStatic<HypershuntPower> hypershuntMock;
    private final List<MarketAPI> markets = new ArrayList<>();
    private final MemoryAPI sectorMemory = mock(MemoryAPI.class);

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        List<String> industryIds = new ArrayList<>(WormholeGateway.INDUSTRY_IDS);
        industryIds.add(PhaseDisplacementGate.INDUSTRY_ID);
        for (String industryId : industryIds) {
            IndustrySpecAPI spec = mock(IndustrySpecAPI.class);
            when(spec.getId()).thenReturn(industryId);
            when(settings.getIndustrySpec(industryId)).thenReturn(spec);
        }
        when(settings.getModManager()).thenReturn(mock(ModManagerAPI.class));
        mockSector = new MockSector();
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarketsCopy()).thenAnswer(invocation -> new ArrayList<>(markets));
        when(mockSector.sector.getEconomy()).thenReturn(economy);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        lunaMock = Mockito.mockStatic(LunaSettings.class);
        hypershuntMock = Mockito.mockStatic(HypershuntPower.class, Mockito.CALLS_REAL_METHODS);
        hypershuntMock.when(() -> HypershuntPower.reaches(any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        hypershuntMock.close();
        lunaMock.close();
        mockSector.close();
    }

    private MarketAPI colonyWithGateway(String gatewayId) {
        MarketAPI market = mock(MarketAPI.class);
        when(market.getName()).thenReturn("Home");
        when(market.getFactionId()).thenReturn("player");
        when(market.hasIndustry(Industries.POPULATION)).thenReturn(true);
        if (gatewayId != null) {
            Industry gateway = mock(Industry.class);
            when(gateway.getId()).thenReturn(gatewayId);
            when(market.getIndustry(gatewayId)).thenReturn(gateway);
            when(market.hasIndustry(gatewayId)).thenReturn(true);
        }
        markets.add(market);
        return market;
    }

    private static PhaseDisplacementGate gateOn(MarketAPI market) {
        PhaseDisplacementGate gate = new PhaseDisplacementGate();
        gate.init(PhaseDisplacementGate.INDUSTRY_ID, market);
        return gate;
    }

    private void unlockWithTheTheorem() {
        when(sectorMemory.getBoolean(PhaseGateRules.UNLOCKED_FLAG)).thenReturn(true);
    }

    @Test
    void itStaysHiddenUntilTheTheoremIsUsed() {
        PhaseDisplacementGate gate = gateOn(colonyWithGateway(WormholeGateway.INDUSTRY_ID));

        assertFalse(gate.isAvailableToBuild());
        assertFalse(gate.showWhenUnavailable());
        assertEquals(PhaseGateRules.Blocker.NOT_UNLOCKED, PhaseGateRules.blocker(gate.getMarket()));
    }

    @Test
    void usingTheTheoremUnlocksItForTheGatewayColony() {
        unlockWithTheTheorem();
        PhaseDisplacementGate gate = gateOn(colonyWithGateway(WormholeGateway.INDUSTRY_ID));

        assertTrue(gate.isAvailableToBuild());
        assertTrue(gate.showWhenUnavailable());
    }

    @Test
    void aColonyWithoutTheGatewayCantBuildIt() {
        unlockWithTheTheorem();
        PhaseDisplacementGate gate = gateOn(colonyWithGateway(null));

        assertFalse(gate.isAvailableToBuild());
        assertTrue(gate.showWhenUnavailable());
        assertEquals("Requires a Wormhole Gateway at this colony.", gate.getUnavailableReason());
    }

    @Test
    void itNeedsAWorkingHypershuntInRange() {
        unlockWithTheTheorem();
        hypershuntMock.when(() -> HypershuntPower.reaches(any())).thenReturn(false);
        PhaseDisplacementGate gate = gateOn(colonyWithGateway(WormholeGateway.ARRAY_INDUSTRY_ID));

        assertFalse(gate.isAvailableToBuild());
        assertEquals("Requires a working coronal hypershunt within 10 light-years.", gate.getUnavailableReason());
    }

    @Test
    void switchingStarChartsOffHidesIt() {
        unlockWithTheTheorem();
        PhaseDisplacementGate gate = gateOn(colonyWithGateway(WormholeGateway.INDUSTRY_ID));
        StarChartSettings.setEnabledForTests(false);
        try {
            assertFalse(gate.isAvailableToBuild());
            assertFalse(gate.showWhenUnavailable());
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    @Test
    void withVaultsOfKnowledgeItNeedsTheResearchAndTheGatewayNexus() {
        try (MockedStatic<VaultsOfKnowledgeCompat> compat = Mockito.mockStatic(VaultsOfKnowledgeCompat.class, Mockito.CALLS_REAL_METHODS)) {
            compat.when(VaultsOfKnowledgeCompat::isModEnabled).thenReturn(true);
            MarketAPI arrayColony = colonyWithGateway(WormholeGateway.ARRAY_INDUSTRY_ID);
            compat.when(() -> VaultsOfKnowledgeCompat.isResearchedAt(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID, arrayColony)).thenReturn(false);

            assertEquals(PhaseGateRules.Blocker.NOT_UNLOCKED, PhaseGateRules.blocker(arrayColony));

            compat.when(() -> VaultsOfKnowledgeCompat.isResearchedAt(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID, arrayColony)).thenReturn(true);
            assertEquals(PhaseGateRules.Blocker.NO_GATEWAY, PhaseGateRules.blocker(arrayColony));
            assertEquals("Requires a Gateway Nexus at this colony.", gateOn(arrayColony).getUnavailableReason());

            MarketAPI nexusColony = colonyWithGateway(WormholeGateway.NEXUS_INDUSTRY_ID);
            compat.when(() -> VaultsOfKnowledgeCompat.isResearchedAt(VaultsOfKnowledgeCompat.PHASE_GATE_RESEARCH_ID, nexusColony)).thenReturn(true);
            assertNull(PhaseGateRules.blocker(nexusColony));
        }
    }

    @Test
    void thePlayersGateMarketIsTheOneHoldingTheStructure() {
        MarketAPI without = colonyWithGateway(null);
        MarketAPI with = colonyWithGateway(WormholeGateway.INDUSTRY_ID);
        when(with.hasIndustry(PhaseDisplacementGate.INDUSTRY_ID)).thenReturn(true);

        assertSame(with, PhaseDisplacementGate.playerGateMarket(mockSector.sector));
        assertFalse(without.hasIndustry(PhaseDisplacementGate.INDUSTRY_ID));
    }
}
