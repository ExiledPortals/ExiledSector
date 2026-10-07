package exiledsector.ui.util;

import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;

public final class HoloFrame {

    public enum Look {
        MODAL(false, true),
        PANEL(true, true),
        VANILLA_HOST(true, false);

        private final boolean borderFadesWithContent;
        private final boolean revealSweep;

        Look(boolean borderFadesWithContent, boolean revealSweep) {
            this.borderFadesWithContent = borderFadesWithContent;
            this.revealSweep = revealSweep;
        }
    }

    @FunctionalInterface
    public interface Content {
        void render(float left, float bottom, float width, float height, float alphaMult);
    }

    private static final float BACKDROP_ALPHA = 0.55f;
    private static final Color BACKDROP_COLOR = Color.BLACK;

    private final BorderedPanel borderedPanel;
    private final HoloTransition holoTransition = new HoloTransition();
    private final Look look;

    public HoloFrame(Class<?> owner, Look look) {
        this.borderedPanel = new BorderedPanel(owner);
        this.look = look;
    }

    public void open() {
        holoTransition.open();
    }

    public void openInstantly() {
        holoTransition.openInstantly();
    }

    public void close() {
        holoTransition.close();
    }

    public boolean advance(float amount) {
        if (!holoTransition.isAnimating()) {
            return false;
        }
        holoTransition.advance(amount);
        return true;
    }

    public boolean isVisible() {
        return holoTransition.isVisible();
    }

    public boolean isBlocking() {
        return holoTransition.isVisible();
    }

    public boolean isFullyClosed() {
        return holoTransition.isFullyClosed();
    }

    public float contentAlpha() {
        return holoTransition.contentAlpha();
    }

    public float chromeAlpha() {
        return chromeAlpha(holoTransition.isOpening(), holoTransition.contentAlpha());
    }

    static float chromeAlpha(boolean opening, float contentAlpha) {
        return opening && contentAlpha >= 1f ? 1f : 0f;
    }

    public void renderBackdrop(PositionAPI hostPosition, float alphaMult) {
        if (!holoTransition.isVisible()) {
            return;
        }
        GLDraw.fillQuad(hostPosition.getX(), hostPosition.getY(), hostPosition.getWidth(), hostPosition.getHeight(), BACKDROP_COLOR,
                BACKDROP_ALPHA * holoTransition.backdropAlpha() * alphaMult);
    }

    public void render(float left, float bottom, float width, float height, Color accent, float alphaMult, Content content) {
        if (!holoTransition.isVisible()) {
            return;
        }
        holoTransition.drawProjection(left, bottom, width, height, accent, alphaMult);
        float contentAlpha = holoTransition.contentAlpha();
        if (contentAlpha <= 0f) {
            return;
        }
        float borderAlpha = look.borderFadesWithContent ? contentAlpha * alphaMult : alphaMult;
        if (!look.revealSweep) {
            borderedPanel.draw(left, bottom, width, height, borderAlpha);
            renderContent(left, bottom, width, height, alphaMult, content);
            return;
        }
        try (GlScope clip = holoTransition.reveal(left, bottom, width, height)) {
            borderedPanel.draw(left, bottom, width, height, borderAlpha);
            renderContent(left, bottom, width, height, alphaMult, content);
        }
        holoTransition.drawRevealLine(left, bottom, width, height, accent, alphaMult);
    }

    private static void renderContent(float left, float bottom, float width, float height, float alphaMult, Content content) {
        if (content != null) {
            content.render(left, bottom, width, height, alphaMult);
        }
    }
}
