package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.Random;

final class FleetShipDrift {

    static final float SPRING_STRENGTH = 3f;
    static final float VELOCITY_DAMPING = 2f;
    static final float ACCELERATION_PER_SPEED = 0.5f;
    static final float RETARGET_DISTANCE = 3f;
    static final float TURN_RATE_MULT = 3f;
    static final float FIRST_TURN_DEG = 120f;
    static final float EXTRA_TURN_DEG = 120f;

    record HullMotion(float scaleMult, float moveSpeed, float maxTurnRate) {

        static HullMotion of(HullSize hullSize) {
            return switch (hullSize == null ? HullSize.DEFAULT : hullSize) {
                case FRIGATE -> new HullMotion(0.11f, 14f, 120f);
                case DESTROYER -> new HullMotion(0.09f, 10f, 80f);
                case CRUISER -> new HullMotion(0.08f, 8f, 60f);
                case CAPITAL_SHIP -> new HullMotion(0.07f, 6f, 40f);
                case FIGHTER -> new HullMotion(0.15f, 15f, 200f);
                default -> new HullMotion(0.10f, 6f, 60f);
            };
        }
    }

    private final HullMotion motion;
    private final float maxOffset;
    private final Random random;
    private float offsetX;
    private float offsetY;
    private float velocityX;
    private float velocityY;
    private float targetX;
    private float targetY;
    private float targetAngleDeg;
    private float facingDeg;

    FleetShipDrift(HullMotion motion, float maxOffset, float startFacingDeg, Random random) {
        this.motion = motion;
        this.maxOffset = maxOffset;
        this.random = random;
        this.facingDeg = startFacingDeg;
        this.targetAngleDeg = random.nextFloat() * 360f;
        pickTarget();
        offsetX = targetX;
        offsetY = targetY;
        pickNextTarget();
    }

    static float maxOffset(float fleetRadius, float largestSizeNum, float sizeNum, boolean onlyLargest) {
        if (onlyLargest || largestSizeNum <= 0f) {
            return 0f;
        }
        return ((largestSizeNum - sizeNum) / largestSizeNum * 0.5f + 0.5f) * fleetRadius;
    }

    void advance(float amount, float fleetFacingDeg) {
        float accelerationX = SPRING_STRENGTH * (targetX - offsetX) - VELOCITY_DAMPING * velocityX;
        float accelerationY = SPRING_STRENGTH * (targetY - offsetY) - VELOCITY_DAMPING * velocityY;
        float acceleration = (float) Math.hypot(accelerationX, accelerationY);
        float maxAcceleration = motion.moveSpeed() * ACCELERATION_PER_SPEED;
        if (acceleration > maxAcceleration) {
            accelerationX *= maxAcceleration / acceleration;
            accelerationY *= maxAcceleration / acceleration;
        }
        velocityX += accelerationX * amount;
        velocityY += accelerationY * amount;
        float speed = (float) Math.hypot(velocityX, velocityY);
        if (speed > motion.moveSpeed()) {
            velocityX *= motion.moveSpeed() / speed;
            velocityY *= motion.moveSpeed() / speed;
        }
        offsetX += velocityX * amount;
        offsetY += velocityY * amount;
        if (maxOffset > 0f && Math.hypot(targetX - offsetX, targetY - offsetY) < RETARGET_DISTANCE) {
            pickNextTarget();
        }
        float turn = shortestTurn(facingDeg, fleetFacingDeg);
        float maxTurn = motion.maxTurnRate() * TURN_RATE_MULT * amount;
        facingDeg += Math.max(-maxTurn, Math.min(maxTurn, turn));
    }

    static float shortestTurn(float fromDeg, float toDeg) {
        float turn = (toDeg - fromDeg) % 360f;
        if (turn > 180f) {
            turn -= 360f;
        } else if (turn < -180f) {
            turn += 360f;
        }
        return turn;
    }

    private void pickNextTarget() {
        targetAngleDeg += FIRST_TURN_DEG + random.nextFloat() * EXTRA_TURN_DEG;
        pickTarget();
    }

    private void pickTarget() {
        float radius = (float) Math.sqrt(random.nextFloat()) * maxOffset;
        double angle = Math.toRadians(targetAngleDeg);
        targetX = radius * (float) Math.cos(angle);
        targetY = radius * (float) Math.sin(angle);
    }

    HullMotion motion() {
        return motion;
    }

    float offsetX() {
        return offsetX;
    }

    float offsetY() {
        return offsetY;
    }

    float facingDeg() {
        return facingDeg;
    }
}
