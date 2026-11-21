package exiledsector.ui.infestation;

import java.awt.Color;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudBlobRim {

    static final float WORLD_ARC_PER_PUFF = 30f;
    static final int FEWEST_PUFFS = 40;
    static final int MOST_PUFFS = 220;
    static final float BREATH_SECONDS = 6f;
    private static final float DRIFT_RAD_PER_SECOND = 0.006f;
    private static final float MIN_LIFE_SECONDS = 4f;
    private static final float EXTRA_LIFE_SECONDS = 4f;
    private static final float RAGGED_SHARE = 0.05f;
    private static final float BREATH_SHARE = 0.015f;
    private static final float BREATH_LOBES = 3f;
    private static final float MIN_SIZE_SHARE = 0.045f;
    private static final float MAX_SIZE_SHARE = 0.075f;
    private static final float SIZE_BREATH_SHARE = 0.15f;
    private static final float DARK_SHARE = 0.55f;
    private static final float DARK_ALPHA = 0.9f;
    private static final float RED_ALPHA = 0.75f;
    private static final float FLASH_CHANCE = 0.08f;
    private static final float GLOW_ALPHA = 0.08f;
    private static final float GLOW_BREATH_ALPHA = 0.06f;
    private static final int SALT = 0x2B1B;

    private final InfestedGeometry geometry;
    private final int puffCount;

    ShroudBlobRim(InfestedGeometry geometry) {
        this.geometry = geometry;
        this.puffCount = puffCount(geometry.worldRadius());
    }

    static int puffCount(float worldRadius) {
        return InfestationHash.scaledCount(TWO_PI * worldRadius / WORLD_ARC_PER_PUFF, FEWEST_PUFFS, MOST_PUFFS);
    }

    static float breath(float atSeconds) {
        return (float) Math.sin(TWO_PI * atSeconds / BREATH_SECONDS);
    }

    static float glowAlpha(float atSeconds) {
        return GLOW_ALPHA + GLOW_BREATH_ALPHA * breath(atSeconds);
    }

    void emitPuffs(PuffSink sink, float atSeconds) {
        emit(sink, atSeconds, false);
    }

    void emitFlashes(PuffSink sink, float atSeconds) {
        emit(sink, atSeconds, true);
    }

    private void emit(PuffSink sink, float atSeconds, boolean flashesOnly) {
        float worldRadius = geometry.worldRadius();
        int seed = geometry.sceneSeed();
        float drift = DRIFT_RAD_PER_SECOND * atSeconds;
        float sizeBreath = 1f + SIZE_BREATH_SHARE * breath(atSeconds);
        for (int index = 0; index < puffCount; index++) {
            int puffKey = seed + index * 197;
            boolean dark = unit(puffKey, 0, SALT) < DARK_SHARE;
            float flash = 1f;
            if (flashesOnly) {
                if (dark) {
                    continue;
                }
                flash = ShroudPuffs.flash(puffKey, atSeconds, FLASH_CHANCE);
                if (flash <= 0f) {
                    continue;
                }
            }
            float lifeSeconds = MIN_LIFE_SECONDS + EXTRA_LIFE_SECONDS * unit(puffKey, 0, SALT + 1);
            float lifeOffset = unit(puffKey, 0, SALT + 2);
            int life = ShroudPuffs.lifeIndex(atSeconds, lifeSeconds, lifeOffset);
            float age = ShroudPuffs.age(atSeconds, lifeSeconds, lifeOffset);
            float angle = TWO_PI * (index + unit(puffKey, life, SALT + 3) - 0.5f) / puffCount + drift;
            float ragged = 1f + (unit(puffKey, life, SALT + 4) - 0.5f) * RAGGED_SHARE
                    + BREATH_SHARE * (float) Math.sin(TWO_PI * atSeconds / BREATH_SECONDS + BREATH_LOBES * angle);
            float puffX = geometry.socketX() + (float) Math.cos(angle) * worldRadius * ragged;
            float puffY = geometry.socketY() + (float) Math.sin(angle) * worldRadius * ragged;
            float size = worldRadius * between(MIN_SIZE_SHARE, MAX_SIZE_SHARE, unit(puffKey, life, SALT + 5)) * sizeBreath;
            Color tint;
            float alpha;
            if (flashesOnly) {
                tint = ShroudPuffs.SHROUD_FLASH;
                alpha = flash;
                size *= ShroudPuffs.FLASH_SIZE_MULT;
            } else {
                tint = dark ? ShroudPuffs.DARKNESS : ShroudPuffs.SHROUD;
                alpha = dark ? DARK_ALPHA : RED_ALPHA;
            }
            float fadeFloor = 0.35f + 0.65f * ShroudPuffs.fade(age);
            sink.puff(puffX, puffY, size, 360f * unit(puffKey, life, SALT + 6) + atSeconds * 6f, ShroudPuffs.cell(puffKey, life), tint,
                    alpha * fadeFloor);
        }
    }
}
