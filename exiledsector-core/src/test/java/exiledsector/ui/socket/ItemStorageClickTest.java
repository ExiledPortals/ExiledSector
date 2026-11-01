package exiledsector.ui.socket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStorageClickTest {

    @Test
    void freeItemsAreAlwaysClickable() {
        assertTrue(ItemStoragePanel.clickable(false, false, false));
        assertTrue(ItemStoragePanel.clickable(false, true, true));
    }

    @Test
    void installedItemsLoadIntoTheBench() {
        assertTrue(ItemStoragePanel.clickable(true, true, false));
    }

    @Test
    void installedItemsCannotBePlacedOrDisassembled() {
        assertFalse(ItemStoragePanel.clickable(true, false, false));
        assertFalse(ItemStoragePanel.clickable(true, true, true));
    }
}
