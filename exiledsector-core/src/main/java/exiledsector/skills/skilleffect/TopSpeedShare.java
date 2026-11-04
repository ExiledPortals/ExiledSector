package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

public final class TopSpeedShare {

    private TopSpeedShare() {
    }

    public static boolean isAbove(ShipAPI ship, float topSpeed, float share) {
        float thresholdSpeed = topSpeed * share;
        return thresholdSpeed > 0f && speedSquared(ship) > thresholdSpeed * thresholdSpeed;
    }

    public static boolean isAtLeast(ShipAPI ship, float topSpeed, float share) {
        float thresholdSpeed = topSpeed * share;
        return thresholdSpeed > 0f && speedSquared(ship) >= thresholdSpeed * thresholdSpeed;
    }

    public static boolean isBelow(ShipAPI ship, float topSpeed, float share) {
        float thresholdSpeed = topSpeed * share;
        return thresholdSpeed > 0f && speedSquared(ship) < thresholdSpeed * thresholdSpeed;
    }

    private static float speedSquared(ShipAPI ship) {
        return ship.getVelocity().lengthSquared();
    }
}
