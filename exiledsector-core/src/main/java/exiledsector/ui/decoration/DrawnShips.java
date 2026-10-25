package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

import java.util.function.Consumer;

final class DrawnShips {

    static final float MIN_SHIP_PIXELS = 1.5f;
    private static final Consumer<TexturedQuadBatch> NO_EXTRA_GLOWS = quads -> { };

    private final SpriteCache spriteCache;
    private final FlameBatch flameBatch = new FlameBatch();
    private final TexturedQuadBatch glowQuads = new TexturedQuadBatch();
    private RoamingShip[] ships = new RoamingShip[0];
    private float[] screenX = new float[0];
    private float[] screenY = new float[0];
    private float[] screenScale = new float[0];
    private float[] engineLevel = new float[0];
    private float[] alpha = new float[0];
    private int count;

    DrawnShips(SpriteCache spriteCache) {
        this.spriteCache = spriteCache;
    }

    void begin(int capacity) {
        count = 0;
        if (capacity > ships.length) {
            ships = new RoamingShip[capacity];
            screenX = new float[capacity];
            screenY = new float[capacity];
            screenScale = new float[capacity];
            engineLevel = new float[capacity];
            alpha = new float[capacity];
        }
    }

    boolean add(RoamingShip ship, float shipScreenX, float shipScreenY, float shipScreenScale, float shipEngineLevel, float shipAlpha) {
        if (shipAlpha <= 0f || Math.max(ship.width(), ship.height()) * shipScreenScale < MIN_SHIP_PIXELS) {
            return false;
        }
        ships[count] = ship;
        screenX[count] = shipScreenX;
        screenY[count] = shipScreenY;
        screenScale[count] = shipScreenScale;
        engineLevel[count] = shipEngineLevel;
        alpha[count] = shipAlpha;
        count++;
        return true;
    }

    boolean isEmpty() {
        return count == 0;
    }

    void renderFlames(float zoom) {
        if (count == 0) return;
        SpriteAPI flameTexture = spriteCache.texture(HullEngines.FLAME_PATH);
        if (flameTexture == null) return;
        try (FlameBatch flames = flameBatch.begin(flameTexture)) {
            for (int i = 0; i < count; i++) {
                RoamingShip ship = ships[i];
                ship.engines().addFlames(flames, screenX[i], screenY[i], Math.toRadians(ship.drift().facingDeg()), screenScale[i], zoom,
                        HullEngines.flameLengthFactor(engineLevel[i]), alpha[i]);
            }
        }
    }

    void renderGlows() {
        renderGlows(NO_EXTRA_GLOWS);
    }

    void renderGlows(Consumer<TexturedQuadBatch> extraGlows) {
        SpriteAPI glowTexture = spriteCache.texture(HullEngines.GLOW_PATH);
        if (glowTexture == null) return;
        try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
            for (int i = 0; i < count; i++) {
                RoamingShip ship = ships[i];
                ship.engines().addGlows(quads, screenX[i], screenY[i], Math.toRadians(ship.drift().facingDeg()), screenScale[i],
                        HullEngines.glowFactor(engineLevel[i]), alpha[i]);
            }
            extraGlows.accept(quads);
        }
    }
}
