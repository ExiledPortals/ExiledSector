package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class PostScript implements EveryFrameScript, FleetEventListener, ChartRunBound {

    private static final Logger LOG = Logger.getLogger(PostScript.class);

    private final String runId;
    private final int tier;
    private final StarSystemAPI system;
    private final String postId;
    private final List<PicketStation> stations = new ArrayList<>();
    private String guardId;
    private float daysSinceCheck;
    private boolean done;
    private transient boolean failureReported;

    public PostScript(String runId, int tier, StarSystemAPI system, String postId) {
        this.runId = runId;
        this.tier = tier;
        this.system = system;
        this.postId = postId;
    }

    public static PostScript in(LocationAPI location) {
        if (location == null) {
            return null;
        }
        for (EveryFrameScript script : location.getScripts()) {
            if (script instanceof PostScript postScript && !postScript.isDone()) {
                return postScript;
            }
        }
        return null;
    }

    void add(PicketStation station) {
        stations.add(station);
    }

    List<PicketStation> stations() {
        return List.copyOf(stations);
    }

    public String runId() {
        return runId;
    }

    public int tier() {
        return tier;
    }

    public SectorEntityToken post() {
        return postId == null ? null : system.getEntityById(postId);
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

    ChartRun run() {
        return ChartRuns.live(runId);
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
        ChartRun run = run();
        if (run == null) {
            return;
        }
        daysSinceCheck += sector.getClock().convertToDays(amount);
        if (daysSinceCheck < HeistRules.CHECK_DAYS) {
            return;
        }
        float days = daysSinceCheck;
        daysSinceCheck = 0f;
        try {
            check(sector, run, days);
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Running the listening post failed; it is tried again next check", e);
            }
        }
    }

    void check(SectorAPI sector, ChartRun run, float days) {
        PostState state = PostState.of(run);
        if (state == null || state.taken()) {
            return;
        }
        SectorEntityToken post = post();
        if (state.alarm()) {
            PostState.record(run, state.withVault(HeistRules.vaultAfter(state.vaultShare(), days, purgeReduction(run))));
            mergeArrivals(post);
            return;
        }
        if (post != null) {
            stations.forEach(station -> station.follow(post.getLocation()));
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getContainingLocation() != system) {
            PostState.record(run, state.withSuspicion(HeistRules.suspicionAfter(state.suspicion(), false, days)));
            return;
        }
        detect(run, state, days);
    }

    void detect(ChartRun run, PostState state, float days) {
        boolean contact = false;
        boolean reported = false;
        for (CampaignFleetAPI picket : livingPickets()) {
            VisibilityLevel level = picket.getVisibilityLevelOfPlayerFleet();
            if (level == VisibilityLevel.SENSOR_CONTACT) {
                contact = true;
            } else if (level == VisibilityLevel.COMPOSITION_DETAILS || level == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
                contact = true;
                if (Pickets.reportDays(picket) < 0f) {
                    Pickets.setReportDays(picket, HeistRules.REPORT_DAYS);
                    message("chart.post.identified");
                }
            }
            float reportDays = Pickets.reportDays(picket);
            if (reportDays >= 0f) {
                float left = reportDays - days;
                Pickets.setReportDays(picket, Math.max(0f, left));
                reported |= left <= 0f;
            }
        }
        float suspicion = HeistRules.suspicionAfter(state.suspicion(), contact, days);
        PostState.record(run, state.withSuspicion(suspicion));
        if (reported || suspicion >= 1f) {
            raiseAlarm(run);
        }
    }

    float reportDaysLeft() {
        float soonest = -1f;
        for (CampaignFleetAPI picket : livingPickets()) {
            float days = Pickets.reportDays(picket);
            if (days >= 0f && (soonest < 0f || days < soonest)) {
                soonest = days;
            }
        }
        return soonest;
    }

    public boolean raiseAlarm(ChartRun run) {
        PostState state = PostState.of(run);
        if (state == null || !state.quiet()) {
            return false;
        }
        PostState.record(run, state.raised());
        SectorEntityToken post = post();
        if (post != null) {
            livingPickets().forEach(picket -> Pickets.gather(picket, post));
        }
        message("chart.post.alarm");
        return true;
    }

    void mergeArrivals(SectorEntityToken post) {
        if (post == null) {
            return;
        }
        for (CampaignFleetAPI picket : livingPickets()) {
            if (Misc.getDistance(picket.getLocation(), post.getLocation()) <= HeistRules.GUARD_MERGE_RANGE + post.getRadius()) {
                joinGuard(picket, post);
            }
        }
    }

    public CampaignFleetAPI gatherDefenders() {
        SectorEntityToken post = post();
        if (post == null) {
            return guard();
        }
        for (CampaignFleetAPI picket : livingPickets()) {
            joinGuard(picket, post);
        }
        return guard();
    }

    private void joinGuard(CampaignFleetAPI picket, SectorEntityToken post) {
        CampaignFleetAPI guard = guard();
        if (guard == null) {
            Pickets.standGuard(picket, post);
            guardId = picket.getId();
            return;
        }
        Pickets.merge(picket, guard);
    }

    public CampaignFleetAPI guard() {
        SectorEntityToken entity = guardId == null ? null : system.getEntityById(guardId);
        return entity instanceof CampaignFleetAPI fleet && fleet.isAlive() && !fleet.isEmpty() ? fleet : null;
    }

    public List<CampaignFleetAPI> livingPickets() {
        List<CampaignFleetAPI> pickets = new ArrayList<>();
        for (PicketStation station : stations) {
            SectorEntityToken entity = station.fleetId() == null ? null : system.getEntityById(station.fleetId());
            if (entity instanceof CampaignFleetAPI fleet && fleet.isAlive() && !fleet.isEmpty() && Pickets.isPicket(fleet)) {
                pickets.add(fleet);
            }
        }
        return pickets;
    }

    public boolean defended() {
        return guard() != null || !livingPickets().isEmpty();
    }

    float purgeReduction(ChartRun run) {
        return ChartRunEffects.contextOf(run).anomalyPercent(ListeningPost.PURGE_KEY);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (fleet == null || battle == null || !battle.isPlayerInvolved() || !Pickets.isPicket(fleet) || fleet.isEmpty()) {
            return;
        }
        ChartRun run = run();
        if (run != null) {
            raiseAlarm(run);
        }
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
    }

    private void message(String key) {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (CampaignMessages.available() && playerFleet != null && playerFleet.getContainingLocation() == system) {
            CampaignMessages.plain(Translation.gameText(key), Misc.getNegativeHighlightColor());
        }
    }
}
