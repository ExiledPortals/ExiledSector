package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.Color;

public final class PlanetClouds {

    private PlanetClouds() {
    }

    public static String texturePath(PlanetSpecAPI spec) {
        if (spec == null || spec.getCloudColor() == null) {
            return null;
        }
        String path = spec.getCloudTexture();
        return path == null || path.isEmpty() ? null : path;
    }

    static float turn(PlanetSpecAPI spec, float startDegrees, float seconds) {
        return startDegrees + seconds * spec.getCloudRotation();
    }

    static float layerAlpha(PlanetSpecAPI spec, float alpha) {
        return alpha * spec.getCloudColor().getAlpha() / 255f;
    }

    public static void draw(UnitSphere sphere, SpriteAPI texture, PlanetSpecAPI spec, float x, float y, float radius, float startDegrees,
                            float seconds, Color tint, float alpha) {
        float turn = turn(spec, startDegrees, seconds);
        float layerAlpha = layerAlpha(spec, alpha);
        if (spec.isStar()) {
            PlanetSphere.drawGlow(sphere, texture, spec, x, y, radius, turn, tint, layerAlpha);
        } else {
            PlanetSphere.draw(sphere, texture, spec, x, y, radius, turn, tint, layerAlpha, 0f, 0f);
        }
    }
}
