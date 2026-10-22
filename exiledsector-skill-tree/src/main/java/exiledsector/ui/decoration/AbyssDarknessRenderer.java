package exiledsector.ui.decoration;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillTree;
import exiledsector.ui.TreeViewport;
import exiledlib.ui.util.Fade;
import exiledlib.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class AbyssDarknessRenderer {

    static final int FADE_BANDS = 6;
    private static final int MAX_VERTICES = 8;
    private static final Color ABYSS_COLOR = Color.BLACK;

    interface BandSink {
        void polygon(float[] vertexXs, float[] vertexYs, float[] vertexDarkness, int vertexCount);
    }

    private final AbyssalDepth abyssalDepth;
    private final float[] rectangleXs = new float[4];
    private final float[] rectangleYs = new float[4];
    private final float[] lowerClipXs = new float[MAX_VERTICES];
    private final float[] lowerClipYs = new float[MAX_VERTICES];
    private final float[] bandXs = new float[MAX_VERTICES];
    private final float[] bandYs = new float[MAX_VERTICES];
    private final float[] bandDarkness = new float[MAX_VERTICES];
    private float screenOriginDepth;
    private float depthPerScreenX;
    private float depthPerScreenY;
    private float drawAlpha;

    AbyssDarknessRenderer(AbyssalDepth abyssalDepth) {
        this.abyssalDepth = abyssalDepth;
    }

    public static AbyssDarknessRenderer forActiveTree() {
        return new AbyssDarknessRenderer(AbyssalDepth.of(SkillTree.getAllNodes().values()));
    }

    public void render(PositionAPI canvasPosition, TreeViewport viewport, float alphaMult) {
        if (abyssalDepth == null || canvasPosition == null || alphaMult <= 0f) return;
        if (!aimAt(viewport)) return;
        drawAlpha = alphaMult;
        try (GlScope scope = GlScope.flat()) {
            forEachBand(canvasPosition.getX(), canvasPosition.getY(), canvasPosition.getWidth(), canvasPosition.getHeight(), this::drawPolygon);
        }
    }

    boolean aimAt(TreeViewport viewport) {
        float fadeSpan = abyssalDepth.fadeEnd() - abyssalDepth.fadeStart();
        float zoom = viewport.zoom();
        if (zoom <= 0f || fadeSpan <= 0f) return false;
        depthPerScreenX = abyssalDepth.directionX() / (zoom * fadeSpan);
        depthPerScreenY = -abyssalDepth.directionY() / (zoom * fadeSpan);
        screenOriginDepth = abyssalDepth.depthAt(-viewport.centerX() / zoom, viewport.centerY() / zoom);
        return true;
    }

    void forEachBand(float rectangleX, float rectangleY, float rectangleWidth, float rectangleHeight, BandSink sink) {
        rectangleXs[0] = rectangleX;
        rectangleYs[0] = rectangleY;
        rectangleXs[1] = rectangleX + rectangleWidth;
        rectangleYs[1] = rectangleY;
        rectangleXs[2] = rectangleX + rectangleWidth;
        rectangleYs[2] = rectangleY + rectangleHeight;
        rectangleXs[3] = rectangleX;
        rectangleYs[3] = rectangleY + rectangleHeight;
        float deepestCorner = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            deepestCorner = Math.max(deepestCorner, depthAt(rectangleXs[corner], rectangleYs[corner]));
        }
        if (deepestCorner <= 0f) return;

        for (int band = 0; band <= FADE_BANDS; band++) {
            float bandStart = (float) band / FADE_BANDS;
            int lowerCount = clip(rectangleXs, rectangleYs, 4, lowerClipXs, lowerClipYs, bandStart, 1f);
            int bandCount = band == FADE_BANDS ? copy(lowerCount)
                    : clip(lowerClipXs, lowerClipYs, lowerCount, bandXs, bandYs, (float) (band + 1) / FADE_BANDS, -1f);
            if (bandCount < 3) continue;
            for (int vertex = 0; vertex < bandCount; vertex++) {
                bandDarkness[vertex] = darkness(depthAt(bandXs[vertex], bandYs[vertex]));
            }
            sink.polygon(bandXs, bandYs, bandDarkness, bandCount);
        }
    }

    static float darkness(float depth) {
        float clamped = Math.max(0f, Math.min(1f, depth));
        return Fade.smooth(clamped);
    }

    private float depthAt(float screenX, float screenY) {
        return screenOriginDepth + depthPerScreenX * screenX + depthPerScreenY * screenY;
    }

    private int copy(int vertexCount) {
        System.arraycopy(lowerClipXs, 0, bandXs, 0, vertexCount);
        System.arraycopy(lowerClipYs, 0, bandYs, 0, vertexCount);
        return vertexCount;
    }

    private int clip(float[] inputXs, float[] inputYs, int inputCount, float[] outputXs, float[] outputYs, float threshold, float keepSign) {
        int outputCount = 0;
        for (int vertex = 0; vertex < inputCount; vertex++) {
            int previous = vertex == 0 ? inputCount - 1 : vertex - 1;
            float previousSide = keepSign * (depthAt(inputXs[previous], inputYs[previous]) - threshold);
            float currentSide = keepSign * (depthAt(inputXs[vertex], inputYs[vertex]) - threshold);
            if ((previousSide >= 0f) != (currentSide >= 0f)) {
                float crossing = previousSide / (previousSide - currentSide);
                outputXs[outputCount] = inputXs[previous] + (inputXs[vertex] - inputXs[previous]) * crossing;
                outputYs[outputCount] = inputYs[previous] + (inputYs[vertex] - inputYs[previous]) * crossing;
                outputCount++;
            }
            if (currentSide >= 0f) {
                outputXs[outputCount] = inputXs[vertex];
                outputYs[outputCount] = inputYs[vertex];
                outputCount++;
            }
        }
        return outputCount;
    }

    private void drawPolygon(float[] vertexXs, float[] vertexYs, float[] vertexDarkness, int vertexCount) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            Misc.setColor(ABYSS_COLOR, drawAlpha * vertexDarkness[vertex]);
            GL11.glVertex2f(vertexXs[vertex], vertexYs[vertex]);
        }
        GL11.glEnd();
    }
}
