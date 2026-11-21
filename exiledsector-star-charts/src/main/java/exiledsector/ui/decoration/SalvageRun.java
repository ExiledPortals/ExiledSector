package exiledsector.ui.decoration;

import java.util.Random;

final class SalvageRun implements EscortPath {

    enum Leg { OUT, WORKING, BACK, RESTING }

    static final float OUT_SECONDS = 3f;
    static final float WORK_SECONDS = 2.5f;
    static final float BACK_SECONDS = 3f;
    static final float MIN_REST_SECONDS = 1.5f;
    static final float EXTRA_REST_SECONDS = 3f;
    static final float MIN_EXTRA_REACH_SHARE = 0.2f;
    static final float EXTRA_REACH_SPREAD = 0.4f;
    static final float SPREAD_DEGREES = 50f;
    static final float DARTING_ENGINE_LEVEL = 1f;
    static final float IDLE_ENGINE_LEVEL = 0.15f;

    private final FlagshipFormation.Slot home;
    private final float fieldRadius;
    private final float cycleSeconds;
    private final float phaseSeconds;
    private final int seed;

    private SalvageRun(FlagshipFormation.Slot home, float fieldRadius, float cycleSeconds, float phaseSeconds, int seed) {
        this.home = home;
        this.fieldRadius = fieldRadius;
        this.cycleSeconds = cycleSeconds;
        this.phaseSeconds = phaseSeconds;
        this.seed = seed;
    }

    static SalvageRun from(FlagshipFormation.Slot home, float fieldRadius, Random random) {
        float cycleSeconds = OUT_SECONDS + WORK_SECONDS + BACK_SECONDS + MIN_REST_SECONDS + random.nextFloat() * EXTRA_REST_SECONDS;
        return new SalvageRun(home, fieldRadius, cycleSeconds, random.nextFloat() * cycleSeconds, random.nextInt());
    }

    float cycleSeconds() {
        return cycleSeconds;
    }

    private float shifted(float seconds) {
        return seconds + phaseSeconds;
    }

    int cycleAt(float seconds) {
        return (int) Math.floor(shifted(seconds) / cycleSeconds);
    }

    private float intoCycle(float seconds) {
        float shifted = shifted(seconds);
        return shifted - (float) Math.floor(shifted / cycleSeconds) * cycleSeconds;
    }

    Leg legAt(float seconds) {
        float into = intoCycle(seconds);
        if (into < OUT_SECONDS) {
            return Leg.OUT;
        }
        if (into < OUT_SECONDS + WORK_SECONDS) {
            return Leg.WORKING;
        }
        return into < OUT_SECONDS + WORK_SECONDS + BACK_SECONDS ? Leg.BACK : Leg.RESTING;
    }

    float shareAt(float seconds) {
        float into = intoCycle(seconds);
        return switch (legAt(seconds)) {
            case OUT -> smooth(into / OUT_SECONDS);
            case WORKING -> 1f;
            case BACK -> 1f - smooth((into - OUT_SECONDS - WORK_SECONDS) / BACK_SECONDS);
            case RESTING -> 0f;
        };
    }

    private static float smooth(float share) {
        float clamped = Math.max(0f, Math.min(1f, share));
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float targetAngleRadians(int cycle) {
        double homeAngle = home.reach() > 0f ? Math.atan2(home.lateral(), home.forward()) : 0.0;
        return (float) (homeAngle + Math.toRadians((hash(cycle, 1) * 2f - 1f) * SPREAD_DEGREES));
    }

    float targetReach(int cycle) {
        return home.reach() + fieldRadius * (MIN_EXTRA_REACH_SHARE + EXTRA_REACH_SPREAD * hash(cycle, 2));
    }

    float targetForwardAt(float seconds) {
        int cycle = cycleAt(seconds);
        return targetReach(cycle) * (float) Math.cos(targetAngleRadians(cycle));
    }

    float targetLateralAt(float seconds) {
        int cycle = cycleAt(seconds);
        return targetReach(cycle) * (float) Math.sin(targetAngleRadians(cycle));
    }

    private float hash(int cycle, int salt) {
        return ShroudedMawRenderer.hash(seed, cycle, salt + 80);
    }

    float forwardAt(float seconds) {
        return home.forward() + (targetForwardAt(seconds) - home.forward()) * shareAt(seconds);
    }

    float lateralAt(float seconds) {
        return home.lateral() + (targetLateralAt(seconds) - home.lateral()) * shareAt(seconds);
    }

    @Override
    public float xAt(float seconds, float headingDeg) {
        double heading = Math.toRadians(headingDeg);
        return forwardAt(seconds) * (float) Math.cos(heading) - lateralAt(seconds) * (float) Math.sin(heading);
    }

    @Override
    public float yAt(float seconds, float headingDeg) {
        double heading = Math.toRadians(headingDeg);
        return forwardAt(seconds) * (float) Math.sin(heading) + lateralAt(seconds) * (float) Math.cos(heading);
    }

    @Override
    public float facingAt(float seconds, float headingDeg) {
        Leg leg = legAt(seconds);
        if (leg == Leg.RESTING) {
            return headingDeg;
        }
        float outward = (float) Math.toDegrees(Math.atan2(targetLateralAt(seconds) - home.lateral(), targetForwardAt(seconds) - home.forward()));
        return headingDeg + (leg == Leg.BACK ? outward + 180f : outward);
    }

    @Override
    public float engineLevelAt(float seconds) {
        Leg leg = legAt(seconds);
        return leg == Leg.OUT || leg == Leg.BACK ? DARTING_ENGINE_LEVEL : IDLE_ENGINE_LEVEL;
    }
}
