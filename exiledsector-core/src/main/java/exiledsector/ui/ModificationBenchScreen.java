package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.items.SocketCustody;
import exiledsector.ui.socket.ModificationBay;
import exiledsector.ui.socket.SocketPanelPlacement;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.GLDraw;
import lunalib.lunaRefit.BaseRefitButton;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.List;
import java.util.Map;

final class ModificationBenchScreen extends BaseCustomUIPanelPlugin {

    static final float SCREEN_MARGIN = 40f;
    private static final float OPEN_SECONDS = 0.2f;
    private static final float BACKDROP_ALPHA = 0.8f;

    interface Host {
        void close();

        void leave(String wormholeMessage);
    }

    record RefitHost(BaseRefitButton refitButton) implements Host {
        @Override
        public void close() {
            refitButton.closePanel();
        }

        @Override
        public void leave(String wormholeMessage) {
            RefitScreenExit.leave(refitButton, wormholeMessage);
        }
    }

    private final Host host;
    private final boolean starChartsOnly;
    private CustomPanelAPI hostPanel;
    private ModificationBay bay;
    private Map<String, FleetMemberAPI> ownedShips = Map.of();
    private float openLevel;
    private boolean bayOpened;
    private boolean closing;
    private boolean closeRequested;
    private String wormholeMessage;

    ModificationBenchScreen(Host host) {
        this(host, false);
    }

    ModificationBenchScreen(Host host, boolean starChartsOnly) {
        this.host = host;
        this.starChartsOnly = starChartsOnly;
    }

    boolean starChartsOnly() {
        return starChartsOnly;
    }

    void attach(CustomPanelAPI panel) {
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
        openLevel = Fade.step(openLevel, !closing, amount / OPEN_SECONDS);
        if (!closing && !bayOpened && openLevel >= 1f) {
            bayOpened = true;
            SkillTreeSounds.panelOpened();
            SocketPanelPlacement area = benchArea(Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight(), false);
            ModificationBay.Listener bayListener = new ModificationBay.Listener() {
                @Override
                public void closed() {
                    closing = true;
                }

                @Override
                public void pressedInside() {
                }

                @Override
                public void wormholeOpened(String message) {
                    wormholeMessage = message;
                }
            };
            bay = starChartsOnly ? ModificationBay.openStarChartsOnly(hostPanel, area, SocketCustody.shipNames(ownedShips), bayListener)
                    : ModificationBay.open(hostPanel, area, SocketCustody.shipNames(ownedShips), bayListener);
        }
        if (closing && openLevel <= 0f && !closeRequested) {
            closeRequested = true;
            if (wormholeMessage != null) {
                host.leave(wormholeMessage);
            } else {
                host.close();
            }
        }
    }

    private void close() {
        if (bay != null && bay.isOpen()) {
            bay.close();
        }
        closing = true;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isConsumed()) {
                continue;
            }
            if (event.isKeyUpEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                if (!closing && (bay == null || !bay.escape())) {
                    close();
                }
            } else if (event.isLMBDownEvent() && !closing && (bay == null || !bay.contains(event.getX(), event.getY()))) {
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
                BACKDROP_ALPHA * Fade.smooth(openLevel) * alphaMult);
    }
}
