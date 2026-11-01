package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.SocketCustody;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.socket.ModificationBay;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.util.GLDraw;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class ModificationBenchScreen extends BaseCustomUIPanelPlugin implements CampaignOverlay.Screen {

    static final float SCREEN_MARGIN = 40f;
    private static final float OPEN_SECONDS = 0.2f;
    private static final float BACKDROP_ALPHA = 0.8f;

    record RefitHost(BaseRefitButton refitButton) implements CampaignOverlay.Host {
        @Override
        public void close() {
            refitButton.closePanel();
        }

        @Override
        public void leave(String wormholeMessage) {
            RefitScreenExit.leave(refitButton, wormholeMessage);
        }
    }

    private final CampaignOverlay.Host host;
    private final BenchMode benchMode;
    private final BenchModeSupport.OpenTarget openTarget;
    private final Consumer<String> opened;
    private CustomPanelAPI hostPanel;
    private ModificationBay bay;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private final OverlayFade fade = new OverlayFade(OPEN_SECONDS);
    private boolean bayOpened;

    ModificationBenchScreen(CampaignOverlay.Host host) {
        this(host, null, null, null);
    }

    ModificationBenchScreen(CampaignOverlay.Host host, BenchMode benchMode, BenchModeSupport.OpenTarget openTarget, Consumer<String> opened) {
        this.host = host;
        this.benchMode = benchMode;
        this.openTarget = openTarget;
        this.opened = opened;
    }

    @Override
    public void attach(CustomPanelAPI panel) {
        hostPanel = panel;
        ownedShips = SocketCustody.reconcile();
    }

    static SocketPanelPlacement benchArea(float screenWidth, float screenHeight, boolean instantly) {
        return new SocketPanelPlacement(SCREEN_MARGIN, SCREEN_MARGIN, screenWidth - SCREEN_MARGIN * 2f, screenHeight - SCREEN_MARGIN * 2f, instantly);
    }

    @Override
    public void advance(float amount) {
        if (hostPanel == null) {
            return;
        }
        boolean finishedClosing = fade.advance(amount);
        if (fade.fullyOpen() && !bayOpened) {
            bayOpened = true;
            SkillTreeSounds.panelOpened();
            SocketPanelPlacement area = benchArea(Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), false);
            ModificationBay.Listener bayListener = new ModificationBay.Listener() {
                @Override
                public void closed() {
                    fade.close();
                }

                @Override
                public void pressedInside() {
                }

                @Override
                public void wormholeOpened(String message) {
                    if (opened != null) {
                        opened.accept(message);
                    }
                }
            };
            bay = benchMode == null ? ModificationBay.open(hostPanel, area, SocketCustody.shipNames(ownedShips), bayListener)
                    : ModificationBay.openMode(hostPanel, area, benchMode, SocketCustody.shipNames(ownedShips), openTarget, bayListener);
        }
        if (finishedClosing) {
            host.close();
        }
    }

    private void close() {
        if (bay != null && bay.isOpen()) {
            bay.close();
        }
        fade.close();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (!fade.closing() && (bay == null || !bay.escape())) {
                    close();
                }
            } else if (event.isLMBDownEvent() && !fade.closing() && (bay == null || !bay.contains(event.getX(), event.getY()))) {
                close();
            }
            event.consume();
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (hostPanel == null) {
            return;
        }
        GLDraw.fillQuad(0f, 0f, Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), Color.BLACK,
                BACKDROP_ALPHA * fade.smoothLevel() * alphaMult);
    }
}
