package exiledsector.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

public final class ModificationBenchCargoLauncher {

    private static final Logger LOG = Logger.getLogger(ModificationBenchCargoLauncher.class);
    private static final String CLICK_SOUND = "ui_button_pressed";

    private ModificationBenchCargoLauncher() {
    }

    // java:S1181: UiReflection declares Throwable, and a changed engine may only cost the inventory Crafting button, never the game
    @SuppressWarnings("java:S1181")
    public static boolean open(CampaignUIAPI campaignUI, Runnable afterClose) {
        try {
            UIPanelAPI hostPanel = hostPanel(campaignUI);
            if (hostPanel == null || benchOpenIn(hostPanel)) return false;
            CargoHost host = new CargoHost(hostPanel, afterClose);
            ModificationBenchScreen screen = new ModificationBenchScreen(host);
            CustomPanelAPI panel = Global.getSettings().createCustom(Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), screen);
            host.benchPanel = panel;
            hostPanel.addComponent(panel).inTL(0f, 0f);
            screen.attach(panel);
            SkillTreeSounds.play(CLICK_SOUND);
            return true;
        } catch (Throwable e) {
            LOG.error("[ExiledSector] Could not open the Crafting window from the inventory screen", e);
            return false;
        }
    }

    static UIPanelAPI hostPanel(CampaignUIAPI campaignUI) throws Throwable {
        if (campaignUI == null) return null;
        if (campaignUI.getCurrentInteractionDialog() instanceof UIPanelAPI dialogPanel) return dialogPanel;
        return UiReflection.call(campaignUI, "getCore") instanceof UIPanelAPI corePanel ? corePanel : null;
    }

    private static boolean benchOpenIn(UIPanelAPI hostPanel) throws Throwable {
        for (Object child : UiReflection.children(hostPanel)) {
            if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof ModificationBenchScreen) return true;
        }
        return false;
    }

    private static final class CargoHost implements ModificationBenchScreen.Host {

        private final UIPanelAPI hostPanel;
        private final Runnable afterClose;
        private CustomPanelAPI benchPanel;

        private CargoHost(UIPanelAPI hostPanel, Runnable afterClose) {
            this.hostPanel = hostPanel;
            this.afterClose = afterClose;
        }

        @Override
        public void close() {
            Global.getSector().addTransientScript(new PanelRemoval(this, null));
        }

        @Override
        public void leave(String wormholeMessage) {
            Global.getSector().addTransientScript(new PanelRemoval(this, wormholeMessage));
        }
    }

    static final class PanelRemoval implements EveryFrameScript {

        private final CargoHost host;
        private final String wormholeMessage;
        private boolean done;

        private PanelRemoval(CargoHost host, String wormholeMessage) {
            this.host = host;
            this.wormholeMessage = wormholeMessage;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if (done) return;
            done = true;
            if (host.benchPanel != null) {
                host.hostPanel.removeComponent(host.benchPanel);
            }
            if (host.afterClose != null) {
                host.afterClose.run();
            }
            if (wormholeMessage != null) {
                RefitScreenExit.leave(null, wormholeMessage);
            }
        }
    }
}
