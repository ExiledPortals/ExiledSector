package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.content.ChartSpot;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
    private final Vector2f entry;
    private final Predicate<ChartSpot> clear;
    private final LongFunction<Random> randoms;
    private final List<SectorEntityToken> placed = new ArrayList<>();

    public AnomalyContentSite(StarSystemAPI system, String runId, int tier, String factionId, ChartGenContext context, List<SectorEntityToken> pois,
                              Vector2f entry, Predicate<ChartSpot> clear, LongFunction<Random> randoms) {
        this.system = system;
        this.runId = runId;
        this.tier = tier;
        this.factionId = factionId;
        this.context = context == null ? ChartGenContext.none() : context;
        this.pois = pois == null ? List.of() : pois.stream().filter(Objects::nonNull).toList();
        this.entry = entry == null ? null : new Vector2f(entry);
        this.clear = clear;
        this.randoms = randoms;
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

    public List<SectorEntityToken> pois() {
        return pois;
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

    public CampaignFleetAPI addFleet(CampaignFleetAPI fleet, Vector2f at) {
        system.addEntity(fleet);
        placed.add(fleet);
        fleet.setLocation(at.x, at.y);
        return fleet;
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
