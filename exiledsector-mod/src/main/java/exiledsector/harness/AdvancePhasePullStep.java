package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import exiledsector.charts.phasing.PhaseDisplacementGate;
import exiledsector.charts.phasing.PhaseGateEntityPlugin;
import exiledsector.charts.phasing.PhaseGateState;
import exiledsector.charts.phasing.PhasePull;
import exiledsector.charts.phasing.PhasedPlanetRecord;

final class AdvancePhasePullStep implements HarnessStep {

    static final float MIN_FINAL_DAYS = 0.01f;

    private final float days;
    private MarketAPI market;
    private SectorEntityToken phaseGate;
    private String planetName;

    AdvancePhasePullStep(float days) {
        this.days = days;
    }

    @Override
    public String name() {
        return days > 0f ? "advancePhasePull " + days + " days" : "advancePhasePull to the end";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        SectorAPI sector = Global.getSector();
        if (context.firstFrame()) {
            return start(sector);
        }
        HarnessCampaign.unpause(sector);
        boolean gateGone = phaseGate == null || phaseGate.isExpired() || phaseGate.getContainingLocation() == null;
        if (!PhaseGateState.hasJob(market) && gateGone) {
            return StepOutcome.passed(planetName + " phased in and its gate faded out");
        }
        String state = PhaseGateState.hasJob(market) ? "phasing in" : "planet placed, gate fading";
        return new StepOutcome(false, false, planetName + " is " + state);
    }

    private StepOutcome start(SectorAPI sector) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        market = HarnessCampaign.gateMarket(sector);
        PhasedPlanetRecord job = market == null ? null : PhaseGateState.job(market);
        if (job == null) {
            return StepOutcome.failed("the player has no Phase Displacement Gate with a running pull");
        }
        planetName = job.name();
        phaseGate = PhasePull.gateOf(sector, market);
        if (phaseGate == null || !phaseGate.isInCurrentLocation()) {
            return StepOutcome.failed("the player fleet isn't in the system of " + planetName + "'s phase gate");
        }
        if (!(market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID) instanceof PhaseDisplacementGate gate)) {
            return StepOutcome.failed(market.getName() + " has no Phase Displacement Gate");
        }
        PhasePull.Pause pause = PhasePull.pauseFor(gate);
        if (pause != null) {
            return StepOutcome.failed("the pull of " + planetName + " is paused for " + pause);
        }
        float finalDays = days > 0f ? days : Math.max(MIN_FINAL_DAYS, PhasePull.PULL_DAYS - PhaseGateState.jobDays(market));
        if (PhasePull.advance(sector, gate, finalDays) != null) {
            return StepOutcome.passed(planetName + " arrived without its phase-in");
        }
        boolean arriving = phaseGate.getCustomPlugin() instanceof PhaseGateEntityPlugin plugin && plugin.isArriving();
        if (!arriving && PhaseGateState.hasJob(market) && PhaseGateState.jobDays(market) < PhasePull.PULL_DAYS) {
            return StepOutcome.passed(planetName + " is " + Math.round(PhaseGateState.jobDays(market)) + " of " + Math.round(PhasePull.PULL_DAYS) + " days in");
        }
        if (!arriving) {
            return StepOutcome.failed("the last " + finalDays + " days passed but " + planetName + "'s gate didn't start its phase-in");
        }
        return new StepOutcome(false, false, planetName + " started phasing in after the last " + finalDays + " days");
    }
}
