package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.ui.SkillTreeRefitButton;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import static exiledsector.ui.refit.UiReflection.call;

final class SkillTreeModsButton extends BaseCustomUIPanelPlugin {

    private static final Logger LOG = Logger.getLogger(SkillTreeModsButton.class);
    static final String BUTTON_ID = "exiledSector_skillTreeButton";
    static final int HOTKEY = Keyboard.KEY_K;
    static final float GAP_BELOW_BUILD_IN = 3f;
    static final float X_ALIGN_OFFSET = -5f;

    private static boolean failed;

    private ButtonAPI skillTreeButton;
    private boolean buttonShown = true;

    static void attach(Object modWidget) {
        if (failed || !(modWidget instanceof UIPanelAPI widget)) return;
        try {
            CustomPanelAPI attachedContainer = attached(widget);
            boolean buttonWanted = RefitButtonConfig.buttonUnderHullMods();
            if (attachedContainer != null) {
                if (attachedContainer.getPlugin() instanceof SkillTreeModsButton plugin) plugin.show(attachedContainer, buttonWanted);
                return;
            }
            if (!buttonWanted || !(call(widget, "getPerm") instanceof ButtonAPI buildIn)) return;
            PositionAPI buildInPosition = buildIn.getPosition();
            float buttonWidth = buildInPosition.getWidth();
            float buttonHeight = buildInPosition.getHeight();
            SkillTreeModsButton plugin = new SkillTreeModsButton();
            CustomPanelAPI container = Global.getSettings().createCustom(buttonWidth, buttonHeight, plugin);
            TooltipMakerAPI element = container.createUIElement(buttonWidth, buttonHeight, false);
            element.setButtonFontOrbitron20();
            ButtonAPI skillTreeButton = element.addButton(Translation.gameText("ui.refitButton"), BUTTON_ID,
                    Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, buttonHeight, 0f);
            skillTreeButton.setShortcut(HOTKEY, true);
            plugin.skillTreeButton = skillTreeButton;
            container.addUIElement(element);
            widget.addComponent(container).belowMid(buildIn, GAP_BELOW_BUILD_IN).setXAlignOffset(X_ALIGN_OFFSET);
        } catch (Throwable e) {
            disable(e);
        }
    }

    private void show(CustomPanelAPI container, boolean visible) {
        if (visible == buttonShown) return;
        buttonShown = visible;
        container.setOpacity(visible ? 1f : 0f);
        if (skillTreeButton != null) skillTreeButton.setEnabled(visible);
    }

    static void resetForTests() {
        failed = false;
    }

    private static CustomPanelAPI attached(UIPanelAPI widget) throws Throwable {
        for (Object child : UiReflection.children(widget)) {
            if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof SkillTreeModsButton) {
                return panel;
            }
        }
        return null;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Could not add the Skill Tree button to the refit screen; it stays off until the game restarts", e);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (!buttonShown || !BUTTON_ID.equals(buttonId)) return;
        try {
            SkillTreeRefitButton.openPanel(null);
        } catch (RuntimeException | LinkageError e) {
            disable(e);
        }
    }
}
