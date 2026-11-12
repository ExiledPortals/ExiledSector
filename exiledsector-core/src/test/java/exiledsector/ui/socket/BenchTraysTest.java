package exiledsector.ui.socket;

import exiledsector.items.CraftingInhibitor;
import exiledsector.items.CraftingMaterials;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BenchTraysTest {

    private final BenchTray first = mock(BenchTray.class);
    private final BenchTray second = mock(BenchTray.class);

    @Test
    void onlyOneTrayIsOpenAtATime() {
        BenchTrays trays = new BenchTrays();

        trays.toggle(first);
        assertSame(first, trays.open());

        trays.toggle(second);
        assertSame(second, trays.open());
        assertFalse(trays.isOpen(first));

        trays.toggle(second);
        assertFalse(trays.isOpen());
    }

    @Test
    void aTrayNoLongerOfferedCloses() {
        BenchTrays trays = new BenchTrays();
        trays.toggle(first);

        trays.keepOnly(List.of(first, second));
        assertSame(first, trays.open());

        trays.keepOnly(List.of(second));
        assertNull(trays.open());
    }

    @Test
    void activeInhibitorsStayOnAcrossTraySwitchesAndCloses() {
        BenchTrays trays = new BenchTrays();
        ActiveInhibitors inhibitors = new ActiveInhibitors();
        trays.toggle(InhibitorTray.INSTANCE);
        inhibitors.toggle(CraftingInhibitor.PREFIX, 1);

        trays.toggle(first);
        assertTrue(inhibitors.contains(CraftingInhibitor.PREFIX));
        trays.close();
        assertTrue(inhibitors.contains(CraftingInhibitor.PREFIX));

        inhibitors.spend(Set.of(CraftingInhibitor.PREFIX));
        assertTrue(inhibitors.isEmpty());
    }

    @Test
    void atMostFourInhibitorsAreOnAtOnce() {
        ActiveInhibitors capped = new ActiveInhibitors(1);

        assertEquals(4, ActiveInhibitors.MAX_ACTIVE);
        assertEquals(ActiveInhibitors.MAX_ACTIVE, new ActiveInhibitors().maxActive());
        assertEquals(ActiveInhibitors.Toggle.ON, capped.toggle(CraftingInhibitor.PREFIX, 1));
        assertEquals(ActiveInhibitors.Toggle.FULL, capped.toggle(CraftingInhibitor.SUFFIX, 1));
        assertEquals(Set.of(CraftingInhibitor.PREFIX), capped.view());
        assertEquals(ActiveInhibitors.Toggle.OFF, capped.toggle(CraftingInhibitor.PREFIX, 1));
        assertEquals(ActiveInhibitors.Toggle.ON, capped.toggle(CraftingInhibitor.SUFFIX, 1));
    }

    @Test
    void anInhibitorYouDoNotHoldCannotBeTurnedOnAndOneYouLoseTurnsOff() {
        ActiveInhibitors inhibitors = new ActiveInhibitors();

        assertEquals(ActiveInhibitors.Toggle.NONE_OWNED, inhibitors.toggle(CraftingInhibitor.SUFFIX, 0));
        inhibitors.toggle(CraftingInhibitor.PREFIX, 1);
        inhibitors.toggle(CraftingInhibitor.SUFFIX, 1);

        assertEquals(List.of(CraftingInhibitor.SUFFIX), inhibitors.dropMissing(itemId -> itemId.equals(CraftingInhibitor.PREFIX.itemId()) ? 1 : 0));
        assertEquals(Set.of(CraftingInhibitor.PREFIX), inhibitors.view());
    }

    @Test
    void theInhibitorTrayListsOnlyTheInhibitorsYouHold() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(Map.of(CraftingInhibitor.SUFFIX.itemId(), 2));

        List<BenchTrayEntry> entries = InhibitorTray.held(materials);

        assertEquals(List.of(new InhibitorTrayEntry(CraftingInhibitor.SUFFIX)), entries);
        assertEquals(List.of(), InhibitorTray.held(CraftingMaterials.storedForTests(Map.of())));
    }

    @Test
    void theInhibitorTrayDealsItsShownEntriesIntoTwoHeadinglessColumns() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(Map.of(CraftingInhibitor.PREFIX.itemId(), 1,
                CraftingInhibitor.SUFFIX.itemId(), 2));
        BenchTrayContext context = mock(BenchTrayContext.class);
        org.mockito.Mockito.when(context.materials()).thenReturn(materials);
        BenchTrayEntry prefix = new InhibitorTrayEntry(CraftingInhibitor.PREFIX);
        BenchTrayEntry suffix = new InhibitorTrayEntry(CraftingInhibitor.SUFFIX);

        assertEquals(List.of(new BenchTray.Column(null, List.of(prefix)), new BenchTray.Column(null, List.of(suffix))),
                InhibitorTray.INSTANCE.columns(context, entry -> true));
        assertEquals(List.of(new BenchTray.Column(null, List.of(suffix)), new BenchTray.Column(null, List.of())),
                InhibitorTray.INSTANCE.columns(context, suffix::equals));
    }

    @Test
    void theHullCraftingBenchOffersTheInhibitorTrayFirstThenEveryDomainTray() {
        assertEquals(List.of(InhibitorTray.INSTANCE, first), HullCraftingBench.hullCraftingBenchTrays(List.of(first)));
        assertEquals(List.of(InhibitorTray.INSTANCE), HullCraftingBench.hullCraftingBenchTrays(List.of()));
    }

    @Test
    void benchModesOtherThanHullItemsOfferNoTrays() {
        BenchModeSupport other = mock(BenchModeSupport.class, org.mockito.Answers.CALLS_REAL_METHODS);

        assertEquals(List.of(), other.trays());
    }
}
