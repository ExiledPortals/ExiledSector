package exiledsector.ui;

public record TreeViewport(float centerX, float centerY, float zoom, float left, float bottom, float right, float top) {

    public TreeViewport(float centerX, float centerY, float zoom) {
        this(centerX, centerY, zoom, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public float screenX(float worldX) {
        return centerX + worldX * zoom;
    }

    public float screenY(float worldY) {
        return centerY - worldY * zoom;
    }

    public boolean isVisible(float screenX, float screenY, float screenRadius) {
        return overlaps(screenX - screenRadius, screenY - screenRadius, screenX + screenRadius, screenY + screenRadius);
    }

    public boolean overlaps(float minX, float minY, float maxX, float maxY) {
        return maxX >= left && minX <= right && maxY >= bottom && minY <= top;
    }
}
