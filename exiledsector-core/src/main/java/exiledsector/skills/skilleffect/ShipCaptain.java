package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class ShipCaptain {

    private ShipCaptain() {
    }

    public static PersonAPI of(MutableShipStatsAPI stats) {
        if (stats == null) {
            return null;
        }
        if (stats.getEntity() instanceof ShipAPI ship) {
            return ship.getCaptain();
        }
        return stats.getFleetMember() == null ? null : stats.getFleetMember().getCaptain();
    }

    public static boolean absent(MutableShipStatsAPI stats) {
        PersonAPI captain = of(stats);
        return captain == null || captain.isDefault();
    }
}
