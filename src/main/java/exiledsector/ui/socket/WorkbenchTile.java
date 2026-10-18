package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;

import java.awt.Color;
import java.util.List;
import java.util.function.BooleanSupplier;

final class WorkbenchTile extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;
    private static final float LIT_GLOW_ALPHA = 0.6f;
    private static final float DULL_ICON_ALPHA = 0.3f;
    private static final float DULL_BORDER_ALPHA = 0.18f;
    private static final SpriteCache ICONS = new SpriteCache(WorkbenchTile.class);

    private final CyclingIcon cyclingIcon;
    private final TileInput tileInput;
    private final CargoIconLook iconLook;
    private final BooleanSupplier lit;
    private PositionAPI tilePosition;

    WorkbenchTile(List<String> iconPaths, CargoIconLook iconLook, TileInput.Handler tileHandler) {
        this(iconPaths, iconLook, null, tileHandler);
    }

    WorkbenchTile(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier lit, TileInput.Handler tileHandler) {
        this.cyclingIcon = new CyclingIcon(iconPaths);
        this.iconLook = iconLook == null ? null : iconLook.unframed();
        this.lit = lit;
        this.tileInput = new TileInput(tileHandler, false);
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

    private boolean isLit() {
        return lit == null || lit.getAsBoolean();
    }

    private boolean isToggle() {
        return lit != null;
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
        boolean toggledOn = isToggle() && isLit();
        Color accent = toggledOn && iconLook != null ? iconLook.color() : SkillTreePanelStyle.GLOW_COLOR;
        boolean hovered = tileInput.isHovered();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (toggledOn) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, LIT_GLOW_ALPHA * alphaMult);
        } else if (hovered) {
            GLDraw.innerGlow(x, y, width, height, GLOW_WIDTH, accent, HOVER_GLOW_ALPHA * alphaMult);
        }
        float idleAlpha = isLit() ? IDLE_BORDER_ALPHA : DULL_BORDER_ALPHA;
        GLDraw.strokeQuad(x, y, width, height, accent, BORDER_WIDTH, (hovered || toggledOn ? 1f : idleAlpha) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (!isLit()) {
            cyclingIcon.render(alphaMult * DULL_ICON_ALPHA);
            return;
        }
        if (iconLook == null || tilePosition == null) {
            cyclingIcon.render(alphaMult);
            return;
        }
        CargoIconFx.render(ICONS, cyclingIcon.firstIconPath(), iconLook, tilePosition.getX(), tilePosition.getY(), tilePosition.getWidth(),
                tilePosition.getHeight(), alphaMult, tileInput.isHovered() ? 1f : 0f, System.identityHashCode(this), -1f);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, tilePosition);
    }
}
