package exiledsector.charts.layout.feature;

import exiledsector.charts.layout.feature.FeaturePlan.World;

import java.util.List;

public final class TwinWorlds implements ChartFeature {

    public static final String FEATURE_ID = "twin_worlds";
    public static final TwinWorlds INSTANCE = new TwinWorlds();
    static final float GAP = 1600f;
    static final List<String> WORLD_TYPES = List.of("arid", "desert", "barren", "toxic", "frozen", "rocky_metallic", "tundra", "barren_venuslike");

    private TwinWorlds() {
    }

    @Override
    public String id() {
        return FEATURE_ID;
    }

    @Override
    public FeaturePlan plan(FeatureRolls rolls) {
        String type = rolls.pick(WORLD_TYPES);
        float orbit = rolls.orbitBeyond(GAP, 600f);
        float firstRadius = rolls.between(110f, 150f);
        World first = new World("twin_a", FeaturePlan.CENTRE, type, firstRadius, orbit, FeatureRolls.orbitDays(orbit), rolls.angle());
        float secondRadius = firstRadius * rolls.between(0.85f, 1f);
        World second = new World("twin_b", 0, type, secondRadius, firstRadius + secondRadius + rolls.between(300f, 400f), rolls.between(20f, 30f),
                rolls.angle());
        return new FeaturePlan(List.of(first, second), List.of(), List.of());
    }
}
