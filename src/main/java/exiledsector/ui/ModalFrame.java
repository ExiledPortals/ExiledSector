package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.HoloTransition;

import java.awt.Color;

final class ModalFrame {

    private static final float BACKDROP_ALPHA = 0.55f;

    interface Content {
        void render(ScreenRect box, float alphaMult);
    }

    private final BorderedPanel borderedPanel;
    private final HoloTransition holoTransition = new HoloTransition();

    ModalFrame(Class<?> owner) {
        this.borderedPanel = new BorderedPanel(owner);
    }

    void open() {
        holoTransition.open();
    }

    void close() {
        holoTransition.close();
    }

    void advance(float amount) {
        holoTransition.advance(amount);
    }

    boolean isVisible() {
        return holoTransition.isVisible();
    }

    ScreenRect render(PositionAPI hostPosition, float boxWidth, float boxHeight, Color accent, float alphaMult, Content content) {
        if (!holoTransition.isVisible()) {
            return null;
        }
        GLDraw.fillQuad(hostPosition.getX(), hostPosition.getY(), hostPosition.getWidth(), hostPosition.getHeight(), Color.BLACK,
                BACKDROP_ALPHA * holoTransition.backdropAlpha() * alphaMult);
        ScreenRect box = new ScreenRect(hostPosition.getX() + (hostPosition.getWidth() - boxWidth) / 2f,
                hostPosition.getY() + (hostPosition.getHeight() - boxHeight) / 2f, boxWidth, boxHeight);
        holoTransition.drawProjection(box.left(), box.bottom(), boxWidth, boxHeight, accent, alphaMult);
        if (holoTransition.contentAlpha() <= 0f) {
            return box;
        }
        boolean clipped = holoTransition.beginReveal(box.left(), box.bottom(), boxWidth, boxHeight);
        try {
            borderedPanel.draw(box.left(), box.bottom(), boxWidth, boxHeight, alphaMult);
            content.render(box, alphaMult);
        } finally {
            if (clipped) {
                HoloTransition.endReveal();
            }
        }
        holoTransition.drawRevealLine(box.left(), box.bottom(), boxWidth, boxHeight, accent, alphaMult);
        return box;
    }
}
