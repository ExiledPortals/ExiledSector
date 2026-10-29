package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.Random;

public final class ListeningPost implements ChartAnomaly {

    public static final String ID = "listening_post";
    public static final String PICKETS_KEY = ID + ".pickets";
    public static final String LOOT_KEY = ID + ".loot";
    public static final String PURGE_KEY = ID + ".purge";
    public static final String ENTITY_TYPE = ChartIds.PREFIX + "listening_post";
    public static final String POST_TAG = ChartIds.PREFIX + "listening_post";
    public static final ListeningPost INSTANCE = new ListeningPost();
    static final long PLACEMENT_SALT = 0x4c495354454e50L;
    static final long PICKET_SALT = 0x5049434b455453L;

    private ListeningPost() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        return traits.specialId() == null && traits.infestationId() == null;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runId().equals(site.runId())) {
            throw new IllegalStateException("no run to record the listening post in");
        }
        place(site, run);
    }

    static PostScript place(AnomalyContentSite site, ChartRun run) {
        StarSystemAPI system = site.system();
        Random random = site.random(PLACEMENT_SALT);
        ChartSites.Orbit orbit = postOrbit(site, random);
        if (orbit == null) {
            throw new IllegalStateException("no clear orbit for the listening post");
        }
        SectorEntityToken post = site.addEntity(postId(site.runId()), Translation.gameText("chart.post.name"), ENTITY_TYPE, site.factionId(), orbit);
        post.addTag(POST_TAG);
        PostScript script = new PostScript(site.runId(), site.tier(), system, post.getId());
        Random picketRandom = site.random(PICKET_SALT);
        int count = HeistRules.picketCount(picketRandom, Math.round(site.context().anomalyPercent(PICKETS_KEY)));
        float firstAngle = picketRandom.nextFloat() * 360f;
        for (int index = 0; index < count; index++) {
            float angle = firstAngle + index * 360f / count;
            float radius = HeistRules.RING_RADIUS + (picketRandom.nextFloat() * 2f - 1f) * HeistRules.RING_JITTER;
            PicketStation station = new PicketStation(index, angle, radius);
            if (Pickets.spawn(site, station, orbit.position(), script, picketRandom) != null) {
                script.add(station);
            }
        }
        if (script.stations().isEmpty()) {
            throw new IllegalStateException("no picket could be formed");
        }
        PostState.record(run, PostState.fresh(random.nextLong()));
        system.addScript(script);
        return script;
    }

    static ChartSites.Orbit postOrbit(AnomalyContentSite site, Random random) {
        SectorEntityToken centre = site.system().getCenter() != null ? site.system().getCenter() : site.system().getStar();
        if (centre == null) {
            return null;
        }
        ChartSites.Orbit farthest = null;
        for (int attempt = 0; attempt < HeistRules.POST_ATTEMPTS; attempt++) {
            ChartSites.Orbit orbit = site.clearOrbit(centre, HeistRules.POST_MIN_RADIUS, HeistRules.POST_MAX_RADIUS, random);
            if (orbit == null) {
                continue;
            }
            float gap = site.distanceToEntry(orbit.position());
            if (gap >= HeistRules.POST_MIN_ENTRY_GAP) {
                return orbit;
            }
            if (farthest == null || gap > site.distanceToEntry(farthest.position())) {
                farthest = orbit;
            }
        }
        return farthest;
    }

    public static String postId(String runId) {
        return ChartIds.PREFIX + "listening_post_" + runId;
    }

    public static boolean isPost(SectorEntityToken entity) {
        return entity != null && entity.hasTag(POST_TAG);
    }

    static boolean inRunSystem(ChartRun run, LocationAPI location) {
        return run != null && run.runState().isLive() && location != null && run.systemId().equals(location.getId());
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        ChartRun run = ChartRuns.currentOrNull();
        PostState state = inRunSystem(run, location) ? PostState.of(run) : null;
        return state != null && state.standing();
    }

    @Override
    public InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return PostDialog.wantsToHandle(target) ? new PostDialog() : null;
    }

    @Override
    public String intelText(ChartRun run, LocationAPI location) {
        PostState state = PostState.of(run);
        PostScript script = PostScript.in(location);
        return state == null ? null : PostText.status(state, script);
    }
}
