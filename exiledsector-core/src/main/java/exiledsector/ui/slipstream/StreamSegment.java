package exiledsector.ui.slipstream;

import java.util.Random;

final class StreamSegment {

    final float distance;
    final float centerX;
    final float centerY;
    final float maxWidth;
    final Wobble leftWobble;
    final Wobble rightWobble;
    float width;
    float normalX;
    float normalY;
    float brightness;
    float fadeOutSeconds;
    boolean fadingOut;

    StreamSegment(float distance, float centerX, float centerY, float width, float maxWidth, float wobbleRadius, Random random) {
        this.distance = distance;
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.maxWidth = maxWidth;
        leftWobble = new Wobble(wobbleRadius, random);
        rightWobble = new Wobble(wobbleRadius, random);
    }

    void advance(float amount, float widenPerSecond, float fadeInSeconds) {
        leftWobble.advance(amount);
        rightWobble.advance(amount);
        width = Math.min(maxWidth, width + widenPerSecond * amount);
        if (fadingOut) {
            brightness = Math.max(0f, brightness - amount / fadeOutSeconds);
        } else {
            brightness = Math.min(1f, brightness + amount / fadeInSeconds);
        }
    }

    void fadeOut(float seconds) {
        if (!fadingOut) {
            fadingOut = true;
            fadeOutSeconds = seconds;
        }
    }

    boolean isGone() {
        return fadingOut && brightness <= 0f;
    }
}
