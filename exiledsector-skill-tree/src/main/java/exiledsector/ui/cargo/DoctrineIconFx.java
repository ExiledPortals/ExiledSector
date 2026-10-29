package exiledsector.ui.cargo;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class DoctrineIconFx {

    public static final String STILL_PREFIX = "graphics/icons/socketables/exiledSector_doctrine_archive_";
    static final String FRAME_PATH = STILL_PREFIX + "frame.png";
    public static final String EMBLEM_PREFIX = "graphics/icons/socketables/exiledSector_doctrine_emblem_";
    static final float CYCLE_SECONDS = 6f;
    static final float IDLE_WAVE = -10f;
    static final float SEAL_KEY = 0.55f;
    static final float STAMP_DROP = 0.04f;
    static final float STAMP_FADE = 0.06f;
    static final float STAMP_SCALE = 1.7f;
    static final float PASS_START = 0.1f;
    static final float PASS_STEP = 0.2f;
    static final float PASS_DURATION = 0.18f;
    static final float DECRYPT_KEY = 0.7f;
    static final float DECRYPT_RESET = 0.94f;
    static final float WRITE_START = 0.02f;
    static final float WRITE_END = 0.78f;
    static final float MANUSCRIPT_KEY = 0.8f;
    static final float PAGE_CLEAR_START = 0.9f;
    static final float PAGE_CLEAR_END = 0.99f;
    static final int BEATS = 8;
    static final float BEAT_SECONDS = CYCLE_SECONDS / BEATS;
    static final float DRILL_SNAP = 0.15f;
    static final float DRILL_KEY = (BEATS - 1f) / BEATS;
    static final float DROPOUT_START = 0.62f;
    static final float GLITCH_KEY = 0.68f;
    static final float EDIT_START = 0.12f;
    static final float EDIT_STEP = 0.25f;
    static final float STRIKE_SPAN = 0.06f;
    static final float ERASE_SPAN = 0.03f;
    static final float REWRITE_SPAN = 0.12f;
    static final int EDITS = 3;
    static final float COMMITTEE_KEY = EDIT_START + (EDITS - 1) * EDIT_STEP + STRIKE_SPAN + ERASE_SPAN + REWRITE_SPAN;
    static final float BLACKOUT_START = 0.47f;
    static final float REDACTED_KEY = 0.5f;
    static final float FLASH_WIDTH = 0.035f;
    static final int ROWS = 9;
    static final float ROW_TOP = 0.2f;
    static final float ROW_BOTTOM = 0.94f;
    static final float ROW_PITCH = 0.085f;
    private static final float DESIGN_SIZE = 80f;
    private static final float DESIGN_CENTRE = 40f;
    private static final float[][] SCREEN_CORNERS = {{13.5f, 17.5f}, {63.8f, 17.5f}, {70.3f, 55.8f}, {19f, 59.6f}};
    private static final float SCREEN_WIDTH = (SCREEN_CORNERS[1][0] + SCREEN_CORNERS[2][0] - SCREEN_CORNERS[0][0] - SCREEN_CORNERS[3][0]) / 2f;
    private static final float SCREEN_HEIGHT = (SCREEN_CORNERS[3][1] + SCREEN_CORNERS[2][1] - SCREEN_CORNERS[0][1] - SCREEN_CORNERS[1][1]) / 2f;
    private static final float[][] SIDE_LIGHTS = {{8.5f, 22f}, {8.5f, 27f}, {8.5f, 32f}};
    private static final float SIDE_LIGHT_WIDTH = 4f;
    private static final float SIDE_LIGHT_HEIGHT = 1.8f;
    private static final float[] SIDE_LIGHT_RATES = {0.9f, 1.7f, 0.55f};
    private static final float FRAME_TINT = 0.4f;
    private static final float FILL_ALPHA = 0.14f;
    private static final float FLASH_FILL_ALPHA = 0.22f;
    private static final float TEXT_LEFT = 0.05f;
    private static final float TEXT_RIGHT = 0.95f;
    private static final float ROW_THICKNESS = 0.032f;
    private static final float ROW_FADE = 0.04f;
    private static final float WORD_GAP = 0.025f;
    private static final int MAX_WORDS = 12;
    private static final float TEXT_ALPHA = 0.5f;
    private static final float TITLE_TOP = 0.075f;
    private static final float TITLE_BOTTOM = 0.125f;
    private static final float TITLE_END = 0.5f;
    private static final float STATUS_LEFT = 0.88f;
    private static final float STATUS_RIGHT = 0.93f;
    private static final float STATUS_TOP = 0.07f;
    private static final float STATUS_BOTTOM = 0.13f;
    private static final float EMBLEM_U = 0.5f;
    private static final float EMBLEM_V = 0.56f;
    private static final float EMBLEM_HEIGHT = 0.6f;
    private static final float EMBLEM_SHADE = 0.7f;
    private static final float EMBLEM_LIFT = 0.45f;
    private static final float SCAN_HALF = 0.07f;
    private static final float SCAN_ALPHA = 0.35f;
    private static final float SCROLL_ROWS_PER_SECOND = 0.6f;
    private static final float SCAN_SECONDS = 2.6f;
    private static final float SCRAMBLE_SECONDS = 0.08f;
    private static final float BLIP_PERIOD = 0.9f;
    private static final float BLIP_LIFE = 0.15f;
    private static final int BLIPS = 3;
    private static final int[] PARAGRAPH_ROWS = {0, 4, 7};
    private static final float DROP_CAP_WIDTH = 0.06f;
    private static final float INDENT = 0.14f;
    private static final float WOBBLE = 0.012f;
    private static final float HALO_WIDTH = 0.06f;
    private static final float DRILL_BLOCK = 0.065f;
    private static final float DRILL_STEP = 0.09f;
    private static final float GLITCH_STEPS_PER_SECOND = 8f;
    private static final float GLITCH_STEP_ROWS = 0.12f;
    private static final float JITTER_SECONDS = 0.07f;
    private static final float JITTER_CHANCE = 0.22f;
    private static final float JITTER_REACH = 0.14f;
    private static final float TEAR_PERIOD = 1.3f;
    private static final float TEAR_LIFE = 0.12f;
    private static final float TEAR_HALF = 0.1f;
    private static final float TEAR_SHIFT = 0.18f;
    private static final float PATCH_CHANCE = 0.35f;
    private static final float PATCH_MIX = 0.45f;
    private static final Color[] PATCH_TINTS = {Color.WHITE, new Color(255, 190, 60), new Color(150, 255, 170)};
    private static final int NOISE_SPECKS = 24;
    private static final float NOISE_SECONDS = 0.04f;
    private static final float COLUMN_SPLIT = 0.5f;
    private static final float COLUMN_GAP = 0.04f;
    private static final Color STRIKE_COLOR = new Color(255, 120, 100);
    private static final float REDACT_THICKNESS = 0.055f;
    private static final float REDACT_ALPHA = 0.32f;
    private static final float FLICKER_SECONDS = 0.06f;
    private static final float FLICKER_CHANCE = 0.05f;
    private static final float[][] FILL_RIM = {{0f, 0f}, {0.5f, 0f}, {1f, 0f}, {1f, 0.5f}, {1f, 1f}, {0.5f, 1f}, {0f, 1f}, {0f, 0.5f}, {0f, 0f}};
    private static final float[] SCREEN = new float[8];

    private DoctrineIconFx() {
    }

    static void render(DoctrineLook doctrine, SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h,
                       float alphaMult, int phaseSeed) {
        DoctrineScreen screen = doctrine.screen();
        boolean animated = ItemAnimationSetting.mode() == ItemAnimationSetting.Mode.FULL;
        float size = Math.min(w, h);
        float unit = size / DESIGN_SIZE;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        boolean framed = look.framed();
        float scale = (framed ? CargoIconFx.ICON_INSET : 1f) * unit;
        Color color = look.color();
        if (!animated) {
            SpriteAPI still = iconPath == null ? null : sprites.sprite(iconPath);
            if (still != null) {
                SpriteDraw.drawAtCenter(still, cx, cy, DESIGN_SIZE * scale, DESIGN_SIZE * scale, null, alphaMult, 0f);
            }
            if (framed) {
                AnchorIconFx.drawFrame(color, cx, cy, unit, alphaMult);
                AnchorIconFx.drawLights(sprites, color, cx, cy, unit, IDLE_WAVE, alphaMult);
            }
            return;
        }
        float t = CargoIconFx.seconds() + CargoIconFx.hash(phaseSeed, 1, 1) * CYCLE_SECONDS;
        float cycle = cycleOf(t);
        float key = keyMoment(screen);
        float flash = flash(cycle, key);
        int seed = doctrine.factionId().hashCode();
        SpriteAPI frame = sprites.sprite(FRAME_PATH);
        if (frame != null) {
            SpriteDraw.drawAtCenter(frame, cx, cy, DESIGN_SIZE * scale, DESIGN_SIZE * scale, mix(Color.WHITE, color, FRAME_TINT), alphaMult, 0f);
        }
        placeScreen(cx, cy, scale);
        try (GlScope scope = GlScope.flat().additive()) {
            drawFill(color, (FILL_ALPHA + FLASH_FILL_ALPHA * flash) * alphaMult);
            drawSideLights(color, cx, cy, scale, t, alphaMult);
            if (!(screen == DoctrineScreen.GLITCH && dropped(cycle)) && !(screen == DoctrineScreen.REDACTED && blackedOut(cycle))) {
                drawTitle(screen, color, seed, t, cycle, alphaMult);
                drawRows(screen, color, seed, t, cycle, alphaMult);
            }
        }
        drawEmblem(sprites, doctrine, screen, color, seed, t, cycle, alphaMult);
        try (GlScope scope = GlScope.flat().additive()) {
            drawOverlay(screen, color, seed, t, cycle, alphaMult);
            if (flash > 0.01f) {
                Misc.setColor(color, FLASH_FILL_ALPHA * flash * alphaMult);
                GL11.glBegin(GL11.GL_QUADS);
                screenQuad(0f, 0f, 1f, 1f);
                GL11.glEnd();
            }
        }
        if (framed) {
            AnchorIconFx.drawFrame(flash > 0.01f ? AnchorIconFx.lighten(color, 0.6f * flash) : color, cx, cy, unit, alphaMult);
            AnchorIconFx.drawLights(sprites, color, cx, cy, unit, wave(cycle, key), alphaMult);
        }
    }

    static float cycleOf(float seconds) {
        float cycle = seconds % CYCLE_SECONDS / CYCLE_SECONDS;
        return cycle < 0f ? cycle + 1f : cycle;
    }

    static float keyMoment(DoctrineScreen screen) {
        return switch (screen) {
            case DECRYPT -> DECRYPT_KEY;
            case MANUSCRIPT -> MANUSCRIPT_KEY;
            case DRILL -> DRILL_KEY;
            case GLITCH -> GLITCH_KEY;
            case COMMITTEE -> COMMITTEE_KEY;
            case REDACTED -> REDACTED_KEY;
            default -> SEAL_KEY;
        };
    }

    static float cyclicDistance(float first, float second) {
        float distance = Math.abs(first - second) % 1f;
        return Math.min(distance, 1f - distance);
    }

    static float flash(float cycle, float key) {
        float offset = cyclicDistance(cycle, key) / FLASH_WIDTH;
        return (float) Math.exp(-offset * offset);
    }

    static float wave(float cycle, float key) {
        float lead = cycle - key;
        lead -= (float) Math.floor(lead + 0.8f);
        return 2f + lead * 2.8f;
    }

    static float stampScale(float cycle) {
        float start = SEAL_KEY - STAMP_DROP;
        if (cycle < start || cycle >= SEAL_KEY) {
            return 1f;
        }
        float progress = (cycle - start) / STAMP_DROP;
        return 1f + (STAMP_SCALE - 1f) * (1f - progress * progress);
    }

    static float stampAlpha(float cycle) {
        float drop = SEAL_KEY - STAMP_DROP;
        float fade = drop - STAMP_FADE;
        if (cycle >= fade && cycle < drop) {
            return 1f - (cycle - fade) / STAMP_FADE;
        }
        if (cycle >= drop && cycle < SEAL_KEY) {
            return (cycle - drop) / STAMP_DROP;
        }
        return 1f;
    }

    static int decryptLevel(float cycle, float rowDepth) {
        if (cycle >= DECRYPT_RESET) {
            return 0;
        }
        int level = 0;
        for (int pass = 0; pass < 3; pass++) {
            if (cycle >= PASS_START + pass * PASS_STEP + rowDepth * PASS_DURATION) {
                level++;
            }
        }
        return level;
    }

    static int rowsNeeded(int seed, int row) {
        return 1 + Math.min(2, (int) (CargoIconFx.hash(seed, row, 61) * 3f));
    }

    static float writeProgress(float cycle) {
        return Math.max(0f, Math.min(1f, (cycle - WRITE_START) / (WRITE_END - WRITE_START))) * ROWS;
    }

    static float pageAlpha(float cycle) {
        return 1f - AnchorIconFx.smooth(PAGE_CLEAR_START, PAGE_CLEAR_END, cycle);
    }

    static float drillOffset(float seconds) {
        float beats = seconds / BEAT_SECONDS;
        float beat = (float) Math.floor(beats);
        return beat + AnchorIconFx.smooth(0f, DRILL_SNAP, beats - beat);
    }

    static boolean dropped(float cycle) {
        return cycle >= DROPOUT_START && cycle < GLITCH_KEY;
    }

    static float editStart(int edit) {
        return EDIT_START + edit * EDIT_STEP;
    }

    static float committeeAlpha(float cycle) {
        return AnchorIconFx.smooth(0f, 0.04f, cycle) * (1f - AnchorIconFx.smooth(0.95f, 1f, cycle));
    }

    static boolean blackedOut(float cycle) {
        return cycle >= BLACKOUT_START && cycle < REDACTED_KEY;
    }

    private static void placeScreen(float cx, float cy, float scale) {
        for (int corner = 0; corner < 4; corner++) {
            SCREEN[corner * 2] = cx + (SCREEN_CORNERS[corner][0] - DESIGN_CENTRE) * scale;
            SCREEN[corner * 2 + 1] = cy + (DESIGN_CENTRE - SCREEN_CORNERS[corner][1]) * scale;
        }
    }

    private static float screenX(float u, float v) {
        float top = SCREEN[0] + (SCREEN[2] - SCREEN[0]) * u;
        float bottom = SCREEN[6] + (SCREEN[4] - SCREEN[6]) * u;
        return top + (bottom - top) * v;
    }

    private static float screenY(float u, float v) {
        float top = SCREEN[1] + (SCREEN[3] - SCREEN[1]) * u;
        float bottom = SCREEN[7] + (SCREEN[5] - SCREEN[7]) * u;
        return top + (bottom - top) * v;
    }

    private static void vertex(float u, float v) {
        GL11.glVertex2f(screenX(u, v), screenY(u, v));
    }

    private static void screenQuad(float fromU, float fromV, float toU, float toV) {
        vertex(fromU, fromV);
        vertex(toU, fromV);
        vertex(toU, toV);
        vertex(fromU, toV);
    }

    private static void drawFill(Color color, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        Misc.setColor(color, alpha);
        vertex(0.5f, 0.5f);
        Misc.setColor(color, 0f);
        for (float[] point : FILL_RIM) {
            vertex(point[0], point[1]);
        }
        GL11.glEnd();
    }

    private static void drawSideLights(Color color, float cx, float cy, float scale, float t, float alphaMult) {
        GL11.glBegin(GL11.GL_QUADS);
        for (int light = 0; light < SIDE_LIGHTS.length; light++) {
            float phase = (t * SIDE_LIGHT_RATES[light] + light * 0.37f) % 1f;
            float level = phase < 0.5f ? 1f : 0.25f;
            Misc.setColor(color, level * alphaMult);
            float lightX = cx + (SIDE_LIGHTS[light][0] - DESIGN_CENTRE) * scale;
            float lightY = cy + (DESIGN_CENTRE - SIDE_LIGHTS[light][1]) * scale;
            float halfWidth = SIDE_LIGHT_WIDTH / 2f * scale;
            float halfHeight = SIDE_LIGHT_HEIGHT / 2f * scale;
            GL11.glVertex2f(lightX - halfWidth, lightY - halfHeight);
            GL11.glVertex2f(lightX + halfWidth, lightY - halfHeight);
            GL11.glVertex2f(lightX + halfWidth, lightY + halfHeight);
            GL11.glVertex2f(lightX - halfWidth, lightY + halfHeight);
        }
        GL11.glEnd();
    }

    private static void drawTitle(DoctrineScreen screen, Color color, int seed, float t, float cycle, float alphaMult) {
        GL11.glBegin(GL11.GL_QUADS);
        Misc.setColor(color, 0.85f * alphaMult);
        if (screen == DoctrineScreen.REDACTED) {
            screenQuad(TEXT_LEFT, TITLE_TOP, TITLE_END + 0.12f, TITLE_BOTTOM);
        } else {
            float glyph = TEXT_LEFT;
            int index = 0;
            while (glyph < TITLE_END) {
                float glyphWidth = 0.03f + 0.05f * CargoIconFx.hash(seed, 900, index++);
                screenQuad(glyph, TITLE_TOP, Math.min(TITLE_END, glyph + glyphWidth), TITLE_BOTTOM);
                glyph += glyphWidth + 0.02f;
            }
        }
        boolean lit = screen == DoctrineScreen.DRILL ? (cycle * BEATS) % 1f < 0.35f : screen == DoctrineScreen.REDACTED || t % 1f < 0.5f;
        if (lit) {
            screenQuad(STATUS_LEFT, STATUS_TOP, STATUS_RIGHT, STATUS_BOTTOM);
        }
        GL11.glEnd();
    }

    private static float rowCentre(int slot, float fraction) {
        return ROW_TOP + (slot + 0.5f - fraction) * ROW_PITCH;
    }

    private static float rowFade(float v) {
        return AnchorIconFx.smooth(ROW_TOP, ROW_TOP + ROW_FADE, v) * (1f - AnchorIconFx.smooth(ROW_BOTTOM - ROW_FADE, ROW_BOTTOM, v));
    }

    private static float rowDepth(float v) {
        return (v - ROW_TOP) / (ROW_BOTTOM - ROW_TOP);
    }

    private static float rowEnd(int seed, int row, float fromU, float toU) {
        return fromU + (toU - fromU) * (0.55f + 0.45f * CargoIconFx.hash(seed, row, 97));
    }

    private static void words(int seed, int row, float v, float fromU, float toU, float revealU, float shiftU, float thickness, float wobble) {
        float end = Math.min(rowEnd(seed, row, fromU, toU), revealU);
        float wordU = fromU;
        for (int word = 0; word < MAX_WORDS && wordU < end; word++) {
            float wordWidth = 0.04f + 0.12f * CargoIconFx.hash(seed, row, word);
            float from = Math.max(0.02f, wordU + shiftU);
            float to = Math.min(0.98f, Math.min(wordU + wordWidth, end) + shiftU);
            float wordV = v + (wobble == 0f ? 0f : (CargoIconFx.hash(seed, row, word + 50) - 0.5f) * wobble);
            if (to > from) {
                screenQuad(from, wordV - thickness / 2f, to, wordV + thickness / 2f);
            }
            wordU += wordWidth + WORD_GAP;
        }
    }

    private static void drawRows(DoctrineScreen screen, Color color, int seed, float t, float cycle, float alphaMult) {
        GL11.glBegin(GL11.GL_QUADS);
        switch (screen) {
            case DECRYPT -> drawDecryptRows(color, seed, t, cycle, alphaMult);
            case MANUSCRIPT -> drawManuscriptRows(color, seed, t, cycle, alphaMult);
            case DRILL -> drawDrillRows(color, seed, drillOffset(t), alphaMult);
            case GLITCH -> drawGlitchRows(color, seed, t, alphaMult);
            case COMMITTEE -> drawCommitteeRows(color, seed, t, cycle, alphaMult);
            case REDACTED -> drawRedactedRows(color, seed, t, cycle, alphaMult);
            default -> drawScrollingRows(color, seed, t * SCROLL_ROWS_PER_SECOND, alphaMult);
        }
        GL11.glEnd();
    }

    private static void drawScrollingRows(Color color, int seed, float offset, float alphaMult) {
        int first = (int) Math.floor(offset);
        float fraction = offset - first;
        for (int slot = 0; slot <= ROWS; slot++) {
            float v = rowCentre(slot, fraction);
            float fade = rowFade(v);
            if (fade <= 0f) {
                continue;
            }
            Misc.setColor(color, TEXT_ALPHA * fade * alphaMult);
            words(seed, first + slot, v, TEXT_LEFT, TEXT_RIGHT, TEXT_RIGHT, 0f, ROW_THICKNESS, 0f);
        }
    }

    private static void drawDecryptRows(Color color, int seed, float t, float cycle, float alphaMult) {
        int scramble = (int) (t / SCRAMBLE_SECONDS);
        for (int row = 0; row < ROWS; row++) {
            float v = rowCentre(row, 0f);
            boolean clear = decryptLevel(cycle, rowDepth(v)) >= rowsNeeded(seed, row);
            if (clear) {
                Misc.setColor(color, TEXT_ALPHA * alphaMult);
                words(seed, row, v, TEXT_LEFT, TEXT_RIGHT, TEXT_RIGHT, 0f, ROW_THICKNESS, 0f);
            } else {
                Misc.setColor(color, (0.2f + 0.3f * CargoIconFx.hash(seed, row + 200, scramble)) * alphaMult);
                words(seed, row * 31 + scramble * 977, v, TEXT_LEFT, TEXT_RIGHT, TEXT_RIGHT, 0f, ROW_THICKNESS, 0f);
            }
        }
        for (int blip = 0; blip < BLIPS; blip++) {
            float local = t + blip * BLIP_PERIOD / BLIPS;
            int round = (int) Math.floor(local / BLIP_PERIOD);
            float age = local - round * BLIP_PERIOD;
            if (age >= BLIP_LIFE) {
                continue;
            }
            float blipV = ROW_TOP + (ROW_BOTTOM - ROW_TOP - 0.03f) * CargoIconFx.hash(seed, 71 + blip, round);
            Misc.setColor(AnchorIconFx.lighten(color, 0.5f), (1f - age / BLIP_LIFE) * alphaMult);
            screenQuad(0.955f, blipV, 0.975f, blipV + 0.03f);
        }
    }

    private static void drawManuscriptRows(Color color, int seed, float t, float cycle, float alphaMult) {
        float progress = writeProgress(cycle);
        float page = pageAlpha(cycle);
        if (page <= 0f) {
            return;
        }
        for (int row = 0; row < ROWS && row < progress; row++) {
            float v = rowCentre(row, 0f);
            boolean opensParagraph = opensParagraph(row);
            boolean indented = opensParagraph || opensParagraph(row - 1);
            float fromU = indented ? INDENT : TEXT_LEFT;
            float written = Math.min(1f, progress - row);
            if (opensParagraph) {
                Misc.setColor(AnchorIconFx.lighten(color, 0.3f), 0.85f * page * alphaMult);
                screenQuad(TEXT_LEFT, v - ROW_THICKNESS, TEXT_LEFT + DROP_CAP_WIDTH, v + ROW_PITCH + ROW_THICKNESS / 2f);
            }
            float revealU = fromU + (TEXT_RIGHT - fromU) * written;
            Misc.setColor(color, TEXT_ALPHA * page * alphaMult);
            words(seed, row, v, fromU, TEXT_RIGHT, revealU, 0f, ROW_THICKNESS, WOBBLE);
            if (written < 1f && t * 4f % 1f < 0.6f) {
                Misc.setColor(AnchorIconFx.lighten(color, 0.6f), alphaMult);
                screenQuad(revealU, v - 0.025f, revealU + 0.015f, v + 0.025f);
            }
        }
    }

    private static boolean opensParagraph(int row) {
        for (int paragraphRow : PARAGRAPH_ROWS) {
            if (paragraphRow == row) {
                return true;
            }
        }
        return false;
    }

    private static void drawDrillRows(Color color, int seed, float offset, float alphaMult) {
        int first = (int) Math.floor(offset);
        float fraction = offset - first;
        for (int slot = 0; slot <= ROWS; slot++) {
            float v = rowCentre(slot, fraction);
            float fade = rowFade(v);
            if (fade <= 0f) {
                continue;
            }
            int row = first + slot;
            Misc.setColor(color, TEXT_ALPHA * fade * alphaMult);
            int blocks = 8 + (int) (CargoIconFx.hash(seed, row, 97) * 3f);
            for (int block = 0; block < blocks; block++) {
                float blockU = TEXT_LEFT + block * DRILL_STEP;
                if (blockU + DRILL_BLOCK > TEXT_RIGHT || CargoIconFx.hash(seed, row, block) > 0.8f) {
                    continue;
                }
                screenQuad(blockU, v - ROW_THICKNESS / 2f, blockU + DRILL_BLOCK, v + ROW_THICKNESS / 2f);
            }
        }
    }

    private static void drawGlitchRows(Color color, int seed, float t, float alphaMult) {
        float offset = (float) Math.floor(t * GLITCH_STEPS_PER_SECOND) * GLITCH_STEP_ROWS;
        int first = (int) Math.floor(offset);
        float fraction = offset - first;
        int jitterStep = (int) (t / JITTER_SECONDS);
        float tearV = tearCentre(seed, t);
        for (int slot = 0; slot <= ROWS; slot++) {
            float v = rowCentre(slot, fraction);
            float fade = rowFade(v);
            if (fade <= 0f) {
                continue;
            }
            int row = first + slot;
            float shiftU = 0f;
            if (CargoIconFx.hash(seed, row, jitterStep) < JITTER_CHANCE) {
                shiftU = (CargoIconFx.hash(seed, row + 500, jitterStep) - 0.5f) * JITTER_REACH;
            }
            if (Math.abs(v - tearV) < TEAR_HALF) {
                shiftU += TEAR_SHIFT;
            }
            float patch = CargoIconFx.hash(seed, row, 33);
            float alpha = TEXT_ALPHA * fade * alphaMult;
            if (patch < PATCH_CHANCE) {
                setMix(color, PATCH_TINTS[(int) (patch / PATCH_CHANCE * PATCH_TINTS.length) % PATCH_TINTS.length], PATCH_MIX, alpha);
            } else {
                Misc.setColor(color, alpha);
            }
            words(seed, row, v, TEXT_LEFT, TEXT_RIGHT, TEXT_RIGHT, shiftU, ROW_THICKNESS, 0f);
        }
    }

    private static float tearCentre(int seed, float t) {
        int tear = (int) Math.floor(t / TEAR_PERIOD);
        if (t - tear * TEAR_PERIOD >= TEAR_LIFE) {
            return -1f;
        }
        return 0.25f + 0.5f * CargoIconFx.hash(seed, 7, tear);
    }

    private static void drawCommitteeRows(Color color, int seed, float t, float cycle, float alphaMult) {
        float page = committeeAlpha(cycle);
        if (page <= 0f) {
            return;
        }
        int session = (int) Math.floor(t / CYCLE_SECONDS);
        Misc.setColor(color, 0.25f * page * alphaMult);
        screenQuad(COLUMN_SPLIT - 0.004f, ROW_TOP, COLUMN_SPLIT + 0.004f, ROW_BOTTOM);
        for (int column = 0; column < 2; column++) {
            float fromU = column == 0 ? TEXT_LEFT : COLUMN_SPLIT + COLUMN_GAP;
            float toU = column == 0 ? COLUMN_SPLIT - COLUMN_GAP : TEXT_RIGHT;
            for (int row = 0; row < ROWS; row++) {
                float v = rowCentre(row, 0f);
                int line = column * ROWS + row;
                int revision = line;
                float textAlpha = 1f;
                float revealU = toU;
                float strikeU = -1f;
                for (int edit = 0; edit < EDITS; edit++) {
                    if (editedLine(seed, session, edit) != line) {
                        continue;
                    }
                    float start = editStart(edit);
                    float erased = start + STRIKE_SPAN + ERASE_SPAN;
                    if (cycle >= start && cycle < erased) {
                        float strike = Math.min(1f, (cycle - start) / STRIKE_SPAN);
                        strikeU = fromU + (rowEnd(seed, revision, fromU, toU) - fromU) * strike;
                        textAlpha = cycle < start + STRIKE_SPAN ? 1f : 1f - (cycle - start - STRIKE_SPAN) / ERASE_SPAN;
                    } else if (cycle >= erased) {
                        revision = ROWS * 2 + session * EDITS + edit;
                        revealU = fromU + (toU - fromU) * Math.min(1f, (cycle - erased) / REWRITE_SPAN);
                    }
                }
                Misc.setColor(color, TEXT_ALPHA * textAlpha * page * alphaMult);
                words(seed, revision, v, fromU, toU, revealU, 0f, ROW_THICKNESS, 0f);
                if (strikeU > fromU) {
                    setMix(color, STRIKE_COLOR, 0.6f, 0.9f * textAlpha * page * alphaMult);
                    screenQuad(fromU, v - 0.009f, strikeU, v + 0.009f);
                }
            }
        }
    }

    static int editedLine(int seed, int session, int edit) {
        return (int) (CargoIconFx.hash(seed, 310 + edit, session) * ROWS * 2);
    }

    private static void drawRedactedRows(Color color, int seed, float t, float cycle, float alphaMult) {
        int flicker = (int) (t / FLICKER_SECONDS);
        float boost = flash(cycle, REDACTED_KEY);
        for (int row = 0; row < ROWS; row++) {
            if (CargoIconFx.hash(seed, row + 400, flicker) < FLICKER_CHANCE) {
                continue;
            }
            float v = rowCentre(row, 0f);
            float end = TEXT_LEFT + (TEXT_RIGHT - TEXT_LEFT) * (0.3f + 0.6f * CargoIconFx.hash(seed, row, 97));
            float split = CargoIconFx.hash(seed, row, 98) < 0.4f ? TEXT_LEFT + (end - TEXT_LEFT) * 0.45f : -1f;
            Misc.setColor(color, (REDACT_ALPHA + 0.6f * boost) * alphaMult);
            if (split > 0f) {
                screenQuad(TEXT_LEFT, v - REDACT_THICKNESS / 2f, split, v + REDACT_THICKNESS / 2f);
                screenQuad(split + WORD_GAP, v - REDACT_THICKNESS / 2f, end, v + REDACT_THICKNESS / 2f);
            } else {
                screenQuad(TEXT_LEFT, v - REDACT_THICKNESS / 2f, end, v + REDACT_THICKNESS / 2f);
            }
        }
    }

    private static void drawEmblem(SpriteCache sprites, DoctrineLook doctrine, DoctrineScreen screen, Color color, int seed, float t, float cycle,
                                   float alphaMult) {
        float emblemAlpha = 1f;
        float emblemScale = 1f;
        float shiftU = 0f;
        switch (screen) {
            case SEAL -> {
                emblemAlpha = stampAlpha(cycle);
                emblemScale = stampScale(cycle);
            }
            case DECRYPT -> {
                int cleared = 0;
                for (int row = 0; row < ROWS; row++) {
                    cleared += decryptLevel(cycle, rowDepth(rowCentre(row, 0f))) >= rowsNeeded(seed, row) ? 1 : 0;
                }
                emblemAlpha = 0.3f + 0.7f * cleared / ROWS;
                if (cleared < ROWS) {
                    shiftU = (CargoIconFx.hash(seed, 13, (int) (t / SCRAMBLE_SECONDS)) - 0.5f) * 0.02f;
                }
            }
            case MANUSCRIPT -> emblemAlpha = 0.85f;
            case DRILL -> emblemScale = 1f + 0.06f * (float) Math.exp(-(cycle * BEATS % 1f) * 10f);
            case GLITCH -> {
                if (dropped(cycle)) {
                    return;
                }
                shiftU = tearCentre(seed, t) >= 0f ? TEAR_SHIFT * 0.3f : 0f;
            }
            case REDACTED -> emblemAlpha = 0.6f + 0.3f * (float) Math.sin(cycle * HoloShapes.TWO_PI);
            default -> {
            }
        }
        if (emblemAlpha <= 0f) {
            return;
        }
        float halfV = EMBLEM_HEIGHT / 2f * emblemScale;
        float halfU = halfV * SCREEN_HEIGHT / SCREEN_WIDTH;
        float centreU = EMBLEM_U + shiftU;
        float centreX = screenX(centreU, EMBLEM_V);
        float centreY = screenY(centreU, EMBLEM_V);
        float radius = halfV * (Math.abs(SCREEN[1] - SCREEN[7]) + Math.abs(SCREEN[3] - SCREEN[5])) / 2f;
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.radialGlow(centreX, centreY, 0f, radius * 1.4f, Color.BLACK, EMBLEM_SHADE * emblemAlpha * alphaMult);
        }
        if (screen == DoctrineScreen.MANUSCRIPT) {
            float halo = (float) Math.exp(-Math.pow(cyclicDistance(cycle, MANUSCRIPT_KEY) / HALO_WIDTH, 2));
            if (halo > 0.01f) {
                SpriteDraw.drawAdditiveAtCenter(sprites, AnchorIconFx.LIGHT_TEXTURE, centreX, centreY, radius * 4f, radius * 4f,
                        AnchorIconFx.lighten(color, 0.4f), halo * alphaMult);
            }
        }
        SpriteAPI emblem = sprites.texture(doctrine.emblemPath());
        if (emblem == null) {
            return;
        }
        float textureLeft = emblem.getTexX();
        float textureBottom = emblem.getTexY();
        float textureWidth = emblem.getTexWidth();
        float textureHeight = emblem.getTexHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            emblem.bindTexture();
            Misc.setColor(AnchorIconFx.lighten(color, EMBLEM_LIFT), emblemAlpha * alphaMult);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(textureLeft, textureBottom + textureHeight);
            vertex(centreU - halfU, EMBLEM_V - halfV);
            GL11.glTexCoord2f(textureLeft + textureWidth, textureBottom + textureHeight);
            vertex(centreU + halfU, EMBLEM_V - halfV);
            GL11.glTexCoord2f(textureLeft + textureWidth, textureBottom);
            vertex(centreU + halfU, EMBLEM_V + halfV);
            GL11.glTexCoord2f(textureLeft, textureBottom);
            vertex(centreU - halfU, EMBLEM_V + halfV);
            GL11.glEnd();
        }
    }

    private static void drawOverlay(DoctrineScreen screen, Color color, int seed, float t, float cycle, float alphaMult) {
        switch (screen) {
            case DECRYPT -> {
                for (int pass = 0; pass < 3; pass++) {
                    float sweep = (cycle - PASS_START - pass * PASS_STEP) / PASS_DURATION;
                    if (sweep >= 0f && sweep <= 1f) {
                        scanBand(color, ROW_TOP + sweep * (ROW_BOTTOM - ROW_TOP), SCAN_ALPHA * 1.4f * alphaMult);
                    }
                }
            }
            case DRILL -> {
                float beat = cycle * BEATS;
                float beatIndex = (float) Math.floor(beat);
                scanBand(color, ROW_TOP + (beatIndex + 0.5f) / BEATS * (ROW_BOTTOM - ROW_TOP), SCAN_ALPHA * (1f - (beat - beatIndex)) * alphaMult);
            }
            case GLITCH -> {
                if (dropped(cycle)) {
                    drawNoise(color, seed, t, alphaMult);
                } else {
                    float stutter = (float) Math.floor(t * 6f) / 6f * 0.45f % 1.2f - 0.1f;
                    scanBand(color, stutter, SCAN_ALPHA * alphaMult);
                }
            }
            case COMMITTEE -> scanBand(color, (t / (SCAN_SECONDS * 1.6f)) % 1f * 1.2f - 0.1f, SCAN_ALPHA * 0.7f * alphaMult);
            case MANUSCRIPT, REDACTED -> {
            }
            default -> scanBand(color, (t / SCAN_SECONDS) % 1f * 1.2f - 0.1f, SCAN_ALPHA * alphaMult);
        }
    }

    private static void drawNoise(Color color, int seed, float t, float alphaMult) {
        int step = (int) (t / NOISE_SECONDS);
        GL11.glBegin(GL11.GL_QUADS);
        for (int speck = 0; speck < NOISE_SPECKS; speck++) {
            float speckU = 0.03f + 0.92f * CargoIconFx.hash(seed, speck, step);
            float speckV = 0.03f + 0.92f * CargoIconFx.hash(seed, speck + 100, step);
            setMix(color, Color.WHITE, 0.5f, 0.5f * CargoIconFx.hash(seed, speck + 200, step) * alphaMult);
            screenQuad(speckU, speckV, speckU + 0.025f, speckV + 0.025f);
        }
        GL11.glEnd();
    }

    private static void scanBand(Color color, float centreV, float alpha) {
        float from = Math.max(0f, centreV - SCAN_HALF);
        float to = Math.min(1f, centreV + SCAN_HALF);
        if (to <= from || alpha <= 0f) {
            return;
        }
        float middle = Math.max(from, Math.min(to, centreV));
        GL11.glBegin(GL11.GL_QUADS);
        scanHalf(color, from, middle, centreV, alpha);
        scanHalf(color, middle, to, centreV, alpha);
        GL11.glEnd();
    }

    private static void scanHalf(Color color, float fromV, float toV, float centreV, float alpha) {
        if (toV <= fromV) {
            return;
        }
        float fromAlpha = alpha * (1f - Math.abs(fromV - centreV) / SCAN_HALF);
        float toAlpha = alpha * (1f - Math.abs(toV - centreV) / SCAN_HALF);
        Misc.setColor(color, fromAlpha);
        vertex(0f, fromV);
        vertex(1f, fromV);
        Misc.setColor(color, toAlpha);
        vertex(1f, toV);
        vertex(0f, toV);
    }

    private static void setMix(Color base, Color other, float amount, float alpha) {
        GL11.glColor4f((base.getRed() + (other.getRed() - base.getRed()) * amount) / 255f,
                (base.getGreen() + (other.getGreen() - base.getGreen()) * amount) / 255f,
                (base.getBlue() + (other.getBlue() - base.getBlue()) * amount) / 255f, Math.max(0f, Math.min(1f, alpha)));
    }

    static Color mix(Color base, Color other, float amount) {
        return new Color(Math.round(base.getRed() + (other.getRed() - base.getRed()) * amount),
                Math.round(base.getGreen() + (other.getGreen() - base.getGreen()) * amount),
                Math.round(base.getBlue() + (other.getBlue() - base.getBlue()) * amount));
    }
}
