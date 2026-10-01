package exiledsector.ui.belt;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class RadialBandGL {

    private RadialBandGL() {
    }

    static void begin(SpriteAPI texture, float cx, float cy, int blendSrcFactor, int blendDstFactor, Color color, float alphaMult) {
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        texture.bindTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(blendSrcFactor, blendDstFactor);
        Misc.setColor(color, alphaMult);
    }

    static void end() {
        GL11.glPopMatrix();
    }

    static int computeSegments(float circumference, float pixelsPerSegment, float minSegments) {
        return (int) Math.max(minSegments, Math.round(circumference / pixelsPerSegment));
    }

    static void strip(UnitCircle circle, int first, int last, float innerRadius, float outerRadius,
                      float innerTexX, float outerTexX, float texPerSegment, RingWave radialWave, RingWave outerWave) {
        int segments = circle.segments();
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = first; i <= last; i++) {
            int segment = i % segments;
            float cos = circle.cos(segment);
            float sin = circle.sin(segment);
            float shift = radialWave.sinAt(circle, segment);
            float inner = innerRadius + shift;
            float outer = outerRadius + shift;
            float texY = texPerSegment * i;

            GL11.glTexCoord2f(innerTexX, texY);
            GL11.glVertex2f(cos * inner, sin * inner);
            GL11.glTexCoord2f(outerTexX, texY);
            GL11.glVertex2f(cos * outer + outerWave.cosAt(circle, segment), sin * outer + outerWave.sinAt(circle, segment));
        }
        GL11.glEnd();
    }
}
