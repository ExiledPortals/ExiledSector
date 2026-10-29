package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemStoreTest {

    private MockSector mockSector;
    private ItemDefinition military;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        military = HullItemRows.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ItemDefinitions.clear();
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void aSaveFromBeforeHullItemsStartsWithAnEmptyStoreThatIsKeptInTheSave() {
        ItemStore store = ItemStore.get();

        assertTrue(store.owned().isEmpty());
        assertSame(store, mockSector.persistentData.get(ItemStore.dataKey()));
        assertSame(store, ItemStore.get());
    }

    @Test
    void theStoresKeepTheSaveKeysOlderSavesUse() {
        assertEquals("exiledSector_socketables", ItemStore.dataKey());
        assertEquals("exiledSector_starCharts", ItemStore.starChartDataKey());
    }

    @Test
    void addedHullItemsGetUniqueIdsAndTheRollTheirSeedGives() {
        ItemStore store = ItemStore.get();

        Item first = store.add(military, 7L);
        Item second = store.add(military, 7L);

        assertEquals(ItemKind.BRIDGE, first.kind());
        assertNotEquals(first.id(), second.id());
        assertEquals(ItemRoller.roll(military, 7L), first.explicits());
        assertEquals(List.of(first, second), store.owned());
    }

    @Test
    void hullItemItemsMoveFromCargoIntoTheStoreAndEverythingElseStays() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI pair = stack(new ItemData(military.id(), 11L).toSpecialItem(), 2f);
        CargoStackAPI single = stack(new ItemData(military.id(), 12L).toSpecialItem(), 1f);
        CargoStackAPI unknown = stack(new ItemData("removed_mod_item", 13L).toSpecialItem(), 1f);
        CargoStackAPI other = stack(new SpecialItemData("pristine_nanoforge", null), 1f);
        CargoStackAPI supplies = stack(null, 100f);
        CargoStackAPI sliver = stack(new ItemData(military.id(), 14L).toSpecialItem(), 0.3f);
        when(cargo.getStacksCopy()).thenReturn(List.of(pair, single, unknown, other, supplies, sliver));

        int moved = ItemStore.get().absorbFrom(cargo);

        assertEquals(3, moved);
        assertEquals(List.of(11L, 11L, 12L), ItemStore.get().owned().stream().map(Item::seed).toList());
        verify(cargo).removeStack(pair);
        verify(cargo).removeStack(single);
        verify(cargo, never()).removeStack(unknown);
        verify(cargo, never()).removeStack(other);
        verify(cargo, never()).removeStack(supplies);
        verify(cargo, never()).removeStack(sliver);
    }
}
