package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.tree.AtlasBodyArt.Nebula;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.AtlasBodyPainter.Placement;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class AtlasNebulaPainter {

    private static final String CLOUD_SHEET = AtlasSystemArt.NEBULA_SHEET;
    private static final String FLASH_SHEET = "graphics/terrain/deep_hyperspace_blue_glow.png";
    private static final int SHEET_SIDE = AtlasSystemArt.SHEET_SIDE;
    private static final Color LIGHTNING = AtlasSystemArt.LIGHTNING_COLOUR;
    private static final Color IONIZED_LIGHTNING = new Color(60, 230, 210);
    private static final float ANOMALY_SHARE_OF_CLOUDS = 0.5f;
    private static final float CLOUD_ALPHA = 0.85f;
    private static final float BACK_PUFF_SHADE = 0.6f;
    private static final float INNER_GLOW_ALPHA = 0.7f;
    private static final float FLASH_CELL_SHARE = 0.9f;
    private static final float AURORA_INNER = 0.42f;
    private static final float AURORA_OUTER = 0.85f;
    private static final float AURORA_ALPHA = 0.4f;
    private static final float AURORA_SHIFT_RATE = 0.5f;

    private final SpriteCache sprites = new SpriteCache(AtlasNebulaPainter.class);
    private final Map<Color, Color> cloudByColour = new HashMap<>();
    private final NebulaWeather.PuffPlace place = new NebulaWeather.PuffPlace();
    private final Vector2f bandCentre = new Vector2f();
    private SpriteAPI cloudSheet;
    private NebulaWeather.Style style;
    private Color cloudTint;
    private float cloudAlpha;
    private float flashAlpha;
    private float cloudSeconds;
    private int seed;
    private float centreX;
    private float centreY;
    private float radius;

    void paint(Nebula nebula, Color colour, Placement placement, Lighting lighting, float realSeconds, float alpha) {
        cloudSheet = sprites.sprite(CLOUD_SHEET);
        if (cloudSheet == null || alpha <= 0f) {
            return;
        }
        style = NebulaWeather.styleOf(nebula);
        cloudTint = AtlasBodyPainter.tintFor(lighting, cloudByColour.computeIfAbsent(colour, AtlasNebulaPainter::cloudOf));
        cloudAlpha = alpha * CLOUD_ALPHA * StormWeather.cloudShare(lighting);
        flashAlpha = alpha * StormWeather.flashShare(lighting);
        cloudSeconds = placement.stormSeconds();
        seed = (int) placement.seedDegrees();
        centreX = placement.screenX();
        centreY = placement.screenY();
        radius = placement.screenRadius();
        boolean ionized = nebula == Nebula.IONIZED;
        int auroraAfter = style.puffs() / 2;
        for (int puff = 0; puff < style.puffs(); puff++) {
            NebulaWeather.place(style, seed, puff, cloudSeconds, place);
            float depth = style.puffs() <= 1 ? 1f : puff / (float) (style.puffs() - 1);
            drawPuff(cloudTint, cloudAlpha * (BACK_PUFF_SHADE + (1f - BACK_PUFF_SHADE) * depth) * place.presence, false);
            if (ionized && puff == auroraAfter) {
                paintAurora(lighting);
            }
        }
        paintFlashes(ionized, NebulaWeather.flashChance(style, StormWeather.flashChance(lighting)), realSeconds);
    }

    private static Color cloudOf(Color colour) {
        return ColorUtil.mix(Color.WHITE, colour, ANOMALY_SHARE_OF_CLOUDS);
    }

    private void drawPuff(Color tint, float puffAlpha, boolean additive) {
        if (puffAlpha <= 0f) {
            return;
        }
        float size = place.halfSize * 2f * radius;
        SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_SIDE, SHEET_SIDE, place.cell, centreX + place.offsetX * radius, centreY + place.offsetY * radius,
                size, size, place.turnDegrees, tint, puffAlpha, additive);
    }

    private void paintAurora(Lighting lighting) {
        SpriteAPI aurora = sprites.texture(AtlasSystemArt.AURORA_TEXTURE);
        if (aurora == null) {
            return;
        }
        float shift = Fade.pulse(cloudSeconds * AURORA_SHIFT_RATE + seed);
        Color auroraColour = AtlasBodyPainter.tintFor(lighting,
                Misc.interpolateColor(AtlasSystemArt.AURORA_COLOUR, AtlasSystemArt.AURORA_SHIFT_COLOUR, shift));
        bandCentre.set(centreX, centreY);
        AuroraBeltRenderer.render(aurora, new RadialBand(bandCentre, radius * AURORA_INNER, radius * AURORA_OUTER), auroraColour,
                flashAlpha * AURORA_ALPHA * MagneticWeather.shimmer(seed, cloudSeconds), cloudSeconds, 1f);
    }

    private void paintFlashes(boolean ionized, float chance, float realSeconds) {
        SpriteAPI flashSheet = sprites.sprite(FLASH_SHEET);
        for (int stream = 0; stream < style.flashStreams(); stream++) {
            int streamSeed = NebulaWeather.streamSeed(seed, stream);
            float progress = NebulaWeather.flashProgress(style, streamSeed, realSeconds, chance);
            if (progress < 0f) {
                continue;
            }
            int slot = NebulaWeather.flashSlot(style, streamSeed, realSeconds);
            float brightness = NebulaWeather.flicker(streamSeed, slot, progress);
            if (brightness <= 0f) {
                continue;
            }
            NebulaWeather.place(style, seed, StormWeather.pick(streamSeed, slot, style.puffs()), cloudSeconds, place);
            Color flashColour = ionized && Math.floorMod(slot + stream, 2) == 0 ? IONIZED_LIGHTNING : LIGHTNING;
            float strength = flashAlpha * brightness * place.presence;
            drawPuff(flashColour, strength * INNER_GLOW_ALPHA, true);
            if (flashSheet != null) {
                float size = place.halfSize * 2f * radius * FLASH_CELL_SHARE;
                SpriteDraw.drawCellAtCenter(flashSheet, SHEET_SIDE, SHEET_SIDE, place.cell + slot, centreX + place.offsetX * radius,
                        centreY + place.offsetY * radius, size, size, place.turnDegrees, flashColour, strength, true);
            }
        }
    }
}
