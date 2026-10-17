package exiledsector.hullitems;

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

    private final HullItemStore store = new HullItemStore();
    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(), List.of(), 0f, 0f);
        SkillTree.register(socket);
        HullItemFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        HullItemDefinitions.clear();
    }

    private HullItem install(String shipId) {
        HullItem hullItem = store.add(HullItemDefinitions.get(HullItemFixtures.MILITARY), shipId.hashCode());
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", hullItem.id());
        ships.put(shipId, data);
        return hullItem;
    }

    @Test
    void anItemCountsAsInstalledOnlyWhileASocketHoldsIt() {
        HullItem hullItem = install("ship-a");
        HullItem loose = store.add(HullItemDefinitions.get(HullItemFixtures.MILITARY), 7L);

        assertTrue(SocketCustody.isInstalled(ships, hullItem));
        assertFalse(SocketCustody.isInstalled(ships, loose));
        ships.get("ship-a").unsocketItem("socket");
        assertFalse(SocketCustody.isInstalled(ships, hullItem));
    }

    @Test
    void installationsMapEachItemToItsShipAndSocket() {
        HullItem hullItem = install("ship-a");

        assertEquals(Map.of(hullItem.id(), new SocketCustody.Installation("ship-a", "socket")), SocketCustody.installations(ships));
    }

    @Test
    void anOwnedShipKeepsItsItems() {
        HullItem hullItem = install("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(hullItem.id(), ships.get("ship-a").getSocketedItem("socket"));
    }

    @Test
    void aSoldOrScuttledShipReturnsItsItemsToStorage() {
        HullItem hullItem = install("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNotNull(store.find(hullItem.id()));
    }

    @Test
    void aShipLostInBattleAndNotRecoveredTakesItsItemsWithIt() {
        HullItem hullItem = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(ships.get("ship-a").getSocketedItem("socket"));
        assertNull(store.find(hullItem.id()));
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
        HullItem hullItem = install("ship-a");
        lost.add("ship-a");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertEquals(hullItem.id(), ships.get("ship-a").getSocketedItem("socket"));
        assertTrue(lost.isEmpty());
    }

    @Test
    void onlyTheLostShipsItemsAreDestroyed() {
        HullItem kept = install("ship-a");
        HullItem returned = install("ship-b");
        HullItem destroyed = install("ship-c");
        lost.add("ship-c");

        SocketCustody.reconcile(ships, Set.of("ship-a"), lost, store);

        assertNotNull(store.find(kept.id()));
        assertNotNull(store.find(returned.id()));
        assertNull(store.find(destroyed.id()));
        assertEquals(List.of(kept, returned), store.owned());
    }
}
