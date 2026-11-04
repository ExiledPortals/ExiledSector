package exiledsector.items;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierSourcesTest {

    private static final String MODIFIER = "TIMEFLOW_PERCENT_TEST";
    private static final String OWNER = "biome:test";

    @AfterEach
    void clear() {
        ModifierSources.clear();
    }

    @Test
    void aReservationKeepsItsOwnerAndJoinsThePartsItsOwnerAdds() {
        ModifierSources.reserve(MODIFIER, OWNER, List.of(ItemKind.BRIDGE));
        ModifierSources.reserve(MODIFIER, OWNER, List.of(ItemKind.REACTOR));

        assertTrue(ModifierSources.isReserved(MODIFIER));
        assertEquals(OWNER, ModifierSources.owner(MODIFIER));
        assertEquals(Set.of(ItemKind.BRIDGE, ItemKind.REACTOR), ModifierSources.parts(MODIFIER));
    }

    @Test
    void anotherOwnerCannotReserveTheSameModifier() {
        ModifierSources.reserve(MODIFIER, OWNER, List.of(ItemKind.BRIDGE));

        assertThrows(IllegalArgumentException.class, () -> ModifierSources.reserve(MODIFIER, "biome:other", List.of(ItemKind.BRIDGE)));
        assertEquals(Set.of(ItemKind.BRIDGE), ModifierSources.parts(MODIFIER));
    }

    @Test
    void releasingDropsTheOwnersModifiersAndLeavesTheRestAsTheyWere() {
        ModifierSources.reserve(MODIFIER, OWNER, List.of(ItemKind.BRIDGE));
        ModifierSources.reserve("OTHER_TEST", "storm_front", ItemKind.sectionItemTypes());
        Map<String, ModifierSources.Source> unchanged = ModifierSources.all();

        ModifierSources.release("nobody");

        assertSame(unchanged, ModifierSources.all());

        ModifierSources.release(OWNER);

        assertFalse(ModifierSources.isReserved(MODIFIER));
        assertEquals(Set.of(), ModifierSources.parts(MODIFIER));
        assertEquals(Set.copyOf(ItemKind.sectionItemTypes()), ModifierSources.parts("OTHER_TEST"));
    }
}
