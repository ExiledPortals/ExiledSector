package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;

public final class LowSpeed {

    static final float TOP_SPEED_FRACTION = 0.1f;

    private LowSpeed() {
    }

    static boolean isMovingSlowly(ShipAPI ship) {
        return isSlowerThan(ship, TOP_SPEED_FRACTION);
    }

    public static boolean isSlowerThan(ShipAPI ship, float topSpeedFraction) {
        float thresholdSpeed = ship.getMaxSpeedWithoutBoost() * topSpeedFraction;
        return thresholdSpeed > 0f && ship.getVelocity().lengthSquared() < thresholdSpeed * thresholdSpeed;
    }
}
