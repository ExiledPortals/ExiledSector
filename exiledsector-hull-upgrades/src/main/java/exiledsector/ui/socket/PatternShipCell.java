package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.decoration.RefitShipPortrait;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UiClip;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

final class PatternShipCell extends BaseCustomUIPanelPlugin {

    static final float NAME_HEIGHT = 20f;
    private static final float SHIP_INSET = 8f;
    private static final float MAX_SHIP_ZOOM = 1f;
    private static final float LINKED_SHIP_ALPHA = 1f;
    private static final float HOVERED_SHIP_ALPHA = 0.6f;
    private static final float DULL_SHIP_ALPHA = 0.3f;
    private static final float GLOW_WIDTH = 18f;
    private static final float LINKED_GLOW_ALPHA = 0.55f;
    private static final float DULL_GLOW_ALPHA = 0.2f;
    private static final float BORDER_WIDTH = 1.5f;
    private static final float HOVER_BORDER_WIDTH = 2.5f;

    private final RefitShipPortrait portrait;
    private final boolean linked;
    private final boolean blocked;
    private final Supplier<List<PositionAPI>> visibleAreas;
    private final TileInput tileInput;
    private final TileChrome.Frame idleFrame;
    private final TileChrome.Frame hoverFrame;
    private PositionAPI cellPosition;

    PatternShipCell(RefitShipPortrait portrait, boolean linked, boolean blocked, Supplier<List<PositionAPI>> visibleAreas,
                    IconTileCell.Hooks hooks) {
        this.portrait = portrait;
        this.linked = linked;
        this.blocked = blocked;
        this.visibleAreas = visibleAreas;
        this.tileInput = TileInput.forHooks(hooks, blocked);
        Color accent = linked ? Misc.getBrightPlayerColor() : Misc.getDarkPlayerColor();
        float glowAlpha = linked ? LINKED_GLOW_ALPHA : DULL_GLOW_ALPHA;
        this.idleFrame = new TileChrome.Frame(accent, GLOW_WIDTH, glowAlpha, BORDER_WIDTH, linked ? 1f : 0.6f);
        this.hoverFrame = blocked ? idleFrame : new TileChrome.Frame(Misc.getBrightPlayerColor(), GLOW_WIDTH, glowAlpha * 1.4f, HOVER_BORDER_WIDTH, 1f);
    }

    @Override
    public void positionChanged(PositionAPI cellPosition) {
        this.cellPosition = cellPosition;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (cellPosition != null) {
            TileChrome.draw(cellPosition, tileInput.isHovered() ? hoverFrame : idleFrame, alphaMult);
        }
    }

    @Override
    public void render(float alphaMult) {
        if (cellPosition == null || portrait == null || alphaMult <= 0f) {
            return;
        }
        float boxWidth = cellPosition.getWidth() - SHIP_INSET * 2f;
        float boxHeight = cellPosition.getHeight() - NAME_HEIGHT - SHIP_INSET * 2f;
        float halfWidth = portrait.halfWidthPixels(1f);
        float halfLength = portrait.halfLengthPixels(1f);
        if (boxWidth <= 0f || boxHeight <= 0f || halfWidth <= 0f || halfLength <= 0f) {
            return;
        }
        float zoom = Math.min(MAX_SHIP_ZOOM, Math.min(boxWidth / (halfWidth * 2f), boxHeight / (halfLength * 2f)));
        float shipCentreX = cellPosition.getX() + cellPosition.getWidth() / 2f;
        float shipCentreY = cellPosition.getY() + NAME_HEIGHT + SHIP_INSET + boxHeight / 2f;
        float[] clip = clipArea();
        if (clip[2] <= 0f || clip[3] <= 0f) {
            return;
        }
        try (GlScope scope = UiClip.clipUi(clip[0], clip[1], clip[2], clip[3])) {
            portrait.draw(shipCentreX, shipCentreY, zoom, alphaMult * shipAlpha());
        }
    }

    private float shipAlpha() {
        if (linked) {
            return LINKED_SHIP_ALPHA;
        }
        return tileInput.isHovered() && !blocked ? HOVERED_SHIP_ALPHA : DULL_SHIP_ALPHA;
    }

    private float[] clipArea() {
        float left = cellPosition.getX();
        float bottom = cellPosition.getY();
        float right = left + cellPosition.getWidth();
        float top = bottom + cellPosition.getHeight();
        for (PositionAPI visible : visibleAreas.get()) {
            if (visible != null) {
                left = Math.max(left, visible.getX());
                bottom = Math.max(bottom, visible.getY());
                right = Math.min(right, visible.getX() + visible.getWidth());
                top = Math.min(top, visible.getY() + visible.getHeight());
            }
        }
        return new float[]{left, bottom, right - left, top - bottom};
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        tileInput.process(events, cellPosition);
    }
}
