package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public final class ChartFleetVisibility {

    public static final float REVEAL_RANGE_MULT = 1000f;
    public static final float REVEAL_EXTENDED_RANGE = 100000f;

    private ChartFleetVisibility() {
    }

    public static void reveal(CampaignFleetAPI fleet, String modId) {
        if (fleet.getStats() != null) {
            fleet.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).modifyMult(modId, REVEAL_RANGE_MULT);
        }
        fleet.setExtendedDetectedAtRange(REVEAL_EXTENDED_RANGE);
    }

    public static boolean isRevealed(CampaignFleetAPI fleet) {
        return fleet.getExtendedDetectedAtRange() >= REVEAL_EXTENDED_RANGE;
    }
}
