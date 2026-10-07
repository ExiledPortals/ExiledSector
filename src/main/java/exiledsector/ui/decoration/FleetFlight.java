package exiledsector.ui.decoration;

import java.util.Random;

final class FleetFlight {

    static final float ARRIVAL_DISTANCE = 150f;
    static final float IDLE_CHANCE = 0.3f;
    static final float MIN_IDLE_SECONDS = 2f;
    static final float MAX_IDLE_SECONDS = 6f;
    static final float ACCELERATION_PER_SPEED = 0.5f;
    static final float IDLE_SPEED_DECAY_PER_SECOND = 1.5f;
    static final float ENGINE_FADE_SECONDS = 0.5f;
    static final float MOVING_SPEED = 5f;
    private static final int WAYPOINT_ATTEMPTS = 8;

    private final CoreVolume coreVolume;
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

    FleetFlight(CoreVolume coreVolume, float travelSpeed, Random random) {
        this.coreVolume = coreVolume;
        this.travelSpeed = travelSpeed;
        this.random = random;
        coreVolume.randomPoint(random, point);
        positionX = point[0];
        positionY = point[1];
        facingDeg = random.nextFloat() * 360f;
        pickWaypoint();
    }

    void advance(float amount) {
        if (idleSecondsLeft > 0f) {
            idleSecondsLeft -= amount;
            float decay = Math.max(0f, 1f - IDLE_SPEED_DECAY_PER_SECOND * amount);
            velocityX *= decay;
            velocityY *= decay;
        } else {
            steerTowardWaypoint(amount);
        }
        positionX += velocityX * amount;
        positionY += velocityY * amount;
        float speed = (float) Math.hypot(velocityX, velocityY);
        if (speed > MOVING_SPEED) {
            facingDeg = (float) Math.toDegrees(Math.atan2(-velocityY, velocityX));
        }
        boolean thrusting = idleSecondsLeft <= 0f && speed > MOVING_SPEED;
        float engineStep = amount / ENGINE_FADE_SECONDS;
        engineLevel = Math.max(0f, Math.min(1f, engineLevel + (thrusting ? engineStep : -engineStep)));
        if (idleSecondsLeft <= 0f && Math.hypot(waypointX - positionX, waypointY - positionY) < ARRIVAL_DISTANCE) {
            if (random.nextFloat() < IDLE_CHANCE) {
                idleSecondsLeft = MIN_IDLE_SECONDS + random.nextFloat() * (MAX_IDLE_SECONDS - MIN_IDLE_SECONDS);
            }
            pickWaypoint();
        }
    }

    private void steerTowardWaypoint(float amount) {
        float towardX = waypointX - positionX;
        float towardY = waypointY - positionY;
        float distance = (float) Math.hypot(towardX, towardY);
        float desiredX = distance > 0f ? towardX / distance * travelSpeed : 0f;
        float desiredY = distance > 0f ? towardY / distance * travelSpeed : 0f;
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

    private void pickWaypoint() {
        for (int attempt = 0; attempt < WAYPOINT_ATTEMPTS; attempt++) {
            coreVolume.randomPoint(random, point);
            if (coreVolume.segmentClearsStar(positionX, positionY, point[0], point[1])) {
                break;
            }
        }
        waypointX = point[0];
        waypointY = point[1];
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
