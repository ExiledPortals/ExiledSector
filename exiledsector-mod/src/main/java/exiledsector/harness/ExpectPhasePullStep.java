package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.charts.phasing.PhaseDisplacementGate;
import exiledsector.charts.phasing.PhaseGateState;
import exiledsector.charts.phasing.PhasePull;
import exiledsector.charts.phasing.PhasedPlanetRecord;
import org.json.JSONException;
import org.json.JSONObject;

final class ExpectPhasePullStep implements HarnessStep {

    private final String dataName;

    ExpectPhasePullStep(String dataName) {
        this.dataName = dataName;
    }

    @Override
    public String name() {
        return "expectPhasePull " + dataName;
    }

    @Override
    public StepOutcome advance(StepContext context) throws JSONException {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("expectPhasePull");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        SectorAPI sector = Global.getSector();
        MarketAPI market = HarnessCampaign.gateMarket(sector);
        if (market == null) {
            return StepOutcome.failed("the player has no Phase Displacement Gate; " + HarnessCampaign.colonyStatus(sector, context.results()));
        }
        PhasedPlanetRecord job = PhaseGateState.job(market);
        if (job == null) {
            PhasedPlanetRecord lock = PhaseGateState.lock(market);
            return StepOutcome.failed("the gate on " + market.getName() + " has no pull job" + (lock == null ? " and no lock" : "; it still holds the lock on " + lock.name()));
        }
        PhasePull.Pause pause = PhasePull.pauseFor((PhaseDisplacementGate) market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID));
        SectorEntityToken wormhole = sector.getEntityById(PhasePull.wormholeId(market));
        float days = PhaseGateState.jobDays(market);
        JSONObject data = new JSONObject();
        data.put("planet", job.name());
        data.put("days", days);
        data.put("pullDays", PhasePull.PULL_DAYS);
        data.put("pause", pause == null ? JSONObject.NULL : pause.name());
        data.put("orbit", PhaseGateState.jobOrbit(market));
        data.put("wormholeRadius", wormhole == null ? JSONObject.NULL : wormhole.getRadius());
        context.results().addData(dataName, data);
        String progress = job.name() + " is " + Math.round(days) + " of " + Math.round(PhasePull.PULL_DAYS) + " days in, wormhole radius "
                + (wormhole == null ? "none" : String.valueOf(Math.round(wormhole.getRadius())));
        return pause == null ? StepOutcome.passed(progress) : StepOutcome.failed(progress + ", paused for " + pause);
    }
}
