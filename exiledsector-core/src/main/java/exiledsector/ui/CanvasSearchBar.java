package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.Rects;

import java.util.function.ToDoubleFunction;

public final class CanvasSearchBar {

    public static final int MAX_QUERY_LENGTH = 32;
    public static final float DEFAULT_WIDTH = 360f;
    private static final float TEXT_PADDING = 22f;

    private final SearchQuery search;
    private final Runnable onSubmit;
    private final float barWidth;
    private final SkillTreeTextField queryField = new SkillTreeTextField(MAX_QUERY_LENGTH,
            UiFonts.DIALOG_TEXT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final String placeholderText;

    public CanvasSearchBar(SearchQuery search, String placeholderText, Runnable onSubmit) {
        this(search, placeholderText, onSubmit, DEFAULT_WIDTH);
    }

    public CanvasSearchBar(SearchQuery search, String placeholderText, Runnable onSubmit, float barWidth) {
        this.search = search;
        this.placeholderText = placeholderText;
        this.onSubmit = onSubmit;
        this.barWidth = barWidth;
    }

    public float barWidth() {
        return barWidth;
    }

    public boolean handleClick(PositionAPI canvasPosition, float x, float y) {
        return handleClick(centredLeft(canvasPosition), topBottom(canvasPosition), x, y);
    }

    public boolean handleClick(float barLeft, float barBottom, float x, float y) {
        boolean hit = contains(barLeft, barBottom, x, y);
        queryField.focus(hit);
        return hit;
    }

    public void unfocus() {
        queryField.focus(false);
    }

    public boolean isFocused() {
        return queryField.isFocused();
    }

    public boolean handleKey(InputEventAPI event) {
        queryField.setText(search.getQuery());
        SkillTreeTextField.KeyResult result = queryField.handleKey(event);
        switch (result) {
            case EDITED -> search.setQuery(queryField.text());
            case SUBMIT -> onSubmit.run();
            default -> {
            }
        }
        return result != SkillTreeTextField.KeyResult.IGNORED;
    }

    public boolean escape() {
        if (!queryField.isFocused()) {
            return false;
        }
        queryField.setText("");
        search.setQuery("");
        queryField.focus(false);
        return true;
    }

    public void advance(float amount) {
        queryField.advance(amount);
    }

    public void render(PositionAPI canvasPosition, float alphaMult) {
        render(centredLeft(canvasPosition), topBottom(canvasPosition), alphaMult);
    }

    public void render(float barLeft, float barBottom, float alphaMult) {
        BorderedPanel.draw(barLeft, barBottom, barWidth, ChromeMetrics.CONTROL_HEIGHT, alphaMult);
        queryField.setText(search.getQuery());
        queryField.render(barLeft + TEXT_PADDING, barBottom, textWidth(), ChromeMetrics.CONTROL_HEIGHT, placeholderText, alphaMult);
    }

    String fitEnd(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitEnd(value, textWidth(), widthOf);
    }

    String fitStart(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitStart(value, textWidth(), widthOf);
    }

    private float textWidth() {
        return barWidth - TEXT_PADDING * 2f;
    }

    public boolean contains(PositionAPI canvasPosition, float x, float y) {
        return contains(centredLeft(canvasPosition), topBottom(canvasPosition), x, y);
    }

    public boolean contains(float barLeft, float barBottom, float x, float y) {
        return Rects.contains(barLeft, barBottom, barWidth, ChromeMetrics.CONTROL_HEIGHT, x, y);
    }

    private float centredLeft(PositionAPI canvasPosition) {
        return canvasPosition.getX() + (canvasPosition.getWidth() - barWidth) / 2f;
    }

    private static float topBottom(PositionAPI canvasPosition) {
        return canvasPosition.getY() + canvasPosition.getHeight() - ChromeMetrics.EDGE_MARGIN - ChromeMetrics.CONTROL_HEIGHT;
    }
}
