package exiledsector.ui.slipstream;

import java.util.Random;

final class Wobble {

    static final float ANGLE_DEGREES_PER_SECOND = 50f;
    static final float MIN_ANGLE_TURNS = 1f;
    static final float MAX_ANGLE_TURNS = 4f;
    static final float RADIUS_RATE_SHARE = 0.5f;

    private final Drift angleDegrees;
    private final Drift radius;
    private float offsetX;
    private float offsetY;

    Wobble(float maxRadius, Random random) {
        float angleReach = 360f * (MIN_ANGLE_TURNS + random.nextFloat() * (MAX_ANGLE_TURNS - MIN_ANGLE_TURNS));
        angleDegrees = new Drift(-angleReach, angleReach, ANGLE_DEGREES_PER_SECOND, random);
        radius = new Drift(0f, maxRadius, maxRadius * RADIUS_RATE_SHARE, random);
        advance(0f);
    }

    void advance(float amount) {
        angleDegrees.advance(amount);
        radius.advance(amount);
        double angle = Math.toRadians(angleDegrees.value);
        offsetX = (float) Math.cos(angle) * radius.value;
        offsetY = (float) Math.sin(angle) * radius.value;
    }

    float offsetX() {
        return offsetX;
    }

    float offsetY() {
        return offsetY;
    }

    private static final class Drift {

        private final float minimum;
        private final float maximum;
        private final float rate;
        private float value;
        private float direction;

        Drift(float minimum, float maximum, float rate, Random random) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.rate = rate;
            value = minimum + random.nextFloat() * (maximum - minimum);
            direction = random.nextBoolean() ? 1f : -1f;
        }

        void advance(float amount) {
            value += direction * rate * amount;
            if (value > maximum) {
                value = maximum;
                direction = -1f;
            } else if (value < minimum) {
                value = minimum;
                direction = 1f;
            }
        }
    }
}
