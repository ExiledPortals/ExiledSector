package exiledsector.charts.anomaly.stellarflares;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.AnomalyRunScript;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FlareTempo extends AnomalyRunScript {

    private final List<PlanetAPI> stars;
    private final float extraShare;
    private transient List<StarCoronaTerrainPlugin> coronas;

    public FlareTempo(String runId, int tier, StarSystemAPI system, List<PlanetAPI> stars, float moreOftenPercent) {
        super(runId, tier, system, false);
        this.stars = new ArrayList<>(stars);
        this.extraShare = Math.max(0f, moreOftenPercent) / 100f;
    }

    public float extraShare() {
        return extraShare;
    }

    @Override
    protected String anomalyId() {
        return StellarFlares.ANOMALY_ID;
    }

    @Override
    protected float checkDays() {
        return Float.MAX_VALUE;
    }

    @Override
    protected void check(SectorAPI sector, ChartRun run, float days) {
    }

    @Override
    protected void everyFrame(SectorAPI sector, float amount) {
        for (StarCoronaTerrainPlugin corona : coronas()) {
            FlareManager flares = corona.getFlareManager();
            if (flares != null) {
                flares.advance(amount * extraShare);
            }
        }
    }

    private List<StarCoronaTerrainPlugin> coronas() {
        if (coronas == null) {
            coronas = stars.stream().filter(Objects::nonNull).map(Misc::getCoronaFor).filter(Objects::nonNull).toList();
        }
        return coronas;
    }
}
