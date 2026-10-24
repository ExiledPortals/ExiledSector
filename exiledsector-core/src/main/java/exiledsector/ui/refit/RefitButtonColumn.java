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
import exiledsector.domain.DomainContributions;
import exiledlib.i18n.Translation;
import exiledsector.ui.ModificationBenchRefitButton;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
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
    static final float HIDDEN_OFFSET = -10000f;
    private static final Logger LOG = Logger.getLogger(RefitButtonColumn.class);
    private static final DomainContributions<ButtonSpec> DOMAIN_BUTTONS = DomainContributions.fromEveryDomain(domain -> domain.refitButtons().stream()
            .map(button -> new ButtonSpec(button.id(), button.labelKey(), button.hotkey(),
                    () -> domain.enabled() && button.wanted().getAsBoolean(), button.press()))
            .toList());

    private static boolean failed;

    private RefitButtonColumn() {
    }

    static List<ButtonSpec> buttons() {
        List<ButtonSpec> buttons = new ArrayList<>(DOMAIN_BUTTONS.all());
        buttons.add(BENCH_BUTTON);
        return buttons;
    }

    public static void attach(Object modWidget) {
        if (failed || !(modWidget instanceof UIPanelAPI widget)) return;
        try {
            List<CustomPanelAPI> attachedContainers = attached(widget);
            if (!attachedContainers.isEmpty()) {
                boolean changed = false;
                for (CustomPanelAPI container : attachedContainers) {
                    changed |= ((ColumnButton) container.getPlugin()).showWanted(container);
                }
                if (changed && call(widget, "getPerm") instanceof ButtonAPI buildIn) {
                    stack(attachedContainers, buildIn);
                }
                return;
            }
            if (!RefitButtonConfig.buttonUnderHullMods() || !(call(widget, "getPerm") instanceof ButtonAPI buildIn)) return;
            PositionAPI buildInPosition = buildIn.getPosition();
            float buttonWidth = buildInPosition.getWidth();
            float buttonHeight = buildInPosition.getHeight();
            List<CustomPanelAPI> containers = new ArrayList<>();
            for (ButtonSpec button : buttons()) {
                CustomPanelAPI container = create(button, buttonWidth, buttonHeight);
                ColumnButton plugin = (ColumnButton) container.getPlugin();
                plugin.placement = widget.addComponent(container);
                plugin.showWanted(container);
                containers.add(container);
            }
            stack(containers, buildIn);
        } catch (Throwable e) {
            disable(e);
        }
    }

    static void stack(List<CustomPanelAPI> containers, ButtonAPI buildIn) {
        CustomPanelAPI above = null;
        for (CustomPanelAPI container : containers) {
            ColumnButton plugin = (ColumnButton) container.getPlugin();
            if (plugin.placement == null || !plugin.buttonShown) continue;
            if (above == null) {
                plugin.placement.belowMid(buildIn, gapBelowBuildIn(buildIn.getPosition().getHeight())).setXAlignOffset(X_ALIGN_OFFSET);
            } else {
                plugin.placement.belowMid(above, GAP_BETWEEN_BUTTONS);
            }
            above = container;
        }
        if (above == null) return;
        for (CustomPanelAPI container : containers) {
            ColumnButton plugin = (ColumnButton) container.getPlugin();
            if (plugin.placement != null && !plugin.buttonShown) plugin.placement.inTL(HIDDEN_OFFSET, HIDDEN_OFFSET);
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
        private PositionAPI placement;
        private boolean buttonShown = true;

        ColumnButton(ButtonSpec button) {
            this.button = button;
        }

        ButtonSpec button() {
            return button;
        }

        boolean showWanted(CustomPanelAPI container) {
            boolean visible = button.wanted().getAsBoolean();
            if (visible == buttonShown) return false;
            buttonShown = visible;
            container.setOpacity(visible ? 1f : 0f);
            if (buttonApi != null) buttonApi.setEnabled(visible);
            return true;
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
