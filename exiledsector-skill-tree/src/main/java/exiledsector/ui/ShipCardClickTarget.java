package exiledsector.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.Rects;

import java.util.List;
import java.util.function.BooleanSupplier;

final class ShipCardClickTarget extends BaseCustomUIPanelPlugin {

    private static final float BORDER_WIDTH = 1.5f;
    private static final float HOVER_BORDER_WIDTH = 2f;
    private static final float GLOW_WIDTH = 14f;
    private static final float SELECTED_GLOW_ALPHA = 0.5f;
    private static final float HOVER_GLOW_ALPHA = 0.3f;
    private static final float HOVER_BORDER_ALPHA = 0.8f;

    private final Runnable clicked;
    private final BooleanSupplier live;
    private final boolean selected;
    private PositionAPI visibleArea;
    private PositionAPI targetPosition;
    private boolean hovered;
    private boolean swallowMouseUp;

    ShipCardClickTarget(Runnable clicked, BooleanSupplier live, boolean selected) {
        this.clicked = clicked;
        this.live = live;
        this.selected = selected;
    }

    void clipTo(PositionAPI visibleArea) {
        this.visibleArea = visibleArea;
    }

    @Override
    public void positionChanged(PositionAPI targetPosition) {
        this.targetPosition = targetPosition;
    }

    @Override
    public void render(float alphaMult) {
        if (targetPosition == null) {
            return;
        }
        float x = targetPosition.getX();
        float y = targetPosition.getY();
        float width = targetPosition.getWidth();
        float height = targetPosition.getHeight();
        boolean showHover = hovered && live.getAsBoolean();
        if (selected) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, Misc.getBrightPlayerColor(), SELECTED_GLOW_ALPHA * alphaMult);
            GLDraw.strokeQuad(x, y, width, height, Misc.getBrightPlayerColor(), BORDER_WIDTH, alphaMult);
        } else if (showHover) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, Misc.getBasePlayerColor(), HOVER_GLOW_ALPHA * alphaMult);
        }
        if (showHover) {
            GLDraw.strokeQuad(x, y, width, height, Misc.getBasePlayerColor(), HOVER_BORDER_WIDTH, HOVER_BORDER_ALPHA * alphaMult);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (targetPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                hovered = false;
            }
            if (event.isLMBUpEvent()) {
                if (swallowMouseUp && !event.isConsumed()) {
                    event.consume();
                }
                swallowMouseUp = false;
                continue;
            }
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = contains(event.getX(), event.getY());
            if (event.isMouseMoveEvent()) {
                hovered = inside;
            } else if (inside && event.isLMBDownEvent() && live.getAsBoolean()) {
                swallowMouseUp = true;
                event.consume();
                clicked.run();
            }
        }
    }

    private boolean contains(float x, float y) {
        return Rects.contains(targetPosition, x, y) && (visibleArea == null || Rects.contains(visibleArea, x, y));
    }
}
