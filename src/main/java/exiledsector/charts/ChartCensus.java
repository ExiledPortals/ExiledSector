package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ChartCensus(int starSystems, int pocketSystems, int hyperspaceEntities, int hyperspaceFleets, int hyperspaceChartEntities,
                          int chartEntities, int usedNames, int topographyScans, int economyMarkets, int intel, int persistentDataKeys) {

    public static final int UNKNOWN = -1;

    public static ChartCensus take(SectorAPI sector) {
        int pockets = 0;
        int chartEntities = 0;
        Collection<StarSystemAPI> systems = sector.getStarSystems();
        for (StarSystemAPI system : systems) {
            if (system.hasTag(ChartIds.POCKET_TAG)) {
                pockets++;
            }
            chartEntities += system.getEntitiesWithTag(ChartIds.ENTITY_TAG).size();
        }
        LocationAPI hyperspace = sector.getHyperspace();
        int hyperspaceEntities = hyperspace == null ? 0 : hyperspace.getAllEntities().size();
        int hyperspaceFleets = hyperspace == null ? 0 : hyperspace.getFleets().size();
        int hyperspaceChartEntities = hyperspace == null ? 0 : hyperspace.getEntitiesWithTag(ChartIds.ENTITY_TAG).size();
        int economyMarkets = sector.getEconomy() == null ? UNKNOWN : sector.getEconomy().getNumMarkets();
        int intel = sector.getIntelManager() == null ? UNKNOWN : sector.getIntelManager().getIntel().size();
        return new ChartCensus(systems.size(), pockets, hyperspaceEntities, hyperspaceFleets, hyperspaceChartEntities,
                chartEntities + hyperspaceChartEntities, ChartUsedNames.count(), ChartTopographyScans.count(sector), economyMarkets, intel,
                sector.getPersistentData().size());
    }

    public static boolean pocketNameReserved() {
        try {
            return ProcgenUsedNames.isUsed(ChartIds.POCKET_BASE_NAME);
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
        return "star systems " + starSystems + ", chart pockets " + pocketSystems + ", hyperspace entities " + hyperspaceEntities
                + " (" + hyperspaceFleets + " fleets, " + hyperspaceChartEntities + " tagged for charts), chart-tagged entities anywhere "
                + chartEntities + ", used procgen names " + shown(usedNames) + ", Hyperspace Topography scans " + shown(topographyScans)
                + ", Economy markets " + shown(economyMarkets) + ", intel " + shown(intel) + ", persistent data keys " + persistentDataKeys;
    }
}
