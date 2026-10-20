package exiledsector.ui.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.UnitCircle;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class AnchorIconFx {

    static final float CYCLE_SECONDS = 6f;
    static final String TUNNEL_TEXTURE = "graphics/fx/radial_fx.png";
    static final String HALO_TEXTURE = "graphics/fx/star_halo.png";
    static final String RING_TEXTURE = "graphics/fx/shields256ringd.png";
    static final String LIGHT_TEXTURE = "graphics/fx/glow64.png";
    static final String BUNDLED_CREST_DIRECTORY = "graphics/icons/crests/";
    static final float IDLE_WAVE = -10f;
    private static final float DESIGN_SIZE = 80f;
    private static final float DESIGN_CENTRE = 40f;
    private static final float GEM_X = 36.5f;
    private static final float GEM_Y_FROM_TOP = 31f;
    private static final float SQUASH = 0.85f;
    private static final float[] TUNNEL_RINGS = {0.06f, 0.1f, 0.15f, 0.21f, 0.3f, 0.42f, 0.56f, 0.72f, 0.86f, 1f};
    private static final int TUNNEL_SEGMENTS = 32;
    private static final UnitCircle TUNNEL_CIRCLE = UnitCircle.of(TUNNEL_SEGMENTS);
    private static final float TUNNEL_LOOP_SECONDS = 2.6f;
    private static final float TUNNEL_SPIN = 0.25f;
    private static final float TUNNEL_DEPTH = 0.22f;
    private static final float TUNNEL_TWIST = 0.12f;
    private static final float TUNNEL_REPEATS = 2f;
    private static final float TUNNEL_IDLE_ALPHA = 0.45f;
    private static final float TUNNEL_OPEN_ALPHA = 0.5f;
    private static final float OPEN_START = 0.30f;
    private static final float OPEN_FULL = 0.40f;
    private static final float CLOSE_START = 0.66f;
    private static final float CLOSE_END = 0.78f;
    private static final float APERTURE_RADIUS = 7.5f;
    private static final float APERTURE_CORE = 0.7f;
    private static final int APERTURE_SEGMENTS = 24;
    private static final UnitCircle APERTURE_CIRCLE = UnitCircle.of(APERTURE_SEGMENTS);
    private static final float HALO_SIZE = 26f;
    private static final int INFALL_RINGS = 3;
    private static final float RING_START = 0.34f;
    private static final float RING_STAGGER = 0.07f;
    private static final float RING_SPAN = 0.3f;
    private static final float RING_SPIN = 1.5f;
    private static final float RING_MIN_RADIUS = 3f;
    private static final float LOCK_FADE_IN = 0.06f;
    private static final float LOCK_TRAVEL_END = 0.22f;
    private static final float LOCK_BLINK_END = 0.32f;
    private static final float LOCK_FADE_END = 0.44f;
    private static final float LOCK_BLINK_STEP = 0.025f;
    private static final float LOCK_BLINK_DIM = 0.25f;
    private static final float LOCK_START_DISTANCE = 34f;
    private static final float LOCK_END_DISTANCE = 9f;
    private static final float LOCK_ARM = 3.2f;
    private static final float LOCK_SWING = 0.6f;
    private static final float FRAME_OUTER = 39f;
    private static final float FRAME_LINE_INNER = 37.5f;
    private static final float FRAME_GLOW_INNER = 35f;
    private static final float FRAME_HALF = 38.25f;
    private static final int LIGHTS_PER_SIDE = 10;
    private static final float LIGHT_SPACING = 0.2f;
    private static final float LIGHT_WIDTH = 0.13f;
    private static final float LIGHT_IDLE_SIZE = 3f;
    private static final float LIGHT_LIT_SIZE = 6f;
    private static final float WAVE_OFFSET = 0.79f;
    private static final float WAVE_SPAN = 2.8f;
    private static final float WAVE_START = -0.2f;
    private static final float WAVE_ARRIVAL = 2f;
    private static final float FLASH_WIDTH = 0.18f;
    private static final float FLASH_SIZE = 34f;
    private static final float BADGE_CENTRE_X = 63f;
    private static final float BADGE_CENTRE_Y = 17f;
    private static final float BADGE_RADIUS = 13f;
    private static final int BADGE_SEGMENTS = 32;
    private static final UnitCircle BADGE_CIRCLE = UnitCircle.of(BADGE_SEGMENTS);
    private static final Color BADGE_RIM = new Color(20, 22, 28);
    private static final Logger LOG = Logger.getLogger(AnchorIconFx.class);
    private static final Map<String, String> CREST_PATHS = new HashMap<>();
    private static final TexturedQuadBatch LIGHT_BATCH = new TexturedQuadBatch();

    private AnchorIconFx() {
    }

    static void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult,
                       int phaseSeed) {
        boolean animated = ItemAnimationSetting.mode() == ItemAnimationSetting.Mode.FULL;
        float size = Math.min(w, h);
        float unit = size / DESIGN_SIZE;
        float left = x + (w - size) / 2f;
        float bottom = y + (h - size) / 2f;
        float cx = left + DESIGN_CENTRE * unit;
        float cy = bottom + DESIGN_CENTRE * unit;
        boolean framed = look.framed();
        float inset = framed ? CargoIconFx.ICON_INSET : 1f;
        float gemX = cx + (GEM_X - DESIGN_CENTRE) * inset * unit;
        float gemY = cy + (DESIGN_CENTRE - GEM_Y_FROM_TOP) * inset * unit;
        float reach = tunnelReach(inset);
        Color color = look.color();
        float t = CargoIconFx.seconds() + CargoIconFx.hash(phaseSeed, 1, 1) * CYCLE_SECONDS;
        float cycle = cycleOf(t);
        float wave = animated ? wave(cycle) : IDLE_WAVE;

        if (animated) {
            drawTunnel(sprites, color, gemX, gemY, reach * unit, t, (TUNNEL_IDLE_ALPHA + TUNNEL_OPEN_ALPHA * openness(cycle)) * alphaMult);
        }
        CargoIconFx.drawIcon(sprites, iconPath, cx, cy, size * inset, alphaMult);
        if (animated) {
            drawAperture(sprites, color, gemX, gemY, unit, openness(cycle), alphaMult);
            drawInfallRings(sprites, color, gemX, gemY, unit, reach, cycle, t, alphaMult);
            drawLockOn(color, gemX, gemY, unit, lockStartDistance(reach), cycle, alphaMult);
        }
        if (framed) {
            drawFrame(color, cx, cy, unit, alphaMult);
            drawLights(sprites, color, cx, cy, unit, wave, alphaMult);
        }
        AnchorLook anchor = look.anchor();
        float flash = animated && framed ? badgeFlash(wave) : 0f;
        drawBadge(sprites, crestPath(anchor == null ? null : anchor.crestFactionId()), color, left + BADGE_CENTRE_X * unit,
                bottom + BADGE_CENTRE_Y * unit, unit, framed, flash, alphaMult);
    }

    static float cycleOf(float seconds) {
        float cycle = seconds % CYCLE_SECONDS / CYCLE_SECONDS;
        return cycle < 0f ? cycle + 1f : cycle;
    }

    static float openness(float cycle) {
        return smooth(OPEN_START, OPEN_FULL, cycle) * (1f - smooth(CLOSE_START, CLOSE_END, cycle));
    }

    static float lockTravel(float cycle) {
        return smooth(0f, LOCK_TRAVEL_END, cycle);
    }

    static float lockAlpha(float cycle) {
        float alpha = smooth(0f, LOCK_FADE_IN, cycle) * (1f - smooth(LOCK_BLINK_END, LOCK_FADE_END, cycle));
        if (cycle > LOCK_TRAVEL_END && cycle < LOCK_BLINK_END && (int) ((cycle - LOCK_TRAVEL_END) / LOCK_BLINK_STEP) % 2 == 1) {
            alpha *= LOCK_BLINK_DIM;
        }
        return alpha;
    }

    static float infallProgress(float cycle, int ring) {
        return (cycle - RING_START - ring * RING_STAGGER) / RING_SPAN;
    }

    static float wave(float cycle) {
        return (cycle + WAVE_OFFSET) % 1f * WAVE_SPAN + WAVE_START;
    }

    static float lightLevel(float distance, float wave) {
        float offset = (distance - wave) / LIGHT_WIDTH;
        return (float) Math.exp(-offset * offset);
    }

    static float badgeFlash(float wave) {
        float offset = (wave - WAVE_ARRIVAL) / FLASH_WIDTH;
        return (float) Math.exp(-offset * offset);
    }

    static float tunnelReach(float inset) {
        float fromLeft = DESIGN_CENTRE + (GEM_X - DESIGN_CENTRE) * inset;
        float fromTop = DESIGN_CENTRE + (GEM_Y_FROM_TOP - DESIGN_CENTRE) * inset;
        return Math.min(Math.min(fromLeft, fromTop), Math.min(DESIGN_SIZE - fromLeft, DESIGN_SIZE - fromTop));
    }

    static float tunnelFade(float ring) {
        return smooth(TUNNEL_RINGS[0], 0.3f, ring) * (1f - smooth(0.62f, 1f, ring));
    }

    static float[] framePoint(int path, float distance) {
        float along = distance < 1f ? distance : distance - 1f;
        float edge = -FRAME_HALF + along * 2f * FRAME_HALF;
        if (path == 0) {
            return distance < 1f ? new float[]{edge, -FRAME_HALF} : new float[]{FRAME_HALF, edge};
        }
        return distance < 1f ? new float[]{-FRAME_HALF, edge} : new float[]{edge, FRAME_HALF};
    }

    static float smooth(float from, float to, float value) {
        float progress = Math.max(0f, Math.min(1f, (value - from) / (to - from)));
        return progress * progress * (3f - 2f * progress);
    }

    static Color lighten(Color color, float amount) {
        return new Color(lightenChannel(color.getRed(), amount), lightenChannel(color.getGreen(), amount), lightenChannel(color.getBlue(), amount));
    }

    private static int lightenChannel(int channel, float amount) {
        return Math.round(channel + (255 - channel) * amount);
    }

    private static void drawTunnel(SpriteCache sprites, Color color, float gemX, float gemY, float radius, float t, float alpha) {
        SpriteAPI texture = sprites.texture(TUNNEL_TEXTURE);
        if (texture == null || alpha <= 0f) {
            return;
        }
        float scroll = t % TUNNEL_LOOP_SECONDS / TUNNEL_LOOP_SECONDS;
        double spin = t * TUNNEL_SPIN % (Math.PI * 2.0);
        float spinCos = (float) Math.cos(spin);
        float spinSin = (float) Math.sin(spin);
        float textureWidth = texture.getTextureWidth();
        float textureHeight = texture.getTextureHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            texture.bindTexture();
            for (int band = 0; band < TUNNEL_RINGS.length - 1; band++) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (int segment = 0; segment <= TUNNEL_SEGMENTS; segment++) {
                    int index = segment % TUNNEL_SEGMENTS;
                    float cos = TUNNEL_CIRCLE.cos(index) * spinCos - TUNNEL_CIRCLE.sin(index) * spinSin;
                    float sin = TUNNEL_CIRCLE.sin(index) * spinCos + TUNNEL_CIRCLE.cos(index) * spinSin;
                    float around = (float) segment / TUNNEL_SEGMENTS;
                    for (int edge = 0; edge < 2; edge++) {
                        float ring = TUNNEL_RINGS[band + edge];
                        Misc.setColor(color, tunnelFade(ring) * alpha);
                        GL11.glTexCoord2f((around * TUNNEL_REPEATS + TUNNEL_TWIST / ring) * textureWidth, (TUNNEL_DEPTH / ring + scroll) * textureHeight);
                        GL11.glVertex2f(gemX + cos * radius * ring, gemY + sin * radius * ring);
                    }
                }
                GL11.glEnd();
            }
        }
    }

    private static void drawAperture(SpriteCache sprites, Color color, float gemX, float gemY, float unit, float open, float alphaMult) {
        if (open <= 0f) {
            return;
        }
        float radius = APERTURE_RADIUS * open * unit;
        float core = radius * APERTURE_CORE;
        try (GlScope scope = GlScope.flat()) {
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            Misc.setColor(Color.BLACK, 0.95f * alphaMult);
            GL11.glVertex2f(gemX, gemY);
            Misc.setColor(Color.BLACK, 0.85f * alphaMult);
            for (int segment = 0; segment <= APERTURE_SEGMENTS; segment++) {
                int index = segment % APERTURE_SEGMENTS;
                GL11.glVertex2f(gemX + APERTURE_CIRCLE.cos(index) * core, gemY + APERTURE_CIRCLE.sin(index) * core);
            }
            GL11.glEnd();
            HoloShapes.radialGlow(gemX, gemY, core, radius, Color.BLACK, 0.85f * alphaMult);
        }
        float haloSize = HALO_SIZE * open * unit;
        SpriteDraw.drawAdditiveAtCenter(sprites, HALO_TEXTURE, gemX, gemY, haloSize, haloSize * SQUASH, lighten(color, 0.35f), 0.9f * alphaMult);
    }

    private static void drawInfallRings(SpriteCache sprites, Color color, float gemX, float gemY, float unit, float reach, float cycle, float t,
                                        float alphaMult) {
        Color tint = null;
        for (int ring = 0; ring < INFALL_RINGS; ring++) {
            float progress = infallProgress(cycle, ring);
            if (progress <= 0f || progress >= 1f) {
                continue;
            }
            if (tint == null) {
                tint = lighten(color, 0.4f);
            }
            float radius = ((reach - RING_MIN_RADIUS) * (float) Math.pow(1f - progress, 1.4) + RING_MIN_RADIUS) * unit;
            float alpha = (float) Math.sin(progress * Math.PI) * 0.85f * alphaMult;
            float angle = (float) Math.toDegrees((t * RING_SPIN + ring * 2f) % (Math.PI * 2.0));
            SpriteDraw.drawAdditiveAtCenter(sprites, RING_TEXTURE, gemX, gemY, radius * 2f, radius * 2f * SQUASH, tint, alpha, angle);
        }
    }

    static float lockStartDistance(float reach) {
        return Math.min(LOCK_START_DISTANCE, reach - LOCK_ARM * 1.5f);
    }

    private static void drawLockOn(Color color, float gemX, float gemY, float unit, float startDistance, float cycle, float alphaMult) {
        float alpha = lockAlpha(cycle) * alphaMult;
        if (alpha <= 0f) {
            return;
        }
        float travel = lockTravel(cycle);
        float distance = startDistance - (startDistance - LOCK_END_DISTANCE) * travel;
        float arm = LOCK_ARM - travel;
        try (GlScope scope = GlScope.flat().additive().lineWidth(Math.max(1f, 1.4f * unit))) {
            Misc.setColor(lighten(color, 0.5f), alpha);
            GL11.glBegin(GL11.GL_LINES);
            for (int quarter = 0; quarter < 4; quarter++) {
                double angle = Math.PI / 4.0 + quarter * Math.PI / 2.0 + (1f - travel) * LOCK_SWING;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float tipX = -cos * distance;
                float tipY = -sin * distance;
                chevronArm(gemX, gemY, unit, tipX, tipY, cos, sin, -arm, -arm);
                chevronArm(gemX, gemY, unit, tipX, tipY, cos, sin, -arm, arm);
            }
            GL11.glEnd();
        }
    }

    private static void chevronArm(float gemX, float gemY, float unit, float tipX, float tipY, float cos, float sin, float armX, float armY) {
        GL11.glVertex2f(gemX + tipX * unit, gemY - tipY * unit);
        float endX = tipX + armX * cos - armY * sin;
        float endY = tipY + armX * sin + armY * cos;
        GL11.glVertex2f(gemX + endX * unit, gemY - endY * unit);
    }

    private static void drawFrame(Color color, float cx, float cy, float unit, float alphaMult) {
        try (GlScope scope = GlScope.flat().additive()) {
            Misc.setColor(color, 0.16f * alphaMult);
            frameBand(cx, cy, FRAME_GLOW_INNER * unit, FRAME_OUTER * unit);
            scope.normalBlend();
            Misc.setColor(color, 0.85f * alphaMult);
            frameBand(cx, cy, FRAME_LINE_INNER * unit, FRAME_OUTER * unit);
        }
    }

    private static void frameBand(float cx, float cy, float inner, float outer) {
        GL11.glBegin(GL11.GL_QUADS);
        quad(cx - outer, cy + inner, cx + outer, cy + outer);
        quad(cx - outer, cy - outer, cx + outer, cy - inner);
        quad(cx - outer, cy - inner, cx - inner, cy + inner);
        quad(cx + inner, cy - inner, cx + outer, cy + inner);
        GL11.glEnd();
    }

    private static void quad(float fromX, float fromY, float toX, float toY) {
        GL11.glVertex2f(fromX, fromY);
        GL11.glVertex2f(toX, fromY);
        GL11.glVertex2f(toX, toY);
        GL11.glVertex2f(fromX, toY);
    }

    private static void drawLights(SpriteCache sprites, Color color, float cx, float cy, float unit, float wave, float alphaMult) {
        SpriteAPI texture = sprites.texture(LIGHT_TEXTURE);
        if (texture == null) {
            return;
        }
        Color tint = lighten(color, 0.55f);
        try (TexturedQuadBatch batch = LIGHT_BATCH.beginAdditive(texture)) {
            for (int path = 0; path < 2; path++) {
                for (int light = 0; light < LIGHTS_PER_SIDE; light++) {
                    float distance = LIGHT_SPACING / 2f + light * LIGHT_SPACING;
                    float level = lightLevel(distance, wave);
                    float[] point = framePoint(path, distance);
                    float lightSize = LIGHT_IDLE_SIZE + LIGHT_LIT_SIZE * level;
                    float limit = DESIGN_CENTRE - lightSize / 2f;
                    float lightX = Math.max(-limit, Math.min(limit, point[0]));
                    float lightY = Math.max(-limit, Math.min(limit, point[1]));
                    batch.quadAtCenter(cx + lightX * unit, cy - lightY * unit, lightSize * unit, lightSize * unit, 0f, tint,
                            (0.25f + 0.75f * level) * alphaMult);
                }
            }
        }
    }

    private static void drawBadge(SpriteCache sprites, String crestPath, Color color, float badgeX, float badgeY, float unit, boolean framed,
                                  float flash, float alphaMult) {
        SpriteAPI crest = crestPath == null ? null : sprites.texture(crestPath);
        if (crest == null) {
            return;
        }
        float radius = BADGE_RADIUS * unit;
        float textureLeft = crest.getTexX();
        float textureBottom = crest.getTexY();
        float textureWidth = crest.getTexWidth();
        float textureHeight = crest.getTexHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            crest.bindTexture();
            GL11.glColor4f(1f, 1f, 1f, alphaMult);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glTexCoord2f(textureLeft + textureWidth / 2f, textureBottom + textureHeight / 2f);
            GL11.glVertex2f(badgeX, badgeY);
            for (int segment = 0; segment <= BADGE_SEGMENTS; segment++) {
                int index = segment % BADGE_SEGMENTS;
                float cos = BADGE_CIRCLE.cos(index);
                float sin = BADGE_CIRCLE.sin(index);
                GL11.glTexCoord2f(textureLeft + textureWidth * (0.5f + 0.5f * cos), textureBottom + textureHeight * (0.5f + 0.5f * sin));
                GL11.glVertex2f(badgeX + cos * radius, badgeY + sin * radius);
            }
            GL11.glEnd();
        }
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.circle(badgeX, badgeY, radius, BADGE_RIM, Math.max(1f, 2f * unit), 0.95f * alphaMult);
            if (framed) {
                HoloShapes.circle(badgeX, badgeY, radius + 1.2f * unit, color, Math.max(1f, unit), 0.9f * alphaMult);
            }
        }
        if (flash > 0.01f) {
            float flashSize = FLASH_SIZE * unit;
            SpriteDraw.drawAdditiveAtCenter(sprites, LIGHT_TEXTURE, badgeX, badgeY, flashSize, flashSize, lighten(color, 0.55f), flash * alphaMult);
        }
    }

    static String crestPath(String factionId) {
        if (factionId == null) {
            return null;
        }
        String path = CREST_PATHS.computeIfAbsent(factionId, AnchorIconFx::lookUpCrest);
        return path.isEmpty() ? null : path;
    }

    private static String lookUpCrest(String factionId) {
        String factionCrest = factionCrest(factionId);
        return factionCrest != null ? factionCrest : bundledCrest(factionId);
    }

    private static String factionCrest(String factionId) {
        try {
            FactionSpecAPI spec = Global.getSettings().getFactionSpec(factionId);
            String crest = spec == null ? null : spec.getCrest();
            return crest == null || crest.isEmpty() ? null : crest;
        } catch (RuntimeException e) {
            LOG.warn("Could not read the crest of faction " + factionId + " for a wormhole anchor badge", e);
            return null;
        }
    }

    private static String bundledCrest(String factionId) {
        String path = BUNDLED_CREST_DIRECTORY + factionId + ".png";
        try {
            Global.getSettings().loadTexture(path);
            return path;
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }
}
