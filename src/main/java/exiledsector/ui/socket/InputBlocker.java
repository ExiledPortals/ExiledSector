package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.input.Keyboard;

import java.util.List;

final class InputBlocker extends BaseCustomUIPanelPlugin {

    private PositionAPI blockerPosition;

    @Override
    public void positionChanged(PositionAPI blockerPosition) {
        this.blockerPosition = blockerPosition;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            boolean escape = event.isKeyboardEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE;
            boolean press = (event.isMouseDownEvent() || event.isMouseScrollEvent()) && blockerPosition != null
                    && blockerPosition.containsEvent(event);
            if (!event.isConsumed() && (press || (event.isKeyboardEvent() && !escape))) {
                event.consume();
            }
        }
    }
}
