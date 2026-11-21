package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

import java.util.List;

final class CampaignTimeSkip implements CampaignInputListener {

    static final float ITERATION_SECONDS = 0.1f;
    private static final float NO_ENGAGING_SECONDS = 1f;
    private static final String ADVANCE = "advance";
    private static final String FAST_FORWARD_ITERATION = "setFastForwardIteration";
    private static final Logger LOG = Logger.getLogger(CampaignTimeSkip.class);

    private final int extraIterationsPerFrame;
    private boolean requested;
    private boolean stopped;
    private String failure;

    CampaignTimeSkip(int extraIterationsPerFrame) {
        this.extraIterationsPerFrame = extraIterationsPerFrame;
    }

    void start() {
        Global.getSector().getListenerManager().addListener(this, true);
    }

    void keepRunning() {
        requested = true;
    }

    void stop() {
        if (!stopped) {
            stopped = true;
            Global.getSector().getListenerManager().removeListener(this);
        }
    }

    String failure() {
        return failure;
    }

    // java:S1181: the extra engine steps reach engine internals by reflection, and any failure must stop the skip and reach the step
    @SuppressWarnings("java:S1181")
    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
        if (stopped) {
            return;
        }
        if (!requested) {
            stop();
            return;
        }
        requested = false;
        SectorAPI sector = Global.getSector();
        try {
            for (int iteration = 0; iteration < extraIterationsPerFrame && !stopped; iteration++) {
                if (sector.isPaused() || sector.getCampaignUI().isShowingDialog()) {
                    break;
                }
                sector.getPlayerFleet().setNoEngaging(NO_ENGAGING_SECONDS);
                UiReflection.call(sector, FAST_FORWARD_ITERATION, true);
                UiReflection.call(sector, ADVANCE, ITERATION_SECONDS, null);
            }
        } catch (Throwable e) {
            LOG.error(Harness.LOG_TAG + "could not step the campaign ahead", e);
            failure = e.toString();
            stop();
        } finally {
            setFastForwardIteration(sector, false);
        }
    }

    // java:S1181: resetting the engine flag is best effort once a step already failed
    @SuppressWarnings("java:S1181")
    private static void setFastForwardIteration(SectorAPI sector, boolean fastForward) {
        try {
            UiReflection.call(sector, FAST_FORWARD_ITERATION, fastForward);
        } catch (Throwable e) {
            LOG.warn(Harness.LOG_TAG + "could not reset the fast-forward flag", e);
        }
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {
    }

    @Override
    public int getListenerInputPriority() {
        return 0;
    }
}
