package exiledsector.ui.decoration;

import java.util.Random;

final class ErraticOrbit {

    static final float MIN_TRACK_SHARE = 1.3f;
    static final float MAX_TRACK_SHARE = 1.6f;
    static final float MIN_WOBBLE_SHARE = 0.1f;
    static final float MAX_WOBBLE_SHARE = 0.16f;
    static final float MAX_RIPPLE_SHARE = 0.06f;
    static final float MIN_RADIANS_PER_SECOND = 0.3f;
    static final float MAX_RADIANS_PER_SECOND = 0.55f;
    static final float MAX_SURGE_SHARE = 0.6f;
    private static final float FULL_TURN = (float) (Math.PI * 2.0);

    private final float trackRadius;
    private final Wave wobble;
    private final Wave ripple;
    private final float radiansPerSecond;
    private final Wave surge;
    private final float startAngle;
    private final int direction;

    private record Wave(float share, float rate, float phase) {

        float at(float seconds) {
            return share * (float) Math.sin(rate * seconds + phase);
        }

        float slopeAt(float seconds) {
            return share * rate * (float) Math.cos(rate * seconds + phase);
        }

        float integralAt(float seconds) {
            return share / rate * ((float) Math.sin(rate * seconds + phase) - (float) Math.sin(phase));
        }
    }

    private ErraticOrbit(float trackRadius, Wave wobble, Wave ripple, float radiansPerSecond, Wave surge, float startAngle, int direction) {
        this.trackRadius = trackRadius;
        this.wobble = wobble;
        this.ripple = ripple;
        this.radiansPerSecond = radiansPerSecond;
        this.surge = surge;
        this.startAngle = startAngle;
        this.direction = direction;
    }

    static ErraticOrbit around(float planetRadius, Random random, boolean clockwise) {
        float trackRadius = planetRadius * between(random, MIN_TRACK_SHARE, MAX_TRACK_SHARE);
        Wave wobble = new Wave(between(random, MIN_WOBBLE_SHARE, MAX_WOBBLE_SHARE), between(random, 0.3f, 0.7f), random.nextFloat() * FULL_TURN);
        Wave ripple = new Wave(between(random, 0.03f, MAX_RIPPLE_SHARE), between(random, 1f, 1.8f), random.nextFloat() * FULL_TURN);
        Wave surge = new Wave(between(random, 0.3f, MAX_SURGE_SHARE), between(random, 0.5f, 1.2f), random.nextFloat() * FULL_TURN);
        return new ErraticOrbit(trackRadius, wobble, ripple, between(random, MIN_RADIANS_PER_SECOND, MAX_RADIANS_PER_SECOND), surge,
                random.nextFloat() * FULL_TURN, clockwise ? -1 : 1);
    }

    private static float between(Random random, float low, float high) {
        return low + (high - low) * random.nextFloat();
    }

    float radiusAt(float seconds) {
        return trackRadius * (1f + wobble.at(seconds) + ripple.at(seconds));
    }

    float angleAt(float seconds) {
        return startAngle + direction * radiansPerSecond * (seconds + surge.integralAt(seconds));
    }

    float angularSpeedAt(float seconds) {
        return direction * radiansPerSecond * (1f + surge.slopeAt(seconds) / surge.rate());
    }

    float xAt(float seconds) {
        return radiusAt(seconds) * (float) Math.cos(angleAt(seconds));
    }

    float yAt(float seconds) {
        return radiusAt(seconds) * (float) Math.sin(angleAt(seconds));
    }

    float headingDegreesAt(float seconds) {
        float angle = angleAt(seconds);
        float radius = radiusAt(seconds);
        float outward = trackRadius * (wobble.slopeAt(seconds) + ripple.slopeAt(seconds));
        float around = radius * angularSpeedAt(seconds);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return (float) Math.toDegrees(Math.atan2(outward * sin + around * cos, outward * cos - around * sin));
    }

    float minRadius() {
        return trackRadius * (1f - wobble.share() - ripple.share());
    }

    float maxRadius() {
        return trackRadius * (1f + wobble.share() + ripple.share());
    }

    boolean clockwise() {
        return direction < 0;
    }
}
