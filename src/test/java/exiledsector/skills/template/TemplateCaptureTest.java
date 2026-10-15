package exiledsector.skills.template;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateCaptureTest {

    private SkillNode root;
    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        RealSkillData.clear();
        root = register(new SkillNode("root", type("root", SkillTier.ROOT).build(), List.of(), 0f, 0f));
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of());
    }

    private static SkillNode register(SkillNode node) {
        SkillTree.register(node);
        return node;
    }

    private static SkillNode node(String id, String... connectedTo) {
        return register(new SkillNode(id, type(id + "_type", SkillTier.SMALL).build(), List.of(connectedTo), 0f, 0f));
    }

    private List<TemplateStep> capture() {
        return TemplateCapture.capture(data, "root", SkillTree.getAllNodes());
    }

    private static List<String> ids(List<TemplateStep> steps) {
        return steps.stream().map(TemplateStep::nodeId).toList();
    }

    @Test
    void anAlreadyValidOrderIsKeptAndTheStartingRootIsLeftOut() {
        SkillNode a = node("a", "root");
        SkillNode b = node("b", "a");
        data.allocate(a, 1);
        data.allocate(b, 1);

        assertEquals(List.of("a", "b"), ids(capture()));
    }

    @Test
    void aChildAllocatedBeforeItsParentIsMovedAfterIt() {
        SkillNode a = node("a", "root");
        SkillNode b = node("b", "a");
        data.allocate(b, 1);
        data.allocate(a, 1);

        assertEquals(List.of("a", "b"), ids(capture()));
    }

    @Test
    void independentBranchesKeepTheirOriginalOrder() {
        SkillNode left = node("left", "root");
        SkillNode right = node("right", "root");
        SkillNode rightChild = node("right_child", "right");
        data.allocate(right, 1);
        data.allocate(rightChild, 1);
        data.allocate(left, 1);

        assertEquals(List.of("right", "right_child", "left"), ids(capture()));
    }

    @Test
    void theChosenOptionIsRecordedOnlyForOptionalNodes() {
        SkillType option = type("hull", SkillTier.SMALL).build();
        SkillTree.registerType(option);
        SkillNode slot = register(new SkillNode("slot", type("slot_type", SkillTier.SMALL).optionalOptionIds(List.of("hull")).build(),
                List.of("root"), 0f, 0f));
        SkillNode plain = node("plain", "root");
        data.selectOption(slot, option, 1);
        data.allocate(plain, 1);

        assertEquals(List.of(new TemplateStep("slot", "hull"), new TemplateStep("plain", null)), capture());
    }

    @Test
    void aWormholePartnerFollowsTheSideThatWasAllocated() {
        SkillType wormholeType = type("wormhole", SkillTier.WORMHOLE).build();
        SkillNode near = register(new SkillNode("near", wormholeType, List.of("root", "far"), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, "far")));
        register(new SkillNode("far", wormholeType, List.of("near"), 0f, 0f, new SkillNodeDecoration(null, null, null, null, "near")));
        SkillNode beyond = node("beyond", "far");
        data.allocate(beyond, 1);
        data.allocate(near, 1);

        assertEquals(List.of("near", "far", "beyond"), ids(capture()));
    }

    @Test
    void unknownNodesAreDroppedAndUnreachableOnesGoLast() {
        SkillNode a = node("a", "root");
        SkillNode stranded = node("stranded", "missing_parent");
        SkillNode unregistered = new SkillNode("gone", type("gone_type", SkillTier.SMALL).build(), List.of("root"), 0f, 0f);
        data.allocate(stranded, 1);
        data.allocate(unregistered, 1);
        data.allocate(a, 1);

        assertEquals(List.of("a", "stranded"), ids(capture()));
    }

    @Test
    void onTheRealTreeAnyAllocationOrderIsCapturedAsAnOrderThatCanBeAllocatedOutwardFromTheRoot() throws Exception {
        RealSkillData.load();
        String rootId = "root_low_tech_1";
        List<String> connected = connectedFrom(rootId, 30);
        for (int seed = 0; seed < 5; seed++) {
            List<String> shuffled = new ArrayList<>(connected);
            Collections.shuffle(shuffled, new Random(seed));
            ShipSkillData shuffledData = new ShipSkillData();
            shuffledData.chooseStartingRoot(SkillTree.get(rootId));
            for (String nodeId : shuffled) {
                shuffledData.allocate(SkillTree.get(nodeId), 1);
            }

            List<TemplateStep> steps = TemplateCapture.capture(shuffledData, rootId, SkillTree.getAllNodes());

            Set<String> placed = new HashSet<>(Set.of(rootId));
            for (TemplateStep step : steps) {
                SkillNode node = SkillTree.get(step.nodeId());
                boolean reachable = node.getConnectedNodeIds().isEmpty()
                        || node.getConnectedNodeIds().stream().anyMatch(placed::contains)
                        || placed.contains(node.getPairedNodeId());
                assertTrue(reachable, step.nodeId() + " came before anything it connects to (seed " + seed + ")");
                placed.add(step.nodeId());
            }
            assertEquals(new HashSet<>(shuffledData.getAllocatedNodeIds()).size() - 1, steps.size());
        }
    }

    private static List<String> connectedFrom(String rootId, int count) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(rootId));
        seen.add(rootId);
        while (!queue.isEmpty() && seen.size() <= count) {
            String current = queue.poll();
            for (SkillNode candidate : SkillTree.getAllNodes().values()) {
                if (candidate.getConnectedNodeIds().contains(current) && candidate.getType().getTier() != SkillTier.ROOT && candidate.getType().getTier() != SkillTier.WORMHOLE
                        && seen.add(candidate.getId())) {
                    queue.add(candidate.getId());
                }
            }
        }
        seen.remove(rootId);
        return new ArrayList<>(seen);
    }

    @Test
    void aGivenOrderIsKeptWhereverEachNodeIsAlreadyReachable() {
        node("a", "root");
        node("b", "a");
        node("c", "root");

        List<TemplateStep> steps = TemplateCapture.capture(List.of("c", "b", "a"), nodeId -> null, "root", SkillTree.getAllNodes());

        assertEquals(List.of("c", "a", "b"), steps.stream().map(TemplateStep::nodeId).toList());
    }
}
