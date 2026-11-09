package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.TextLabel;

final class SchematicDialog {

    enum Choice { NONE, CONFIRM, CANCEL }

    private static final float WIDTH = 520f;
    private static final float MIN_HEIGHT = 200f;
    private static final float PADDING = 24f;
    private static final float TITLE_BODY_GAP = 12f;
    private static final float BODY_BUTTON_GAP = 20f;
    private static final float BUTTON_WIDTH = 140f;
    private static final float BUTTON_HEIGHT = 36f;
    private static final float BODY_MAX_HEIGHT = 300f;

    private final HoloFrame modalFrame = new HoloFrame(HoloFrame.Look.MODAL);
    private final SkillTreeUiButton confirmButton = new SkillTreeUiButton(Translation.text("ui.schematicDialog.confirm"));
    private final SkillTreeUiButton cancelButton = new SkillTreeUiButton(Translation.text("ui.schematicDialog.cancel"));
    private final TextLabel titleLine = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel bodyText = new TextLabel(UiFonts.DIALOG_TEXT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private boolean dialogOpen;
    private float dialogHeight = MIN_HEIGHT;

    void open(String title, String body) {
        dialogOpen = true;
        modalFrame.open();
        hideButtons();
        titleLine.set(title);
        bodyText.setWrapped(body, WIDTH - PADDING * 2f, BODY_MAX_HEIGHT);
        float contentHeight = PADDING + UiFonts.TITLE.size() + TITLE_BODY_GAP + bodyText.height() + BODY_BUTTON_GAP + BUTTON_HEIGHT + PADDING;
        dialogHeight = Math.max(MIN_HEIGHT, contentHeight);
    }

    void close() {
        dialogOpen = false;
        modalFrame.close();
        hideButtons();
    }

    boolean isOpen() {
        return dialogOpen;
    }

    boolean isBlocking() {
        return dialogOpen || modalFrame.isBlocking();
    }

    Choice choiceAt(float x, float y) {
        if (confirmButton.isClickable(x, y)) {
            return Choice.CONFIRM;
        }
        return cancelButton.isClickable(x, y) ? Choice.CANCEL : Choice.NONE;
    }

    void advance(float amount) {
        modalFrame.advance(amount);
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        if (!modalFrame.isVisible()) {
            return;
        }
        ScreenRect dialogBox = ScreenRect.centeredIn(canvasPosition, WIDTH, dialogHeight);
        modalFrame.renderBackdrop(canvasPosition, alphaMult);
        modalFrame.render(dialogBox.left(), dialogBox.bottom(), WIDTH, dialogHeight, SkillTreePanelStyle.GLOW_COLOR, alphaMult,
                (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderContent(frameLeft, frameBottom, mouseX, mouseY, frameAlpha));
    }

    private void renderContent(float dialogLeft, float dialogBottom, float mouseX, float mouseY, float alphaMult) {
        float cursorTop = dialogBottom + dialogHeight - PADDING;
        titleLine.setAlpha(alphaMult).draw(dialogLeft + PADDING, cursorTop);
        cursorTop -= UiFonts.TITLE.size() + TITLE_BODY_GAP;
        bodyText.setAlpha(alphaMult).draw(dialogLeft + PADDING, cursorTop);

        float buttonBottom = dialogBottom + PADDING;
        cancelButton.place(dialogLeft + WIDTH - PADDING - BUTTON_WIDTH, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        confirmButton.place(dialogLeft + WIDTH - PADDING - BUTTON_WIDTH * 2f - 8f, buttonBottom, BUTTON_WIDTH, BUTTON_HEIGHT);
        confirmButton.render(mouseX, mouseY, alphaMult);
        cancelButton.render(mouseX, mouseY, alphaMult);
    }

    private void hideButtons() {
        confirmButton.hide();
        cancelButton.hide();
    }
}
