package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayTierLimitTest {

    private static MockedStatic<VaultsOfKnowledgeCompat> vaults(boolean enabled) {
        MockedStatic<VaultsOfKnowledgeCompat> compat = Mockito.mockStatic(VaultsOfKnowledgeCompat.class);
        compat.when(VaultsOfKnowledgeCompat::isModEnabled).thenReturn(enabled);
        return compat;
    }

    private static SectorAPI sectorWithGateway(String industryId) {
        MarketAPI colony = mock(MarketAPI.class);
        when(colony.isPlayerOwned()).thenReturn(true);
        when(colony.hasIndustry(industryId)).thenReturn(true);
        Industry gateway = mock(Industry.class);
        when(gateway.getId()).thenReturn(industryId);
        when(colony.getIndustry(industryId)).thenReturn(gateway);
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarketsCopy()).thenReturn(List.of(colony));
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getEconomy()).thenReturn(economy);
        return sector;
    }

    @Test
    void withoutVaultsOfKnowledgeEveryTierOpens() {
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(false)) {
            assertEquals(ChartTiers.CORRUPTED_TIER, GatewayTierLimit.maxTier(sectorWithGateway(WormholeGateway.INDUSTRY_ID)));
        }
    }

    @Test
    void theBuiltGatewayRaisesTheLimitFromFiveToTenToEveryTier() {
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(true)) {
            assertEquals(5, GatewayTierLimit.maxTier(sectorWithGateway(WormholeGateway.INDUSTRY_ID)));
            assertEquals(10, GatewayTierLimit.maxTier(sectorWithGateway(WormholeGateway.ADVANCED_INDUSTRY_ID)));
            assertEquals(ChartTiers.CORRUPTED_TIER, GatewayTierLimit.maxTier(sectorWithGateway(WormholeGateway.PERFECTED_INDUSTRY_ID)));
        }
    }

    @Test
    void withoutASectorThereIsNoLimit() {
        try (MockedStatic<VaultsOfKnowledgeCompat> ignored = vaults(true)) {
            assertEquals(ChartTiers.CORRUPTED_TIER, GatewayTierLimit.maxTier(null));
        }
    }
}
