package exiledsector.charts.layout.feature;

import exiledsector.charts.layout.feature.FeaturePlan.World;

import java.util.List;

public final class RoguePlanet implements ChartFeature {

    public static final String FEATURE_ID = "rogue_planet";
    public static final RoguePlanet INSTANCE = new RoguePlanet();
    static final float GAP = 4000f;
    static final List<String> ROGUE_TYPES = List.of("frozen", "frozen2", "barren", "rocky_ice", "cryovolcanic");

    private RoguePlanet() {
    }

    @Override
    public String id() {
        return FEATURE_ID;
    }

    @Override
    public FeaturePlan plan(FeatureRolls rolls) {
        float orbit = rolls.orbitBeyond(GAP, 2000f);
        World rogue = new World("rogue", FeaturePlan.CENTRE, rolls.pick(ROGUE_TYPES), rolls.between(90f, 140f), orbit, orbit * 2f, rolls.angle());
        return new FeaturePlan(List.of(rogue), List.of(), List.of());
    }
}
