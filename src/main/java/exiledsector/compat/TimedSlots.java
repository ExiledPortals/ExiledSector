package exiledsector.compat;

final class TimedSlots {

    private final float[] freeAtSeconds;

    TimedSlots(int slotCount) {
        freeAtSeconds = new float[slotCount];
    }

    boolean claim(float nowSeconds, float durationSeconds) {
        for (int slot = 0; slot < freeAtSeconds.length; slot++) {
            if (freeAtSeconds[slot] <= nowSeconds) {
                freeAtSeconds[slot] = nowSeconds + Math.max(0f, durationSeconds);
                return true;
            }
        }
        return false;
    }
}
