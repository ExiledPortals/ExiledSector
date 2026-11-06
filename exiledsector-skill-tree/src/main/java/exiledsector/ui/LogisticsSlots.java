package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Translation;
import exiledsector.items.LogisticsSchematic;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.LogisticsNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.render.SpriteCache;
import org.lwjgl.input.Keyboard;

final class LogisticsSlots {

    private static final SpriteCache SPRITES = new SpriteCache(LogisticsSlots.class);
    private static final float BUTTON_GAP = 8f;

    private final String memberId;
    private final SkillTreeUiButton button = new SkillTreeUiButton("");
    private final LogisticsSlotDialog dialog = new LogisticsSlotDialog();
    private final String tooltipTitle = Translation.text("ui.logisticsSlots.title");
    private ShipSkillData shownData;
    private int shownRevision = -1;
    private int allocated;
    private int cap;
    private int held;
    private boolean hovered;
    private String tooltipBody;
    private LogisticsSlotDialog.Choice pressedChoice = LogisticsSlotDialog.Choice.NONE;

    LogisticsSlots(String memberId) {
        this.memberId = memberId;
        button.setIcon(SPRITES, LogisticsSchematic.ICON_PATH);
        held = LogisticsSchematic.held(PlayerFleet.cargoOrNull());
        refreshCounts();
    }

    void layout(ScreenRect besideButton, boolean shown, float mouseX, float mouseY) {
        refreshCounts();
        if (!shown || besideButton == ScreenRect.NONE) {
            button.hide();
            hovered = false;
            return;
        }
        button.place(besideButton.left() + besideButton.width() + BUTTON_GAP, besideButton.bottom(), button.preferredWidth(),
                ChromeMetrics.CONTROL_HEIGHT);
        boolean nowHovered = button.contains(mouseX, mouseY);
        if (nowHovered && !hovered) {
            refreshHeld();
        }
        hovered = nowHovered;
    }

    boolean contains(float x, float y) {
        return button.contains(x, y);
    }

    boolean press(float x, float y) {
        if (!button.isClickable(x, y)) {
            return false;
        }
        refreshHeld();
        if (held > 0) {
            SkillTreeSounds.panelOpened();
            dialog.open(cap, held);
        }
        return true;
    }

    void renderButton(float mouseX, float mouseY, float alphaMult, boolean live) {
        button.setEnabled(live);
        button.render(mouseX, mouseY, alphaMult);
    }

    String tooltipTitle() {
        return tooltipTitle;
    }

    String tooltipBody() {
        return tooltipBody;
    }

    boolean isDialogBlocking() {
        return dialog.isBlocking();
    }

    void advance(float amount) {
        dialog.advance(amount);
    }

    void handleModalEvent(InputEventAPI event) {
        if (!dialog.isOpen()) {
            return;
        }
        if (event.isLMBDownEvent()) {
            pressedChoice = dialog.choiceAt(event.getX(), event.getY());
        } else if (event.isLMBUpEvent()) {
            LogisticsSlotDialog.Choice released = dialog.choiceAt(event.getX(), event.getY());
            if (released == pressedChoice) {
                choose(released);
            }
            pressedChoice = LogisticsSlotDialog.Choice.NONE;
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            choose(LogisticsSlotDialog.Choice.CANCEL);
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_RETURN) {
            choose(LogisticsSlotDialog.Choice.CONFIRM);
        }
    }

    void renderDialog(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        dialog.render(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private void choose(LogisticsSlotDialog.Choice choice) {
        if (choice == LogisticsSlotDialog.Choice.CONFIRM && LogisticsSchematic.useOn(PlayerFleet.cargoOrNull(), ShipSkillDataManager.get(memberId))) {
            SkillTreeSounds.socketed();
            refreshHeld();
            refreshCounts();
        }
        if (choice != LogisticsSlotDialog.Choice.NONE) {
            dialog.close();
        }
    }

    private void refreshHeld() {
        int nowHeld = LogisticsSchematic.held(PlayerFleet.cargoOrNull());
        if (nowHeld != held) {
            held = nowHeld;
            tooltipBody = null;
        }
        rebuildTooltipBody();
    }

    private void refreshCounts() {
        ShipSkillData data = ShipSkillDataManager.get(memberId);
        if (data == shownData && data.revision() == shownRevision) {
            return;
        }
        shownData = data;
        shownRevision = data.revision();
        allocated = LogisticsNodes.allocated(data);
        cap = LogisticsNodes.cap(data);
        button.setLabel(Translation.msg("ui.logisticsSlots.button").arg("used", allocated).arg("cap", cap).text());
        button.setTextColor(allocated > cap ? SkillTreePanelStyle.NEGATIVE_STAT_COLOR : null);
        tooltipBody = null;
        rebuildTooltipBody();
    }

    private void rebuildTooltipBody() {
        if (tooltipBody != null) {
            return;
        }
        String item = LogisticsSchematic.INSTANCE.displayName();
        String countLine = Translation.msg("ui.logisticsSlots.body").arg("used", allocated).arg("cap", cap).text();
        String itemLine = held > 0 ? Translation.msg("ui.logisticsSlots.useHint").arg("item", item).arg("held", held).text()
                : Translation.msg("ui.logisticsSlots.noItems").arg("item", item).text();
        tooltipBody = countLine + "\n\n" + itemLine;
    }
}
