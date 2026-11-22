package exiledsector.harness;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.SkillTreeCanvasPlugin;
import exiledsector.ui.node.TreeAllocationSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BuildTreeStep implements HarnessStep {

    static final int CANDIDATES_TRIED = 12;
    static final float TREE_TIMEOUT_SECONDS = 60f;

    private final String rootTypeId;
    private final List<String> goalTypeIds;
    private final int nodeLimit;
    private int goalIndex;
    private boolean allocatedThisPass;
    private boolean filling;
    private int filledNodes;

    BuildTreeStep(String rootTypeId, List<String> goalTypeIds, int nodeLimit) {
        if (goalTypeIds.isEmpty()) {
            throw new IllegalArgumentException("buildTree needs at least one goal node type");
        }
        if (nodeLimit < 0) {
            throw new IllegalArgumentException("buildTree needs a node limit of 0 (none) or more, not " + nodeLimit);
        }
        this.rootTypeId = rootTypeId;
        this.goalTypeIds = List.copyOf(goalTypeIds);
        this.nodeLimit = nodeLimit;
    }

    @Override
    public String name() {
        return "buildTree from " + rootTypeId + " toward " + goalTypeIds.size() + " goals";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof SkillTreeCanvasPlugin canvas)) {
            return StepOutcome.failed("buildTree needs an open skill tree; run openSkillTree first");
        }
        TreeAllocationSession session = canvas.treeSession();
        if (session.getStartingRoot() == null) {
            return chooseRoot(session);
        }
        if (session.isBusy()) {
            session.hurryStartingRootFlight();
            return new StepOutcome(false, false, "waiting for the starting root to settle");
        }
        if (!session.getStartingRoot().getType().getId().equals(rootTypeId)) {
            return StepOutcome.failed("the ship already grows from " + session.getStartingRoot().getType().getId() + ", not " + rootTypeId);
        }
        if (nodeLimit > 0 && session.allocatedNodeCount() >= nodeLimit) {
            return StepOutcome.passed(summary(session));
        }
        if (session.hasPointsLeft() && !filling) {
            filling = passFinishedWithoutProgress();
            if (!filling) {
                allocateGoal(session, goalTypeIds.get(goalIndex % goalTypeIds.size()));
                goalIndex++;
                return new StepOutcome(false, false, "allocating toward " + goalTypeIds.size() + " goals");
            }
        }
        if (session.hasPointsLeft() && fillNearest(session)) {
            filledNodes++;
            return new StepOutcome(false, false, "spending the points left on nearby nodes");
        }
        return StepOutcome.passed(summary(session));
    }

    private String summary(TreeAllocationSession session) {
        Map<String, Integer> allocatedByGoal = new LinkedHashMap<>();
        Set<String> goals = new LinkedHashSet<>(goalTypeIds);
        for (SkillNode node : SkillTree.topology().nodes()) {
            if (session.isAllocated(node) && goals.contains(node.getType().getId())) {
                allocatedByGoal.merge(node.getType().getId(), 1, Integer::sum);
            }
        }
        List<String> unreachedGoals = goals.stream().filter(goal -> !allocatedByGoal.containsKey(goal)).toList();
        return session.allocatedNodeCount() + " nodes allocated, goals reached " + allocatedByGoal
                + (unreachedGoals.isEmpty() ? "" : ", out of reach " + unreachedGoals)
                + (filledNodes == 0 ? "" : ", " + filledNodes + " nearby nodes filled the rest")
                + (session.hasPointsLeft() ? ", points left unspent" : "");
    }

    private StepOutcome chooseRoot(TreeAllocationSession session) {
        if (!session.isChoosingStartingRoot()) {
            return new StepOutcome(false, false, "waiting for the starting root choice");
        }
        SkillNode root = SkillTree.topology().roots().stream()
                .filter(candidate -> candidate.getType().getId().equals(rootTypeId))
                .findFirst().orElse(null);
        if (root == null) {
            return StepOutcome.failed("no starting root of type " + rootTypeId);
        }
        session.chooseStartingRoot(root);
        return new StepOutcome(false, false, "chose " + root.getId());
    }

    private boolean passFinishedWithoutProgress() {
        if (goalIndex == 0 || goalIndex % goalTypeIds.size() != 0) {
            return false;
        }
        if (!allocatedThisPass) {
            return true;
        }
        allocatedThisPass = false;
        return false;
    }

    private void allocateGoal(TreeAllocationSession session, String goalTypeId) {
        SkillTreeTopology topology = SkillTree.topology();
        Map<String, Integer> distances = distancesFromAllocated(session, topology);
        List<SkillNode> candidates = topology.nodes().stream()
                .filter(node -> node.getType().getId().equals(goalTypeId) && !session.isAllocated(node) && !session.isHidden(node)
                        && distances.containsKey(node.getId()))
                .sorted(Comparator.comparingInt((SkillNode node) -> distances.get(node.getId())).thenComparing(SkillNode::getId))
                .limit(CANDIDATES_TRIED)
                .toList();
        for (SkillNode candidate : candidates) {
            int allocatedBefore = session.allocatedNodeCount();
            session.allocatePath(candidate);
            if (session.allocatedNodeCount() > allocatedBefore) {
                allocatedThisPass = true;
                return;
            }
        }
    }

    private boolean fillNearest(TreeAllocationSession session) {
        SkillTreeTopology topology = SkillTree.topology();
        Map<String, Integer> distances = distancesFromAllocated(session, topology);
        Set<String> goals = Set.copyOf(goalTypeIds);
        List<SkillNode> candidates = topology.nodes().stream()
                .filter(node -> node.getTier() == SkillTier.SMALL && !session.isAllocated(node) && !session.isHidden(node)
                        && Integer.valueOf(1).equals(distances.get(node.getId())))
                .sorted(Comparator.comparing((SkillNode node) -> !goals.contains(node.getType().getId())).thenComparing(SkillNode::getId))
                .toList();
        for (SkillNode candidate : candidates) {
            int allocatedBefore = session.allocatedNodeCount();
            session.allocatePath(candidate);
            if (session.allocatedNodeCount() > allocatedBefore) {
                return true;
            }
        }
        return false;
    }

    static Map<String, Integer> distancesFromAllocated(TreeAllocationSession session, SkillTreeTopology topology) {
        Map<String, Integer> distances = new HashMap<>();
        Deque<SkillNode> frontier = new ArrayDeque<>();
        for (SkillNode node : topology.nodes()) {
            if (session.isAllocated(node) || node == session.getStartingRoot()) {
                distances.put(node.getId(), 0);
                frontier.add(node);
            }
        }
        while (!frontier.isEmpty()) {
            SkillNode node = frontier.poll();
            int nextDistance = distances.get(node.getId()) + 1;
            List<SkillNode> neighbours = new ArrayList<>(topology.dependents(node.getId()));
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode connected = topology.node(connectedId);
                if (connected != null) {
                    neighbours.add(connected);
                }
            }
            for (SkillNode neighbour : neighbours) {
                if (!distances.containsKey(neighbour.getId()) && !session.isHidden(neighbour)) {
                    distances.put(neighbour.getId(), nextDistance);
                    frontier.add(neighbour);
                }
            }
        }
        return distances;
    }

    @Override
    public float timeoutSeconds() {
        return TREE_TIMEOUT_SECONDS;
    }
}
