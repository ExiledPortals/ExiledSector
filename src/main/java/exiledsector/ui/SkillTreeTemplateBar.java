package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class SkillTreeTemplateBar {

    private static final float MARGIN = 16f;
    private static final float BUTTON_HEIGHT = 40f;
    private static final float GAP = 8f;
    private static final float MIN_BUTTON_WIDTH = 120f;
    private static final float STATUS_GAP = 8f;
    private static final float STATUS_LINE_HEIGHT = 22f;
    private static final float FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final float RESULT_SECONDS = 4f;
    private static final Color RESULT_COLOR = SkillTreePanelStyle.TOOLTIP_BODY_COLOR;

    private final SkillTreeUiButton save = new SkillTreeUiButton(Translation.text("ui.template.button.save"));
    private final SkillTreeUiButton load = new SkillTreeUiButton(Translation.text("ui.template.button.load"));
    private final SkillTreeUiButton auto = new SkillTreeUiButton(Translation.text("ui.template.button.autoAllocate"));
    private final SkillTreeInfoTooltipRenderer tooltip;
    private final Map<String, String> texts = new HashMap<>();
    private final ReusableText templateLine = new ReusableText(FONT_SIZE, SkillTreePanelStyle.POSITIVE_STAT_COLOR);
    private final ReusableText resultLine = new ReusableText(FONT_SIZE, RESULT_COLOR);

    private TemplateBarState state = TemplateBarState.HIDDEN;
    private String templateName;
    private String resultText;
    private float resultSeconds;
    private boolean laidOutVisible;
    private float laidOutBottom = Float.NaN;
    private float laidOutRight = Float.NaN;

    SkillTreeTemplateBar(SkillTreePanelStyle style) {
        this.tooltip = new SkillTreeInfoTooltipRenderer(style);
    }

    void update(TemplateBarState newState, String newTemplateName) {
        state = newState;
        save.setEnabled(newState.saveEnabled());
        load.setEnabled(newState.loadEnabled());
        auto.setEnabled(newState.autoEnabled());
        if (newTemplateName == null ? templateName != null : !newTemplateName.equals(templateName)) {
            templateName = newTemplateName;
            setLine(true, newTemplateName == null ? null : Translation.msg("ui.template.current").arg("name", newTemplateName).text());
        }
    }

    void showResult(String text) {
        resultText = text;
        resultSeconds = RESULT_SECONDS;
        setLine(false, text);
    }

    void advance(float amount) {
        if (resultSeconds > 0f) {
            resultSeconds -= amount;
            if (resultSeconds <= 0f) {
                resultText = null;
            }
        }
    }

    boolean contains(float x, float y) {
        return state.visible() && (save.contains(x, y) || load.contains(x, y) || auto.contains(x, y));
    }

    TemplateAction actionAt(float x, float y) {
        if (!state.visible()) {
            return null;
        }
        if (save.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.OPEN_SAVE);
        }
        if (load.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.OPEN_LOAD);
        }
        if (auto.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.AUTO_ALLOCATE);
        }
        return contains(x, y) ? TemplateAction.NONE : null;
    }

    void layout(PositionAPI position) {
        float bottom = position.getY() + MARGIN;
        float right = position.getX() + position.getWidth() - MARGIN;
        if (state.visible() == laidOutVisible && bottom == laidOutBottom && right == laidOutRight) {
            return;
        }
        laidOutVisible = state.visible();
        laidOutBottom = bottom;
        laidOutRight = right;
        if (!laidOutVisible) {
            save.hide();
            load.hide();
            auto.hide();
            return;
        }
        right = placeLeftOf(auto, right, bottom);
        right = placeLeftOf(load, right, bottom);
        placeLeftOf(save, right, bottom);
    }

    private static float placeLeftOf(SkillTreeUiButton button, float right, float bottom) {
        float width = Math.max(MIN_BUTTON_WIDTH, button.preferredWidth());
        button.place(right - width, bottom, width, BUTTON_HEIGHT);
        return right - width - GAP;
    }

    void render(PositionAPI position, float mouseX, float mouseY, float alphaMult) {
        save.render(mouseX, mouseY, alphaMult);
        load.render(mouseX, mouseY, alphaMult);
        auto.render(mouseX, mouseY, alphaMult);
        if (!state.visible()) {
            return;
        }
        resultLine.setAlpha(alphaMult);
        templateLine.setAlpha(alphaMult);
        float right = position.getX() + position.getWidth() - MARGIN;
        float top = position.getY() + MARGIN + BUTTON_HEIGHT + STATUS_GAP + STATUS_LINE_HEIGHT;
        if (resultText != null && resultLine.draw(right - resultLine.width(), top)) {
            top += STATUS_LINE_HEIGHT;
        }
        if (templateName != null) {
            templateLine.draw(right - templateLine.width(), top);
        }
    }

    void renderTooltip(float mouseX, float mouseY, float alphaMult) {
        if (!state.visible()) {
            return;
        }
        if (save.contains(mouseX, mouseY)) {
            tooltip.render(text("ui.template.button.save"), text(state.saveHintKey()), mouseX, mouseY, alphaMult);
        } else if (load.contains(mouseX, mouseY)) {
            tooltip.render(text("ui.template.button.load"), text("ui.template.hint.load"), mouseX, mouseY, alphaMult);
        } else if (auto.contains(mouseX, mouseY)) {
            tooltip.render(text("ui.template.button.autoAllocate"), text(state.autoHintKey()), mouseX, mouseY, alphaMult);
        }
    }

    private String text(String key) {
        return texts.computeIfAbsent(key, Translation::text);
    }

    private void setLine(boolean templateNameLine, String text) {
        if (text != null) {
            (templateNameLine ? templateLine : resultLine).set(text);
        }
    }
}
