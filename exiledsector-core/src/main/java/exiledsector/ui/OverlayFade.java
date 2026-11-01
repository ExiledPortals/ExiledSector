package exiledsector.ui;

import exiledsector.skills.skilleffect.render.Fade;

public final class OverlayFade {

    private final float openSeconds;
    private float openLevel;
    private boolean closing;
    private boolean closed;

    public OverlayFade(float openSeconds) {
        this.openSeconds = openSeconds;
    }

    public boolean advance(float amount) {
        return advance(amount, false);
    }

    public boolean advance(float amount, boolean holdOpen) {
        openLevel = Fade.step(openLevel, !closing || holdOpen, amount / openSeconds);
        if (closing && openLevel <= 0f && !closed) {
            closed = true;
            return true;
        }
        return false;
    }

    public void close() {
        closing = true;
    }

    public boolean closing() {
        return closing;
    }

    public boolean fullyOpen() {
        return !closing && openLevel >= 1f;
    }

    public float smoothLevel() {
        return Fade.smooth(openLevel);
    }
}
