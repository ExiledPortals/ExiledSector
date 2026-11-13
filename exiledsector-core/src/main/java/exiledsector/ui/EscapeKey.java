package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;

public final class EscapeKey {

    private boolean pressSeen;

    public boolean released(InputEventAPI event) {
        if (event.getEventValue() != Keyboard.KEY_ESCAPE) {
            return false;
        }
        if (event.isKeyDownEvent()) {
            pressSeen = true;
            return false;
        }
        if (!event.isKeyUpEvent()) {
            return false;
        }
        boolean released = pressSeen;
        pressSeen = false;
        return released;
    }
}
