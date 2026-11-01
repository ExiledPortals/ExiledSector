package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin.AsteroidFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.content.ChartSites;

import java.awt.Color;
import java.util.Random;

public final class AsteroidStorm implements LayoutShaper, ContentPlacer {

    public static final String ANOMALY_ID = "asteroid_storm";
    public static final AsteroidStorm INSTANCE = new AsteroidStorm();
    static final float BELT_WIDTH = 500f;
    static final int BELTS = 2;
    static final int FIELDS = 3;
    static final float FIELD_RADIUS = 450f;
    public static final String BELT_TAG = ChartIds.PREFIX + "asteroid_storm_belt";
    static final long CACHE_SALT = 0x41535443414348L;
    static final int MIN_CACHES_PER_BELT = 2;
    static final int EXTRA_CACHES_PER_BELT = 2;
    static final float CACHE_BAND_SHARE = 0.5f;

    private AsteroidStorm() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void shape(AnomalyLayoutSite site) {
        float reach = Math.max(4000f, site.outerRadius());
        for (int belt = 0; belt < BELTS; belt++) {
            float orbit = site.between(2500f, reach);
            float days = orbit / 15f;
            int asteroids = 100 + site.random().nextInt(80);
            SectorEntityToken beltTerrain = site.system().addAsteroidBelt(site.centre(), asteroids, orbit, BELT_WIDTH, days * 0.75f, days * 1.5f,
                    Terrain.ASTEROID_BELT, null);
            if (beltTerrain != null) {
                beltTerrain.addTag(BELT_TAG);
            }
            site.system().addRingBand(site.centre(), "misc", "rings_asteroids0", 256f, site.random().nextInt(4), Color.white, 256f, orbit,
                    days, null, null);
        }
        for (int field = 0; field < FIELDS; field++) {
            float orbit = site.between(3000f, reach + 1000f);
            int count = 60 + site.random().nextInt(40);
            SectorEntityToken asteroids = site.system().addTerrain(Terrain.ASTEROID_FIELD,
                    new AsteroidFieldParams(FIELD_RADIUS, FIELD_RADIUS + 150f, count, count, 4f, 16f, null));
            asteroids.setCircularOrbit(site.centre(), site.angle(), orbit, orbit / site.between(20f, 25f));
        }
    }

    @Override
    public void generate(AnomalyContentSite site) {
        Random random = site.random(CACHE_SALT);
        for (CampaignTerrainAPI belt : site.system().getTerrainCopy()) {
            if (!belt.hasTag(BELT_TAG) || !(belt.getPlugin() instanceof BaseRingTerrain ring) || ring.params == null) {
                continue;
            }
            SectorEntityToken focus = belt.getOrbitFocus() != null ? belt.getOrbitFocus() : site.system().getCenter();
            float halfBand = ring.params.bandWidthInEngine * CACHE_BAND_SHARE * 0.5f;
            int count = MIN_CACHES_PER_BELT + random.nextInt(EXTRA_CACHES_PER_BELT);
            for (int i = 0; i < count; i++) {
                ChartPoi cache = ChartSites.pick(ChartPoi.Kind.CACHE, site.tier(), random);
                ChartSites.Orbit orbit = site.clearOrbit(focus, ring.params.middleRadius - halfBand, ring.params.middleRadius + halfBand, random);
                if (cache != null && orbit != null) {
                    site.addSite(cache, orbit, random);
                }
            }
        }
    }
}
