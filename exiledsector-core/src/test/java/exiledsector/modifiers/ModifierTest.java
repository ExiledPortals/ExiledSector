package exiledsector.modifiers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierTest {

    @Test
    void prefixesAndSuffixesSitUnderTheExplicitCategory() {
        assertEquals(ModifierCategory.IMPLICIT, ModifierSlot.IMPLICIT.category());
        assertEquals(ModifierCategory.EXPLICIT, ModifierSlot.PREFIX.category());
        assertEquals(ModifierCategory.EXPLICIT, ModifierSlot.SUFFIX.category());
    }

    @Test
    void onlyImplicitsAreNotExplicit() {
        assertFalse(ModifierSlot.IMPLICIT.isExplicit());
        assertTrue(ModifierSlot.PREFIX.isExplicit());
        assertTrue(ModifierSlot.SUFFIX.isExplicit());
    }

    @Test
    void aSingleLineModifierIsNamedAfterItsEffect() {
        ModifierLine armor = ModifierLine.range("ARMOR_PERCENT", 10f, 5f);
        Modifier modifier = Modifier.single(ModifierSlot.IMPLICIT, armor);

        assertEquals("ARMOR_PERCENT", modifier.id());
        assertEquals(5f, armor.min());
        assertEquals(10f, armor.max());
        assertSame(armor, modifier.line());
        assertFalse(modifier.isHybrid());
    }

    @Test
    void aHybridModifierCarriesSeveralLinesAndFindsThemByEffect() {
        ModifierLine armor = ModifierLine.range("ARMOR_PERCENT", 5f, 10f);
        ModifierLine hull = ModifierLine.range("HULL_PERCENT", 3f, 6f);
        Modifier hybrid = new Modifier("plated_frame", ModifierSlot.PREFIX, List.of(armor, hull));

        assertTrue(hybrid.isHybrid());
        assertSame(hull, hybrid.line("HULL_PERCENT"));
        assertNull(hybrid.line("TOP_SPEED_PERCENT"));
    }

    @Test
    void aModifierNeedsAnIdASlotAndALine() {
        ModifierLine armor = ModifierLine.range("ARMOR_PERCENT", 5f, 10f);

        assertThrows(IllegalArgumentException.class, () -> new Modifier("", ModifierSlot.PREFIX, List.of(armor)));
        assertThrows(IllegalArgumentException.class, () -> new Modifier("x", null, List.of(armor)));
        assertThrows(IllegalArgumentException.class, () -> new Modifier("x", ModifierSlot.PREFIX, List.of()));
    }

    @Test
    void parsingKeepsAnyTrailingWeightFieldForTheCaller() {
        ModifierLine.Parsed weighted = ModifierLine.parse("ARMOR_FLAT:10/15/20/30:750");
        ModifierLine.Parsed plain = ModifierLine.parse("HULL_PERCENT:4:6");

        assertEquals(List.of(10f, 15f, 20f, 30f), weighted.line().hullValues());
        assertEquals("750", weighted.trailingField());
        assertNull(plain.trailingField());
    }
}
