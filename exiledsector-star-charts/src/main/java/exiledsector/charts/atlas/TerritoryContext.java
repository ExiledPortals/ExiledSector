package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FactionFlagships;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerStartingItems;
import exiledsector.charts.officer.StartingItemRoll;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

public record TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue, StartingItemRoll startingItems,
                               int openRunOfficerId) {

    public static final TerritoryContext NONE = new TerritoryContext(null, null, FlagshipCatalogue.NONE);

    public TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue) {
        this(excludedFactionId, openRunCell, catalogue, StartingItemRoll.NONE, Intent.NO_OFFICER);
    }

    public static TerritoryContext current() {
        return new TerritoryContext(commissionFactionId(), openRunCellOrNull(), FactionFlagships.INSTANCE,
                OfficerStartingItems.forSector(Global.getSector()), openRunOfficerIdOrNone());
    }

    public TerritoryContext withoutOpenRun() {
        return new TerritoryContext(excludedFactionId, null, catalogue, startingItems, Intent.NO_OFFICER);
    }

    public TerritoryContext withOpenRun(AtlasCell cell, int officerRecordId) {
        return new TerritoryContext(excludedFactionId, cell, catalogue, startingItems, officerRecordId);
    }

    public boolean takesPartInOpenRun(AtlasState state, AtlasOfficer officer) {
        if (openRunOfficerId != Intent.NO_OFFICER && officer.recordId() == openRunOfficerId) {
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

    private static int openRunOfficerIdOrNone() {
        try {
            ChartRun run = ChartRuns.currentOrNull();
            Integer recordId = run == null ? null : OfficerFleets.recordIdOf(run.atlasNode());
            return recordId == null ? Intent.NO_OFFICER : recordId;
        } catch (RuntimeException e) {
            return Intent.NO_OFFICER;
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
