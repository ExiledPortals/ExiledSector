package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class AtmosphereRing {

    public static final String TEXTURE_CATEGORY = "planets";
    public static final String TEXTURE_ID = "atmosphere2";
    public static final float INNER_INSET_MULT = 0.4f;
    private static final int SEGMENTS = 64;
    private static final UnitCircle CIRCLE = UnitCircle.of(SEGMENTS);

    private AtmosphereRing() {
    }

    public static void draw(SpriteAPI texture, float centerX, float centerY, float innerRadius, float outerRadius, Color atmosphereColor,
                            float alphaMult) {
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(centerX, centerY, 0f);
            texture.bindTexture();
            Misc.setColor(atmosphereColor, alphaMult);

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= SEGMENTS; i++) {
                float cos = CIRCLE.cos(i % SEGMENTS);
                float sin = CIRCLE.sin(i % SEGMENTS);
                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(cos * innerRadius, sin * innerRadius);
                GL11.glTexCoord2f(0f, 0.99f);
                GL11.glVertex2f(cos * outerRadius, sin * outerRadius);
            }
            GL11.glEnd();
            GL11.glPopMatrix();
        }
    }
}
