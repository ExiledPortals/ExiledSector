package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

public record AnomalyRun(ChartRun run, StarSystemAPI system) {

    public static AnomalyRun in(LocationAPI location) {
        ChartRun run = ChartRuns.liveIn(location);
        return run != null && location instanceof StarSystemAPI system ? new AnomalyRun(run, system) : null;
    }

    public static AnomalyRun live(SectorAPI sector) {
        ChartRun run = ChartRuns.live();
        StarSystemAPI system = run == null || sector == null ? null : sector.getStarSystem(run.systemId());
        return system == null ? null : new AnomalyRun(run, system);
    }

    public String runId() {
        return run.runId();
    }

    public int tier() {
        return run.tier();
    }

    public ChartGenContext context() {
        return ChartRunEffects.contextOf(run);
    }

    public boolean has(ChartAnomaly anomaly) {
        return run.anomalyIds().contains(anomaly.id());
    }

    public boolean holds(SectorEntityToken entity) {
        return entity != null && entity.getContainingLocation() == system && ChartIds.belongsToRun(entity, run.runId());
    }
}
