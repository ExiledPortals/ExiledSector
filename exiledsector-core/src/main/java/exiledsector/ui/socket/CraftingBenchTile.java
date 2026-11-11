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

final class CraftingBenchTile extends BaseCustomUIPanelPlugin {

    record Style(float onGlowAlpha, float onBorderWidth, boolean scrollClearsHover) {
    }

    static final Style TOGGLE_STYLE = new Style(0.6f, 1.5f, false);
    static final Style SELECTION_STYLE = new Style(0.7f, 2.5f, true);
    private static final BooleanSupplier ALWAYS = () -> true;
    private static final BooleanSupplier NEVER = () -> false;
    private static final float BORDER_WIDTH = 1.5f;
    private static final float GLOW_WIDTH = 12f;
    private static final float IDLE_BORDER_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.45f;
    private static final float DULL_ICON_ALPHA = 0.3f;
    private static final float DULL_BORDER_ALPHA = 0.18f;
    static final TileChrome.Frame HOVER_FRAME = new TileChrome.Frame(SkillTreePanelStyle.GLOW_COLOR, GLOW_WIDTH, HOVER_GLOW_ALPHA, BORDER_WIDTH, 1f);
    static final TileChrome.Frame IDLE_FRAME = new TileChrome.Frame(SkillTreePanelStyle.GLOW_COLOR, GLOW_WIDTH, 0f, BORDER_WIDTH, IDLE_BORDER_ALPHA);
    static final TileChrome.Frame DULL_FRAME = new TileChrome.Frame(SkillTreePanelStyle.GLOW_COLOR, GLOW_WIDTH, 0f, BORDER_WIDTH, DULL_BORDER_ALPHA);
    private static final SpriteCache ICONS = new SpriteCache(CraftingBenchTile.class);

    private final CyclingIcon cyclingIcon;
    private final TileInput tileInput;
    private final CargoIconLook iconLook;
    private final BooleanSupplier available;
    private final BooleanSupplier toggledOn;
    private final TileChrome.Frame onFrame;
    private PositionAPI tilePosition;

    private CraftingBenchTile(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier available, BooleanSupplier toggledOn, Style style,
                          TileInput.Handler tileHandler) {
        this.cyclingIcon = new CyclingIcon(iconPaths);
        this.iconLook = iconLook;
        this.available = available;
        this.toggledOn = toggledOn;
        Color onAccent = iconLook != null ? iconLook.color() : SkillTreePanelStyle.GLOW_COLOR;
        this.onFrame = new TileChrome.Frame(onAccent, GLOW_WIDTH, style.onGlowAlpha(), style.onBorderWidth(), 1f);
        this.tileInput = new TileInput(tileHandler, style.scrollClearsHover());
    }

    static CraftingBenchTile usable(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier available, TileInput.Handler tileHandler) {
        return new CraftingBenchTile(iconPaths, unframed(iconLook), available, NEVER, TOGGLE_STYLE, tileHandler);
    }

    static CraftingBenchTile toggle(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier toggledOn, TileInput.Handler tileHandler) {
        return new CraftingBenchTile(iconPaths, unframed(iconLook), toggledOn, toggledOn, TOGGLE_STYLE, tileHandler);
    }

    static CraftingBenchTile entry(List<String> iconPaths, CargoIconLook iconLook, BooleanSupplier available, BooleanSupplier lit,
                                   TileInput.Handler tileHandler) {
        return new CraftingBenchTile(iconPaths, unframed(iconLook), available, lit, TOGGLE_STYLE, tileHandler);
    }

    private static CargoIconLook unframed(CargoIconLook iconLook) {
        return iconLook == null ? null : iconLook.unframed();
    }

    static CraftingBenchTile selectable(String iconPath, CargoIconLook iconLook, BooleanSupplier selected, TileInput.Handler tileHandler) {
        return new CraftingBenchTile(List.of(iconPath), iconLook, ALWAYS, selected, SELECTION_STYLE, tileHandler);
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
        TileChrome.Frame idleFrame = available.getAsBoolean() ? IDLE_FRAME : DULL_FRAME;
        TileChrome.draw(tilePosition, toggledOn.getAsBoolean() ? onFrame : tileInput.isHovered() ? HOVER_FRAME : idleFrame, alphaMult);
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
