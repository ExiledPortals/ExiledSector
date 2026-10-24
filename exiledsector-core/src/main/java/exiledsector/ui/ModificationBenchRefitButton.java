package exiledsector.ui;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import lunalib.lunaRefit.LunaRefitManager;

public class ModificationBenchRefitButton extends FullScreenRefitButton {

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
    protected String buttonKey() {
        return "ui.benchRefitButton";
    }

    @Override
    protected String iconPath() {
        return ICON_PATH;
    }

    @Override
    public void initPanel(CustomPanelAPI backgroundPanel, FleetMemberAPI member, ShipVariantAPI variant, MarketAPI market) {
        if (backgroundPanel.getPlugin() instanceof ModificationBenchScreen) {
            return;
        }
        ModificationBenchScreen screen = new ModificationBenchScreen(new ModificationBenchScreen.RefitHost(this));
        fillWithScreen(backgroundPanel, screen, screen::attach);
    }
}
