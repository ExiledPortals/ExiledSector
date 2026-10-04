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

    static void attach(Object modWidget) {
        if (failed || !(modWidget instanceof UIPanelAPI widget)) return;
        try {
            CustomPanelAPI attached = attached(widget);
            if (!RefitButtonConfig.buttonUnderHullMods()) {
                if (attached != null) widget.removeComponent(attached);
                return;
            }
            if (attached != null || !(call(widget, "getPerm") instanceof ButtonAPI buildIn)) return;
            PositionAPI buildInPosition = buildIn.getPosition();
            float width = buildInPosition.getWidth();
            float height = buildInPosition.getHeight();
            CustomPanelAPI container = Global.getSettings().createCustom(width, height, new SkillTreeModsButton());
            TooltipMakerAPI element = container.createUIElement(width, height, false);
            element.setButtonFontOrbitron20();
            ButtonAPI button = element.addButton(Translation.gameText("ui.refitButton"), BUTTON_ID,
                    Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, width, height, 0f);
            button.setShortcut(HOTKEY, true);
            container.addUIElement(element);
            widget.addComponent(container).belowMid(buildIn, GAP_BELOW_BUILD_IN).setXAlignOffset(X_ALIGN_OFFSET);
        } catch (Throwable e) {
            disable(e);
        }
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
        if (!BUTTON_ID.equals(buttonId)) return;
        try {
            SkillTreeRefitButton.openPanel(null);
        } catch (Throwable e) {
            disable(e);
        }
    }
}
