package exiledsector.items;

import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
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

class VolumeGrantCustodyTest {

    private static final String CHIP_ID = "item_volume";

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
            public String grantedNodeId(String socketedItemId) {
                return CHIP_ID.equals(socketedItemId) ? "notable" : null;
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
        data.chooseStartingRoot(root);
        data.allocate(socket, 1);
        data.socketItem("socket", CHIP_ID);
        data.allocate(notable, 1);
        ships.put("ship-a", data);
        return data;
    }

    @Test
    void aShipLeavingCustodyLosesItsSubroutineAndTheNotableItGrantedWithItsItemRefunded() {
        ShipSkillData data = shipWithGrant();
        SkillItemCost itemCharge = new SkillItemCost("abyssal_core", 1f);
        data.recordItemCharge("notable", itemCharge);
        List<SkillItemCost> refunds = new ArrayList<>();

        SocketCustody.reconcile(ships, Set.of(), new HashSet<>(), new ItemStore(), refunds::add);

        assertNull(data.getSocketedItem("socket"));
        assertFalse(data.isAllocated("notable"));
        assertTrue(data.isAllocated("socket"));
        assertEquals(List.of(itemCharge), refunds);
    }

    @Test
    void aShipDestroyedInBattleRefundsNothingForItsGrantedNotable() {
        ShipSkillData data = shipWithGrant();
        data.recordItemCharge("notable", new SkillItemCost("abyssal_core", 1f));
        List<SkillItemCost> refunds = new ArrayList<>();

        Set<String> lostForGood = SocketCustody.reconcile(ships, Set.of(), new HashSet<>(Set.of("ship-a")), new ItemStore(), refunds::add);

        assertEquals(Set.of("ship-a"), lostForGood);
        assertNull(data.getSocketedItem("socket"));
        assertEquals(List.of(), refunds);
    }

    @Test
    void aNotableAllocatedByPathStaysWhenTheSubroutineLeaves() {
        ShipSkillData data = shipWithGrant();
        data.allocate(pathNode, 1);

        SocketCustody.reconcile(ships, Set.of(), new HashSet<>(), new ItemStore(), refund -> { });

        assertTrue(data.isAllocated("notable"));
    }

    @Test
    void anOwnedShipKeepsItsGrant() {
        ShipSkillData data = shipWithGrant();

        SocketCustody.reconcile(ships, Set.of("ship-a"), new HashSet<>(), new ItemStore(), refund -> { });

        assertTrue(data.isAllocated("notable"));
        assertEquals(CHIP_ID, data.getSocketedItem("socket"));
    }
}
