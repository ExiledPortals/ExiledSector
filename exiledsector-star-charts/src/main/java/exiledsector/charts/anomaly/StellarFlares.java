package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

public final class StellarFlares implements ChartAnomaly {

    public static final String ID = "stellar_flares";
    public static final StellarFlares INSTANCE = new StellarFlares();
    static final float FLARE_CHANCE = 1f;

    private StellarFlares() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void shape(AnomalyLayoutSite site) {
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
