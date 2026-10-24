package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.skills.ShipFacts;
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

class NodeGrantTest {

    private static final String CACHE = "cache";
    private static final String SECOND_CACHE = "second_cache";
    private static final String DOUBLE_CACHE = "double_cache";

    private final Map<String, List<String>> grantedNodesByItemId = new HashMap<>();
    private SkillNode root;
    private SkillNode coreNode;
    private SkillNode socket;
    private SkillNode secondSocket;
    private SkillNode pathNode;
    private SkillNode notable;
    private SkillNode beyond;
    private SkillNode farNotable;
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
        farNotable = node("far_notable", SkillTier.NOTABLE, "abyss", "beyond");
        nodesById = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, coreNode, socket, secondSocket, pathNode, notable, beyond, farNotable)) {
            nodesById.put(node.getId(), node);
            SkillTree.register(node);
        }
        topology = SkillTreeTopology.of(nodesById.values());
        grantedNodesByItemId.put(CACHE, List.of("notable"));
        grantedNodesByItemId.put(SECOND_CACHE, List.of("notable"));
        grantedNodesByItemId.put(DOUBLE_CACHE, List.of("notable", "far_notable"));
        NodeGrants.install(new NodeGrants.GrantLookup() {
            @Override
            public List<String> grantedNodeIds(String socketedItemId) {
                return grantedNodesByItemId.getOrDefault(socketedItemId, List.of());
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

    private ShipSkillData shipWithSocketed(String itemId) {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, coreNode, 1);
        ShipTrees.allocate(data, socket, 1);
        data.socketItem("socket", itemId);
        return data;
    }

    private ShipSkillData shipWithGrantedNotable() {
        ShipSkillData data = shipWithSocketed(CACHE);
        ShipTrees.allocateGranted(data, notable);
        return data;
    }

    private AllocationGate gate(ShipSkillData data, int totalOp, int maxAllocatedNodes) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        return new AllocationGate(data, topology, "root", new AllocationGate.Budget(totalOp, 1, maxAllocatedNodes), eligibility);
    }

    private Map<String, SkillType> types() {
        Map<String, SkillType> types = new HashMap<>();
        nodesById.values().forEach(node -> types.put(node.getType().getId(), node.getType()));
        return types;
    }

    @Test
    void aGrantedNotableIsReachableWithoutAPathButDoesNotConnectTheNodesBeyondIt() {
        ShipSkillData data = shipWithGrantedNotable();

        assertEquals(Map.of("socket", List.of("notable")), ShipTreeLinks.grantedNodesBySocket(data));
        assertTrue(ShipTrees.reachableAllocatedNodeIds(data, topology, "root").contains("notable"));
        assertFalse(ShipTrees.pathReachableAllocatedNodeIds(data, topology, "root").contains("notable"));
        assertEquals(Set.of("notable"), ShipTrees.grantOnlyNodeIds(data, topology, "root"));
        AllocationGate gate = gate(data, 100, 100);
        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, gate.allocation(beyond).refusal());
        assertTrue(gate.allocation(pathNode).allowed());
    }

    @Test
    void aGrantedNodeCostsNoOpNoNodeSlotAndNoFreeAllocation() {
        ShipSkillData data = shipWithSocketed(CACHE);
        data.addFreeAllocationCredit();
        int spentBefore = ShipTrees.getSpentOp(data, 1);
        int slotsBefore = data.usedNodeSlots();

        ShipTrees.allocateGranted(data, notable);

        assertTrue(data.isAllocated("notable"));
        assertTrue(data.isGrantFree("notable"));
        assertTrue(data.isFreeNode("notable"));
        assertEquals(spentBefore, ShipTrees.getSpentOp(data, 1));
        assertEquals(slotsBefore, data.usedNodeSlots());
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(AllocationGate.Refusal.NODE_CAP, gate(data, 100, slotsBefore).allocation(pathNode).refusal());
        assertTrue(gate(data, 100, slotsBefore + 1).allocation(pathNode).allowed());
    }

    @Test
    void theGrantVerdictNeedsNoSlotNoOpAndNoPath() {
        ShipSkillData data = shipWithSocketed(CACHE);
        int slots = data.usedNodeSlots();

        assertTrue(gate(data, 0, slots).grant(notable).allowed());
        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, gate(data, 100, slots + 1).allocation(notable).refusal());
        ShipTrees.allocateGranted(data, notable);
        assertEquals(AllocationGate.Refusal.ALREADY_ALLOCATED, gate(data, 100, 100).grant(notable).refusal());
    }

    @Test
    void aCacheGrantsEveryNodeItRolled() {
        ShipSkillData data = shipWithSocketed(DOUBLE_CACHE);
        ShipTrees.allocateGranted(data, notable);
        ShipTrees.allocateGranted(data, farNotable);

        assertEquals(Map.of("socket", List.of("notable", "far_notable")), ShipTreeLinks.grantedNodesBySocket(data));
        assertEquals("socket", ShipTreeLinks.grantingSocket(data, "far_notable"));
        assertEquals(Set.of("notable", "far_notable"), ShipTrees.grantOnlyNodeIds(data, topology, "root"));
        assertEquals(Set.of("notable", "far_notable"), ShipTrees.previewRelease(data, topology, "root", planned -> planned.unsocketItem("socket")));
    }

    @Test
    void theGrantedNotableCannotBeUnallocatedOnItsOwnButItsSocketCan() {
        ShipSkillData data = shipWithGrantedNotable();
        AllocationGate gate = gate(data, 100, 100);

        assertEquals(AllocationGate.Refusal.HELD_BY_SUBROUTINE, gate.deallocation(notable).refusal());
        assertTrue(gate.deallocation(socket).allowed());
        assertTrue(ShipTrees.canDeallocate(data, socket, topology, "root"));
    }

    @Test
    void removingTheSubroutineReleasesTheGrantedNotableWithoutTouchingBankedAllocations() {
        ShipSkillData data = shipWithGrantedNotable();
        data.addFreeAllocationCredit();

        assertEquals(Set.of("notable"), ShipTrees.previewRelease(data, topology, "root", planned -> planned.unsocketItem("socket")));
        Set<String> reachableBefore = ShipTrees.reachableAllocatedNodeIds(data, topology, "root");
        data.unsocketItem("socket");
        ReleasedNodes released = ShipTrees.releaseNewlyStranded(data, topology, "root", reachableBefore);

        assertEquals(List.of("notable"), released.nodeIds());
        assertEquals(Set.of("root", "core_a", "socket"), data.getAllocatedNodeIds());
        assertFalse(data.isGrantFree("notable"));
        assertEquals(1, data.getBankedFreeAllocations());
    }

    @Test
    void aGrantedNodeLeftWithoutItsGrantIsReleasedByTheRefreshSweep() {
        ShipSkillData data = shipWithGrantedNotable();
        data.unsocketItem("socket");

        ReleasedNodes released = ShipTrees.releaseUngrantedFreeNodes(data, topology, "root");

        assertEquals(List.of("notable"), released.nodeIds());
        assertTrue(ShipTrees.releaseUngrantedFreeNodes(data, topology, "root").nodeIds().isEmpty());
    }

    @Test
    void unallocatingTheSocketReleasesTheGrantedNotableToo() {
        ShipSkillData data = shipWithGrantedNotable();

        assertEquals(Set.of("socket", "notable"), ShipTrees.previewRelease(data, topology, "root", planned -> ShipTrees.deallocate(planned, socket)));
    }

    @Test
    void aPathBuiltToTheGrantedNotableKeepsItFreeUntilTheSubroutineIsRemoved() {
        ShipSkillData data = shipWithGrantedNotable();
        ShipTrees.allocate(data, pathNode, 1);

        assertEquals(Set.of(), ShipTrees.grantOnlyNodeIds(data, topology, "root"));
        assertTrue(gate(data, 100, 100).allocation(beyond).allowed());
        ShipTrees.allocate(data, beyond, 1);
        assertTrue(data.isGrantFree("notable"));

        assertEquals(Set.of("notable", "beyond"), ShipTrees.previewRelease(data, topology, "root", planned -> planned.unsocketItem("socket")));
        assertFalse(ShipTrees.canDeallocate(data, pathNode, topology, "root"));
    }

    @Test
    void aNotableAllocatedByPathBeforeSocketingIsNotFreeAndStaysWhenTheSubroutineIsRemoved() {
        ShipSkillData data = shipWithSocketed(CACHE);
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, notable, 1);

        assertFalse(data.isGrantFree("notable"));
        assertEquals(Set.of(), ShipTrees.previewRelease(data, topology, "root", planned -> planned.unsocketItem("socket")));
    }

    @Test
    void aSecondSubroutineForTheSameNotableKeepsItWhenTheFirstIsRemoved() {
        ShipSkillData data = shipWithGrantedNotable();
        ShipTrees.allocate(data, secondSocket, 1);
        data.socketItem("socket_b", SECOND_CACHE);

        assertEquals("socket", ShipTreeLinks.grantingSocket(data, "notable"));
        assertEquals(Set.of(), ShipTrees.previewRelease(data, topology, "root", planned -> planned.unsocketItem("socket")));
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

        assertTrue(ShipTrees.releaseUnreachable(data, topology, "root").isEmpty());
        assertTrue(data.isAllocated("notable"));
    }

    @Test
    void aDormantGrantedNotableWakesThroughItsGrantWithoutANodeSlot() {
        ShipSkillData data = shipWithGrantedNotable();
        int slots = data.usedNodeSlots();
        Map<String, SkillNode> withoutVolume = new LinkedHashMap<>(nodesById);
        withoutVolume.remove("notable");
        withoutVolume.remove("beyond");

        ShipTrees.forgetUnknownNodes(data, withoutVolume, nodesById, types());
        assertTrue(data.getDormantNodeIds().contains("notable"));

        assertEquals(List.of(), DormantNodes.wakeDormantNodes(data, nodesById, slots, dormant -> true));
        assertTrue(data.isAllocated("notable"));
        assertTrue(data.isGrantFree("notable"));
    }

    @Test
    void aDormantSocketKeepsItsGrantedNodeAndThePaidNodesBeyondIt() {
        ShipSkillData data = shipWithGrantedNotable();
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, beyond, 1);
        Map<String, SkillNode> withoutSocket = new LinkedHashMap<>(nodesById);
        withoutSocket.remove("socket");
        ShipTrees.forgetUnknownNodes(data, withoutSocket, nodesById, types());
        assertTrue(data.getDormantNodeIds().contains("socket"));

        assertTrue(ShipTrees.releaseUngrantedFreeNodes(data, topology, "root").nodeIds().isEmpty());
        assertTrue(data.isAllocated("notable"));
        assertTrue(data.isAllocated("beyond"));
        assertTrue(data.isGrantFree("notable"));
    }

    @Test
    void aDormantFreeNodeWhoseGrantIsGoneIsReleasedNotWokenByPath() {
        ShipSkillData data = shipWithGrantedNotable();
        ShipTrees.allocate(data, pathNode, 1);
        Map<String, SkillNode> withoutVolume = new LinkedHashMap<>(nodesById);
        withoutVolume.remove("notable");
        withoutVolume.remove("beyond");
        ShipTrees.forgetUnknownNodes(data, withoutVolume, nodesById, types());
        data.unsocketItem("socket");

        assertEquals(List.of("notable"), DormantNodes.wakeDormantNodes(data, nodesById, 100, dormant -> true));
        assertFalse(data.isAllocated("notable"));
        assertFalse(data.isGrantFree("notable"));
    }

    @Test
    void aNotableWokenThroughItsGrantDoesNotReconnectItsDormantNeighbours() {
        ShipSkillData data = shipWithSocketed(CACHE);
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, notable, 1);
        ShipTrees.allocate(data, beyond, 1);
        ShipTrees.deallocate(data, pathNode);
        Map<String, SkillNode> withoutVolume = new LinkedHashMap<>(nodesById);
        withoutVolume.remove("notable");
        withoutVolume.remove("beyond");
        ShipTrees.forgetUnknownNodes(data, withoutVolume, nodesById, types());
        assertEquals(Set.of("notable", "beyond"), data.getDormantNodeIds());

        assertEquals(List.of("beyond"), DormantNodes.wakeDormantNodes(data, nodesById, 100, dormant -> true));

        assertTrue(data.isAllocated("notable"));
        assertFalse(data.isAllocated("beyond"));
        assertEquals(Set.of(), data.getDormantNodeIds());
    }

    @Test
    void aDormantNeighbourOfAPathAllocatedNotableStillWakes() {
        ShipSkillData data = shipWithGrantedNotable();
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, beyond, 1);
        Map<String, SkillNode> withoutBeyond = new LinkedHashMap<>(nodesById);
        withoutBeyond.remove("beyond");
        ShipTrees.forgetUnknownNodes(data, withoutBeyond, nodesById, types());

        assertEquals(List.of(), DormantNodes.wakeDormantNodes(data, nodesById, 100, dormant -> true));
        assertTrue(data.isAllocated("beyond"));
    }

    @Test
    void aTemplateCaptureLeavesTheGrantedNotableOut() {
        ShipSkillData data = shipWithGrantedNotable();

        List<String> capturedIds = TemplateCapture.capture(data, "root", nodesById).stream().map(TemplateStep::nodeId).toList();

        assertEquals(List.of("core_a", "socket"), capturedIds);
    }
}
