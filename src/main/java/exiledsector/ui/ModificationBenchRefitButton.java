package exiledsector.ui;

import com.fs.starfarer.api.Global;
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
import lunalib.lunaRefit.LunaRefitManager;

public class ModificationBenchRefitButton extends BaseRefitButton {

    static final String ICON_PATH = "graphics/icons/cargo/nanoswarm_augmentation.png";

    public static void addButton() {
        LunaRefitManager.addRefitButton(new ModificationBenchRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        return RefitPanelLauncher.open(ModificationBenchRefitButton.class, event, context -> {
            ModificationBenchScreen screen = new ModificationBenchScreen(new ModificationBenchScreen.RefitHost(context.button()));
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
        return Translation.gameText("ui.benchRefitButton");
    }

    @Override
    public String getIconName(FleetMemberAPI member, ShipVariantAPI variant) {
        return ICON_PATH;
    }

    @Override
    public boolean shouldShow(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitButtonConfig.buttonUnderHullMods() && !RefitModuleTabs.isModuleTab(member);
    }

    @Override
    public boolean hasPanel(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return !RefitModuleTabs.isModuleTab(member);
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        if (backgroundPanel.getPlugin() instanceof ModificationBenchScreen) {
            return;
        }
        ModificationBenchScreen screen = new ModificationBenchScreen(new ModificationBenchScreen.RefitHost(this));
        PositionAPI backgroundPosition = backgroundPanel.getPosition();
        CustomPanelAPI screenPanel = Global.getSettings().createCustom(backgroundPosition.getWidth(), backgroundPosition.getHeight(), screen);
        backgroundPanel.addComponent(screenPanel).inTL(0f, 0f);
        screen.attach(screenPanel);
    }
}
