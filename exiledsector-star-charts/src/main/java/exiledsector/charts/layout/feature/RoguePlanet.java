package exiledsector.charts.layout.feature;

import com.fs.starfarer.api.campaign.SectorEntityToken;

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
    public SectorEntityToken build(FeatureSite site) {
        float orbit = site.orbitBeyond(GAP, 2000f);
        return site.addPlanet("rogue", site.centre(), site.pick(ROGUE_TYPES), site.between(90f, 140f), orbit, orbit * 2f);
    }
}
