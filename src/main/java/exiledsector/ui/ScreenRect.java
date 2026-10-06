package exiledsector.ui;

import exiledsector.ui.util.Rects;

record ScreenRect(float left, float bottom, float width, float height) {

    static final ScreenRect NONE = new ScreenRect(0f, 0f, 0f, 0f);

    boolean contains(float x, float y) {
        return width > 0f && height > 0f && Rects.contains(left, bottom, width, height, x, y);
    }
}
