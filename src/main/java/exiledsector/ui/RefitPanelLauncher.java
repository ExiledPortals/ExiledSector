package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import lunalib.backend.ui.refit.RefitButtonAdder;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;

import java.util.function.Consumer;
import java.util.function.Function;

final class RefitPanelLauncher {

    record Background(CustomUIPanelPlugin plugin, Consumer<CustomPanelAPI> attached) {
    }

    record Context(UIPanelAPI corePanel, FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton button) {
    }

    private static final String CLICK_SOUND = "ui_button_pressed";

    private RefitPanelLauncher() {
    }

    static boolean open(Class<? extends BaseRefitButton> buttonClass, InputEventAPI event, Function<Context, Background> backgroundFor) {
        RefitButtonAdder adder = refitButtonAdder();
        if (adder == null || adder.getActivePanel() != null || adder.getCorePanel() == null) return false;
        BaseRefitButton button = LunaRefitManager.getFirstButtonOfClass(buttonClass);
        if (button == null) return false;
        FleetMemberAPI member = adder.getMember();
        ShipVariantAPI variant = adder.getVariant();
        MarketAPI market = adder.getMarket();
        if (member == null || variant == null || !button.isClickable(member, variant, market) || !button.hasPanel(member, variant, market)) {
            return false;
        }
        SkillTreeSounds.play(CLICK_SOUND);
        button.onClick(member, variant, event, market);

        UIPanelAPI corePanel = adder.getCorePanel();
        float panelWidth = button.getPanelWidth(member, variant);
        float panelHeight = button.getPanelHeight(member, variant);
        Background background = backgroundFor.apply(new Context(corePanel, member, variant, button));
        CustomPanelAPI panel = Global.getSettings().createCustom(panelWidth, panelHeight, background.plugin());
        adder.setActivePanel(panel);
        adder.setActivePanelButton(button);
        panel.getPosition().inTL(Global.getSettings().getScreenWidth() / 2f - panelWidth / 2f, Global.getSettings().getScreenHeight() / 2f - panelHeight / 2f);
        try {
            background.attached().accept(panel);
            corePanel.addComponent(panel);
            button.initPanel(panel, member, variant, market);
        } catch (RuntimeException | LinkageError e) {
            corePanel.removeComponent(panel);
            adder.setActivePanel(null);
            adder.setActivePanelButton(null);
            throw e;
        }
        return true;
    }

    private static RefitButtonAdder refitButtonAdder() {
        for (Object script : Global.getSector().getTransientScripts()) {
            if (script instanceof RefitButtonAdder adder) {
                return adder;
            }
        }
        return null;
    }
}
