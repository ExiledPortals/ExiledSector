package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.AtlasBodyPainter.Placement;
import exiledsector.ui.decoration.PlanetClouds;
import exiledsector.ui.decoration.PlanetSphere;
import exiledsector.ui.decoration.UnitSphere;
import exiledsector.ui.util.ColorUtil;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class AtlasCloudPainter {

    enum Cover { NONE, CALM, STORM }

    private static final float CLOUD_LIFT = 1.025f;
    private static final float SHEAR_LIFT = 1.045f;

    private static final String STORM_SWIRL_TEXTURE = "graphics/planets/clouds_terran03.png";
    private static final String STORM_SHEAR_TEXTURE = "graphics/planets/clouds_white.png";
    private static final Color STORM_SHADOW = new Color(62, 52, 96);
    private static final float ANOMALY_SHARE_OF_STORM = 0.25f;
    private static final float SWIRL_ALPHA = 0.7f;
    private static final float SHEAR_ALPHA = 0.45f;
    private static final float SWIRL_DRIFT_DEGREES_PER_SECOND = 16f;
    private static final float SHEAR_DRIFT_DEGREES_PER_SECOND = -11f;
    private static final float CHURN_DEGREES = 6f;
    private static final float CHURN_SECONDS = 2.3f;
    private static final float INNER_LIGHTNING_SECONDS = 0.6f;
    private static final float INNER_LIGHTNING_CHANCE_MULT = 1.5f;
    private static final float INNER_LIGHTNING_ALPHA = 0.55f;
    private static final int INNER_LIGHTNING_SALT = 7919;
    private static final Color INNER_LIGHTNING = AtlasSystemArt.LIGHTNING_COLOUR;

    private final SpriteCache sprites = new SpriteCache(AtlasCloudPainter.class);
    private final Map<Color, Color> stormShadeByColour = new HashMap<>();

    static Cover coverOf(AtlasBodyArt.Storm storm, AtlasBodyArt.Field field, String siteLight, boolean planetHasClouds) {
        if (field != AtlasBodyArt.Field.NONE || siteLight != null) {
            return Cover.NONE;
        }
        return switch (storm) {
            case GENERIC, LIGHTNING_ROD, SUPERCELL, EYE, CAPTURED, DUEL -> Cover.STORM;
            case NONE, SOLAR -> planetHasClouds ? Cover.CALM : Cover.NONE;
        };
    }

    void paint(AtlasBodyArt art, Color colour, PlanetSpecAPI spec, UnitSphere sphere, Placement placement, float turnDegrees, Lighting lighting,
               float seconds, float alpha) {
        if (spec.isStar() || spec.isBlackHole()) {
            return;
        }
        String calmTexture = PlanetClouds.texturePath(spec);
        boolean planetHasClouds = calmTexture != null;
        switch (coverOf(art.storm(), art.field(), art.lightColour(), planetHasClouds)) {
            case CALM -> paintCalm(calmTexture, spec, sphere, placement, lighting, seconds, alpha);
            case STORM -> paintStorm(colour, spec, sphere, placement, turnDegrees, lighting, seconds, alpha);
            case NONE -> {
            }
        }
    }

    private void paintCalm(String texturePath, PlanetSpecAPI spec, UnitSphere sphere, Placement placement, Lighting lighting, float seconds,
                           float alpha) {
        SpriteAPI layer = sprites.texture(texturePath);
        if (layer != null) {
            PlanetClouds.draw(sphere, layer, spec, placement.screenX(), placement.screenY(), placement.screenRadius(), placement.spinDegrees(), seconds,
                    AtlasBodyPainter.tintFor(lighting, spec.getCloudColor()), alpha);
        }
    }

    private void paintStorm(Color colour, PlanetSpecAPI spec, UnitSphere sphere, Placement placement, float turnDegrees, Lighting lighting,
                            float seconds, float alpha) {
        SpriteAPI swirl = sprites.texture(STORM_SWIRL_TEXTURE);
        SpriteAPI shear = sprites.texture(STORM_SHEAR_TEXTURE);
        if (swirl == null || shear == null) {
            return;
        }
        float stormSeconds = placement.stormSeconds();
        float churn = CHURN_DEGREES * (float) Math.sin(stormSeconds / CHURN_SECONDS);
        float swirlTurn = turnDegrees + stormSeconds * SWIRL_DRIFT_DEGREES_PER_SECOND + churn;
        float shearTurn = turnDegrees + stormSeconds * SHEAR_DRIFT_DEGREES_PER_SECOND - churn;
        Color shade = AtlasBodyPainter.tintFor(lighting, stormShadeByColour.computeIfAbsent(colour, AtlasCloudPainter::stormShadeOf));
        float cloudAlpha = alpha * StormWeather.cloudShare(lighting);
        float x = placement.screenX();
        float y = placement.screenY();
        float radius = placement.screenRadius();
        PlanetSphere.draw(sphere, swirl, spec, x, y, radius * CLOUD_LIFT, swirlTurn, shade, cloudAlpha * SWIRL_ALPHA, 0f, 0f);
        PlanetSphere.draw(sphere, shear, spec, x, y, radius * SHEAR_LIFT, shearTurn, shade, cloudAlpha * SHEAR_ALPHA, 0f, 0f);
        int seed = (int) placement.seedDegrees() + INNER_LIGHTNING_SALT;
        float chance = Math.min(1f, StormWeather.flashChance(lighting) * INNER_LIGHTNING_CHANCE_MULT);
        float strength = StormWeather.flash(seed, seconds, INNER_LIGHTNING_SECONDS, chance);
        if (strength > 0f) {
            PlanetSphere.drawGlow(sphere, swirl, spec, x, y, radius * CLOUD_LIFT, swirlTurn, INNER_LIGHTNING,
                    alpha * StormWeather.flashShare(lighting) * INNER_LIGHTNING_ALPHA * strength);
        }
    }

    private static Color stormShadeOf(Color colour) {
        return ColorUtil.mix(STORM_SHADOW, colour, ANOMALY_SHARE_OF_STORM);
    }
}
