package exiledsector.ui.infestation;

final class InfestationHash {

    static final float TWO_PI = (float) (Math.PI * 2);

    private InfestationHash() {
    }

    static float unit(int index, int life, int salt) {
        int mixed = index * 0x9E3779B1 + life * 0x85EBCA77 + salt * 0xC2B2AE3D;
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        mixed *= 0x846CA68B;
        mixed ^= mixed >>> 16;
        return (mixed >>> 8) / (float) (1 << 24);
    }

    static float between(float low, float high, float share) {
        return low + (high - low) * share;
    }

    static float fraction(float value) {
        return value - (float) Math.floor(value);
    }

    static float direction(float share) {
        return share < 0.5f ? -1f : 1f;
    }

    static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    static int scaledCount(float amount, int fewest, int most) {
        return Math.max(fewest, Math.min(most, Math.round(amount)));
    }
}
