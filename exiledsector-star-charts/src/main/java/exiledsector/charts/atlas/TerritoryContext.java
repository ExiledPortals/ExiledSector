package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FactionFlagships;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerScavenging;
import exiledsector.charts.officer.OfficerStartingItems;
import exiledsector.charts.officer.ScavengeRoll;
import exiledsector.charts.officer.StartingItemRoll;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.Set;

public record TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue, StartingItemRoll startingItems,
                               ScavengeRoll scavenges, Set<Integer> openRunOfficerIds) {

    public static final TerritoryContext NONE = new TerritoryContext(null, null, FlagshipCatalogue.NONE);

    public TerritoryContext {
        openRunOfficerIds = openRunOfficerIds == null ? Set.of() : Set.copyOf(openRunOfficerIds);
    }

    public TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue) {
        this(excludedFactionId, openRunCell, catalogue, StartingItemRoll.NONE, ScavengeRoll.NONE, Set.of());
    }

    public static TerritoryContext current() {
        SectorAPI sector = Global.getSector();
        return new TerritoryContext(commissionFactionId(), openRunCellOrNull(), FactionFlagships.INSTANCE, OfficerStartingItems.forSector(sector),
                OfficerScavenging.forSector(sector), openRunOfficerIdsOrNone());
    }

    public TerritoryContext withoutOpenRun() {
        return new TerritoryContext(excludedFactionId, null, catalogue, startingItems, scavenges, Set.of());
    }

    public TerritoryContext withOpenRun(AtlasCell cell, Set<Integer> officerRecordIds) {
        return new TerritoryContext(excludedFactionId, cell, catalogue, startingItems, scavenges, officerRecordIds);
    }

    public boolean takesPartInOpenRun(AtlasState state, AtlasOfficer officer) {
        if (openRunOfficerIds.contains(officer.recordId())) {
            return true;
        }
        return openRunCell != null && officer.isActive()
                && (officer.commandCentre().equals(openRunCell) || officer.commandCentre().equals(state.commandCentreOf(openRunCell)));
    }

    private static String commissionFactionId() {
        try {
            return Misc.getCommissionFactionId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Set<Integer> openRunOfficerIdsOrNone() {
        try {
            ChartRun run = ChartRuns.currentOrNull();
            return run == null ? Set.of() : OfficerFleets.fightingRecordIdsOf(run.atlasNode());
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    private static AtlasCell openRunCellOrNull() {
        try {
            return AtlasAnomalies.openRunCell();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
