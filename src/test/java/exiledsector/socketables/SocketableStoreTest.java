package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketableStoreTest {

    private final Map<String, Object> persistentData = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private SocketableDefinition military;

    @BeforeEach
    void setUp() throws Exception {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        military = SocketableFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SocketableDefinitions.clear();
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void aSaveFromBeforeSocketablesStartsWithAnEmptyStoreThatIsKeptInTheSave() {
        SocketableStore store = SocketableStore.get();

        assertTrue(store.owned().isEmpty());
        assertSame(store, persistentData.get(SocketableStore.DATA_KEY));
        assertSame(store, SocketableStore.get());
    }

    @Test
    void addedSocketablesGetUniqueIdsAndTheRollTheirSeedGives() {
        SocketableStore store = SocketableStore.get();

        Socketable first = store.add(military, 7L);
        Socketable second = store.add(military, 7L);

        assertInstanceOf(Subroutine.class, first);
        assertNotEquals(first.id(), second.id());
        assertEquals(SocketableRoller.roll(military, 7L), first.effects());
        assertEquals(List.of(first, second), store.owned());
    }

    @Test
    void socketableItemsMoveFromCargoIntoTheStoreAndEverythingElseStays() {
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI pair = stack(new SocketableItemData(military.id(), 11L).toSpecialItem(), 2f);
        CargoStackAPI single = stack(new SocketableItemData(military.id(), 12L).toSpecialItem(), 1f);
        CargoStackAPI unknown = stack(new SocketableItemData("removed_mod_item", 13L).toSpecialItem(), 1f);
        CargoStackAPI other = stack(new SpecialItemData("pristine_nanoforge", null), 1f);
        CargoStackAPI supplies = stack(null, 100f);
        CargoStackAPI sliver = stack(new SocketableItemData(military.id(), 14L).toSpecialItem(), 0.3f);
        when(cargo.getStacksCopy()).thenReturn(List.of(pair, single, unknown, other, supplies, sliver));

        int moved = SocketableStore.get().absorbFrom(cargo);

        assertEquals(3, moved);
        assertEquals(List.of(11L, 11L, 12L), SocketableStore.get().owned().stream().map(Socketable::seed).toList());
        verify(cargo).removeStack(pair);
        verify(cargo).removeStack(single);
        verify(cargo, never()).removeStack(unknown);
        verify(cargo, never()).removeStack(other);
        verify(cargo, never()).removeStack(supplies);
        verify(cargo, never()).removeStack(sliver);
    }
}
