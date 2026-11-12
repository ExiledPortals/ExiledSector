package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.effects.SkillTreeHullMod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Scars {

    private Scars() {
    }

    public static void grant(CampaignFleetAPI fleet, String flagshipMemberId, Map<String, Integer> scars) {
        if (fleet == null || flagshipMemberId == null) {
            return;
        }
        ScarMemory.write(fleet.getMemoryWithoutUpdate(), flagshipMemberId, scars);
        reinstall(fleet);
    }

    public static void reinstall(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        String flagshipMemberId = ScarMemory.flagshipId(fleet.getMemoryWithoutUpdate());
        if (flagshipMemberId == null || ScarMemory.scars(fleet.getMemoryWithoutUpdate()).isEmpty()) {
            return;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (flagshipMemberId.equals(member.getId())) {
                installHullMod(member);
            }
        }
    }

    private static void installHullMod(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();
        if (variant == null || variant.hasHullMod(SkillTreeHullMod.ID)) {
            return;
        }
        if (variant.getSource() != VariantSource.REFIT) {
            variant = variant.clone();
            variant.setSource(VariantSource.REFIT);
            member.setVariant(variant, false, true);
        }
        variant.addMod(SkillTreeHullMod.ID);
    }

    public static Map<String, Integer> scarsOf(CampaignFleetAPI fleet) {
        return fleet == null ? Map.of() : ScarMemory.scars(fleet.getMemoryWithoutUpdate());
    }

    public static String flagshipIdOf(CampaignFleetAPI fleet) {
        return fleet == null ? null : ScarMemory.flagshipId(fleet.getMemoryWithoutUpdate());
    }

    public static Map<String, Integer> withScar(Map<String, Integer> scars, String scarId) {
        Map<String, Integer> added = new LinkedHashMap<>(scars == null ? Map.of() : scars);
        if (scarId != null) {
            added.merge(scarId, 1, Integer::sum);
        }
        return added;
    }

    public static String pickAfterBattle(String flagshipMemberId, Map<String, Integer> currentScars, HullFacts hull) {
        return pickAfterBattle(ScarBattleResults.take(flagshipMemberId), currentScars, hull);
    }

    public static String pickAfterBattle(ScarEvidence evidence, Map<String, Integer> currentScars, HullFacts hull) {
        return evidence == null ? ScarTable.FALLBACK_SCAR_ID : ScarScoring.pick(evidence, ScarTable.rows(), currentScars, hull);
    }

    public static String pickAfterChallengeLoss(String winnerFactionId, Map<String, Integer> currentScars, HullFacts hull) {
        return ScarChallengePick.pick(winnerFactionId, currentScars, hull);
    }

    public static String dModFor(String scarId, List<String> currentDMods, HullFacts hull, Random random) {
        return ScarDMods.pick(ScarTable.row(scarId), currentDMods, hull, random);
    }

    public static int extraLevels(Map<String, Integer> scars) {
        return ScarLevels.extraLevels(scars);
    }
}
