package exiledsector.ui.infestation;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.clamp;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ThreatSwarm {

    static final float WORLD_RADIUS_PER_FLOCK = 110f;
    static final int FEWEST_FLOCKS = 4;
    static final int MOST_FLOCKS = 14;
    static final int FEWEST_FRAGMENTS_PER_FLOCK = 8;
    static final int EXTRA_FRAGMENTS_PER_FLOCK = 8;
    static final float LOWEST_ORBIT_SHARE = 0.3f;
    static final float HIGHEST_ORBIT_SHARE = 0.8f;
    static final float WEAVE_SHARE = 0.12f;
    static final float INNERMOST_SHARE = 0.12f;
    static final float OUTERMOST_SHARE = 0.88f;
    static final float FLOCK_LENGTH_SHARE = 0.09f;
    static final float FLOCK_WIDTH_SHARE = 0.04f;
    private static final float MIN_ORBIT_SECONDS = 50f;
    private static final float EXTRA_ORBIT_SECONDS = 40f;
    private static final float MIN_WEAVE_SECONDS = 9f;
    private static final float EXTRA_WEAVE_SECONDS = 8f;
    private static final float CONTRARY_FLOCK_SHARE = 0.25f;
    private static final float JITTER_SHARE = 0.008f;
    private static final float MIN_FRAGMENT_WORLD_SIZE = 9f;
    private static final float MAX_FRAGMENT_WORLD_SIZE = 14f;
    private static final float MIN_SPIN_DEG_PER_SECOND = 30f;
    private static final float EXTRA_SPIN_DEG_PER_SECOND = 70f;
    private static final int FLOCKS_OVER_NODES_EVERY = 4;
    private static final int SALT = 0x51A2;

    record Flock(float orbitShare, float orbitSeconds, float orbitDirection, float orbitPhase, float weaveSeconds, float weavePhase,
                 boolean overNodes, List<SchoolMember> members) {
    }

    record SchoolMember(float alongShare, float sideShare, float jitterRate, float jitterPhase, float worldSize, float spinDegPerSecond,
                        float spinPhaseDeg) {
    }

    private final InfestedGeometry geometry;
    private final List<Flock> flocks;

    ThreatSwarm(InfestedGeometry geometry) {
        this.geometry = geometry;
        this.flocks = layout(geometry);
    }

    static int flockCount(float worldRadius) {
        return InfestationHash.scaledCount(worldRadius / WORLD_RADIUS_PER_FLOCK, FEWEST_FLOCKS, MOST_FLOCKS);
    }

    static float centreShare(Flock flock, float atSeconds) {
        float weave = WEAVE_SHARE * (float) Math.sin(TWO_PI * atSeconds / flock.weaveSeconds() + flock.weavePhase());
        return clamp(flock.orbitShare() + weave, INNERMOST_SHARE, OUTERMOST_SHARE);
    }

    List<Flock> flocks() {
        return flocks;
    }

    void emit(FragmentSink sink, float atSeconds, boolean overNodes) {
        float worldRadius = geometry.worldRadius();
        for (Flock flock : flocks) {
            if (flock.overNodes() != overNodes) {
                continue;
            }
            float orbitAngle = flock.orbitPhase() + flock.orbitDirection() * TWO_PI * atSeconds / flock.orbitSeconds();
            float centreDistance = centreShare(flock, atSeconds) * worldRadius;
            float centreX = geometry.socketX() + (float) Math.cos(orbitAngle) * centreDistance;
            float centreY = geometry.socketY() + (float) Math.sin(orbitAngle) * centreDistance;
            float headingAngle = orbitAngle + flock.orbitDirection() * (float) (Math.PI / 2);
            float headingX = (float) Math.cos(headingAngle);
            float headingY = (float) Math.sin(headingAngle);
            float headingDeg = (float) Math.toDegrees(headingAngle);
            for (SchoolMember member : flock.members()) {
                float jitter = JITTER_SHARE * worldRadius * (float) Math.sin(atSeconds * member.jitterRate() + member.jitterPhase());
                float along = member.alongShare() * FLOCK_LENGTH_SHARE * worldRadius;
                float side = member.sideShare() * FLOCK_WIDTH_SHARE * worldRadius + jitter;
                float fragmentX = centreX + headingX * along - headingY * side;
                float fragmentY = centreY + headingY * along + headingX * side;
                float spinDeg = member.spinPhaseDeg() + member.spinDegPerSecond() * atSeconds;
                sink.fragment(fragmentX, fragmentY, member.worldSize(), -headingDeg + spinDeg * 0.15f, 1f);
            }
        }
    }

    private static List<Flock> layout(InfestedGeometry geometry) {
        int count = flockCount(geometry.worldRadius());
        int sceneSeed = geometry.sceneSeed();
        float sharedDirection = InfestationHash.direction(unit(sceneSeed, 0, SALT));
        List<Flock> laidOut = new ArrayList<>(count);
        for (int flockIndex = 0; flockIndex < count; flockIndex++) {
            int memberCount = FEWEST_FRAGMENTS_PER_FLOCK + (int) (unit(sceneSeed, flockIndex, SALT + 1) * EXTRA_FRAGMENTS_PER_FLOCK);
            List<SchoolMember> members = new ArrayList<>(memberCount);
            for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
                int memberKey = flockIndex * 97 + memberIndex;
                members.add(new SchoolMember(
                        unit(sceneSeed, memberKey, SALT + 2) - 0.5f,
                        (unit(sceneSeed, memberKey, SALT + 3) - 0.5f) * (1f - Math.abs(unit(sceneSeed, memberKey, SALT + 2) - 0.5f)),
                        between(0.8f, 2.4f, unit(sceneSeed, memberKey, SALT + 4)),
                        TWO_PI * unit(sceneSeed, memberKey, SALT + 5),
                        between(MIN_FRAGMENT_WORLD_SIZE, MAX_FRAGMENT_WORLD_SIZE, unit(sceneSeed, memberKey, SALT + 6)),
                        InfestationHash.direction(unit(sceneSeed, memberKey, SALT + 7))
                                * (MIN_SPIN_DEG_PER_SECOND + EXTRA_SPIN_DEG_PER_SECOND * unit(sceneSeed, memberKey, SALT + 8)),
                        360f * unit(sceneSeed, memberKey, SALT + 9)));
            }
            boolean contrary = unit(sceneSeed, flockIndex, SALT + 10) < CONTRARY_FLOCK_SHARE;
            laidOut.add(new Flock(
                    between(LOWEST_ORBIT_SHARE, HIGHEST_ORBIT_SHARE, unit(sceneSeed, flockIndex, SALT + 11)),
                    MIN_ORBIT_SECONDS + EXTRA_ORBIT_SECONDS * unit(sceneSeed, flockIndex, SALT + 12),
                    contrary ? -sharedDirection : sharedDirection,
                    TWO_PI * unit(sceneSeed, flockIndex, SALT + 13),
                    MIN_WEAVE_SECONDS + EXTRA_WEAVE_SECONDS * unit(sceneSeed, flockIndex, SALT + 14),
                    TWO_PI * unit(sceneSeed, flockIndex, SALT + 15),
                    flockIndex % FLOCKS_OVER_NODES_EVERY == FLOCKS_OVER_NODES_EVERY - 1,
                    List.copyOf(members)));
        }
        return List.copyOf(laidOut);
    }
}
