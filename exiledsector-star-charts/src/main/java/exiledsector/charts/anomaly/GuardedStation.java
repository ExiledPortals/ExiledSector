package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.i18n.Translation;

import java.util.List;

public record GuardedStation(String entityType, String tag, String idPrefix, String nameKey, long guardSalt) {

    public SectorEntityToken place(AnomalyContentSite site, ChartSites.Orbit orbit) {
        String name = nameKey == null ? null : Translation.gameText(nameKey);
        SectorEntityToken station = site.addEntity(id(site.runId()), name, entityType, Factions.NEUTRAL, orbit);
        station.addTag(tag);
        VanillaDefenders.arm(station, site.random(guardSalt).nextLong());
        return station;
    }

    public String id(String runId) {
        return idPrefix + runId;
    }

    public boolean holds(SectorEntityToken entity) {
        return entity != null && entity.hasTag(tag);
    }

    public SectorEntityToken in(LocationAPI location) {
        List<SectorEntityToken> found = location == null ? List.of() : location.getEntitiesWithTag(tag);
        return found.isEmpty() ? null : found.get(0);
    }

    public ChartRun liveRun(SectorEntityToken entity, String anomalyId) {
        ChartRun run = holds(entity) ? ChartRuns.liveOwning(entity) : null;
        return run != null && run.anomalyIds().contains(anomalyId) ? run : null;
    }
}
