package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class RadialBandGL {

    private RadialBandGL() {
    }

    static void begin(SpriteAPI texture, float cx, float cy, int blendSrcFactor, int blendDstFactor, Color color, float alphaMult) {
        GlScope.textured(blendSrcFactor, blendDstFactor);
        texture.bindTexture();
        Misc.setColor(color, alphaMult);
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0f);
    }

    static void end() {
        GL11.glPopMatrix();
        GlScope.restore();
    }

    static int computeSegments(float circumference, float pixelsPerSegment, float minSegments) {
        return quantised((int) Math.max(minSegments, Math.round(circumference / pixelsPerSegment)));
    }

    static int quantised(int segments) {
        if (segments <= 1) {
            return 1;
        }
        int powerOfTwo = Integer.highestOneBit(segments);
        if (segments == powerOfTwo) {
            return segments;
        }
        int midStep = powerOfTwo + powerOfTwo / 2;
        return segments <= midStep ? midStep : powerOfTwo * 2;
    }

    record StripSpec(float innerRadius, float outerRadius, float innerTexX, float outerTexX, float texPerSegment, RingWave radialWave,
                     RingWave outerWave) {
    }

    static void strip(UnitCircle circle, int first, int last, StripSpec spec) {
        int segments = circle.segments();
        RingWave radialWave = spec.radialWave();
        RingWave outerWave = spec.outerWave();
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = first; i <= last; i++) {
            int segment = i % segments;
            float cos = circle.cos(segment);
            float sin = circle.sin(segment);
            float shift = radialWave.sinAt(circle, segment);
            float inner = spec.innerRadius() + shift;
            float outer = spec.outerRadius() + shift;
            float texY = spec.texPerSegment() * i;

            GL11.glTexCoord2f(spec.innerTexX(), texY);
            GL11.glVertex2f(cos * inner, sin * inner);
            GL11.glTexCoord2f(spec.outerTexX(), texY);
            GL11.glVertex2f(cos * outer + outerWave.cosAt(circle, segment), sin * outer + outerWave.sinAt(circle, segment));
        }
        GL11.glEnd();
    }
}
