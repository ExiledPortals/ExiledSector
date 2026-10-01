package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_SIZE;

final class StartingRootChoice {

    enum Phase { CHOOSING, FLYING, CHOSEN, RETURNING }

    static final float FLIGHT_SECONDS = 1.5f;
    static final float ROOT_FOOTPRINT = NODE_SIZE * SkillTier.ROOT.getSizeMultiplier();
    static final float CLUSTER_RADIUS = ROOT_FOOTPRINT * 0.7f;
    static final float PROMPT_GAP = 30f;
    private static final float MIN_DIRECTION_LENGTH = 0.001f;

    private final Map<String, Vector2f> clusterOffsets;
    private Phase phase;
    private SkillNode chosen;
    private float flightElapsed;

    private StartingRootChoice(Map<String, Vector2f> clusterOffsets, Phase phase, SkillNode chosen) {
        this.clusterOffsets = clusterOffsets;
        this.phase = phase;
        this.chosen = chosen;
    }

    static StartingRootChoice alreadyChosen(SkillNode root) {
        return new StartingRootChoice(Map.of(), Phase.CHOSEN, root);
    }

    static StartingRootChoice pending(List<SkillNode> roots) {
        if (roots.isEmpty()) {
            return alreadyChosen(null);
        }
        Map<String, Vector2f> offsets = new HashMap<>();
        for (int i = 0; i < roots.size(); i++) {
            SkillNode root = roots.get(i);
            Vector2f direction = directionFromSun(root, i, roots.size());
            offsets.put(root.getId(), new Vector2f(direction.x * CLUSTER_RADIUS, direction.y * CLUSTER_RADIUS));
        }
        return new StartingRootChoice(offsets, Phase.CHOOSING, null);
    }

    private static Vector2f directionFromSun(SkillNode root, int index, int count) {
        float x = root.getOffsetX();
        float y = root.getOffsetY();
        float length = (float) Math.hypot(x, y);
        if (length < MIN_DIRECTION_LENGTH) {
            double angle = 2.0 * Math.PI * index / count;
            return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
        }
        return new Vector2f(x / length, y / length);
    }

    Phase phase() {
        return phase;
    }

    SkillNode chosen() {
        return chosen;
    }

    boolean isInputLocked() {
        return phase != Phase.CHOSEN;
    }

    void choose(SkillNode root) {
        if (phase != Phase.CHOOSING || !clusterOffsets.containsKey(root.getId())) {
            return;
        }
        chosen = root;
        phase = Phase.FLYING;
        flightElapsed = 0f;
    }

    static StartingRootChoice returning(List<SkillNode> roots, SkillNode chosen) {
        StartingRootChoice choice = pending(roots);
        if (choice.phase == Phase.CHOOSING && choice.clusterOffsets.containsKey(chosen.getId())) {
            choice.chosen = chosen;
            choice.phase = Phase.RETURNING;
        }
        return choice;
    }

    boolean isMoving() {
        return phase == Phase.FLYING || phase == Phase.RETURNING;
    }

    SkillNode rootForAllocation() {
        return phase == Phase.FLYING || phase == Phase.CHOSEN ? chosen : null;
    }

    void advance(float amount) {
        if (!isMoving()) {
            return;
        }
        flightElapsed = Math.min(FLIGHT_SECONDS, flightElapsed + amount);
        if (flightElapsed < FLIGHT_SECONDS) {
            return;
        }
        if (phase == Phase.FLYING) {
            phase = Phase.CHOSEN;
        } else {
            phase = Phase.CHOOSING;
            chosen = null;
            flightElapsed = 0f;
        }
    }

    float offsetX(SkillNode node) {
        Vector2f cluster = clusterOffsets.get(node.getId());
        return cluster == null ? node.getOffsetX() : lerp(cluster.x, node.getOffsetX(), progress());
    }

    float offsetY(SkillNode node) {
        Vector2f cluster = clusterOffsets.get(node.getId());
        return cluster == null ? node.getOffsetY() : lerp(cluster.y, node.getOffsetY(), progress());
    }

    float cameraTargetX() {
        return headingOut() && chosen != null ? offsetX(chosen) : 0f;
    }

    float cameraTargetY() {
        return headingOut() && chosen != null ? offsetY(chosen) : 0f;
    }

    float cameraProgress() {
        return headingOut() ? progress() : 1f - progress();
    }

    private boolean headingOut() {
        return phase == Phase.FLYING || phase == Phase.CHOSEN;
    }

    float treeAlpha() {
        return lerp(NodeSearch.DIM_ALPHA, 1f, progress());
    }

    float promptOffsetY() {
        float top = 0f;
        for (Vector2f offset : clusterOffsets.values()) {
            top = Math.min(top, offset.y);
        }
        return top - ROOT_FOOTPRINT / 2f - PROMPT_GAP;
    }

    private float progress() {
        return switch (phase) {
            case CHOOSING -> 0f;
            case CHOSEN -> 1f;
            case FLYING -> eased(flightElapsed / FLIGHT_SECONDS);
            case RETURNING -> 1f - eased(flightElapsed / FLIGHT_SECONDS);
        };
    }

    private static float eased(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
