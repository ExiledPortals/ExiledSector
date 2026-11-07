package exiledsector.skills.skilleffect;

public final class Taper {

    private float durationSeconds;
    private float secondsLeft;

    public void start(float durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.secondsLeft = durationSeconds;
    }

    public void stop() {
        secondsLeft = 0f;
    }

    public void advance(float amount) {
        secondsLeft = Math.max(0f, secondsLeft - amount);
    }

    public boolean isRunning() {
        return secondsLeft > 0f;
    }

    public float share() {
        return durationSeconds <= 0f ? 0f : Math.max(0f, Math.min(1f, secondsLeft / durationSeconds));
    }
}
