package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeletedSubroutinesTest {

    private static final String DELETED = "domain_subroutine_military";
    private static final String MISSING_HULL_ITEM = "retired_bridge";

    private final Map<String, ShipSkillData> shipDataById = new LinkedHashMap<>();
    private SkillNode socket;
    private SkillNode secondSocket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SkillType socketType = new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build();
        socket = new SkillNode("socket", socketType, List.of(), 0f, 0f);
        secondSocket = new SkillNode("second_socket", socketType, List.of(), 0f, 0f);
        SkillTree.register(socket);
        SkillTree.register(secondSocket);
        ItemFixtures.registerMilitaryAndSubroutine();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        ItemDefinitions.clear();
        NpcItems.clearCache();
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    private static ItemStore storeSavedWith(String... savedItems) {
        StringBuilder xml = new StringBuilder("<exiledSector.SocketableStore><owned>");
        for (String savedItem : savedItems) {
            xml.append(savedItem);
        }
        xml.append("</owned><nextId>").append(savedItems.length + 1).append("</nextId></exiledSector.SocketableStore>");
        return (ItemStore) xstream().fromXML(xml.toString());
    }

    private static String saved(String alias, String itemId, String definitionId) {
        return "<" + alias + "><id>" + itemId + "</id><definitionId>" + definitionId + "</definitionId><seed>5</seed><explicitModifiers>"
                + "<exiledSector.RolledModifier><modifierId>HULL_MULT</modifierId><slot>SUFFIX</slot><lines><exiledSector.RolledEffect>"
                + "<effectName>HULL_MULT</effectName><magnitude>5.0</magnitude></exiledSector.RolledEffect></lines>"
                + "</exiledSector.RolledModifier></explicitModifiers></" + alias + ">";
    }

    private ShipSkillData shipHolding(String shipId, String... socketedItemIds) {
        ShipSkillData shipData = new ShipSkillData();
        List<SkillNode> sockets = List.of(socket, secondSocket);
        for (int index = 0; index < socketedItemIds.length; index++) {
            shipData.allocate(sockets.get(index), 3);
            shipData.socketItem(sockets.get(index).getId(), socketedItemIds[index]);
        }
        shipDataById.put(shipId, shipData);
        return shipData;
    }

    @Test
    void aSavedSubroutineWhoseDefinitionIsGoneLoadsAsAnUnresolvedItemLikeAnyOtherItem() {
        ItemStore store = storeSavedWith(saved("exiledSector.Subroutine", "socketable_1", DELETED),
                saved("exiledSector.HullItem", "socketable_2", MISSING_HULL_ITEM),
                saved("exiledSector.Subroutine", "socketable_3", ItemFixtures.SUBROUTINE));

        assertEquals(UnresolvedItem.class, store.find("socketable_1").getClass());
        assertNull(store.find("socketable_1").definition());
        assertEquals(UnresolvedItem.class, store.find("socketable_2").getClass());
        assertEquals(Subroutine.class, store.find("socketable_3").getClass());
        assertEquals(SubroutineKind.MODSPEC_CACHE, store.find("socketable_3").definition().subroutineKind());
    }

    @Test
    void aReleasedSaveWithLegacySocketableEntriesLosesItsRetiredSubroutinesFromStorageAndSockets() {
        ItemStore store = storeSavedWith(saved("exiledSector.Socketable", "socketable_1", DELETED),
                saved("exiledSector.Socketable", "socketable_2", "domain_subroutine_consumer"),
                saved("exiledSector.Socketable", "socketable_3", ItemFixtures.MILITARY));
        ShipSkillData installedShip = shipHolding("ship_a", "socketable_1");

        assertEquals(UnresolvedItem.class, store.find("socketable_1").getClass());
        assertEquals(2, DeletedSubroutines.remove(shipDataById, store));
        assertEquals(List.of("socketable_3"), store.owned().stream().map(Item::id).toList());
        assertEquals(HullItem.class, store.find("socketable_3").getClass());
        assertEquals(Map.of(), installedShip.getSocketedItems());
    }

    @Test
    void aSubroutineWhoseDefinitionIsOnlyMissingSurvivesTheSweepAsAnUnresolvedItem() {
        String missingSubroutine = "volume_subroutine_from_another_mod";
        ItemStore store = storeSavedWith(saved("exiledSector.Subroutine", "socketable_1", missingSubroutine));
        ShipSkillData installedShip = shipHolding("ship_a", "socketable_1", NpcItems.id(missingSubroutine, 9L));
        CargoStackAPI cargoStack = stack(new ItemData(missingSubroutine, 5L).toSpecialItem(), 1f);
        CargoAPI playerCargo = cargoHolding(cargoStack);

        assertEquals(0, DeletedSubroutines.remove(shipDataById, store));
        assertEquals(0, DeletedSubroutines.removeFromCargoes(List.of(playerCargo)));

        assertEquals(UnresolvedItem.class, store.find("socketable_1").getClass());
        assertEquals(Map.of("socket", "socketable_1", "second_socket", NpcItems.id(missingSubroutine, 9L)), installedShip.getSocketedItems());
        verify(playerCargo, never()).removeStack(any());
    }

    @Test
    void aRetiredIdThatIsDefinedAgainIsNeverDeleted() throws Exception {
        ItemDefinitions.register(new org.json.JSONArray().put(ItemFixtures.subroutineRow(DELETED)));
        ItemStore store = new ItemStore();
        Item revived = store.add(ItemDefinitions.get(DELETED), 4L);

        assertEquals(0, DeletedSubroutines.remove(shipDataById, store));
        assertEquals(List.of(revived), store.owned());
    }

    @Test
    void retiredSubroutinesLeaveStorageAndEverySocketWhileEverythingElseStays() {
        ItemStore store = storeSavedWith(saved("exiledSector.Subroutine", "socketable_1", DELETED),
                saved("exiledSector.Subroutine", "socketable_2", DELETED),
                saved("exiledSector.HullItem", "socketable_3", MISSING_HULL_ITEM),
                saved("exiledSector.Subroutine", "socketable_4", ItemFixtures.SUBROUTINE),
                saved("exiledSector.HullItem", "socketable_5", ItemFixtures.MILITARY));
        ShipSkillData installedShip = shipHolding("ship_a", "socketable_1", "socketable_4");
        ShipSkillData recoveredShip = shipHolding("ship_b", NpcItems.id(DELETED, 9L), NpcItems.id(ItemFixtures.SUBROUTINE, 9L));

        int removedCount = DeletedSubroutines.remove(shipDataById, store);

        assertEquals(3, removedCount);
        assertEquals(List.of("socketable_3", "socketable_4", "socketable_5"), store.owned().stream().map(Item::id).toList());
        assertNull(store.find("socketable_1"));
        assertEquals(Map.of("second_socket", "socketable_4"), installedShip.getSocketedItems());
        assertEquals(Map.of("second_socket", NpcItems.id(ItemFixtures.SUBROUTINE, 9L)), recoveredShip.getSocketedItems());
        assertSame(UnresolvedItem.class, store.find("socketable_3").getClass());
    }

    @Test
    void aSaveWithNothingToRemoveIsLeftAlone() {
        ItemStore store = new ItemStore();
        Item subroutine = store.add(ItemDefinitions.get(ItemFixtures.SUBROUTINE), 4L);
        Item hullItem = store.add(ItemDefinitions.get(ItemFixtures.MILITARY), 4L);
        ShipSkillData shipData = shipHolding("ship_a", subroutine.id());

        assertEquals(0, DeletedSubroutines.remove(shipDataById, store));
        assertEquals(List.of(subroutine, hullItem), store.owned());
        assertEquals(Map.of("socket", subroutine.id()), shipData.getSocketedItems());
        assertTrue(DeletedSubroutines.isDeleted(storeSavedWith(saved("exiledSector.Subroutine", "socketable_1", DELETED)).owned().get(0)));
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    private static CargoAPI cargoHolding(CargoStackAPI... stacks) {
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(List.of(stacks));
        return cargo;
    }

    @Test
    void deletedSubroutinesLeaveCargoWhileHullItemsAndOtherStacksStay() {
        CargoStackAPI deletedStack = stack(new ItemData(DELETED, 5L).toSpecialItem(), 2f);
        CargoStackAPI rolledDeletedStack = stack(new ItemData("domain_subroutine_management", 6L,
                List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("HULL_MULT", 5f))), null).toSpecialItem(), 1f);
        CargoStackAPI missingHullItemStack = stack(new ItemData(MISSING_HULL_ITEM, 5L).toSpecialItem(), 1f);
        CargoStackAPI subroutineStack = stack(new ItemData(ItemFixtures.SUBROUTINE, 5L).toSpecialItem(), 1f);
        CargoStackAPI hullItemStack = stack(new ItemData(ItemFixtures.MILITARY, 5L).toSpecialItem(), 1f);
        CargoStackAPI otherSpecialStack = stack(new SpecialItemData("exiledSector_socketable_parts", null), 30f);
        CargoStackAPI commodityStack = stack(null, 100f);
        CargoAPI playerCargo = cargoHolding(deletedStack, missingHullItemStack, subroutineStack, commodityStack);
        CargoAPI storageCargo = cargoHolding(rolledDeletedStack, hullItemStack, otherSpecialStack);
        List<CargoAPI> savedCargoes = new ArrayList<>(List.of(playerCargo, storageCargo));
        savedCargoes.add(null);

        assertEquals(3, DeletedSubroutines.removeFromCargoes(savedCargoes));

        verify(playerCargo).removeStack(deletedStack);
        verify(storageCargo).removeStack(rolledDeletedStack);
        verify(playerCargo, times(1)).removeStack(any());
        verify(storageCargo, times(1)).removeStack(any());
    }

    @Test
    void cargoIsLeftAloneWhenNoDefinitionsLoaded() {
        CargoStackAPI deletedStack = stack(new ItemData(DELETED, 5L).toSpecialItem(), 1f);
        CargoAPI playerCargo = cargoHolding(deletedStack);
        ItemDefinitions.clear();

        DeletedSubroutines.removeFromSave(List.of(playerCargo));

        verify(playerCargo, never()).removeStack(any());
        verify(playerCargo, never()).getStacksCopy();
    }
}
