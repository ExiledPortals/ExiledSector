package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.decoration.ShroudedMaw;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.List;

public final class ShroudedMawRenderer extends UnitDecorationRenderer<ShroudedMaw> {

    record Puff(float side, float forward, float size, float spinDeg, float alpha, float flash, int cell) {
    }

    record Placement(float centerX, float centerY, float screenLength, float angleDeg, float cos, float sin) {

        static Placement of(float centerX, float centerY, float screenLength, float angleDeg) {
            double angleRad = Math.toRadians(angleDeg);
            return new Placement(centerX, centerY, screenLength, angleDeg, (float) Math.cos(angleRad), (float) Math.sin(angleRad));
        }

        float x(float side, float forward) {
            return centerX + (side * cos - forward * sin) * screenLength;
        }

        float y(float side, float forward) {
            return centerY + (side * sin + forward * cos) * screenLength;
        }
    }

    static final String BODY_SPRITE_PATH = "graphics/ships/shroud/maw_base.png";
    static final String GLOW_SPRITE_PATH = "graphics/ships/shroud/maw_glow.png";
    static final String SPOTS_SPRITE_PATH = "graphics/ships/shroud/spot_cluster.png";
    static final String SHROUD_SHEET_PATH = "graphics/ships/shroud/shrouded_blobs.png";
    static final int SHEET_SIDE = 4;
    static final int SHROUD_PUFFS = 120;
    static final int DARK_PUFFS = 60;
    static final float SHROUD_RADIUS_LENGTHS = 0.52f;
    static final float FORWARD_SQUASH = 0.65f;
    static final float SHROUD_PUFF_LENGTHS = 0.335f;
    static final float DARK_PUFF_LENGTHS = 0.42f;
    static final float DARK_RADIUS_SHARE = 0.72f;
    static final float REACH_LENGTHS = 0.9f;
    private static final float MIN_SPAWN_SHARE = 0.4f;
    private static final float MAX_SPAWN_SHARE = 0.8f;
    private static final float OUTWARD_DRIFT = 0.35f;
    private static final float SWIRL_RAD_PER_SECOND = 0.12f;
    private static final float MIN_PUFF_SECONDS = 6f;
    private static final float EXTRA_PUFF_SECONDS = 6f;
    private static final float MIN_DARK_SECONDS = 4f;
    private static final float EXTRA_DARK_SECONDS = 4f;
    private static final float SPIN_DEG_PER_SECOND = 20f;
    private static final float FLASH_SLOT_SECONDS = 4f;
    private static final float FLASH_SECONDS = 0.9f;
    private static final float FLASH_CHANCE = 0.15f;
    private static final float FLASH_SIZE_MULT = 1.2f;
    private static final float BODY_HEIGHT_PER_LENGTH = 256f / 384f;
    private static final float GLOW_FORWARD_LENGTHS = 0.078f;
    private static final float SPOTS_WIDTH_LENGTHS = 0.52f;
    private static final float SPOTS_HEIGHT_LENGTHS = 0.427f;
    private static final float SPOTS_FORWARD_LENGTHS = 0.013f;
    private static final int WARP_VERTICES = 5;
    private static final float BODY_WARP_LENGTHS = 0.028f;
    private static final float GLOW_WARP_LENGTHS = 0.02f;
    private static final float MIN_WARP_RATE = 0.5f;
    private static final float EXTRA_WARP_RATE = 0.7f;
    private static final float SWAY_DEGREES = 3f;
    private static final float SWAY_SECONDS = 26f;
    private static final float DRIFT_LENGTHS = 0.02f;
    private static final float DRIFT_SECONDS = 37f;
    private static final float BODY_ALPHA = 0.9f;
    private static final float SHROUD_ALPHA = 0.85f;
    private static final float DARK_ALPHA = 0.85f;
    private static final float GLOW_ALPHA = 0.35f;
    private static final float GLOW_PULSE = 0.1f;
    private static final float GLOW_PULSE_SECONDS = 5f;
    private static final float SPOTS_ALPHA = 0.45f;
    private static final float SPOTS_PULSE = 0.3f;
    private static final float SPOTS_PULSE_SECONDS = 3.5f;
    private static final Color SHROUD = new Color(175, 0, 28);
    private static final Color SHROUD_FLASH = new Color(200, 10, 20);
    private static final Color PART_GLOW = new Color(255, 0, 50);
    private static final Color DARKNESS = new Color(6, 0, 3);
    private static final float TWO_PI = (float) (Math.PI * 2);

    private final TexturedQuadBatch quadBatch = new TexturedQuadBatch();

    ShroudedMawRenderer(List<ShroudedMaw> maws) {
        super(maws);
    }

