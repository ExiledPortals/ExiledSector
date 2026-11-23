package exiledsector.ui.slipstream;

import exiledsector.ui.util.LineBatch;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

final class StreamParticles {

    static final float AREA_PER_PARTICLE_WIDTHS_SQUARED = 0.05f;
    static final int MAX_PARTICLES = 500;
    static final float MIN_SPEED_WIDTHS = 0.65f;
    static final float MAX_SPEED_WIDTHS = 1.1f;
    static final float MIN_LIFE_SECONDS = 2f;
    static final float MAX_LIFE_SECONDS = 6f;
    static final float FADE_IN_SECONDS = 1f;
    static final float FADE_OUT_SECONDS = 0.5f;
    static final float TRAIL_SECONDS = 0.25f;
    static final float LATERAL_REACH = 0.85f;
    static final float CALM_LATERAL = 0.33f;
    static final float EDGE_SPEED_SHARE = 0.65f;
    static final Color SLOW_COLOR = new Color(0.5f, 0.3f, 0.75f, 0.1f);
    static final Color FAST_COLOR = new Color(0.5f, 0.6f, 1f, 0.3f);

    private final float[] distance = new float[MAX_PARTICLES];
    private final float[] lateral = new float[MAX_PARTICLES];
    private final float[] speed = new float[MAX_PARTICLES];
    private final float[] age = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final int[] colour = new int[MAX_PARTICLES];
    private final Vector2f head = new Vector2f();
    private final Vector2f tail = new Vector2f();
    private final float[] placed = new float[SlipstreamProjection.PLACED_LENGTH];
    private int count;

    int count() {
        return count;
    }

    void advance(float amount, Slipstream stream, Random random) {
        float laid = stream.laidLength();
        for (int i = count - 1; i >= 0; i--) {
            age[i] += amount;
            distance[i] += speed[i] * amount;
            if (age[i] >= life[i] || distance[i] >= laid) {
                remove(i);
            }
        }
        int target = Math.min(MAX_PARTICLES, (int) (laid / (stream.baseWidth() * AREA_PER_PARTICLE_WIDTHS_SQUARED)));
        while (count < target && !stream.isDissolving()) {
            spawn(stream, laid, random);
        }
    }

    void draw(LineBatch lines, Slipstream stream, SlipstreamProjection projection, float alphaMult) {
        SlipstreamRoute route = stream.route();
        for (int i = 0; i < count; i++) {
            float streamAlpha = stream.alphaAt(distance[i]);
            if (streamAlpha <= 0f) {
                continue;
            }
            float fade = Math.min(1f, Math.min(age[i] / FADE_IN_SECONDS, (life[i] - age[i]) / FADE_OUT_SECONDS));
            float alpha = fade * streamAlpha * alphaMult;
            if (alpha <= 0f) {
                continue;
            }
            float halfWidth = stream.widthAt(distance[i]) / 2f;
            route.offsetPoint(distance[i], lateral[i] * halfWidth, head);
            route.offsetPoint(Math.max(0f, distance[i] - speed[i] * TRAIL_SECONDS), lateral[i] * halfWidth, tail);
            projection.place(head.x, head.y, placed);
            float headX = placed[SlipstreamProjection.X];
            float headY = placed[SlipstreamProjection.Y];
            projection.place(tail.x, tail.y, placed);
            float tailX = placed[SlipstreamProjection.X];
            float tailY = placed[SlipstreamProjection.Y];
            float middleX = headX + (tailX - headX) / 3f;
            float middleY = headY + (tailY - headY) / 3f;
            int bright = colour[i];
            int clear = bright & 0x00FFFFFF;
            lines.add(headX, headY, clear, middleX, middleY, bright, alpha);
            lines.add(middleX, middleY, bright, tailX, tailY, clear, alpha);
        }
    }

    private void spawn(Slipstream stream, float laid, Random random) {
        int i = count++;
        distance[i] = random.nextFloat() * laid;
        lateral[i] = (random.nextFloat() * 2f - 1f) * LATERAL_REACH;
        float calm = Math.abs(lateral[i]) <= CALM_LATERAL ? 1f : 1f - (Math.abs(lateral[i]) - CALM_LATERAL) / (1f - CALM_LATERAL);
        float speedRoll = random.nextFloat();
        speed[i] = stream.baseWidth() * (MIN_SPEED_WIDTHS + (MAX_SPEED_WIDTHS - MIN_SPEED_WIDTHS) * speedRoll)
                * (EDGE_SPEED_SHARE + (1f - EDGE_SPEED_SHARE) * calm);
        age[i] = 0f;
        life[i] = MIN_LIFE_SECONDS + random.nextFloat() * (MAX_LIFE_SECONDS - MIN_LIFE_SECONDS);
        colour[i] = mixed(speedRoll * calm);
    }

    private void remove(int i) {
        int last = --count;
        distance[i] = distance[last];
        lateral[i] = lateral[last];
        speed[i] = speed[last];
        age[i] = age[last];
        life[i] = life[last];
        colour[i] = colour[last];
    }

    private static int mixed(float share) {
        int alpha = Math.round(SLOW_COLOR.getAlpha() + (FAST_COLOR.getAlpha() - SLOW_COLOR.getAlpha()) * share);
        int red = Math.round(SLOW_COLOR.getRed() + (FAST_COLOR.getRed() - SLOW_COLOR.getRed()) * share);
        int green = Math.round(SLOW_COLOR.getGreen() + (FAST_COLOR.getGreen() - SLOW_COLOR.getGreen()) * share);
        int blue = Math.round(SLOW_COLOR.getBlue() + (FAST_COLOR.getBlue() - SLOW_COLOR.getBlue()) * share);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
