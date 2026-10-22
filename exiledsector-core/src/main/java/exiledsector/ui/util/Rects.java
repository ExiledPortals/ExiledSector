package exiledsector.ui.util;

import com.fs.starfarer.api.ui.PositionAPI;

public final class Rects {

    private Rects() {
    }

    public static boolean contains(float left, float bottom, float width, float height, float x, float y) {
        return x >= left && x <= left + width && y >= bottom && y <= bottom + height;
    }

    public static boolean contains(PositionAPI position, float x, float y) {
        return position != null && contains(position.getX(), position.getY(), position.getWidth(), position.getHeight(), x, y);
    }
}
