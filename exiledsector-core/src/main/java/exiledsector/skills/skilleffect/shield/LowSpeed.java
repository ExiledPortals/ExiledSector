package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;

final class LowSpeed {

    static final float TOP_SPEED_FRACTION = 0.1f;

    private LowSpeed() {
    }

    static boolean isMovingSlowly(ShipAPI ship) {
        float thresholdSpeed = ship.getMaxSpeedWithoutBoost() * TOP_SPEED_FRACTION;
        return thresholdSpeed > 0f && ship.getVelocity().lengthSquared() < thresholdSpeed * thresholdSpeed;
    }
}
