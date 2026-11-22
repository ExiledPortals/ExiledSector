package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;

final class PhasedPlanetGhost {

    static final int LATITUDE_BANDS = 16;
    static final int LONGITUDE_BANDS = 24;
    static final float AMBIENT_LIGHT = 0.25f;
    static final float LIGHT_HEIGHT = 0.6f;
    static final float VISIBLE_TEXTURE_START = 0.25f;
    static final float VISIBLE_TEXTURE_SHARE = 0.5f;

    private final SpriteAPI texture;

    private PhasedPlanetGhost(SpriteAPI texture) {
        this.texture = texture;
    }

    static PhasedPlanetGhost load(String texturePath) throws IOException {
        Global.getSettings().loadTexture(texturePath);
        return new PhasedPlanetGhost(Global.getSettings().getSprite(texturePath));
    }

    static float[] lightDirection(float towardsLightX, float towardsLightY) {
        float length = (float) Math.sqrt(towardsLightX * towardsLightX + towardsLightY * towardsLightY);
        float x = length > 0f ? towardsLightX / length : 0f;
        float y = length > 0f ? towardsLightY / length : 0f;
        float z = length > 0f ? LIGHT_HEIGHT : 1f;
        float norm = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[] {x / norm, y / norm, z / norm};
    }

    static float brightness(float[] normal, float[] light) {
        float facing = normal[0] * light[0] + normal[1] * light[1] + normal[2] * light[2];
        return AMBIENT_LIGHT + (1f - AMBIENT_LIGHT) * Math.max(0f, facing);
    }

    void render(float centreX, float centreY, float radius, float[] light, Color tint, float alpha, boolean additive) {
        if (alpha <= 0f || radius <= 0f) {
            return;
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        texture.bindTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        float red = tint.getRed() / 255f;
        float green = tint.getGreen() / 255f;
        float blue = tint.getBlue() / 255f;
        for (int band = 0; band < LATITUDE_BANDS; band++) {
            double lowerLatitude = Math.PI * band / LATITUDE_BANDS - Math.PI / 2;
            double upperLatitude = Math.PI * (band + 1) / LATITUDE_BANDS - Math.PI / 2;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int step = 0; step <= LONGITUDE_BANDS; step++) {
                double longitude = Math.PI * step / LONGITUDE_BANDS - Math.PI / 2;
                float textureX = texture.getTexX() + texture.getTexWidth() * (VISIBLE_TEXTURE_START + VISIBLE_TEXTURE_SHARE * step / LONGITUDE_BANDS);
                vertex(centreX, centreY, radius, upperLatitude, longitude, textureX, (band + 1f) / LATITUDE_BANDS, light, red, green, blue, alpha);
                vertex(centreX, centreY, radius, lowerLatitude, longitude, textureX, (float) band / LATITUDE_BANDS, light, red, green, blue, alpha);
            }
            GL11.glEnd();
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void vertex(float centreX, float centreY, float radius, double latitude, double longitude, float textureX, float latitudeShare,
                        float[] light, float red, float green, float blue, float alpha) {
        float[] normal = {(float) (Math.cos(latitude) * Math.sin(longitude)), (float) Math.sin(latitude),
                (float) (Math.cos(latitude) * Math.cos(longitude))};
        float lit = brightness(normal, light);
        GL11.glColor4f(red * lit, green * lit, blue * lit, alpha);
        GL11.glTexCoord2f(textureX, texture.getTexY() + texture.getTexHeight() * latitudeShare);
        GL11.glVertex2f(centreX + normal[0] * radius, centreY + normal[1] * radius);
    }
}
