package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.generation.ChartTiers;

public final class GatewayTierLimit {

    public static final int BASE_MAX_TIER = 5;
    public static final int ARRAY_MAX_TIER = 10;

    private GatewayTierLimit() {
    }

    public static int maxTier(SectorAPI sector) {
        if (sector == null || !WormholeGateway.researchGated()) {
            return ChartTiers.CORRUPTED_TIER;
        }
        return maxTierFor(WormholeGateway.playerGatewayId(sector));
    }

    static int maxTierFor(String industryId) {
        if (WormholeGateway.NEXUS_INDUSTRY_ID.equals(industryId)) {
            return ChartTiers.CORRUPTED_TIER;
        }
        return WormholeGateway.ARRAY_INDUSTRY_ID.equals(industryId) ? ARRAY_MAX_TIER : BASE_MAX_TIER;
    }
}
