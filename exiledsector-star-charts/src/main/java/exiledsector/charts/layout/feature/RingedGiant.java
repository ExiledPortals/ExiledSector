package exiledsector.charts.layout.feature;

import exiledsector.charts.layout.feature.FeaturePlan.RingBand;
import exiledsector.charts.layout.feature.FeaturePlan.World;

import java.util.ArrayList;
import java.util.List;

public final class RingedGiant implements ChartFeature {

    public static final String FEATURE_ID = "ringed_giant";
    public static final RingedGiant INSTANCE = new RingedGiant();
    static final float GAP = 600f;
    static final float BAND_WIDTH = 256f;
    static final int BANDS = 3;
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
    public FeaturePlan plan(FeatureRolls rolls) {
        String giantType = rolls.pick(GIANT_TYPES);
        float giantRadius = rolls.between(320f, 380f);
        String texture = rolls.pick(RING_TEXTURES);
        float ringStart = giantRadius + 300f;
        int moonCount = 3 + rolls.below(2);
        float[] moonRadii = new float[moonCount];
        float[] moonOrbits = new float[moonCount];
        float moonOrbit = ringStart + BAND_WIDTH * 2f + 400f;
        for (int moon = 0; moon < moonCount; moon++) {
            moonRadii[moon] = rolls.between(50f, 90f);
            moonOrbit += moonRadii[moon] + rolls.between(250f, 400f);
            moonOrbits[moon] = moonOrbit;
        }
        float reach = moonOrbit + moonRadii[moonCount - 1];
        float orbit = rolls.orbitBeyond(reach + GAP, 600f);
        List<World> worlds = new ArrayList<>();
        worlds.add(new World("giant", FeaturePlan.CENTRE, giantType, giantRadius, orbit, FeatureRolls.orbitDays(orbit), rolls.angle()));
        List<RingBand> bands = new ArrayList<>();
        for (int band = 0; band < BANDS; band++) {
            float middle = ringStart + band * BAND_WIDTH * 0.6f;
            bands.add(new RingBand(0, texture, rolls.below(4), BAND_WIDTH, middle, middle / rolls.between(15f, 20f), true));
        }
        for (int moon = 0; moon < moonCount; moon++) {
            worlds.add(new World("moon_" + moon, 0, rolls.pick(MOON_TYPES), moonRadii[moon], moonOrbits[moon],
                    moonOrbits[moon] / rolls.between(25f, 40f), rolls.angle()));
        }
        return new FeaturePlan(worlds, bands, List.of());
    }
}
