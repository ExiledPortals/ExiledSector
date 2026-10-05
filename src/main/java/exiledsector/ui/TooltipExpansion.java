package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.input.Keyboard;

public final class TooltipExpansion {

    public static final int HOTKEY = Keyboard.KEY_F1;

    private static boolean expanded;

    private TooltipExpansion() {
    }

    public static boolean isExpanded() {
        return expanded;
    }

    public static boolean isToggle(InputEventAPI event) {
        return event.isKeyDownEvent() && event.getEventValue() == HOTKEY;
    }

    public static void toggle() {
        expanded = !expanded;
    }
}
