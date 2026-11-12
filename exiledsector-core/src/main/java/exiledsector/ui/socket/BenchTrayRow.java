package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.CargoIconLook;

import java.util.List;
import java.util.function.BooleanSupplier;

final class BenchTrayRow extends BaseCustomUIPanelPlugin {

    record Layout(float iconSize, float iconInset, float iconGap) {
    }

    private static final float DULL_ICON_ALPHA = 0.3f;
    private static final SpriteCache ICONS = new SpriteCache(BenchTrayRow.class);

    private final List<String> iconPaths;
    private final List<CargoIconLook> iconLooks;
    private final boolean available;
    private final BooleanSupplier lit;
    private final TileChrome.Frame litFrame;
    private final Layout layout;
    private final TileInput rowInput;
    private PositionAPI rowPosition;

    BenchTrayRow(List<BenchTrayEntry.RowIcon> icons, boolean available, BooleanSupplier lit, Layout layout, TileInput.Handler rowHandler) {
        this.iconPaths = icons.stream().map(BenchTrayEntry.RowIcon::iconPath).toList();
        this.iconLooks = icons.stream().map(icon -> icon.look() == null ? null : icon.look().unframed()).toList();
        this.available = available;
        this.lit = lit;
        this.litFrame = CraftingBenchTile.onFrame(iconLooks.isEmpty() ? null : iconLooks.get(0), CraftingBenchTile.TOGGLE_STYLE);
        this.layout = layout;
        this.rowInput = new TileInput(rowHandler, true);
    }

    @Override
    public void positionChanged(PositionAPI rowPosition) {
        this.rowPosition = rowPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (rowPosition == null) {
            return;
        }
        TileChrome.Frame idleFrame = available ? CraftingBenchTile.IDLE_FRAME : CraftingBenchTile.DULL_FRAME;
        TileChrome.Frame frame = lit.getAsBoolean() ? litFrame : rowInput.isHovered() ? CraftingBenchTile.HOVER_FRAME : idleFrame;
        TileChrome.draw(rowPosition, frame, alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (rowPosition == null) {
            return;
        }
        float iconSize = layout.iconSize();
        float iconCentreX = rowPosition.getX() + layout.iconInset() + iconSize / 2f;
        float topCentreY = rowPosition.getCenterY() + stackHeight(iconPaths.size(), iconSize, layout.iconGap()) / 2f - iconSize / 2f;
        float hoverShare = rowInput.isHovered() ? 1f : 0f;
        for (int i = 0; i < iconPaths.size(); i++) {
            float iconCentreY = topCentreY - i * (iconSize + layout.iconGap());
            if (available) {
                TileChrome.drawIcon(ICONS, iconPaths.get(i), iconLooks.get(i), iconCentreX, iconCentreY, iconSize, alphaMult, hoverShare,
                        System.identityHashCode(this) + i);
            } else {
                TileChrome.drawIcon(ICONS, iconPaths.get(i), null, iconCentreX, iconCentreY, iconSize, alphaMult * DULL_ICON_ALPHA, 0f, 0);
            }
        }
    }

    static float stackHeight(int iconCount, float iconSize, float iconGap) {
        return iconCount * iconSize + Math.max(0, iconCount - 1) * iconGap;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        rowInput.process(events, rowPosition);
    }
}
