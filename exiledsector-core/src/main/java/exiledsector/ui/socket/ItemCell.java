package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.items.Item;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public final class ItemCell extends BaseCustomUIPanelPlugin {

    private static final float BORDER_WIDTH = 1.5f;
    private static final float SELECTED_BORDER_WIDTH = 3f;
    private static final float CHARGED_BORDER_WIDTH = 2.5f;
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
    private final Color chargedBorderColor;
    private final TileChrome.Frame markedFrame;
    private final TileChrome.Frame selectedFrame;
    private final TileChrome.Frame hoverFrame;
    private final TileChrome.Frame idleFrame;
    private final String iconPath;
    private final TileInput tileInput;
    private PositionAPI cellPosition;

    public ItemCell(Item item, Look look, Listener cellListener) {
        this.item = Objects.requireNonNull(item, "item");
        this.look = look;
        this.cellListener = cellListener;
        CargoIconLook cargoLook = CargoIconLooks.forItem(item);
        this.ownArtLook = cargoLook != null && cargoLook.ownArt() ? cargoLook : null;
        Color frameColor = item.frameColor();
        this.chargedBorderColor = cargoLook == null ? null : cargoLook.borderColor();
        Color idleBorderColor = chargedBorderColor != null ? chargedBorderColor : frameColor;
        float idleBorderWidth = chargedBorderColor != null ? CHARGED_BORDER_WIDTH : BORDER_WIDTH;
        float glowAlpha = look.rarityGlow() ? GLOW_ALPHA : 0f;
        this.markedFrame = new TileChrome.Frame(frameColor, GLOW_WIDTH, glowAlpha, Misc.getNegativeHighlightColor(), SELECTED_BORDER_WIDTH, 1f);
        this.selectedFrame = new TileChrome.Frame(frameColor, GLOW_WIDTH, glowAlpha, Misc.getBrightPlayerColor(), SELECTED_BORDER_WIDTH, 1f);
        this.hoverFrame = new TileChrome.Frame(frameColor, GLOW_WIDTH, glowAlpha, idleBorderColor, idleBorderWidth, 1f);
        this.idleFrame = new TileChrome.Frame(frameColor, GLOW_WIDTH, glowAlpha, idleBorderColor, idleBorderWidth, look.idleBorderAlpha());
        this.iconPath = item.iconPath();
        this.tileInput = new TileInput(TileInput.Handler.of(cellListener::hovered, cellListener::left, cellListener::clicked,
                cellListener::rightClicked), true);
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
        if (cellListener.marked()) {
            TileChrome.draw(cellPosition, markedFrame, alphaMult);
            GLDraw.fillQuad(cellPosition.getX(), cellPosition.getY(), cellPosition.getWidth(), cellPosition.getHeight(), markedFrame.borderColor(),
                    MARKED_FILL_ALPHA * alphaMult);
            return;
        }
        TileChrome.Frame shownFrame = cellListener.selected() ? selectedFrame : tileInput.isHovered() ? hoverFrame : idleFrame;
        TileChrome.draw(cellPosition, shownFrame, alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null) {
            return;
        }
        float iconSize = cellPosition.getWidth() - look.iconInset() * 2f;
        float iconAlpha = (cellListener.dimmed() ? DIMMED_ICON_ALPHA : 1f) * alphaMult;
        int phaseSeed = System.identityHashCode(item);
        TileChrome.drawIcon(ICONS, iconPath, ownArtLook, cellPosition, iconSize, iconAlpha, 0f, phaseSeed);
        if (chargedBorderColor != null) {
            float boxHalf = Math.min(cellPosition.getWidth(), cellPosition.getHeight()) / 2f - CHARGED_BORDER_WIDTH / 2f;
            CargoIconFx.renderArcs(chargedBorderColor, cellPosition.getCenterX(), cellPosition.getCenterY(), boxHalf, phaseSeed, iconAlpha);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, cellPosition);
    }
}
