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

        void hovered(PositionAPI tilePosition);

        void left();

        void clicked();

        void rightClicked();
    }

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;

    private final CyclingIcon cyclingIcon;
    private final Listener tileListener;
    private PositionAPI tilePosition;
    private boolean hovered;

    WorkbenchTile(List<String> iconPaths, Listener tileListener) {
        this.cyclingIcon = new CyclingIcon(iconPaths);
        this.tileListener = tileListener;
    }

    @Override
    public void positionChanged(PositionAPI tilePosition) {
        this.tilePosition = tilePosition;
        cyclingIcon.positionChanged(tilePosition);
    }

    @Override
    public void advance(float amount) {
        cyclingIcon.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (tilePosition == null) {
            return;
        }
        float x = tilePosition.getX();
        float y = tilePosition.getY();
        float width = tilePosition.getWidth();
        float height = tilePosition.getHeight();
        Color accent = SkillTreePanelStyle.GLOW_COLOR;
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (hovered) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, HOVER_GLOW_ALPHA * alphaMult);
        }
        GLDraw.strokeQuad(x, y, width, height, accent, BORDER_WIDTH, (hovered ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        cyclingIcon.render(alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (tilePosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = tilePosition.containsEvent(event);
            if (event.isMouseMoveEvent() && inside != hovered) {
                hovered = inside;
                if (inside) {
                    tileListener.hovered(tilePosition);
                } else {
                    tileListener.left();
                }
            } else if (inside && event.isLMBDownEvent()) {
                event.consume();
                tileListener.clicked();
            } else if (inside && event.isRMBDownEvent()) {
                event.consume();
                tileListener.rightClicked();
            }
        }
    }
}
