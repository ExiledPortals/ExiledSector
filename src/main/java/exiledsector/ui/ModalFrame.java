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

    private final BorderedPanel panel;
    private final HoloTransition transition = new HoloTransition();

    ModalFrame(Class<?> owner) {
        this.panel = new BorderedPanel(owner);
    }

    void open() {
        transition.open();
    }

    void close() {
        transition.close();
    }

    void advance(float amount) {
        transition.advance(amount);
    }

    boolean isVisible() {
        return transition.isVisible();
    }

    ScreenRect render(PositionAPI position, float width, float height, Color accent, float alphaMult, Content content) {
        if (!transition.isVisible()) {
            return null;
        }
        GLDraw.fillQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), Color.BLACK,
                BACKDROP_ALPHA * transition.backdropAlpha() * alphaMult);
        ScreenRect box = new ScreenRect(position.getX() + (position.getWidth() - width) / 2f,
                position.getY() + (position.getHeight() - height) / 2f, width, height);
        transition.drawProjection(box.left(), box.bottom(), width, height, accent, alphaMult);
        if (transition.contentAlpha() <= 0f) {
            return box;
        }
        boolean clipped = transition.beginReveal(box.left(), box.bottom(), width, height);
        try {
            panel.draw(box.left(), box.bottom(), width, height, alphaMult);
            content.render(box, alphaMult);
        } finally {
            if (clipped) {
                HoloTransition.endReveal();
            }
        }
        transition.drawRevealLine(box.left(), box.bottom(), width, height, accent, alphaMult);
        return box;
    }
}
