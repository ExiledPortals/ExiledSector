package exiledsector.charts.phasing;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class PhaseArcs {

    static final int ARC_POINTS = 12;
    static final float LIFE_SECONDS = 0.3f;
    static final float MIN_SPAN_DEGREES = 25f;
    static final float MAX_SPAN_DEGREES = 80f;
    static final float MIN_BULGE = 1.02f;
    static final float MAX_BULGE = 1.3f;
    static final float JAGGEDNESS = 0.08f;
    static final float FRINGE_WIDTH_PER_RADIUS = 0.12f;
    static final float CORE_WIDTH_SHARE = 0.35f;
    static final float MIN_FRINGE_WIDTH = 12f;

    static final class Arc {
        final float[] offsetsX;
        final float[] offsetsY;
        final float width;
        float ageSeconds;

        Arc(float[] offsetsX, float[] offsetsY, float width) {
            this.offsetsX = offsetsX;
            this.offsetsY = offsetsY;
            this.width = width;
        }

        float fade() {
            return Math.max(0f, 1f - ageSeconds / LIFE_SECONDS);
        }
    }

    private final List<Arc> arcs = new ArrayList<>();

    List<Arc> arcs() {
        return arcs;
    }

    void spawnAround(Random random, float radius) {
        float startDegrees = random.nextFloat() * 360f;
        float spanDegrees = MIN_SPAN_DEGREES + random.nextFloat() * (MAX_SPAN_DEGREES - MIN_SPAN_DEGREES);
        float bulge = MIN_BULGE + random.nextFloat() * (MAX_BULGE - MIN_BULGE);
        float[] offsetsX = new float[ARC_POINTS];
        float[] offsetsY = new float[ARC_POINTS];
        for (int point = 0; point < ARC_POINTS; point++) {
            float share = point / (ARC_POINTS - 1f);
            float lift = (float) Math.sin(Math.PI * share) * (bulge - 1f);
            float jitter = point == 0 || point == ARC_POINTS - 1 ? 0f : (random.nextFloat() * 2f - 1f) * JAGGEDNESS;
            float distance = radius * (1f + lift + jitter);
            double angle = Math.toRadians(startDegrees + spanDegrees * share);
            offsetsX[point] = (float) Math.cos(angle) * distance;
            offsetsY[point] = (float) Math.sin(angle) * distance;
        }
        arcs.add(new Arc(offsetsX, offsetsY, Math.max(MIN_FRINGE_WIDTH, radius * FRINGE_WIDTH_PER_RADIUS)));
    }

    void advance(float seconds) {
        Iterator<Arc> remaining = arcs.iterator();
        while (remaining.hasNext()) {
            Arc arc = remaining.next();
            arc.ageSeconds += seconds;
            if (arc.ageSeconds >= LIFE_SECONDS) {
                remaining.remove();
            }
        }
    }

    void render(SpriteAPI fringe, SpriteAPI core, float centreX, float centreY, Color colour, float alpha) {
        if (arcs.isEmpty() || alpha <= 0f) {
            return;
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        fringe.bindTexture();
        for (Arc arc : arcs) {
            strip(fringe, arc, centreX, centreY, arc.width, colour, alpha * arc.fade());
        }
        core.bindTexture();
        for (Arc arc : arcs) {
            strip(core, arc, centreX, centreY, arc.width * CORE_WIDTH_SHARE, Color.WHITE, alpha * arc.fade());
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static void strip(SpriteAPI texture, Arc arc, float centreX, float centreY, float width, Color colour, float alpha) {
        GL11.glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, alpha);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        int last = arc.offsetsX.length - 1;
        for (int point = 0; point <= last; point++) {
            int before = Math.max(0, point - 1);
            int after = Math.min(last, point + 1);
            float alongX = arc.offsetsX[after] - arc.offsetsX[before];
            float alongY = arc.offsetsY[after] - arc.offsetsY[before];
            float length = (float) Math.sqrt(alongX * alongX + alongY * alongY);
            float sideX = length > 0f ? -alongY / length * width * 0.5f : 0f;
            float sideY = length > 0f ? alongX / length * width * 0.5f : 0f;
            float textureX = texture.getTexX() + texture.getTexWidth() * point / last;
            float x = centreX + arc.offsetsX[point];
            float y = centreY + arc.offsetsY[point];
            GL11.glTexCoord2f(textureX, texture.getTexY());
            GL11.glVertex2f(x + sideX, y + sideY);
            GL11.glTexCoord2f(textureX, texture.getTexY() + texture.getTexHeight());
            GL11.glVertex2f(x - sideX, y - sideY);
        }
        GL11.glEnd();
    }
}
