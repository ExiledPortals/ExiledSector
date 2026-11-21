package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Nebula;

final class NebulaWeather {

    enum Drift { SWIRL, TRAIL, SETTLE }

    record Style(int puffs, float backPuffHalfSize, float frontPuffHalfSize, Drift drift, float driftDegreesPerSecond, float cycleSeconds,
                 int flashStreams, float flashSlotSeconds, float flashChanceMult, float flashShareOfSlot) {
    }

    static final class PuffPlace {
        float offsetX;
        float offsetY;
        float halfSize;
        float turnDegrees;
        float presence;
        int cell;
    }

    static final int SHEET_CELLS = 16;
    static final float TRAIL_LENGTH = 0.9f;
    static final float TRAIL_BEND = 0.4f;
    static final int MIN_SPIKES = 2;
    static final int EXTRA_SPIKES = 2;
    static final float SPIKE_SHARE = 0.32f;
    static final float LATEST_SPIKE_START = 0.65f;
    private static final float HALF_SIZE_JITTER = 0.04f;
    private static final float SWIRL_INNER_SHARE = 0.15f;
    private static final float SWIRL_BREATH = 0.12f;
    private static final float SWIRL_BREATH_RATE = 0.3f;
    private static final float TURN_DEGREES_PER_SECOND = 4f;
    private static final float SETTLE_SPIN_SHARE = 0.4f;
    private static final int CELL_SALT = 11;
    private static final int ANGLE_SALT = 12;
    private static final int REACH_SALT = 13;
    private static final int JITTER_SALT = 14;
    private static final int TURN_SALT = 15;
    private static final int SPIKE_COUNT_SALT = 16;
    private static final int SPIKE_START_SALT = 17;
    private static final int SPIKE_PEAK_SALT = 40;
    private static final int STREAM_SALT = 0x3c91a5;

    static final Style PUFF = new Style(5, 0.62f, 0.42f, Drift.SWIRL, 6f, 0f, 1, 1.7f, 0.9f, 0.4f);
    static final Style TRAILS = new Style(9, 0.4f, 0.28f, Drift.TRAIL, 3f, 18f, 2, 1.9f, 0.8f, 0.4f);
    static final Style SETTLING = new Style(12, 0.5f, 0.3f, Drift.SETTLE, 1.5f, 40f, 2, 3.2f, 0.7f, 0.5f);
    static final Style IONIZED = new Style(10, 0.48f, 0.3f, Drift.SWIRL, 5f, 0f, 2, 1.3f, 1.2f, 0.4f);

    private NebulaWeather() {
    }

    static Style styleOf(Nebula nebula) {
        return switch (nebula) {
            case TRAILS -> TRAILS;
            case SETTLING -> SETTLING;
            case IONIZED -> IONIZED;
            case NONE, PUFF -> PUFF;
        };
    }

    static void place(Style style, int seed, int puff, float cloudSeconds, PuffPlace place) {
        float depth = style.puffs() <= 1 ? 0f : puff / (float) (style.puffs() - 1);
        float halfSize = style.backPuffHalfSize() + (style.frontPuffHalfSize() - style.backPuffHalfSize()) * depth
                + HALF_SIZE_JITTER * StormWeather.signedUnit(seed, puff, JITTER_SALT);
        place.halfSize = Math.max(0.05f, Math.min(0.95f, halfSize));
        place.cell = (int) (StormWeather.unit(seed, puff, CELL_SALT) * SHEET_CELLS);
        float spinSign = puff % 2 == 0 ? 1f : -1f;
        place.turnDegrees = StormWeather.unit(seed, puff, TURN_SALT) * 360f + spinSign * cloudSeconds * TURN_DEGREES_PER_SECOND;
        float room = 1f - place.halfSize;
        switch (style.drift()) {
            case TRAIL -> placeOnTrail(style, seed, puff, cloudSeconds, room, place);
            case SETTLE -> placeSettling(style, seed, puff, cloudSeconds, room, place);
            case SWIRL -> placeSwirling(style, seed, puff, cloudSeconds, room, place);
        }
    }

