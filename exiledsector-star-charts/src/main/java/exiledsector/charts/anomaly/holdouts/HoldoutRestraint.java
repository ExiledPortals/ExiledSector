package exiledsector.charts.anomaly.holdouts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.anomaly.AnomalyRunScript;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class HoldoutRestraint extends AnomalyRunScript {

    static final float CHECK_DAYS = 0.25f;
    static final float RESTRAINT_DAYS = CHECK_DAYS * 2f;
    static final float OVERWHELMING_ADVANTAGE = 2f;

    private final String holdoutFactionId;
    private final List<String> holdoutFleetIds;

    HoldoutRestraint(String runId, int tier, StarSystemAPI system, String holdoutFactionId, List<String> holdoutFleetIds) {
        super(runId, tier, system, true);
        this.holdoutFactionId = holdoutFactionId;
        this.holdoutFleetIds = new ArrayList<>(holdoutFleetIds);
    }

    @Override
    protected String anomalyId() {
        return Holdouts.ANOMALY_ID;
    }

    @Override
    protected float checkDays() {
        return CHECK_DAYS;
    }

    @Override
    protected void check(SectorAPI sector, ChartRun run, float days) {
        for (String holdoutFleetId : holdoutFleetIds) {
            CampaignFleetAPI holdout = livingFleet(holdoutFleetId);
            if (holdout != null) {
                restrain(holdout);
            }
        }
    }

    private void restrain(CampaignFleetAPI holdout) {
        for (CampaignFleetAPI other : system().getFleets()) {
            String otherFactionId = other.getFaction() == null ? null : other.getFaction().getId();
            if (other == holdout || otherFactionId == null || holdoutFactionId.equals(otherFactionId)) {
                continue;
            }
            if (!other.isPlayerFleet()) {
                ChartAnomalyFleets.makeHostileTo(holdout, otherFactionId);
            }
            if (holdout.getAI() != null && !overwhelms(holdout, other)) {
                holdout.getAI().doNotAttack(other, RESTRAINT_DAYS);
            }
        }
    }

    static boolean overwhelms(CampaignFleetAPI holdout, CampaignFleetAPI target) {
        return holdout.getEffectiveStrength() >= target.getEffectiveStrength() * OVERWHELMING_ADVANTAGE;
    }
}
