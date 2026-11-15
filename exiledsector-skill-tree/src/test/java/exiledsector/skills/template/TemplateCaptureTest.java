package exiledsector.skills.template;

import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.WormholePartners;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static exiledsector.skills.TestTrees.node;
import static exiledsector.skills.TestTrees.small;
import static exiledsector.skills.TestTrees.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class TemplateCaptureTest {

    private SkillNode root;
    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        root = node("root", type("root", SkillTier.ROOT).build());
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
    }

    private static SkillNode register(SkillNode node) {
        SkillTree.register(node);
        return node;
    }

    private List<TemplateStep> capture() {
        return TemplateCapture.capture(data, "root", SkillTree.getAllNodes());
    }

    private static List<String> ids(List<TemplateStep> steps) {
        return steps.stream().map(TemplateStep::nodeId).toList();
    }

    @Test
    void anAlreadyValidOrderIsKeptAndTheStartingRootIsLeftOut() {
        SkillNode a = small("a", "root");
        SkillNode b = small("b", "a");
        ShipTrees.allocate(data, a, 1);
        ShipTrees.allocate(data, b, 1);

        assertEquals(List.of("a", "b"), ids(capture()));
    }

    @Test
    void aChildAllocatedBeforeItsParentIsMovedAfterIt() {
        SkillNode a = small("a", "root");
        SkillNode b = small("b", "a");
        ShipTrees.allocate(data, b, 1);
        ShipTrees.allocate(data, a, 1);

        assertEquals(List.of("a", "b"), ids(capture()));
    }

    @Test
    void independentBranchesKeepTheirOriginalOrder() {
        SkillNode left = small("left", "root");
        SkillNode right = small("right", "root");
        SkillNode rightChild = small("right_child", "right");
        ShipTrees.allocate(data, right, 1);
        ShipTrees.allocate(data, rightChild, 1);
        ShipTrees.allocate(data, left, 1);

        assertEquals(List.of("right", "right_child", "left"), ids(capture()));
    }

    @Test
    void theChosenOptionIsRecordedOnlyForChoiceNodes() {
        SkillType option = type("hull", SkillTier.SMALL).build();
        SkillTree.registerType(option);
        SkillNode slot = register(new SkillNode("slot", type("slot_type", SkillTier.SMALL).choiceOptionIds(List.of("hull")).build(),
                List.of("root"), 0f, 0f));
        SkillNode plain = small("plain", "root");
        ShipTrees.selectOption(data, slot, option, 1);
        ShipTrees.allocate(data, plain, 1);

        assertEquals(List.of(new TemplateStep("slot", "hull"), new TemplateStep("plain", null)), capture());
    }

    @Test
    void aWormholePartnerFollowsTheSideThatWasAllocated() {
        SkillType wormholeType = type("wormhole", SkillTier.WORMHOLE).build();
        SkillNode near = register(new SkillNode("near", wormholeType, List.of("root", "far"), 0f, 0f,
                new SkillNodeDecoration(null, null, null, null, "far")));
        register(new SkillNode("far", wormholeType, List.of("near"), 0f, 0f, new SkillNodeDecoration(null, null, null, null, "near")));
        SkillNode beyond = small("beyond", "far");
        ShipTrees.allocate(data, beyond, 1);
        ShipTrees.allocate(data, near, 1);

        assertEquals(List.of("near", "far", "beyond"), ids(capture()));
    }

    @Test
    void unknownNodesAreDroppedAndUnreachableOnesGoLast() {
        SkillNode a = small("a", "root");
        SkillNode stranded = small("stranded", "missing_parent");
        SkillNode unregistered = new SkillNode("gone", type("gone_type", SkillTier.SMALL).build(), List.of("root"), 0f, 0f);
        ShipTrees.allocate(data, stranded, 1);
        ShipTrees.allocate(data, unregistered, 1);
        ShipTrees.allocate(data, a, 1);

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
            ShipTrees.chooseStartingRoot(shuffledData, SkillTree.get(rootId));
            for (String nodeId : shuffled) {
                ShipTrees.allocate(shuffledData, SkillTree.get(nodeId), 1);
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
        small("a", "root");
        small("b", "a");
        small("c", "root");

        List<TemplateStep> steps = TemplateCapture.capture(List.of("c", "b", "a"), nodeId -> null, WormholePartners::pairedWith, "root",
                SkillTree.getAllNodes());

        assertEquals(List.of("c", "a", "b"), steps.stream().map(TemplateStep::nodeId).toList());
    }
}
