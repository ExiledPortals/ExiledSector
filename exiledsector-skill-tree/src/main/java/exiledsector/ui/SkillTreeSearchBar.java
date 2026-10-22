package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.i18n.Translation;
import exiledlib.ui.SkillTreePanelStyle;
import exiledsector.ui.node.NodeSearch;
import exiledlib.ui.util.BorderedPanel;
import exiledlib.ui.util.Rects;

import java.util.function.ToDoubleFunction;

final class SkillTreeSearchBar {

    static final int MAX_QUERY_LENGTH = 32;
    private static final float WIDTH = 360f;
    private static final float TEXT_PADDING = 22f;
    private static final float TEXT_WIDTH = WIDTH - TEXT_PADDING * 2f;

    private final NodeSearch nodeSearch;
    private final Runnable onSubmit;
    private final SkillTreeTextField queryField = new SkillTreeTextField(MAX_QUERY_LENGTH,
            SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final String placeholderText = Translation.text("ui.search.placeholder");

    SkillTreeSearchBar(NodeSearch nodeSearch, Runnable onSubmit) {
        this.nodeSearch = nodeSearch;
        this.onSubmit = onSubmit;
    }

    boolean handleClick(PositionAPI canvasPosition, float x, float y) {
        boolean hit = contains(canvasPosition, x, y);
        queryField.focus(hit);
        return hit;
    }

    void unfocus() {
        queryField.focus(false);
    }

    boolean isFocused() {
        return queryField.isFocused();
    }

    boolean handleKey(InputEventAPI event) {
        queryField.setText(nodeSearch.getQuery());
        SkillTreeTextField.KeyResult result = queryField.handleKey(event);
        switch (result) {
            case EDITED -> nodeSearch.setQuery(queryField.text());
            case CANCEL -> {
                queryField.setText("");
                nodeSearch.setQuery("");
                queryField.focus(false);
            }
            case SUBMIT -> onSubmit.run();
            default -> {
            }
        }
        return result != SkillTreeTextField.KeyResult.IGNORED;
    }

    void advance(float amount) {
        queryField.advance(amount);
    }

    void render(PositionAPI canvasPosition, float alphaMult) {
        float barLeft = left(canvasPosition);
        float barBottom = bottom(canvasPosition);
        BorderedPanel.draw(barLeft, barBottom, WIDTH, ChromeMetrics.CONTROL_HEIGHT, alphaMult);
        queryField.setText(nodeSearch.getQuery());
        queryField.render(barLeft + TEXT_PADDING, barBottom, TEXT_WIDTH, ChromeMetrics.CONTROL_HEIGHT, placeholderText, alphaMult);
    }

    static String fitEnd(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitEnd(value, TEXT_WIDTH, widthOf);
    }

    static String fitStart(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitStart(value, TEXT_WIDTH, widthOf);
    }

    static boolean contains(PositionAPI canvasPosition, float x, float y) {
        return Rects.contains(left(canvasPosition), bottom(canvasPosition), WIDTH, ChromeMetrics.CONTROL_HEIGHT, x, y);
    }

    private static float left(PositionAPI canvasPosition) {
        return canvasPosition.getX() + (canvasPosition.getWidth() - WIDTH) / 2f;
    }

    private static float bottom(PositionAPI canvasPosition) {
        return canvasPosition.getY() + canvasPosition.getHeight() - ChromeMetrics.EDGE_MARGIN - ChromeMetrics.CONTROL_HEIGHT;
    }
}
