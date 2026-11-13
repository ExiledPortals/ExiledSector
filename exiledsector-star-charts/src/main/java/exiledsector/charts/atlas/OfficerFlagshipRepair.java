package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import exiledsector.Seeds;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarDMods;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

public final class OfficerFlagshipRepair {

    private static final long REROLL_SALT = 0x4f46524f4c4cL;

    private OfficerFlagshipRepair() {
    }

    public static boolean repair(AtlasState state, List<OfficerRank> ranks, TerritoryContext context) {
        return repair(state, ranks, context, OfficerFlagshipRepair::dModsSuiting);
    }

    static boolean repair(AtlasState state, List<OfficerRank> ranks, TerritoryContext context,
                          BiFunction<FlagshipCatalogue.Flagship, List<String>, List<String>> dModsKept) {
        if (state.flagshipRoll() >= AtlasState.FLAGSHIP_ROLL) {
            return false;
        }
        List<AtlasOfficer> officers = state.officers();
        if (officers.stream().anyMatch(officer -> context.takesPartInOpenRun(state, officer))) {
            return false;
        }
        boolean changed = false;
        for (AtlasOfficer officer : officers) {
            Random random = Seeds.derived(state.seed(), REROLL_SALT ^ officer.recordId());
            FlagshipCatalogue.Flagship flagship = context.catalogue().pick(officer.factionId(), OfficerSeats.order(ranks, officer.rankId()),
                    ranks.size(), random);
            if (flagship != null) {
                state.putOfficer(officer.flying(flagship, dModsKept.apply(flagship, officer.dmodIds())));
                changed = true;
            }
        }
        state.setFlagshipRoll(AtlasState.FLAGSHIP_ROLL);
        return changed;
    }

    private static List<String> dModsSuiting(FlagshipCatalogue.Flagship flagship, List<String> dModIds) {
        try {
            return ScarDMods.suiting(dModIds, HullFacts.of(Global.getSettings().getVariant(flagship.variantId())));
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
