package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    private final SkillTreeUiButton templateModeButton = new SkillTreeUiButton(Translation.text("ui.template.button.mode"));
    private final SkillTreeUiButton saveButton = new SkillTreeUiButton(Translation.text("ui.template.button.save"));
    private final SkillTreeUiButton loadButton = new SkillTreeUiButton(Translation.text("ui.template.button.load"));
    private final SkillTreeUiButton autoAllocateButton = new SkillTreeUiButton(Translation.text("ui.template.button.autoOff"));
    private final SkillTreeInfoTooltipRenderer tooltipRenderer;
    private final Map<String, String> translatedTexts = new HashMap<>();
    private final TextLabel templateLine = new TextLabel(FONT_SIZE, SkillTreePanelStyle.POSITIVE_STAT_COLOR);
    private final TextLabel resultLine = new TextLabel(FONT_SIZE, RESULT_COLOR);

    private TemplateBarState barState = TemplateBarState.HIDDEN;
    private String statusText;
    private String resultText;
    private float resultSeconds;
    private String laidOutSaveLabelKey;
    private boolean laidOutAutoOn;
    private boolean laidOutVisible;
    private float laidOutBottom = Float.NaN;
    private float laidOutRight = Float.NaN;

    SkillTreeTemplateBar(SkillTreePanelStyle style) {
        this.tooltipRenderer = new SkillTreeInfoTooltipRenderer(style);
    }

    void update(TemplateBarState newState, String newStatusText) {
        barState = newState;
        templateModeButton.setEnabled(newState.templateModeEnabled());
        templateModeButton.setSelected(newState.planning());
        saveButton.setEnabled(newState.saveEnabled());
        loadButton.setEnabled(newState.loadEnabled());
        autoAllocateButton.setEnabled(newState.autoEnabled());
        autoAllocateButton.setSelected(newState.autoOn());
        if (!Objects.equals(newStatusText, statusText)) {
            statusText = newStatusText;
            setLine(true, newStatusText);
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
        return barState.visible() && (templateModeButton.contains(x, y) || saveButton.contains(x, y) || loadButton.contains(x, y)
                || autoAllocateButton.contains(x, y));
    }

    TemplateAction actionAt(float x, float y) {
        if (!barState.visible()) {
            return null;
        }
        if (templateModeButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.TOGGLE_TEMPLATE_MODE);
        }
        if (saveButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.OPEN_SAVE);
        }
        if (loadButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.OPEN_LOAD);
        }
        if (autoAllocateButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.TOGGLE_AUTO_ALLOCATE);
        }
        return contains(x, y) ? TemplateAction.NONE : null;
    }

    void layout(PositionAPI canvasPosition) {
        float rowBottom = canvasPosition.getY() + MARGIN;
        float rowRight = canvasPosition.getX() + canvasPosition.getWidth() - MARGIN;
        if (barState.visible() == laidOutVisible && rowBottom == laidOutBottom && rowRight == laidOutRight
                && barState.saveLabelKey().equals(laidOutSaveLabelKey) && barState.autoOn() == laidOutAutoOn) {
            return;
        }
        laidOutVisible = barState.visible();
        laidOutBottom = rowBottom;
        laidOutRight = rowRight;
        laidOutSaveLabelKey = barState.saveLabelKey();
        laidOutAutoOn = barState.autoOn();
        if (!laidOutVisible) {
            templateModeButton.hide();
            saveButton.hide();
            loadButton.hide();
            autoAllocateButton.hide();
            return;
        }
        saveButton.setLabel(text(laidOutSaveLabelKey));
        autoAllocateButton.setLabel(text(laidOutAutoOn ? "ui.template.button.autoOn" : "ui.template.button.autoOff"));
        rowRight = placeLeftOf(autoAllocateButton, rowRight, rowBottom);
        rowRight = placeLeftOf(loadButton, rowRight, rowBottom);
        rowRight = placeLeftOf(saveButton, rowRight, rowBottom);
        placeLeftOf(templateModeButton, rowRight, rowBottom);
    }

    private static float placeLeftOf(SkillTreeUiButton button, float right, float bottom) {
        float width = Math.max(MIN_BUTTON_WIDTH, button.preferredWidth());
        button.place(right - width, bottom, width, BUTTON_HEIGHT);
        return right - width - GAP;
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        templateModeButton.render(mouseX, mouseY, alphaMult);
        saveButton.render(mouseX, mouseY, alphaMult);
        loadButton.render(mouseX, mouseY, alphaMult);
        autoAllocateButton.render(mouseX, mouseY, alphaMult);
        if (!barState.visible()) {
            return;
        }
        resultLine.setAlpha(alphaMult);
        templateLine.setAlpha(alphaMult);
        float statusRight = canvasPosition.getX() + canvasPosition.getWidth() - MARGIN;
        float statusTop = canvasPosition.getY() + MARGIN + BUTTON_HEIGHT + STATUS_GAP + STATUS_LINE_HEIGHT;
        if (resultText != null && resultLine.draw(statusRight - resultLine.width(), statusTop)) {
            statusTop += STATUS_LINE_HEIGHT;
        }
        if (statusText != null) {
            templateLine.draw(statusRight - templateLine.width(), statusTop);
        }
    }

    void renderTooltip(float mouseX, float mouseY, float alphaMult) {
        if (!barState.visible()) {
            return;
        }
        if (templateModeButton.contains(mouseX, mouseY)) {
            tooltipRenderer.render(text("ui.template.button.mode"), text(barState.planning() ? "ui.template.hint.modeOn" : "ui.template.hint.mode"),
                    mouseX, mouseY, alphaMult);
        } else if (saveButton.contains(mouseX, mouseY)) {
            tooltipRenderer.render(text(barState.saveLabelKey()), text(barState.saveHintKey()), mouseX, mouseY, alphaMult);
        } else if (loadButton.contains(mouseX, mouseY)) {
            tooltipRenderer.render(text("ui.template.button.load"), text("ui.template.hint.load"), mouseX, mouseY, alphaMult);
        } else if (autoAllocateButton.contains(mouseX, mouseY)) {
            tooltipRenderer.render(text(barState.autoOn() ? "ui.template.button.autoOn" : "ui.template.button.autoOff"),
                    text(barState.autoHintKey()), mouseX, mouseY, alphaMult);
        }
    }

    private String text(String key) {
        return translatedTexts.computeIfAbsent(key, Translation::text);
    }

    private void setLine(boolean statusLine, String text) {
        if (text != null) {
            (statusLine ? templateLine : resultLine).set(text);
        }
    }
}
