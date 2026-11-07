package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSector;
import exiledsector.i18n.Translation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TowLines {

    static final String TOW_BURN_LEVEL_KEY = "exiledSector_towSlowestShipBurnLevel";
    private static final String TOW_LINE_MEMBER_MOD = "towLine";

    private TowLines() {
    }

    public static void apply(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) {
            return;
        }
        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        String modId = ExiledSector.key(TOW_LINE_MEMBER_MOD);
        for (FleetMemberAPI member : members) {
            member.getStats().getMaxBurnLevel().unmodifyFlat(modId);
        }
        Map<FleetMemberAPI, Float> bonuses = towBonuses(members);
        if (bonuses.isEmpty()) {
            return;
        }
        String source = Translation.gameText("fleet.towLine");
        bonuses.forEach((towed, bonus) -> towed.getStats().getMaxBurnLevel().modifyFlat(modId, bonus, source));
    }

    static Map<FleetMemberAPI, Float> towBonuses(List<FleetMemberAPI> members) {
        List<FleetMemberAPI> tugs = members.stream().filter(member -> towBonus(member) > 0f)
                .sorted(Comparator.comparing(TowLines::hullSize).thenComparing(TowLines::burnLevel)).toList();
        Map<FleetMemberAPI, Float> bonuses = new LinkedHashMap<>();
        for (FleetMemberAPI tug : tugs) {
            float tugBurnLevel = towedBurnLevel(tug, bonuses);
            members.stream()
                    .filter(member -> canTow(tug, tugBurnLevel, member, bonuses))
                    .min(Comparator.<FleetMemberAPI, Float>comparing(member -> towedBurnLevel(member, bonuses))
                            .thenComparing(TowLines::hullSize, Comparator.reverseOrder()))
                    .ifPresent(towed -> bonuses.merge(towed,
                            Math.min(towBonus(tug), tugBurnLevel - towedBurnLevel(towed, bonuses)), Float::sum));
        }
        return bonuses;
    }

    private static boolean canTow(FleetMemberAPI tug, float tugBurnLevel, FleetMemberAPI towed, Map<FleetMemberAPI, Float> bonuses) {
        return !towed.isFighterWing() && towedBurnLevel(towed, bonuses) < tugBurnLevel
                && hullSize(towed).compareTo(hullSize(tug)) <= 0;
    }

    private static float towedBurnLevel(FleetMemberAPI member, Map<FleetMemberAPI, Float> bonuses) {
        return burnLevel(member) + bonuses.getOrDefault(member, 0f);
    }

    private static float towBonus(FleetMemberAPI member) {
        return member.isMothballed() ? 0f : member.getStats().getDynamic().getValue(TOW_BURN_LEVEL_KEY, 0f);
    }

    private static float burnLevel(FleetMemberAPI member) {
        return member.getStats().getMaxBurnLevel().getModifiedValue();
    }

    private static HullSize hullSize(FleetMemberAPI member) {
        return member.getHullSpec().getHullSize();
    }
}
