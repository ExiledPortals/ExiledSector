package exiledsector.ui.exoship;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ExoshipVoyage {

    public enum Phase { ORBITING, TURNING, BURNING, WARPING, ARRIVING }

    public interface Events {

        void chargingUp(float shipX, float shipY);

        void departed(float shipX, float shipY, float headingX, float headingY);

        void arrived(float shipX, float shipY, float headingX, float headingY);
    }

    public record Anchor(float x, float y, boolean core) {
    }

    static final float JUMP_INTERVAL_SECONDS = 30f;
    static final float ORBIT_RADIUS = 120f;
    static final float ORBIT_SPEED = 28f;
    static final float ORBIT_SPEED_RAMP_SECONDS = 3f;
    static final float TURN_DEGREES_PER_SECOND = 70f;
    static final float MAX_TURN_SECONDS = 3f;
    static final float BURN_SECONDS = 2.5f;
    static final float BURN_ACCELERATION = 260f;
    static final float WARP_SPEED = 30000f;
    static final float MIN_WARP_SECONDS = 0.35f;
    static final float MAX_WARP_SECONDS = 1f;
    static final float ARRIVAL_SECONDS = 3f;
    static final float APPROACH_DISTANCE = 900f;
    static final float MIN_JUMP_DISTANCE = 2500f;
    static final float ESCORT_LAUNCH_DELAY_SECONDS = 0.5f;
    private static final float ALIGNED_DEGREES = 1f;

    private final List<Anchor> anchors;
    private final Random random;
    private final Events events;
    private Phase phase = Phase.ORBITING;
    private float phaseSeconds;
    private float secondsUntilJump;
    private int anchorIndex;
    private int destinationIndex;
    private float orbitAngleRad;
    private float orbitSpeedLevel = 1f;
    private float shipX;
    private float shipY;
    private float facingDeg;
    private float headingX;
    private float headingY;
    private float legStartX;
    private float legStartY;
    private float legEndX;
    private float legEndY;
    private float legLength;
    private float warpSeconds;

    public ExoshipVoyage(List<Anchor> anchors, Random random, Events events) {
        this.anchors = List.copyOf(anchors);
        this.random = random;
        this.events = events;
        if (this.anchors.isEmpty()) {
            return;
        }
        anchorIndex = random.nextInt(this.anchors.size());
        orbitAngleRad = random.nextFloat() * (float) (Math.PI * 2);
        secondsUntilJump = JUMP_INTERVAL_SECONDS * random.nextFloat();
        placeOnOrbit();
        facingDeg = tangentDeg();
    }

    public boolean hasRoute() {
        return !anchors.isEmpty();
    }

    public void advance(float amount) {
        if (anchors.isEmpty()) {
            return;
        }
        phaseSeconds += amount;
        switch (phase) {
            case ORBITING -> orbit(amount);
            case TURNING -> turn(amount);
            case BURNING -> burn();
            case WARPING -> warp();
            case ARRIVING -> arrive();
        }
    }

    private void orbit(float amount) {
        circle(amount);
        facingDeg = turnToward(facingDeg, tangentDeg(), TURN_DEGREES_PER_SECOND * amount);
        secondsUntilJump -= amount;
        if (secondsUntilJump <= 0f && anchors.size() > 1) {
            destinationIndex = pickDestination();
            enter(Phase.TURNING);
        }
    }

    private void turn(float amount) {
        circle(amount);
        Anchor destination = anchors.get(destinationIndex);
        float targetDeg = degreesOf(destination.x() - shipX, destination.y() - shipY);
        facingDeg = turnToward(facingDeg, targetDeg, TURN_DEGREES_PER_SECOND * amount);
        if (Math.abs(angleBetween(facingDeg, targetDeg)) <= ALIGNED_DEGREES || phaseSeconds >= MAX_TURN_SECONDS) {
            double facingRad = Math.toRadians(facingDeg);
            headingX = (float) Math.cos(facingRad);
            headingY = (float) Math.sin(facingRad);
            legStartX = shipX;
            legStartY = shipY;
            enter(Phase.BURNING);
            events.chargingUp(shipX, shipY);
        }
    }

    private void burn() {
        float seconds = Math.min(phaseSeconds, BURN_SECONDS);
        float distance = ORBIT_SPEED * seconds + 0.5f * BURN_ACCELERATION * seconds * seconds;
        shipX = legStartX + headingX * distance;
        shipY = legStartY + headingY * distance;
        if (phaseSeconds >= BURN_SECONDS) {
            depart();
        }
    }

    private void depart() {
        Anchor destination = anchors.get(destinationIndex);
        float towardX = destination.x() - shipX;
        float towardY = destination.y() - shipY;
        float towardLength = (float) Math.hypot(towardX, towardY);
        if (towardLength > 0f) {
            headingX = towardX / towardLength;
            headingY = towardY / towardLength;
        }
        float insertionX = destination.x() - headingX * ORBIT_RADIUS;
        float insertionY = destination.y() - headingY * ORBIT_RADIUS;
        float approach = Math.min(APPROACH_DISTANCE, Math.max(0f, (insertionX - shipX) * headingX + (insertionY - shipY) * headingY));
        legStartX = shipX;
        legStartY = shipY;
        legEndX = insertionX - headingX * approach;
        legEndY = insertionY - headingY * approach;
        float warpDistance = (float) Math.hypot(legEndX - legStartX, legEndY - legStartY);
        warpSeconds = Math.max(MIN_WARP_SECONDS, Math.min(MAX_WARP_SECONDS, warpDistance / WARP_SPEED));
        legLength = approach;
        facingDeg = degreesOf(headingX, headingY);
        enter(Phase.WARPING);
        events.departed(shipX, shipY, headingX, headingY);
    }

    private void warp() {
        float progress = Math.min(1f, phaseSeconds / warpSeconds);
        shipX = legStartX + (legEndX - legStartX) * progress;
        shipY = legStartY + (legEndY - legStartY) * progress;
        if (progress >= 1f) {
            legStartX = legEndX;
            legStartY = legEndY;
            enter(Phase.ARRIVING);
            events.arrived(shipX, shipY, headingX, headingY);
        }
    }

    private void arrive() {
        float progress = Math.min(1f, phaseSeconds / ARRIVAL_SECONDS);
        float remaining = 1f - progress;
        float travelled = legLength * (1f - remaining * remaining);
        shipX = legStartX + headingX * travelled;
        shipY = legStartY + headingY * travelled;
        if (progress >= 1f) {
            anchorIndex = destinationIndex;
            Anchor anchor = anchors.get(anchorIndex);
            orbitAngleRad = (float) Math.atan2(shipY - anchor.y(), shipX - anchor.x());
            orbitSpeedLevel = 0f;
            secondsUntilJump = JUMP_INTERVAL_SECONDS;
            enter(Phase.ORBITING);
        }
    }

    private void circle(float amount) {
        orbitSpeedLevel = Math.min(1f, orbitSpeedLevel + amount / ORBIT_SPEED_RAMP_SECONDS);
        orbitAngleRad += ORBIT_SPEED * orbitSpeedLevel / ORBIT_RADIUS * amount;
        placeOnOrbit();
    }

    private void placeOnOrbit() {
        Anchor anchor = anchors.get(anchorIndex);
        shipX = anchor.x() + ORBIT_RADIUS * (float) Math.cos(orbitAngleRad);
        shipY = anchor.y() + ORBIT_RADIUS * (float) Math.sin(orbitAngleRad);
    }

    private float tangentDeg() {
        return (float) Math.toDegrees(orbitAngleRad) + 90f;
    }

    private int pickDestination() {
        Anchor current = anchors.get(anchorIndex);
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < anchors.size(); index++) {
            if (index != anchorIndex && anchors.get(index).core() != current.core()) {
                candidates.add(index);
            }
        }
        if (candidates.isEmpty()) {
            for (int index = 0; index < anchors.size(); index++) {
                if (index != anchorIndex) {
                    candidates.add(index);
                }
            }
        }
        List<Integer> farEnough = new ArrayList<>();
        int farthest = candidates.get(0);
        float farthestDistance = -1f;
        for (int index : candidates) {
            float distance = distanceBetween(current, anchors.get(index));
            if (distance >= MIN_JUMP_DISTANCE) {
                farEnough.add(index);
            }
            if (distance > farthestDistance) {
                farthestDistance = distance;
                farthest = index;
            }
        }
        return farEnough.isEmpty() ? farthest : farEnough.get(random.nextInt(farEnough.size()));
    }

    private void enter(Phase next) {
        phase = next;
        phaseSeconds = 0f;
    }

    private static float distanceBetween(Anchor from, Anchor to) {
        return (float) Math.hypot(to.x() - from.x(), to.y() - from.y());
    }

    private static float degreesOf(float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y, x));
    }

    static float angleBetween(float fromDeg, float toDeg) {
        float difference = (toDeg - fromDeg) % 360f;
        if (difference > 180f) difference -= 360f;
        if (difference < -180f) difference += 360f;
        return difference;
    }

    private static float turnToward(float fromDeg, float toDeg, float maxStep) {
        float difference = angleBetween(fromDeg, toDeg);
        if (Math.abs(difference) <= maxStep) {
            return toDeg;
        }
        return fromDeg + Math.signum(difference) * maxStep;
    }

    public Phase phase() {
        return phase;
    }

    public float shipX() {
        return shipX;
    }

    public float shipY() {
        return shipY;
    }

    public float facingDeg() {
        return facingDeg;
    }

    public boolean isVisible() {
        return phase != Phase.WARPING;
    }

    public float thrustLevel() {
        return switch (phase) {
            case ORBITING -> 0.25f;
            case TURNING -> 0.35f;
            case BURNING -> 0.4f + 0.6f * Math.min(1f, phaseSeconds / BURN_SECONDS);
            case WARPING -> 1f;
            case ARRIVING -> 0.25f;
        };
    }

    public float speedLevel() {
        return switch (phase) {
            case ORBITING, TURNING -> 0f;
            case BURNING -> Math.min(1f, phaseSeconds / BURN_SECONDS);
            case WARPING -> 1f;
            case ARRIVING -> Math.max(0f, 1f - phaseSeconds / ARRIVAL_SECONDS);
        };
    }

    public float warpLevel() {
        return phase == Phase.WARPING ? 1f : 0f;
    }

    public boolean escortsLaunched() {
        return phase == Phase.ORBITING || phase == Phase.ARRIVING && phaseSeconds >= ESCORT_LAUNCH_DELAY_SECONDS;
    }

    public Anchor currentAnchor() {
        return anchors.isEmpty() ? null : anchors.get(anchorIndex);
    }
}
