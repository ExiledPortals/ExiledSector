package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class AtlasDebrisField {

    private static final String SHEET_PATH = "graphics/debris/debris_sheet0.png";
    private static final String GLOW_SHEET_PATH = "graphics/debris/debris_sheet0_glow.png";
    private static final int SHEET_SIDE = 4;
    private static final int PIECES = 40;
    private static final float REACH = 1.5f;
    private static final float MIN_SIZE = 12f;
    private static final float SIZE_SPREAD = 26f;
    private static final float MIN_LIFE_SECONDS = 10f;
    private static final float LIFE_SPREAD_SECONDS = 10f;
    private static final float FADE_SECONDS = 2f;
    private static final float MIN_SPEED = 2f;
    private static final float SPEED_SPREAD = 6f;
    private static final float MAX_SPIN_DEGREES_PER_SECOND = 18f;
    private static final float GLOW_SHARE = 0.3f;
    private static final float MIN_GLOW_LEG_SECONDS = 1f;
    private static final float GLOW_LEG_SPREAD_SECONDS = 1f;
    private static final float INDICATOR_SHARE = 0.25f;
    private static final float INDICATOR_IN_SECONDS = 3f;
    private static final float MIN_INDICATOR_OUT_SECONDS = 3f;
    private static final float INDICATOR_OUT_SPREAD_SECONDS = 7f;
    private static final float INDICATOR_PADDING = 1.3f;
    private static final float INDICATOR_CORNER_SHARE = 0.25f;
    private static final float INDICATOR_LINE_WIDTH = 1.5f;
    private static final float INDICATOR_ALPHA = 0.7f;
    private static final float MIN_INDICATOR_PIXELS = 6f;
    private static final Color LIGHT = new Color(255, 240, 225);
    private static final Color GLOW = new Color(255, 165, 100);
    private static final Color INDICATOR = new Color(160, 200, 220);
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private final SpriteCache spriteCache;
    private final TexturedQuadBatch quadBatch = new TexturedQuadBatch();
    private final float radius;
    private final int seed;
    private final float pieceScale;
    private final Piece[] pieces;

    private static final class Piece {
        private float x;
        private float y;
        private float size;
        private float angleDeg;
        private int cell;
        private float alpha;
        private float glow;
        private float indicator;
    }

    AtlasDebrisField(SpriteCache spriteCache, float radius, long seed) {
        this(spriteCache, radius, seed, PIECES, 1f);
    }

    AtlasDebrisField(SpriteCache spriteCache, float radius, long seed, int pieceCount, float pieceScale) {
        this.spriteCache = spriteCache;
        this.radius = radius;
        this.seed = (int) (seed ^ (seed >>> 32));
        this.pieceScale = pieceScale;
        this.pieces = new Piece[Math.max(0, pieceCount)];
        for (int slot = 0; slot < pieces.length; slot++) {
            pieces[slot] = new Piece();
        }
    }

    void render(float screenX, float screenY, float zoom, float seconds, float alpha) {
        SpriteAPI sheet = spriteCache.texture(SHEET_PATH);
        if (sheet == null || alpha <= 0f) {
            return;
        }
        for (int slot = 0; slot < pieces.length; slot++) {
            place(pieces[slot], slot, seconds);
        }
        try (TexturedQuadBatch quads = quadBatch.beginNormal(sheet)) {
            for (Piece piece : pieces) {
                draw(quads, piece, screenX, screenY, zoom, LIGHT, alpha * piece.alpha);
            }
        }
        SpriteAPI glowSheet = spriteCache.texture(GLOW_SHEET_PATH);
        if (glowSheet != null) {
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(glowSheet)) {
                for (Piece piece : pieces) {
                    if (piece.glow > 0f) {
                        draw(quads, piece, screenX, screenY, zoom, GLOW, alpha * piece.alpha * piece.glow);
                    }
                }
            }
        }
        drawIndicators(screenX, screenY, zoom, alpha);
    }

    private static void draw(TexturedQuadBatch quads, Piece piece, float screenX, float screenY, float zoom, Color tint, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float size = piece.size * zoom;
        quads.cellAtCenter(SHEET_SIDE, SHEET_SIDE, piece.cell, screenX + piece.x * zoom, screenY - piece.y * zoom, size, size, piece.angleDeg,
                tint, alpha);
    }

    private void drawIndicators(float screenX, float screenY, float zoom, float alpha) {
        try (GlScope scope = GlScope.flat().lineWidth(INDICATOR_LINE_WIDTH)) {
            GL11.glBegin(GL11.GL_LINES);
            for (Piece piece : pieces) {
                if (piece.indicator <= 0f) {
                    continue;
                }
                float side = Math.max(piece.size * zoom * INDICATOR_PADDING, MIN_INDICATOR_PIXELS);
                GL11.glColor4f(INDICATOR.getRed() / 255f, INDICATOR.getGreen() / 255f, INDICATOR.getBlue() / 255f,
                        INDICATOR_ALPHA * alpha * piece.indicator * piece.alpha);
                bracket(screenX + piece.x * zoom, screenY - piece.y * zoom, side / 2f, side * INDICATOR_CORNER_SHARE);
            }
            GL11.glEnd();
        }
    }

    private static void bracket(float centreX, float centreY, float half, float corner) {
        for (int quarter = 0; quarter < 4; quarter++) {
            float signX = quarter % 2 == 0 ? 1f : -1f;
            float signY = quarter < 2 ? 1f : -1f;
            float cornerX = centreX + signX * half;
            float cornerY = centreY + signY * half;
            GL11.glVertex2f(cornerX, cornerY);
            GL11.glVertex2f(cornerX - signX * corner, cornerY);
            GL11.glVertex2f(cornerX, cornerY);
            GL11.glVertex2f(cornerX, cornerY - signY * corner);
        }
    }

    private void place(Piece piece, int slot, float seconds) {
        float lifeSeconds = MIN_LIFE_SECONDS + LIFE_SPREAD_SECONDS * hash(slot, 0, 1);
        float cycleSeconds = lifeSeconds + FADE_SECONDS;
        float aged = seconds / cycleSeconds + hash(slot, 0, 2);
        int life = (int) Math.floor(aged);
        float age = (aged - life) * cycleSeconds;
        float reach = (float) Math.sqrt(hash(slot, life, 3)) * radius * REACH;
        double spawnAngle = TWO_PI * hash(slot, life, 4);
        float spawnX = reach * (float) Math.cos(spawnAngle);
        float spawnY = reach * (float) Math.sin(spawnAngle);
        float direction = hash(slot, life, 5) < 0.5f ? 1f : -1f;
        float speed = MIN_SPEED + SPEED_SPREAD * hash(slot, life, 6);
        float travelX = (float) Math.sin(spawnAngle) * direction * speed;
        float travelY = -(float) Math.cos(spawnAngle) * direction * speed;
        float spin = (hash(slot, life, 7) * 2f - 1f) * MAX_SPIN_DEGREES_PER_SECOND;
        float fade = Math.min(1f, Math.min(age / FADE_SECONDS, (cycleSeconds - age) / FADE_SECONDS));
        piece.x = spawnX + travelX * age;
        piece.y = spawnY + travelY * age;
        piece.size = (MIN_SIZE + SIZE_SPREAD * hash(slot, life, 8)) * pieceScale;
        piece.angleDeg = 360f * hash(slot, life, 9) + spin * age;
        piece.cell = (int) (hash(slot, life, 10) * SHEET_SIDE * SHEET_SIDE);
        piece.alpha = Math.max(0f, fade);
        piece.glow = glow(slot, life, seconds);
        piece.indicator = indicator(slot, life, age, lifeSeconds);
    }

    private float glow(int slot, int life, float seconds) {
        if (hash(slot, life, 11) >= GLOW_SHARE) {
            return 0f;
        }
        float rise = MIN_GLOW_LEG_SECONDS + GLOW_LEG_SPREAD_SECONDS * hash(slot, life, 12);
        float fall = MIN_GLOW_LEG_SECONDS + GLOW_LEG_SPREAD_SECONDS * hash(slot, life, 13);
        float into = (seconds + hash(slot, life, 14) * (rise + fall)) % (rise + fall);
        float bounce = into < rise ? into / rise : 1f - (into - rise) / fall;
        return 0.5f + 0.5f * bounce;
    }

    private float indicator(int slot, int life, float age, float lifeSeconds) {
        if (hash(slot, life, 15) >= INDICATOR_SHARE) {
            return 0f;
        }
        float outSeconds = MIN_INDICATOR_OUT_SECONDS + INDICATOR_OUT_SPREAD_SECONDS * hash(slot, life, 16);
        float start = FADE_SECONDS + Math.max(0f, lifeSeconds - INDICATOR_IN_SECONDS - outSeconds) * hash(slot, life, 17);
        float into = age - start;
        if (into <= 0f || into >= INDICATOR_IN_SECONDS + outSeconds) {
            return 0f;
        }
        return into < INDICATOR_IN_SECONDS ? into / INDICATOR_IN_SECONDS : 1f - (into - INDICATOR_IN_SECONDS) / outSeconds;
    }

    private float hash(int slot, int life, int salt) {
        return ShroudedMawRenderer.hash(slot + seed * 31, life, salt + 40);
    }
}
