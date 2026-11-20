package exiledsector.ui.decoration;

record SceneFrame(float screenX, float screenY, float zoom, float baseHeadingDeg, float seconds, float alpha) {

    float screenXOf(float worldX) {
        return screenX + worldX * zoom;
    }

    float screenYOf(float worldY) {
        return screenY + worldY * zoom;
    }
}
