package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.template.TemplateCapture;
import exiledsector.skills.template.TemplateStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumeGrantTest {

    private static final String VOLUME_CHIP = "volume_chip";
    private static final String SECOND_CHIP = "second_chip";

    private final Map<String, String> grantedNodeByItemId = new HashMap<>();
    private SkillNode root;
    private SkillNode coreNode;
    private SkillNode socket;
    private SkillNode secondSocket;
    private SkillNode pathNode;
    private SkillNode notable;
    private SkillNode beyond;
    private Map<String, SkillNode> nodesById;
    private SkillTreeTopology topology;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, "core");
        coreNode = node("core_a", SkillTier.SMALL, "core", "root", "socket", "socket_b", "core_b");
        socket = node("socket", SkillTier.SOCKET, "core", "core_a");
        secondSocket = node("socket_b", SkillTier.SOCKET, "core", "core_a");
        pathNode = node("core_b", SkillTier.SMALL, "core", "core_a", "notable");
        notable = node("notable", SkillTier.NOTABLE, "hegemony", "core_b", "beyond");
        beyond = node("beyond", SkillTier.SMALL, "hegemony", "notable");
        nodesById = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, coreNode, socket, secondSocket, pathNode, notable, beyond)) {
            nodesById.put(node.getId(), node);
            SkillTree.register(node);
        }
        topology = SkillTreeTopology.of(nodesById.values());
        grantedNodeByItemId.put(VOLUME_CHIP, "notable");
        grantedNodeByItemId.put(SECOND_CHIP, "notable");
        NodeGrants.install(new NodeGrants.GrantLookup() {
            @Override
            public String grantedNodeId(String socketedItemId) {
                return grantedNodeByItemId.get(socketedItemId);
            }

            @Override
            public int revision() {
                return 0;
            }
        });
    }

    @AfterEach
    void tearDown() {
        NodeGrants.install(null);
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String region, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    private ShipSkillData shipWithGrantedNotable() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(coreNode, 1);
        data.allocate(socket, 1);
        data.socketItem("socket", VOLUME_CHIP);
        data.allocate(notable, 1);
        return data;
    }

    private AllocationGate gate(ShipSkillData data, int totalOp, int maxAllocatedNodes) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        return new AllocationGate(data, topology, "root", new AllocationGate.Budget(totalOp, 1, maxAllocatedNodes), eligibility);
    }

    @Test
    void aGrantedNotableIsReachableWithoutAPathButDoesNotConnectTheNodesBeyondIt() {
        ShipSkillData data = shipWithGrantedNotable();

        assertEquals(Map.of("socket", "notable"), data.grantedNodesBySocket());
        assertTrue(data.reachableAllocatedNodeIds(topology, "root").contains("notable"));
        assertFalse(data.pathReachableAllocatedNodeIds(topology, "root").contains("notable"));
        assertEquals(Set.of("notable"), data.grantOnlyNodeIds(topology, "root"));
        AllocationGate gate = gate(data, 100, 100);
        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, gate.allocation(beyond).refusal());
        assertTrue(gate.allocation(pathNode).allowed());
    }

    @Test
    void theGrantedNotableCannotBeUnallocatedOnItsOwnButItsSocketCan() {
        ShipSkillData data = shipWithGrantedNotable();
        AllocationGate gate = gate(data, 100, 100);

        assertEquals(AllocationGate.Refusal.HELD_BY_SUBROUTINE, gate.deallocation(notable).refusal());
        assertTrue(gate.deallocation(socket).allowed());
        assertTrue(data.canDeallocate(socket, topology, "root"));
    }

    @Test
    void removingTheSubroutineReleasesOnlyTheGrantedNotableAndBanksItsFreeAllocation() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(coreNode, 1);
        data.allocate(socket, 1);
        data.socketItem("socket", VOLUME_CHIP);
        data.addFreeAllocationCredit();
        data.allocate(notable, 1);
        assertTrue(data.isFreeNode("notable"));
        assertEquals(0, data.getBankedFreeAllocations());

        assertEquals(Set.of("notable"), data.previewRelease(topology, "root", planned -> planned.unsocketItem("socket")));
        Set<String> reachableBefore = data.reachableAllocatedNodeIds(topology, "root");
        data.unsocketItem("socket");
        ReleasedNodes released = data.releaseNewlyStranded(topology, "root", reachableBefore);

        assertEquals(List.of("notable"), released.nodeIds());
        assertEquals(Set.of("root", "core_a", "socket"), data.getAllocatedNodeIds());
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void unallocatingTheSocketReleasesTheGrantedNotableToo() {
        ShipSkillData data = shipWithGrantedNotable();

        assertEquals(Set.of("socket", "notable"), data.previewRelease(topology, "root", planned -> planned.deallocate(socket)));
    }

    @Test
    void aPathBuiltToTheGrantedNotableTakesItOverSoRemovingTheSubroutineReleasesNothing() {
        ShipSkillData data = shipWithGrantedNotable();
        data.allocate(pathNode, 1);

        assertEquals(Set.of(), data.grantOnlyNodeIds(topology, "root"));
        assertTrue(gate(data, 100, 100).allocation(beyond).allowed());
        assertTrue(gate(data, 100, 100).deallocation(notable).allowed());
        data.allocate(beyond, 1);

        assertEquals(Set.of(), data.previewRelease(topology, "root", planned -> planned.unsocketItem("socket")));
        assertFalse(data.canDeallocate(pathNode, topology, "root"));
    }

    @Test
    void aSecondSubroutineForTheSameNotableKeepsItWhenTheFirstIsRemoved() {
        ShipSkillData data = shipWithGrantedNotable();
        data.allocate(secondSocket, 1);
        data.socketItem("socket_b", SECOND_CHIP);

        assertEquals("socket", data.grantingSocket("notable"));
        assertEquals(Set.of(), data.previewRelease(topology, "root", planned -> planned.unsocketItem("socket")));
    }

    @Test
    void theGrantVerdictNeedsASlotAndOpButNoPath() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(coreNode, 1);
        data.allocate(socket, 1);
        data.socketItem("socket", VOLUME_CHIP);
        int allocatedCount = data.getAllocatedNodeIds().size();

        assertTrue(gate(data, 100, allocatedCount + 1).grant(notable).allowed());
        assertEquals(AllocationGate.Refusal.NODE_CAP, gate(data, 100, allocatedCount).grant(notable).refusal());
        assertEquals(AllocationGate.Refusal.OUT_OF_OP, gate(data, data.getSpentOp(1), allocatedCount + 1).grant(notable).refusal());
        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, gate(data, 100, allocatedCount + 1).allocation(notable).refusal());
    }

    @Test
    void aRespecOfTheSocketRemovesTheGrantedNotableAfterTheSocket() {
        ShipSkillData data = shipWithGrantedNotable();

        assertEquals(List.of(socket, notable), RespecPlan.of(data, topology, "root", socket));
        List<SkillNode> rootPlan = RespecPlan.of(data, topology, "root", root);
        assertEquals(root, rootPlan.get(rootPlan.size() - 1));
        assertTrue(rootPlan.indexOf(notable) < rootPlan.indexOf(root));
    }

    @Test
    void sweepsThatReleaseUnreachableNodesKeepTheGrantedNotable() {
        ShipSkillData data = shipWithGrantedNotable();

        assertTrue(data.releaseUnreachable(topology, "root").isEmpty());
        assertTrue(data.isAllocated("notable"));
    }

    @Test
    void aDormantGrantedNotableWakesThroughItsGrant() {
        ShipSkillData data = shipWithGrantedNotable();
        Map<String, SkillNode> withoutVolume = new LinkedHashMap<>(nodesById);
        withoutVolume.remove("notable");
        withoutVolume.remove("beyond");
        Map<String, SkillType> types = new HashMap<>();
        nodesById.values().forEach(node -> types.put(node.getType().getId(), node.getType()));

        data.forgetUnknownNodes(withoutVolume, nodesById, types);
        assertTrue(data.getDormantNodeIds().contains("notable"));

        assertEquals(List.of(), data.wakeDormantNodes(nodesById, 100, dormant -> true));
        assertTrue(data.isAllocated("notable"));
    }

    @Test
    void aNotableWokenThroughItsGrantDoesNotReconnectItsDormantNeighbours() {
        ShipSkillData data = shipWithGrantedNotable();
        data.allocate(beyond, 1);
        Map<String, SkillNode> withoutVolume = new LinkedHashMap<>(nodesById);
        withoutVolume.remove("notable");
        withoutVolume.remove("beyond");
        Map<String, SkillType> types = new HashMap<>();
        nodesById.values().forEach(node -> types.put(node.getType().getId(), node.getType()));
        data.forgetUnknownNodes(withoutVolume, nodesById, types);
        assertEquals(Set.of("notable", "beyond"), data.getDormantNodeIds());

        assertEquals(List.of("beyond"), data.wakeDormantNodes(nodesById, 100, dormant -> true));

        assertTrue(data.isAllocated("notable"));
        assertFalse(data.isAllocated("beyond"));
        assertEquals(Set.of(), data.getDormantNodeIds());
    }

    @Test
    void aDormantNeighbourOfAPathAllocatedNotableStillWakes() {
        ShipSkillData data = shipWithGrantedNotable();
        data.allocate(pathNode, 1);
        data.allocate(beyond, 1);
        Map<String, SkillNode> withoutBeyond = new LinkedHashMap<>(nodesById);
        withoutBeyond.remove("beyond");
        Map<String, SkillType> types = new HashMap<>();
        nodesById.values().forEach(node -> types.put(node.getType().getId(), node.getType()));
        data.forgetUnknownNodes(withoutBeyond, nodesById, types);

        assertEquals(List.of(), data.wakeDormantNodes(nodesById, 100, dormant -> true));
        assertTrue(data.isAllocated("beyond"));
    }

    @Test
    void aTemplateCaptureLeavesTheGrantedNotableOut() {
        ShipSkillData data = shipWithGrantedNotable();

        List<String> capturedIds = TemplateCapture.capture(data, "root", nodesById).stream().map(TemplateStep::nodeId).toList();

        assertEquals(List.of("core_a", "socket"), capturedIds);
    }
}
