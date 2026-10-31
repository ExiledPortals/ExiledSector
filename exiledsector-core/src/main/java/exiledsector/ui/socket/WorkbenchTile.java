package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.cargo.CargoIconLook;

import java.awt.Color;
import java.util.List;
import java.util.function.BooleanSupplier;

final class WorkbenchTile extends BaseCustomUIPanelPlugin {

    record Style(float onGlowAlpha, float onBorderWidth, boolean scrollClearsHover) {
    }

    static final Style TOGGLE = new Style(0.6f, 1.5f, false);
    static final Style SELECTION = new Style(0.7f, 2.5f, true);
    private static final BooleanSupplier ALWAYS = () -> true;
    private static final BooleanSupplier NEVER = () -> false;
    private static final float BORDER_WIDTH = 1.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;
    private static final float DULL_ICON_ALPHA = 0.3f;
    private static final float DULL_BORDER_ALPHA = 0.18f;
    private static final SpriteCache ICONS = new SpriteCache(WorkbenchTile.class);

    private final CyclingIcon cyclingIcon;
    private final TileInput tileInput;
    private final CargoIconLook iconLook;
    private final BooleanSupplier available;
    private final BooleanSupplier toggledOn;
    private final Style style;
    private PositionAPI tilePosition;

    private WorkbenchTile(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier available, BooleanSupplier toggledOn, Style style,
                          TileInput.Handler tileHandler) {
        this.cyclingIcon = new CyclingIcon(iconPaths);
        this.iconLook = iconLook;
        this.available = available;
        this.toggledOn = toggledOn;
        this.style = style;
        this.tileInput = new TileInput(tileHandler, style.scrollClearsHover());
    }

    static WorkbenchTile usable(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier available, TileInput.Handler tileHandler) {
        return new WorkbenchTile(iconPaths, unframed(iconLook), available, NEVER, TOGGLE, tileHandler);
    }

    static WorkbenchTile toggle(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier toggledOn, TileInput.Handler tileHandler) {
        return new WorkbenchTile(iconPaths, unframed(iconLook), toggledOn, toggledOn, TOGGLE, tileHandler);
    }

    private static CargoIconLook unframed(CargoIconLook iconLook) {
        return iconLook == null ? null : iconLook.unframed();
    }

    static WorkbenchTile selectable(String iconPath, CargoIconLook iconLook, BooleanSupplier selected, TileInput.Handler tileHandler) {
        return new WorkbenchTile(List.of(iconPath), iconLook, ALWAYS, selected, SELECTION, tileHandler);
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
        boolean on = toggledOn.getAsBoolean();
        boolean hovered = tileInput.isHovered();
        Color accent = on && iconLook != null ? iconLook.color() : SkillTreePanelStyle.GLOW_COLOR;
        float glowAlpha = on ? style.onGlowAlpha() : hovered ? HOVER_GLOW_ALPHA : 0f;
        float idleAlpha = available.getAsBoolean() ? IDLE_BORDER_ALPHA : DULL_BORDER_ALPHA;
        TileChrome.draw(tilePosition, new TileChrome.Frame(accent, GLOW_WIDTH, glowAlpha, on ? style.onBorderWidth() : BORDER_WIDTH,
                hovered || on ? 1f : idleAlpha), alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (!available.getAsBoolean()) {
            cyclingIcon.render(alphaMult * DULL_ICON_ALPHA);
            return;
        }
        if (iconLook == null || tilePosition == null) {
            cyclingIcon.render(alphaMult);
            return;
        }
        TileChrome.drawIcon(ICONS, cyclingIcon.firstIconPath(), iconLook, tilePosition, tilePosition.getWidth(), alphaMult,
                tileInput.isHovered() ? 1f : 0f, System.identityHashCode(this));
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, tilePosition);
    }
}
