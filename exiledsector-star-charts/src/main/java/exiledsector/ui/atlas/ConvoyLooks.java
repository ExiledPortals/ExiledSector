package exiledsector.ui.atlas;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.AtlasShipGroup;

import java.util.List;

final class ConvoyLooks {

    static final float SMALL_SHIP_SCALE_SHARE = 0.15f;
    static final float LARGE_SHIP_SCALE_SHARE = 0.35f;
    static final float HIDDEN_BELOW_ZOOM = 0.3f;
    static final float SHOWN_FROM_ZOOM = 0.45f;
    private static final float SMALL_RADIUS = 60f;
    private static final float LARGE_RADIUS = 70f;
    private static final float SMALL_CULL_RADIUS = 30f;
    private static final float LARGE_CULL_RADIUS = SMALL_CULL_RADIUS * LARGE_SHIP_SCALE_SHARE / SMALL_SHIP_SCALE_SHARE;

    private final AtlasShipGroup small;
    private final AtlasShipGroup large;

    ConvoyLooks(List<String> smallHulls, List<String> largeHulls, long seed) {
        this.small = smallHulls.isEmpty() ? null : new AtlasShipGroup(AtlasShipGroup.Mode.UNDER_WAY, smallHulls, SMALL_RADIUS, seed);
        this.large = largeHulls.isEmpty() ? null : new AtlasShipGroup(AtlasShipGroup.Mode.UNDER_WAY, largeHulls, LARGE_RADIUS, seed + 1L);
    }

    static float smallVisibility(float zoom) {
        return Math.max(0f, Math.min(1f, (zoom - HIDDEN_BELOW_ZOOM) / (SHOWN_FROM_ZOOM - HIDDEN_BELOW_ZOOM)));
    }

    static float largeVisibility(float zoom) {
        return 1f - smallVisibility(zoom);
    }

    void advance(float amount, float headingDegrees) {
        if (small != null) {
            small.advance(amount, headingDegrees);
        }
        if (large != null) {
            large.advance(amount, headingDegrees);
        }
    }

    void render(TreeViewport viewport, ConvoyRun.Position position, float alpha) {
        float shownAlpha = alpha * position.visibility();
        if (shownAlpha <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        float screenX = viewport.screenX(position.x());
        float screenY = viewport.screenY(position.y());
        draw(viewport, small, screenX, screenY, zoom * SMALL_SHIP_SCALE_SHARE, SMALL_CULL_RADIUS * zoom, position.headingDegrees(),
                shownAlpha * smallVisibility(zoom));
        draw(viewport, large, screenX, screenY, zoom * LARGE_SHIP_SCALE_SHARE, LARGE_CULL_RADIUS * zoom, position.headingDegrees(),
                shownAlpha * largeVisibility(zoom));
    }

    private static void draw(TreeViewport viewport, AtlasShipGroup ships, float screenX, float screenY, float shipZoom, float cullRadius,
                             float headingDegrees, float alpha) {
        if (ships == null || alpha <= 0f || !viewport.isVisible(screenX, screenY, cullRadius)) {
            return;
        }
        ships.render(screenX, screenY, shipZoom, headingDegrees, alpha);
    }
}
