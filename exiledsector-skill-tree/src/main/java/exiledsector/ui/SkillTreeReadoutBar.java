package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;

final class SkillTreeReadoutBar {

    static final float BAR_WIDTH = ReadoutBar.DEFAULT_WIDTH;
    private static final float BAR_HEIGHT = ReadoutBar.BAR_HEIGHT;
    private static final float ROW_GAP = 8f;

    private final ReadoutBar readoutBar;
    private final int row;

    SkillTreeReadoutBar(Class<?> owner, int row) {
        this(owner, row, null);
    }

    SkillTreeReadoutBar(Class<?> owner, int row, String labelKey) {
        this(owner, row, labelKey, null);
    }

    SkillTreeReadoutBar(Class<?> owner, int row, String labelKey, String plannedLabelKey) {
        this.readoutBar = new ReadoutBar(owner, BAR_WIDTH, labelKey, plannedLabelKey);
        this.row = row;
    }

    void setPlanned(int plannedSpent) {
        readoutBar.setPlanned(plannedSpent);
    }

    static float stackHeight(int rowCount) {
        return ChromeMetrics.EDGE_MARGIN + rowCount * BAR_HEIGHT + Math.max(0, rowCount - 1) * ROW_GAP;
    }

    void advance(float amount, PositionAPI canvasPosition, int targetSpent, int targetTotal, float mouseX, float mouseY, boolean mouseKnown) {
        readoutBar.advance(amount, targetSpent, targetTotal, mouseKnown && isHovered(canvasPosition, mouseX, mouseY));
    }

    boolean isHovered(PositionAPI canvasPosition, float x, float y) {
        if (canvasPosition == null) return false;
        return readoutBar.contains(left(canvasPosition), bottom(canvasPosition), x, y);
    }

    private static float left(PositionAPI canvasPosition) {
        return canvasPosition.getX() + ChromeMetrics.EDGE_MARGIN;
    }

    private float bottom(PositionAPI canvasPosition) {
        float barTop = canvasPosition.getY() + canvasPosition.getHeight() - ChromeMetrics.EDGE_MARGIN;
        return barTop - BAR_HEIGHT - row * (BAR_HEIGHT + ROW_GAP);
    }

    void render(PositionAPI canvasPosition, float alphaMult, String labelOverride) {
        readoutBar.render(left(canvasPosition), bottom(canvasPosition), alphaMult, labelOverride);
    }
}
