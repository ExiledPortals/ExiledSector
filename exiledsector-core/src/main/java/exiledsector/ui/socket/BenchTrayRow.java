package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.CargoIconLook;

import java.util.List;

final class BenchTrayRow extends BaseCustomUIPanelPlugin {

    private static final float DULL_ICON_ALPHA = 0.3f;
    private static final SpriteCache ICONS = new SpriteCache(BenchTrayRow.class);

    private final String iconPath;
    private final CargoIconLook iconLook;
    private final boolean available;
    private final float iconSize;
    private final float iconInset;
    private final TileInput rowInput;
    private PositionAPI rowPosition;

    BenchTrayRow(List<String> iconPaths, CargoIconLook iconLook, boolean available, float iconSize, float iconInset, TileInput.Handler rowHandler) {
        this.iconPath = BenchTrayElement.shownIcons(iconPaths).get(0);
        this.iconLook = iconLook == null ? null : iconLook.unframed();
        this.available = available;
        this.iconSize = iconSize;
        this.iconInset = iconInset;
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
        TileChrome.draw(rowPosition, rowInput.isHovered() ? CraftingBenchTile.HOVER_FRAME : idleFrame, alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (rowPosition == null) {
            return;
        }
        float iconCentreX = rowPosition.getX() + iconInset + iconSize / 2f;
        float iconCentreY = rowPosition.getCenterY();
        if (!available) {
            TileChrome.drawIcon(ICONS, iconPath, null, iconCentreX, iconCentreY, iconSize, alphaMult * DULL_ICON_ALPHA, 0f, 0);
            return;
        }
        TileChrome.drawIcon(ICONS, iconPath, iconLook, iconCentreX, iconCentreY, iconSize, alphaMult, rowInput.isHovered() ? 1f : 0f,
                System.identityHashCode(this));
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        rowInput.process(events, rowPosition);
    }
}
