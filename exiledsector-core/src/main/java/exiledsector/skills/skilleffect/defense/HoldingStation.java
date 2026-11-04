package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.TopSpeedShare;

final class HoldingStation {

    static final float TOP_SPEED_FRACTION = 0.25f;

    private HoldingStation() {
    }

    static boolean isHoldingStation(ShipAPI ship) {
        return TopSpeedShare.isBelow(ship, ship.getMaxSpeedWithoutBoost(), TOP_SPEED_FRACTION);
    }
}
