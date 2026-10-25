package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.effects.TransientScripts;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

import java.util.function.Function;

public final class CampaignOverlay {

    public interface Host {

        void close();

        void leave(String wormholeMessage);
    }

    public interface Screen extends CustomUIPanelPlugin {

        void attach(CustomPanelAPI panel);
    }

    private static final Logger LOG = Logger.getLogger(CampaignOverlay.class);
    private static final String CLICK_SOUND = "ui_button_pressed";

    private CampaignOverlay() {
    }

    // java:S1181: UiReflection declares Throwable, and a changed engine may only cost this overlay, never the game
    @SuppressWarnings("java:S1181")
    public static boolean open(CampaignUIAPI campaignUI, Function<Host, Screen> screenFor, Runnable afterClose) {
        try {
            UIPanelAPI hostPanel = hostPanel(campaignUI);
            if (hostPanel == null || overlayOpenIn(hostPanel)) return false;
            OverlayHost host = new OverlayHost(hostPanel, afterClose);
            Screen screen = screenFor.apply(host);
            CustomPanelAPI panel = Global.getSettings().createCustom(Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), screen);
            host.overlayPanel = panel;
            hostPanel.addComponent(panel).inTL(0f, 0f);
            screen.attach(panel);
            SkillTreeSounds.play(CLICK_SOUND);
            return true;
        } catch (Throwable e) {
            LOG.error("[ExiledSector] Could not open a full-screen window over the campaign screens", e);
            return false;
        }
    }

    static UIPanelAPI hostPanel(CampaignUIAPI campaignUI) throws Throwable {
        if (campaignUI == null) return null;
        if (campaignUI.getCurrentInteractionDialog() instanceof UIPanelAPI dialogPanel) return dialogPanel;
        return UiReflection.call(campaignUI, "getCore") instanceof UIPanelAPI corePanel ? corePanel : null;
    }

    private static boolean overlayOpenIn(UIPanelAPI hostPanel) throws Throwable {
        for (Object child : UiReflection.children(hostPanel)) {
            if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof Screen) return true;
        }
        return false;
    }

    private static final class OverlayHost implements Host {

        private final UIPanelAPI hostPanel;
        private final Runnable afterClose;
        private CustomPanelAPI overlayPanel;

        private OverlayHost(UIPanelAPI hostPanel, Runnable afterClose) {
            this.hostPanel = hostPanel;
            this.afterClose = afterClose;
        }

        @Override
        public void close() {
            TransientScripts.nextFrame(Global.getSector(), () -> remove(null));
        }

        @Override
        public void leave(String wormholeMessage) {
            TransientScripts.nextFrame(Global.getSector(), () -> remove(wormholeMessage));
        }

        private void remove(String wormholeMessage) {
            if (overlayPanel != null) {
                hostPanel.removeComponent(overlayPanel);
            }
            if (afterClose != null) {
                afterClose.run();
            }
            if (wormholeMessage != null) {
                RefitScreenExit.leave(null, wormholeMessage);
            }
        }
    }
}
