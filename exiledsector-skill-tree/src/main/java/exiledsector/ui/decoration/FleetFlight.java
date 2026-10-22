package exiledsector.ui.decoration;

import exiledlib.skills.skilleffect.Fade;

import java.util.Random;
import java.util.function.UnaryOperator;

final class FleetFlight {

    static final float ARRIVAL_DISTANCE = 150f;
    static final float IDLE_CHANCE = 0.3f;
    static final float MIN_IDLE_SECONDS = 2f;
    static final float MAX_IDLE_SECONDS = 6f;
    static final float ACCELERATION_PER_SPEED = 0.5f;
    static final float IDLE_SPEED_DECAY_PER_SECOND = 1.5f;
    static final float ENGINE_FADE_SECONDS = 0.5f;
    static final float MOVING_SPEED = 5f;
    static final float HOLD_DISTANCE = 40f;
    static final float HOLD_SLOWING_SECONDS = 2.5f;
    private static final int WAYPOINT_ATTEMPTS = 8;

    private final UnaryOperator<RoamVolume> nextVolume;
    private final Random random;
    private final float travelSpeed;
    private final float[] point = new float[2];
    private float positionX;
    private float positionY;
    private float velocityX;
    private float velocityY;
    private float waypointX;
    private float waypointY;
    private float idleSecondsLeft;
    private float engineLevel;
    private float facingDeg;
    private RoamVolume roamVolume;
    private boolean holding;
    private float faceTowardX;
    private float faceTowardY;

    FleetFlight(RoamVolume roamVolume, float travelSpeed, Random random) {
        this(roamVolume, travelSpeed, random, UnaryOperator.identity());
    }

    FleetFlight(RoamVolume roamVolume, float travelSpeed, Random random, UnaryOperator<RoamVolume> nextVolume) {
        this.roamVolume = roamVolume;
        this.nextVolume = nextVolume;
        this.travelSpeed = travelSpeed;
        this.random = random;
        roamVolume.randomPoint(random, point);
        positionX = point[0];
        positionY = point[1];
        facingDeg = random.nextFloat() * 360f;
        pickWaypoint(roamVolume);
    }

    void advance(float amount) {
        if (idleSecondsLeft > 0f) {
            idleSecondsLeft -= amount;
            float decay = Math.max(0f, 1f - IDLE_SPEED_DECAY_PER_SECOND * amount);
            velocityX *= decay;
            velocityY *= decay;
        } else {
            steerTowardWaypoint(amount, holding ? Math.min(1f, distanceToWaypoint() / (travelSpeed * HOLD_SLOWING_SECONDS)) : 1f);
        }
        positionX += velocityX * amount;
        positionY += velocityY * amount;
        float speed = (float) Math.hypot(velocityX, velocityY);
        if (speed > MOVING_SPEED) {
            facingDeg = (float) Math.toDegrees(Math.atan2(-velocityY, velocityX));
        } else if (holding) {
            facingDeg = (float) Math.toDegrees(Math.atan2(-(faceTowardY - positionY), faceTowardX - positionX));
        }
        boolean thrusting = idleSecondsLeft <= 0f && speed > MOVING_SPEED;
        float engineStep = amount / ENGINE_FADE_SECONDS;
        engineLevel = Fade.step(engineLevel, thrusting, engineStep);
        if (!holding && idleSecondsLeft <= 0f && distanceToWaypoint() < ARRIVAL_DISTANCE) {
            if (random.nextFloat() < IDLE_CHANCE) {
                idleSecondsLeft = MIN_IDLE_SECONDS + random.nextFloat() * (MAX_IDLE_SECONDS - MIN_IDLE_SECONDS);
            }
            RoamVolume leaving = roamVolume;
            roamVolume = nextVolume.apply(roamVolume);
            if (!holding) {
                pickWaypoint(leaving);
            }
        }
    }

    void holdAt(float holdX, float holdY, float towardX, float towardY) {
        holding = true;
        idleSecondsLeft = 0f;
        waypointX = holdX;
        waypointY = holdY;
        faceTowardX = towardX;
        faceTowardY = towardY;
    }

    void release(RoamVolume destination) {
        holding = false;
        RoamVolume leaving = roamVolume;
        roamVolume = destination;
        pickWaypoint(leaving);
    }

    boolean isAtHold() {
        return holding && distanceToWaypoint() < HOLD_DISTANCE;
    }

    private float distanceToWaypoint() {
        return (float) Math.hypot(waypointX - positionX, waypointY - positionY);
    }

    private void steerTowardWaypoint(float amount, float speedFraction) {
        float towardX = waypointX - positionX;
        float towardY = waypointY - positionY;
        float distance = (float) Math.hypot(towardX, towardY);
        float desiredX = distance > 0f ? towardX / distance * travelSpeed * speedFraction : 0f;
        float desiredY = distance > 0f ? towardY / distance * travelSpeed * speedFraction : 0f;
        float changeX = desiredX - velocityX;
        float changeY = desiredY - velocityY;
        float change = (float) Math.hypot(changeX, changeY);
        float maxChange = travelSpeed * ACCELERATION_PER_SPEED * amount;
        if (change > maxChange) {
            changeX *= maxChange / change;
            changeY *= maxChange / change;
        }
        velocityX += changeX;
        velocityY += changeY;
    }

    private void pickWaypoint(RoamVolume leaving) {
        for (int attempt = 0; attempt < WAYPOINT_ATTEMPTS; attempt++) {
            roamVolume.randomPoint(random, point);
            if (leaving.segmentClearsStar(positionX, positionY, point[0], point[1])
                    && roamVolume.segmentClearsStar(positionX, positionY, point[0], point[1])) {
                break;
            }
        }
        waypointX = point[0];
        waypointY = point[1];
    }

    RoamVolume roamVolume() {
        return roamVolume;
    }

    float positionX() {
        return positionX;
    }

    float positionY() {
        return positionY;
    }

    float facingDeg() {
        return facingDeg;
    }

    float engineLevel() {
        return engineLevel;
    }

    float waypointX() {
        return waypointX;
    }

    float waypointY() {
        return waypointY;
    }
}
