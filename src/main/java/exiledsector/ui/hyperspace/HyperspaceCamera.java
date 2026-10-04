package exiledsector.ui.hyperspace;

import java.util.List;

public record HyperspaceCamera(float x, float y, float zoom) {

    public static final float ANCHOR_REACH = 1.7f;

    public static HyperspaceCamera fit(List<HyperspaceAnchor> anchors, float mapStarRadius, float width, float height,
                                       float margin, float labelSpace) {
        if (anchors.isEmpty()) {
            return new HyperspaceCamera(0f, 0f, 1f);
        }
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (HyperspaceAnchor anchor : anchors) {
            float reach = anchor.displayRadius(mapStarRadius, 1f) * ANCHOR_REACH;
            minX = Math.min(minX, anchor.x() - reach);
            maxX = Math.max(maxX, anchor.x() + reach);
            minY = Math.min(minY, anchor.y() - reach);
            maxY = Math.max(maxY, anchor.y() + reach);
        }
        float usableWidth = Math.max(1f, width - margin * 2f);
        float usableHeight = Math.max(1f, height - margin * 2f - labelSpace);
        float zoom = Math.min(usableWidth / Math.max(1f, maxX - minX), usableHeight / Math.max(1f, maxY - minY));
        float centreX = (minX + maxX) / 2f;
        float centreY = (minY - labelSpace / zoom + maxY) / 2f;
        return new HyperspaceCamera(centreX, centreY, zoom);
    }

    public HyperspaceCamera zoomedAbout(float pivotX, float pivotY, float newZoom) {
        float keep = zoom / newZoom;
        return new HyperspaceCamera(pivotX - (pivotX - x) * keep, pivotY - (pivotY - y) * keep, newZoom);
    }

    static HyperspaceCamera between(HyperspaceCamera from, HyperspaceCamera to, float t) {
        float zoom = (float) Math.exp(Math.log(from.zoom) + (Math.log(to.zoom) - Math.log(from.zoom)) * t);
        return new HyperspaceCamera(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, zoom);
    }
}
