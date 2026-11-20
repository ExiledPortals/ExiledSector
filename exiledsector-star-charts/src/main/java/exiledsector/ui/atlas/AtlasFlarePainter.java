package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Flare;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.AtlasBodyPainter.Placement;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class AtlasFlarePainter {

    private static final String SHIMMER_PATH = "graphics/fx/hit_glow.png";
    private static final float SHIMMER_SIZE = 2.8f;
    private static final float SHIMMER_ALPHA = 0.18f;
    private static final float GLINT_SIZE = 2.6f;
    private static final float GLINT_CORE_SIZE = 0.9f;

    private final SpriteCache sprites = new SpriteCache(AtlasFlarePainter.class);
    private final StarShade starShade = new StarShade();

    void paintGlint(Flare flare, Color colour, Placement placement, Lighting lighting, float alpha) {
        if (flare == Flare.NONE || alpha <= 0f) {
            return;
        }
        FlareWeather.Style style = FlareWeather.styleOf(flare);
        int streamSeed = FlareWeather.streamSeed((int) placement.seedDegrees(), 0);
        float glint = FlareWeather.glint(FlareWeather.progress(streamSeed, placement.stormSeconds(), style));
        if (glint <= 0f) {
            return;
        }
        Color glintColour = starShade.of(style.colourOr(colour), lighting);
        float glintAlpha = alpha * FlareWeather.glowShare(lighting) * glint;
        float outer = placement.screenRadius() * GLINT_SIZE;
        float core = placement.screenRadius() * GLINT_CORE_SIZE;
        SpriteDraw.drawAdditiveAtCenter(sprites, SHIMMER_PATH, placement.screenX(), placement.screenY(), outer, outer, glintColour, glintAlpha * 0.6f);
        SpriteDraw.drawAdditiveAtCenter(sprites, SHIMMER_PATH, placement.screenX(), placement.screenY(), core, core, Color.WHITE, glintAlpha);
    }

    void paint(Flare flare, Color colour, Placement placement, Lighting lighting, float alpha) {
        if (flare == Flare.NONE || alpha <= 0f) {
            return;
        }
        FlareWeather.Style style = FlareWeather.styleOf(flare);
        Color flareColour = starShade.of(style.colourOr(colour), lighting);
        float glowAlpha = alpha * FlareWeather.glowShare(lighting);
        int seed = (int) placement.seedDegrees();
        float flareSeconds = placement.stormSeconds();
        float starX = placement.screenX();
        float starY = placement.screenY();
        float starRadius = placement.screenRadius();
        if (!style.arched()) {
            float shimmerSize = starRadius * SHIMMER_SIZE;
            SpriteDraw.drawAdditiveAtCenter(sprites, SHIMMER_PATH, starX, starY, shimmerSize, shimmerSize, flareColour,
                    glowAlpha * SHIMMER_ALPHA * FlareWeather.shimmer(seed, flareSeconds));
        }
        for (int stream = 0; stream < style.streams(); stream++) {
            int streamSeed = FlareWeather.streamSeed(seed, stream);
            float progress = FlareWeather.progress(streamSeed, flareSeconds, style);
            if (progress < 0f) {
                continue;
            }
            int slot = FlareWeather.slotOf(streamSeed, flareSeconds, style);
            double angle = Math.toRadians(FlareWeather.angleDegrees(flare, streamSeed, slot, placement.headingDegrees(), flareSeconds));
            if (style.arched()) {
                StarFlareGlow.drawArch(sprites, starX, starY, starRadius, angle, progress, flareColour, glowAlpha);
            } else {
                StarFlareGlow.drawRising(sprites, starX, starY, starRadius, angle, progress, style.rise(), flareColour, glowAlpha);
            }
        }
    }
}
