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
import exiledsector.compat.ExoticaCompat;
import exiledsector.i18n.Translation;
import exiledsector.ui.ModificationBenchRefitButton;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import static exiledsector.ui.refit.UiReflection.call;

public final class RefitButtonColumn {

    public record ButtonSpec(String id, String labelKey, IntSupplier hotkey, BooleanSupplier wanted, Runnable press) {
    }

    static final ButtonSpec BENCH_BUTTON = new ButtonSpec("exiledSector_modificationBenchButton", "ui.benchRefitButton",
            RefitButtonConfig::benchKey, RefitButtonConfig::buttonUnderHullMods, () -> ModificationBenchRefitButton.openPanel(null));
    static final float GAP_BELOW_BUILD_IN = 3f;
    static final float GAP_BETWEEN_BUTTONS = 3f;
    static final float X_ALIGN_OFFSET = -5f;
    private static final Logger LOG = Logger.getLogger(RefitButtonColumn.class);
    private static final List<ButtonSpec> DOMAIN_BUTTONS = new CopyOnWriteArrayList<>();

    private static boolean failed;

    private RefitButtonColumn() {
    }

    public static void register(ButtonSpec button) {
        if (!DOMAIN_BUTTONS.contains(button)) {
            DOMAIN_BUTTONS.add(button);
        }
    }

    static List<ButtonSpec> buttons() {
        List<ButtonSpec> buttons = new ArrayList<>(DOMAIN_BUTTONS);
        buttons.add(BENCH_BUTTON);
        return buttons;
    }

    public static void attach(Object modWidget) {
        if (failed || !(modWidget instanceof UIPanelAPI widget)) return;
        try {
            List<CustomPanelAPI> attachedContainers = attached(widget);
            if (!attachedContainers.isEmpty()) {
                attachedContainers.forEach(container -> ((ColumnButton) container.getPlugin()).showWanted(container));
                return;
            }
            if (!RefitButtonConfig.buttonUnderHullMods() || !(call(widget, "getPerm") instanceof ButtonAPI buildIn)) return;
            PositionAPI buildInPosition = buildIn.getPosition();
            float buttonWidth = buildInPosition.getWidth();
            float buttonHeight = buildInPosition.getHeight();
            CustomPanelAPI above = null;
            for (ButtonSpec button : buttons()) {
                CustomPanelAPI container = create(button, buttonWidth, buttonHeight);
                if (above == null) {
                    widget.addComponent(container).belowMid(buildIn, gapBelowBuildIn(buttonHeight)).setXAlignOffset(X_ALIGN_OFFSET);
                } else {
                    widget.addComponent(container).belowMid(above, GAP_BETWEEN_BUTTONS);
                }
                ((ColumnButton) container.getPlugin()).showWanted(container);
                above = container;
            }
        } catch (Throwable e) {
            disable(e);
        }
    }

    static float gapBelowBuildIn(float buttonHeight) {
        return ExoticaCompat.isModEnabled() ? GAP_BELOW_BUILD_IN + buttonHeight + GAP_BELOW_BUILD_IN : GAP_BELOW_BUILD_IN;
    }

    private static CustomPanelAPI create(ButtonSpec button, float buttonWidth, float buttonHeight) {
        ColumnButton plugin = new ColumnButton(button);
        CustomPanelAPI container = Global.getSettings().createCustom(buttonWidth, buttonHeight, plugin);
        TooltipMakerAPI element = container.createUIElement(buttonWidth, buttonHeight, false);
        element.setButtonFontOrbitron20();
        ButtonAPI buttonApi = element.addButton(Translation.gameText(button.labelKey()), button.id(), Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM, buttonWidth, buttonHeight, 0f);
        int hotkey = button.hotkey().getAsInt();
        if (hotkey > 0) buttonApi.setShortcut(hotkey, true);
        plugin.buttonApi = buttonApi;
        container.addUIElement(element);
        return container;
    }

    private static List<CustomPanelAPI> attached(UIPanelAPI widget) throws Throwable {
        List<CustomPanelAPI> containers = new ArrayList<>();
        for (Object child : UiReflection.children(widget)) {
            if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof ColumnButton) {
                containers.add(panel);
            }
        }
        return containers;
    }

    static void resetForTests() {
        failed = false;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Could not add the buttons under the hull mods in the refit screen; they stay off until the game restarts", e);
    }

    static final class ColumnButton extends BaseCustomUIPanelPlugin {

        private final ButtonSpec button;
        private ButtonAPI buttonApi;
        private boolean buttonShown = true;

        ColumnButton(ButtonSpec button) {
            this.button = button;
        }

        ButtonSpec button() {
            return button;
        }

        void showWanted(CustomPanelAPI container) {
            boolean visible = button.wanted().getAsBoolean();
            if (visible == buttonShown) return;
            buttonShown = visible;
            container.setOpacity(visible ? 1f : 0f);
            if (buttonApi != null) buttonApi.setEnabled(visible);
        }

        @Override
        public void buttonPressed(Object buttonId) {
            if (!buttonShown || !button.id().equals(buttonId)) return;
            try {
                button.press().run();
            } catch (RuntimeException | LinkageError e) {
                disable(e);
            }
        }
    }
}
