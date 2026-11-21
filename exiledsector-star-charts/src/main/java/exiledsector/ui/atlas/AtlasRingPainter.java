package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.CategorySprite;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class AtlasRingPainter {

    private static final String TEXTURE_CATEGORY = "misc";
    private static final float SLOT_WIDTH = 256f;
    private static final float ANOMALY_SHARE_OF_RING = 0.2f;
    private static final float MIN_ROCK_PIXELS = 1.5f;
    private static final String[] ROCK_PATHS = new String[AtlasPlanetRings.ROCK_SPRITES];

    static {
        for (int index = 0; index < ROCK_PATHS.length; index++) {
            ROCK_PATHS[index] = String.format("graphics/asteroids/ring_asteroid%02d.png", index);
        }
    }

    private final SpriteCache sprites = new SpriteCache(AtlasRingPainter.class);
    private final Map<String, CategorySprite> textures = new HashMap<>();
    private final Map<Color, Color> ringColourByAnomaly = new HashMap<>();
    private final Vector2f bandCentre = new Vector2f();

    void paint(AtlasPlanetRings rings, Color colour, float screenX, float screenY, float footprintRadius, Lighting lighting, float seconds,
               float alpha) {
        if (rings == null || alpha <= 0f) {
            return;
        }
        Color ringColour = AtlasBodyPainter.tintFor(lighting, ringColourByAnomaly.computeIfAbsent(colour, AtlasRingPainter::ringColourOf));
        bandCentre.set(screenX, screenY);
        for (AtlasPlanetRings.Band band : rings.bands()) {
            SpriteAPI texture = textures.computeIfAbsent(band.textureId(), id -> new CategorySprite(TEXTURE_CATEGORY, id)).get();
            if (texture != null) {
                RingBeltRenderer.renderSlot(texture, new RadialBand(bandCentre, footprintRadius * band.innerShare(), footprintRadius * band.outerShare()),
                        band.slot(), SLOT_WIDTH, ringColour, alpha * band.alpha(), band.startDegrees() + seconds * band.degreesPerSecond());
            }
        }
        Color rockTint = AtlasBodyPainter.tintFor(lighting, Color.WHITE);
        for (AtlasPlanetRings.Rock rock : rings.rocks()) {
            paintRock(rock, screenX, screenY, footprintRadius, rockTint, seconds, alpha);
        }
    }

    private void paintRock(AtlasPlanetRings.Rock rock, float screenX, float screenY, float footprintRadius, Color tint, float seconds, float alpha) {
        float size = footprintRadius * rock.sizeShare() * 2f;
        if (size < MIN_ROCK_PIXELS) {
            return;
        }
        SpriteAPI sprite = sprites.sprite(ROCK_PATHS[rock.sprite()]);
        float longest = sprite == null ? 0f : Math.max(sprite.getWidth(), sprite.getHeight());
        if (longest <= 0f) {
            return;
        }
        double angle = Math.toRadians(rock.startDegrees() + seconds * rock.degreesPerSecond());
        float orbit = footprintRadius * rock.orbitShare();
        float scale = size / longest;
        SpriteDraw.drawAtCenter(sprite, screenX + (float) Math.cos(angle) * orbit, screenY + (float) Math.sin(angle) * orbit, sprite.getWidth() * scale,
                sprite.getHeight() * scale, tint, alpha, rock.facingDegrees() + seconds * rock.spinDegreesPerSecond());
    }

    private static Color ringColourOf(Color anomalyColour) {
        return ColorUtil.mix(Color.WHITE, anomalyColour, ANOMALY_SHARE_OF_RING);
    }
}
