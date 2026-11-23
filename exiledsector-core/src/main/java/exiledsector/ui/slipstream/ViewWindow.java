package exiledsector.ui.slipstream;

import exiledsector.ui.TreeViewport;

public record ViewWindow(float minX, float minY, float maxX, float maxY) {

    public static ViewWindow of(TreeViewport viewport) {
        if (viewport == null || viewport.zoom() <= 0f || Float.isInfinite(viewport.left()) || Float.isInfinite(viewport.right())
                || Float.isInfinite(viewport.bottom()) || Float.isInfinite(viewport.top())) {
            return null;
        }
        float zoom = viewport.zoom();
        return new ViewWindow((viewport.left() - viewport.centerX()) / zoom, (viewport.centerY() - viewport.top()) / zoom,
                (viewport.right() - viewport.centerX()) / zoom, (viewport.centerY() - viewport.bottom()) / zoom);
    }

    public float width() {
        return maxX - minX;
    }

    public float height() {
        return maxY - minY;
    }

    public float shortSide() {
        return Math.min(width(), height());
    }

    public ViewWindow expanded(float margin) {
        return new ViewWindow(minX - margin, minY - margin, maxX + margin, maxY + margin);
    }

    public boolean contains(float x, float y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public boolean overlaps(float otherMinX, float otherMinY, float otherMaxX, float otherMaxY) {
        return otherMaxX >= minX && otherMinX <= maxX && otherMaxY >= minY && otherMinY <= maxY;
    }
}
