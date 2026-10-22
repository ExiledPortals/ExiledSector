package exiledsector.items;

import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeGrantCustodyTest {

    private static final String CACHE_ID = "item_cache";

    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private SkillNode root;
    private SkillNode socket;
    private SkillNode notable;
    private SkillNode pathNode;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        root = node("root", SkillTier.ROOT, "socket", "path");
        socket = node("socket", SkillTier.SOCKET, "root");
        pathNode = node("path", SkillTier.SMALL, "root", "notable");
        notable = node("notable", SkillTier.NOTABLE, "path");
        for (SkillNode node : List.of(root, socket, pathNode, notable)) {
            SkillTree.register(node);
        }
        NodeGrants.install(new NodeGrants.GrantLookup() {
            @Override
            public List<String> grantedNodeIds(String socketedItemId) {
                return CACHE_ID.equals(socketedItemId) ? List.of("notable") : List.of();
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
        SkillTree.clearNodes();
    }

    private static SkillNode node(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).build();
        return new SkillNode(id, type, List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
    }

    private ShipSkillData shipWithGrant() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, socket, 1);
        data.socketItem("socket", CACHE_ID);
        ShipTrees.allocateGranted(data, notable);
        ships.put("ship-a", data);
        return data;
    }

    @Test
    void aShipLeavingCustodyLosesItsSubroutineAndTheNotableItGranted() {
        ShipSkillData data = shipWithGrant();
        List<CargoRefund> refunds = new ArrayList<>();

        SocketCustody.reconcile(ships, Set.of(), new HashSet<>(), new ItemStore(), refunds::add);

        assertNull(data.getSocketedItem("socket"));
        assertFalse(data.isAllocated("notable"));
        assertTrue(data.isAllocated("socket"));
        assertEquals(List.of(), refunds);
    }

    @Test
    void aShipDestroyedInBattleRefundsNothingForItsGrantedNotable() {
        ShipSkillData data = shipWithGrant();
        List<CargoRefund> refunds = new ArrayList<>();

        Set<String> lostForGood = SocketCustody.reconcile(ships, Set.of(), new HashSet<>(Set.of("ship-a")), new ItemStore(), refunds::add);

        assertEquals(Set.of("ship-a"), lostForGood);
        assertNull(data.getSocketedItem("socket"));
        assertEquals(List.of(), refunds);
    }

    @Test
    void aNotablePaidForByPathStaysWhenTheSubroutineLeaves() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, socket, 1);
        ShipTrees.allocate(data, pathNode, 1);
        ShipTrees.allocate(data, notable, 1);
        data.socketItem("socket", CACHE_ID);
        ships.put("ship-a", data);

        SocketCustody.reconcile(ships, Set.of(), new HashSet<>(), new ItemStore(), refund -> { });

        assertTrue(data.isAllocated("notable"));
    }

    @Test
    void aGrantedNotableReachedByPathLeavesWithTheSubroutine() {
        ShipSkillData data = shipWithGrant();
        ShipTrees.allocate(data, pathNode, 1);

        SocketCustody.reconcile(ships, Set.of(), new HashSet<>(), new ItemStore(), refund -> { });

        assertFalse(data.isAllocated("notable"));
        assertTrue(data.isAllocated("path"));
    }

    @Test
    void anOwnedShipKeepsItsGrant() {
        ShipSkillData data = shipWithGrant();

        SocketCustody.reconcile(ships, Set.of("ship-a"), new HashSet<>(), new ItemStore(), refund -> { });

        assertTrue(data.isAllocated("notable"));
        assertEquals(CACHE_ID, data.getSocketedItem("socket"));
    }
}