    private static void placeSwirling(Style style, int seed, int puff, float cloudSeconds, float room, PuffPlace place) {
        float spinSign = puff % 2 == 0 ? 1f : -1f;
        float share = SWIRL_INNER_SHARE + (1f - SWIRL_INNER_SHARE - SWIRL_BREATH) * StormWeather.unit(seed, puff, REACH_SALT);
        float breath = SWIRL_BREATH * (0.5f + 0.5f * (float) Math.sin(cloudSeconds * SWIRL_BREATH_RATE + puff * 2.1f + seed));
        double angle = Math.toRadians(StormWeather.unit(seed, puff, ANGLE_SALT) * 360f + spinSign * cloudSeconds * style.driftDegreesPerSecond());
        setOffset(place, angle, room * (share + breath));
        place.presence = 1f;
    }

    private static void placeOnTrail(Style style, int seed, int puff, float cloudSeconds, float room, PuffPlace place) {
        float travel = cycleShare(style, seed, puff, cloudSeconds);
        float along = travel * 2f - 1f;
        float localX = along * TRAIL_LENGTH * room;
        float localY = TRAIL_BEND * (1f - along * along) * room;
        double trailTurn = Math.toRadians(seed * 3f + cloudSeconds * style.driftDegreesPerSecond());
        float cos = (float) Math.cos(trailTurn);
        float sin = (float) Math.sin(trailTurn);
        place.offsetX = localX * cos - localY * sin;
        place.offsetY = localX * sin + localY * cos;
        place.presence = (float) Math.sin(Math.PI * travel);
    }

    private static void placeSettling(Style style, int seed, int puff, float cloudSeconds, float room, PuffPlace place) {
        float settled = cycleShare(style, seed, puff, cloudSeconds);
        double angle = Math.toRadians(StormWeather.unit(seed, puff, ANGLE_SALT) * 360f
                + cloudSeconds * style.driftDegreesPerSecond() * (1f + SETTLE_SPIN_SHARE * settled));
        setOffset(place, angle, room * (1f - settled));
        place.presence = (float) Math.sin(Math.PI * settled);
    }

    private static float cycleShare(Style style, int seed, int puff, float cloudSeconds) {
        float position = cloudSeconds / Math.max(style.cycleSeconds(), 0.01f) + puff / (float) style.puffs()
                + StormWeather.unit(seed, 0, REACH_SALT) / style.puffs();
        return position - (float) Math.floor(position);
    }

    private static void setOffset(PuffPlace place, double angle, float reach) {
        place.offsetX = (float) Math.cos(angle) * reach;
        place.offsetY = (float) Math.sin(angle) * reach;
    }

    static int streamSeed(int seed, int stream) {
        return seed + stream * STREAM_SALT;
    }

    static float flashChance(Style style, float lightingChance) {
        return Math.min(1f, lightingChance * style.flashChanceMult());
    }

    static float flashProgress(Style style, int streamSeed, float realSeconds, float chance) {
        return StormWeather.burstProgress(streamSeed, realSeconds, style.flashSlotSeconds(), chance, style.flashShareOfSlot());
    }

    static int flashSlot(Style style, int streamSeed, float realSeconds) {
        return StormWeather.slotOf(streamSeed, realSeconds, style.flashSlotSeconds());
    }

    static float flicker(int streamSeed, int slot, float progress) {
        if (progress < 0f || progress >= 1f) {
            return 0f;
        }
        int spikes = MIN_SPIKES + Math.min(EXTRA_SPIKES, (int) (StormWeather.unit(streamSeed, slot, SPIKE_COUNT_SALT) * (EXTRA_SPIKES + 1)));
        float brightness = 0f;
        for (int spike = 0; spike < spikes; spike++) {
            float start = spike == 0 ? 0f : StormWeather.unit(streamSeed, slot, SPIKE_START_SALT + spike) * LATEST_SPIKE_START;
            float since = (progress - start) / SPIKE_SHARE;
            if (since < 0f || since >= 1f) {
                continue;
            }
            float peak = spike == 0 ? 1f : 0.55f + 0.45f * StormWeather.unit(streamSeed, slot, SPIKE_PEAK_SALT + spike);
            float fade = 1f - since;
            brightness = Math.max(brightness, fade * fade * peak);
        }
        return brightness;
    }
}
