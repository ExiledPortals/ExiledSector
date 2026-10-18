package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.ui.refit.RefitButtonConfig;
import exiledsector.ui.refit.RefitScreenLayout;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;

public class HullUpgradesRefitButton extends BaseRefitButton {

    static final String ICON_PATH = "graphics/icons/frameworks/framework_cruiser_unique.png";

    public static void addButton() {
        LunaRefitManager.addRefitButton(new HullUpgradesRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        if (!HullUpgradesSetting.enabled()) {
            return false;
        }
        return RefitPanelLauncher.open(HullUpgradesRefitButton.class, event, context -> {
            HullUpgradesScreen screen = new HullUpgradesScreen(context.member(), context.variant(), context.button(), RefitScreenLayout.measure());
            return new RefitPanelLauncher.Background(screen, screen::attach);
        });
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
        return Translation.gameText("ui.hullUpgradesRefitButton");
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return ICON_PATH;
    }

    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitButtonConfig.buttonUnderHullMods() && HullUpgradesSetting.enabled();
    }

    @Override
    public boolean isClickable(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return HullUpgradesSetting.enabled();
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return true;
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        if (backgroundPanel.getPlugin() instanceof HullUpgradesScreen) {
            return;
        }
        HullUpgradesScreen screen = new HullUpgradesScreen(member, variant, this, RefitScreenLayout.measure());
        PositionAPI backgroundPosition = backgroundPanel.getPosition();
        CustomPanelAPI screenPanel = Global.getSettings().createCustom(backgroundPosition.getWidth(), backgroundPosition.getHeight(), screen);
        backgroundPanel.addComponent(screenPanel).inTL(0f, 0f);
        screen.attach(screenPanel);
    }
}
