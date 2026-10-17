package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.socketables.HullUpgradesSetting;
import exiledsector.ui.HullUpgradesRefitButton;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

final class HullUpgradesModsButton extends BaseCustomUIPanelPlugin {

    private static final Logger LOG = Logger.getLogger(HullUpgradesModsButton.class);
    static final String BUTTON_ID = "exiledSector_hullUpgradesButton";
    static final int HOTKEY = Keyboard.KEY_L;
    static final float GAP_BELOW_SKILL_TREE = 3f;

    private static boolean failed;

    private ButtonAPI hullUpgradesButton;
    private boolean buttonShown = true;

    static boolean wanted() {
        return RefitButtonConfig.buttonUnderHullMods() && HullUpgradesSetting.enabled();
    }

    static void attachBelow(UIPanelAPI widget, CustomPanelAPI skillTreeContainer, float buttonWidth, float buttonHeight) {
        if (failed) return;
        try {
            HullUpgradesModsButton plugin = new HullUpgradesModsButton();
            CustomPanelAPI container = Global.getSettings().createCustom(buttonWidth, buttonHeight, plugin);
            TooltipMakerAPI element = container.createUIElement(buttonWidth, buttonHeight, false);
            element.setButtonFontOrbitron20();
            ButtonAPI button = element.addButton(Translation.gameText("ui.hullUpgradesRefitButton"), BUTTON_ID,
                    Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, buttonHeight, 0f);
            button.setShortcut(HOTKEY, true);
            plugin.hullUpgradesButton = button;
            container.addUIElement(element);
            widget.addComponent(container).belowMid(skillTreeContainer, GAP_BELOW_SKILL_TREE);
            plugin.show(container, wanted());
        } catch (Throwable e) {
            disable(e);
        }
    }

    static void updateVisibility(UIPanelAPI widget) {
        if (failed) return;
        try {
            for (Object child : UiReflection.children(widget)) {
                if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof HullUpgradesModsButton plugin) {
                    plugin.show(panel, wanted());
                    return;
                }
            }
        } catch (Throwable e) {
            disable(e);
        }
    }

    private void show(CustomPanelAPI container, boolean visible) {
        if (visible == buttonShown) return;
        buttonShown = visible;
        container.setOpacity(visible ? 1f : 0f);
        if (hullUpgradesButton != null) hullUpgradesButton.setEnabled(visible);
    }

    static void resetForTests() {
        failed = false;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Could not add the Hull Upgrades button to the refit screen; it stays off until the game restarts", e);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (!buttonShown || !BUTTON_ID.equals(buttonId)) return;
        try {
            HullUpgradesRefitButton.openPanel(null);
        } catch (RuntimeException | LinkageError e) {
            disable(e);
        }
    }
}
