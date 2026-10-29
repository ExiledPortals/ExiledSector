package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;
import java.util.function.BooleanSupplier;

final class CatalystTile extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float SELECTED_BORDER_WIDTH = 2.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;
    private static final float SELECTED_GLOW_ALPHA = 0.7f;
    private static final SpriteCache ICONS = new SpriteCache(CatalystTile.class);

    private final String iconPath;
    private final CargoIconLook iconLook;
    private final BooleanSupplier selected;
    private final TileInput tileInput;
    private PositionAPI tilePosition;

    CatalystTile(String iconPath, CargoIconLook iconLook, BooleanSupplier selected, TileInput.Handler tileHandler) {
        this.iconPath = iconPath;
        this.iconLook = iconLook;
        this.selected = selected;
        this.tileInput = new TileInput(tileHandler, true);
    }

    @Override
    public void positionChanged(PositionAPI tilePosition) {
        this.tilePosition = tilePosition;
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
        boolean chosen = selected.getAsBoolean();
        boolean hovered = tileInput.isHovered();
        Color accent = chosen ? iconLook.color() : SkillTreePanelStyle.GLOW_COLOR;
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (chosen) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, SELECTED_GLOW_ALPHA * alphaMult);
        } else if (hovered) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, HOVER_GLOW_ALPHA * alphaMult);
        }
        GLDraw.strokeQuad(x, y, width, height, accent, chosen ? SELECTED_BORDER_WIDTH : BORDER_WIDTH,
                (hovered || chosen ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (tilePosition == null) {
            return;
        }
        CargoIconFx.render(ICONS, iconPath, iconLook, tilePosition.getX(), tilePosition.getY(), tilePosition.getWidth(), tilePosition.getHeight(),
                alphaMult, tileInput.isHovered() ? 1f : 0f, System.identityHashCode(this), -1f);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, tilePosition);
    }
}
