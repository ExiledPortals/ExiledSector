package exiledsector.ui;

import exiledsector.ui.framework.FrameworkSocketLayout;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.socket.SocketStoragePanel;

record HullUpgradesLayout(float panelLeft, float panelWidth, float settledShipX, HullSocketsDisplay.SocketBounds socketBounds) {

    static final float PANEL_MARGIN = 10f;
    static final float SOCKET_BAND_MARGIN = 16f;
    static final float SHIP_EDGE_MARGIN = 24f;
    static final float SOCKET_LABEL_OVERHANG = 24f;
    static final float MAX_PANEL_WIDTH = 640f;

    static HullUpgradesLayout of(RefitScreenLayout layout, FrameworkSocketLayout.ShipExtent extent) {
        float radius = FrameworkSocketLayout.SOCKET_RADIUS;
        float panelLeft = layout.frameX() + PANEL_MARGIN;
        float frameRight = layout.frameX() + layout.frameWidth();
        float settledShipX = frameRight - SHIP_EDGE_MARGIN - Math.max(extent.halfWidth(), radius + SOCKET_LABEL_OVERHANG);
        float leftColumnCentre = settledShipX - (extent.halfWidth() + FrameworkSocketLayout.COLUMN_GAP + radius);
        float leftColumnEdge = leftColumnCentre - radius - SOCKET_LABEL_OVERHANG;
        float panelWidth = Math.max(SocketStoragePanel.COMPACT_WIDTH, Math.min(MAX_PANEL_WIDTH, leftColumnEdge - SOCKET_BAND_MARGIN - panelLeft));
        float verticalReach = HullSocketsDisplay.VERTICAL_REACH;
        HullSocketsDisplay.SocketBounds bounds = new HullSocketsDisplay.SocketBounds(
                panelLeft + panelWidth + SOCKET_BAND_MARGIN + radius, frameRight - SOCKET_BAND_MARGIN - radius,
                layout.frameY() + PANEL_MARGIN + verticalReach, layout.frameTop() - PANEL_MARGIN - verticalReach);
        return new HullUpgradesLayout(panelLeft, panelWidth, settledShipX, bounds);
    }

    float panelRight() {
        return panelLeft + panelWidth;
    }
}
