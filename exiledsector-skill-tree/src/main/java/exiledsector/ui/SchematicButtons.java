package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

final class SchematicButtons {

    private final List<SchematicButton> buttons;

    SchematicButtons(SchematicButton... buttons) {
        this.buttons = List.of(buttons);
    }

    void layout(ScreenRect besideButton, boolean shown, float mouseX, float mouseY) {
        ScreenRect previous = besideButton;
        for (SchematicButton button : buttons) {
            previous = button.layout(previous, shown, mouseX, mouseY);
        }
    }

    boolean contains(float x, float y) {
        return hovered(x, y) != null;
    }

    SchematicButton hovered(float x, float y) {
        for (SchematicButton button : buttons) {
            if (button.contains(x, y)) {
                return button;
            }
        }
        return null;
    }

    boolean press(float x, float y) {
        for (SchematicButton button : buttons) {
            if (button.press(x, y)) {
                return true;
            }
        }
        return false;
    }

    void renderButtons(float mouseX, float mouseY, float alphaMult, boolean live) {
        for (SchematicButton button : buttons) {
            button.renderButton(mouseX, mouseY, alphaMult, live);
        }
    }

    boolean isDialogBlocking() {
        for (SchematicButton button : buttons) {
            if (button.isDialogBlocking()) {
                return true;
            }
        }
        return false;
    }

    void advance(float amount) {
        for (SchematicButton button : buttons) {
            button.advance(amount);
        }
    }

    void handleModalEvent(InputEventAPI event) {
        for (SchematicButton button : buttons) {
            button.handleModalEvent(event);
        }
    }

    void renderDialogs(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        for (SchematicButton button : buttons) {
            button.renderDialog(canvasPosition, mouseX, mouseY, alphaMult);
        }
    }
}
