package exiledsector.charts.phenomenon;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin.AsteroidFieldParams;

import java.awt.Color;

public final class AsteroidStorm implements ChartPhenomenon {

    public static final String ID = "asteroid_storm";
    public static final AsteroidStorm INSTANCE = new AsteroidStorm();
    static final float BELT_WIDTH = 500f;
    static final int BELTS = 2;
    static final int FIELDS = 3;
    static final float FIELD_RADIUS = 450f;

    private AsteroidStorm() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Kind kind() {
        return Kind.OVERLAY;
    }

    @Override
    public void shape(PhenomenonSite site) {
        float reach = Math.max(4000f, site.outerRadius());
        for (int belt = 0; belt < BELTS; belt++) {
            float orbit = site.between(2500f, reach);
            float days = orbit / 15f;
            int asteroids = 100 + site.random().nextInt(80);
            site.system().addAsteroidBelt(site.centre(), asteroids, orbit, BELT_WIDTH, days * 0.75f, days * 1.5f, Terrain.ASTEROID_BELT, null);
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
}
