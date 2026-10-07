package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.util.Rects;

record ScreenRect(float left, float bottom, float width, float height) {

    static final ScreenRect NONE = new ScreenRect(0f, 0f, 0f, 0f);

    static ScreenRect centeredIn(PositionAPI hostPosition, float boxWidth, float boxHeight) {
        return centeredIn(hostPosition.getX(), hostPosition.getY(), hostPosition.getWidth(), hostPosition.getHeight(), boxWidth, boxHeight);
    }

    static ScreenRect centeredIn(float hostLeft, float hostBottom, float hostWidth, float hostHeight, float boxWidth, float boxHeight) {
        return new ScreenRect(hostLeft + (hostWidth - boxWidth) / 2f, hostBottom + (hostHeight - boxHeight) / 2f, boxWidth, boxHeight);
    }

    boolean contains(float x, float y) {
        return width > 0f && height > 0f && Rects.contains(left, bottom, width, height, x, y);
    }
}
