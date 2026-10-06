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
        List<String> returned = new ArrayList<>();
        if (member == null || variant == null || cargo == null || member.getStats() == null) {
            return returned;
        }
        int bays = Math.max(0, Math.round(member.getStats().getNumFighterBays().getModifiedValue()));
        for (int bay = variant.getWings().size() - 1; bay >= bays; bay--) {
            String wingId = variant.getWingId(bay);
            if (wingId == null || wingId.isEmpty() || variant.getHullSpec().isBuiltInWing(bay)) {
                continue;
            }
            variant.setWingId(bay, null);
            cargo.addFighters(wingId, 1);
            returned.add(wingId);
        }
        return returned;
    }
}
