package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class PlanetSphere {

    private PlanetSphere() {
    }

    public static void draw(UnitSphere sphere, SpriteAPI texture, PlanetSpecAPI spec, float x, float y, float radius, float rotation,
                            Color colour, float alpha, float rimAlpha, float rimStep) {
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS | GL11.GL_POLYGON_BIT).enable(GL11.GL_TEXTURE_2D).normalBlend()
                .enable(GL11.GL_CULL_FACE)) {
            // TODO: if a sphere renders inside-out in-game, swap GL11.GL_CW <-> GL11.GL_CCW here.
            GL11.glFrontFace(GL11.GL_CW);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glRotatef(spec.getTilt(), 0f, 0f, 1f);
            GL11.glRotatef(spec.getPitch(), 1f, 0f, 0f);
            GL11.glRotatef(rotation, 0f, 1f, 0f);
            GL11.glRotatef(-90f, 1f, 0f, 0f);
            texture.bindTexture();
            Misc.setColor(colour, alpha);
            sphere.draw(radius);
            if (rimAlpha > 0f) {
                Misc.setColor(colour, rimAlpha);
                sphere.draw(radius + rimStep);
                sphere.draw(radius + rimStep * 2f);
            }
            GL11.glPopMatrix();
        }
    }
}
