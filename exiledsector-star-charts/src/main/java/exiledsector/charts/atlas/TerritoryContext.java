package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.FactionFlagships;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerStartingItems;
import exiledsector.charts.officer.StartingItemRoll;

public record TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue, StartingItemRoll startingItems) {

    public static final TerritoryContext NONE = new TerritoryContext(null, null, FlagshipCatalogue.NONE);

    public TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue) {
        this(excludedFactionId, openRunCell, catalogue, StartingItemRoll.NONE);
    }

    public static TerritoryContext current() {
        return new TerritoryContext(commissionFactionId(), openRunCellOrNull(), FactionFlagships.INSTANCE,
                OfficerStartingItems.forSector(Global.getSector()));
    }

    private static String commissionFactionId() {
        try {
            return Misc.getCommissionFactionId();
        } catch (RuntimeException e) {
            return null;
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
