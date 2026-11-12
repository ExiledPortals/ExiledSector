package exiledsector.charts.atlas;

import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.officer.FactionFlagships;
import exiledsector.charts.officer.FlagshipCatalogue;

public record TerritoryContext(String excludedFactionId, AtlasCell openRunCell, FlagshipCatalogue catalogue) {

    public static final TerritoryContext NONE = new TerritoryContext(null, null, FlagshipCatalogue.NONE);

    public static TerritoryContext current() {
        return new TerritoryContext(commissionFactionId(), openRunCellOrNull(), FactionFlagships.INSTANCE);
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
