package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;

final class HighSpeed {

    static final float TOP_SPEED_FRACTION = 0.75f;

    private HighSpeed() {
    }

    static boolean isMovingFast(ShipAPI ship) {
        float thresholdSpeed = ship.getMaxSpeedWithoutBoost() * TOP_SPEED_FRACTION;
        return thresholdSpeed > 0f && ship.getVelocity().lengthSquared() > thresholdSpeed * thresholdSpeed;
    }
}
