package exiledsector.ui.util;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.function.Consumer;

public final class FramedPanelPlugin extends BaseCustomUIPanelPlugin {

    private final BorderedPanel frame;
    private final Consumer<Object> buttonListener;
    private PositionAPI position;

    public FramedPanelPlugin(Class<?> owner) {
        this(owner, id -> { });
    }

    public FramedPanelPlugin(Class<?> owner, Consumer<Object> buttonListener) {
        this.frame = new BorderedPanel(owner);
        this.buttonListener = buttonListener;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position != null) {
            frame.draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), alphaMult);
        }
    }

    @Override
    public void buttonPressed(Object id) {
        buttonListener.accept(id);
    }
}
