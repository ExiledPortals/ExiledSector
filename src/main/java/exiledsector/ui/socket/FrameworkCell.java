package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.socketables.HullFramework;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

final class FrameworkCell extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final Color BLOCKED_COLOR = new Color(110, 110, 110);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float HOVER_BORDER_WIDTH = 2.5f;
    private static final float GLOW_WIDTH = 18f;
    private static final float GLOW_ALPHA = 0.55f;
    private static final float IDLE_BORDER_ALPHA = 0.6f;
    private static final float BLOCKED_ICON_ALPHA = 0.35f;
    private static final float ICON_INSET = 10f;
    private static final SpriteCache ICONS = new SpriteCache(FrameworkCell.class);

    private final HullFramework framework;
    private final boolean blocked;
    private final Runnable onClick;
    private final Consumer<PositionAPI> onHover;
    private final Runnable onLeave;
    private PositionAPI cellPosition;
    private boolean hovered;

    FrameworkCell(HullFramework framework, boolean blocked, Runnable onClick, Consumer<PositionAPI> onHover, Runnable onLeave) {
        this.framework = framework;
        this.blocked = blocked;
        this.onClick = onClick;
        this.onHover = onHover;
        this.onLeave = onLeave;
    }

    @Override
    public void positionChanged(PositionAPI cellPosition) {
        this.cellPosition = cellPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (cellPosition == null) {
            return;
        }
        float x = cellPosition.getX();
        float y = cellPosition.getY();
        float width = cellPosition.getWidth();
        float height = cellPosition.getHeight();
        Color accent = blocked ? BLOCKED_COLOR : framework.rarity().color();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, GLOW_ALPHA * (hovered && !blocked ? 1.4f : 1f) * alphaMult);
        GLDraw.strokeQuad(x, y, width, height, accent, hovered && !blocked ? HOVER_BORDER_WIDTH : BORDER_WIDTH,
                (hovered ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null) {
            return;
        }
        float iconSize = cellPosition.getWidth() - ICON_INSET * 2f;
        SpriteDraw.drawAtCenter(ICONS, framework.iconPath(), cellPosition.getCenterX(), cellPosition.getCenterY(), iconSize, iconSize,
                Color.WHITE, (blocked ? BLOCKED_ICON_ALPHA : 1f) * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (cellPosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                hovered = false;
            }
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = cellPosition.containsEvent(event);
            if (event.isMouseMoveEvent()) {
                if (inside != hovered) {
                    hovered = inside;
                    if (inside) {
                        onHover.accept(cellPosition);
                    } else {
                        onLeave.run();
                    }
                }
            } else if (inside && event.isLMBDownEvent()) {
                if (!blocked) {
                    onClick.run();
                }
                event.consume();
            }
        }
    }
}
