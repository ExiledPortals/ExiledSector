package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarpedModifierTest {

    private static final String VENTS = "test_reactor_vents";
    private static final String FOREIGN_FLUX = "test_foreign_flux";
    private static final RolledModifier VENTED = new RolledModifier(VENTS, ModifierSlot.SUFFIX, List.of(new RolledLine("VENT_RATE_PERCENT", 15f)));

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(VENTS, "suffix", "VENT_RATE_PERCENT:10:20", "Vented"))
                .put(HullItemRows.modifierRow(FOREIGN_FLUX, "prefix", "FLUX_CAPACITY_MULT:1:2", "Fluxed")));
        ItemDefinitions.register(new JSONArray().put(HullItemRows.row(HullItemRows.MILITARY, HullItemRows.MILITARY_SECTION,
                HullItemRows.MILITARY_PREFIXES, HullItemRows.MILITARY_SUFFIXES)));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    @Test
    void theWarpedFlagSurvivesEveryCopy() {
        RolledModifier warped = VENTED.asWarped();

        assertTrue(warped.warped());
        assertFalse(VENTED.warped());
        assertNotEquals(VENTED, warped);
        assertTrue(warped.asImmutable().warped());
        assertTrue(warped.asImmutable().immutable());
        assertTrue(warped.withLines(List.of(new RolledLine("VENT_RATE_PERCENT", 12f))).warped());
        assertTrue(warped.withModifierId("renamed").warped());
        assertEquals(warped, VENTED.asWarped());
        assertEquals(warped.hashCode(), VENTED.asWarped().hashCode());
    }

    @Test
    void anItemResolvesItsWarpedModifierFromTheCatalogue() {
        Item warped = Item.create("warped", HullItemRows.MILITARY, 7L, List.of(VENTED.asWarped()));
        Item plain = Item.create("plain", HullItemRows.MILITARY, 7L, List.of(VENTED));

        assertTrue(warped.isWarped());
        assertFalse(plain.isWarped());
        assertNotNull(warped.explicitModifier(VENTS));
        assertEquals(20f, warped.explicitModifier(VENTS).line("VENT_RATE_PERCENT").max(), 0.001f);
        assertNull(plain.explicitModifier(VENTS));
    }

    @Test
    void aWarpedModifierKeepsItsIdWhenTheOwnPoolHasTheSameEffect() {
        ItemDefinition military = ItemDefinitions.get(HullItemRows.MILITARY);
        RolledModifier foreign = new RolledModifier(FOREIGN_FLUX, ModifierSlot.PREFIX, List.of(new RolledLine("FLUX_CAPACITY_MULT", 1.5f)));

        assertEquals("FLUX_CAPACITY_MULT", military.withCurrentIds(List.of(foreign)).get(0).modifierId());
        assertEquals(List.of(foreign.asWarped()), military.withCurrentIds(List.of(foreign.asWarped())));
    }
}
