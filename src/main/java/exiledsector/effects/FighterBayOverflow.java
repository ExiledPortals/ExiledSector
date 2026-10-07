package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.List;

public final class FighterBayOverflow {

    private FighterBayOverflow() {
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
}
