package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.persistence.OpSpentSlotManager;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.progression.SkillNodeOpCost;

import java.util.ArrayList;
import java.util.List;

public final class OpReserveHullMods {

    static final String ID_PREFIX = "exiledSector_opSpent_";

    private OpReserveHullMods() {
    }

    public static void sync(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || variant == null || SkillDataResolver.isNpcTree(variant)) return;

        ShipSkillData data = ShipSkillDataManager.find(member.getId());
        int opSpent = data == null ? 0 : data.getSpentOp(SkillNodeOpCost.perNode(member.getHullSpec()));
        if (opSpent <= 0) {
            Integer slot = OpSpentSlotManager.existingSlot(member.getId());
            if (slot != null) {
                String reserveId = ID_PREFIX + slot;
                setReserveCost(reserveId, 0);
                variant.removeMod(reserveId);
            }
            removeReservesOfReleasedSlots(variant);
            return;
        }

        String reserveId = reserveHullModFor(member.getId(), opSpent);
        for (String hullModId : new ArrayList<>(variant.getHullMods())) {
            if (hullModId.startsWith(ID_PREFIX) && !hullModId.equals(reserveId)) {
                variant.removeMod(hullModId);
            }
        }
        if (reserveId != null && !variant.hasHullMod(reserveId)) {
            variant.addMod(reserveId);
        }
    }

    private static void removeReservesOfReleasedSlots(ShipVariantAPI variant) {
        List<String> stale = null;
        for (String hullModId : variant.getHullMods()) {
            if (hullModId.startsWith(ID_PREFIX) && isReleasedSlot(hullModId)) {
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(hullModId);
            }
        }
        if (stale != null) {
            stale.forEach(variant::removeMod);
        }
    }

    private static boolean isReleasedSlot(String reserveId) {
        try {
            return !OpSpentSlotManager.isAssigned(Integer.parseInt(reserveId.substring(ID_PREFIX.length())));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String reserveHullModFor(String shipId, int opSpent) {
        String hullModId = ID_PREFIX + OpSpentSlotManager.slotFor(shipId);
        return setReserveCost(hullModId, opSpent) ? hullModId : null;
    }

    private static boolean setReserveCost(String hullModId, int cost) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        if (spec == null) return false;

        spec.setFrigateCost(cost);
        spec.setDestroyerCost(cost);
        spec.setCruiserCost(cost);
        spec.setCapitalCost(cost);
        return true;
    }
}
