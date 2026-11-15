package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldSource;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.layout.feature.FeaturePlan.AsteroidBelt;
import exiledsector.charts.layout.feature.FeaturePlan.RingBand;

import java.util.List;

public final class DebrisRing implements ChartFeature {

    public static final String FEATURE_ID = "debris_ring";
    public static final DebrisRing INSTANCE = new DebrisRing();
    static final float GAP = 1400f;
    static final float BELT_WIDTH = 600f;
    static final float BAND_WIDTH = 256f;
    static final String BAND_TEXTURE = "rings_asteroids0";
    static final float FIELD_RADIUS = 500f;
    static final float FOREVER_DAYS = 10000000f;

    private DebrisRing() {
    }

    @Override
    public String id() {
        return FEATURE_ID;
    }

    @Override
    public FeaturePlan plan(FeatureRolls rolls) {
        float orbit = rolls.orbitBeyond(GAP + BELT_WIDTH / 2f, 500f);
        float days = FeatureRolls.orbitDays(orbit);
        AsteroidBelt belt = new AsteroidBelt(80 + rolls.below(60), orbit, BELT_WIDTH, days);
        RingBand inner = new RingBand(FeaturePlan.CENTRE, BAND_TEXTURE, rolls.below(2), BAND_WIDTH, orbit - BELT_WIDTH * 0.25f, days * 1.05f, false);
        RingBand outer = new RingBand(FeaturePlan.CENTRE, BAND_TEXTURE, rolls.below(4), BAND_WIDTH, orbit + BELT_WIDTH * 0.25f, days, false);
        return new FeaturePlan(List.of(), List.of(inner, outer), List.of(belt));
    }

    @Override
    public SectorEntityToken build(FeatureSite site) {
        FeaturePlan plan = plan(site.rolls());
        site.place(plan);
        AsteroidBelt belt = plan.asteroidBelts().get(0);
        DebrisFieldParams params = new DebrisFieldParams(FIELD_RADIUS, 1f, FOREVER_DAYS, 0f);
        params.source = DebrisFieldSource.MIXED;
        SectorEntityToken field = Misc.addDebrisField(site.system(), params, site.random());
        field.setCircularOrbit(site.centre(), site.rolls().angle(), belt.orbitRadius(), belt.orbitDays());
        return field;
    }
}
