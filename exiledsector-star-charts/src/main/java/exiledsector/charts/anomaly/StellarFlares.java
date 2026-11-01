package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.layout.ChartLayoutGenerator;

public final class StellarFlares implements LayoutShaper, AtlasRule {

    public static final String ANOMALY_ID = "stellar_flares";
    public static final StellarFlares INSTANCE = new StellarFlares();
    static final float FLARE_CHANCE = 1f;

    private StellarFlares() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return ChartLayoutGenerator.hasStar(AtlasOpening.systemTypeAt(generator, state, cell));
    }

    @Override
    public void shape(AnomalyLayoutSite site) {
        if (site.stars().isEmpty()) {
            throw new IllegalStateException("no star for the stellar flares");
        }
        for (PlanetAPI star : site.stars()) {
            setFlareChance(star, FLARE_CHANCE);
        }
    }

    public static void setFlareChance(PlanetAPI star, float chance) {
        StarCoronaTerrainPlugin corona = star == null ? null : Misc.getCoronaFor(star);
        if (corona != null && corona.getParams() != null) {
            corona.getParams().flareProbability = chance;
        }
    }
}
