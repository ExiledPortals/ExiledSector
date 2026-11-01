package exiledsector.charts.anomaly;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.PlayerFleet;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.function.Supplier;

public abstract class AnomalyRunScript implements EveryFrameScript, ChartRunBound {

    private static final Logger LOG = Logger.getLogger(AnomalyRunScript.class);

    private final String runId;
    private final int tier;
    private final StarSystemAPI system;
    private float daysSinceCheck;
    private boolean done;
    private transient boolean failureReported;

    protected AnomalyRunScript(String runId, int tier, StarSystemAPI system, boolean checkOnFirstFrame) {
        this.runId = runId;
        this.tier = tier;
        this.system = system;
        this.daysSinceCheck = checkOnFirstFrame ? Float.MAX_VALUE : 0f;
    }

    public static <T extends AnomalyRunScript> T in(LocationAPI location, Class<T> type) {
        if (location == null) {
            return null;
        }
        for (EveryFrameScript script : location.getScripts()) {
            if (type.isInstance(script) && !script.isDone()) {
                return type.cast(script);
            }
        }
        return null;
    }

    protected abstract String anomalyId();

    protected abstract float checkDays();

    protected abstract void check(SectorAPI sector, ChartRun run, float days);

    protected void everyFrame(SectorAPI sector, float amount) {
    }

    public String runId() {
        return runId;
    }

    public int tier() {
        return tier;
    }

    public StarSystemAPI system() {
        return system;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }
        if (ChartRuns.unclosed(runId) == null) {
            done = true;
            return;
        }
        ChartRun run = liveRun();
        if (run == null) {
            return;
        }
        try {
            everyFrame(sector, amount);
            daysSinceCheck += sector.getClock().convertToDays(amount);
            if (daysSinceCheck < checkDays()) {
                return;
            }
            float days = daysSinceCheck;
            daysSinceCheck = 0f;
            check(sector, run, days);
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Running the " + anomalyId() + " anomaly failed; it is tried again next check", e);
            }
        }
    }

    protected ChartRun liveRun() {
        return ChartRuns.live(runId);
    }

    public SectorEntityToken entity(String entityId) {
        return entityId == null ? null : system.getEntityById(entityId);
    }

    protected CampaignFleetAPI livingFleet(String fleetId) {
        return entity(fleetId) instanceof CampaignFleetAPI fleet && fleet.isAlive() ? fleet : null;
    }

    protected boolean playerInSystem() {
        return PlayerFleet.isIn(system);
    }

    protected void tellPlayer(Supplier<String> text, Supplier<Color> colour) {
        if (CampaignMessages.available() && playerInSystem()) {
            CampaignMessages.plain(text.get(), colour.get());
        }
    }
}
