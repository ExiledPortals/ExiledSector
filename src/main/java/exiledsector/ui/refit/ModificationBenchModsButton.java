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
import exiledsector.ui.ModificationBenchRefitButton;
import org.apache.log4j.Logger;

final class ModificationBenchModsButton extends BaseCustomUIPanelPlugin {

    private static final Logger LOG = Logger.getLogger(ModificationBenchModsButton.class);
    static final String BUTTON_ID = "exiledSector_modificationBenchButton";
    static final float GAP_BELOW_HULL_ITEMS = 3f;

    private static boolean failed;

    private ButtonAPI benchButton;
    private boolean buttonShown = true;

    static boolean wanted() {
        return RefitButtonConfig.buttonUnderHullMods();
    }

    static void attachBelow(UIPanelAPI widget, CustomPanelAPI hullItemsContainer, float buttonWidth, float buttonHeight) {
        if (failed) return;
        try {
            ModificationBenchModsButton plugin = new ModificationBenchModsButton();
            CustomPanelAPI container = Global.getSettings().createCustom(buttonWidth, buttonHeight, plugin);
            TooltipMakerAPI element = container.createUIElement(buttonWidth, buttonHeight, false);
            element.setButtonFontOrbitron20();
            ButtonAPI button = element.addButton(Translation.gameText("ui.benchRefitButton"), BUTTON_ID,
                    Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, buttonHeight, 0f);
            int hotkey = RefitButtonConfig.benchKey();
            if (hotkey > 0) button.setShortcut(hotkey, true);
            plugin.benchButton = button;
            container.addUIElement(element);
            widget.addComponent(container).belowMid(hullItemsContainer, GAP_BELOW_HULL_ITEMS);
            plugin.show(container, wanted());
        } catch (Throwable e) {
            disable(e);
        }
    }

    static void updateVisibility(UIPanelAPI widget) {
        if (failed) return;
        try {
            for (Object child : UiReflection.children(widget)) {
                if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof ModificationBenchModsButton plugin) {
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
        if (benchButton != null) benchButton.setEnabled(visible);
    }

    static void resetForTests() {
        failed = false;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Could not add the Modification Bench button to the refit screen; it stays off until the game restarts", e);
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (!buttonShown || !BUTTON_ID.equals(buttonId)) return;
        try {
            ModificationBenchRefitButton.openPanel(null);
        } catch (RuntimeException | LinkageError e) {
            disable(e);
        }
    }
}
