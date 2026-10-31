package exiledsector.ui.cargo;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public final class CatalystIconFx {

    static final String CORONA_TEXTURE = "graphics/fx/wormhole_corona.png";
    static final String STRAIN_TEXTURE = "graphics/fx/shields128ringc.png";
    static final String COLLAPSE_TEXTURE = "graphics/fx/explosion_ring0.png";
    static final String SCAR_TEXTURE = "graphics/fx/dust_clouds_colorless.png";
    static final String SWEEP_TEXTURE = "graphics/fx/scanline_large.png";
    static final String SCREEN_TEXTURE = "graphics/fx/shimmer.png";
    static final String STREAK_TEXTURE = "graphics/fx/contrail64b.png";
    static final String JUMP_TEXTURE = "graphics/fx/hit_glow.png";
    static final List<String> TEXTURES = List.of(CORONA_TEXTURE, STRAIN_TEXTURE, COLLAPSE_TEXTURE, SCAR_TEXTURE, SWEEP_TEXTURE, SCREEN_TEXTURE,
            STREAK_TEXTURE, JUMP_TEXTURE);
    static final float TERMINUS_SECONDS = 4.2f;
    static final float SURVEY_SECONDS = 3.6f;
    static final float ORDERS_SECONDS = 3f;
    static final float CALM_SLOWDOWN = 2f;
    private static final SpriteCache FX_SPRITES = new SpriteCache(CatalystIconFx.class);
    private static final float DESIGN_SIZE = 80f;
    private static final float HALF = DESIGN_SIZE / 2f;
    private static final float CORONA_SIZE = 88f;
    private static final float CORONA_ALPHA = 0.35f;
    private static final float CORONA_BREATHE = 0.08f;
    private static final float BRACKET_OPEN = 39f;
    private static final float BRACKET_HELD = 33f;
    private static final float BRACKET_ARM = 7f;
    private static final float BRACKET_CLOSE_END = 0.15f;
    private static final float BRACKET_FADE_START = 0.85f;
    private static final float CALM_BRACKET_ALPHA = 0.6f;
    private static final float STRAIN_START = 0.1f;
    private static final float STRAIN_PEAK = 0.45f;
    private static final float SNAP_END = 0.52f;
    private static final float STRAIN_MIN_RADIUS = 20f;
    private static final float STRAIN_MAX_RADIUS = 37f;
    private static final float STRAIN_SNAP_RADIUS = 6f;
    private static final float STRAIN_WOBBLE_HZ = 9f;
    private static final float STRAIN_WOBBLE = 0.08f;
    private static final float CALM_STRAIN_ALPHA = 0.35f;
    private static final float COLLAPSE_START = 0.5f;
    private static final float COLLAPSE_END = 0.62f;
    private static final float COLLAPSE_FROM = 64f;
    private static final float COLLAPSE_TO = 20f;
    private static final float SCAR_START = 0.52f;
    private static final float SCAR_END = 0.8f;
    private static final float SCAR_ALPHA = 0.6f;
    private static final float SCAR_SIZE = 46f;
    private static final int SCAR_CELLS = 4;
    private static final Color SCAR_COLOR = new Color(24, 16, 30);
    private static final float SURVEY_DIM = 0.35f;
    private static final float CALM_SURVEY_DIM = 0.6f;
    private static final float SWEEP_START = 0.05f;
    private static final float SWEEP_END = 0.55f;
    private static final float REVEAL_FADE_START = 0.75f;
    private static final float REVEAL_FADE_END = 0.95f;
    private static final float REVEAL_SOFTNESS = 0.12f;
    private static final int REVEAL_ROWS = 16;
    private static final float SWEEP_HEIGHT = 6f;
    private static final float CALM_SWEEP_ALPHA = 0.4f;
    private static final float SCREEN_ALPHA = 0.08f;
    private static final float[] STREAK_STARTS = {0.1f, 0.3f, 0.55f};
    private static final float STREAK_SPAN = 0.25f;
    private static final float STREAK_TRAVEL = 28f;
    private static final float STREAK_LENGTH = 24f;
    private static final float STREAK_WIDTH = 5f;
    private static final float STREAK_LANE_SPREAD = 14f;
    private static final float STREAK_ANGLE = 35f;
    private static final float STREAK_ALPHA = 0.85f;
    private static final float CALM_STREAK_ALPHA = 0.4f;
    private static final float JUMP_SIZE = 14f;
    private static final float JUMP_SHARE = 0.2f;

    private CatalystIconFx() {
    }

    public static void preload() {
        TEXTURES.forEach(FX_SPRITES::ensureLoaded);
    }

    static void render(CatalystLook catalyst, SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h,
                       float alphaMult, int phaseSeed) {
        ItemAnimationSetting.Mode mode = ItemAnimationSetting.mode();
        float size = Math.min(w, h);
        float unit = size / DESIGN_SIZE;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float iconSize = size * CargoIconFx.ICON_INSET;
        if (mode == ItemAnimationSetting.Mode.OFF) {
            CargoIconFx.drawIcon(sprites, iconPath, cx, cy, iconSize, alphaMult);
            return;
        }
        boolean full = mode == ItemAnimationSetting.Mode.FULL;
        AtlasCatalysts.Family family = catalyst.family() == null ? AtlasCatalysts.Family.TERMINUS : catalyst.family();
        float seconds = cycleSeconds(family) * (full ? 1f : CALM_SLOWDOWN);
        float t = CargoIconFx.seconds() + CargoIconFx.hash(phaseSeed, 1, 1) * seconds;
        float cycle = Fade.cycle(t, seconds);
        int round = (int) Math.floor(t / seconds);
        Color color = look.color();
        float coronaAlpha = CORONA_ALPHA + (full ? CORONA_BREATHE * (float) Math.sin(t * 1.3f) : 0f);
        SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, CORONA_TEXTURE, cx, cy, CORONA_SIZE * unit, CORONA_SIZE * unit, color, coronaAlpha * alphaMult);
        switch (family) {
            case TERMINUS -> {
                CargoIconFx.drawIcon(sprites, iconPath, cx, cy, iconSize, alphaMult);
                drawTerminus(color, cx, cy, unit, cycle, t, round, phaseSeed, full, alphaMult);
            }
            case SURVEY -> drawSurvey(sprites, iconPath, color, cx, cy, unit, iconSize, cycle, full, alphaMult);
            case ORDERS -> {
                CargoIconFx.drawIcon(sprites, iconPath, cx, cy, iconSize, alphaMult);
                drawOrders(color, cx, cy, unit, cycle, round, phaseSeed, full, alphaMult);
            }
        }
        drawBrackets(color, cx, cy, unit, full ? bracketDistance(cycle) : BRACKET_HELD, (full ? bracketAlpha(cycle) : CALM_BRACKET_ALPHA) * alphaMult);
    }

    static float cycleSeconds(AtlasCatalysts.Family family) {
        return switch (family) {
            case TERMINUS -> TERMINUS_SECONDS;
            case SURVEY -> SURVEY_SECONDS;
            case ORDERS -> ORDERS_SECONDS;
        };
    }

    static float bracketDistance(float cycle) {
        return BRACKET_OPEN - (BRACKET_OPEN - BRACKET_HELD) * Fade.window(0f, BRACKET_CLOSE_END, cycle);
    }

    static float bracketAlpha(float cycle) {
        return Fade.window(0f, BRACKET_CLOSE_END / 2f, cycle) * (1f - Fade.window(BRACKET_FADE_START, 1f, cycle));
    }

    static float strainRadius(float cycle) {
        if (cycle <= STRAIN_PEAK) {
            return STRAIN_MIN_RADIUS + (STRAIN_MAX_RADIUS - STRAIN_MIN_RADIUS) * Fade.window(STRAIN_START, STRAIN_PEAK, cycle);
        }
        return STRAIN_MAX_RADIUS - (STRAIN_MAX_RADIUS - STRAIN_SNAP_RADIUS) * Fade.window(STRAIN_PEAK, SNAP_END, cycle);
    }

    static float strainAlpha(float cycle, boolean full) {
        if (!full) {
            return CALM_STRAIN_ALPHA * Fade.window(STRAIN_START, STRAIN_PEAK, cycle) * (1f - Fade.window(STRAIN_PEAK, 0.7f, cycle));
        }
        return 0.8f * Fade.window(STRAIN_START, STRAIN_PEAK, cycle) * (1f - Fade.window(SNAP_END - 0.02f, SNAP_END, cycle));
    }

    static float collapseProgress(float cycle) {
        return (cycle - COLLAPSE_START) / (COLLAPSE_END - COLLAPSE_START);
    }

    static float scarAlpha(float cycle) {
        if (cycle < SCAR_START || cycle > SCAR_END) {
            return 0f;
        }
        return SCAR_ALPHA * (1f - Fade.window(SCAR_START, SCAR_END, cycle));
    }

    static float sweepPosition(float cycle) {
        return (cycle - SWEEP_START) / (SWEEP_END - SWEEP_START);
    }

    static float revealLevel(float fromTop, float cycle) {
        float passed = Fade.window(fromTop - REVEAL_SOFTNESS, fromTop, sweepPosition(cycle));
        return passed * (1f - Fade.window(REVEAL_FADE_START, REVEAL_FADE_END, cycle));
    }

    static float streakProgress(float cycle, int streak) {
        return (cycle - STREAK_STARTS[streak]) / STREAK_SPAN;
    }

    private static void drawTerminus(Color color, float cx, float cy, float unit, float cycle, float t, int round, int phaseSeed, boolean full,
                                     float alphaMult) {
        float ringAlpha = strainAlpha(cycle, full) * alphaMult;
        if (ringAlpha > 0f) {
            float strain = Fade.window(STRAIN_START, STRAIN_PEAK, cycle);
            float wobble = full ? STRAIN_WOBBLE * strain * (float) Math.sin(t * STRAIN_WOBBLE_HZ * Math.PI * 2.0) : 0f;
            float diameter = strainRadius(full ? cycle : Math.min(cycle, STRAIN_PEAK)) * 2f * unit;
            SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, STRAIN_TEXTURE, cx, cy, diameter * (1f + wobble), diameter * (1f - wobble),
                    CargoIconFx.lighten(color, 0.3f), ringAlpha, t * 20f % 360f);
        }
        if (!full) {
            return;
        }
        float collapse = collapseProgress(cycle);
        if (collapse > 0f && collapse < 1f) {
            float collapseSize = (COLLAPSE_FROM - (COLLAPSE_FROM - COLLAPSE_TO) * collapse) * unit;
            SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, COLLAPSE_TEXTURE, cx, cy, collapseSize, collapseSize, CargoIconFx.lighten(color, 0.5f),
                    0.9f * (1f - collapse) * alphaMult);
        }
        float scar = scarAlpha(cycle) * alphaMult;
        if (scar > 0f) {
            int cell = (int) (CargoIconFx.hash(phaseSeed, round, 7) * SCAR_CELLS * SCAR_CELLS);
            drawCell(FX_SPRITES.texture(SCAR_TEXTURE), cell % SCAR_CELLS, cell / SCAR_CELLS, cx, cy, SCAR_SIZE * unit, SCAR_COLOR, scar);
        }
    }

    private static void drawCell(SpriteAPI texture, int column, int row, float cx, float cy, float cellSize, Color tint, float alpha) {
        if (texture == null) {
            return;
        }
        float cellWidth = texture.getTexWidth() / SCAR_CELLS;
        float cellHeight = texture.getTexHeight() / SCAR_CELLS;
        float left = texture.getTexX() + column * cellWidth;
        float bottom = texture.getTexY() + row * cellHeight;
        float half = cellSize / 2f;
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            texture.bindTexture();
            Misc.setColor(tint, alpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(left, bottom);
            GL11.glVertex2f(cx - half, cy - half);
            GL11.glTexCoord2f(left + cellWidth, bottom);
            GL11.glVertex2f(cx + half, cy - half);
            GL11.glTexCoord2f(left + cellWidth, bottom + cellHeight);
            GL11.glVertex2f(cx + half, cy + half);
            GL11.glTexCoord2f(left, bottom + cellHeight);
            GL11.glVertex2f(cx - half, cy + half);
            GL11.glEnd();
        }
    }

    private static void drawSurvey(SpriteCache sprites, String iconPath, Color color, float cx, float cy, float unit, float iconSize, float cycle,
                                   boolean full, float alphaMult) {
        SpriteAPI icon = iconPath == null ? null : sprites.texture(iconPath);
        float dim = full ? SURVEY_DIM : CALM_SURVEY_DIM;
        if (icon != null) {
            drawRevealedIcon(icon, cx, cy, iconSize, dim, cycle, alphaMult);
        }
        if (full) {
            SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, SCREEN_TEXTURE, cx, cy, iconSize, iconSize, color, SCREEN_ALPHA * alphaMult);
        }
        float sweep = sweepPosition(cycle);
        if (sweep > 0f && sweep < 1f) {
            float sweepY = cy + iconSize / 2f - sweep * iconSize;
            float sweepAlpha = (full ? 0.9f : CALM_SWEEP_ALPHA) * (float) Math.sin(sweep * Math.PI);
            SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, SWEEP_TEXTURE, cx, sweepY, iconSize, SWEEP_HEIGHT * unit, CargoIconFx.lighten(color, 0.4f),
                    sweepAlpha * alphaMult);
        }
    }

    private static void drawRevealedIcon(SpriteAPI icon, float cx, float cy, float iconSize, float dim, float cycle, float alphaMult) {
        float half = iconSize / 2f;
        float left = icon.getTexX();
        float right = left + icon.getTexWidth();
        float bottom = icon.getTexY();
        float top = bottom + icon.getTexHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            icon.bindTexture();
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int row = 0; row <= REVEAL_ROWS; row++) {
                float fromTop = (float) row / REVEAL_ROWS;
                float alpha = (dim + (1f - dim) * revealLevel(fromTop, cycle)) * alphaMult;
                GL11.glColor4f(1f, 1f, 1f, alpha);
                float rowY = cy + half - fromTop * iconSize;
                float textureY = top - fromTop * (top - bottom);
                GL11.glTexCoord2f(left, textureY);
                GL11.glVertex2f(cx - half, rowY);
                GL11.glTexCoord2f(right, textureY);
                GL11.glVertex2f(cx + half, rowY);
            }
            GL11.glEnd();
        }
    }

    private static void drawOrders(Color color, float cx, float cy, float unit, float cycle, int round, int phaseSeed, boolean full,
                                   float alphaMult) {
        double angle = Math.toRadians(STREAK_ANGLE);
        float alongX = (float) Math.cos(angle);
        float alongY = (float) Math.sin(angle);
        Color tint = CargoIconFx.lighten(color, 0.3f);
        int streaks = full ? STREAK_STARTS.length : 1;
        for (int streak = 0; streak < streaks; streak++) {
            float progress = streakProgress(cycle, streak);
            if (progress <= 0f || progress >= 1f) {
                continue;
            }
            float lane = (CargoIconFx.hash(phaseSeed, round, streak) * 2f - 1f) * STREAK_LANE_SPREAD;
            float along = -STREAK_TRAVEL + 2f * STREAK_TRAVEL * progress;
            float streakX = cx + (alongX * along - alongY * lane) * unit;
            float streakY = cy + (alongY * along + alongX * lane) * unit;
            float alpha = (full ? STREAK_ALPHA : CALM_STREAK_ALPHA) * (float) Math.sin(progress * Math.PI) * alphaMult;
            SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, STREAK_TEXTURE, streakX, streakY, STREAK_LENGTH * unit, STREAK_WIDTH * unit, tint, alpha,
                    STREAK_ANGLE);
            if (full && progress < JUMP_SHARE) {
                float entryX = cx + (-alongX * STREAK_TRAVEL - alongY * lane) * unit;
                float entryY = cy + (-alongY * STREAK_TRAVEL + alongX * lane) * unit;
                float flash = 1f - progress / JUMP_SHARE;
                SpriteDraw.drawAdditiveAtCenter(FX_SPRITES, JUMP_TEXTURE, entryX, entryY, JUMP_SIZE * unit, JUMP_SIZE * unit, tint, flash * alphaMult);
            }
        }
    }

    private static void drawBrackets(Color color, float cx, float cy, float unit, float distance, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float reach = Math.min(distance, HALF - 1f) * unit;
        float arm = BRACKET_ARM * unit;
        try (GlScope scope = GlScope.flat().additive().lineWidth(Math.max(1f, 1.4f * unit))) {
            Misc.setColor(CargoIconFx.lighten(color, 0.35f), alpha);
            GL11.glBegin(GL11.GL_LINES);
            for (int corner = 0; corner < 4; corner++) {
                float signX = corner % 2 == 0 ? -1f : 1f;
                float signY = corner < 2 ? -1f : 1f;
                float cornerX = cx + signX * reach;
                float cornerY = cy + signY * reach;
                GL11.glVertex2f(cornerX, cornerY);
                GL11.glVertex2f(cornerX - signX * arm, cornerY);
                GL11.glVertex2f(cornerX, cornerY);
                GL11.glVertex2f(cornerX, cornerY - signY * arm);
            }
            GL11.glEnd();
        }
    }
}
