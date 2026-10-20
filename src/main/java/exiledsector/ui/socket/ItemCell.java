package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.items.Item;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.cargo.AnimatedItemIcons;
import exiledsector.ui.util.SpriteCache;

import java.awt.Color;
import java.util.List;

public final class ItemCell extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float SELECTED_BORDER_WIDTH = 3f;
    private static final float GLOW_WIDTH = 14f;
    private static final float GLOW_ALPHA = 0.6f;
    private static final float DIMMED_ICON_ALPHA = 0.35f;
    private static final float MARKED_FILL_ALPHA = 0.25f;
    private static final SpriteCache ICONS = new SpriteCache(ItemCell.class);

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

    private final Item item;
    private final Look look;
    private final Listener cellListener;
    private final CargoIconLook ownArtLook;
    private final TileInput tileInput;
    private PositionAPI cellPosition;

    public ItemCell(Item item, Look look, Listener cellListener) {
        this.item = item;
        this.look = look;
        this.cellListener = cellListener;
        CargoIconLook cargoLook = CargoIconLooks.forItem(item);
        this.ownArtLook = cargoLook != null && cargoLook.ownArt() ? cargoLook : null;
        this.tileInput = new TileInput(new TileInput.Handler() {
            @Override
            public void entered(PositionAPI enteredPosition) {
                cellListener.hovered(enteredPosition);
            }

            @Override
            public void left() {
                cellListener.left();
            }

            @Override
            public boolean clicked() {
                return cellListener.clicked();
            }

            @Override
            public boolean rightClicked() {
                return cellListener.rightClicked();
            }
        }, true);
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
        Color rarityColor = item.frameColor();
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
                selected ? SELECTED_BORDER_WIDTH : BORDER_WIDTH, (tileInput.isHovered() || selected ? 1f : look.idleBorderAlpha()) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null) {
            return;
        }
        float iconSize = cellPosition.getWidth() - look.iconInset() * 2f;
        float iconAlpha = (cellListener.dimmed() ? DIMMED_ICON_ALPHA : 1f) * alphaMult;
        if (ownArtLook != null) {
            CargoIconFx.render(ICONS, item.iconPath(), ownArtLook, cellPosition.getCenterX() - iconSize / 2f,
                    cellPosition.getCenterY() - iconSize / 2f, iconSize, iconSize, iconAlpha, 0f, System.identityHashCode(item), -1f);
            return;
        }
        AnimatedItemIcons.drawAtCenter(ICONS, item.iconPath(), cellPosition.getCenterX(), cellPosition.getCenterY(), iconSize, iconSize,
                Color.WHITE, iconAlpha);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, cellPosition);
    }
}
