package exiledsector.socketables;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
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

    private final SocketableStore store = new SocketableStore();
    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(), List.of(), 0f, 0f);
        SkillTree.register(socket);
        SocketableFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SocketableDefinitions.clear();
    }

    private Socketable install(String shipId) {
        Socketable socketable = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), shipId.hashCode());
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", socketable.id());
        ships.put(shipId, data);
        return socketable;
    }

    @Test
    void anItemCountsAsInstalledOnlyWhileASocketHoldsIt() {
        Socketable socketable = install("ship-a");
        Socketable loose = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), 7L);

        assertTrue(SocketCustody.isInstalled(ships, socketable));
        assertFalse(SocketCustody.isInstalled(ships, loose));
        ships.get("ship-a").unsocketItem("socket");
        assertFalse(SocketCustody.isInstalled(ships, socketable));
    }

    @Test
    void installationsMapEachItemToItsShipAndSocket() {
        Socketable socketable = install("ship-a");

        assertEquals(Map.of(socketable.id(), new SocketCustody.Installation("ship-a", "socket")), SocketCustody.installations(ships));
    }

    @Test
    void anOwnedShipKeepsItsItems() {
        Socketable socketable = install("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(socketable.id(), ships.get("ship-a").getSocketedItem("socket"));
    }

    @Test
    void aSoldOrScuttledShipReturnsItsItemsToStorage() {
        Socketable socketable = install("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNotNull(store.find(socketable.id()));
    }

    @Test
    void aShipLostInBattleAndNotRecoveredTakesItsItemsWithIt() {
        Socketable socketable = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNull(store.find(socketable.id()));
        assertTrue(lost.isEmpty());
    }

    @Test
    void aRecoveredShipKeepsItsItemsAndIsNoLongerCountedAsLost() {
        Socketable socketable = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(socketable.id(), ships.get("ship-a").getSocketedItem("socket"));
        assertTrue(lost.isEmpty());
    }

    @Test
    void onlyTheLostShipsItemsAreDestroyed() {
        Socketable kept = install("ship-a");
        Socketable returned = install("ship-b");
        Socketable destroyed = install("ship-c");
        lost.add("ship-c");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertNotNull(store.find(kept.id()));
        assertNotNull(store.find(returned.id()));
        assertNull(store.find(destroyed.id()));
        assertEquals(List.of(kept, returned), store.owned());
    }
}
