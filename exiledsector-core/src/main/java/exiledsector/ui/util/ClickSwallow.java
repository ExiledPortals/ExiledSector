package exiledsector.ui.util;

import com.fs.starfarer.api.input.InputEventAPI;

public final class ClickSwallow {

    private boolean swallowingRelease;

    public void swallowNextRelease() {
        swallowingRelease = true;
    }

    public void released(InputEventAPI mouseUp) {
        if (swallowingRelease && !mouseUp.isConsumed()) {
            mouseUp.consume();
        }
        swallowingRelease = false;
    }

    public void cancel() {
        swallowingRelease = false;
    }
}
