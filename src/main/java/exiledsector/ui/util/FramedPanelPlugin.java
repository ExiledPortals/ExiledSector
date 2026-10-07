package exiledsector.ui.util;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.function.Consumer;

public final class FramedPanelPlugin extends BaseCustomUIPanelPlugin {

    private final BorderedPanel panelFrame;
    private final Consumer<Object> buttonListener;
    private PositionAPI panelPosition;

    public FramedPanelPlugin(Class<?> owner) {
        this(owner, id -> { });
    }

    public FramedPanelPlugin(Class<?> owner, Consumer<Object> buttonListener) {
        this.panelFrame = new BorderedPanel(owner);
        this.buttonListener = buttonListener;
    }

    @Override
    public void positionChanged(PositionAPI panelPosition) {
        this.panelPosition = panelPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panelPosition != null) {
            panelFrame.draw(panelPosition.getX(), panelPosition.getY(), panelPosition.getWidth(), panelPosition.getHeight(), alphaMult);
        }
    }

    @Override
    public void buttonPressed(Object id) {
        buttonListener.accept(id);
    }
}
