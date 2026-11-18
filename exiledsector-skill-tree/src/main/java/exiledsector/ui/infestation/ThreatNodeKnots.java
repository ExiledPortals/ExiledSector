package exiledsector.ui.infestation;

import exiledsector.ui.infestation.InfestedGeometry.InfestedNode;

import java.util.ArrayList;
import java.util.List;

import static exiledsector.ui.infestation.InfestationHash.TWO_PI;
import static exiledsector.ui.infestation.InfestationHash.between;
import static exiledsector.ui.infestation.InfestationHash.unit;

final class ThreatNodeKnots {

    static final int LARGE_NODE_FRAGMENTS = 7;
    static final int SMALL_NODE_FRAGMENTS = 4;
    static final int FEWEST_FRAGMENTS_PER_NODE = 2;
    static final int MOST_KNOT_FRAGMENTS = 420;
    private static final float MIN_ORBIT_RADIUS_SHARE = 1.1f;
    private static final float MAX_ORBIT_RADIUS_SHARE = 1.5f;
    private static final float MIN_TILT = 0.35f;
    private static final float MAX_TILT = 0.75f;
    private static final float MIN_ORBIT_SECONDS = 2.5f;
    private static final float EXTRA_ORBIT_SECONDS = 3.5f;
    private static final float LUNGE_DEPTH = 0.35f;
    private static final float MIN_LUNGE_RATE = 0.6f;
    private static final float EXTRA_LUNGE_RATE = 0.9f;
    private static final float MIN_FRAGMENT_WORLD_SIZE = 7f;
    private static final float MAX_FRAGMENT_WORLD_SIZE = 11f;
    private static final float BEHIND_BRIGHTNESS = 0.55f;
    private static final float SPIN_DEG_PER_SECOND = 90f;
    private static final int SALT = 0x6E07;

    record KnotFragment(InfestedNode node, float orbitRadiusShare, float tilt, float ellipseCos, float ellipseSin, float orbitSeconds,
                        float orbitDirection, float orbitPhase, float lungeRate, float lungePhase, float worldSize) {
    }

    private final List<KnotFragment> fragments;

    ThreatNodeKnots(InfestedGeometry geometry) {
        this.fragments = layout(geometry);
    }

    static int fragmentsPerNode(boolean large, int nodeCount) {
        int wanted = large ? LARGE_NODE_FRAGMENTS : SMALL_NODE_FRAGMENTS;
        if (nodeCount * wanted <= MOST_KNOT_FRAGMENTS) {
            return wanted;
        }
        return Math.max(FEWEST_FRAGMENTS_PER_NODE, wanted * MOST_KNOT_FRAGMENTS / (nodeCount * LARGE_NODE_FRAGMENTS));
    }

    List<KnotFragment> fragments() {
        return fragments;
    }

    void emit(FragmentSink sink, float atSeconds, boolean inFront) {
        for (KnotFragment fragment : fragments) {
            float orbitAngle = fragment.orbitPhase() + fragment.orbitDirection() * TWO_PI * atSeconds / fragment.orbitSeconds();
            float sine = (float) Math.sin(orbitAngle);
            if (sine > 0f != inFront) {
                continue;
            }
            float lunge = (float) Math.max(0.0, Math.sin(atSeconds * fragment.lungeRate() + fragment.lungePhase()));
            float lungeSquared = lunge * lunge;
            float orbitRadius = fragment.node().visualRadius() * fragment.orbitRadiusShare() * (1f - LUNGE_DEPTH * lungeSquared * lungeSquared);
            float localX = (float) Math.cos(orbitAngle) * orbitRadius;
            float localY = sine * orbitRadius * fragment.tilt();
            float worldX = fragment.node().worldX() + localX * fragment.ellipseCos() - localY * fragment.ellipseSin();
            float worldY = fragment.node().worldY() + localX * fragment.ellipseSin() + localY * fragment.ellipseCos();
            float brightness = inFront ? 1f : BEHIND_BRIGHTNESS;
            sink.fragment(worldX, worldY, fragment.worldSize(), fragment.orbitPhase() * 57f + atSeconds * SPIN_DEG_PER_SECOND * fragment.orbitDirection(),
                    brightness);
        }
    }

    private static List<KnotFragment> layout(InfestedGeometry geometry) {
        List<KnotFragment> laidOut = new ArrayList<>();
        int nodeCount = geometry.nodes().size();
        for (InfestedNode node : geometry.nodes()) {
            int count = fragmentsPerNode(node.large(), nodeCount);
            for (int index = 0; index < count; index++) {
                int seed = node.nodeSeed();
                float ellipseAngle = TWO_PI * unit(seed, index, SALT);
                laidOut.add(new KnotFragment(node,
                        between(MIN_ORBIT_RADIUS_SHARE, MAX_ORBIT_RADIUS_SHARE, unit(seed, index, SALT + 1)),
                        between(MIN_TILT, MAX_TILT, unit(seed, index, SALT + 2)),
                        (float) Math.cos(ellipseAngle),
                        (float) Math.sin(ellipseAngle),
                        MIN_ORBIT_SECONDS + EXTRA_ORBIT_SECONDS * unit(seed, index, SALT + 3),
                        InfestationHash.direction(unit(seed, index, SALT + 4)),
                        TWO_PI * unit(seed, index, SALT + 5),
                        MIN_LUNGE_RATE + EXTRA_LUNGE_RATE * unit(seed, index, SALT + 6),
                        TWO_PI * unit(seed, index, SALT + 7),
                        between(MIN_FRAGMENT_WORLD_SIZE, MAX_FRAGMENT_WORLD_SIZE, unit(seed, index, SALT + 8))));
            }
        }
        return List.copyOf(laidOut);
    }
}
