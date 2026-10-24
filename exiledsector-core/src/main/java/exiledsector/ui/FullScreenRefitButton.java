package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.i18n.Translation;
import exiledsector.ui.refit.RefitButtonConfig;
import exiledsector.ui.refit.RefitModuleTabs;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.function.Consumer;

public abstract class FullScreenRefitButton extends BaseRefitButton {

    protected abstract String buttonKey();

    protected abstract String iconPath();

    protected boolean enabled() {
        return true;
    }

    protected static void fillWithScreen(CustomPanelAPI backgroundPanel, CustomUIPanelPlugin screen, Consumer<CustomPanelAPI> attach) {
        PositionAPI backgroundPosition = backgroundPanel.getPosition();
        CustomPanelAPI screenPanel = Global.getSettings().createCustom(backgroundPosition.getWidth(), backgroundPosition.getHeight(), screen);
        backgroundPanel.addComponent(screenPanel).inTL(0f, 0f);
        attach.accept(screenPanel);
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

    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitButtonConfig.buttonUnderHullMods() && enabled() && !RefitModuleTabs.isModuleTab(member);
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitModuleTabs.isModuleTab(member);
    }
}
