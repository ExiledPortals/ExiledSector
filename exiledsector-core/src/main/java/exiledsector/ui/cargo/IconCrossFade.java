package exiledsector.ui.cargo;

import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;

import java.awt.Color;
import java.util.List;

public final class IconCrossFade {

    public static final float HOLD_SECONDS = 1.6f;
    public static final float FADE_SECONDS = 0.6f;
    public static final float STEP_SECONDS = HOLD_SECONDS + FADE_SECONDS;

    public record Frame(int shownIndex, int nextIndex, float nextAlpha) {

        public float shownAlpha() {
            return 1f - nextAlpha;
        }
    }

    private IconCrossFade() {
    }

    public static float periodSeconds(int iconCount) {
        return Math.max(1, iconCount) * STEP_SECONDS;
    }

    public static Frame frameAt(float seconds, int iconCount) {
        if (iconCount <= 1) {
            return new Frame(0, 0, 0f);
        }
        float cycleSeconds = Fade.cycle(seconds, periodSeconds(iconCount)) * periodSeconds(iconCount);
        int shownIndex = Math.min(iconCount - 1, (int) (cycleSeconds / STEP_SECONDS));
        float secondsIntoStep = cycleSeconds - shownIndex * STEP_SECONDS;
        float fade = secondsIntoStep <= HOLD_SECONDS ? 0f : Math.min(1f, (secondsIntoStep - HOLD_SECONDS) / FADE_SECONDS);
        return new Frame(shownIndex, (shownIndex + 1) % iconCount, Fade.smooth(fade));
    }

    public static void drawAtCenter(SpriteCache cache, List<String> iconPaths, float seconds, float centreX, float centreY, float iconWidth,
                                    float iconHeight, float alphaMult) {
        if (iconPaths.isEmpty()) {
            return;
        }
        Frame frame = frameAt(seconds, iconPaths.size());
        AnimatedItemIcons.drawAtCenter(cache, iconPaths.get(frame.shownIndex()), centreX, centreY, iconWidth, iconHeight, Color.WHITE,
                frame.shownAlpha() * alphaMult);
        if (frame.nextAlpha() > 0f) {
            AnimatedItemIcons.drawAtCenter(cache, iconPaths.get(frame.nextIndex()), centreX, centreY, iconWidth, iconHeight, Color.WHITE,
                    frame.nextAlpha() * alphaMult);
        }
    }
}
