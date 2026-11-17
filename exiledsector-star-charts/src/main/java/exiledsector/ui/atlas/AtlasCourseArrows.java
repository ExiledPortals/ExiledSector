package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;

final class AtlasCourseArrows {

    static final String ARROW_PATH = "graphics/warroom/ship_arrow.png";
    static final float GAP_SHARE = 0.3f;
    static final float FADE_SIZES = 4f;
    static final float PIXELS_PER_SECOND = 40f;
    private static final float SPRITE_FACING_OFFSET = -90f;

    interface Sink {

        void arrow(float x, float y, float headingDegrees, float alphaShare);
    }

    private final SpriteCache sprites = new SpriteCache(AtlasCourseArrows.class);
    private final TexturedQuadBatch batch = new TexturedQuadBatch();
    private float[] lengths = new float[0];
    private double travelledPixels;

    void advance(float amount) {
        travelledPixels += amount * PIXELS_PER_SECOND;
    }

    void render(float[] xs, float[] ys, int count, float startTrim, float endTrim, float size, Color colour, float alpha) {
        SpriteAPI sprite = sprites.sprite(ARROW_PATH);
        if (sprite == null || alpha <= 0f) {
            return;
        }
        try (TexturedQuadBatch quads = batch.beginNormal(sprite)) {
            place(xs, ys, count, startTrim, endTrim, size, travelledPixels,
                    (x, y, heading, share) -> quads.quadAtCenter(x, y, size, size, heading + SPRITE_FACING_OFFSET, colour, alpha * share));
        }
    }

    void place(float[] xs, float[] ys, int count, float startTrim, float endTrim, float size, double travelled, Sink sink) {
        if (count < 2 || size <= 0f) {
            return;
        }
        if (lengths.length < count) {
            lengths = new float[count];
        }
        lengths[0] = 0f;
        for (int point = 1; point < count; point++) {
            lengths[point] = lengths[point - 1] + (float) Math.hypot(xs[point] - xs[point - 1], ys[point] - ys[point - 1]);
        }
        float visible = lengths[count - 1] - startTrim - endTrim;
        if (visible <= size) {
            return;
        }
        float spacing = size * (1f + GAP_SHARE);
        float fade = Math.min(FADE_SIZES * size, visible / 2f);
        float first = (float) (travelled % spacing);
        int segment = 1;
        for (float along = first; along < visible; along += spacing) {
            float distance = startTrim + along;
            while (segment < count - 1 && lengths[segment] < distance) {
                segment++;
            }
            float stepLength = lengths[segment] - lengths[segment - 1];
            if (stepLength <= 0f) {
                continue;
            }
            float share = (distance - lengths[segment - 1]) / stepLength;
            float dx = xs[segment] - xs[segment - 1];
            float dy = ys[segment] - ys[segment - 1];
            float alphaShare = Math.min(1f, Math.min(along, visible - along) / fade);
            sink.arrow(xs[segment - 1] + dx * share, ys[segment - 1] + dy * share, (float) Math.toDegrees(Math.atan2(dy, dx)), alphaShare);
        }
    }
}
