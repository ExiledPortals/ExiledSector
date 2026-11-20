package exiledsector.ui.decoration;

import java.util.Random;

record CommandPulse(float intervalSeconds, float sweepSeconds, float phaseSeconds, int rings, float reach, float strength) {

    static final float STEADY_SWEEP_SECONDS = 2.6f;
    static final float STEADY_MIN_INTERVAL = 6f;
    static final float STEADY_EXTRA_INTERVAL = 2f;
    static final float STEADY_STRENGTH = 0.6f;
    static final float HEAVY_SWEEP_SECONDS = 4.2f;
    static final float HEAVY_MIN_INTERVAL = 10f;
    static final float HEAVY_EXTRA_INTERVAL = 2f;
    static final float HEAVY_STRENGTH = 0.85f;
    static final int HEAVY_RINGS = 2;
    static final float RING_GAP_SECONDS = 0.7f;
    static final float CLASH_SPREAD = 0.18f;

    static CommandPulse steady(float reach, Random random) {
        float interval = STEADY_MIN_INTERVAL + random.nextFloat() * STEADY_EXTRA_INTERVAL;
        return new CommandPulse(interval, STEADY_SWEEP_SECONDS, random.nextFloat() * interval, 1, reach, STEADY_STRENGTH);
    }

    static CommandPulse heavy(float reach, Random random) {
        float interval = HEAVY_MIN_INTERVAL + random.nextFloat() * HEAVY_EXTRA_INTERVAL;
        return new CommandPulse(interval, HEAVY_SWEEP_SECONDS, random.nextFloat() * interval, HEAVY_RINGS, reach, HEAVY_STRENGTH);
    }

    float ringProgressAt(int ring, float seconds) {
        float shifted = seconds + phaseSeconds - ring * RING_GAP_SECONDS;
        float sinceLast = (shifted % intervalSeconds + intervalSeconds) % intervalSeconds;
        return sinceLast < sweepSeconds ? sinceLast / sweepSeconds : -1f;
    }

    float meetingProgress(float distance) {
        return reach <= 0f ? -1f : distance / reach;
    }

    float clashAt(float seconds, float distance) {
        float progress = ringProgressAt(0, seconds);
        float meeting = meetingProgress(distance);
        if (progress < 0f || meeting < 0f || meeting > 1f) {
            return 0f;
        }
        return Math.max(0f, 1f - Math.abs(progress - meeting) / CLASH_SPREAD);
    }
}
