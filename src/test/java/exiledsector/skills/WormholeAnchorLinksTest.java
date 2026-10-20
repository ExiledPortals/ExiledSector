package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WormholeAnchorLinksTest {

    private static final String ANCHOR = "anchor_1";
    private static final String SECOND_ANCHOR = "anchor_2";
    private static final String VOLUME = "hegemony";

    private final Map<String, String> exitByItemId = new HashMap<>();
    private final Set<String> inertItemIds = new HashSet<>();
    private int lookupRevision;
    private SkillNode root;
    private SkillNode coreNode;
    private SkillNode socket;
    private SkillNode secondSocket;
    private SkillNode exit;
    private SkillNode volumeSmall;
    private SkillNode volumeNotable;
    private SkillTreeTopology topology;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, "core");
        coreNode = node("core_a", SkillTier.SMALL, "core", "root");
        socket = node("socket", SkillTier.SOCKET, "core", "core_a");
        secondSocket = node("socket_b", SkillTier.SOCKET, "core", "core_a");
        exit = node("exit", SkillTier.WORMHOLE_EXIT, VOLUME, "vol_a");
        volumeSmall = node("vol_a", SkillTier.SMALL, VOLUME, "exit");
        volumeNotable = node("vol_b", SkillTier.NOTABLE, VOLUME, "vol_a");
        topology = SkillTreeTopology.of(List.of(root, coreNode, socket, secondSocket, exit, volumeSmall, volumeNotable));
        exitByItemId.put(ANCHOR, "exit");
        exitByItemId.put(SECOND_ANCHOR, "exit");
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                return exitByItemId.get(socketedItemId);
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return !inertItemIds.contains(socketedItemId);
            }

            @Override
            public int revision() {
                return lookupRevision;
            }
        });
    }

    @AfterEach
    void tearDown() {
        AnchorLinks.install(null);
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String region, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    private ShipSkillData shipWithSocket() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(coreNode, 1);
        data.allocate(socket, 1);
        return data;
    }

    private ShipSkillData shipWithOpenVolume() {
        ShipSkillData data = shipWithSocket();
        data.socketItem("socket", ANCHOR);
        data.openAnchorWormholes(100);
        data.allocate(volumeSmall, 1);
        data.allocate(volumeNotable, 1);
        return data;
    }

    private AllocationGate gate(ShipSkillData data, int maxAllocatedNodes) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        return new AllocationGate(data, topology, "root", new AllocationGate.Budget(100, 1, maxAllocatedNodes), eligibility);
    }

    @Test
    void aSocketedAnchorLinksItsSocketAndExitOnlyWhileTheSocketIsAllocated() {
        ShipSkillData data = shipWithSocket();
        data.socketItem("socket", ANCHOR);

        assertEquals("exit", data.wormholePartner(socket));
        assertEquals("socket", data.wormholePartner(exit));
        assertEquals("exit", data.anchorExit("socket"));

        data.deallocate(socket);

        assertNull(data.wormholePartner(socket));
        assertNull(data.wormholePartner(exit));
    }

    @Test
    void openingAllocatesTheExitAtNoCostButNeedsAFreeNodeSlot() {
        ShipSkillData data = shipWithSocket();
        data.socketItem("socket", ANCHOR);
        int paidBefore = data.paidNodeCount();

        assertEquals(List.of(), data.openAnchorWormholes(data.getAllocatedNodeIds().size()));
        assertFalse(data.isAllocated("exit"));

        assertEquals(List.of("exit"), data.openAnchorWormholes(data.getAllocatedNodeIds().size() + 1));
        assertTrue(data.isAllocated("exit"));
        assertTrue(data.isFreeNode("exit"));
        assertEquals(paidBefore, data.paidNodeCount());
    }

    @Test
    void aSecondAnchorForTheSameExitStaysInert() {
        ShipSkillData data = shipWithSocket();
        data.allocate(secondSocket, 1);
        data.socketItem("socket", ANCHOR);
        data.socketItem("socket_b", SECOND_ANCHOR);

        assertEquals("exit", data.anchorExit("socket"));
        assertNull(data.anchorExit("socket_b"));
        assertEquals("socket", data.anchorSocket("exit"));
    }

    @Test
    void anExitIsNeverAllocatedByAPathFromInsideItsVolume() {
        ShipSkillData data = shipWithSocket();
        data.allocate(volumeSmall, 1);

        assertEquals(AllocationGate.Refusal.NOT_CONNECTED, gate(data, 100).allocation(exit).refusal());
        assertFalse(data.reachableAllocatedNodeIds(topology, "root", Set.of()).contains("vol_a"));
    }

    @Test
    void theReachabilitySearchOnlyEntersAnExitThroughItsAnchor() {
        ShipSkillData data = shipWithOpenVolume();

        assertTrue(data.reachableAllocatedNodeIds(topology, "root", Set.of()).containsAll(List.of("exit", "vol_a", "vol_b")));

        data.unsocketItem("socket");

        assertFalse(data.reachableAllocatedNodeIds(topology, "root", Set.of()).contains("exit"));
    }

    @Test
    void removingTheAnchorReleasesEveryNodeReachedThroughItAndRefundsThem() {
        ShipSkillData data = shipWithSocket();
        data.socketItem("socket", ANCHOR);
        data.openAnchorWormholes(100);
        data.addFreeAllocationCredit();
        data.allocate(volumeSmall, 1);
        data.allocate(volumeNotable, 1);
        data.recordItemCharge("vol_b", new SkillItemCost("alpha_core", 1f));
        int paidBefore = data.paidNodeCount();

        data.unsocketItem("socket");
        ReleasedNodes released = data.releaseUnreachable(topology, "root");

        assertEquals(Set.of("exit", "vol_a", "vol_b"), Set.copyOf(released.nodeIds()));
        assertEquals(List.of(new SkillItemCost("alpha_core", 1f)), released.charges());
        assertTrue(data.isAllocated("socket") && data.isAllocated("core_a"));
        assertEquals(1, data.getBankedFreeAllocations());
        assertEquals(paidBefore - 1, data.paidNodeCount());
    }

    @Test
    void deallocatingTheSocketIsAlwaysAllowedAndReleasesTheVolume() {
        ShipSkillData data = shipWithOpenVolume();

        assertTrue(data.canDeallocate(socket, topology, "root"));
        assertTrue(gate(data, 100).deallocation(socket).allowed());

        data.deallocate(socket);
        ReleasedNodes released = data.releaseUnreachable(topology, "root");

        assertFalse(data.isAllocated("exit"));
        assertEquals(Set.of("vol_a", "vol_b"), Set.copyOf(released.nodeIds()));
        assertTrue(data.getSocketedItems().isEmpty());
    }

    @Test
    void deallocatingTheExitClosesTheWormholeAndTakesItsSocketWithIt() {
        ShipSkillData data = shipWithOpenVolume();

        assertTrue(data.canDeallocate(exit, topology, "root"));

        data.deallocate(exit);

        assertFalse(data.isAllocated("socket"));
        assertTrue(data.getSocketedItems().isEmpty());
        assertEquals(Set.of("vol_a", "vol_b"), Set.copyOf(data.releaseUnreachable(topology, "root").nodeIds()));
    }

    @Test
    void anOrdinaryNodeThatWouldStrandTheVolumeIsStillRefused() {
        ShipSkillData data = shipWithOpenVolume();

        assertFalse(data.canDeallocate(coreNode, topology, "root"));
        assertFalse(data.canDeallocate(volumeSmall, topology, "root"));
        assertTrue(data.canDeallocate(volumeNotable, topology, "root"));
    }

    @Test
    void aPreviewNamesWhatClosingWouldReleaseWithoutChangingTheShip() {
        ShipSkillData data = shipWithOpenVolume();

        Set<String> released = data.previewRelease(topology, "root", planned -> planned.unsocketItem("socket"));

        assertEquals(Set.of("exit", "vol_a", "vol_b"), released);
        assertEquals(2, data.paidNodeCount(released));
        assertTrue(data.isAllocated("vol_b"));
        assertEquals(ANCHOR, data.getSocketedItem("socket"));
    }

    @Test
    void thePlanningCopySeesTheSameLinksAndFollowsTheLiveSockets() {
        ShipSkillData data = shipWithOpenVolume();
        ShipSkillData plan = data.planningCopy();

        assertEquals("exit", plan.wormholePartner(socket));

        data.unsocketItem("socket");
        plan.mirrorSocketedItems(data);

        assertNull(plan.wormholePartner(socket));
    }

    @Test
    void theShipsWormholePairsListOnlyOpenAnchorWormholes() {
        ShipSkillData data = shipWithSocket();
        data.socketItem("socket", ANCHOR);

        assertTrue(data.wormholePairs(topology).isEmpty());

        data.openAnchorWormholes(100);

        assertEquals(List.of(new SkillTreeTopology.WormholePair(socket, exit)), data.wormholePairs(topology));
    }

    @Test
    void aRespecOfTheSocketRemovesTheVolumeFirstAndTheExitWithTheSocket() {
        ShipSkillData data = shipWithOpenVolume();

        List<String> planIds = RespecPlan.of(data, topology, "root", socket).stream().map(SkillNode::getId).toList();

        assertEquals(List.of("vol_b", "vol_a", "socket"), planIds);
    }

    @Test
    void anExitThatWentDormantWakesThroughItsAnchor() {
        ShipSkillData data = shipWithOpenVolume();
        Map<String, SkillNode> fullTree = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, coreNode, socket, secondSocket, exit, volumeSmall, volumeNotable)) {
            fullTree.put(node.getId(), node);
        }
        Map<String, SkillNode> coreOnly = new LinkedHashMap<>(fullTree);
        coreOnly.keySet().removeAll(List.of("exit", "vol_a", "vol_b"));

        data.forgetUnknownNodes(coreOnly, fullTree, Map.of());
        assertTrue(data.getDormantNodeIds().containsAll(List.of("exit", "vol_a", "vol_b")));

        data.wakeDormantNodes(fullTree, 100, dormantNode -> true);

        assertTrue(data.isAllocated("exit") && data.isAllocated("vol_a") && data.isAllocated("vol_b"));
    }

    @Test
    void trimmingATreeNeverClosesAWormholeOnItsOwn() {
        ShipSkillData data = shipWithOpenVolume();
        Map<String, SkillNode> tree = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, coreNode, socket, secondSocket, exit, volumeSmall, volumeNotable)) {
            tree.put(node.getId(), node);
        }

        assertEquals(List.of("vol_b", "vol_a"), data.trimPaidNodes(tree, topology));
        assertTrue(data.isAllocated("socket") && data.isAllocated("exit"));
    }

    @Test
    void anInertAnchorStaysLinkedButOnlyOpensOnceItCan() {
        ShipSkillData data = shipWithSocket();
        inertItemIds.add(ANCHOR);
        data.socketItem("socket", ANCHOR);

        assertEquals("exit", data.anchorExit("socket"));
        assertEquals(List.of(), data.openAnchorWormholes(100));

        inertItemIds.clear();

        assertEquals(List.of("exit"), data.openAnchorWormholes(100));
    }

    @Test
    void anOpenWormholeWhoseAnchorReadsInertKeepsItsVolumeThroughOtherChanges() {
        ShipSkillData data = shipWithOpenVolume();
        data.allocate(secondSocket, 1);
        inertItemIds.add(ANCHOR);
        lookupRevision++;

        Set<String> reachableBefore = data.reachableAllocatedNodeIds(topology, "root");
        data.deallocate(secondSocket);
        ReleasedNodes released = data.releaseNewlyStranded(topology, "root", reachableBefore);

        assertTrue(released.isEmpty());
        assertEquals("exit", data.wormholePartner(socket));
        assertTrue(data.isAllocated("exit") && data.isAllocated("vol_a") && data.isAllocated("vol_b"));
        assertEquals(Set.of("vol_b"), data.previewRelease(topology, "root", planned -> planned.deallocate(volumeNotable)));
    }

    @Test
    void aVolumeNoLinkReachesIsLeftAloneByUnrelatedChanges() {
        ShipSkillData data = shipWithOpenVolume();
        data.allocate(secondSocket, 1);
        exitByItemId.clear();
        lookupRevision++;

        Set<String> reachableBefore = data.reachableAllocatedNodeIds(topology, "root");
        data.deallocate(secondSocket);

        assertTrue(data.releaseNewlyStranded(topology, "root", reachableBefore).isEmpty());
        assertTrue(data.isAllocated("vol_a") && data.isAllocated("vol_b"));
    }

    @Test
    void aChangeInTheLookupReopensTheLinkCacheWithoutAnAllocationChange() {
        ShipSkillData data = shipWithSocket();
        exitByItemId.remove(ANCHOR);
        data.socketItem("socket", ANCHOR);

        assertNull(data.anchorExit("socket"));

        exitByItemId.put(ANCHOR, "exit");
        lookupRevision++;

        assertEquals("exit", data.anchorExit("socket"));
    }
}
