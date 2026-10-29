package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

class ShipCardTooltip {

    static final float GAP = 8f;
    static final ShipCardTooltip NONE = new ShipCardTooltip(null, null);
    private static final Logger LOG = Logger.getLogger(ShipCardTooltip.class);
    private static boolean failureLogged;

    record Placement(float left, float bottom) {
    }

    private final CustomPanelAPI hostPanel;
    private UIComponentAPI tooltip;
    private boolean shown;

    ShipCardTooltip(CustomPanelAPI hostPanel, UIComponentAPI tooltip) {
        this.hostPanel = hostPanel;
        this.tooltip = tooltip;
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed game build may only cost the ship card tooltip
    @SuppressWarnings("java:S1181")
    static ShipCardTooltip takeFrom(UIComponentAPI shipList, FleetMemberAPI member, CustomPanelAPI hostPanel) {
        try {
            Object button = UiReflection.call(shipList, "getButtonForMember", member);
            if (!(UiReflection.call(button, "getTooltip") instanceof UIComponentAPI tooltip)) {
                return NONE;
            }
            UiReflection.call(button, "setTooltip", 0f, null);
            return new ShipCardTooltip(hostPanel, tooltip);
        } catch (Throwable e) {
            logFailure(e);
            return NONE;
        }
    }

    static Placement place(ScreenRect card, float tooltipWidth, float tooltipHeight, ScreenRect host) {
        float hostRight = host.left() + host.width();
        float left = card.left() + card.width() + GAP;
        if (left + tooltipWidth > hostRight) {
            left = card.left() - GAP - tooltipWidth;
        }
        left = clamp(left, host.left(), hostRight - tooltipWidth);
        float bottom = clamp(card.bottom() + card.height() - tooltipHeight, host.bottom(), host.bottom() + host.height() - tooltipHeight);
        return new Placement(left - host.left(), bottom - host.bottom());
    }

    boolean isShown() {
        return shown;
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed game build may only cost the ship card tooltip
    @SuppressWarnings("java:S1181")
    void show(PositionAPI cardPosition) {
        if (shown || tooltip == null || cardPosition == null) {
            return;
        }
        try {
            UiReflection.call(tooltip, "beforeShown");
            PositionAPI tooltipPosition = tooltip.getPosition();
            Placement placement = place(rect(cardPosition), tooltipPosition.getWidth(), tooltipPosition.getHeight(),
                    rect(hostPanel.getPosition()));
            hostPanel.addComponent(tooltip).inBL(placement.left(), placement.bottom());
            shown = true;
            UiReflection.call(UiReflection.call(tooltip, "getFader"), "forceIn");
            UiReflection.call(tooltip, "notifyShown");
        } catch (Throwable e) {
            logFailure(e);
            hide();
            tooltip = null;
        }
    }

    // java:S1181: UiReflection calls declare Throwable, and a changed game build may only cost the ship card tooltip
    @SuppressWarnings("java:S1181")
    void hide() {
        if (!shown) {
            return;
        }
        shown = false;
        hostPanel.removeComponent(tooltip);
        try {
            UiReflection.call(tooltip, "notiftyHidden");
        } catch (Throwable e) {
            logFailure(e);
        }
    }

    private static ScreenRect rect(PositionAPI position) {
        return new ScreenRect(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static void logFailure(Throwable e) {
        if (!failureLogged) {
            failureLogged = true;
            LOG.warn("[ExiledSector] Couldn't reuse the vanilla ship tooltip; skill tree ship cards show none", e);
        }
    }
}
