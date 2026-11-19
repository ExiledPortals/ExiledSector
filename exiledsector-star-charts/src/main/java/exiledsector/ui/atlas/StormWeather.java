package exiledsector.ui.atlas;

import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;

final class StormWeather {

    static final float FLASH_SHARE_OF_SLOT = 0.35f;
    private static final int SLOT_OFFSET_SALT = 0x51ed27;
    private static final int UNIT_BITS = 24;
    private static final float UNIT_SCALE = 1f / (1 << UNIT_BITS);

    private StormWeather() {
    }

    static float pace(Lighting lighting) {
        return switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.55f;
            case DULL -> 0.3f;
        };
    }

    static float flashChance(Lighting lighting) {
        return switch (lighting) {
            case LIT -> 0.6f;
            case AVAILABLE -> 0.3f;
            case DULL -> 0.12f;
        };
    }

    static float cloudShare(Lighting lighting) {
        return switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.75f;
            case DULL -> 0.5f;
        };
    }

    static float flashShare(Lighting lighting) {
        return switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.65f;
            case DULL -> 0.45f;
        };
    }

    static int slotOf(int seed, float seconds, float slotSeconds) {
        return (int) Math.floor(slotPosition(seed, seconds, slotSeconds));
    }

    static float flash(int seed, float seconds, float slotSeconds, float chance) {
        float position = slotPosition(seed, seconds, slotSeconds);
        int slot = (int) Math.floor(position);
        if (unit(seed, slot, 0) >= chance) {
            return 0f;
        }
        float share = (position - slot) / FLASH_SHARE_OF_SLOT;
        if (share >= 1f) {
            return 0f;
        }
        float fade = 1f - share;
        return fade * fade;
    }

    static int pick(int seed, int slot, int count) {
        return count <= 0 ? 0 : Math.min(count - 1, (int) (unit(seed, slot, 1) * count));
    }

    static float unit(int seed, int slot, int salt) {
        return (mix(seed, slot, salt) >>> (Integer.SIZE - UNIT_BITS)) * UNIT_SCALE;
    }

    static float signedUnit(int seed, int slot, int salt) {
        return unit(seed, slot, salt) * 2f - 1f;
    }

    private static float slotPosition(int seed, float seconds, float slotSeconds) {
        return seconds / Math.max(slotSeconds, 0.01f) + unit(seed, SLOT_OFFSET_SALT, SLOT_OFFSET_SALT);
    }

    private static int mix(int seed, int slot, int salt) {
        int hash = seed * 0x9E3779B1 + slot * 0x85EBCA77 + salt * 0xC2B2AE3D;
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        hash *= 0x846CA68B;
        hash ^= hash >>> 16;
        return hash;
    }
}
