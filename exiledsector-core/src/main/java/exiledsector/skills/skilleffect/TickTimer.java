package exiledsector.skills.skilleffect;

public final class TickTimer {

    private final float intervalSeconds;
    private float secondsSinceTick;
    private float lastTickSeconds;

    public TickTimer(float intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public static TickTimer dueOnFirstAdvance(float intervalSeconds) {
        TickTimer timer = new TickTimer(intervalSeconds);
        timer.secondsSinceTick = intervalSeconds;
        return timer;
    }

    public boolean advance(float amount) {
        secondsSinceTick += amount;
        if (secondsSinceTick < intervalSeconds) {
            return false;
        }
        lastTickSeconds = secondsSinceTick;
        secondsSinceTick = 0f;
        return true;
    }

    public float elapsed() {
        return lastTickSeconds;
    }
}
