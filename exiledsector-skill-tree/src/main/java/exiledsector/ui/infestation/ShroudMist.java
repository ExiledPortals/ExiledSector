package exiledsector.ui.infestation;

import java.awt.Color;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudMist {

    static final float WORLD_RADIUS_PER_PUFF = 12f;
    static final int FEWEST_PUFFS = 30;
    static final int MOST_PUFFS = 100;
    static final float FARTHEST_CENTRE_SHARE = 0.78f;
    static final float MIN_SIZE_SHARE = 0.22f;
    static final float MAX_SIZE_SHARE = 0.34f;
    private static final float MIN_LIFE_SECONDS = 7f;
    private static final float EXTRA_LIFE_SECONDS = 6f;
    private static final float OUTWARD_DRIFT_SHARE = 0.08f;
    private static final float SWIRL_RAD_PER_SECOND = 0.03f;
    private static final float CENTRE_BIAS = 1.4f;
    private static final float CORE_ALPHA = 0.55f;
    private static final float EDGE_ALPHA = 0.08f;
    private static final float DEEP_PURPLE_SHARE = 0.25f;
    private static final int SALT = 0x4D15;

    private final InfestedGeometry geometry;
    private final int puffCount;

    ShroudMist(InfestedGeometry geometry) {
        this.geometry = geometry;
        this.puffCount = puffCount(geometry.worldRadius());
    }

    static int puffCount(float worldRadius) {
        return InfestationHash.scaledCount(worldRadius / WORLD_RADIUS_PER_PUFF, FEWEST_PUFFS, MOST_PUFFS);
    }

    static float alphaAt(float distanceShare) {
        float closeness = Math.max(0f, 1f - distanceShare);
        return EDGE_ALPHA + (CORE_ALPHA - EDGE_ALPHA) * (float) Math.pow(closeness, 0.8);
    }

    void emit(PuffSink sink, float atSeconds) {
        float worldRadius = geometry.worldRadius();
        int seed = geometry.sceneSeed();
        for (int index = 0; index < puffCount; index++) {
            int puffKey = seed + index * 131;
            float lifeSeconds = MIN_LIFE_SECONDS + EXTRA_LIFE_SECONDS * unit(puffKey, 0, SALT);
            float lifeOffset = unit(puffKey, 0, SALT + 1);
            int life = ShroudPuffs.lifeIndex(atSeconds, lifeSeconds, lifeOffset);
            float age = ShroudPuffs.age(atSeconds, lifeSeconds, lifeOffset);
            float startShare = FARTHEST_CENTRE_SHARE * (float) Math.pow(unit(puffKey, life, SALT + 2), CENTRE_BIAS);
            float distanceShare = Math.min(FARTHEST_CENTRE_SHARE, startShare + OUTWARD_DRIFT_SHARE * age);
            float swirl = InfestationHash.direction(unit(puffKey, 0, SALT + 3)) * SWIRL_RAD_PER_SECOND * atSeconds;
            float angle = TWO_PI * unit(puffKey, life, SALT + 4) + swirl;
            float size = worldRadius * between(MIN_SIZE_SHARE, MAX_SIZE_SHARE, unit(puffKey, life, SALT + 5)) * (0.9f + 0.2f * age);
            Color tint = unit(puffKey, life, SALT + 6) < DEEP_PURPLE_SHARE ? ShroudPuffs.DEEP_PURPLE : ShroudPuffs.DARKNESS;
            float puffX = geometry.socketX() + (float) Math.cos(angle) * distanceShare * worldRadius;
            float puffY = geometry.socketY() + (float) Math.sin(angle) * distanceShare * worldRadius;
            sink.puff(puffX, puffY, size, 360f * unit(puffKey, life, SALT + 7) + swirl * 57f, ShroudPuffs.cell(puffKey, life), tint,
                    alphaAt(distanceShare) * ShroudPuffs.fade(age));
        }
    }
}
