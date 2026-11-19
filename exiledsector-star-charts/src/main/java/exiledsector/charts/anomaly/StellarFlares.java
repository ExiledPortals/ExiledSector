package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.stellarflares.FlareTempo;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemData;
import exiledsector.items.modifier.LuckyRolls;

import java.util.List;

public final class StellarFlares implements LayoutShaper, ContentPlacer, AtlasRule, LootShaper {

    public static final String ANOMALY_ID = "stellar_flares";
    public static final StellarFlares INSTANCE = new StellarFlares();
    public static final String FLARE_FREQUENCY_KEY = ChartModifier.CHART_STELLAR_FLARES_FLARE_FREQUENCY_MULT.anomalyTuningKey();
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

    @Override
    public void generate(AnomalyContentSite site) {
        float moreOftenPercent = site.context().anomalyPercent(FLARE_FREQUENCY_KEY);
        if (moreOftenPercent <= 0f) {
            return;
        }
        List<PlanetAPI> stars = site.system().getPlanets().stream().filter(PlanetAPI::isStar).toList();
        if (!stars.isEmpty()) {
            site.addScript(new FlareTempo(site.runId(), site.tier(), site.system(), stars, moreOftenPercent));
        }
    }

    @Override
    public ItemData shapeDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        return LuckyRolls.withLuck(item, FlareLuck.dropLuck(ChartRuns.liveIn(location)));
    }

    public static void setFlareChance(PlanetAPI star, float chance) {
        StarCoronaTerrainPlugin corona = star == null ? null : Misc.getCoronaFor(star);
        if (corona != null && corona.getParams() != null) {
            corona.getParams().flareProbability = chance;
        }
    }
}
