package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Set;

public record ChartCensus(int starSystems, int pocketSystems, int hyperspaceEntities, int hyperspaceChartEntities, int chartEntities,
                          int usedNames, int intel, int persistentDataKeys) {

    public static final int UNKNOWN = -1;
    static final String USED_NAMES_FIELD = "names";
    private static final Logger LOG = Logger.getLogger(ChartCensus.class);
    private static MethodHandle usedNamesGetter;
    private static boolean usedNamesUnreachable;

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
        int hyperspaceChartEntities = hyperspace == null ? 0 : hyperspace.getEntitiesWithTag(ChartIds.ENTITY_TAG).size();
        int intel = sector.getIntelManager() == null ? UNKNOWN : sector.getIntelManager().getIntel().size();
        return new ChartCensus(systems.size(), pockets, hyperspaceEntities, hyperspaceChartEntities, chartEntities + hyperspaceChartEntities,
                usedNameCount(), intel, sector.getPersistentData().size());
    }

    public static boolean pocketNameReserved() {
        try {
            return ProcgenUsedNames.isUsed(ChartIds.POCKET_BASE_NAME);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost this dev readout
    @SuppressWarnings("java:S1181")
    static int usedNameCount() {
        if (usedNamesUnreachable) {
            return UNKNOWN;
        }
        try {
            if (usedNamesGetter == null) {
                usedNamesGetter = MethodHandles.privateLookupIn(ProcgenUsedNames.class, MethodHandles.lookup())
                        .findGetter(ProcgenUsedNames.class, USED_NAMES_FIELD, Set.class);
            }
            return usedNamesGetter.invoke(ProcgenUsedNames.getUsed()) instanceof Set<?> names ? names.size() : UNKNOWN;
        } catch (Throwable e) {
            usedNamesUnreachable = true;
            LOG.warn("[ExiledSector] The used procgen name list can't be read, so the Star Chart readout shows it as unknown", e);
            return UNKNOWN;
        }
    }

    public String describe() {
        return "star systems " + starSystems + ", chart pockets " + pocketSystems + ", hyperspace entities " + hyperspaceEntities
                + " (" + hyperspaceChartEntities + " tagged for charts), chart-tagged entities anywhere " + chartEntities
                + ", used procgen names " + (usedNames == UNKNOWN ? "unknown" : String.valueOf(usedNames))
                + ", intel " + (intel == UNKNOWN ? "unknown" : String.valueOf(intel)) + ", persistent data keys " + persistentDataKeys;
    }
}
