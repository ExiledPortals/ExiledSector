package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.shield.LowSpeed;

final class HoldingStation {

    static final float TOP_SPEED_FRACTION = 0.25f;

    private HoldingStation() {
    }

    static boolean isHoldingStation(ShipAPI ship) {
        return LowSpeed.isSlowerThan(ship, TOP_SPEED_FRACTION);
    }
}
