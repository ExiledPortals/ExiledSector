package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedNode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudBlobShells {

    static final int LARGE_NODE_PUFFS = 14;
    static final int SMALL_NODE_PUFFS = 9;
    static final int LARGE_NODE_RIM_PUFFS = 5;
    static final int SMALL_NODE_RIM_PUFFS = 3;
    static final int MOST_SHELL_PUFFS = 900;
    static final float MIN_RIM_DISTANCE_SHARE = 0.95f;
    static final float MAX_RIM_DISTANCE_SHARE = 1.1f;
    static final float MIN_RIM_SIZE_SHARE = 0.35f;
    static final float MAX_RIM_SIZE_SHARE = 0.55f;
    private static final float MIN_DISTANCE_SHARE = 0.85f;
    private static final float MAX_DISTANCE_SHARE = 1.3f;
    private static final float MIN_SIZE_SHARE = 0.75f;
    private static final float MAX_SIZE_SHARE = 1.15f;
    private static final float MIN_LIFE_SECONDS = 3.5f;
    private static final float EXTRA_LIFE_SECONDS = 3.5f;
    private static final float SWIRL_RAD_PER_SECOND = 0.25f;
    private static final float SPIN_DEG_PER_SECOND = 20f;
    private static final float SHELL_ALPHA = 0.85f;
    private static final float RIM_ALPHA = 0.85f;
    private static final float FLASH_CHANCE = 0.12f;
    private static final int DARK_PUFF_EVERY = 3;
    private static final int SALT = 0x5E11;

    record ShellPuff(InfestedNode node, int puffKey, float lifeSeconds, float lifeOffset, float swirlDirection, boolean dark, boolean overRim) {
    }

    private final List<ShellPuff> puffs;

    ShroudBlobShells(InfestedGeometry geometry) {
        this.puffs = layout(geometry);
    }

    static int puffsPerNode(int wanted, int nodeCount) {
        if (nodeCount * (LARGE_NODE_PUFFS + LARGE_NODE_RIM_PUFFS) <= MOST_SHELL_PUFFS) {
            return wanted;
        }
        return Math.max(1, wanted * MOST_SHELL_PUFFS / (nodeCount * (LARGE_NODE_PUFFS + LARGE_NODE_RIM_PUFFS)));
    }

    List<ShellPuff> puffs() {
        return puffs;
    }

    void emitPuffs(PuffSink sink, float atSeconds, boolean overRim) {
        for (ShellPuff puff : puffs) {
            if (puff.overRim() == overRim) {
                emitPuff(sink, puff, atSeconds, 1f, puff.dark() ? ShroudPuffs.DARKNESS : ShroudPuffs.SHROUD,
                        overRim ? RIM_ALPHA : SHELL_ALPHA);
            }
        }
    }

    void emitFlashes(PuffSink sink, float atSeconds, boolean overRim) {
        for (ShellPuff puff : puffs) {
            if (puff.overRim() != overRim || puff.dark()) {
                continue;
            }
            float flash = ShroudPuffs.flash(puff.puffKey(), atSeconds, FLASH_CHANCE);
            if (flash > 0f) {
                emitPuff(sink, puff, atSeconds, ShroudPuffs.FLASH_SIZE_MULT, ShroudPuffs.SHROUD_FLASH, flash);
            }
        }
    }

    private static void emitPuff(PuffSink sink, ShellPuff puff, float atSeconds, float sizeMult, Color tint, float alpha) {
        int key = puff.puffKey();
        int life = ShroudPuffs.lifeIndex(atSeconds, puff.lifeSeconds(), puff.lifeOffset());
        float age = ShroudPuffs.age(atSeconds, puff.lifeSeconds(), puff.lifeOffset());
        float visualRadius = puff.node().visualRadius();
        float distanceShare;
        float sizeShare;
        if (puff.overRim()) {
            distanceShare = between(MIN_RIM_DISTANCE_SHARE, MAX_RIM_DISTANCE_SHARE, unit(key, life, SALT));
            sizeShare = between(MIN_RIM_SIZE_SHARE, MAX_RIM_SIZE_SHARE, unit(key, life, SALT + 1));
        } else {
            distanceShare = between(MIN_DISTANCE_SHARE, MAX_DISTANCE_SHARE, unit(key, life, SALT)) * (0.95f + 0.1f * age);
            sizeShare = between(MIN_SIZE_SHARE, MAX_SIZE_SHARE, unit(key, life, SALT + 1)) * (0.85f + 0.3f * age);
        }
        float angle = TWO_PI * unit(key, life, SALT + 2) + puff.swirlDirection() * SWIRL_RAD_PER_SECOND * atSeconds;
        float puffX = puff.node().worldX() + (float) Math.cos(angle) * distanceShare * visualRadius;
        float puffY = puff.node().worldY() + (float) Math.sin(angle) * distanceShare * visualRadius;
        float spinDeg = 360f * unit(key, life, SALT + 3) + puff.swirlDirection() * SPIN_DEG_PER_SECOND * age * puff.lifeSeconds();
        sink.puff(puffX, puffY, sizeShare * visualRadius * sizeMult, spinDeg, ShroudPuffs.cell(key, life), tint, alpha * ShroudPuffs.fade(age));
    }

    private static List<ShellPuff> layout(InfestedGeometry geometry) {
        List<ShellPuff> laidOut = new ArrayList<>();
        int nodeCount = geometry.nodes().size();
        for (InfestedNode node : geometry.nodes()) {
            int shellCount = puffsPerNode(node.large() ? LARGE_NODE_PUFFS : SMALL_NODE_PUFFS, nodeCount);
            int rimCount = puffsPerNode(node.large() ? LARGE_NODE_RIM_PUFFS : SMALL_NODE_RIM_PUFFS, nodeCount);
            for (int index = 0; index < shellCount + rimCount; index++) {
                int puffKey = node.nodeSeed() * 61 + index;
                boolean overRim = index >= shellCount;
                laidOut.add(new ShellPuff(node, puffKey,
                        MIN_LIFE_SECONDS + EXTRA_LIFE_SECONDS * unit(puffKey, 0, SALT + 4),
                        unit(puffKey, 0, SALT + 5),
                        InfestationHash.direction(unit(node.nodeSeed(), 0, SALT + 6)),
                        !overRim && index % DARK_PUFF_EVERY == 0,
                        overRim));
            }
        }
        return List.copyOf(laidOut);
    }
}
