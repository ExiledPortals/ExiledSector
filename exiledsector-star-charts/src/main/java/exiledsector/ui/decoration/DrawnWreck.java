package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.Random;

final class DrawnWreck {

    static final Color HULL_TINT = new Color(150, 140, 132);
    private static final String EMBER_PATH = "graphics/fx/hit_glow.png";
    private static final float EMBER_SPREAD = 0.3f;
    private static final float MIN_EMBER_PIXELS = 3f;
    private static final float FULL_TURN = (float) (Math.PI * 2.0);

    record Embers(int count, Color colour, float flickersPerSecond, float sharpness, float sizeShare) {

        static final Embers NONE = new Embers(0, Color.WHITE, 0f, 1f, 0f);
    }

    record Tumble(float startAngleDeg, float degPerSecond) {
    }

    private final SpriteCache spriteCache = new SpriteCache(DrawnWreck.class);
    private final String spritePath;
    private final float width;
    private final float height;
    private final float scale;
    private final Tumble tumble;
    private final Embers embers;
    private final float[] emberAcross;
    private final float[] emberAlong;
    private final float[] emberPhase;

    private DrawnWreck(String spritePath, SpriteAPI texture, float scale, Tumble tumble, Embers embers, Random random) {
        this.spritePath = spritePath;
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.scale = scale;
        this.tumble = tumble;
        this.embers = embers;
        this.emberAcross = new float[embers.count()];
        this.emberAlong = new float[embers.count()];
        this.emberPhase = new float[embers.count()];
        for (int ember = 0; ember < embers.count(); ember++) {
            emberAcross[ember] = (random.nextFloat() * 2f - 1f) * EMBER_SPREAD;
            emberAlong[ember] = (random.nextFloat() * 2f - 1f) * EMBER_SPREAD;
            emberPhase[ember] = random.nextFloat() * FULL_TURN;
        }
    }

    static DrawnWreck of(ShipHullSpecAPI hull, SpriteCache spriteCache, float scale, Tumble tumble, Embers embers, Random random) {
        String spritePath = hull.getSpriteName();
        SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
        return texture == null ? null : new DrawnWreck(spritePath, texture, scale, tumble, embers, random);
    }

    float halfLength() {
        return Math.max(width, height) * scale / 2f;
    }

    private float angleAt(float seconds) {
        return tumble.startAngleDeg() + tumble.degPerSecond() * seconds;
    }

    void renderHull(float worldX, float worldY, SceneFrame frame) {
        float screenScale = scale * frame.zoom();
        SpriteDraw.drawAtCenter(spriteCache, spritePath, frame.screenXOf(worldX), frame.screenYOf(worldY), width * screenScale, height * screenScale,
                HULL_TINT, frame.alpha(), angleAt(frame.seconds()));
    }

    void renderEmbers(float worldX, float worldY, SceneFrame frame) {
        if (embers.count() == 0 || frame.alpha() <= 0f) {
            return;
        }
        float seconds = frame.seconds();
        float screenX = frame.screenXOf(worldX);
        float screenY = frame.screenYOf(worldY);
        float screenScale = scale * frame.zoom();
        double angle = Math.toRadians(angleAt(seconds));
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float size = Math.max(MIN_EMBER_PIXELS, Math.max(width, height) * screenScale * embers.sizeShare());
        for (int ember = 0; ember < embers.count(); ember++) {
            float flicker = (float) Math.pow(0.5 + 0.5 * Math.sin(seconds * embers.flickersPerSecond() * FULL_TURN + emberPhase[ember]),
                    embers.sharpness());
            if (flicker <= 0.01f) {
                continue;
            }
            float localX = emberAcross[ember] * width * screenScale;
            float localY = emberAlong[ember] * height * screenScale;
            SpriteDraw.drawAdditiveAtCenter(spriteCache, EMBER_PATH, screenX + localX * cos - localY * sin, screenY + localX * sin + localY * cos,
                    size, size, embers.colour(), frame.alpha() * flicker);
        }
    }
}