    @Override
    void renderUnit(TreeViewport viewport, ShroudedMaw maw, float alphaMult) {
        float bodyLength = maw.getLength();
        float driftPhase = TWO_PI * elapsedSeconds / DRIFT_SECONDS;
        float screenX = viewport.screenX(maw.getX() + (float) Math.cos(driftPhase) * bodyLength * DRIFT_LENGTHS);
        float screenY = viewport.screenY(maw.getY() + (float) Math.sin(driftPhase * 2f) * bodyLength * DRIFT_LENGTHS);
        float screenLength = bodyLength * viewport.zoom();
        if (!viewport.isVisible(screenX, screenY, screenLength * REACH_LENGTHS)) {
            return;
        }
        float swayDeg = SWAY_DEGREES * (float) Math.sin(TWO_PI * elapsedSeconds / SWAY_SECONDS);
        Placement placement = Placement.of(screenX, screenY, screenLength, -(maw.getRotation() + swayDeg));

        SpriteAPI body = spriteCache.texture(BODY_SPRITE_PATH);
        if (body != null) {
            try (TexturedQuadBatch quads = quadBatch.beginNormal(body)) {
                drawWarped(quads, placement, BODY_HEIGHT_PER_LENGTH, 0f, BODY_WARP_LENGTHS, Color.WHITE, alphaMult * BODY_ALPHA);
            }
        }
        SpriteAPI sheet = spriteCache.texture(SHROUD_SHEET_PATH);
        if (sheet != null) {
            try (TexturedQuadBatch quads = quadBatch.beginNormal(sheet)) {
                for (int index = 0; index < SHROUD_PUFFS; index++) {
                    drawPuff(quads, placement, shroudPuff(index, elapsedSeconds), 1f, SHROUD, alphaMult * SHROUD_ALPHA);
                }
                for (int index = 0; index < DARK_PUFFS; index++) {
                    drawPuff(quads, placement, darkPuff(index, elapsedSeconds), 1f, DARKNESS, alphaMult * DARK_ALPHA);
                }
            }
        }
        SpriteAPI glow = spriteCache.texture(GLOW_SPRITE_PATH);
        if (glow != null) {
            float glowAlpha = GLOW_ALPHA + GLOW_PULSE * (float) Math.sin(TWO_PI * elapsedSeconds / GLOW_PULSE_SECONDS);
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(glow)) {
                drawWarped(quads, placement, BODY_HEIGHT_PER_LENGTH, GLOW_FORWARD_LENGTHS, GLOW_WARP_LENGTHS, PART_GLOW, alphaMult * glowAlpha);
            }
        }
        SpriteAPI spots = spriteCache.texture(SPOTS_SPRITE_PATH);
        if (spots != null) {
            float spotsAlpha = SPOTS_ALPHA + SPOTS_PULSE * (float) Math.sin(TWO_PI * elapsedSeconds / SPOTS_PULSE_SECONDS + 1f);
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(spots)) {
                quads.quadAtCenter(placement.x(0f, SPOTS_FORWARD_LENGTHS), placement.y(0f, SPOTS_FORWARD_LENGTHS),
                        SPOTS_WIDTH_LENGTHS * screenLength, SPOTS_HEIGHT_LENGTHS * screenLength, placement.angleDeg(), PART_GLOW, alphaMult * spotsAlpha);
            }
        }
        if (sheet != null) {
            try (TexturedQuadBatch quads = quadBatch.beginAdditive(sheet)) {
                for (int index = 0; index < SHROUD_PUFFS; index++) {
                    Puff puff = shroudPuff(index, elapsedSeconds);
                    if (puff.flash() > 0f) {
                        drawPuff(quads, placement, puff, FLASH_SIZE_MULT, SHROUD_FLASH, alphaMult * puff.flash());
                    }
                }
            }
        }
    }

    private void drawWarped(TexturedQuadBatch quads, Placement placement, float heightLengths, float forwardOffset, float warpLengths,
                            Color tint, float alpha) {
        quads.color(tint, alpha);
        for (int column = 0; column < WARP_VERTICES - 1; column++) {
            for (int row = 0; row < WARP_VERTICES - 1; row++) {
                warpedVertex(quads, placement, column, row, heightLengths, forwardOffset, warpLengths);
                warpedVertex(quads, placement, column, row + 1, heightLengths, forwardOffset, warpLengths);
                warpedVertex(quads, placement, column + 1, row + 1, heightLengths, forwardOffset, warpLengths);
                warpedVertex(quads, placement, column + 1, row, heightLengths, forwardOffset, warpLengths);
            }
        }
    }

    private void warpedVertex(TexturedQuadBatch quads, Placement placement, int column, int row, float heightLengths, float forwardOffset,
                              float warpLengths) {
        float fractionX = column / (float) (WARP_VERTICES - 1);
        float fractionY = row / (float) (WARP_VERTICES - 1);
        int vertex = column * WARP_VERTICES + row;
        float side = fractionX - 0.5f + warpLengths * warp(vertex, 0, elapsedSeconds);
        float forward = (fractionY - 0.5f) * heightLengths + forwardOffset + warpLengths * warp(vertex, 1, elapsedSeconds);
        quads.meshVertex(fractionX, fractionY, placement.x(side, forward), placement.y(side, forward));
    }

    static float warp(int vertex, int axis, float atSeconds) {
        float rate = MIN_WARP_RATE + EXTRA_WARP_RATE * hash(vertex, axis, 1);
        return (float) Math.sin(atSeconds * rate + TWO_PI * hash(vertex, axis, 2));
    }

    private static void drawPuff(TexturedQuadBatch quads, Placement placement, Puff puff, float sizeMult, Color tint, float alphaMult) {
        if (puff.alpha() <= 0f) {
            return;
        }
        float size = puff.size() * sizeMult * placement.screenLength();
        quads.cellAtCenter(SHEET_SIDE, SHEET_SIDE, puff.cell(), placement.x(puff.side(), puff.forward()), placement.y(puff.side(), puff.forward()),
                size, size, placement.angleDeg() + puff.spinDeg(), tint, alphaMult * puff.alpha());
    }

    static Puff shroudPuff(int index, float atSeconds) {
        float lifeSeconds = MIN_PUFF_SECONDS + EXTRA_PUFF_SECONDS * hash(index, 0, 3);
        float aged = atSeconds / lifeSeconds + hash(index, 0, 4);
        int life = (int) Math.floor(aged);
        float age = aged - life;
        float spawnShare = MIN_SPAWN_SHARE + (MAX_SPAWN_SHARE - MIN_SPAWN_SHARE) * (float) Math.sqrt(hash(index, life, 5));
        float radiusShare = spawnShare + (1f - spawnShare) * OUTWARD_DRIFT * age;
        float swirl = (index % 2 == 0 ? 1f : -1f) * SWIRL_RAD_PER_SECOND * atSeconds;
        float angle = TWO_PI * hash(index, life, 6) + swirl;
        float size = SHROUD_PUFF_LENGTHS * (0.75f + 0.5f * hash(index, life, 7)) * (0.85f + 0.3f * age);
        float spinDeg = 360f * hash(index, life, 8) + (hash(index, life, 9) - 0.5f) * 2f * SPIN_DEG_PER_SECOND * age * lifeSeconds;
        return new Puff(
                (float) Math.cos(angle) * radiusShare * SHROUD_RADIUS_LENGTHS,
                (float) Math.sin(angle) * radiusShare * SHROUD_RADIUS_LENGTHS * FORWARD_SQUASH,
                size, spinDeg, (float) Math.sin(Math.PI * age), flash(index, atSeconds), cell(index, life));
    }

    static Puff darkPuff(int index, float atSeconds) {
        float lifeSeconds = MIN_DARK_SECONDS + EXTRA_DARK_SECONDS * hash(index, 0, 10);
        float aged = atSeconds / lifeSeconds + hash(index, 0, 11);
        int life = (int) Math.floor(aged);
        float age = aged - life;
        float radiusShare = DARK_RADIUS_SHARE * (float) Math.sqrt(hash(index, life, 12));
        float angle = TWO_PI * hash(index, life, 13);
        float spinDeg = 360f * hash(index, life, 14);
        return new Puff(
                (float) Math.cos(angle) * radiusShare * SHROUD_RADIUS_LENGTHS,
                (float) Math.sin(angle) * radiusShare * SHROUD_RADIUS_LENGTHS * FORWARD_SQUASH,
                DARK_PUFF_LENGTHS * (0.9f + 0.2f * age), spinDeg, (float) Math.sin(Math.PI * age), 0f, cell(index + SHROUD_PUFFS, life));
    }

    private static float flash(int index, float atSeconds) {
        float slotted = atSeconds / FLASH_SLOT_SECONDS + hash(index, 0, 15);
        int slot = (int) Math.floor(slotted);
        float intoSlotSeconds = (slotted - slot) * FLASH_SLOT_SECONDS;
        if (intoSlotSeconds >= FLASH_SECONDS || hash(index, slot, 16) >= FLASH_CHANCE) {
            return 0f;
        }
        return (float) Math.sin(Math.PI * intoSlotSeconds / FLASH_SECONDS);
    }

    private static int cell(int index, int life) {
        return (int) (hash(index, life, 17) * SHEET_SIDE * SHEET_SIDE);
    }

    static float hash(int index, int life, int salt) {
        int mixed = index * 0x9E3779B1 + life * 0x85EBCA77 + salt * 0xC2B2AE3D;
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        mixed *= 0x846CA68B;
        mixed ^= mixed >>> 16;
        return (mixed >>> 8) / (float) (1 << 24);
    }
}
