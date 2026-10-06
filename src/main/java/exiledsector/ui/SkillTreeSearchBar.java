package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.node.NodeSearch;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.Rects;

import java.util.function.ToDoubleFunction;

final class SkillTreeSearchBar {

    static final int MAX_QUERY_LENGTH = 32;
    private static final float WIDTH = 360f;
    private static final float HEIGHT = 40f;
    private static final float TOP_MARGIN = 16f;
    private static final float TEXT_PADDING = 22f;
    private static final float TEXT_WIDTH = WIDTH - TEXT_PADDING * 2f;

    private final NodeSearch search;
    private final BorderedPanel panel = new BorderedPanel(SkillTreeSearchBar.class);
    private final SkillTreeTextField field = new SkillTreeTextField(MAX_QUERY_LENGTH,
            SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final String placeholder = Translation.text("ui.search.placeholder");

    SkillTreeSearchBar(NodeSearch search) {
        this.search = search;
    }

    boolean handleClick(PositionAPI position, float x, float y) {
        boolean hit = contains(position, x, y);
        field.focus(hit);
        return hit;
    }

    void unfocus() {
        field.focus(false);
    }

    boolean handleKey(InputEventAPI event) {
        field.setText(search.getQuery());
        SkillTreeTextField.KeyResult result = field.handleKey(event);
        switch (result) {
            case EDITED -> search.setQuery(field.text());
            case CANCEL -> {
                field.setText("");
                search.setQuery("");
                field.focus(false);
            }
            case SUBMIT -> field.focus(false);
            default -> {
            }
        }
        return result != SkillTreeTextField.KeyResult.IGNORED;
    }

    void advance(float amount) {
        field.advance(amount);
    }

    void render(PositionAPI position, float alphaMult) {
        float x = left(position);
        float y = bottom(position);
        panel.draw(x, y, WIDTH, HEIGHT, alphaMult);
        field.setText(search.getQuery());
        field.render(x + TEXT_PADDING, y, TEXT_WIDTH, HEIGHT, placeholder, alphaMult);
    }

    static String fitEnd(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitEnd(value, TEXT_WIDTH, widthOf);
    }

    static String fitStart(String value, ToDoubleFunction<String> widthOf) {
        return SkillTreeTextField.fitStart(value, TEXT_WIDTH, widthOf);
    }

    static boolean contains(PositionAPI position, float x, float y) {
        return Rects.contains(left(position), bottom(position), WIDTH, HEIGHT, x, y);
    }

    private static float left(PositionAPI position) {
        return position.getX() + (position.getWidth() - WIDTH) / 2f;
    }

    private static float bottom(PositionAPI position) {
        return position.getY() + position.getHeight() - TOP_MARGIN - HEIGHT;
    }
}
