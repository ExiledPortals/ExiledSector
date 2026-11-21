package exiledsector.ui.util;

import com.fs.starfarer.api.Global;

public final class UiClip {

    private UiClip() {
    }

    public static GlScope clipUi(float left, float bottom, float width, float height) {
        float screenScale = Global.getSettings().getScreenScaleMult();
        return GlScope.clip((int) Math.floor(left * screenScale), (int) Math.floor(bottom * screenScale),
                (int) Math.ceil(width * screenScale), (int) Math.ceil(height * screenScale));
    }
}
