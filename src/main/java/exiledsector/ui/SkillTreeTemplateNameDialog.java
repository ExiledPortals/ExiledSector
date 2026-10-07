package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateNames;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

final class SkillTreeTemplateNameDialog {

    private static final float WIDTH = 520f;
    private static final float HEIGHT = 236f;
    private static final float PADDING = 24f;
    private static final float FIELD_HEIGHT = 40f;
    private static final float FIELD_TEXT_PADDING = 18f;
    private static final float BUTTON_WIDTH = 140f;
    private static final float BUTTON_HEIGHT = 36f;
    private static final Color PROBLEM_COLOR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;

    private final HoloFrame modalFrame = new HoloFrame(SkillTreeTemplateNameDialog.class, HoloFrame.Look.MODAL);
    private final BorderedPanel fieldPanel = new BorderedPanel(SkillTreeTemplateNameDialog.class);
    private final SkillTreeTextField nameField = new SkillTreeTextField(TemplateNames.MAX_LENGTH,
            SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final SkillTreeUiButton saveButton = new SkillTreeUiButton(Translation.text("ui.template.dialog.save"));
    private final SkillTreeUiButton cancelButton = new SkillTreeUiButton(Translation.text("ui.template.dialog.cancel"));
    private final TextLabel titleLine = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR)
            .set(Translation.text("ui.template.dialog.title"));
    private final TextLabel summaryLine = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final TextLabel problemLine = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, PROBLEM_COLOR)
            .set(Translation.text("ui.template.dialog.duplicate"));

    private boolean dialogOpen;
    private String rootNodeId;
    private Collection<SkillTreeTemplate> existingTemplates = List.of();
    private TemplateNames.Problem nameProblem = TemplateNames.Problem.EMPTY;

    void open(String rootNodeId, int nodeCount, Collection<SkillTreeTemplate> existingTemplates) {
        this.rootNodeId = rootNodeId;
        this.existingTemplates = existingTemplates;
        this.dialogOpen = true;
        modalFrame.open();
        hideButtons();
        nameField.setText("");
        nameField.focus(true);
        revalidate();
        summaryLine.set(Translation.msg("ui.template.dialog.summary").count(nodeCount).text());
    }

    void close() {
        dialogOpen = false;
        modalFrame.close();
        hideButtons();
        nameField.focus(false);
    }

    boolean isOpen() {
        return dialogOpen;
    }

    boolean isBlocking() {
        return dialogOpen || modalFrame.isBlocking();
    }

    String name() {
        return TemplateNames.normalise(nameField.text());
    }

    boolean canSave() {
        return nameProblem == TemplateNames.Problem.NONE;
    }

    SkillTreeTextField.KeyResult handleKey(InputEventAPI event) {
        SkillTreeTextField.KeyResult result = nameField.handleKey(event);
        if (result == SkillTreeTextField.KeyResult.EDITED) {
            revalidate();
        }
        return result;
    }

    TemplateAction actionAt(float x, float y) {
        if (saveButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.DIALOG_SAVE);
        }
        if (cancelButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.DIALOG_CANCEL);
        }
        return TemplateAction.NONE;
    }

    void advance(float amount) {
        modalFrame.advance(amount);
        if (dialogOpen) {
            nameField.advance(amount);
        }
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        if (!modalFrame.isVisible()) {
            return;
        }
        ScreenRect dialogBox = ScreenRect.centeredIn(canvasPosition, WIDTH, HEIGHT);
        modalFrame.renderBackdrop(canvasPosition, alphaMult);
        modalFrame.render(dialogBox.left(), dialogBox.bottom(), WIDTH, HEIGHT, SkillTreePanelStyle.GLOW_COLOR, alphaMult,
                (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderContent(frameLeft, frameBottom, mouseX, mouseY, frameAlpha));
    }

    private void renderContent(float dialogLeft, float dialogBottom, float mouseX, float mouseY, float alphaMult) {
        float cursorTop = dialogBottom + HEIGHT - PADDING;
        titleLine.setAlpha(alphaMult).draw(dialogLeft + PADDING, cursorTop);
        cursorTop -= SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE + 8f;
        summaryLine.setAlpha(alphaMult).draw(dialogLeft + PADDING, cursorTop);
        cursorTop -= SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE + 12f;
        float fieldBottom = cursorTop - FIELD_HEIGHT;
        fieldPanel.draw(dialogLeft + PADDING, fieldBottom, WIDTH - PADDING * 2f, FIELD_HEIGHT, alphaMult);
        nameField.render(dialogLeft + PADDING + FIELD_TEXT_PADDING, fieldBottom, WIDTH - (PADDING + FIELD_TEXT_PADDING) * 2f, FIELD_HEIGHT, "", alphaMult);
        if (nameProblem == TemplateNames.Problem.DUPLICATE) {
            problemLine.setAlpha(alphaMult).draw(dialogLeft + PADDING, fieldBottom - 6f);
        }

        float buttonBottom = dialogBottom + PADDING;
        cancelButton.place(dialogLeft + WIDTH - PADDING - BUTTON_WIDTH, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        saveButton.place(dialogLeft + WIDTH - PADDING - BUTTON_WIDTH * 2f - 8f, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        saveButton.setEnabled(canSave());
        saveButton.render(mouseX, mouseY, alphaMult);
        cancelButton.render(mouseX, mouseY, alphaMult);
    }

    private void hideButtons() {
        saveButton.hide();
        cancelButton.hide();
    }

    private void revalidate() {
        nameProblem = TemplateNames.validate(nameField.text(), rootNodeId, existingTemplates);
    }
}
