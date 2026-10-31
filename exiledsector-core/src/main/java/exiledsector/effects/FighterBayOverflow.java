package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;

import java.util.ArrayList;
import java.util.List;

public final class FighterBayOverflow {

    private static final String KEPT_BAYS_MOD_ID = "exiledSector_keptBays";

    private FighterBayOverflow() {
    }

    public static void keepBaysForFittedWings(MutableShipStatsAPI stats, ShipVariantAPI variant) {
        if (stats == null || variant == null || variant.getWings() == null) {
            return;
        }
        int missingBays = variant.getWings().size() - Math.round(stats.getNumFighterBays().getModifiedValue());
        if (missingBays > 0) {
            stats.getNumFighterBays().modifyFlat(KEPT_BAYS_MOD_ID, missingBays);
        }
    }

    public static List<String> returnUnhousedWings(FleetMemberAPI member, ShipVariantAPI variant, CargoAPI cargo) {
        List<String> returnedWingIds = new ArrayList<>();
        if (member == null || variant == null || cargo == null || member.getStats() == null) {
            return returnedWingIds;
        }
        int bayCount = Math.max(0, Math.round(member.getStats().getNumFighterBays().getModifiedValue()));
        for (int bayIndex = variant.getWings().size() - 1; bayIndex >= bayCount; bayIndex--) {
            String wingId = variant.getWingId(bayIndex);
            if (wingId == null || wingId.isEmpty() || variant.getHullSpec().isBuiltInWing(bayIndex)) {
                continue;
            }
            variant.setWingId(bayIndex, null);
            cargo.addFighters(wingId, 1);
            returnedWingIds.add(wingId);
        }
        return returnedWingIds;
    }

    public static void returnUnhousedWingsAndAnnounce(FleetMemberAPI member, ShipVariantAPI variant, CargoAPI cargo) {
        ReturnedToCargo.announce(member, returnUnhousedWings(member, variant, cargo), FighterBayOverflow::wingName, "fighterBay.returned", "wing");
    }

    private static String wingName(String wingId) {
        FighterWingSpecAPI wing = Global.getSettings().getFighterWingSpec(wingId);
        return wing == null ? wingId : wing.getWingName();
    }
}
