package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.Predicate;

public final class AnomalyContentSite {

    static final int ORBIT_ATTEMPTS = 12;
    static final float ORBIT_SPEED = 15f;

    private final StarSystemAPI system;
    private final String runId;
    private final int tier;
    private final String factionId;
    private final ChartGenContext context;
    private final List<SectorEntityToken> pois;
    private final List<SectorEntityToken> guardedPois;
    private final Vector2f entry;
    private final Predicate<ChartSpot> clear;
    private final LongFunction<Random> randoms;
    private final Set<String> claimedPlanetIds;
    private final List<SectorEntityToken> placed = new ArrayList<>();
    private final List<String> claims = new ArrayList<>();
    private ChartRun recordingRun;

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, List<SectorEntityToken> pois,
                              Vector2f entry, Predicate<ChartSpot> clear, LongFunction<Random> randoms) {
        this(system, runId, tier, factionId, context, pois, List.of(), entry, clear, randoms);
    }

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, List<SectorEntityToken> pois,
                              List<SectorEntityToken> guardedPois, Vector2f entry, Predicate<ChartSpot> clear, LongFunction<Random> randoms) {
        this(system, runId, tier, factionId, context, pois, guardedPois, entry, clear, randoms, new HashSet<>());
    }

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, List<SectorEntityToken> pois,
                              List<SectorEntityToken> guardedPois, Vector2f entry, Predicate<ChartSpot> clear, LongFunction<Random> randoms,
                              Set<String> claimedPlanetIds) {
        this.system = system;
        this.runId = runId;
        this.tier = tier;
        this.factionId = factionId;
        this.context = context == null ? ChartGenContext.none() : context;
        this.pois = pois == null ? List.of() : pois.stream().filter(Objects::nonNull).toList();
        this.guardedPois = guardedPois == null ? List.of() : guardedPois.stream().filter(Objects::nonNull).toList();
        this.entry = entry == null ? null : new Vector2f(entry);
        this.clear = clear;
        this.randoms = randoms;
        this.claimedPlanetIds = claimedPlanetIds;
    }

    public StarSystemAPI system() {
        return system;
    }

    public String runId() {
        return runId;
    }

    public int tier() {
        return tier;
    }

    public String factionId() {
        return factionId;
    }

    public ChartGenContext context() {
        return context;
    }

    public ChartRun run() {
        if (recordingRun == null) {
            ChartRun run = ChartRuns.currentOrNull();
            if (run == null || !run.runId().equals(runId)) {
                throw new IllegalStateException("no run to record the anomaly in");
            }
            recordingRun = run;
        }
        return recordingRun;
    }

    public void forgetRecordedStates(String anomalyId) {
        if (recordingRun != null) {
            recordingRun.forgetAnomalyStates(anomalyId);
        }
    }

    public boolean isClaimed(SectorEntityToken planet) {
        return planet != null && claimedPlanetIds.contains(planet.getId());
    }

    public void claim(SectorEntityToken planet) {
        if (planet != null && claimedPlanetIds.add(planet.getId())) {
            claims.add(planet.getId());
        }
    }

    public void releaseClaims() {
        claims.forEach(claimedPlanetIds::remove);
        claims.clear();
    }

    public List<SectorEntityToken> pois() {
        return pois;
    }

    public List<SectorEntityToken> guardedPois() {
        return guardedPois;
    }

    public boolean isClear(Vector2f point) {
        return point != null && (clear == null || clear.test(new ChartSpot(null, point, null, 1f)));
    }

    public boolean isClear(ChartSites.Orbit orbit) {
        return orbit != null && (clear == null || clear.test(new ChartSpot(null, orbit.position(), orbit.focus(), 1f)));
    }

    public ChartSites.Orbit clearOrbit(SectorEntityToken focus, float minRadius, float maxRadius, Random random) {
        for (int attempt = 0; attempt < ORBIT_ATTEMPTS; attempt++) {
            float radius = minRadius + random.nextFloat() * Math.max(0f, maxRadius - minRadius);
            ChartSites.Orbit orbit = new ChartSites.Orbit(focus, random.nextFloat() * 360f, radius, radius / ORBIT_SPEED);
            if (isClear(orbit)) {
                return orbit;
            }
        }
        return null;
    }

    public SectorEntityToken addSite(ChartPoi poi, ChartSites.Orbit orbit, Random random) {
        SectorEntityToken site = ChartSites.addSite(system, runId, tier, factionId, context, poi, orbit, random);
        if (site != null) {
            placed.add(site);
        }
        return site;
    }

    public SectorEntityToken addEntity(String entityId, String name, String entityType, String entityFactionId, ChartSites.Orbit orbit) {
        if (orbit == null) {
            return null;
        }
        SectorEntityToken entity = system.addCustomEntity(entityId, name, entityType, entityFactionId);
        try {
            entity.setCircularOrbitPointingDown(orbit.focus(), orbit.angle(), orbit.radius(), orbit.days());
            ChartContentGenerator.tagContent(entity, runId);
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(entity);
            throw e;
        }
        placed.add(entity);
        return entity;
    }

    public CampaignFleetAPI addFleet(CampaignFleetAPI fleet, Vector2f at) {
        system.addEntity(fleet);
        placed.add(fleet);
        fleet.setLocation(at.x, at.y);
        return fleet;
    }

    public void track(SectorEntityToken entity) {
        if (entity != null) {
            placed.add(entity);
        }
    }

    public List<SectorEntityToken> placed() {
        return List.copyOf(placed);
    }

    public float distanceToEntry(Vector2f point) {
        return entry == null || point == null ? Float.MAX_VALUE : Vector2f.sub(point, entry, new Vector2f()).length();
    }

    public Random random(long salt) {
        return randoms.apply(salt);
    }
}
