package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartPoi;
import exiledsector.charts.ChartPois;
import exiledsector.charts.ChartSites;

import java.util.Random;
import java.util.function.Consumer;

public final class StarAnomalies {

    static final int NEUTRON_STAR_MIN_PLANETS = 7;
    public static final ChartAnomaly BLACK_HOLE = new Forced("black_hole", "black_hole", StarSystemType.SINGLE, 0, StarAnomalies::scatterDerelicts);
    public static final ChartAnomaly NEUTRON_STAR = new Forced("neutron_star", "star_neutron", StarSystemType.SINGLE, NEUTRON_STAR_MIN_PLANETS,
            StarAnomalies::placeResearchStation);
    public static final ChartAnomaly DENSE_NEBULA = new Forced("dense_nebula", null, StarSystemType.NEBULA, 0, site -> {
    });
    public static final String RESEARCH_STATION = "research_station";
    static final long DERELICT_SALT = 0x424c4b44524c43L;
    static final long STATION_SALT = 0x4e5452535441L;
    static final int MIN_DERELICTS = 6;
    static final int EXTRA_DERELICTS = 4;
    static final float DERELICT_GAP = 1000f;
    static final float DERELICT_REACH = 7000f;
    static final float STATION_GAP = 400f;
    static final float STATION_STEP = 250f;
    static final float STATION_REACH = 6000f;

    private StarAnomalies() {
    }

    private record Forced(String id, String primaryStar, StarSystemType systemType, int minPlanets,
                          Consumer<AnomalyContentSite> content) implements ChartAnomaly {

        @Override
        public void generate(AnomalyContentSite site) {
            content.accept(site);
        }
    }

    static void scatterDerelicts(AnomalyContentSite site) {
        PlanetAPI star = site.system().getStar();
        if (star == null) {
            return;
        }
        Random random = site.random(DERELICT_SALT);
        int count = MIN_DERELICTS + random.nextInt(EXTRA_DERELICTS);
        for (int i = 0; i < count; i++) {
            ChartPoi derelict = ChartSites.pick(ChartPoi.Kind.DERELICT, site.tier(), random);
            ChartSites.Orbit orbit = site.clearOrbit(star, star.getRadius() + DERELICT_GAP, DERELICT_REACH, random);
            if (derelict != null && orbit != null) {
                site.addSite(derelict, orbit, random);
            }
        }
    }

    static void placeResearchStation(AnomalyContentSite site) {
        PlanetAPI star = site.system().getStar();
        ChartPoi station = ChartPois.byId(RESEARCH_STATION);
        if (star == null || station == null) {
            return;
        }
        Random random = site.random(STATION_SALT);
        for (float radius = star.getRadius() + STATION_GAP; radius <= STATION_REACH; radius += STATION_STEP) {
            ChartSites.Orbit orbit = site.clearOrbit(star, radius, radius, random);
            if (orbit != null) {
                site.addSite(station, orbit, random);
                return;
            }
        }
    }
}
