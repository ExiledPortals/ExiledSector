package exiledsector.charts.feature;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;

import java.awt.Color;
import java.util.List;

public final class RingedGiant implements ChartFeature {

    public static final String FEATURE_ID = "ringed_giant";
    public static final RingedGiant INSTANCE = new RingedGiant();
    static final float GAP = 600f;
    static final float BAND_WIDTH = 256f;
    static final List<String> GIANT_TYPES = List.of("gas_giant", "gas_giant", "ice_giant");
    static final List<String> RING_TEXTURES = List.of("rings_ice0", "rings_dust0");
    static final List<String> MOON_TYPES = List.of("barren", "frozen", "rocky_ice", "lava_minor", "cryovolcanic", "barren-bombarded");

    private RingedGiant() {
    }

    @Override
    public String id() {
        return FEATURE_ID;
    }

    @Override
    public SectorEntityToken build(FeatureSite site) {
        String giantType = site.pick(GIANT_TYPES);
        float giantRadius = site.between(320f, 380f);
        String texture = site.pick(RING_TEXTURES);
        float ringStart = giantRadius + 300f;
        int moonCount = 3 + site.random().nextInt(2);
        float[] moonRadii = new float[moonCount];
        float[] moonOrbits = new float[moonCount];
        float moonOrbit = ringStart + BAND_WIDTH * 2f + 400f;
        for (int moon = 0; moon < moonCount; moon++) {
            moonRadii[moon] = site.between(50f, 90f);
            moonOrbit += moonRadii[moon] + site.between(250f, 400f);
            moonOrbits[moon] = moonOrbit;
        }
        float reach = moonOrbit + moonRadii[moonCount - 1];
        float orbit = site.outerRadius() + reach + GAP + site.between(0f, 600f);
        PlanetAPI giant = site.addPlanet("giant", site.centre(), giantType, giantRadius, orbit, FeatureSite.orbitDays(orbit));
        for (int band = 0; band < 3; band++) {
            float middle = ringStart + band * BAND_WIDTH * 0.6f;
            site.system().addRingBand(giant, "misc", texture, BAND_WIDTH, site.random().nextInt(4), Color.white, BAND_WIDTH, middle,
                    middle / site.between(15f, 20f), Terrain.RING, null);
        }
        for (int moon = 0; moon < moonCount; moon++) {
            site.addPlanet("moon_" + moon, giant, site.pick(MOON_TYPES), moonRadii[moon], moonOrbits[moon], moonOrbits[moon] / site.between(25f, 40f));
        }
        return giant;
    }
}
