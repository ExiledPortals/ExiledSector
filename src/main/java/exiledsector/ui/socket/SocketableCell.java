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
    private static final SpriteCache ICONS = new SpriteCache(SocketableCell.class);

    public interface Listener {

        default void hovered(PositionAPI cell) {
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

        default boolean dimmed() {
            return false;
        }
    }

    public record Look(float iconInset, boolean rarityGlow, float idleBorderAlpha) {
    }

    private final Socketable socketable;
    private final Look look;
    private final Listener listener;
    private PositionAPI position;
    private boolean hovered;

    public SocketableCell(Socketable socketable, Look look, Listener listener) {
        this.socketable = socketable;
        this.look = look;
        this.listener = listener;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
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
        Color rarity = socketable.rarity().color();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (look.rarityGlow()) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, rarity, GLOW_ALPHA * alphaMult);
        }
        boolean selected = listener.selected();
        GLDraw.strokeQuad(x, y, width, height, selected ? Misc.getBrightPlayerColor() : rarity, selected ? SELECTED_BORDER_WIDTH : BORDER_WIDTH,
                (hovered || selected ? 1f : look.idleBorderAlpha()) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (position == null) {
            return;
        }
        float size = position.getWidth() - look.iconInset() * 2f;
        SpriteDraw.drawAtCenter(ICONS, socketable.iconPath(), position.getCenterX(), position.getCenterY(), size, size, Color.WHITE,
                (listener.dimmed() ? DIMMED_ICON_ALPHA : 1f) * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                hovered = false;
            }
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = position.containsEvent(event);
            if (event.isMouseMoveEvent()) {
                if (inside != hovered) {
                    hovered = inside;
                    if (inside) {
                        listener.hovered(position);
                    } else {
                        listener.left();
                    }
                }
            } else if (inside && event.isLMBDownEvent() && listener.clicked()) {
                event.consume();
            } else if (inside && event.isRMBDownEvent() && listener.rightClicked()) {
                event.consume();
            }
        }
    }
}
