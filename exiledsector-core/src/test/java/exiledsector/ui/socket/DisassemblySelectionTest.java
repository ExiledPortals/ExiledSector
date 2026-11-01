package exiledsector.ui.socket;

import exiledsector.items.Item;
import exiledsector.items.ItemRarity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisassemblySelectionTest {

    private static Item item() {
        Item item = mock(Item.class);
        when(item.rarity()).thenReturn(ItemRarity.COMMON);
        return item;
    }

    @Test
    void markingTogglesAndLeavingClearsTheMarks() {
        DisassemblySelection selection = new DisassemblySelection(DisassemblySelection.ITEM_TEXTS);
        Item first = item();
        Item second = item();

        selection.enter();
        selection.toggle(first);
        selection.toggle(second);
        selection.toggle(second);

        assertTrue(selection.active());
        assertEquals(List.of(first), selection.marked());
        selection.forget(List.of(first));
        assertTrue(selection.nothingMarked());
        selection.toggle(second);
        selection.leave();
        assertFalse(selection.active());
        assertTrue(selection.nothingMarked());
    }

    @Test
    void disassemblingUnmarksTheTargetsAndGivesNoNoticeWhenNothingBrokeDown() {
        DisassemblySelection selection = new DisassemblySelection(DisassemblySelection.ITEM_TEXTS);
        Item marked = item();
        selection.enter();
        selection.toggle(marked);
        selection.confirming(List.of(marked));

        assertNull(selection.disassemble(List.of(marked), target -> 0));
        assertTrue(selection.nothingMarked());
        assertEquals(List.of(marked), selection.pending());
        selection.confirmClosed();
        assertEquals(List.of(), selection.pending());
    }
}
