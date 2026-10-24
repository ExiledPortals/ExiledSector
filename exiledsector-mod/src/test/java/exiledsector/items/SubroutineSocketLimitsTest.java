package exiledsector.items;

import exiledlib.MockSector;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static exiledsector.items.ItemFixtures.subroutineRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubroutineSocketLimitsTest {

    private static final String VOLUME = "volume_sub";
    private static final String BYPASS = "bypass_sub";
    private static final String DOCTRINE = "doctrine_sub";
    private static final String UNIQUE = "unique_sub";
    private static final String LIMITED_UNIQUE = "limited_unique_sub";
    private static final String UNLIMITED_UNIQUE = "zero_limit_unique_sub";
    private static final String HULL_ITEM = "plate";

    private MockSector mockSector;
    private final ShipSkillData shipData = new ShipSkillData();

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ItemDefinitions.register(new JSONArray()
                .put(subroutineRow(VOLUME, SubroutineKind.MODSPEC_CACHE))
                .put(subroutineRow(BYPASS, SubroutineKind.BYPASS))
                .put(subroutineRow(DOCTRINE, SubroutineKind.DOCTRINE))
                .put(subroutineRow(UNIQUE, SubroutineKind.UNIQUE).put("unique", "true"))
                .put(subroutineRow(LIMITED_UNIQUE, SubroutineKind.UNIQUE).put("unique", "true").put(SubroutineDefinitions.SOCKET_LIMIT_COLUMN, "1"))
                .put(subroutineRow(UNLIMITED_UNIQUE, SubroutineKind.UNIQUE).put("unique", "true").put(SubroutineDefinitions.SOCKET_LIMIT_COLUMN, "0"))
                .put(HullItemRows.row(HULL_ITEM, "armor_plating", "HULL_MULT:4:6")));
        SkillType socketType = new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build();
        for (String socketId : List.of("socket_1", "socket_2", "socket_3")) {
            SkillNode socket = new SkillNode(socketId, socketType, List.of(), 0f, 0f);
            SkillTree.register(socket);
            ShipTrees.allocate(shipData, socket, 1);
        }
    }

    @AfterEach
    void tearDown() {
        NpcItems.clearCache();
        ItemDefinitions.clear();
        SkillTree.clearNodes();
        mockSector.close();
    }

    private static Item owned(String definitionId, long seed) {
        return ItemStore.get().add(ItemDefinitions.get(definitionId), seed);
    }

    @Test
    void aSecondDoctrineOrBypassOnOneShipIsRefusedWithAReasonThePlayerCanRead() {
        for (String definitionId : List.of(DOCTRINE, BYPASS)) {
            ShipSkillData ship = new ShipSkillData();
            SkillTree.getAllNodes().values().forEach(socket -> ShipTrees.allocate(ship, socket, 1));
            Item first = owned(definitionId, 1L);
            Item second = owned(definitionId, 2L);
            assertNull(SubroutineSocketLimits.refusal(first, "socket_1", ship.getSocketedItems()));
            ship.socketItem("socket_1", first.id());

            String refusal = SubroutineSocketLimits.refusal(second, "socket_2", ship.getSocketedItems());

            assertEquals(second.name() + " can't be installed: this ship already has the most subroutines of its kind (1).", refusal);
            assertNull(SubroutineSocketLimits.refusal(second, "socket_1", ship.getSocketedItems()), "replacing the one already socketed is fine");
            assertNull(SubroutineSocketLimits.refusal(first, "socket_2", ship.getSocketedItems()), "moving the same item is not a second copy");
        }
    }

    @Test
    void volumeAndUniqueSubroutinesHaveNoPerShipLimit() {
        for (String definitionId : List.of(VOLUME, UNIQUE)) {
            ShipSkillData ship = new ShipSkillData();
            SkillTree.getAllNodes().values().forEach(socket -> ShipTrees.allocate(ship, socket, 1));
            ship.socketItem("socket_1", owned(definitionId, 1L).id());
            ship.socketItem("socket_2", owned(definitionId, 2L).id());

            assertFalse(SubroutineSocketLimits.limitReached(owned(definitionId, 3L), "socket_3", ship.getSocketedItems()), definitionId);
        }
    }

    @Test
    void otherKindsAndNpcItemsCountOnlyTowardsTheirOwnKind() {
        shipData.socketItem("socket_1", owned(DOCTRINE, 1L).id());
        shipData.socketItem("socket_2", NpcItems.id(BYPASS, 5L));

        assertFalse(SubroutineSocketLimits.limitReached(owned(VOLUME, 2L), "socket_3", shipData.getSocketedItems()));
        assertTrue(SubroutineSocketLimits.limitReached(owned(BYPASS, 3L), "socket_3", shipData.getSocketedItems()));
        assertTrue(SubroutineSocketLimits.limitReached(ItemDefinitions.get(DOCTRINE), "socket_3", shipData.getSocketedItems()));
        assertFalse(SubroutineSocketLimits.limitReached(ItemDefinitions.get(HULL_ITEM), "socket_3", shipData.getSocketedItems()));
    }

    @Test
    void theLimitCountsDefinitionsThroughTheGivenLookup() {
        Map<String, String> socketedItemIds = new LinkedHashMap<>();
        socketedItemIds.put("socket_1", "item_a");
        socketedItemIds.put("socket_2", "missing");
        Map<String, ItemDefinition> definitionsByItemId = Map.of("item_a", ItemDefinitions.get(BYPASS));

        assertTrue(SubroutineSocketLimits.limitReached(SubroutineKind.BYPASS, "item_b", "socket_3", socketedItemIds, definitionsByItemId::get));
        assertFalse(SubroutineSocketLimits.limitReached(SubroutineKind.DOCTRINE, "item_b", "socket_3", socketedItemIds, definitionsByItemId::get));
        assertFalse(SubroutineSocketLimits.limitReached(null, "item_b", "socket_3", socketedItemIds, definitionsByItemId::get));
    }

    @Test
    void anUnresolvedSocketedItemDoesNotCountSoTheLoadSweepMustCatchTheExtra() {
        Map<String, String> socketedItemIds = Map.of("socket_1", "unresolved_doctrine");

        assertFalse(SubroutineSocketLimits.limitReached(SubroutineKind.DOCTRINE, "second", "socket_2", socketedItemIds, itemId -> null));
    }

    @Test
    void theLoadSweepKeepsTheFirstOfAKindBySocketIdAndReturnsTheRestToStorage() {
        Item thirdDoctrine = owned(DOCTRINE, 3L);
        Item firstDoctrine = owned(DOCTRINE, 1L);
        Item secondDoctrine = owned(DOCTRINE, 2L);
        Item firstVolume = owned(VOLUME, 4L);
        ShipSkillData otherShip = new ShipSkillData();
        SkillTree.getAllNodes().values().forEach(socket -> ShipTrees.allocate(otherShip, socket, 1));
        Item otherShipBypass = owned(BYPASS, 5L);
        shipData.socketItem("socket_3", thirdDoctrine.id());
        shipData.socketItem("socket_1", firstDoctrine.id());
        shipData.socketItem("socket_2", secondDoctrine.id());
        otherShip.socketItem("socket_1", otherShipBypass.id());
        otherShip.socketItem("socket_2", firstVolume.id());
        Map<String, ShipSkillData> ships = new LinkedHashMap<>();
        ships.put("ship", shipData);
        ships.put("other", otherShip);

        List<Item> returned = SubroutineSocketLimits.unsocketExtras(ships, ItemStore.get());

        assertEquals(List.of(secondDoctrine, thirdDoctrine), returned);
        assertEquals(Map.of("socket_1", firstDoctrine.id()), shipData.getSocketedItems());
        assertEquals(Map.of("socket_1", otherShipBypass.id(), "socket_2", firstVolume.id()), otherShip.getSocketedItems());
        assertTrue(ItemStore.get().owned().containsAll(List.of(firstDoctrine, secondDoctrine, thirdDoctrine)), "extras go back to storage");
        assertEquals(List.of(), SubroutineSocketLimits.unsocketExtras(ships, ItemStore.get()), "a second sweep finds nothing");
    }

    @Test
    void theLoadSweepRunsWithTheSocketTypeMigrationOnGameLoad() {
        shipData.socketItem("socket_1", owned(BYPASS, 1L).id());
        shipData.socketItem("socket_2", owned(BYPASS, 2L).id());
        ShipSkillDataManager.put("ship", shipData);

        SocketTypeMigration.run();

        assertEquals(1, shipData.getSocketedItems().size());
    }

    @Test
    void aDefinitionWithASocketLimitRefusesAnotherCopyButAllowsSwapsAndMoves() {
        Item first = owned(LIMITED_UNIQUE, 1L);
        Item second = owned(LIMITED_UNIQUE, 2L);
        shipData.socketItem("socket_1", first.id());

        String refusal = SubroutineSocketLimits.refusal(second, "socket_2", shipData.getSocketedItems());

        assertEquals(1, ItemDefinitions.get(LIMITED_UNIQUE).socketLimit());
        assertEquals(second.name() + " can't be installed: this ship already has the most copies of it (1).", refusal);
        assertNull(SubroutineSocketLimits.refusal(second, "socket_1", shipData.getSocketedItems()), "swapping a copy into the same socket is fine");
        assertNull(SubroutineSocketLimits.refusal(first, "socket_2", shipData.getSocketedItems()), "moving the same item is not a second copy");
        assertNull(SubroutineSocketLimits.refusal(owned(UNIQUE, 3L), "socket_2", shipData.getSocketedItems()), "another definition does not count");
        assertTrue(SubroutineSocketLimits.limitReached(ItemDefinitions.get(LIMITED_UNIQUE), "socket_2", shipData.getSocketedItems()));
    }

    @Test
    void aBlankOrZeroSocketLimitIsUnlimited() {
        for (String definitionId : List.of(UNIQUE, UNLIMITED_UNIQUE)) {
            ShipSkillData ship = new ShipSkillData();
            SkillTree.getAllNodes().values().forEach(socket -> ShipTrees.allocate(ship, socket, 1));
            ship.socketItem("socket_1", owned(definitionId, 1L).id());
            ship.socketItem("socket_2", owned(definitionId, 2L).id());

            assertFalse(ItemDefinitions.get(definitionId).limitsSocketsPerShip(), definitionId);
            assertNull(SubroutineSocketLimits.refusal(owned(definitionId, 3L), "socket_3", ship.getSocketedItems()), definitionId);
        }
    }

    @Test
    void npcCopiesCountTowardsTheDefinitionLimitSoFleetsRollAtMostTheLimit() {
        shipData.socketItem("socket_1", NpcItems.id(LIMITED_UNIQUE, 5L));

        assertTrue(SubroutineSocketLimits.limitReached(ItemDefinitions.get(LIMITED_UNIQUE), "socket_2", shipData.getSocketedItems()));
        assertFalse(SubroutineSocketLimits.limitReached(ItemDefinitions.get(LIMITED_UNIQUE), "socket_1", shipData.getSocketedItems()));
        assertFalse(SubroutineSocketLimits.definitionLimitReached(ItemDefinitions.get(UNIQUE), null, "socket_2", shipData.getSocketedItems(),
                SubroutineSocketLimits::definitionOf));
    }

    @Test
    void theLoadSweepKeepsTheFirstCopyBySocketIdAndReturnsTheRest() {
        Item thirdCopy = owned(LIMITED_UNIQUE, 3L);
        Item firstCopy = owned(LIMITED_UNIQUE, 1L);
        Item unlimited = owned(UNIQUE, 2L);
        shipData.socketItem("socket_3", thirdCopy.id());
        shipData.socketItem("socket_1", firstCopy.id());
        shipData.socketItem("socket_2", unlimited.id());

        List<Item> returned = SubroutineSocketLimits.unsocketExtras(Map.of("ship", shipData), ItemStore.get());

        assertEquals(List.of(thirdCopy), returned);
        assertEquals(Map.of("socket_1", firstCopy.id(), "socket_2", unlimited.id()), shipData.getSocketedItems());
    }

    @Test
    void aTranspositionIntoALimitedDefinitionUnsocketsTheNewcomer() {
        Item firstCopy = owned(LIMITED_UNIQUE, 1L);
        Item unique = owned(UNIQUE, 2L);
        shipData.socketItem("socket_1", firstCopy.id());
        shipData.socketItem("socket_2", unique.id());
        Map<String, ShipSkillData> ships = Map.of("ship", shipData);

        Item transposed = Item.create(unique.id(), LIMITED_UNIQUE, 9L, List.of());
        ItemStore.get().replace(unique, transposed);

        assertTrue(SocketCustody.unsocketIfMisfit(ships, transposed));
        assertEquals(Map.of("socket_1", firstCopy.id()), shipData.getSocketedItems());
    }

    @Test
    void aTranspositionThatBreaksTheLimitUnsocketsTheNewcomer() {
        Item firstBypass = owned(BYPASS, 1L);
        Item unique = owned(UNIQUE, 2L);
        shipData.socketItem("socket_1", firstBypass.id());
        shipData.socketItem("socket_2", unique.id());
        Map<String, ShipSkillData> ships = Map.of("ship", shipData);
        assertFalse(SocketCustody.unsocketIfMisfit(ships, unique));

        Item transposed = Item.create(unique.id(), BYPASS, 9L, List.of());
        ItemStore.get().replace(unique, transposed);

        assertTrue(SocketCustody.unsocketIfMisfit(ships, transposed));
        assertEquals(Map.of("socket_1", firstBypass.id()), shipData.getSocketedItems());
    }
}
