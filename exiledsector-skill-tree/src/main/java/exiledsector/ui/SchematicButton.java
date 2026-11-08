package exiledsector.ui;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Translation;
import exiledsector.items.ShipSchematic;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.render.SpriteCache;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

abstract class SchematicButton {

    private static final SpriteCache SPRITES = new SpriteCache(SchematicButton.class);
    private static final float BUTTON_GAP = 8f;

    private final String memberId;
    private final ShipSchematic schematic;
    private final SkillTreeUiButton button = new SkillTreeUiButton("");
    private final SchematicDialog dialog = new SchematicDialog();
    private final String tooltipTitle;
    private ShipSkillData shownData;
    private int shownRevision = -1;
    private int held = -1;
    private boolean hovered;
    private String tooltipBody;
    private SchematicDialog.Choice pressedChoice = SchematicDialog.Choice.NONE;

    SchematicButton(String memberId, ShipSchematic schematic, String iconPath, String tooltipTitle) {
        this.memberId = memberId;
        this.schematic = schematic;
        this.tooltipTitle = tooltipTitle;
        button.setIcon(SPRITES, iconPath);
    }

    abstract int heldIn(CargoAPI cargo);

    abstract void read(ShipSkillData data);

    abstract String label();

    Color labelColor() {
        return null;
    }

    abstract String tooltipBody(int held, String itemName);

    abstract boolean canUse();

    abstract String dialogBody(int held);

    abstract boolean use(CargoAPI cargo, ShipSkillData data);

    final ScreenRect layout(ScreenRect besideButton, boolean shown, float mouseX, float mouseY) {
        refreshCounts();
        if (held < 0) {
            refreshHeld();
        }
        if (!shown || besideButton == ScreenRect.NONE) {
            button.hide();
            hovered = false;
            return ScreenRect.NONE;
        }
        button.place(besideButton.left() + besideButton.width() + BUTTON_GAP, besideButton.bottom(), button.preferredWidth(),
                ChromeMetrics.CONTROL_HEIGHT);
        boolean nowHovered = button.contains(mouseX, mouseY);
        if (nowHovered && !hovered) {
            refreshHeld();
        }
        hovered = nowHovered;
        return button.bounds();
    }

    final boolean contains(float x, float y) {
        return button.contains(x, y);
    }

    final boolean press(float x, float y) {
        if (!button.isClickable(x, y)) {
            return false;
        }
        refreshHeld();
        if (held > 0 && canUse()) {
            SkillTreeSounds.panelOpened();
            dialog.open(Translation.msg("ui.schematicDialog.title").arg("item", schematic.displayName()).text(), dialogBody(held));
        }
        return true;
    }

    final void renderButton(float mouseX, float mouseY, float alphaMult, boolean live) {
        button.setEnabled(live);
        button.render(mouseX, mouseY, alphaMult);
    }

    final String tooltipTitle() {
        return tooltipTitle;
    }

    final String tooltipBody() {
        return tooltipBody;
    }

    final boolean isDialogBlocking() {
        return dialog.isBlocking();
    }

    final void advance(float amount) {
        dialog.advance(amount);
    }

    final void handleModalEvent(InputEventAPI event) {
        if (!dialog.isOpen()) {
            return;
        }
        if (event.isLMBDownEvent()) {
            pressedChoice = dialog.choiceAt(event.getX(), event.getY());
        } else if (event.isLMBUpEvent()) {
            SchematicDialog.Choice released = dialog.choiceAt(event.getX(), event.getY());
            if (released == pressedChoice) {
                choose(released);
            }
            pressedChoice = SchematicDialog.Choice.NONE;
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
            choose(SchematicDialog.Choice.CANCEL);
        } else if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_RETURN) {
            choose(SchematicDialog.Choice.CONFIRM);
        }
    }

    final void renderDialog(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        dialog.render(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private void choose(SchematicDialog.Choice choice) {
        if (choice == SchematicDialog.Choice.CONFIRM && use(PlayerFleet.cargoOrNull(), ShipSkillDataManager.get(memberId))) {
            SkillTreeSounds.socketed();
            refreshHeld();
            refreshCounts();
        }
        if (choice != SchematicDialog.Choice.NONE) {
            dialog.close();
        }
    }

    private void refreshHeld() {
        int nowHeld = heldIn(PlayerFleet.cargoOrNull());
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
        read(data);
        button.setLabel(label());
        button.setTextColor(labelColor());
        tooltipBody = null;
        rebuildTooltipBody();
    }

    private void rebuildTooltipBody() {
        if (tooltipBody == null) {
            tooltipBody = tooltipBody(Math.max(0, held), schematic.displayName());
        }
    }
}
