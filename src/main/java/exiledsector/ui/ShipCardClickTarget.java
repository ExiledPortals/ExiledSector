package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

final class ShipCardClickTarget extends BaseCustomUIPanelPlugin {

    private final Runnable onClick;
    private PositionAPI cardPosition;
    private boolean swallowMouseUp;

    ShipCardClickTarget(Runnable onClick) {
        this.onClick = onClick;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.cardPosition = position;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (cardPosition == null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (event.isLMBDownEvent() && cardPosition.containsEvent(event)) {
                swallowMouseUp = true;
                onClick.run();
                event.consume();
            } else if (event.isLMBUpEvent() && swallowMouseUp) {
                swallowMouseUp = false;
                event.consume();
            }
        }
    }
}
