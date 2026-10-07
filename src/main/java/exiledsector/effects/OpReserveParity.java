package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.progression.SkillNodeOpCost;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OpReserveParity {

    private static final Set<String> WARNED = new HashSet<>();

    private OpReserveParity() {
    }

    public static int reservedOp(ShipVariantAPI variant) {
        int reservedOpTotal = 0;
        for (String hullModId : reserveHullMods(variant)) {
            HullModSpecAPI reserveSpec = Global.getSettings().getHullModSpec(hullModId);
            if (reserveSpec != null) {
                reservedOpTotal += reserveSpec.getCostFor(variant.getHullSize());
            }
        }
        return reservedOpTotal;
    }

    public static void warnIfOutOfSync(FleetMemberAPI member, ShipVariantAPI variant, String moment) {
        if (member == null || variant == null || SkillDataResolver.isNpcTree(variant)) return;

        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        int spentOp = shipData == null ? 0 : shipData.getSpentOp(SkillNodeOpCost.perNode(member.getHullSpec()));
        warnIfOutOfSync(member, variant, spentOp, reservedOp(variant), moment);
    }

    public static void warnIfOutOfSync(FleetMemberAPI member, ShipVariantAPI variant, int spentOp, int reservedOp, String moment) {
        if (spentOp == reservedOp || !WARNED.add(member.getId() + "|" + moment + "|" + spentOp + "|" + reservedOp)) return;

        Logger.getLogger(OpReserveParity.class).warn("[ExiledSector] OP reserve out of sync " + moment + " on "
                + member.getShipName() + " (" + member.getHullId() + ", member " + member.getId() + "): its paid nodes cost "
                + spentOp + " OP but the variant reserves " + reservedOp + " OP " + reserveHullMods(variant)
                + (variant == member.getVariant() ? "" : " on the refit screen's working copy") + ".");
    }

    private static List<String> reserveHullMods(ShipVariantAPI variant) {
        List<String> reserveIds = new ArrayList<>();
        for (String hullModId : variant.getHullMods()) {
            if (hullModId.startsWith(OpReserveHullMods.ID_PREFIX)) {
                reserveIds.add(hullModId);
            }
        }
        return reserveIds;
    }
}
