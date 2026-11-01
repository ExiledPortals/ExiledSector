package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.refit.RefitButtonConfig;
import exiledsector.ui.refit.RefitModuleTabs;
import lunalib.lunaRefit.BaseRefitButton;

public abstract class FullScreenRefitButton extends BaseRefitButton {

    protected abstract String buttonKey();

    protected abstract String iconPath();

    protected boolean enabled() {
        return true;
    }

    protected Class<? extends CustomUIPanelPlugin> screenClass() {
        return null;
    }

    protected RefitPanelLauncher.Background createScreen(FleetMemberAPI member, ShipVariantAPI variant) {
        return null;
    }

    protected static boolean openScreen(Class<? extends FullScreenRefitButton> buttonClass, InputEventAPI event) {
        return RefitPanelLauncher.open(buttonClass, event,
                context -> ((FullScreenRefitButton) context.button()).createScreen(context.member(), context.variant()));
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        Class<? extends CustomUIPanelPlugin> openScreenClass = screenClass();
        if (openScreenClass == null || openScreenClass.isInstance(backgroundPanel.getPlugin())) {
            return;
        }
        RefitPanelLauncher.Background screen = createScreen(member, variant);
        PositionAPI backgroundPosition = backgroundPanel.getPosition();
        CustomPanelAPI screenPanel = Global.getSettings().createCustom(backgroundPosition.getWidth(), backgroundPosition.getHeight(), screen.plugin());
        backgroundPanel.addComponent(screenPanel).inTL(0f, 0f);
        screen.attached().accept(screenPanel);
    }

    @Override
    public float getPanelWidth(FleetMemberAPI member, ShipVariantAPI variant) {
        return Global.getSettings().getScreenWidth();
    }

    @Override
    public float getPanelHeight(FleetMemberAPI member, ShipVariantAPI variant) {
        return Global.getSettings().getScreenHeight();
    }

    @Override
    public String getButtonName(FleetMemberAPI member, ShipVariantAPI variant) {
        return Translation.gameText(buttonKey());
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return iconPath();
    }

    protected boolean opensOnModuleTab(FleetMemberAPI member, ShipVariantAPI variant) {
        return false;
    }

    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitButtonConfig.buttonUnderHullMods() && enabled() && hasPanel(member, variant, market);
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitModuleTabs.isModuleTab(member) || opensOnModuleTab(member, variant);
    }
}
