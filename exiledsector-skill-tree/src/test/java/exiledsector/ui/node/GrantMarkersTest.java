package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipFacts;
import exiledsector.items.NodeGrantStatus;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrantMarkersTest {

    private static final String CACHE = "cache";
    private static final String SECOND_CACHE = "second_cache";
    private static final String DOUBLE_CACHE = "double_cache";
    private static final Color CACHE_COLOR = new Color(200, 40, 40);
    private static final Color SECOND_CACHE_COLOR = new Color(40, 40, 200);
    private static final Color DOUBLE_CACHE_COLOR = new Color(40, 200, 40);

    private final Map<String, List<String>> grantedNodesByItemId = new HashMap<>();
    private final Map<String, Color> frameColorByItemId = Map.of(CACHE, CACHE_COLOR, SECOND_CACHE, SECOND_CACHE_COLOR,
            DOUBLE_CACHE, DOUBLE_CACHE_COLOR);
    private SkillNode root;
    private SkillNode coreNode;
    private SkillNode socket;
    private SkillNode secondSocket;
    private SkillNode pathNode;
    private SkillNode notable;
    private SkillNode farNotable;
    private SkillTreeTopology topology;
    private GrantMarkers grantMarkers;

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, "core_a");
        coreNode = node("core_a", SkillTier.SMALL, "root", "socket", "socket_b", "core_b");
        socket = node("socket", SkillTier.SOCKET, "core_a");
        secondSocket = node("socket_b", SkillTier.SOCKET, "core_a");
        pathNode = node("core_b", SkillTier.SMALL, "core_a", "notable");
        notable = node("notable", SkillTier.NOTABLE, "core_b", "beyond");
        SkillNode beyond = node("beyond", SkillTier.SMALL, "notable", "far_notable");
        farNotable = node("far_notable", SkillTier.NOTABLE, "beyond");
        Map<String, SkillNode> nodesById = new LinkedHashMap<>();
        for (SkillNode node : List.of(root, coreNode, socket, secondSocket, pathNode, notable, beyond, farNotable)) {
            nodesById.put(node.getId(), node);
            SkillTree.register(node);
        }
        topology = SkillTreeTopology.of(nodesById.values());
        grantedNodesByItemId.put(CACHE, List.of("notable"));
        grantedNodesByItemId.put(SECOND_CACHE, List.of("far_notable"));
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
        grantMarkers = new GrantMarkers(frameColorByItemId::get);
    }

    @AfterEach
    void tearDown() {
        NodeGrants.install(null);
        SkillTree.clearTypes();
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
    }

    private ShipSkillData shipWithSockets() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, coreNode, 1);
        ShipTrees.allocate(data, socket, 1);
        ShipTrees.allocate(data, secondSocket, 1);
        return data;
    }

    private NodeAllocator.Snapshot snapshotOf(ShipSkillData data) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.of(data,
                new ShipFacts(HullSize.CRUISER, ShieldType.FRONT, false, 0f, hullModId -> false), null);
        AllocationGate gate = new AllocationGate(data, topology, "root", new AllocationGate.Budget(100, 1, 100), eligibility);
        return new NodeAllocator.Snapshot(data, null, 0, Set.of(), gate, null);
    }

    @Test
    void theMapHoldsEveryActiveGrantInItsOwnItemsFrameColour() {
        ShipSkillData data = shipWithSockets();
        data.socketItem("socket", CACHE);
        data.socketItem("socket_b", SECOND_CACHE);
        ShipTrees.allocateGranted(data, notable);
        ShipTrees.allocateGranted(data, farNotable);

        grantMarkers.refresh(snapshotOf(data));

        assertEquals(CACHE_COLOR, grantMarkers.colorFor("notable"));
        assertEquals(SECOND_CACHE_COLOR, grantMarkers.colorFor("far_notable"));
        assertEquals(List.of("socket", "socket_b"), grantMarkers.grantingSocketIds());
        assertEquals(List.of("notable"), grantMarkers.grantedBy("socket"));
        assertEquals(List.of("far_notable"), grantMarkers.grantedBy("socket_b"));
        assertEquals(CACHE_COLOR, grantMarkers.colorForSocket("socket"));
        assertEquals(SECOND_CACHE_COLOR, grantMarkers.colorForSocket("socket_b"));
    }

    @Test
    void aGrantNotYetAllocatedGetsNoMarker() {
        ShipSkillData data = shipWithSockets();
        data.socketItem("socket", CACHE);
        NodeAllocator.Snapshot allocation = snapshotOf(data);

        grantMarkers.refresh(allocation);

        assertEquals(NodeGrantStatus.State.READY, allocation.nodeGrantStatus("socket", "notable").state());
        assertTrue(grantMarkers.isEmpty());
        assertNull(grantMarkers.colorFor("notable"));
        assertEquals(List.of(), grantMarkers.grantingSocketIds());
    }

    @Test
    void aNodeAlsoReachedByPathGetsNoMarker() {
        ShipSkillData data = shipWithSockets();
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, notable, 1);
        data.socketItem("socket", DOUBLE_CACHE);
        ShipTrees.allocateGranted(data, farNotable);
        NodeAllocator.Snapshot allocation = snapshotOf(data);

        grantMarkers.refresh(allocation);

        assertEquals(NodeGrantStatus.State.BY_PATH, allocation.nodeGrantStatus("socket", "notable").state());
        assertNull(grantMarkers.colorFor("notable"));
        assertEquals(DOUBLE_CACHE_COLOR, grantMarkers.colorFor("far_notable"));
        assertEquals(List.of("far_notable"), grantMarkers.grantedBy("socket"));
    }

    @Test
    void aSecondSocketGrantingTheSameNodeLeavesTheMarkerWithTheFirst() {
        ShipSkillData data = shipWithSockets();
        data.socketItem("socket", CACHE);
        data.socketItem("socket_b", DOUBLE_CACHE);
        ShipTrees.allocateGranted(data, notable);
        ShipTrees.allocateGranted(data, farNotable);

        grantMarkers.refresh(snapshotOf(data));

        assertEquals(CACHE_COLOR, grantMarkers.colorFor("notable"));
        assertEquals(DOUBLE_CACHE_COLOR, grantMarkers.colorFor("far_notable"));
        assertEquals(List.of("notable"), grantMarkers.grantedBy("socket"));
        assertEquals(List.of("far_notable"), grantMarkers.grantedBy("socket_b"));
    }

    @Test
    void theMapIsReusedUntilTheTreeChangesAndRebuiltAfterASocketChange() {
        ShipSkillData data = shipWithSockets();
        data.socketItem("socket", CACHE);
        ShipTrees.allocateGranted(data, notable);
        grantMarkers.refresh(snapshotOf(data));
        List<String> firstFrame = grantMarkers.grantingSocketIds();

        grantMarkers.refresh(snapshotOf(data));
        assertSame(firstFrame, grantMarkers.grantingSocketIds());

        data.unsocketItem("socket");
        data.socketItem("socket", SECOND_CACHE);
        ShipTrees.releaseUngrantedFreeNodes(data, topology, "root");
        ShipTrees.allocateGranted(data, farNotable);
        grantMarkers.refresh(snapshotOf(data));

        assertNotSame(firstFrame, grantMarkers.grantingSocketIds());
        assertNull(grantMarkers.colorFor("notable"));
        assertEquals(SECOND_CACHE_COLOR, grantMarkers.colorFor("far_notable"));
        assertEquals(List.of("far_notable"), grantMarkers.grantedBy("socket"));
        assertEquals(SECOND_CACHE_COLOR, grantMarkers.colorForSocket("socket"));
    }

    @Test
    void anotherShipsDataRebuildsTheMap() {
        ShipSkillData granted = shipWithSockets();
        granted.socketItem("socket", CACHE);
        ShipTrees.allocateGranted(granted, notable);
        ShipSkillData plain = shipWithSockets();
        grantMarkers.refresh(snapshotOf(granted));

        grantMarkers.refresh(snapshotOf(plain));

        assertTrue(grantMarkers.isEmpty());
    }
}
