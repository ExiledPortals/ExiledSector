package exiledsector.charts.feature;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

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
    public SectorEntityToken build(FeatureSite site) {
        String type = site.pick(WORLD_TYPES);
        float orbit = site.outerRadius() + GAP + site.between(0f, 600f);
        float firstRadius = site.between(110f, 150f);
        PlanetAPI first = site.addPlanet("twin_a", site.centre(), type, firstRadius, orbit, FeatureSite.orbitDays(orbit));
        float secondRadius = firstRadius * site.between(0.85f, 1f);
        site.addPlanet("twin_b", first, type, secondRadius, firstRadius + secondRadius + site.between(300f, 400f), site.between(20f, 30f));
        return first;
    }
}
