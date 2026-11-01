package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import lunalib.lunaRefit.LunaRefitManager;

public class ModificationBenchRefitButton extends FullScreenRefitButton {

    public static final String BUTTON_KEY = "ui.benchRefitButton";
    static final String ICON_PATH = "graphics/icons/cargo/nanoswarm_augmentation.png";

    public static void addButton() {
        LunaRefitManager.addRefitButton(new ModificationBenchRefitButton());
    }

    public static boolean openPanel(InputEventAPI event) {
        return openScreen(ModificationBenchRefitButton.class, event);
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
    protected Class<ModificationBenchScreen> screenClass() {
        return ModificationBenchScreen.class;
    }

    @Override
    protected RefitPanelLauncher.Background createScreen(FleetMemberAPI member, ShipVariantAPI variant) {
        ModificationBenchScreen screen = new ModificationBenchScreen(new ModificationBenchScreen.RefitHost(this));
        return new RefitPanelLauncher.Background(screen, screen::attach);
    }
}
