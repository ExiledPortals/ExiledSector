package exiledsector.ui;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.refit.RefitButtonConfig;
import exiledsector.ui.refit.RefitScreenLayout;
import lunalib.lunaRefit.LunaRefitManager;

public class HullItemsRefitButton extends FullScreenRefitButton {

    static final String BUTTON_KEY = "ui.hullItemsRefitButton";
    public static final RefitButtonColumn.ButtonSpec COLUMN_BUTTON = new RefitButtonColumn.ButtonSpec("exiledSector_hullUpgradesButton",
            BUTTON_KEY, RefitButtonConfig::hullItemsKey, RefitButtonConfig::buttonUnderHullMods, () -> openPanel(null));
    static final String ICON_PATH = "graphics/icons/hull_upgrades/hull_upgrade_cruiser_unique.png";

    public static void addButton() {
        LunaRefitManager.addRefitButton(new HullItemsRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        return HullUpgradesSetting.enabled() && openScreen(HullItemsRefitButton.class, event);
    }

    @Override
    protected String buttonKey() {
        return BUTTON_KEY;
    }

    @Override
    protected String iconPath() {
        return ICON_PATH;
    }

    @Override
    protected boolean enabled() {
        return HullUpgradesSetting.enabled();
    }

    @Override
    public boolean isClickable(FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        return enabled();
    }

    @Override
    protected Class<HullItemsScreen> screenClass() {
        return HullItemsScreen.class;
    }

    @Override
    protected RefitPanelLauncher.Background createScreen(FleetMemberAPI member, ShipVariantAPI variant) {
        HullItemsScreen screen = new HullItemsScreen(member, variant, this, RefitScreenLayout.measure());
        return new RefitPanelLauncher.Background(screen, screen::attach);
    }
}
