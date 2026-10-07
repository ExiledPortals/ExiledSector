package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.socketables.Socketable;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

public final class SocketableCell extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float SELECTED_BORDER_WIDTH = 3f;
    private static final float GLOW_WIDTH = 14f;
    private static final float GLOW_ALPHA = 0.6f;
    private static final float DIMMED_ICON_ALPHA = 0.35f;
    private static final float MARKED_FILL_ALPHA = 0.25f;
    private static final SpriteCache ICONS = new SpriteCache(SocketableCell.class);

    public interface Listener {

        default void hovered(PositionAPI cellPosition) {
        }

        default void left() {
        }

        default boolean clicked() {
            return false;
        }

        default boolean rightClicked() {
            return false;
        }

        default boolean selected() {
            return false;
        }

        default boolean marked() {
            return false;
        }

        default boolean dimmed() {
            return false;
        }
    }

    public record Look(float iconInset, boolean rarityGlow, float idleBorderAlpha) {
    }

    private final Socketable socketable;
    private final Look look;
    private final Listener cellListener;
    private PositionAPI cellPosition;
    private boolean hovered;

    public SocketableCell(Socketable socketable, Look look, Listener cellListener) {
        this.socketable = socketable;
        this.look = look;
        this.cellListener = cellListener;
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
        Color rarityColor = socketable.rarity().color();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (look.rarityGlow()) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, rarityColor, GLOW_ALPHA * alphaMult);
        }
        if (cellListener.marked()) {
            Color warningColor = Misc.getNegativeHighlightColor();
            GLDraw.fillQuad(x, y, width, height, warningColor, MARKED_FILL_ALPHA * alphaMult);
            GLDraw.strokeQuad(x, y, width, height, warningColor, SELECTED_BORDER_WIDTH, alphaMult);
            return;
        }
        boolean selected = cellListener.selected();
        GLDraw.strokeQuad(x, y, width, height, selected ? Misc.getBrightPlayerColor() : rarityColor,
                selected ? SELECTED_BORDER_WIDTH : BORDER_WIDTH, (hovered || selected ? 1f : look.idleBorderAlpha()) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null) {
            return;
        }
        float iconSize = cellPosition.getWidth() - look.iconInset() * 2f;
        SpriteDraw.drawAtCenter(ICONS, socketable.iconPath(), cellPosition.getCenterX(), cellPosition.getCenterY(), iconSize, iconSize,
                Color.WHITE, (cellListener.dimmed() ? DIMMED_ICON_ALPHA : 1f) * alphaMult);
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
                        cellListener.hovered(cellPosition);
                    } else {
                        cellListener.left();
                    }
                }
            } else if (inside && event.isLMBDownEvent() && cellListener.clicked()) {
                event.consume();
            } else if (inside && event.isRMBDownEvent() && cellListener.rightClicked()) {
                event.consume();
            }
        }
    }
}
