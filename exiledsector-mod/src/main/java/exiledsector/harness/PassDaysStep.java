package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

final class PassDaysStep implements HarnessStep {

    static final int DEFAULT_SPEED = 50;
    static final float VANILLA_SECONDS_PER_DAY = 10f;
    static final float SLOWEST_ITERATION_SECONDS = 0.03f;

    private final float days;
    private final int speed;
    private CampaignTimeSkip timeSkip;
    private long startTimestamp;

    PassDaysStep(float days, int speed) {
        if (days <= 0f || speed < 1) {
            throw new IllegalArgumentException("passDays needs days above 0 and a speed of at least 1, not " + days + " days at " + speed);
        }
        this.days = days;
        this.speed = speed;
    }

    @Override
    public String name() {
        return "passDays " + days + " at x" + speed;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        SectorAPI sector = Global.getSector();
        if (context.firstFrame()) {
            StepOutcome notInCampaign = HarnessCampaign.notInCampaign("passDays");
            if (notInCampaign != null) {
                return notInCampaign;
            }
            startTimestamp = sector.getClock().getTimestamp();
            timeSkip = new CampaignTimeSkip(speed - 1);
            timeSkip.start();
        }
        CampaignClockAPI clock = sector.getClock();
        float passed = clock.getElapsedDaysSince(startTimestamp);
        if (timeSkip.failure() != null) {
            return StepOutcome.failed("stopped after " + passed + " days: " + timeSkip.failure());
        }
        if (passed >= days) {
            timeSkip.stop();
            return StepOutcome.passed("passed " + Math.round(passed * 10f) / 10f + " days; it's now " + clock.getDateString());
        }
        HarnessCampaign.unpause(sector);
        timeSkip.keepRunning();
        String blocked = sector.getCampaignUI().isShowingDialog() ? ", held up by an open dialog" : "";
        return new StepOutcome(false, false, "passed " + Math.round(passed * 10f) / 10f + " of " + days + " days" + blocked);
    }

    @Override
    public float timeoutSeconds() {
        float iterations = days * VANILLA_SECONDS_PER_DAY / CampaignTimeSkip.ITERATION_SECONDS;
        return DEFAULT_TIMEOUT_SECONDS + iterations * SLOWEST_ITERATION_SECONDS;
    }
}
