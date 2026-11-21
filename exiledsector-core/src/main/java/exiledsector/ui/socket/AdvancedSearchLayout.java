package exiledsector.ui.socket;

public record AdvancedSearchLayout(float paneLeft, float paneWidth, float paneBottom, float paneHeight, boolean twoLineRows) {

    static final float CONTROL_HEIGHT = 24f;
    static final float BOUND_FIELD_WIDTH = 52f;
    static final float REMOVE_BUTTON_WIDTH = 24f;
    static final float MIN_PICKER_WIDTH = 200f;
    private static final float CONTROLS_AFTER_PICKER = BOUND_FIELD_WIDTH * 2f + REMOVE_BUTTON_WIDTH + HoloPanel.GAP * 3f;
    private static final float PANE_EDGES = HoloPanel.PAD * 2f + ItemGrid.SCROLLBAR_ROOM;
    public static final float MIN_ONE_LINE_PANE_WIDTH = MIN_PICKER_WIDTH + CONTROLS_AFTER_PICKER + PANE_EDGES;

    record RowColumns(float pickerWidth, float minLeft, float maxLeft, float boundWidth, float removeLeft, float controlsTop, float rowHeight) {
    }

    public static AdvancedSearchLayout in(FreeArea area) {
        return new AdvancedSearchLayout(area.left(), area.width(), area.bottom(), area.height(), area.width() < MIN_ONE_LINE_PANE_WIDTH);
    }

    public float innerWidth() {
        return paneWidth - HoloPanel.PAD * 2f;
    }

    public float rowWidth() {
        return paneWidth - PANE_EDGES;
    }

    public float paneTop() {
        return paneBottom - paneHeight;
    }

    RowColumns columns() {
        float rowWidth = rowWidth();
        if (!twoLineRows) {
            float pickerWidth = rowWidth - CONTROLS_AFTER_PICKER;
            return columnsFrom(pickerWidth, pickerWidth + HoloPanel.GAP, BOUND_FIELD_WIDTH, 0f, CONTROL_HEIGHT);
        }
        float pickerWidth = Math.max(0f, rowWidth);
        float boundWidth = Math.min(BOUND_FIELD_WIDTH, Math.max(0f, rowWidth - REMOVE_BUTTON_WIDTH - HoloPanel.GAP * 2f) / 2f);
        float controlsTop = CONTROL_HEIGHT + HoloPanel.LINE_PAD;
        return columnsFrom(pickerWidth, 0f, boundWidth, controlsTop, controlsTop + CONTROL_HEIGHT);
    }

    private static RowColumns columnsFrom(float pickerWidth, float minLeft, float boundWidth, float controlsTop, float rowHeight) {
        float maxLeft = minLeft + boundWidth + HoloPanel.GAP;
        float removeLeft = maxLeft + boundWidth + HoloPanel.GAP;
        return new RowColumns(pickerWidth, minLeft, maxLeft, boundWidth, removeLeft, controlsTop, rowHeight);
    }
}
