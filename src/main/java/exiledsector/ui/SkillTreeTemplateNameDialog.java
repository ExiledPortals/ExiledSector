package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateNames;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;

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
    private static final float BACKDROP_ALPHA = 0.55f;
    private static final Color PROBLEM_COLOR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;

    private final BorderedPanel panel = new BorderedPanel(SkillTreeTemplateNameDialog.class);
    private final BorderedPanel fieldPanel = new BorderedPanel(SkillTreeTemplateNameDialog.class);
    private final SkillTreeTextField field = new SkillTreeTextField(TemplateNames.MAX_LENGTH,
            SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final SkillTreeUiButton save = new SkillTreeUiButton(Translation.text("ui.template.dialog.save"));
    private final SkillTreeUiButton cancel = new SkillTreeUiButton(Translation.text("ui.template.dialog.cancel"));
    private final ReusableText title = new ReusableText(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR)
            .set(Translation.text("ui.template.dialog.title"));
    private final ReusableText summary = new ReusableText(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final ReusableText problemLine = new ReusableText(SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE, PROBLEM_COLOR)
            .set(Translation.text("ui.template.dialog.duplicate"));

    private boolean open;
    private String rootNodeId;
    private Collection<SkillTreeTemplate> existing = List.of();
    private TemplateNames.Problem problem = TemplateNames.Problem.EMPTY;

    void open(String rootNodeId, int nodeCount, Collection<SkillTreeTemplate> existing) {
        this.rootNodeId = rootNodeId;
        this.existing = existing;
        this.open = true;
        field.setText("");
        field.focus(true);
        revalidate();
        summary.set(Translation.msg("ui.template.dialog.summary").count(nodeCount).text());
    }

    void close() {
        open = false;
        field.focus(false);
    }

    boolean isOpen() {
        return open;
    }

    String name() {
        return TemplateNames.normalise(field.text());
    }

    boolean canSave() {
        return problem == TemplateNames.Problem.NONE;
    }

    SkillTreeTextField.KeyResult handleKey(InputEventAPI event) {
        SkillTreeTextField.KeyResult result = field.handleKey(event);
        if (result == SkillTreeTextField.KeyResult.EDITED) {
            revalidate();
        }
        return result;
    }

    TemplateAction actionAt(float x, float y) {
        if (save.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.DIALOG_SAVE);
        }
        if (cancel.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.DIALOG_CANCEL);
        }
        return TemplateAction.NONE;
    }

    void advance(float amount) {
        if (open) {
            field.advance(amount);
        }
    }

    void render(PositionAPI position, float mouseX, float mouseY, float alphaMult) {
        if (!open) {
            return;
        }
        GLDraw.fillQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), Color.BLACK, BACKDROP_ALPHA * alphaMult);
        float left = position.getX() + (position.getWidth() - WIDTH) / 2f;
        float bottom = position.getY() + (position.getHeight() - HEIGHT) / 2f;
        panel.draw(left, bottom, WIDTH, HEIGHT, alphaMult);

        float top = bottom + HEIGHT - PADDING;
        title.draw(left + PADDING, top);
        top -= SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE + 8f;
        summary.draw(left + PADDING, top);
        top -= SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE + 12f;
        float fieldBottom = top - FIELD_HEIGHT;
        fieldPanel.draw(left + PADDING, fieldBottom, WIDTH - PADDING * 2f, FIELD_HEIGHT, alphaMult);
        field.render(left + PADDING + FIELD_TEXT_PADDING, fieldBottom, WIDTH - (PADDING + FIELD_TEXT_PADDING) * 2f, FIELD_HEIGHT, "");
        if (problem == TemplateNames.Problem.DUPLICATE) {
            problemLine.draw(left + PADDING, fieldBottom - 6f);
        }

        float buttonBottom = bottom + PADDING;
        cancel.place(left + WIDTH - PADDING - BUTTON_WIDTH, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        save.place(left + WIDTH - PADDING - BUTTON_WIDTH * 2f - 8f, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        save.setEnabled(canSave());
        save.render(mouseX, mouseY, alphaMult);
        cancel.render(mouseX, mouseY, alphaMult);
    }

    private void revalidate() {
        problem = TemplateNames.validate(field.text(), rootNodeId, existing);
    }
}
