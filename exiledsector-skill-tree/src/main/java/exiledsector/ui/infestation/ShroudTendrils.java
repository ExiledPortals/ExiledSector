package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedLink;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.fraction;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudTendrils {

    static final float WORLD_LENGTH_PER_PUFF = 18f;
    static final int FEWEST_PUFFS_PER_LINK = 4;
    static final int MOST_PUFFS_PER_LINK = 26;
    private static final float MIN_CRAWL_WORLD_PER_SECOND = 10f;
    private static final float MAX_CRAWL_WORLD_PER_SECOND = 18f;
    private static final float SWAY_WORLD = 9f;
    private static final float SWAY_WAVES = 1.5f;
    private static final float SWAY_RATE = 0.9f;
    private static final float END_THICKNESS_SHARE = 0.75f;
    private static final float MIDDLE_THINNING_SHARE = 0.35f;
    private static final float RED_CORE_SIZE_MULT = 0.6f;
    private static final float DARK_ALPHA = 0.85f;
    private static final float RED_ALPHA = 0.7f;
    private static final float FLASH_CHANCE = 0.06f;
    private static final int SALT = 0x7E4D;

    record Tendril(InfestedLink link, float crawlWorldPerSecond, float swayPhase, int puffCount, float meanVisualRadius) {
    }

    private final List<Tendril> tendrils;

    ShroudTendrils(InfestedGeometry geometry) {
        this.tendrils = layout(geometry);
    }

    static int puffCount(float worldLength) {
        return InfestationHash.scaledCount(worldLength / WORLD_LENGTH_PER_PUFF, FEWEST_PUFFS_PER_LINK, MOST_PUFFS_PER_LINK);
    }

    static float thicknessShare(float alongShare) {
        return END_THICKNESS_SHARE - MIDDLE_THINNING_SHARE * (float) Math.sin(Math.PI * alongShare);
    }

    void emitPuffs(PuffSink sink, float atSeconds) {
        for (Tendril tendril : tendrils) {
            emitTendril(sink, tendril, atSeconds, 0f, 1f, ShroudPuffs.DARKNESS, DARK_ALPHA, false);
        }
        for (Tendril tendril : tendrils) {
            emitTendril(sink, tendril, atSeconds, 0.5f, RED_CORE_SIZE_MULT, ShroudPuffs.SHROUD, RED_ALPHA, false);
        }
    }

    void emitFlashes(PuffSink sink, float atSeconds) {
        for (Tendril tendril : tendrils) {
            emitTendril(sink, tendril, atSeconds, 0.5f, RED_CORE_SIZE_MULT * ShroudPuffs.FLASH_SIZE_MULT, ShroudPuffs.SHROUD_FLASH, 1f, true);
        }
    }

    private static void emitTendril(PuffSink sink, Tendril tendril, float atSeconds, float slotOffset, float sizeMult, Color tint, float alpha,
                                    boolean flashesOnly) {
        InfestedLink link = tendril.link();
        float length = link.worldLength();
        float directionX = link.directionX();
        float directionY = link.directionY();
        float crawlShare = atSeconds * tendril.crawlWorldPerSecond() / Math.max(1f, length);
        int count = tendril.puffCount();
        for (int index = 0; index < count; index++) {
            int puffKey = link.linkSeed() * 53 + index;
            float alongShare = fraction((index + 0.5f + slotOffset) / count + crawlShare);
            float flash = flashesOnly ? ShroudPuffs.flash(puffKey, atSeconds, FLASH_CHANCE) : 1f;
            if (flash <= 0f) {
                continue;
            }
            float envelope = 0.4f + 0.6f * (float) Math.sin(Math.PI * alongShare);
            float sway = SWAY_WORLD * envelope
                    * (float) Math.sin(alongShare * TWO_PI * SWAY_WAVES + atSeconds * SWAY_RATE + tendril.swayPhase());
            float along = alongShare * length;
            float puffX = link.from().worldX() + directionX * along - directionY * sway;
            float puffY = link.from().worldY() + directionY * along + directionX * sway;
            float size = tendril.meanVisualRadius() * thicknessShare(alongShare) * sizeMult;
            int life = (int) Math.floor((index + slotOffset) / count + crawlShare);
            sink.puff(puffX, puffY, size, 360f * unit(puffKey, life, SALT) + atSeconds * 8f, ShroudPuffs.cell(puffKey, life), tint, alpha * flash);
        }
    }

    private static List<Tendril> layout(InfestedGeometry geometry) {
        List<Tendril> laidOut = new ArrayList<>(geometry.links().size());
        for (InfestedLink link : geometry.links()) {
            if (link.worldLength() <= 0f) {
                continue;
            }
            int seed = link.linkSeed();
            laidOut.add(new Tendril(link,
                    between(MIN_CRAWL_WORLD_PER_SECOND, MAX_CRAWL_WORLD_PER_SECOND, unit(seed, 0, SALT + 1)),
                    TWO_PI * unit(seed, 0, SALT + 2),
                    puffCount(link.worldLength()),
                    (link.from().visualRadius() + link.to().visualRadius()) / 2f));
        }
        return List.copyOf(laidOut);
    }
}
