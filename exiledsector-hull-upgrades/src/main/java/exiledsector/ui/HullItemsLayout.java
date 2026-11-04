package exiledsector.ui;

import exiledsector.ui.section.SectionLayout;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledsector.ui.socket.FreeArea;
import exiledsector.ui.socket.ItemStoragePanel;

record HullItemsLayout(float panelLeft, float panelWidth, float settledShipX, SectionsDisplay.SectionBounds sectionBounds) {

    static final float PANEL_MARGIN = 10f;
    static final float SECTION_BAND_MARGIN = 16f;
    static final float SHIP_EDGE_MARGIN = 24f;
    static final float SECTION_LABEL_OVERHANG = 24f;
    static final float MAX_PANEL_WIDTH = 640f;
    static final float SCREEN_MARGIN = 10f;

    static HullItemsLayout of(RefitScreenLayout layout, SectionLayout.ShipExtent extent) {
        float radius = SectionLayout.SECTION_RADIUS;
        float panelLeft = layout.fleetListRight() + PANEL_MARGIN;
        float frameRight = layout.frameX() + layout.frameWidth();
        float settledShipX = frameRight - SHIP_EDGE_MARGIN - Math.max(extent.halfWidth(), radius + SECTION_LABEL_OVERHANG);
        float leftColumnCentre = settledShipX - (extent.halfWidth() + SectionLayout.COLUMN_GAP + radius);
        float leftColumnEdge = leftColumnCentre - radius - SECTION_LABEL_OVERHANG;
        float panelWidth = Math.max(ItemStoragePanel.COMPACT_WIDTH, Math.min(MAX_PANEL_WIDTH, leftColumnEdge - SECTION_BAND_MARGIN - panelLeft));
        float verticalReach = SectionsDisplay.VERTICAL_REACH;
        SectionsDisplay.SectionBounds bounds = new SectionsDisplay.SectionBounds(
                panelLeft + panelWidth + SECTION_BAND_MARGIN + radius, frameRight - SECTION_BAND_MARGIN - radius,
                layout.frameY() + PANEL_MARGIN + verticalReach, layout.frameTop() - PANEL_MARGIN - verticalReach);
        return new HullItemsLayout(panelLeft, panelWidth, settledShipX, bounds);
    }

    FreeArea advancedSearchArea(float panelTop, float panelHeight) {
        float areaRight = panelLeft - PANEL_MARGIN;
        return new FreeArea(SCREEN_MARGIN, panelTop, Math.max(0f, areaRight - SCREEN_MARGIN), panelHeight);
    }
}
