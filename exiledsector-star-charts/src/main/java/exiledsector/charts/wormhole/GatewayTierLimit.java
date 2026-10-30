package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.compat.VaultsOfKnowledgeCompat;

public final class GatewayTierLimit {

    public static final int BASE_MAX_TIER = 5;
    public static final int ADVANCED_MAX_TIER = 10;

    private GatewayTierLimit() {
    }

    public static int maxTier(SectorAPI sector) {
        if (sector == null || !VaultsOfKnowledgeCompat.isModEnabled()) {
            return ChartTiers.CORRUPTED_TIER;
        }
        return maxTierFor(WormholeGateway.playerGatewayId(sector));
    }

    static int maxTierFor(String industryId) {
        if (WormholeGateway.PERFECTED_INDUSTRY_ID.equals(industryId)) {
            return ChartTiers.CORRUPTED_TIER;
        }
        return WormholeGateway.ADVANCED_INDUSTRY_ID.equals(industryId) ? ADVANCED_MAX_TIER : BASE_MAX_TIER;
    }
}
