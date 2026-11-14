package exiledsector.ui.cargo;

public final class IconFxClock {

    private static final long START_NANOS = System.nanoTime();

    private IconFxClock() {
    }

    public static float seconds() {
        return (System.nanoTime() - START_NANOS) / 1_000_000_000f;
    }

    public static float phasedSeconds(int phaseSeed, float period) {
        return seconds() + CargoIconFx.hash(phaseSeed, 1, 1) * period;
    }
}
