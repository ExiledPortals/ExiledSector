package exiledsector.ui.util;

import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Arrays;

public final class LineBatch {

    private final float thickness;
    private float[] positions = new float[512];
    private int[] colors = new int[256];
    private float[] alphas = new float[256];
    private int vertexCount;

    public LineBatch(float thickness) {
        this.thickness = thickness;
    }

    public void add(float x1, float y1, int argb1, float x2, float y2, int argb2, float alphaMult) {
        addVertex(x1, y1, argb1, alphaMult);
        addVertex(x2, y2, argb2, alphaMult);
    }

    public void add(float x1, float y1, float x2, float y2, Color color, float alphaMult) {
        add(x1, y1, color.getRGB(), x2, y2, color.getRGB(), alphaMult);
    }

    private void addVertex(float x, float y, int argb, float alphaMult) {
        if (vertexCount == colors.length) {
            positions = Arrays.copyOf(positions, positions.length * 2);
            colors = Arrays.copyOf(colors, colors.length * 2);
            alphas = Arrays.copyOf(alphas, alphas.length * 2);
        }
        positions[vertexCount * 2] = x;
        positions[vertexCount * 2 + 1] = y;
        colors[vertexCount] = argb;
        alphas[vertexCount] = Math.max(0f, Math.min(1f, alphaMult));
        vertexCount++;
    }

    public void clear() {
        vertexCount = 0;
    }

    public void flush() {
        if (vertexCount == 0) return;

        try (GlScope scope = GlScope.save(GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT).lineWidth(thickness)) {
            GL11.glBegin(GL11.GL_LINES);
            for (int i = 0; i < vertexCount; i++) {
                int argb = colors[i];
                GL11.glColor4ub((byte) (argb >> 16), (byte) (argb >> 8), (byte) argb, (byte) ((argb >>> 24) * alphas[i]));
                GL11.glVertex2f(positions[i * 2], positions[i * 2 + 1]);
            }
            GL11.glEnd();
        }
    }
}
