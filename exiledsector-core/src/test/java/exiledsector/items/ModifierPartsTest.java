package exiledsector.items;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierPartsTest {

    private static final String BRIDGE_PREFIX = "HULL_PERCENT_TEST";
    private static final String SHARED_SUFFIX = "ARMOR_PERCENT_TEST";
    private static final String UNIQUE_PREFIX = "FLUX_CAPACITY_MULT_TEST";
    private static final String BRIDGE_IMPLICIT = "TOP_SPEED_PERCENT_TEST_IMPLICIT";
    private static final String CORRUPTION_IMPLICIT = "CARGO_CAPACITY_PERCENT_TEST_IMPLICIT";
    private static final String RESERVED = "TIMEFLOW_PERCENT_TEST";
    private static final String UNUSED = "HULL_MULT_TEST";
    private static final String OWNER = "biome:test";
    private static final Set<ItemKind> EVERY_PART = Set.copyOf(ItemKind.sectionItemTypes());

    @BeforeEach
    void register() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(BRIDGE_PREFIX, "prefix", "HULL_PERCENT:4:6", "Sturdy"))
                .put(HullItemRows.modifierRow(SHARED_SUFFIX, "suffix", "ARMOR_PERCENT:4:6", "Plating"))
                .put(HullItemRows.modifierRow(UNIQUE_PREFIX, "prefix", "FLUX_CAPACITY_MULT:4:6", ""))
                .put(HullItemRows.modifierRow(BRIDGE_IMPLICIT, "implicit", "TOP_SPEED_PERCENT:4:6", ""))
                .put(HullItemRows.modifierRow(CORRUPTION_IMPLICIT, "implicit", "CARGO_CAPACITY_PERCENT:4:6", ""))
                .put(HullItemRows.modifierRow(RESERVED, "prefix", "TIMEFLOW_PERCENT:3:5", "Chronal"))
                .put(HullItemRows.modifierRow(UNUSED, "suffix", "HULL_MULT:4:6", "Spare")));
        ItemDefinitions.register(List.of(
                definition("test_bridge", ItemKind.BRIDGE, false, List.of(entry(BRIDGE_PREFIX)), List.of(entry(SHARED_SUFFIX))),
                definition("test_reactor", ItemKind.REACTOR, false, List.of(), List.of(entry(SHARED_SUFFIX))),
                definition("test_relic", ItemKind.ARMOR_PLATING, true, List.of(entry(UNIQUE_PREFIX)), List.of())));
    }

    @AfterEach
    void clear() {
        ModifierSources.clear();
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static PoolEntry entry(String modifierId) {
        return new PoolEntry(ItemModifiers.get(modifierId), 1f);
    }

    private static ItemDefinition definition(String id, ItemKind kind, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, kind, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    private static void registerImplicits() throws Exception {
        ItemImplicits.register(new JSONArray().put(new JSONObject().put("section", "bridge").put("implicits", "@" + BRIDGE_IMPLICIT)));
        ItemImplicits.registerCorruptionPool(new JSONArray()
                .put(new JSONObject().put(CorruptionImplicitPool.IMPLICIT_COLUMN, "@" + CORRUPTION_IMPLICIT)));
    }

    @Test
    void eachPoolAndUniqueDefinitionAddsItsOwnPart() {
        assertEquals(Set.of(ItemKind.BRIDGE), ModifierParts.parts(BRIDGE_PREFIX));
        assertEquals(Set.of(ItemKind.BRIDGE, ItemKind.REACTOR), ModifierParts.parts(SHARED_SUFFIX));
        assertEquals(Set.of(ItemKind.ARMOR_PLATING), ModifierParts.parts(UNIQUE_PREFIX));
        assertEquals(Set.of(), ModifierParts.parts(UNUSED));
        assertTrue(ModifierParts.modifiers(ItemKind.REACTOR).stream().anyMatch(modifier -> modifier.id().equals(SHARED_SUFFIX)));
    }

    @Test
    void corruptionCanPutACorruptionOrPartImplicitOnEveryPart() throws Exception {
        registerImplicits();

        assertEquals(EVERY_PART, ModifierParts.parts(CORRUPTION_IMPLICIT));
        assertEquals(EVERY_PART, ModifierParts.parts(BRIDGE_IMPLICIT));
        assertTrue(ModifierParts.modifiers(ItemKind.FLIGHT_DECK).stream().anyMatch(modifier -> modifier.id().equals(BRIDGE_IMPLICIT)));
    }

    @Test
    void aSourceAddsThePartsItReservedForUntilItReleasesThem() {
        ModifierSources.reserve(RESERVED, OWNER, Set.of(ItemKind.REACTOR));
        ModifierSources.reserve(RESERVED, OWNER, Set.of(ItemKind.PHASE_COIL));

        assertEquals(EnumSet.of(ItemKind.REACTOR, ItemKind.PHASE_COIL), ModifierParts.parts(RESERVED));

        ModifierSources.release(OWNER);

        assertEquals(Set.of(), ModifierParts.parts(RESERVED));
    }

    @Test
    void theIndexIsReusedUntilItsDataChanges() {
        ModifierParts.Index first = ModifierParts.index();

        assertSame(first, ModifierParts.index());

        ModifierSources.reserve(RESERVED, OWNER, Set.of(ItemKind.BRIDGE));
        ModifierParts.Index reserved = ModifierParts.index();

        assertNotSame(first, reserved);
        assertEquals(Set.of(ItemKind.BRIDGE), reserved.parts(RESERVED));

        ItemDefinitions.register(List.of(definition("test_coil", ItemKind.PHASE_COIL, false, List.of(entry(BRIDGE_PREFIX)), List.of())));

        assertEquals(Set.of(ItemKind.PHASE_COIL), ModifierParts.parts(BRIDGE_PREFIX));
    }

    @Test
    void aSubroutinePartIsLeftOutOfTheHullItemIndex() {
        ModifierSources.reserve(RESERVED, OWNER, Set.of(ItemKind.SUBROUTINE, ItemKind.BRIDGE));

        assertEquals(Set.of(ItemKind.BRIDGE), ModifierParts.parts(RESERVED));
        assertEquals(List.of(), ModifierParts.modifiers(ItemKind.SUBROUTINE));
    }
}
