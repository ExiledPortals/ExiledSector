package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.ChartIds;
import exiledsector.data.EnumKeys;

enum FightingSide {
    ATTACKER,
    DEFENDER;

    String key() {
        return EnumKeys.key(this);
    }

    static FightingSide of(CampaignFleetAPI fleet) {
        String key = fleet == null ? null : fleet.getMemoryWithoutUpdate().getString(ChartIds.SKIRMISH_SIDE_MEMORY_KEY);
        return key == null ? null : EnumKeys.parse(FightingSide.class, key);
    }
}
