package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDataTest {

    @Test
    void withExplicitsReplacesTheModifiersAndNameButKeepsTheRest() {
        ItemTraits traits = ItemTraits.ofTier(3);
        ItemData item = new ItemData("some_plate", 42L, List.of(), FrozenName.NONE, traits);
        List<RolledModifier> replacementExplicits = List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledLine("HULL_MULT", 5f)));
        FrozenName replacementName = FrozenName.product("Sturdy", null, null, null);

        ItemData replaced = item.withExplicits(replacementExplicits, replacementName);

        assertEquals("some_plate", replaced.definitionId());
        assertEquals(42L, replaced.seed());
        assertEquals(replacementExplicits, replaced.explicits());
        assertSame(replacementName, replaced.frozenName());
        assertSame(traits, replaced.traits());
    }

    @Test
    void aDefinitionIsAHullItemOnlyWhenItFitsAHullSection() {
        ItemDefinition plate = new ItemDefinition("plate", ItemKind.ARMOR_PLATING, "Plate", ItemDefinition.FALLBACK_ICON, 20f, false, "",
                List.of(), List.of(), null);
        ItemDefinition chart = new ItemDefinition("chart", null, "Chart", ItemDefinition.FALLBACK_ICON, 20f, false, "",
                List.of(), List.of(), null, ItemType.STAR_CHART, null, List.of(), ItemDefinition.NO_SOCKET_LIMIT);

        assertTrue(plate.isHullItem());
        assertFalse(chart.isHullItem());
    }

    @Test
    void anItemWithNoKnownDefinitionIsNotAHullItem() {
        assertFalse(new ItemData("no_such_definition", 1L).isHullItem());
    }
}
