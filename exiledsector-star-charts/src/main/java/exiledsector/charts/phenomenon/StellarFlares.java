package exiledsector.charts.phenomenon;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

public final class StellarFlares implements ChartPhenomenon {

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
    public Kind kind() {
        return Kind.OVERLAY;
    }

    @Override
    public void shape(PhenomenonSite site) {
        for (PlanetAPI star : site.stars()) {
            StarCoronaTerrainPlugin corona = Misc.getCoronaFor(star);
            if (corona != null && corona.getParams() != null) {
                corona.getParams().flareProbability = FLARE_CHANCE;
            }
        }
    }
}
