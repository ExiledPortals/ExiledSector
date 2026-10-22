package exiledsector.items;

import exiledlib.items.Item;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemStore;
import exiledsector.skills.AnchorLinks;
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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketCustodyTest {

    private final ItemStore store = new ItemStore();
    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(), List.of(), 0f, 0f);
        SkillTree.register(socket);
        ItemFixtures.registerMilitaryAndSubroutine();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        ItemDefinitions.clear();
    }

    private Item install(String shipId) {
        Item item = store.add(ItemDefinitions.get(ItemFixtures.SUBROUTINE), shipId.hashCode());
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 3);
        data.socketItem("socket", item.id());
        ships.put(shipId, data);
        return item;
    }

    @Test
    void anItemCountsAsInstalledOnlyWhileASocketHoldsIt() {
        Item item = install("ship-a");
        Item loose = store.add(ItemDefinitions.get(ItemFixtures.SUBROUTINE), 7L);

        assertTrue(SocketCustody.isInstalled(ships, item));
        assertFalse(SocketCustody.isInstalled(ships, loose));
        ships.get("ship-a").unsocketItem("socket");
        assertFalse(SocketCustody.isInstalled(ships, item));
    }

    @Test
    void installationsMapEachItemToItsShipAndSocket() {
        Item item = install("ship-a");

        assertEquals(Map.of(item.id(), new SocketCustody.Installation("ship-a", "socket")), SocketCustody.installations(ships));
    }

    @Test
    void aShipOutOfCustodyKeepsTheVolumeItsAnchorOpenedEvenThoughTheAnchorReturnsToStorage() {
        SkillNode root = new SkillNode("root", new SkillType.Builder("root_type", "Root", "", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        SkillNode anchorSocket = new SkillNode("anchor_socket", socket.getType(), List.of("root"), 0f, 0f);
        SkillNode exit = new SkillNode("exit", new SkillType.Builder("exit_type", "Exit", "", SkillTier.WORMHOLE_EXIT).build(),
                List.of("volume_node"), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony"));
        SkillNode volumeNode = new SkillNode("volume_node", new SkillType.Builder("volume_type", "Volume", "", SkillTier.SMALL).build(),
                List.of("exit"), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony"));
        List.of(root, anchorSocket, exit, volumeNode).forEach(SkillTree::register);
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                return "anchor_item".equals(socketedItemId) ? "exit" : null;
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return true;
            }

            @Override
            public int revision() {
                return 0;
            }
        });
        try {
            ShipSkillData data = new ShipSkillData();
            ShipTrees.chooseStartingRoot(data, root);
            ShipTrees.allocate(data, anchorSocket, 1);
            data.socketItem("anchor_socket", "anchor_item");
            ShipTrees.openAnchorWormholes(data, 10);
            ShipTrees.allocate(data, volumeNode, 1);
            ships.put("ship-a", data);

            SocketCustody.reconcile(ships, Set.of(), lost, store);

            assertTrue(data.getSocketedItems().isEmpty());
            assertTrue(data.isAllocated("exit") && data.isAllocated("volume_node") && data.isAllocated("anchor_socket"));
        } finally {
            AnchorLinks.install(null);
        }
    }

    @Test
    void anOwnedShipKeepsItsItems() {
        Item item = install("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(item.id(), ships.get("ship-a").getSocketedItem("socket"));
    }

    @Test
    void aSoldOrScuttledShipReturnsItsItemsToStorage() {
        Item item = install("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNotNull(store.find(item.id()));
    }

    @Test
    void aShipLostInBattleAndNotRecoveredTakesItsItemsWithIt() {
        Item item = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNull(store.find(item.id()));
        assertTrue(lost.isEmpty());
    }

    @Test
    void shipsLostInCombatAndNotRecoveredAreReportedLostForGoodWithOrWithoutItems() {
        install("ship-a");
        ships.put("ship-b", new ShipSkillData());
        ships.put("ship-c", new ShipSkillData());
        ships.put("sold", new ShipSkillData());
        lost.addAll(Set.of("ship-a", "ship-b", "ship-c", "no-record"));

        Set<String> lostForGood = SocketCustody.reconcile(ships, Set.of("ship-c"), lost, store);

        assertEquals(Set.of("ship-a", "ship-b"), lostForGood);
    }

    @Test
    void aRecoveredShipKeepsItsItemsAndIsNoLongerCountedAsLost() {
        Item item = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(item.id(), ships.get("ship-a").getSocketedItem("socket"));
        assertTrue(lost.isEmpty());
    }

    @Test
    void onlyTheLostShipsItemsAreDestroyed() {
        Item kept = install("ship-a");
        Item returned = install("ship-b");
        Item destroyed = install("ship-c");
        lost.add("ship-c");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertNotNull(store.find(kept.id()));
        assertNotNull(store.find(returned.id()));
        assertNull(store.find(destroyed.id()));
        assertEquals(List.of(kept, returned), store.owned());
    }
}
