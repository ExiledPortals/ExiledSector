package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;

final class WorkbenchTile extends BaseCustomUIPanelPlugin {

    interface Listener {

        void hovered(PositionAPI tile);

        void left();

        void clicked();

        void rightClicked();
    }

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;

    private final CyclingIcon icon;
    private final Listener listener;
    private PositionAPI position;
    private boolean hovered;

    WorkbenchTile(List<String> iconPaths, Listener listener) {
        this.icon = new CyclingIcon(iconPaths);
        this.listener = listener;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
        icon.positionChanged(position);
    }

    @Override
    public void advance(float amount) {
        icon.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) {
            return;
        }
        float x = position.getX();
        float y = position.getY();
        float width = position.getWidth();
        float height = position.getHeight();
        Color accent = SkillTreePanelStyle.GLOW_COLOR;
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (hovered) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, HOVER_GLOW_ALPHA * alphaMult);
        }
        GLDraw.strokeQuad(x, y, width, height, accent, BORDER_WIDTH, (hovered ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        icon.render(alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = position.containsEvent(event);
            if (event.isMouseMoveEvent() && inside != hovered) {
                hovered = inside;
                if (inside) {
                    listener.hovered(position);
                } else {
                    listener.left();
                }
            } else if (inside && event.isLMBDownEvent()) {
                event.consume();
                listener.clicked();
            } else if (inside && event.isRMBDownEvent()) {
                event.consume();
                listener.rightClicked();
            }
        }
    }
}
