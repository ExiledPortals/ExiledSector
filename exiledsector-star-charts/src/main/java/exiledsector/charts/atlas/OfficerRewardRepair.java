package exiledsector.charts.atlas;

import exiledsector.Seeds;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerStartingItems;

import java.util.Random;

public final class OfficerRewardRepair {

    private static final long REFILL_SALT = 0x4f52455246494cL;

    private OfficerRewardRepair() {
    }

    public static boolean repair(AtlasState state, TerritoryContext context) {
        boolean changed = false;
        for (AtlasOfficer officer : state.officers()) {
            if (officer.carried().isEmpty()) {
                Random random = Seeds.derived(state.seed(), REFILL_SALT ^ officer.recordId());
                AtlasOfficer dealt = OfficerStartingItems.dealt(officer, context.startingItems(), random);
                state.putOfficer(dealt);
                changed |= dealt != officer;
            }
        }
        return changed;
    }
}
