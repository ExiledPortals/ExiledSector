package exiledsector.charts.anomaly.abyssstorm;

import java.util.Random;

public final class StormCycle {

    static final float MIN_CALM_SECONDS = 27f;
    static final float MAX_CALM_SECONDS = 33f;
    static final float MIN_STORM_SECONDS = 10f;
    static final float MAX_STORM_SECONDS = 16f;
    static final float RISE_PER_SECOND = 3.75f;
    static final float FADE_PER_SECOND = 0.75f;
    static final float MAX_BURST_WAIT_SECONDS = 3.3f;
    static final float MIN_BURST_PEAK = 0.75f;

    private float calmLeft;
    private float stormLeft;
    private float burstWait;
    private float burstPeak;
    private float brightness;
    private boolean rising;
    private boolean peakFrame;

    StormCycle(Random random) {
        calmLeft = between(MIN_CALM_SECONDS, MAX_CALM_SECONDS, random);
    }

    void advance(float seconds, Random random) {
        peakFrame = false;
        if (stormLeft > 0f) {
            stormLeft -= seconds;
        } else {
            calmLeft -= seconds;
            if (calmLeft <= 0f) {
                stormLeft = between(MIN_STORM_SECONDS, MAX_STORM_SECONDS, random);
                calmLeft = between(MIN_CALM_SECONDS, MAX_CALM_SECONDS, random);
                burstWait = 0f;
            }
        }
        flicker(seconds, random);
    }

    private void flicker(float seconds, Random random) {
        if (rising) {
            brightness = Math.min(burstPeak, brightness + seconds * RISE_PER_SECOND);
            rising = brightness < burstPeak;
            return;
        }
        brightness = Math.max(0f, brightness - seconds * FADE_PER_SECOND);
        if (brightness > 0f || !storming()) {
            return;
        }
        burstWait -= seconds;
        if (burstWait <= 0f) {
            rising = true;
            peakFrame = true;
            burstPeak = MIN_BURST_PEAK + (1f - MIN_BURST_PEAK) * random.nextFloat();
            burstWait = MAX_BURST_WAIT_SECONDS * random.nextFloat();
        }
    }

    boolean storming() {
        return stormLeft > 0f;
    }

    float brightness() {
        return brightness;
    }

    boolean struckThisFrame() {
        return peakFrame;
    }

    private static float between(float min, float max, Random random) {
        return min + (max - min) * random.nextFloat();
    }
}
