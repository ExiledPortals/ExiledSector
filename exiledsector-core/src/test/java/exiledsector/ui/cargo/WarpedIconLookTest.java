package exiledsector.ui.cargo;

import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.modifier.PoolEntry;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpedIconLookTest {

    private static final String BRIDGE = "warped_test_bridge";

    @BeforeEach
    void register() {
        ItemDefinitions.register(List.of(new ItemDefinition(BRIDGE, ItemKind.BRIDGE, BRIDGE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "")));
    }

    @AfterEach
    void clear() {
        ItemDefinitions.clear();
    }

    private static RolledModifier hull() {
        return RolledModifier.single(ModifierSlot.PREFIX, new RolledLine("HULL_PERCENT", 5f));
    }

    @Test
    void aWarpedItemGetsAChargedBreachPurpleBorder() {
        CargoIconLook look = CargoIconLooks.forItem(Item.create("warped", BRIDGE, 1L, List.of(hull().asWarped())));

        assertTrue(look.arcs());
        assertEquals(CargoIconLooks.WARPED_BORDER, look.borderColor());
    }

    @Test
    void anUnwarpedItemHasNoBreachBorder() {
        CargoIconLook look = CargoIconLooks.forItem(Item.create("plain", BRIDGE, 1L, List.of(hull())));

        assertNotEquals(CargoIconLooks.WARPED_BORDER, look.borderColor());
    }
}
