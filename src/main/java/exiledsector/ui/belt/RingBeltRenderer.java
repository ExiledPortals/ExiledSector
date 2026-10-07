package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class RingBeltRenderer {
    private static final float PIXELS_PER_SEGMENT = 5f;
    private static final float MAX_TILE_COUNT = 60f;
    private static final double FULL_TURN = 2 * Math.PI;
    private static final int[] HIDDEN = new int[0];

    private RingBeltRenderer() {
    }

    public static void render(SpriteAPI texture, RadialBand band, Color color, float alphaMult) {
        render(texture, band, color, alphaMult, 0f, null);
    }

    public static void render(SpriteAPI texture, RadialBand band, Color color, float alphaMult, float rotationDeg, TreeViewport clip) {
        if (alphaMult <= 0f) {
            return;
        }
        float innerRadius = band.innerRadius();
        float outerRadius = band.outerRadius();
        float middleRadius = (innerRadius + outerRadius) / 2f;
        float circumference = (float) (FULL_TURN * middleRadius);
        int segments = Math.max(1, RadialBandGL.computeSegments(circumference, PIXELS_PER_SEGMENT, 0));
        int[] range = visibleVertexRange(band.center().x, band.center().y, innerRadius, outerRadius, rotationDeg, segments, clip);
        if (range.length == 0) {
            return;
        }
        float thickness = outerRadius - innerRadius;

        float imageWidth = texture.getWidth();
        float imageHeight = texture.getHeight();
        float aspectRatio = imageHeight / imageWidth;
        float tileCount = Math.min(MAX_TILE_COUNT, Math.max(1f, circumference / (thickness * aspectRatio)));
        float texPerSegment = tileCount / segments;

        RadialBandGL.begin(texture, band.center().x, band.center().y, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, color, alphaMult);
        GL11.glRotatef(rotationDeg, 0f, 0f, 1f);
        UnitCircle circle = UnitCircle.of(segments);
        RadialBandGL.StripSpec spec = new RadialBandGL.StripSpec(innerRadius, outerRadius, 0f, 1f, texPerSegment, RingWave.NONE,
                RingWave.NONE);
        if (range[1] <= segments) {
            RadialBandGL.strip(circle, range[0], range[1], spec);
        } else {
            RadialBandGL.strip(circle, range[0], segments, spec);
            RadialBandGL.strip(circle, 0, range[1] - segments, spec);
        }
        RadialBandGL.end();
    }

    static int[] visibleVertexRange(float cx, float cy, float innerRadius, float outerRadius, float rotationDeg, int segments,
                                    TreeViewport clip) {
        if (clip == null) {
            return new int[]{0, segments};
        }
        float nearestX = Math.max(clip.left(), Math.min(cx, clip.right()));
        float nearestY = Math.max(clip.bottom(), Math.min(cy, clip.top()));
        double nearest = Math.hypot(cx - nearestX, cy - nearestY);
        if (nearest > outerRadius) {
            return HIDDEN;
        }
        float[] cornerXs = {clip.left(), clip.right(), clip.right(), clip.left()};
        float[] cornerYs = {clip.bottom(), clip.bottom(), clip.top(), clip.top()};
        double farthest = 0;
        for (int i = 0; i < 4; i++) {
            farthest = Math.max(farthest, Math.hypot(cornerXs[i] - cx, cornerYs[i] - cy));
        }
        if (farthest < innerRadius) {
            return HIDDEN;
        }
        if (nearest == 0) {
            return new int[]{0, segments};
        }

        double reference = Math.atan2((clip.bottom() + clip.top()) / 2.0 - cy, (clip.left() + clip.right()) / 2.0 - cx);
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            double delta = Math.IEEEremainder(Math.atan2(cornerYs[i] - cy, cornerXs[i] - cx) - reference, FULL_TURN);
            lowest = Math.min(lowest, delta);
            highest = Math.max(highest, delta);
        }
        double rotation = Math.toRadians(rotationDeg);
        double anglePerSegment = FULL_TURN / segments;
        int first = (int) Math.floor((reference + lowest - rotation) / anglePerSegment) - 1;
        int last = (int) Math.ceil((reference + highest - rotation) / anglePerSegment) + 1;
        if (last - first >= segments) {
            return new int[]{0, segments};
        }
        int shift = Math.floorDiv(first, segments) * segments;
        return new int[]{first - shift, last - shift};
    }
}
