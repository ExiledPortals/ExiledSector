package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.RetiredReserveIds;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.progression.SkillNodeOpCost;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class OpReserveHullMods {

    static final String ID_PREFIX = "exiledSector_opReserve_";
    static final String LEGACY_ID_PREFIX = RetiredReserveIds.LEGACY_PREFIX;
    static final int MAX_RESERVED_OP = 9999;
    private static final int[] PLACE_VALUES = {1000, 100, 10, 1};
    private static final String LEGACY_SLOTS_KEY = "exiledSector_opSpentSlots";
    private static final String LEGACY_NEXT_SLOT_KEY = "exiledSector_opSpentNextSlot";

    private static boolean capReported;

    private OpReserveHullMods() {
    }

    public static boolean isReserveHullMod(String hullModId) {
        return hullModId.startsWith(ID_PREFIX) || hullModId.startsWith(LEGACY_ID_PREFIX);
    }

    static List<String> reserveHullModsFor(int opSpent) {
        List<String> reserveIds = new ArrayList<>();
        int remainingOp = Math.max(0, Math.min(opSpent, MAX_RESERVED_OP));
        for (int placeValue : PLACE_VALUES) {
            int digit = remainingOp / placeValue;
            if (digit > 0) {
                reserveIds.add(ID_PREFIX + digit * placeValue);
            }
            remainingOp %= placeValue;
        }
        return reserveIds;
    }

    public static void sync(FleetMemberAPI member, ShipVariantAPI variant) {
        if (member == null || variant == null || SkillDataResolver.isNpcTree(variant)) return;

        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        if (shipData == null) {
            removeMatching(variant, hullModId -> hullModId.startsWith(LEGACY_ID_PREFIX));
            return;
        }
        int opSpent = shipData.getSpentOp(SkillNodeOpCost.perNode(member.getHullSpec()));
        reportIfCapped(member, opSpent);
        List<String> wantedReserveIds = reserveHullModsFor(opSpent);
        removeMatching(variant, hullModId -> isReserveHullMod(hullModId) && !wantedReserveIds.contains(hullModId));
        for (String reserveId : wantedReserveIds) {
            if (!variant.hasHullMod(reserveId) && Global.getSettings().getHullModSpec(reserveId) != null) {
                variant.addMod(reserveId);
            }
        }
    }

    public static void forgetLegacySlots(Map<String, Object> persistentData) {
        persistentData.remove(LEGACY_SLOTS_KEY);
        persistentData.remove(LEGACY_NEXT_SLOT_KEY);
    }

    private static void removeMatching(ShipVariantAPI variant, Predicate<String> filter) {
        List<String> removedIds = null;
        for (String hullModId : variant.getHullMods()) {
            if (filter.test(hullModId)) {
                if (removedIds == null) {
                    removedIds = new ArrayList<>();
                }
                removedIds.add(hullModId);
            }
        }
        if (removedIds != null) {
            removedIds.forEach(variant::removeMod);
        }
    }

    private static void reportIfCapped(FleetMemberAPI member, int opSpent) {
        if (opSpent <= MAX_RESERVED_OP || capReported) return;

        capReported = true;
        Logger.getLogger(OpReserveHullMods.class).error("[ExiledSector] Ship " + member.getId() + " has spent " + opSpent
                + " OP on paid nodes, more than the " + MAX_RESERVED_OP + " OP the reserve hull mods can hold.");
    }
}
