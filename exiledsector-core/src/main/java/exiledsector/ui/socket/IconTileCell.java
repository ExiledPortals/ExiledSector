package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.AnimatedItemIcons;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

final class IconTileCell extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final Color BLOCKED_COLOR = new Color(110, 110, 110);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float HOVER_BORDER_WIDTH = 2.5f;
    private static final float GLOW_WIDTH = 18f;
    private static final float GLOW_ALPHA = 0.55f;
    private static final float IDLE_BORDER_ALPHA = 0.6f;
    private static final float BLOCKED_ICON_ALPHA = 0.35f;
    private static final float ICON_INSET = 10f;
    private static final SpriteCache ICONS = new SpriteCache(IconTileCell.class);

    record Hooks(Runnable onClick, Consumer<PositionAPI> onHover, Runnable onLeave) {
    }

    private final String iconPath;
    private final Color accent;
    private final boolean blocked;
    private final TileInput tileInput;
    private PositionAPI cellPosition;

    IconTileCell(String iconPath, Color accent, boolean blocked, Hooks hooks) {
        this.iconPath = iconPath;
        this.accent = accent;
        this.blocked = blocked;
        this.tileInput = TileInput.forHooks(hooks, blocked);
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
        Color shownAccent = blocked ? BLOCKED_COLOR : accent;
        boolean hovered = tileInput.isHovered();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, shownAccent, GLOW_ALPHA * (hovered && !blocked ? 1.4f : 1f) * alphaMult);
        GLDraw.strokeQuad(x, y, width, height, shownAccent, hovered && !blocked ? HOVER_BORDER_WIDTH : BORDER_WIDTH,
                (hovered ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null || iconPath == null) {
            return;
        }
        float iconSize = cellPosition.getWidth() - ICON_INSET * 2f;
        AnimatedItemIcons.drawAtCenter(ICONS, iconPath, cellPosition.getCenterX(), cellPosition.getCenterY(), iconSize, iconSize,
                Color.WHITE, (blocked ? BLOCKED_ICON_ALPHA : 1f) * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, cellPosition);
    }
}
