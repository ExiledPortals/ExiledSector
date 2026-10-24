package exiledsector.charts.feature;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

public final class DebrisRing implements ChartFeature {

    public static final String ID = "debris_ring";
    public static final DebrisRing INSTANCE = new DebrisRing();
    static final float GAP = 1400f;
    static final float BELT_WIDTH = 600f;
    static final float FIELD_RADIUS = 500f;
    static final float FOREVER_DAYS = 10000000f;

    private DebrisRing() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public SectorEntityToken build(FeatureSite site) {
        float orbit = site.outerRadius() + GAP + BELT_WIDTH / 2f + site.between(0f, 500f);
        float days = FeatureSite.orbitDays(orbit);
        int asteroids = 80 + site.random().nextInt(60);
        site.system().addAsteroidBelt(site.centre(), asteroids, orbit, BELT_WIDTH, days * 0.75f, days * 1.5f, Terrain.ASTEROID_BELT, null);
        site.system().addRingBand(site.centre(), "misc", "rings_asteroids0", 256f, site.random().nextInt(2), Color.white, 256f,
                orbit - BELT_WIDTH * 0.25f, days * 1.05f, null, null);
        site.system().addRingBand(site.centre(), "misc", "rings_asteroids0", 256f, site.random().nextInt(4), Color.white, 256f,
                orbit + BELT_WIDTH * 0.25f, days, null, null);
        DebrisFieldParams params = new DebrisFieldParams(FIELD_RADIUS, 1f, FOREVER_DAYS, 0f);
        params.source = DebrisFieldSource.MIXED;
        SectorEntityToken field = Misc.addDebrisField(site.system(), params, site.random());
        field.setCircularOrbit(site.centre(), site.angle(), orbit, days);
        return field;
    }
}
