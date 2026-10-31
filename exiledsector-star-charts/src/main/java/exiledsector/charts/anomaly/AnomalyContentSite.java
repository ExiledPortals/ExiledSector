package exiledsector.charts.anomaly;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.ContentForces;
import exiledsector.charts.content.SettledObjective;
import exiledsector.charts.run.ChartRun;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.function.Predicate;

public final class AnomalyContentSite {

    static final int ORBIT_ATTEMPTS = 12;
    static final float ORBIT_SPEED = 15f;
    static final int SPOT_ATTEMPTS = 24;
    static final float MIN_OPEN_DISTANCE = 2000f;
    static final float MAX_OPEN_DISTANCE = 8000f;

    @FunctionalInterface
    public interface PoiDistance {
        float from(SectorEntityToken poi, Random random);
    }

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
    private final ContentForces forces;
    private final Object plan;
    private SettledObjective settledObjective;
    private final List<SectorEntityToken> placed = new ArrayList<>();
    private final List<EveryFrameScript> scripts = new ArrayList<>();
    private final List<String> claims = new ArrayList<>();
    private final Map<String, String> stagedStates = new LinkedHashMap<>();

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
        this(system, runId, tier, factionId, context, pois, guardedPois, entry, clear, randoms, claimedPlanetIds, ContentForces.NONE, null);
    }

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, List<SectorEntityToken> pois,
                              List<SectorEntityToken> guardedPois, Vector2f entry, Predicate<ChartSpot> clear, LongFunction<Random> randoms,
                              Set<String> claimedPlanetIds, ContentForces forces, Object plan) {
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
        this.forces = forces == null ? ContentForces.NONE : forces;
        this.plan = plan;
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

    public ContentForces forces() {
        return forces;
    }

    public <T> T plan(Class<T> type) {
        return type.isInstance(plan) ? type.cast(plan) : null;
    }

    public void settle(SettledObjective objective) {
        settledObjective = objective;
    }

    public SettledObjective settledObjective() {
        return settledObjective;
    }

    public Vector2f entry() {
        return entry == null ? null : new Vector2f(entry);
    }

    public SectorEntityToken centre() {
        return ChartSites.centreOf(system);
    }

    public List<PlanetAPI> worlds() {
        return system.getPlanets().stream().filter(planet -> !planet.isStar()).toList();
    }

    public List<PlanetAPI> freePlanets(boolean solidFirst) {
        List<PlanetAPI> free = worlds().stream().filter(planet -> !isClaimed(planet)).toList();
        List<PlanetAPI> solid = free.stream().filter(planet -> !planet.isGasGiant()).toList();
        return solidFirst && !solid.isEmpty() ? solid : free;
    }

    public boolean isClaimed(SectorEntityToken planet) {
        return planet != null && claimedPlanetIds.contains(planet.getId());
    }

    public void claim(SectorEntityToken planet) {
        if (planet != null && claimedPlanetIds.add(planet.getId())) {
            claims.add(planet.getId());
        }
    }

    public AnomalyState state(String anomalyId) {
        return AnomalyState.staged(anomalyId, stagedStates);
    }

    public boolean hasStagedStates() {
        return !stagedStates.isEmpty();
    }

    public void commitStates(ChartRun run) {
        if (stagedStates.isEmpty()) {
            return;
        }
        if (run == null || !run.runId().equals(runId)) {
            throw new IllegalStateException("no run to record the anomaly states in");
        }
        stagedStates.forEach(run::recordAnomalyState);
        stagedStates.clear();
    }

    public void rollBack() {
        placed.forEach(system::removeEntity);
        placed.clear();
        scripts.forEach(system::removeScript);
        scripts.clear();
        claims.forEach(claimedPlanetIds::remove);
        claims.clear();
        stagedStates.clear();
        settledObjective = null;
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

    public static ChartSites.Orbit orbit(SectorEntityToken focus, float angle, float radius) {
        return new ChartSites.Orbit(focus, angle, radius, radius / ORBIT_SPEED);
    }

    public ChartSites.Orbit clearOrbit(SectorEntityToken focus, float minRadius, float maxRadius, Random random) {
        return clearOrbit(focus, minRadius, maxRadius, position -> true, random);
    }

    public ChartSites.Orbit clearOrbit(SectorEntityToken focus, float minRadius, float maxRadius, Predicate<Vector2f> alsoClear, Random random) {
        for (int attempt = 0; attempt < ORBIT_ATTEMPTS; attempt++) {
            float radius = minRadius + random.nextFloat() * Math.max(0f, maxRadius - minRadius);
            ChartSites.Orbit orbit = orbit(focus, random.nextFloat() * 360f, radius);
            if (isClear(orbit) && alsoClear.test(orbit.position())) {
                return orbit;
            }
        }
        return null;
    }

    public ChartSites.Orbit clearOrbit(OrbitSearch search, Random random) {
        if (search.focus() == null) {
            return null;
        }
        ChartSites.Orbit farthest = null;
        for (int band = 0; band < search.bands(); band++) {
            float bandMin = search.bandMin(band);
            ChartSites.Orbit orbit = clearOrbit(search.focus(), bandMin, bandMin + search.bandWidth(), random);
            if (orbit == null) {
                continue;
            }
            float gap = distanceToEntry(orbit.position());
            if (gap >= search.minEntryGap()) {
                return orbit;
            }
            if (farthest == null || gap > distanceToEntry(farthest.position())) {
                farthest = orbit;
            }
        }
        return farthest;
    }

    public Vector2f findSpot(PoiDistance nearPoi, Predicate<Vector2f> fits, Random random) {
        for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
            Vector2f candidate = attempt < SPOT_ATTEMPTS / 2 && !pois.isEmpty()
                    ? nearPoi(pois.get(random.nextInt(pois.size())), nearPoi, random)
                    : offset(new Vector2f(), MIN_OPEN_DISTANCE + random.nextFloat() * (MAX_OPEN_DISTANCE - MIN_OPEN_DISTANCE), random);
            if (candidate != null && isClear(candidate) && fits.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Vector2f nearPoi(SectorEntityToken poi, PoiDistance distance, Random random) {
        if (poi.getLocation() == null) {
            return null;
        }
        return offset(poi.getLocation(), distance.from(poi, random), random);
    }

    private static Vector2f offset(Vector2f from, float distance, Random random) {
        double angle = random.nextDouble() * Math.PI * 2d;
        return ChartSites.pointAround(from, angle, distance);
    }

    public SectorEntityToken addSite(ChartPoi poi, ChartSites.Orbit orbit, Random random) {
        return track(ChartSites.addSite(system, runId, tier, factionId, context, poi, orbit, random));
    }

    public SectorEntityToken addEntity(String entityId, String name, String entityType, String entityFactionId, ChartSites.Orbit orbit) {
        if (orbit == null) {
            return null;
        }
        SectorEntityToken entity = track(system.addCustomEntity(entityId, name, entityType, entityFactionId));
        entity.setCircularOrbitPointingDown(orbit.focus(), orbit.angle(), orbit.radius(), orbit.days());
        ChartContentGenerator.tagContent(entity, runId);
        return entity;
    }

    public SectorEntityToken addTerrain(String terrainId, Object params, Vector2f at, String... tags) {
        SectorEntityToken terrain = track(system.addTerrain(terrainId, params));
        terrain.setFixedLocation(at.x, at.y);
        for (String tag : tags) {
            terrain.addTag(tag);
        }
        return terrain;
    }

    public CampaignFleetAPI addFleet(CampaignFleetAPI fleet, Vector2f at) {
        system.addEntity(fleet);
        track(fleet);
        fleet.setLocation(at.x, at.y);
        return fleet;
    }

    public <T extends EveryFrameScript> T addScript(T script) {
        system.addScript(script);
        scripts.add(script);
        return script;
    }

    public <T extends SectorEntityToken> T track(T entity) {
        if (entity != null) {
            placed.add(entity);
        }
        return entity;
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
