package exiledsector.charts.run.diagnostics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.cleanup.ChartTopographyScans;
import exiledsector.charts.run.cleanup.ChartUsedNames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ChartCensus(int starSystems, int chartSystems, int hyperspaceEntities, int hyperspaceFleets, int hyperspaceChartEntities,
                          int chartEntities, int usedNames, int topographyScans, int economyMarkets, int intel, int persistentDataKeys,
                          int sectorFleets, int chartFleets, int sectorScripts, int sectorListeners) {

    public static final int UNKNOWN = -1;

    public ChartCensus(int starSystems, int chartSystems, int hyperspaceEntities, int hyperspaceFleets, int hyperspaceChartEntities,
                       int chartEntities, int usedNames, int topographyScans, int economyMarkets, int intel, int persistentDataKeys) {
        this(starSystems, chartSystems, hyperspaceEntities, hyperspaceFleets, hyperspaceChartEntities, chartEntities, usedNames,
                topographyScans, economyMarkets, intel, persistentDataKeys, 0, 0, 0, 0);
    }

    public static ChartCensus take(SectorAPI sector) {
        int chartSystems = 0;
        int chartEntities = 0;
        int sectorFleets = 0;
        int chartFleets = 0;
        Collection<StarSystemAPI> systems = sector.getStarSystems();
        for (StarSystemAPI system : systems) {
            if (system.hasTag(ChartIds.SYSTEM_TAG)) {
                chartSystems++;
            }
            chartEntities += system.getEntitiesWithTag(ChartIds.ENTITY_TAG).size();
            sectorFleets += system.getFleets().size();
            chartFleets += chartFleetsIn(system);
        }
        LocationAPI hyperspace = sector.getHyperspace();
        int hyperspaceEntities = hyperspace == null ? 0 : hyperspace.getAllEntities().size();
        int hyperspaceFleets = hyperspace == null ? 0 : hyperspace.getFleets().size();
        int hyperspaceChartEntities = hyperspace == null ? 0 : hyperspace.getEntitiesWithTag(ChartIds.ENTITY_TAG).size();
        sectorFleets += hyperspaceFleets;
        chartFleets += hyperspace == null ? 0 : chartFleetsIn(hyperspace);
        int economyMarkets = sector.getEconomy() == null ? UNKNOWN : sector.getEconomy().getNumMarkets();
        int intel = sector.getIntelManager() == null ? UNKNOWN : sector.getIntelManager().getIntel().size();
        int scripts = sector.getScripts() == null ? UNKNOWN : sector.getScripts().size();
        int listeners = sector.getAllListeners() == null ? UNKNOWN : sector.getAllListeners().size();
        return new ChartCensus(systems.size(), chartSystems, hyperspaceEntities, hyperspaceFleets, hyperspaceChartEntities,
                chartEntities + hyperspaceChartEntities, ChartUsedNames.count(), ChartTopographyScans.count(sector), economyMarkets, intel,
                sector.getPersistentData().size(), sectorFleets, chartFleets, scripts, listeners);
    }

    private static int chartFleetsIn(LocationAPI location) {
        int found = 0;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet.getMemoryWithoutUpdate() != null && fleet.getMemoryWithoutUpdate().contains(ChartIds.TIER_MEMORY_KEY)) {
                found++;
            }
        }
        return found;
    }

    public static boolean systemNameReserved() {
        try {
            return ProcgenUsedNames.isUsed(ChartIds.SYSTEM_BASE_NAME);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public int hyperspaceEntitiesOtherThanFleets() {
        return hyperspaceEntities - hyperspaceFleets;
    }

    public List<String> changesSince(ChartCensus before) {
        List<String> changes = new ArrayList<>();
        compare(changes, "used procgen names", before.usedNames, usedNames);
        compare(changes, "Hyperspace Topography scans", before.topographyScans, topographyScans);
        compare(changes, "star systems", before.starSystems, starSystems);
        compare(changes, "hyperspace entities other than fleets", before.hyperspaceEntitiesOtherThanFleets(), hyperspaceEntitiesOtherThanFleets());
        compare(changes, "chart-tagged entities", before.chartEntities, chartEntities);
        compare(changes, "Economy markets", before.economyMarkets, economyMarkets);
        compare(changes, "intel", before.intel, intel);
        compare(changes, "fleets in the sector", before.sectorFleets, sectorFleets);
        compare(changes, "chart fleets anywhere", before.chartFleets, chartFleets);
        compare(changes, "sector scripts", before.sectorScripts, sectorScripts);
        compare(changes, "sector listeners", before.sectorListeners, sectorListeners);
        return changes;
    }

    private static void compare(List<String> changes, String label, int before, int after) {
        if (before != after) {
            changes.add(label + " " + shown(before) + " -> " + shown(after));
        }
    }

    private static String shown(int count) {
        return count == UNKNOWN ? "unknown" : String.valueOf(count);
    }

    public String describe() {
        return "star systems " + starSystems + ", chart systems " + chartSystems + ", hyperspace entities " + hyperspaceEntities
                + " (" + hyperspaceFleets + " fleets, " + hyperspaceChartEntities + " tagged for charts), chart-tagged entities anywhere "
                + chartEntities + ", used procgen names " + shown(usedNames) + ", Hyperspace Topography scans " + shown(topographyScans)
                + ", Economy markets " + shown(economyMarkets) + ", intel " + shown(intel) + ", persistent data keys " + persistentDataKeys
                + ", fleets in the sector " + sectorFleets + " (" + chartFleets + " from charts), sector scripts " + shown(sectorScripts)
                + ", sector listeners " + shown(sectorListeners);
    }
}
