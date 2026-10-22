package exiledlib.ui.util;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class FramedPanelPlugin extends BaseCustomUIPanelPlugin {

    private final Consumer<Object> buttonListener;
    private PositionAPI panelPosition;
    private int hotkeyCode = -1;
    private BooleanSupplier hotkeyAction;

    public FramedPanelPlugin() {
        this(id -> { });
    }

    public FramedPanelPlugin(Consumer<Object> buttonListener) {
        this.buttonListener = buttonListener;
    }

    @Override
    public void positionChanged(PositionAPI panelPosition) {
        this.panelPosition = panelPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition != null) {
            BorderedPanel.draw(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), alphaMult);
        }
    }

    public FramedPanelPlugin onKeyDown(int keyCode, BooleanSupplier action) {
        hotkeyCode = keyCode;
        hotkeyAction = action;
        return this;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (hotkeyAction == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isKeyDownEvent() && event.getEventValue() == hotkeyCode && hotkeyAction.getAsBoolean()) {
                event.consume();
            }
        }
    }

    @Override
    public void buttonPressed(Object id) {
        buttonListener.accept(id);
    }
}
